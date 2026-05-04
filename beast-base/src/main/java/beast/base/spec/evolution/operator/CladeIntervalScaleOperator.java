package beast.base.spec.evolution.operator;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Bactrian draw whose scale factor is keyed on the node's *clade*
 * (= BitSet of descendant leaf numbers), and walks up the ancestor chain
 * to recompute heights so that exactly one margin changes. Each clade
 * carries its own adaptive σ; unseen clades fall back to a global default
 * σ that itself adapts as a running average.
 *
 * The advantage over rank-indexed adaptation is that a clade's identity
 * is topology-invariant: a learned σ for a particular clade survives
 * topology moves and can be reused whenever that clade reappears.
 *
 * Sampled-ancestor / fake nodes are skipped, matching IntervalScaleOperator.
 */
@Description("Per-clade adaptive scale move on a tree's interval margins. "
        + "Each internal node's clade (set of descendant leaves) carries "
        + "its own scale factor that adapts toward the 1D acceptance target. "
        + "Stable across topology moves because clade identity is "
        + "topology-invariant.")
public class CladeIntervalScaleOperator extends TreeOperator {

    public final Input<KernelDistribution> kernelDistributionInput = new Input<>(
            "kernelDistribution", "provides sample distribution for proposals",
            KernelDistribution.newDefaultKernelDistribution());

    public final Input<Double> initialScaleFactorInput = new Input<>("scaleFactor",
            "starting scale factor for each clade (range 0..1; close to 1 = small jumps)", 0.75);

    public final Input<Double> upperInput = new Input<>("upper",
            "upper limit of per-clade scale factor", 1.0 - 1e-8);

    public final Input<Double> lowerInput = new Input<>("lower",
            "lower limit of per-clade scale factor", 1e-8);

    public final Input<Boolean> optimiseInput = new Input<>("optimise",
            "auto-tune the per-clade scale factors toward the 1D acceptance target", true);

    public final Input<Double> defaultLearningRateInput = new Input<>(
            "defaultLearningRate",
            "rate at which the global default scale factor (used for unseen clades) "
                    + "tracks the mean of the seen clades; 0 = frozen, 1 = pure copy of last update", 0.01);

    private KernelDistribution kernel;
    private double upper, lower;
    private double defaultScaleFactor;
    private double defaultLearningRate;
    private final Map<BitSet, Double> cladeScaleFactor = new HashMap<>();
    private BitSet lastClade;

    @Override
    public void initAndValidate() {
        kernel = kernelDistributionInput.get();
        upper = upperInput.get();
        lower = lowerInput.get();
        defaultScaleFactor = initialScaleFactorInput.get();
        defaultLearningRate = defaultLearningRateInput.get();
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

        BitSet clade = new BitSet();
        collectLeaves(node, clade);
        lastClade = clade;

        double sf = cladeScaleFactor.computeIfAbsent(clade, k -> defaultScaleFactor);

        double childMax = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        double oldMargin = node.getHeight() - childMax;
        if (oldMargin <= 0) return Double.NEGATIVE_INFINITY;

        double scaler = kernel.getScaler(0, sf);

        // Same per-margin scale + ancestor recursion as PerRankIntervalScaleOperator.
        // Snapshot ancestor margins from the *original* tree before mutating.
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

    private static void collectLeaves(Node node, BitSet clade) {
        if (node.isLeaf()) {
            clade.set(node.getNr());
        } else {
            collectLeaves(node.getLeft(), clade);
            collectLeaves(node.getRight(), clade);
        }
    }

    @Override
    public void optimize(double logAlpha) {
        if (!optimiseInput.get()) return;
        if (lastClade == null) return;
        Double sf = cladeScaleFactor.get(lastClade);
        if (sf == null) return;
        double delta = calcDelta(logAlpha);
        delta += Math.log(sf);
        double newSf = Math.exp(delta);
        newSf = Math.max(Math.min(newSf, upper), lower);
        cladeScaleFactor.put(lastClade, newSf);
        // Track running average so unseen clades get a sensible starting σ.
        defaultScaleFactor = (1.0 - defaultLearningRate) * defaultScaleFactor
                + defaultLearningRate * newSf;
    }

    @Override
    public double getTargetAcceptanceProbability() {
        return 0.44;
    }

    @Override
    public double getCoercableParameterValue() {
        return defaultScaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        defaultScaleFactor = Math.max(Math.min(value, upper), lower);
    }

    public Map<BitSet, Double> getCladeScaleFactors() {
        return new HashMap<>(cladeScaleFactor);
    }

    public int getNumLearnedClades() {
        return cladeScaleFactor.size();
    }

    @Override
    public void storeToFile(final java.io.PrintWriter out) {
        // Write per-clade scale factors as a JSON object for offline analysis.
        // Each clade is encoded as a string of '1'/'0' bits over leaf indices.
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(getID() == null ? "unknown" : getID()).append("\",");
        sb.append("\"p\":").append(getCoercableParameterValue()).append(',');
        sb.append("\"defaultScale\":").append(defaultScaleFactor).append(',');
        sb.append("\"numClades\":").append(cladeScaleFactor.size()).append(',');
        sb.append("\"clades\":[");
        boolean first = true;
        for (Map.Entry<BitSet, Double> e : cladeScaleFactor.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            BitSet b = e.getKey();
            sb.append("{\"size\":").append(b.cardinality());
            sb.append(",\"sigma\":").append(e.getValue());
            sb.append("}");
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
        DecimalFormat fmt = new DecimalFormat("#.###");
        if (prob < 0.20 || prob > 0.65) {
            return "Acceptance " + fmt.format(prob)
                    + " is outside [0.20, 0.65]; default scaleFactor is " + fmt.format(defaultScaleFactor);
        }
        return "";
    }
}
