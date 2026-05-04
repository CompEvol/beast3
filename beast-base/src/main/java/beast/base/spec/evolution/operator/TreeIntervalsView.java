package beast.base.spec.evolution.operator;


import java.util.ArrayList;
import java.util.List;

import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.spec.domain.Real;
import beast.base.spec.type.Tensor;


/**
 * Exposes a Tree as a vector of interval margins, where the i-th margin is the
 * difference between the i-th internal node's height and the maximum height of
 * its children. Internal nodes are ordered by current height (children before
 * parents), so writing margins in index order keeps child heights up-to-date
 * before each parent is updated. The ordering is rebuilt at the start of every
 * read batch (when index 0 is accessed) and held stable across the matching
 * write batch within the same proposal.
 *
 * Sampled-ancestor / fake nodes are skipped, matching IntervalScaleOperator;
 * trees that contain them are not yet fully supported.
 */
@Description("Vector view of a Tree as its n-1 interval margins (per internal node), indexed by age rank with children before parents.")
public class TreeIntervalsView extends BEASTObject implements Tensor<Real, Double> {

    public final Input<Tree> treeInput = new Input<>("tree", "tree to view as a vector of interval margins", Validate.REQUIRED);

    private Tree tree;
    private Node[] orderedInternal;
    private final int[] shape = new int[1];

    @Override
    public void initAndValidate() {
        this.tree = treeInput.get();
        rebuildOrder();
    }

    private void rebuildOrder() {
        Node[] all = tree.getNodesAsArray();
        List<Node> internals = new ArrayList<>();
        for (Node n : all) {
            if (n != null && !n.isLeaf() && !n.isFake()) {
                internals.add(n);
            }
        }
        internals.sort((a, b) -> Double.compare(a.getHeight(), b.getHeight()));
        orderedInternal = internals.toArray(new Node[0]);
        shape[0] = orderedInternal.length;
    }

    public Tree getTree() {
        return tree;
    }

    public int getDimension() {
        return orderedInternal.length;
    }

    public double getMargin(int rank) {
        Node n = orderedInternal[rank];
        return n.getHeight() - Math.max(n.getLeft().getHeight(), n.getRight().getHeight());
    }

    public void setMargin(int rank, double margin) {
        Node n = orderedInternal[rank];
        double childMax = Math.max(n.getLeft().getHeight(), n.getRight().getHeight());
        n.setHeight(childMax + margin);
    }

    // === Tensor<Real, Double> ===

    @Override
    public Double get(int... idx) {
        if (idx.length != 1) {
            throw new IllegalArgumentException("TreeIntervalsView is rank 1; expected exactly one index");
        }
        // Anchor the order at the start of every read batch. Within a single
        // AVMN proposal, AVMN reads all values starting at idx 0 before any
        // writeback, so rebuilding here keeps reads and writes consistent.
        if (idx[0] == 0) rebuildOrder();
        return getMargin(idx[0]);
    }

    @Override
    public Real getDomain() {
        return Real.INSTANCE;
    }

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public int[] shape() {
        return shape;
    }

    @Override
    public boolean isValid(Double value) {
        return value != null && !Double.isNaN(value) && !Double.isInfinite(value) && value > 0.0;
    }
}
