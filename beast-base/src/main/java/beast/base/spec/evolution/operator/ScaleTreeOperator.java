package beast.base.spec.evolution.operator;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.spec.inference.operator.AbstractScale;

/**
 * Scale operator specialised for trees. Scales all divergence times in a tree
 * by the same factor, optionally scaling only the root node.
 */
@Description("Scale operator that scales all divergence times in a tree by the same factor.")
public class ScaleTreeOperator extends AbstractScale {

    public final Input<Tree> treeInput = new Input<>("tree",
            "All beast.tree divergence times are scaled");

    final public Input<Boolean> rootOnlyInput = new Input<>("rootOnly",
            "scale root of a tree only, ignored if tree is not specified (default false)",
            false);

    @Override
    public void initAndValidate() {
        super.initAndValidate();
    }

    @Override
    public double proposal() {
        try {

            final Tree tree = treeInput.get();

            if (rootOnlyInput.get()) {
                final Node root = tree.getRoot();
                final double scale = getScaler(root.getNr(), root.getHeight());
                final double newHeight = root.getHeight() * scale;

                if (newHeight < Math.max(root.getLeft().getHeight(), root.getRight().getHeight())) {
                    return Double.NEGATIVE_INFINITY;
                }
                root.setHeight(newHeight);
                // hastings ratio
                return Math.log(scale);

            } else {

                // scale the beast.tree
                final double scale = getScaler(0, Double.NaN);
                final int scaledNodes = tree.scale(scale);
                // hastings ratio
                return Math.log(scale) * scaledNodes;
            }

        } catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }

    }



}
