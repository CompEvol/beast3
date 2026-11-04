package test.beast.evolution.operator;

import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.evolution.operator.kernel.AdaptableVarianceMultivariateNormalOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.inference.distribution.LogNormalDistributionModel;
import beast.base.inference.distribution.Prior;
import beast.base.inference.operator.kernel.Transform;
import beast.base.inference.parameter.RealParameter;
import beast.base.spec.evolution.operator.AdaptableVarianceMultivariateNormalOperatorTest.ExpSumOfIntervals;
import beast.base.util.Randomizer;
import org.apache.commons.math4.legacy.stat.StatUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hastings ratio of the legacy AVMN operator when it moves a tree. The tree
 * contributes one dimension, its sum of intervals V, and the other dof - 1
 * intervals dilate with it, so the proposal must add (dof - 1) log(V'/V) on top
 * of the transform's own Jacobian. BEAUti 2.7 puts the tree in a NoTransform,
 * where the own Jacobian is 0.
 */
public class AdaptableVarianceMultivariateNormalOperatorTest {

    @Test
    public void noTransformTreeCountsTheCoScaledIntervals() throws Exception {
        assertLogHastingsRatio(new Transform.NoTransform(), 2);
    }

    @Test
    public void logTransformTreeCountsEveryScaledInterval() throws Exception {
        assertLogHastingsRatio(new Transform.LogTransform(), 3);
    }

    /**
     * expectedTreeDims is the multiple of log(V'/V) the proposal must return for
     * the 4-taxon ultrametric tree (dof 3): dof - 1 under NoTransform, dof under
     * LogTransform. The log-transformed parameter adds log(p'/p).
     */
    private void assertLogHastingsRatio(Transform treeTransform, int expectedTreeDims) throws Exception {
        Randomizer.setSeed(127);
        Tree tree = ultrametricTree();
        RealParameter param = new RealParameter(new Double[]{1.0});
        param.setID("param");
        AdaptableVarianceMultivariateNormalOperator operator = avmn(param, tree, treeTransform);
        registerInState(tree, param);

        int proposed = 0;
        for (int i = 0; i < 200; i++) {
            double[] heights = heightsOf(tree);
            double v = tree.getScalableValue();
            double p = param.getValue();

            double logHR = operator.proposal();
            if (logHR != Double.NEGATIVE_INFINITY) {
                proposed++;
                double expected = expectedTreeDims * Math.log(tree.getScalableValue() / v)
                        + Math.log(param.getValue() / p);
                assertEquals(expected, logHR, 1e-10, "proposal " + i);
            }

            restoreHeights(tree, heights);
            param.setValue(p);
        }
        assertTrue(proposed > 0, "every proposal was rejected");
    }

    /**
     * Density exp(-V) on node heights makes the 3 intervals iid Exp(1), so V must
     * be sampled from Gamma(3, 1). This is the BEAUti 2.7 set-up: the tree in a
     * NoTransform, a parameter in a LogTransform.
     */
    @Test
    @Tag("slow")
    public void noTransformTreeSamplesSumOfIntervalsFromGammaPrior() throws Exception {
        Randomizer.setSeed(123);

        Tree tree = ultrametricTree();
        RealParameter param = new RealParameter(new Double[]{1.0});
        param.setID("param");

        AdaptableVarianceMultivariateNormalOperator operator = avmn(param, tree, new Transform.NoTransform());

        ExpSumOfIntervals treePrior = new ExpSumOfIntervals();
        treePrior.initByName("tree", tree);
        LogNormalDistributionModel logNormal = new LogNormalDistributionModel();
        logNormal.initByName("M", "1.0", "S", "1.0", "meanInRealSpace", true);
        Prior paramPrior = new Prior();
        paramPrior.initByName("x", param, "distr", logNormal);
        CompoundDistribution posterior = new CompoundDistribution();
        posterior.initByName("distribution", treePrior, "distribution", paramPrior);

        State state = new State();
        state.initByName("stateNode", tree, "stateNode", param);

        TraceOf trace = new TraceOf();
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

    private AdaptableVarianceMultivariateNormalOperator avmn(RealParameter param, Tree tree,
                                                            Transform treeTransform) {
        Transform.LogTransform paramTransform = new Transform.LogTransform();
        paramTransform.initByName("f", param);
        ((BEASTObject) treeTransform).initByName("f", tree);
        AdaptableVarianceMultivariateNormalOperator operator =
                new AdaptableVarianceMultivariateNormalOperator();
        operator.initByName("weight", 1.0,
                "transformations", Arrays.asList(paramTransform, treeTransform),
                "beta", 0.05, "optimise", false);
        return operator;
    }

    /** Intervals: (A,B) 1, (C,D) 0.5, root 1; V = 2.5. */
    private Tree ultrametricTree() {
        Tree tree = new TreeParser("((A:1,B:1):1,(C:0.5,D:0.5):1.5);");
        tree.setID("tree");
        return tree;
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

    /** Records the first logged tree's sum of intervals and the first logged parameter. */
    public static class TraceOf extends Logger {
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
                } else if (o instanceof RealParameter param) {
                    paramValues.add(param.getValue());
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
