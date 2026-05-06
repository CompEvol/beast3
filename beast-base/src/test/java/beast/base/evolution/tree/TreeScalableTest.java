package beast.base.evolution.tree;

import beast.base.inference.ScalableContractTest;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * any positive scale factor on any tree shape (ultrametric or heterochronous),
 * and the move never throws.</p>
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
