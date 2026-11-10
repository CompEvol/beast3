package beast.base.evolution.tree;



import java.io.PrintStream;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.core.Loggable;
import beast.base.core.Input.Validate;
import beast.base.inference.CalculationNode;


/**
 * Kept so that BEAST 2 XML that logs the tree height with TreeHeightLogger still loads. Its output
 * is unchanged: one column, named after the tree ("Tree.t:x.height") unless the logger has an id.
 * New XML should use {@link TreeStatLogger}, which BEAUti writes. To be removed in 3.0.0.
 */
@Description("Logger to report height of a tree -- deprecated: use TreeStatLogger instead")
@Deprecated(forRemoval = true)
public class TreeHeightLogger extends CalculationNode implements Loggable, Function {
    final public Input<Tree> treeInput = new Input<>("tree", "tree to report height for.", Validate.REQUIRED);
    
    @Override
    public void initAndValidate() {
        Log.warning("TreeHeightLogger is deprecated and will be removed in BEAST 3.0.0: use TreeStatLogger instead.");
    }

    @Override
    public void init(PrintStream out) {
        final Tree tree = treeInput.get();
        if (getID() == null || getID().matches("\\s*")) {
            out.print(tree.getID() + ".height\t");
        } else {
            out.print(getID() + "\t");
        }
    }

    @Override
    public void log(long sample, PrintStream out) {
        final Tree tree = treeInput.get();
        out.print(tree.getRoot().getHeight() + "\t");
    }

	@Override
    public void close(PrintStream out) {
        // nothing to do
    }

    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public double getArrayValue() {
        return treeInput.get().getRoot().getHeight();
    }

    @Override
    public double getArrayValue(int dim) {
        return treeInput.get().getRoot().getHeight();
    }
}
