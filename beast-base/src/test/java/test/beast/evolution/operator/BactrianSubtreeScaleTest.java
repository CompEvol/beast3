package test.beast.evolution.operator;

import beast.base.evolution.operator.kernel.BactrianSubtreeScale;
import beast.base.evolution.operator.kernel.BactrianSubtreeSlide;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.evolution.tree.TreeParser;
import beast.base.evolution.tree.coalescent.Coalescent;
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.evolution.tree.coalescent.ConstantPopulation;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BactrianSubtreeScale scales, in log space, the branch above the subtree a slid node carries,
 * instead of adding a step to the node height (BactrianSubtreeSlide).
 *
 * <p>Mechanics: on an ultrametric tree it never proposes a node below its carried subtree and never
 * flat-rejects from that bound.</p>
 *
 * <p>Exact distribution: these tests run it as the only operator on a 4-taxon tree
 * under a constant-population coalescent with no data, whose distribution is known exactly:
 * with k lineages the waiting time is exponential with mean 2θ/(k(k-1)), i.e. θ/6, θ/3 and θ
 * for k = 4, 3, 2, and every ranked topology is equally likely, so the balanced shape
 * ((·,·),(·,·)) has probability 1/3. The additive slide is run the same way as a control.
 */
public class BactrianSubtreeScaleTest {

    private static final double THETA = 1.0;
    private static final int STEPS = 2_000_000;
    private static final int LOG_EVERY = 20;

    /** records, per sample, the three coalescent intervals and whether the root splits 2|2 */
    static class Recorder extends Logger {
        final Tree tree;
        final List<double[]> intervals = new ArrayList<>();
        final List<Boolean> balanced = new ArrayList<>();

        Recorder(Tree tree) {
            this.tree = tree;
            initByName("log", tree, "logEvery", LOG_EVERY);
        }

        @Override public void init() { }
        @Override public void close() { }

        @Override
        public void log(long sampleNr) {
            if (sampleNr <= 0 || sampleNr % LOG_EVERY != 0) return;
            List<Double> heights = new ArrayList<>();
            for (Node n : tree.getInternalNodes()) heights.add(n.getHeight());
            heights.sort(null);
            intervals.add(new double[]{heights.get(0), heights.get(1) - heights.get(0), heights.get(2) - heights.get(1)});
            Node root = tree.getRoot();
            balanced.add(root.getLeft().getLeafNodeCount() == 2);
        }
    }

    static class Run {
        Recorder recorder;
        Operator operator;
    }

    private static Run run(boolean scaled, double theta, double size, boolean optimise, long seed) {
        Randomizer.setSeed(seed);
        Tree tree = new TreeParser("((A:0.1,B:0.1):0.2,(C:0.2,D:0.2):0.1);", false);
        tree.setID("tree");
        TreeIntervals intervals = new TreeIntervals();
        intervals.initByName("tree", tree);
        ConstantPopulation pop = new ConstantPopulation();
        pop.initByName("popSize", new RealScalarParam<>(theta, PositiveReal.INSTANCE));
        Coalescent coalescent = new Coalescent();
        coalescent.initByName("treeIntervals", intervals, "populationModel", pop);

        Operator slide = scaled ? new BactrianSubtreeScale() : new BactrianSubtreeSlide();
        slide.initByName("tree", tree, "weight", 1.0, "size", size, "optimise", optimise);

        State state = new State();
        state.initByName("stateNode", tree);
        Run r = new Run();
        r.recorder = new Recorder(tree);
        r.operator = slide;
        MCMC mcmc = new MCMC();
        mcmc.initByName("chainLength", (long) STEPS, "state", state, "distribution", coalescent,
                "operator", slide, "logger", r.recorder);
        try {
            mcmc.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    /** mean over samples after 10% burn-in, with a batch-means standard error */
    private static double[] meanAndSe(List<Double> x) {
        List<Double> y = x.subList(x.size() / 10, x.size());
        int batches = 50, size = y.size() / batches;
        double total = 0;
        double[] b = new double[batches];
        for (int k = 0; k < batches; k++) {
            double s = 0;
            for (int j = 0; j < size; j++) s += y.get(k * size + j);
            b[k] = s / size;
            total += b[k];
        }
        double mean = total / batches, v = 0;
        for (double m : b) v += (m - mean) * (m - mean);
        return new double[]{mean, Math.sqrt(v / (batches - 1) / batches)};
    }

    private static void assertCoalescentPrior(Run r, double theta, String label) {
        double[] expected = {theta / 6, theta / 3, theta};
        for (int k = 0; k < 3; k++) {
            final int kk = k;
            List<Double> x = new ArrayList<>();
            for (double[] v : r.recorder.intervals) x.add(v[kk]);
            double[] ms = meanAndSe(x);
            System.out.printf("%s interval %d: mean %.4f (exact %.4f, se %.4f)%n", label, k + 1, ms[0], expected[k], ms[1]);
            assertEquals(expected[k], ms[0], 4 * ms[1] + 1e-3 * expected[k], label + ": mean of interval " + (k + 1));
        }
        List<Double> bal = new ArrayList<>();
        for (boolean b : r.recorder.balanced) bal.add(b ? 1.0 : 0.0);
        double[] ms = meanAndSe(bal);
        System.out.printf("%s balanced shape: %.4f (exact 0.3333, se %.4f)%n", label, ms[0], ms[1]);
        assertEquals(1.0 / 3.0, ms[0], 4 * ms[1] + 1e-3, label + ": frequency of the balanced shape");
    }

    /** Every non-root node must sit below its parent, and no height may be negative. */
    private static void assertValid(Tree tree) {
        for (Node node : tree.getNodesAsArray()) {
            if (!node.isRoot()) {
                assertTrue(node.getHeight() < node.getParent().getHeight() + 1e-12,
                        "node " + node.getNr() + " height " + node.getHeight() + " not below parent " + node.getParent().getHeight());
            }
            assertTrue(node.getHeight() >= 0.0, "negative height");
        }
    }

    /** From beast-phylonco: on an ultrametric tree no proposal is invalid or violates the floor. */
    @Test
    public void floorNeverViolated() {
        Randomizer.setSeed(42);
        Tree tree = new TreeParser();
        tree.initByName("newick", "((A:2,B:2):1,(C:2,(D:1,(E:0.5,F:0.5):0.5):1):1):0;",
                "IsLabelledNewick", true, "adjustTipHeights", false);
        BactrianSubtreeScale op = new BactrianSubtreeScale();
        op.initByName("tree", tree, "size", 0.5, "weight", 1.0);
        int infinite = 0;
        for (int k = 0; k < 20000; k++) {
            double logHR = op.proposal();
            if (logHR == Double.NEGATIVE_INFINITY) {
                infinite++;
            } else {
                assertTrue(Double.isFinite(logHR), "non-finite logHR " + logHR);
            }
            assertValid(tree);
        }
        assertEquals(0, infinite, "unexpected flat rejects on an ultrametric tree");
    }

    @Test
    public void scaledSlideSamplesTheCoalescentPrior() {
        assertCoalescentPrior(run(true, THETA, 0.5, true, 127), THETA, "scaled");
    }

    @Test
    public void additiveSlideSamplesTheCoalescentPrior() {
        assertCoalescentPrior(run(false, THETA, 0.3, true, 127), THETA, "additive");
    }

    /**
     * The scaled move is unitless: with tuning off and the same size, trees 10^6 times apart in height
     * accept moves at the same rate and sample the same (rescaled) distribution. (Not step for step:
     * rounding eventually flips one accept/reject decision, after which the chains differ.) The
     * additive slide with a fixed size is far from unit-free, as a contrast.
     */
    @Test
    public void scaledSlideIsUnitFree() {
        Run small = run(true, 1e-3, 0.5, false, 42), large = run(true, 1e3, 0.5, false, 42);
        double accSmall = acceptance(small), accLarge = acceptance(large);
        System.out.printf("scaled, size 0.5: acceptance %.4f (theta 1e-3) vs %.4f (theta 1e3)%n", accSmall, accLarge);
        assertEquals(accSmall, accLarge, 0.005, "acceptance differs between theta = 1e-3 and 1e3");
        assertCoalescentPrior(small, 1e-3, "scaled, theta 1e-3");
        assertCoalescentPrior(large, 1e3, "scaled, theta 1e3");

        Run addSmall = run(false, 1e-3, 0.3, false, 42), addLarge = run(false, 1e3, 0.3, false, 42);
        double accAddSmall = acceptance(addSmall), accAddLarge = acceptance(addLarge);
        System.out.printf("additive, size 0.3 at theta 1e-3 vs 1e3: acceptance %.4f vs %.4f%n",
                accAddSmall, accAddLarge);
        // contrast: the additive slide's fixed size is far too large at theta 1e-3 and far too small at 1e3
        assertTrue(accAddSmall < 0.05, "additive slide at theta 1e-3 should almost always reject");
        assertTrue(accAddLarge > 0.9, "additive slide at theta 1e3 should almost always accept");
    }

    private static double acceptance(Run r) {
        return r.operator.get_m_nNrAccepted() / (double) (r.operator.get_m_nNrAccepted() + r.operator.get_m_nNrRejected());
    }
}
