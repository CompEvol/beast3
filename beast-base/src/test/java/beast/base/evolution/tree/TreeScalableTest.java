package beast.base.evolution.tree;

import beast.base.inference.ScalableContractTest;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link beast.base.inference.Scalable} contract on {@link Tree},
 * including heterochronous (serially-sampled) trees that the previous
 * affine {@code Tree.scale} implementation could not handle for small scale
 * factors.
 *
 * <p>The contract requires:</p>
 * <ul>
 *   <li>{@code scale(s)} followed by {@code getScalableValue()} returns
 *       {@code s × original}.</li>
 *   <li>{@code setScalableValue(V)} lands at exactly {@code V}.</li>
 *   <li>{@code setScalableValue(get × s)} produces the same state as
 *       {@code scale(s)}.</li>
 * </ul>
 *
 * <p>Under the new interval-style {@code Tree.scale}, the contract holds for
 * any positive scale factor on a tree without sampled ancestors (ultrametric or
 * heterochronous), and the move never throws.</p>
 *
 * <p>Sampled-ancestor trees are the exception. A fake node is pinned to its
 * direct-ancestor leaf's sampling time and caps the subtree below it, so a large
 * enough scale factor has nowhere valid to land and is rejected by throwing.
 * The contract invariants apply to the moves that do not throw; see
 * {@link Tree#scale(double)}.</p>
 */
public class TreeScalableTest {

    @Test
    void contractHoldsForUltrametricTree() {
        ScalableContractTest.assertContractAcrossScales(
                () -> buildUltrametric(),
                this::assertSameTreeState
        );
    }

    @Test
    void contractHoldsForHeterochronousTree() {
        ScalableContractTest.assertContractAcrossScales(
                () -> buildHeterochronous(),
                this::assertSameTreeState
        );
    }

    @Test
    void contractHoldsForLeafIntrudingTopology() {
        // 4-tip topology where the always-taller-child path doesn't reach the
        // oldest leaf. Under the old affine Tree.scale, scaling by s < 0.5
        // would either throw or produce wrong root height.
        ScalableContractTest.assertContractAcrossScales(
                () -> buildLeafIntruding(),
                this::assertSameTreeState
        );
    }

    @Test
    void scaleNeverThrowsForHeterochronousSmallScales() {
        // Path B fix: the new interval-scale Tree.scale should succeed for any
        // positive s on a heterochronous tree, including very small s where the
        // old affine implementation threw.
        for (double s : new double[] { 0.001, 0.01, 0.1, 0.5, 0.9, 1.1, 10.0 }) {
            Tree tree = buildHeterochronous();
            // should not throw
            tree.scale(s);
            // tree should still be valid: all parents above their children
            for (Node n : tree.getNodesAsArray()) {
                if (!n.isLeaf()) {
                    assertTrue(n.getHeight() >= n.getLeft().getHeight(),
                            "Parent below left child after scale(" + s + ")");
                    if (n.getRight() != null) {
                        assertTrue(n.getHeight() >= n.getRight().getHeight(),
                                "Parent below right child after scale(" + s + ")");
                    }
                }
            }
        }
    }

    @Test
    void sumIntervalsIsExactlyScaleEquivariant() {
        // Spot-check: getScalableValue scales by EXACTLY s under interval scaling.
        Tree tree = buildHeterochronous();
        double v0 = tree.getScalableValue();
        tree.scale(1.7);
        assertEquals(1.7 * v0, tree.getScalableValue(), Math.abs(1.7 * v0) * 1e-12);
    }

    @Test
    void contractHoldsForSampledAncestorTreeWithHeadroom() {
        // Fake root sits far above the subtree below it, so every scale factor
        // the contract helper tries stays under the ceiling.
        ScalableContractTest.assertContractAcrossScales(
                () -> buildSampledAncestorRoomy(),
                this::assertSameTreeState
        );
    }

    @Test
    void contractHoldsForInternalSampledAncestorWithHeadroom() {
        // Same, but with the fake node partway down the tree rather than at the root.
        ScalableContractTest.assertContractAcrossScales(
                () -> buildInternalSampledAncestorRoomy(),
                this::assertSameTreeState
        );
    }

    @Test
    void scalePreservesSampledAncestorInvariants() {
        // A scale that fits: the fake node must not move (its height is the SA's
        // sampling time), and its child must stay strictly below it.
        for (double s : new double[] { 0.1, 0.5, 0.9, 1.1, 1.9 }) {
            Tree tree = buildSampledAncestorTight();
            tree.scale(s);
            for (Node n : tree.getNodesAsArray()) {
                if (n.isFake()) {
                    assertEquals(5.0, n.getHeight(), 1e-12,
                            "Fake node height must stay pinned under scale(" + s + ")");
                    Node child = n.getLeft().isDirectAncestor() ? n.getRight() : n.getLeft();
                    assertTrue(child.getHeight() < n.getHeight(),
                            "Child of fake node reached its parent under scale(" + s + ")");
                }
            }
        }
    }

    @Test
    void scaleThrowsWhenSampledAncestorCeilingIsBreached() {
        // buildSampledAncestorTight: child of the fake root ends at 2s + 1, and the
        // fake root is pinned at 5, so anything from s = 2.0 up has nowhere to land.
        for (double s : new double[] { 2.0, 2.5, 10.0 }) {
            Tree tree = buildSampledAncestorTight();
            assertThrows(IllegalArgumentException.class, () -> tree.scale(s),
                    "scale(" + s + ") should be rejected: it lifts the fake root's child "
                    + "to " + (2 * s + 1) + ", at or above the pinned height 5.0");
        }
    }

    @Test
    void rejectedScaleNeverLeavesTheSampledAncestorInvariantBroken() {
        // The breach is caught during the traversal, so a rejected scale can leave
        // heights below the fake node already scaled -- callers recover by restoring
        // state. What must never happen is the throw being skipped and an invalid
        // tree (child at or above its pinned sampled ancestor) surviving the call.
        Tree tree = buildSampledAncestorTight();
        assertThrows(IllegalArgumentException.class, () -> tree.scale(3.0));

        for (Node n : tree.getNodesAsArray()) {
            if (n.isFake()) {
                assertEquals(5.0, n.getHeight(), 1e-12,
                        "Fake node height must stay pinned even on a rejected scale");
            }
        }
    }

    @Test
    void everyAcceptedScaleHasAnAvailableReverseMove() {
        // Detailed balance needs the reverse of any move we can make to be a move we
        // could also have made. Rejecting proposals that breach a sampled-ancestor
        // ceiling is only sound if it never strands the chain somewhere it cannot
        // scale back from: whenever scale(s) succeeds, scale(1/s) must succeed too,
        // land back on the original heights, and contribute the opposite Jacobian.
        for (double s : new double[] { 0.01, 0.5, 0.9, 1.1, 1.5, 1.9, 1.99 }) {
            Tree tree = buildSampledAncestorTight();
            double[] original = new double[tree.getNodesAsArray().length];
            for (int i = 0; i < original.length; i++) {
                original[i] = tree.getNodesAsArray()[i].getHeight();
            }

            double forward = tree.scale(s);
            double reverse = tree.scale(1.0 / s);

            assertEquals(0.0, forward + reverse, 1e-9,
                    "log Jacobians of scale(" + s + ") and its reverse should cancel, so "
                    + "the same number of intervals must be scaled in both directions");

            Node[] nodes = tree.getNodesAsArray();
            for (int i = 0; i < nodes.length; i++) {
                assertEquals(original[i], nodes[i].getHeight(),
                        Math.abs(original[i]) * 1e-9 + 1e-9,
                        "scale(" + s + ") then scale(1/" + s + ") should return node " + i
                        + " to its original height");
            }
        }
    }

    @Test
    void rejectedScaleFactorsAreRejectedFromBothEnds() {
        // The flip side: a scale factor that is rejected must be rejected wherever the
        // chain sits, not silently accepted from a state it could not have reached.
        // Here s = 2.5 breaches the ceiling from the starting state; after shrinking,
        // the same s becomes legitimately available -- and the state it reaches is one
        // scale(1/2.5) can leave again.
        Tree tree = buildSampledAncestorTight();
        assertThrows(IllegalArgumentException.class, () -> tree.scale(2.5));

        Tree shrunk = buildSampledAncestorTight();
        shrunk.scale(0.25);
        double forward = shrunk.scale(2.5);           // now fits under the ceiling
        double reverse = shrunk.scale(1.0 / 2.5);     // and is reversible
        assertEquals(0.0, forward + reverse, 1e-9,
                "a scale that fits must still be exactly reversible");
    }

    @Test
    void sampledAncestorDirectlyAboveALeafNeverRejects() {
        // A sampled ancestor whose other child is a plain leaf: nothing under the
        // fake node can move, so no scale factor can breach its ceiling.
        for (double s : new double[] { 0.5, 1.1, 5.0, 100.0 }) {
            Tree tree = buildSampledAncestorOverLeaf();
            tree.scale(s);  // should not throw
        }
    }

    @Test
    void shrinkingNeverBreachesSampledAncestorCeiling() {
        // Scaling down only lowers the subtree, so it can never hit the ceiling
        // no matter how tight the tree is.
        for (double s : new double[] { 1e-6, 0.001, 0.5, 0.999 }) {
            Tree tree = buildSampledAncestorTight();
            tree.scale(s);  // should not throw
        }
    }

    @Test
    void zeroLengthTipBranchIsTreatedAsASampledAncestorRegardlessOfTreePrior() {
        // isDirectAncestor()/isFake() are decided purely by height equality and never
        // consult the tree prior, so a zero-length tip branch reaching the tree any
        // other way -- a starting Newick, a tip-date move -- gets the sampled-ancestor
        // treatment too. Documenting that here because it decides what the check below
        // does outside an FBD analysis.
        Tree tree = buildZeroLengthTipBranch();
        Node p = tree.getNodesAsArray()[5];
        assertTrue(p.isFake(),
                "a node whose leaf child sits at its own height is 'fake' by height alone");

        // Its height is pinned exactly as a sampled ancestor's would be, so it caps the
        // subtree below it: X is at 3 with margin 2, so scale(s) puts X at 2s + 1 and
        // anything from s = 1.5 up would push X through the pinned 4.
        for (double s : new double[] { 1.5, 2.0, 5.0 }) {
            Tree t = buildZeroLengthTipBranch();
            assertThrows(IllegalArgumentException.class, () -> t.scale(s),
                    "scale(" + s + ") should be rejected rather than silently producing "
                    + "a child above its parent");
        }

        // Below the cap it scales normally and stays valid.
        for (double s : new double[] { 0.1, 0.9, 1.4 }) {
            Tree t = buildZeroLengthTipBranch();
            t.scale(s);
            assertTreeValid(t, "after scale(" + s + ") on a zero-length tip branch");
        }
    }

    @Test
    void zeroLengthInternalBranchStaysZeroAndValid() {
        // A zero-length branch between two INTERNAL nodes is not "fake" (isDirectAncestor
        // requires a leaf), so no ceiling applies. It is still frozen: interval scaling
        // multiplies margins, and a zero margin is absorbing -- s x 0 = 0 for any s. The
        // branch cannot be reopened by scaling, only by a topology or node-height move.
        for (double s : new double[] { 0.5, 1.0, 2.0, 10.0 }) {
            Tree tree = buildZeroLengthInternalBranch();
            Node p = tree.getNodesAsArray()[5];
            Node y = tree.getNodesAsArray()[4];
            assertTrue(!p.isFake(), "an internal zero-length child is not a sampled ancestor");

            tree.scale(s);   // must not throw

            assertEquals(0.0, p.getHeight() - y.getHeight(), 1e-12,
                    "a zero margin stays zero under scale(" + s + ")");
            assertTreeValid(tree, "after scale(" + s + ") on a zero-length internal branch");
        }
    }

    @Test
    void aNodeHeightOperatorEscapesBothZeroLengthStates() {
        // Scale and up-down are MULTIPLICATIVE, so a zero margin is a fixed point:
        // s x 0 = 0 forever. A node-height operator is ADDITIVE -- it draws a height
        // uniformly between a node's taller child and its parent -- so it steps off
        // the zero-length state on the first proposal that touches the node. Under any
        // continuous tree prior that state has measure zero, so nothing puts it back.
        //
        // Both pathologies are therefore transient once such an operator is present:
        // a fake node caused by a zero-length tip branch has only "up" available (its
        // taller child sits at its own height), and a zero-length internal branch has
        // "down" available. Either way the branch opens.
        // node 3 is leaf D, sitting at its parent P's height -- the zero-length branch
        assertEscapesZeroLength(buildZeroLengthTipBranch(), 3,
                "zero-length tip branch (fake node)");
        // node 4 is internal Y, sitting at its parent P's height
        assertEscapesZeroLength(buildZeroLengthInternalBranch(), 4,
                "zero-length internal branch");
    }

    private void assertEscapesZeroLength(Tree tree, int nodeNr, String what) {
        Randomizer.setSeed(127);
        beast.base.evolution.operator.Uniform operator =
                new beast.base.evolution.operator.Uniform();
        try {
            operator.initByName("weight", "1", "tree", tree);
        } catch (Exception e) {
            throw new AssertionError("could not set up Uniform operator", e);
        }

        assertEquals(0.0, tree.getNode(nodeNr).getLength(), 1e-12,
                what + ": fixture should start with a zero-length branch");

        boolean escaped = false;
        for (int i = 0; i < 200 && !escaped; i++) {
            operator.proposal();
            assertTreeValid(tree, what + ": after Uniform proposal " + i);
            if (tree.getNode(nodeNr).getLength() > 0.0) {
                escaped = true;
            }
        }
        assertTrue(escaped,
                what + ": a node-height operator should open the zero-length branch");

        // and once open, scaling works on it again -- the margin is no longer absorbing
        double before = tree.getScalableValue();
        tree.scale(1.5);
        assertEquals(1.5 * before, tree.getScalableValue(),
                Math.abs(1.5 * before) * 1e-9 + 1e-9,
                what + ": scaling should be well behaved once the branch is open");
    }

    private void assertTreeValid(Tree tree, String when) {
        for (Node n : tree.getNodesAsArray()) {
            if (!n.isRoot() && n.getParent().getHeight() < n.getHeight()) {
                throw new AssertionError("Invalid tree " + when + ": node " + n.getNr()
                        + " at " + n.getHeight() + " is above parent " + n.getParent().getNr()
                        + " at " + n.getParent().getHeight());
            }
        }
    }

    /**
     * No sampled ancestors intended, but leaf D sits exactly at its parent's height:
     * <pre>
     *   root (h=10)
     *    ├── C (leaf, h=0)
     *    └── P (h=4)          &lt;- "fake" by height alone
     *         ├── D (leaf, h=4)
     *         └── X (h=3)
     *              ├── A (h=0)
     *              └── B (h=1)
     * </pre>
     */
    private Tree buildZeroLengthTipBranch() {
        Node a = leaf("A", 0, 0.0);
        Node b = leaf("B", 1, 1.0);
        Node c = leaf("C", 2, 0.0);
        Node d = leaf("D", 3, 4.0);
        Node x = internal(4, 3.0, a, b);
        Node p = internal(5, 4.0, d, x);
        Node root = internal(6, 10.0, c, p);
        return new Tree(root);
    }

    /**
     * Zero-length branch between two internal nodes, which is not a sampled ancestor:
     * <pre>
     *   root (h=10)
     *    ├── C (leaf, h=0)
     *    └── P (h=4)
     *         ├── Y (h=4)     &lt;- internal, same height as P
     *         │    ├── A (h=0)
     *         │    └── B (h=1)
     *         └── D (leaf, h=2)
     * </pre>
     */
    private Tree buildZeroLengthInternalBranch() {
        Node a = leaf("A", 0, 0.0);
        Node b = leaf("B", 1, 1.0);
        Node c = leaf("C", 2, 0.0);
        Node d = leaf("D", 3, 2.0);
        Node y = internal(4, 4.0, a, b);
        Node p = internal(5, 4.0, y, d);
        Node root = internal(6, 10.0, c, p);
        return new Tree(root);
    }

    /** Ultrametric: leaves A, B, C all at height 0; internal P at 1; root at 2. */
    private Tree buildUltrametric() {
        Node a = leaf("A", 0, 0.0);
        Node b = leaf("B", 1, 0.0);
        Node c = leaf("C", 2, 0.0);
        Node p = internal(3, 1.0, a, b);
        Node root = internal(4, 2.0, p, c);
        return new Tree(root);
    }

    /** Heterochronous: A=0, B=2, C=1, P at 4, root at 5. */
    private Tree buildHeterochronous() {
        Node a = leaf("A", 0, 0.0);
        Node b = leaf("B", 1, 2.0);
        Node c = leaf("C", 2, 1.0);
        Node p = internal(3, 4.0, a, b);
        Node root = internal(4, 5.0, p, c);
        return new Tree(root);
    }

    /**
     * 4-tip leaf-intrusion topology: A=0, B=0, C=1, D=2.
     * Topology is (D, ((B, C), A)). The always-taller-child path from root
     * leads through ((B,C), A) to (B,C) to C — bypassing D, which is the
     * oldest leaf. Under the old affine Tree.scale, scaling by s &lt; 2/3
     * causes leaves to violate parent constraints.
     */
    private Tree buildLeafIntruding() {
        Node a = leaf("A", 0, 0.0);
        Node b = leaf("B", 1, 0.0);
        Node c = leaf("C", 2, 1.0);
        Node d = leaf("D", 3, 2.0);
        Node y = internal(4, 2.0, b, c);  // (B, C)
        Node x = internal(5, 3.0, y, a);  // ((B, C), A)
        Node root = internal(6, 4.0, d, x);
        return new Tree(root);
    }

    /**
     * Sampled-ancestor tree with a fake root and plenty of headroom:
     * <pre>
     *   root (fake, h=100)
     *    ├── SA   (leaf, h=100)   direct ancestor: same height as its parent
     *    └── X    (h=6)
     *         ├── Y (h=3)
     *         │    ├── A (h=0)
     *         │    └── B (h=1)
     *         └── C (h=2)
     * </pre>
     * Margins: Y = 3-1 = 2, X = 6-3 = 3; sum = 5. The fake root contributes none.
     * Even scale(3.7) lands X at 19.5, well below the pinned 100.
     */
    private Tree buildSampledAncestorRoomy() {
        Node a = leaf("A", 0, 0.0);
        Node b = leaf("B", 1, 1.0);
        Node c = leaf("C", 2, 2.0);
        Node sa = leaf("SA", 3, 100.0);
        Node y = internal(4, 3.0, a, b);
        Node x = internal(5, 6.0, y, c);
        Node root = internal(6, 100.0, sa, x);
        return new Tree(root);
    }

    /**
     * Sampled-ancestor tree with the fake node partway down rather than at the root:
     * <pre>
     *   root (h=200)
     *    ├── C  (leaf, h=0)
     *    └── F  (fake, h=100)
     *         ├── SA (leaf, h=100)
     *         └── X  (h=3)
     *              ├── A (h=0)
     *              └── B (h=1)
     * </pre>
     * Margins: X = 3-1 = 2, root = 200-100 = 100; sum = 102. F contributes none.
     */
    private Tree buildInternalSampledAncestorRoomy() {
        Node a = leaf("A", 0, 0.0);
        Node b = leaf("B", 1, 1.0);
        Node c = leaf("C", 2, 0.0);
        Node sa = leaf("SA", 3, 100.0);
        Node x = internal(4, 3.0, a, b);
        Node f = internal(5, 100.0, sa, x);
        Node root = internal(6, 200.0, c, f);
        return new Tree(root);
    }

    /**
     * Sampled-ancestor tree with a tight ceiling, for exercising rejection:
     * <pre>
     *   root (fake, h=5)
     *    ├── SA (leaf, h=5)
     *    └── X  (h=3)
     *         ├── A (h=0)
     *         └── B (h=1)
     * </pre>
     * X's margin is 3-1 = 2, so scale(s) puts X at 2s + 1. The pinned root is at
     * 5, so s &lt; 2 fits and s &ge; 2 has nowhere to land.
     */
    private Tree buildSampledAncestorTight() {
        Node a = leaf("A", 0, 0.0);
        Node b = leaf("B", 1, 1.0);
        Node sa = leaf("SA", 2, 5.0);
        Node x = internal(3, 3.0, a, b);
        Node root = internal(4, 5.0, sa, x);
        return new Tree(root);
    }

    /**
     * Sampled ancestor sitting directly above a single tip:
     * <pre>
     *   root (h=10)
     *    ├── C  (leaf, h=0)
     *    └── F  (fake, h=5)
     *         ├── SA (leaf, h=5)
     *         └── D  (leaf, h=2)
     * </pre>
     * Nothing below F can move, so F's ceiling can never be breached.
     */
    private Tree buildSampledAncestorOverLeaf() {
        Node c = leaf("C", 0, 0.0);
        Node d = leaf("D", 1, 2.0);
        Node sa = leaf("SA", 2, 5.0);
        Node f = internal(3, 5.0, sa, d);
        Node root = internal(4, 10.0, c, f);
        return new Tree(root);
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

    /** Compare two Trees node-for-node by height. */
    private void assertSameTreeState(Tree a, Tree b) {
        Node[] na = a.getNodesAsArray();
        Node[] nb = b.getNodesAsArray();
        assertEquals(na.length, nb.length, "Trees should have same number of nodes");
        for (int i = 0; i < na.length; i++) {
            assertEquals(na[i].getHeight(), nb[i].getHeight(),
                    Math.abs(na[i].getHeight()) * 1e-9 + 1e-9,
                    "Node " + i + " (id=" + na[i].getID() + ") height should match across paths");
        }
    }
}
