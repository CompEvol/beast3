//package beast.base.evolution;
//
//import java.io.PrintStream;
//import java.util.ArrayList;
//import java.util.List;
//
//import beast.base.core.BEASTObject;
//import beast.base.core.Description;
//import beast.base.core.Function;
//import beast.base.core.Input;
//import beast.base.core.Loggable;
//import beast.base.core.Input.Validate;
//import beast.base.evolution.tree.Tree;
//import beast.base.inference.CalculationNode;
//import beast.base.inference.parameter.BooleanParameter;
//
//
//@Description("calculates sum of a valuable")
//public class Sum extends CalculationNode implements Function, Loggable {
//    final public Input<List<Tensor<?>>> functionInput = new Input<>("arg", "argument to be summed", new ArrayList<>(), Validate.REQUIRED);
//
//    final public Input<Tree> treeInput = new Input<>("tree", "the tree corresponding to the function to be summed, indexing by node numbers assumed.", Validate.OPTIONAL);
//
//    final public Input<Boolean> ignoreZeroBranchLengthsInput = new Input<>("ignoreZeroBranchLengths", "true if quantities in the argument should only be summed for non-zero branch lengths.", false, Validate.OPTIONAL);
//
//    enum Mode {integer_mode, double_mode}
//
//    Mode mode;
//
//    boolean needsRecompute = true;
//    double sum = 0;
//    double storedSum = 0;
//
//    Tree tree;
//    boolean ignoreZeroBranchLengths = false;
//
//    @Override
//    public void initAndValidate() {
//        List<Tensor<?>> valuable = functionInput.get();
//        mode = Mode.integer_mode;
//        for (Tensor<?> v : valuable) {
//	        if (!(v instanceof IntegerParameter || v instanceof BooleanParameter)) {
//	            mode = Mode.double_mode;
//	        }
//        }
//        tree = treeInput.get();
//        ignoreZeroBranchLengths = ignoreZeroBranchLengthsInput.get();
//    }
//
//    @Override
//    public int getDimension() {
//        return 1;
//    }
//
//    @Override
//    public double getArrayValue() {
//        if (needsRecompute) {
//            compute();
//        }
//        return sum;
//    }
//
//    /**
//     * do the actual work, and reset flag *
//     */
//    void compute() {
//        sum = 0;
//        if (tree != null && ignoreZeroBranchLengths) {
//            for (Tensor<?> v : functionInput.get()) {
//                for (int i = 0; i < v.size(); i++) {
//                    if (!tree.getNode(i).isDirectAncestor()) {
//                        sum += v.getDoubleValue(i);
//                    }
//                }
//            }
//        } else {
//            for (Tensor<?> v : functionInput.get()) {
//                for (int i = 0; i < v.size(); i++) {
//                	Object o = v.get(i);
//                	if (o instanceof Double x) {
//                		sum += x;
//                	} else if (o instanceof Integer x) {
//                		sum += x;
//                	} else if (o instanceof Boolean x) {
//                		sum += x ? 1 : 0;
//                	}
//                }
//            }
//        }
//        needsRecompute = false;
//    }
//
//    @Override
//    public double getArrayValue(int dim) {
//        if (dim == 0) {
//            return getArrayValue();
//        }
//        return Double.NaN;
//    }
//
//    /**
//     * CalculationNode methods *
//     */
//    @Override
//    public void store() {
//        storedSum = sum;
//        super.store();
//    }
//
//    @Override
//    public void restore() {
//        sum = storedSum;
//        super.restore();
//    }
//
//    @Override
//    public boolean requiresRecalculation() {
//        needsRecompute = true;
//        return true;
//    }
//
//    /**
//     * Loggable interface implementation follows
//     */
//    @Override
//    public void init(PrintStream out) {
//        out.print("sum(" + ((BEASTObject) functionInput.get().get(0)).getID() + ")\t");
//    }
//
//    @Override
//    public void log(long sampleNr, PrintStream out) {
//        double sum = 0;
//        for (Tensor<?> v : functionInput.get()) {
//	        for (int i = 0; i < v.size(); i++) {
//	            sum += v.getDoubleValue(i);
//	        }
//        }
//        if (mode == Mode.integer_mode) {
//            out.print((int) sum + "\t");
//        } else {
//            out.print(sum + "\t");
//        }
//    }
//
//    @Override
//    public void close(PrintStream out) {
//        // nothing to do
//    }
//
//} // class Sum
