package beast.base.spec.evolution.operator;

import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.operator.Transform.LogTransform;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;
import org.apache.commons.math4.legacy.stat.StatUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trees enter the AVMN operator through {@link LogTransform}'s {@code scalable}
 * input, contributing one dimension each: their sum of intervals.
 */
public class AdaptableVarianceMultivariateNormalOperatorTest {

    @Test
    public void scalablesAddOneDimensionAndTheirStateNodes() throws Exception {
        RealVectorParam<PositiveReal> param =
                new RealVectorParam<>(new double[]{1.0, 2.0}, PositiveReal.INSTANCE);
        param.setID("param");
        Tree tree = ultrametricTree();

        AdaptableVarianceMultivariateNormalOperator operator = avmn(param, tree);

        assertEquals(3, operator.getMeans().length);
        List<StateNode> nodes = operator.listStateNodes();
        assertTrue(nodes.contains(param));
        assertTrue(nodes.contains(tree));
    }

    /** Every interval of a tree without sampled ancestors moves: dof = internal node count. */
    @Test
    public void logHastingsRatioCountsEveryScaledInterval() throws Exception {
        assertLogHastingsRatio(ultrametricTree(), 3);
    }

    /**
     * A fake node's height is pinned, so it is not a degree of freedom of the
     * dilation: the sampled-ancestor tree below has two internal nodes but dof 1.
     */
    @Test
    public void logHastingsRatioSkipsSampledAncestorFakeNodes() throws Exception {
        assertLogHastingsRatio(sampledAncestorTree(), 1);
    }

    /**
     * Each proposal must return dof * log(V'/V) for the tree, plus log(p'/p)
     * for the log-transformed parameter.
     */
    private void assertLogHastingsRatio(Tree tree, int dof) throws Exception {
        Randomizer.setSeed(127);
        RealScalarParam<PositiveReal> param = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        param.setID("param");
        AdaptableVarianceMultivariateNormalOperator operator = avmn(param, tree);
        registerInState(tree, param);

        int proposed = 0;
        for (int i = 0; i < 200; i++) {
            double[] heights = heightsOf(tree);
            double v = tree.getScalableValue();
            double p = param.get();

            double logHR = operator.proposal();
            if (logHR != Double.NEGATIVE_INFINITY) {
                proposed++;
                double expected = dof * Math.log(tree.getScalableValue() / v)
                        + Math.log(param.get() / p);
                assertEquals(expected, logHR, 1e-10, "proposal " + i);
            }

            restoreHeights(tree, heights);
            param.set(p);
        }
        assertTrue(proposed > 0, "every proposal was rejected");
    }

    /**
     * Density exp(-V) on node heights makes the n intervals iid Exp(1). Moving
     * only V at fixed interval proportions, the operator must then sample
     * V ~ Gamma(n, 1); a Jacobian that ignores the n-1 co-scaled intervals
     * would leave V ~ Exp(1) instead. A log-normal parameter shares the
     * transform to check that both are sampled jointly.
     */
    @Test
    @Tag("slow")
    public void samplesSumOfIntervalsFromGammaPrior() throws Exception {
        Randomizer.setSeed(123);

        Tree tree = ultrametricTree();
        RealScalarParam<PositiveReal> param = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        param.setID("param");

        AdaptableVarianceMultivariateNormalOperator operator = avmn(param, tree);

        ExpSumOfIntervals treePrior = new ExpSumOfIntervals();
        treePrior.initByName("tree", tree);
        LogNormal paramPrior = new LogNormal(param,
                new RealScalarParam<>(1.0, Real.INSTANCE),
                new RealScalarParam<>(1.0, PositiveReal.INSTANCE), true);
        CompoundDistribution posterior = new CompoundDistribution();
        posterior.initByName("distribution", treePrior, "distribution", paramPrior);

        State state = new State();
        state.initByName("stateNode", tree, "stateNode", param);

        ScalableTrace trace = new ScalableTrace();
        trace.initByName("log", tree, "log", param, "logEvery", 10, "burnin", 10000);

        MCMC mcmc = new MCMC();
        mcmc.initByName(
                "chainLength", "2000000",
                "state", state,
                "distribution", posterior,
                "operator", operator,
                "logger", trace);
        mcmc.run();

        double[] v = trace.sumOfIntervals();
        double[] p = trace.paramValues();
        // n = 3 intervals: Gamma(3, 1) has mean 3 and variance 3
        assertEquals(3.0, StatUtils.mean(v), 0.05);
        assertEquals(3.0, StatUtils.variance(v), 0.2);
        // LogNormal with real-space mean 1 and S = 1
        assertEquals(1.0, StatUtils.mean(p), 0.05);
        assertEquals(Math.exp(-0.5), StatUtils.percentile(p, 50), 0.03);
    }

    private AdaptableVarianceMultivariateNormalOperator avmn(StateNode param, Tree tree) {
        LogTransform transform = new LogTransform();
        transform.initByName("f", param, "scalable", tree);
        AdaptableVarianceMultivariateNormalOperator operator =
                new AdaptableVarianceMultivariateNormalOperator();
        operator.initByName("weight", 1.0, "transformations", transform,
                "beta", 0.05, "optimise", false);
        return operator;
    }

    /** Intervals: (A,B) 1, (C,D) 0.5, root 1; V = 2.5. */
    private Tree ultrametricTree() {
        Tree tree = new TreeParser("((A:1,B:1):1,(C:0.5,D:0.5):1.5);");
        tree.setID("tree");
        return tree;
    }

    /**
     * Sampled-ancestor tree; only X moves under interval scaling, V = 2.
     * <pre>
     *   root (fake, h=5)
     *    ├── SA (leaf, h=5)
     *    └── X  (h=3)
     *         ├── A (h=0)
     *         └── B (h=1)
     * </pre>
     */
    private Tree sampledAncestorTree() {
        Node a = leaf("A", 0, 0.0);
        Node b = leaf("B", 1, 1.0);
        Node sa = leaf("SA", 2, 5.0);
        Node x = internal(3, 3.0, a, b);
        Node root = internal(4, 5.0, sa, x);
        Tree tree = new Tree(root);
        tree.setID("tree");
        return tree;
    }

    private Node leaf(String id, int nr, double height) {
        Node n = new Node(id);
        n.setNr(nr);
        n.setHeight(height);
        return n;
    }

    private Node internal(int nr, double height, Node left, Node right) {
        Node n = new Node();
        n.setNr(nr);
        n.setHeight(height);
        n.addChild(left);
        n.addChild(right);
        return n;
    }

    private double[] heightsOf(Tree tree) {
        Node[] nodes = tree.getNodesAsArray();
        double[] heights = new double[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            heights[i] = nodes[i].getHeight();
        }
        return heights;
    }

    private void restoreHeights(Tree tree, double[] heights) {
        Node[] nodes = tree.getNodesAsArray();
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setHeight(heights[i]);
        }
    }

    /** Operators need their StateNodes owned by a State before proposal() works. */
    private void registerInState(StateNode... nodes) throws Exception {
        State state = new State();
        state.initByName("stateNode", Arrays.asList(nodes));
        state.initialise();
        state.setPosterior(new CompoundDistribution());
    }

    /** log density -V on node heights, V the tree's sum of intervals. */
    public static class ExpSumOfIntervals extends Distribution {
        final public Input<Tree> treeInput = new Input<>("tree", "tree", Input.Validate.REQUIRED);

        @Override
        public double calculateLogP() {
            logP = -treeInput.get().getScalableValue();
            return logP;
        }

        @Override
        protected boolean requiresRecalculation() {
            return true;
        }

        @Override
        public List<String> getArguments() {
            return List.of(treeInput.get().getID());
        }

        @Override
        public List<String> getConditions() {
            return List.of();
        }

        @Override
        public void sample(State state, Random random) {
            throw new UnsupportedOperationException();
        }
    }

    /** Records the first logged tree's sum of intervals and the first logged parameter. */
    public static class ScalableTrace extends Logger {
        final public Input<Integer> burninInput = new Input<>("burnin", "samples to skip", 0);

        private final List<Double> sumOfIntervals = new ArrayList<>();
        private final List<Double> paramValues = new ArrayList<>();

        @Override
        public void initAndValidate() {
        }

        @Override
        public void init() {
        }

        @Override
        public void log(long sample) {
            if (sample % everyInput.get() > 0 || sample < burninInput.get()) {
                return;
            }
            for (BEASTObject o : loggersInput.get()) {
                if (o instanceof Tree tree) {
                    sumOfIntervals.add(tree.getScalableValue());
                } else if (o instanceof RealScalarParam<?> param) {
                    paramValues.add(param.get());
                }
            }
        }

        @Override
        public void close() {
        }

        double[] sumOfIntervals() {
            return sumOfIntervals.stream().mapToDouble(Double::doubleValue).toArray();
        }

        double[] paramValues() {
            return paramValues.stream().mapToDouble(Double::doubleValue).toArray();
        }
    }
}
