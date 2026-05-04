package beast.base.spec.evolution.operator;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.inference.OperatorSchedule;
import beast.base.util.Randomizer;


public class CladeIntervalScaleOperatorTest {

    private Tree tree;
    private CladeIntervalScaleOperator op;

    @BeforeEach
    public void setUp() {
        Randomizer.setSeed(42);
        tree = new TreeParser("((A:1.0,B:1.0):2.0,C:3.0):0.0;", false, false, true, 1);
        op = new CladeIntervalScaleOperator();
        op.initByName("tree", tree, "scaleFactor", 0.5, "weight", 1.0);
        OperatorSchedule schedule = new OperatorSchedule();
        schedule.initAndValidate();
        schedule.addOperator(op);
    }

    @Test
    public void testProposalKeepsAllOtherMarginsConstant() {
        int n = tree.getInternalNodeCount();
        java.util.Map<Integer, Double> oldM = new java.util.HashMap<>();
        for (var node : tree.getNodesAsArray()) {
            if (node != null && !node.isLeaf() && !node.isFake()) {
                double childMax = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
                oldM.put(node.getNr(), node.getHeight() - childMax);
            }
        }

        double logHR = op.proposal();
        assertTrue(Double.isFinite(logHR));

        int changedMargins = 0;
        for (var node : tree.getNodesAsArray()) {
            if (node != null && !node.isLeaf() && !node.isFake()) {
                double childMax = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
                double newMargin = node.getHeight() - childMax;
                if (Math.abs(newMargin - oldM.get(node.getNr())) > 1e-10) changedMargins++;
            }
        }
        assertEquals(1, changedMargins, "exactly one margin should change per proposal");
    }

    @Test
    public void testHeightsRemainBelowParentAfterManyProposals() {
        for (int i = 0; i < 1000; i++) {
            op.proposal();
            for (var node : tree.getNodesAsArray()) {
                if (!node.isRoot()) {
                    assertTrue(node.getHeight() < node.getParent().getHeight(),
                            "child height >= parent height after proposal");
                }
            }
        }
    }

    @Test
    public void testEachInternalNodeHasItsOwnCladeEntry() {
        // 200 proposals on a 3-leaf tree: 2 internal nodes (clades {A,B} and
        // {A,B,C}). Both should appear in the learned clade map.
        for (int i = 0; i < 200; i++) op.proposal();
        var m = op.getCladeScaleFactors();
        assertTrue(m.size() >= 2, "expected at least 2 distinct clade entries, got " + m.size());

        // The full-tree clade is the bitset {0, 1, 2}; verify it's present.
        BitSet full = new BitSet();
        full.set(0); full.set(1); full.set(2);
        assertTrue(m.containsKey(full), "root clade {A,B,C} should be present");
    }

    @Test
    public void testCladeKeyIsTopologyInvariant() {
        // The clade {A,B} appears regardless of how we wire (A,B). Even if we
        // "moved" the topology so that node 3 became (A,B) again, the same
        // BitSet would key the same σ entry. We simulate this by computing
        // clades on two trees with different sister but the same {A,B} clade.
        Tree t1 = new TreeParser("((A:1.0,B:1.0):2.0,C:3.0):0.0;", false, false, true, 1);
        Tree t2 = new TreeParser("(C:3.0,(A:1.0,B:1.0):2.0):0.0;", false, false, true, 1);

        BitSet ab1 = cladeOf(t1, "A", "B");
        BitSet ab2 = cladeOf(t2, "A", "B");
        assertEquals(ab1, ab2,
                "clade BitSets must match regardless of subtree placement");
    }

    private static BitSet cladeOf(Tree tree, String... taxa) {
        BitSet b = new BitSet();
        for (String t : taxa) {
            for (var node : tree.getNodesAsArray()) {
                if (node.isLeaf() && node.getID().equals(t)) {
                    b.set(node.getNr());
                    break;
                }
            }
        }
        return b;
    }
}
