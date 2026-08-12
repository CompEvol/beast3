package beast.base.spec.evolution.operator;

import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.State;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression tests for interval scaling of sampled-ancestor trees by the tree
 * operators.
 *
 * <p>A fake node's height is pinned to its direct-ancestor leaf's sampling time,
 * so it caps the subtree below it. Scaling that subtree up through the cap yields
 * a tree with a child above its parent, which downstream code does not survive:
 * {@code MRCAPrior.getCommonAncestor} spins forever walking parents and
 * {@code TreeLikelihood.traverse} blows the stack.</p>
 *
 * <p>The scaling recursion existed in three copies &mdash; {@link Node#intervalScale(double)},
 * {@link UpDownOperator} and {@link IntervalScaleOperator} &mdash; and only the
 * first was checked, so this went unnoticed. The operators now delegate to
 * {@code Node.intervalScale}. These tests drive the operators rather than the
 * recursion so a future private copy is caught here.</p>
 */
public class SampledAncestorScalingTest {

    /** How many proposals to draw per operator; scale factors are random. */
    private static final int PROPOSALS = 2000;

    @Test
    public void upDownOperatorNeverLeavesSampledAncestorTreeInvalid() throws Exception {
        Randomizer.setSeed(127);

        Tree tree = buildSampledAncestorTree();
        RealScalarParam<PositiveReal> rate =
                new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        rate.setID("rate");

        UpDownOperator operator = new UpDownOperator();
        operator.initByName("weight", "1", "up", tree, "down", rate,
                "scaleFactor", 0.5, "optimise", false);
        registerInState(operator, tree, rate);

        assertEveryProposalLeavesTreeValid(operator, tree);
    }

    @Test
    public void intervalScaleOperatorNeverLeavesSampledAncestorTreeInvalid() throws Exception {
        Randomizer.setSeed(127);

        Tree tree = buildSampledAncestorTree();
        RealScalarParam<PositiveReal> rate =
                new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
        rate.setID("rate");

        IntervalScaleOperator operator = new IntervalScaleOperator();
        operator.initByName("weight", "1", "tree", tree, "down", rate,
                "scaleFactor", 0.5, "optimise", false);
        registerInState(operator, tree, rate);

        assertEveryProposalLeavesTreeValid(operator, tree);
    }

    @Test
    public void scaleTreeOperatorNeverLeavesSampledAncestorTreeInvalid() throws Exception {
        Randomizer.setSeed(127);

        Tree tree = buildSampledAncestorTree();

        ScaleTreeOperator operator = new ScaleTreeOperator();
        operator.initByName("weight", "1", "tree", tree,
                "scaleFactor", 0.5, "optimise", false);
        registerInState(operator, tree);

        assertEveryProposalLeavesTreeValid(operator, tree);
    }

    /**
     * Draw many proposals and require that the tree is valid after each one,
     * whether the proposal was accepted or rejected. A rejected proposal may leave
     * the tree partly scaled (MCMC restores state), so the tree is restored here
     * too before the next draw.
     */
    private void assertEveryProposalLeavesTreeValid(
            beast.base.inference.Operator operator, Tree tree) {

        int proposed = 0;
        for (int i = 0; i < PROPOSALS; i++) {
            double[] before = heightsOf(tree);
            double logHR = operator.proposal();
            if (logHR != Double.NEGATIVE_INFINITY) {
                proposed++;
                assertTreeValid(tree, "after accepted proposal " + i);
            }
            restoreHeights(tree, before);
        }
        assertTrue(proposed > 0,
                "operator rejected every one of " + PROPOSALS + " proposals, so the test "
                + "never exercised a successful scale");
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

    private void assertTreeValid(Tree tree, String when) {
        for (Node n : tree.getNodesAsArray()) {
            if (!n.isRoot() && n.getParent().getHeight() < n.getHeight()) {
                fail("Invalid tree " + when + ": node " + n.getNr() + " at height "
                        + n.getHeight() + " is above its parent " + n.getParent().getNr()
                        + " at height " + n.getParent().getHeight());
            }
        }
    }

    /** Operators need their StateNodes owned by a State before proposal() works. */
    private void registerInState(beast.base.inference.Operator operator,
                                 beast.base.inference.StateNode... nodes) throws Exception {
        State state = new State();
        state.initByName("stateNode", java.util.Arrays.asList(nodes));
        state.initialise();
        state.setPosterior(new beast.base.inference.CompoundDistribution());
    }

    /**
     * Sampled-ancestor tree with a tight ceiling:
     * <pre>
     *   root (fake, h=5)
     *    ├── SA (leaf, h=5)      direct ancestor, pins the root
     *    └── X  (h=3)
     *         ├── A (h=0)
     *         └── B (h=1)
     * </pre>
     * X's margin is 2, so a scale of s puts X at 2s + 1; anything from s = 2 up
     * would push X through the pinned root at 5.
     */
    private Tree buildSampledAncestorTree() {
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
}
