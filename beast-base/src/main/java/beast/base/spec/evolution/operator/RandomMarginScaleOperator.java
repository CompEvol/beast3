package beast.base.spec.evolution.operator;


import java.text.DecimalFormat;
import java.util.ArrayList;
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
 * Picks one internal node per proposal, scales its interval margin
 * (h_i - max(child h_i)) by a Bactrian draw with a single shared scale
 * factor, then walks up the ancestor chain recomputing each parent as
 * max(updated child heights) + parent's old margin. Multiple heights
 * change in general but only one margin does; log Jacobian = log(scaler).
 *
 * Compared to {@link PerRankIntervalScaleOperator} this operator drops
 * the per-rank scale-factor array — a single shared σ adapts toward the
 * 1D acceptance optimum (0.44, Gelman-Roberts-Gilks 1996). On problems
 * where the per-rank optimal σ is roughly uniform (which is the typical
 * case for constant-population coalescent posteriors), this operator
 * gives the same statistical performance with simpler bookkeeping.
 *
 * Sampled-ancestor / fake nodes are skipped, matching IntervalScaleOperator.
 */
@Description("Single-σ adaptive scale move on a random tree margin. "
        + "Picks one internal node, scales its margin, propagates the "
        + "height change up the ancestor chain. Cheaper, equally effective "
        + "alternative to per-rank or per-clade adaptive variants when "
        + "optimal σ is roughly uniform across positions.")
public class RandomMarginScaleOperator extends TreeOperator {

    public final Input<KernelDistribution> kernelDistributionInput = new Input<>(
            "kernelDistribution", "provides sample distribution for proposals",
            KernelDistribution.newDefaultKernelDistribution());

    public final Input<Double> initialScaleFactorInput = new Input<>("scaleFactor",
            "starting scale factor (range 0..1; close to 1 = small jumps)", 0.75);

    public final Input<Double> upperInput = new Input<>("upper",
            "upper limit of scale factor", 1.0 - 1e-8);

    public final Input<Double> lowerInput = new Input<>("lower",
            "lower limit of scale factor", 1e-8);

    public final Input<Boolean> optimiseInput = new Input<>("optimise",
            "auto-tune the scale factor toward the 1D acceptance target", true);

    private KernelDistribution kernel;
    private double scaleFactor;
    private double upper, lower;

    @Override
    public void initAndValidate() {
        kernel = kernelDistributionInput.get();
        scaleFactor = initialScaleFactorInput.get();
        upper = upperInput.get();
        lower = lowerInput.get();
    }

    @Override
    public double proposal() {
        Tree tree = (Tree) InputUtil.get(treeInput, this);

        List<Node> internals = new ArrayList<>();
        for (Node n : tree.getNodesAsArray()) {
            if (n != null && !n.isLeaf() && !n.isFake()) internals.add(n);
        }
        if (internals.isEmpty()) return Double.NEGATIVE_INFINITY;

        Node node = internals.get(Randomizer.nextInt(internals.size()));

        double childMax = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        double oldMargin = node.getHeight() - childMax;
        if (oldMargin <= 0) return Double.NEGATIVE_INFINITY;

        double scaler = kernel.getScaler(0, scaleFactor);

        // Snapshot ancestor margins from the *original* tree before mutating,
        // then apply per-margin scale and walk up to keep all other margins
        // unchanged in m-coordinates. Same recursion as PerRank /
        // CladeIntervalScaleOperator.
        List<Node> chain = new ArrayList<>();
        List<Double> ancestorOldMargins = new ArrayList<>();
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
        double delta = calcDelta(logAlpha);
        delta += Math.log(scaleFactor);
        double sf = Math.exp(delta);
        scaleFactor = Math.max(Math.min(sf, upper), lower);
    }

    @Override
    public double getTargetAcceptanceProbability() {
        return 0.44;
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        scaleFactor = Math.max(Math.min(value, upper), lower);
    }

    @Override
    public String getPerformanceSuggestion() {
        if (m_nNrAccepted + m_nNrRejected == 0) return "";
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();
        double ratio = Math.max(0.5, Math.min(2.0, prob / targetProb));
        DecimalFormat fmt = new DecimalFormat("#.###");
        if (prob < 0.30 || prob > 0.55) {
            return "Try setting scaleFactor to about " + fmt.format(scaleFactor * ratio);
        }
        return "";
    }
}
