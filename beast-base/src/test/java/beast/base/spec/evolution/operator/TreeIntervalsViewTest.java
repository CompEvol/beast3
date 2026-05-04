package beast.base.spec.evolution.operator;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;


public class TreeIntervalsViewTest {

    private TreeIntervalsView buildView(String newick) {
        Tree tree = new TreeParser(newick, false, false, true, 1);
        TreeIntervalsView v = new TreeIntervalsView();
        v.initByName("tree", tree);
        return v;
    }

    @Test
    public void testDimensionAndOrdering() {
        // ultrametric: X=(A,B) at height 1.0, root at height 3.0
        TreeIntervalsView v = buildView("((A:1.0,B:1.0):2.0,C:3.0):0.0;");

        assertEquals(2, v.getDimension());
        // children-before-parents: rank 0 = youngest internal node
        assertEquals(1.0, v.get(0), 1e-12);
        assertEquals(2.0, v.get(1), 1e-12);
    }

    @Test
    public void testIdentityWriteRoundTrip() {
        TreeIntervalsView v = buildView("((A:1.0,B:1.0):2.0,C:3.0):0.0;");
        int n = v.getDimension();

        double[] before = new double[n];
        for (int i = 0; i < n; i++) before[i] = v.get(i);

        for (int i = 0; i < n; i++) v.setMargin(i, before[i]);

        // refresh snapshot
        v.get(0);
        for (int i = 0; i < n; i++) {
            assertEquals(before[i], v.get(i), 1e-12);
        }
        assertEquals(3.0, v.getTree().getRoot().getHeight(), 1e-12);
    }

    @Test
    public void testScaleAllMarginsKeepsTreeValid() {
        TreeIntervalsView v = buildView("((A:1.0,B:1.0):2.0,C:3.0):0.0;");
        int n = v.getDimension();

        double[] before = new double[n];
        for (int i = 0; i < n; i++) before[i] = v.get(i);

        // Write in age-rank order; children update before parents.
        for (int i = 0; i < n; i++) v.setMargin(i, 2.0 * before[i]);

        v.get(0); // refresh ordering after writes
        for (int i = 0; i < n; i++) {
            assertEquals(2.0 * before[i], v.get(i), 1e-12);
        }
        assertEquals(6.0, v.getTree().getRoot().getHeight(), 1e-12);
    }

    @Test
    public void testReadBatchSnapshotIsStable() {
        // After a get(0) anchors the order, subsequent gets follow the
        // same snapshot even if heights mutate from outside the view.
        TreeIntervalsView v = buildView("((A:1.0,B:1.0):2.0,C:3.0):0.0;");
        int n = v.getDimension();

        double m0 = v.get(0); // anchors order
        // mutate the tree underneath the view via a setMargin
        v.setMargin(0, 0.25);
        // reading get(1) should still refer to the parent slot in the
        // same snapshot — i.e. the root margin, computed against the
        // freshly-updated child height.
        Tree tree = v.getTree();
        double rootMargin = tree.getRoot().getHeight()
                - Math.max(tree.getRoot().getLeft().getHeight(),
                           tree.getRoot().getRight().getHeight());
        assertEquals(rootMargin, v.get(1), 1e-12);

        // sanity: m0 was the original youngest margin (1.0)
        assertEquals(1.0, m0, 1e-12);
        // and idx 0 read without rebuild in this batch... but get(0)
        // would re-anchor — so call getMargin directly to bypass.
        assertEquals(0.25, v.getMargin(0), 1e-12);

        // tree must still satisfy parent>child everywhere
        for (var node : tree.getNodesAsArray()) {
            if (!node.isLeaf()) {
                double childMax = Math.max(node.getLeft().getHeight(),
                                           node.getRight().getHeight());
                assertTrue(node.getHeight() >= childMax,
                           "parent height must be >= max child height");
            }
        }
    }
}
