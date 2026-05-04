package beast.base.spec.evolution.operator;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;


/**
 * Picks one internal node per proposal, scales its interval margin by a
 * per-rank Bactrian draw, and adapts that rank's scale factor toward the
 * 1D acceptance target. The age-rank ordering matches {@link TreeIntervalsView}
 * (children-before-parents).
 *
 * Compared to a full multivariate-normal proposal over all intervals, this
 * operator targets the diagonal of the log-margin posterior — useful when
 * the prior (e.g. constant-population coalescent) makes the off-diagonals
 * nearly zero. It needs O(n) adaptation state instead of O(n^2), avoids
 * the per-step Cholesky, and converts joint accept/reject into n
 * independent decisions.
 *
 * Sampled-ancestor / fake nodes are skipped, matching IntervalScaleOperator.
 */
@Description("Single-rank adaptive scale move on a tree's interval margins. "
        + "Each internal node has its own scale factor that adapts toward "
        + "the 1D acceptance target. Rank-indexed by age, children before parents.")
public class PerRankIntervalScaleOperator extends TreeOperator {

    public final Input<KernelDistribution> kernelDistributionInput = new Input<>(
            "kernelDistribution", "provides sample distribution for proposals",
            KernelDistribution.newDefaultKernelDistribution());

    public final Input<Double> initialScaleFactorInput = new Input<>("scaleFactor",
            "starting per-rank scale factor (range 0..1; close to 1 = small jumps)", 0.75);

    public final Input<Double> upperInput = new Input<>("upper",
            "upper limit of per-rank scale factor", 1.0 - 1e-8);

    public final Input<Double> lowerInput = new Input<>("lower",
            "lower limit of per-rank scale factor", 1e-8);

    public final Input<Boolean> optimiseInput = new Input<>("optimise",
            "auto-tune the per-rank scale factors toward the 1D acceptance target", true);

    private KernelDistribution kernel;
    private double upper, lower;
    private double[] scaleFactor;
    private Node[] orderedInternal;
    private int lastRank;

    @Override
    public void initAndValidate() {
        kernel = kernelDistributionInput.get();
        upper = upperInput.get();
        lower = lowerInput.get();
        rebuildOrder();
        scaleFactor = new double[orderedInternal.length];
        Arrays.fill(scaleFactor, initialScaleFactorInput.get());
    }

    private void rebuildOrder() {
        Tree tree = (Tree) treeInput.get();
        Node[] all = tree.getNodesAsArray();
        List<Node> internals = new ArrayList<>();
        for (Node n : all) {
            if (n != null && !n.isLeaf() && !n.isFake()) internals.add(n);
        }
        internals.sort((a, b) -> Double.compare(a.getHeight(), b.getHeight()));
        orderedInternal = internals.toArray(new Node[0]);
        if (scaleFactor != null && scaleFactor.length != orderedInternal.length) {
            // tree dimension changed (sampled-ancestor toggling, etc.) — reset
            double init = initialScaleFactorInput.get();
            scaleFactor = new double[orderedInternal.length];
            Arrays.fill(scaleFactor, init);
        }
    }

    @Override
    public double proposal() {
        // Force a tree access so the operator framework treats the tree as touched.
        Tree tree = (Tree) InputUtil.get(treeInput, this);
        rebuildOrder();
        if (orderedInternal.length == 0) return Double.NEGATIVE_INFINITY;

        int rank = Randomizer.nextInt(orderedInternal.length);
        lastRank = rank;

        Node node = orderedInternal[rank];
        double childMax = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        double oldMargin = node.getHeight() - childMax;
        if (oldMargin <= 0) return Double.NEGATIVE_INFINITY;

        double scaler = kernel.getScaler(rank, scaleFactor[rank]);

        // We are scaling exactly one *margin* m_i = h_i - max(child h_i),
        // while leaving every other margin m_j (j != i) unchanged. In
        // m-coordinates that's a 1D scale with Jacobian = scaler. To keep the
        // tree consistent in h-coordinates we must walk up the ancestor chain
        // and recompute each ancestor's height as max(updated child heights)
        // + its own (unchanged) margin. Multiple heights change in general;
        // only one margin does. The recursion guarantees the tree remains
        // valid (parent above all children) without any rejection check.

        // Snapshot ancestor margins from the *original* tree before mutating.
        java.util.List<Node> chain = new java.util.ArrayList<>();
        java.util.List<Double> ancestorOldMargins = new java.util.ArrayList<>();
        Node cur = node;
        while (!cur.isRoot()) {
            Node parent = cur.getParent();
            double parentChildMaxOld = Math.max(parent.getLeft().getHeight(),
                                                parent.getRight().getHeight());
            ancestorOldMargins.add(parent.getHeight() - parentChildMaxOld);
            chain.add(parent);
            cur = parent;
        }

        double newMargin = oldMargin * scaler;
        node.setHeight(childMax + newMargin);

        for (int j = 0; j < chain.size(); j++) {
            Node parent = chain.get(j);
            double parentChildMaxNew = Math.max(parent.getLeft().getHeight(),
                                                parent.getRight().getHeight());
            parent.setHeight(parentChildMaxNew + ancestorOldMargins.get(j));
        }

        return Math.log(scaler);
    }

    @Override
    public void optimize(double logAlpha) {
        if (!optimiseInput.get()) return;
        if (lastRank < 0 || lastRank >= scaleFactor.length) return;
        double delta = calcDelta(logAlpha);
        double sf = scaleFactor[lastRank];
        delta += Math.log(sf);
        sf = Math.exp(delta);
        scaleFactor[lastRank] = Math.max(Math.min(sf, upper), lower);
    }

    @Override
    public double getTargetAcceptanceProbability() {
        // Each proposal is a 1D scale on a single per-rank margin; the
        // 1D random-walk Metropolis optimum is 0.44 (Gelman, Roberts,
        // Gilks 1996; Roberts & Rosenthal 2001). For high-dim joint
        // moves like AVMN the asymptotic 0.234 (Roberts, Gelman, Gilks
        // 1997) applies, but that is not this operator.
        return 0.44;
    }

    @Override
    public double getCoercableParameterValue() {
        // Average for state-file checkpointing; per-rank values are
        // re-bootstrapped on restart from this scalar.
        if (scaleFactor == null || scaleFactor.length == 0) return initialScaleFactorInput.get();
        double s = 0;
        for (double v : scaleFactor) s += v;
        return s / scaleFactor.length;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        double clamped = Math.max(Math.min(value, upper), lower);
        if (scaleFactor == null) {
            scaleFactor = new double[]{clamped};
        } else {
            Arrays.fill(scaleFactor, clamped);
        }
    }

    public double[] getScaleFactors() {
        return scaleFactor.clone();
    }

    @Override
    public void storeToFile(final java.io.PrintWriter out) {
        // Write per-rank scale factors as a JSON object for offline analysis.
        // Format: {"id":"...", "p":mean, "scales":[s_0, s_1, ...], "accept":..., "reject":...}
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(getID() == null ? "unknown" : getID()).append("\",");
        sb.append("\"p\":").append(getCoercableParameterValue()).append(',');
        sb.append("\"scales\":[");
        if (scaleFactor != null) {
            for (int i = 0; i < scaleFactor.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(scaleFactor[i]);
            }
        }
        sb.append("],");
        sb.append("\"accept\":").append(m_nNrAccepted).append(',');
        sb.append("\"reject\":").append(m_nNrRejected);
        sb.append('}');
        out.print(sb.toString());
    }

    @Override
    public String getPerformanceSuggestion() {
        if (m_nNrAccepted + m_nNrRejected == 0) return "";
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();
        double meanSF = getCoercableParameterValue();
        double ratio = Math.max(0.5, Math.min(2.0, prob / targetProb));
        DecimalFormat fmt = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Per-rank mean scaleFactor ~" + fmt.format(meanSF)
                    + "; consider initial scaleFactor near " + fmt.format(meanSF * ratio);
        }
        return "";
    }
}
