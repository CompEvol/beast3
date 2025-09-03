package phylospec.base.inference;

import beast.base.inference.StateNode;
import org.phylospec.primitives.Real;
import org.phylospec.types.Scalar;
import org.w3c.dom.Node;
import phylospec.base.core.TensorInput;

import java.io.PrintStream;

/**
 * It is extendable to more types.
 * @param <P> any types belong to {@link Real}
 */
public abstract class ScalarParam<P extends Real> extends StateNode implements Scalar<P> {

    // TODO how to update desc
    final public TensorInput<Scalar<P>> lowerValueInput = new TensorInput<>(
            "lower","lower value for this parameter (default -infinity)");
    final public TensorInput<Scalar<P>> upperValueInput = new TensorInput<>(
            "upper","upper value for this parameter (default +infinity)");

    protected double value;
    protected double storedValue;
    protected Double upper;
    protected Double lower;
    protected boolean isDirty;

    public ScalarParam(Double startValue) {
//        this.valueInput = new TensorInput<>(
//                "value",
//                "start value for this scalar parameter",
//                new ScalarParam<>(startValue, primitive),  // start value
//                Input.Validate.REQUIRED,
//                primitive
//        );
//        this.primitive = real;
        this.value = startValue;
        this.storedValue = startValue;
        if (!primitiveType().isValid(startValue))
            throw new IllegalArgumentException("start value " + startValue + " is invalid ! ");
        isDirty = false;

//        initByName("value", value, "lower", Double.NEGATIVE_INFINITY,
//                "upper", Double.POSITIVE_INFINITY);
    }

    public ScalarParam() {
        //TODO
    }

    @Override
    public void initAndValidate() {
        if (lowerValueInput.get() != null) {
            lower = lowerValueInput.get().get();
            //TODO need safe way to setPrimitiveType
            lowerValueInput.setPrimitiveType(primitiveType());

            if (!primitiveType().isValid(lower))
                throw new IllegalArgumentException("lower value " + lower + " is invalid ! ");
        } else {
            lower = Double.NEGATIVE_INFINITY;
        }
        if (upperValueInput.get() != null) {
            upper = upperValueInput.get().get();
            //TODO need safe way to setPrimitiveType
            upperValueInput.setPrimitiveType(primitiveType());

            if (!primitiveType().isValid(lower))
                throw new IllegalArgumentException("upper value " + upper + " is invalid ! ");
        } else {
            upper = Double.POSITIVE_INFINITY;
        }

        if (!isInBound())
            throw new IllegalArgumentException("Value " + value + " should be within [" + lower + ", " + upper + "]");
    }

    public boolean isInBound() {
        return value >= lower && value <= upper;
    }

    @Override
    public double get() {
        return value;
    }

    @Override
    public Double get(int... idx) {
        return value;
    }

    public Double getUpper() {
        return upper;
    }

    public Double getLower() {
        return lower;
    }

    @Override
    public void setEverythingDirty(boolean isDirty) {

    }

    @Override
    public StateNode copy() {
        return null;
    }

    @Override
    public void assignTo(StateNode other) {

    }

    @Override
    public void assignFrom(StateNode other) {

    }

    @Override
    public void assignFromFragile(StateNode other) {

    }

    @Override
    public void fromXML(Node node) {

    }

    @Override
    public int scale(double scale) {
        return 0;
    }

    @Override
    protected void store() {
        storedValue = value;
    }

    @Override
    public void restore() {
        final double tmp = storedValue;
        storedValue = value;
        value = tmp;
        hasStartedEditing = false;
        isDirty = false;
    }

    @Override
    public int getDimension() {
        return rank();
    }

    //TODO need to fix StateNode, may inherit NumberTensor?
    @Override
    public double getArrayValue(int dim) {
        return 0;
    }

    @Override
    public void init(PrintStream out) {

    }

    @Override
    public void log(long sample, PrintStream out) {

    }

    @Override
    public void close(PrintStream out) {

    }
}
