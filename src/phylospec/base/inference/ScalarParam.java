package phylospec.base.inference;

import beast.base.inference.StateNode;
import org.phylospec.primitives.Primitive;
import org.phylospec.primitives.Real;
import org.phylospec.types.Scalar;
import org.w3c.dom.Node;
import phylospec.base.core.Input;
import phylospec.base.core.PrimitiveInput;

import java.io.PrintStream;

public class ScalarParam<P extends Real> extends StateNode implements Scalar<P> {

    final public PrimitiveInput<Double, Scalar<Real>> valueInput = new PrimitiveInput<>(
            "value","start value for this scalar parameter",
            0.0, Input.Validate.REQUIRED, Real.INSTANCE);

    final public PrimitiveInput<Double, Scalar<Real>> lowerValueInput = new PrimitiveInput<>(
            "lower","lower value for this parameter (default -infinity)",
            Double.NEGATIVE_INFINITY, Real.INSTANCE);
    final public PrimitiveInput<Double, Scalar<Real>> upperValueInput = new PrimitiveInput<>(
            "upper","upper value for this parameter (default +infinity)",
            Double.POSITIVE_INFINITY, Real.INSTANCE);


    //++++++++ Scalar ++++++++

    protected double value;
    protected double storedValue;

    protected Double upper;
    protected Double lower;

    protected boolean isDirty;

    /**
     * Constructs a Boolean with the given value.
     *
     * @param value the boolean value
     */
    public ScalarParam(double value) {
        initByName("value", value);
    }

    @Override
    public void initAndValidate() {
        this.value = valueInput.getJValue();
        this.lower = lowerValueInput.getJValue();
        this.upper = upperValueInput.getJValue();

        if (!isValid())
            throw new IllegalArgumentException( "..., but was: " + value);
        if (!isValidBound())
            throw new IllegalArgumentException("Value " + value + " should be within [" + lower + ", " + upper + "]");
    }

    public boolean isValidBound() {
        return this.lower.compareTo(value) <= 0 && this.upper.compareTo(value) >= 0;
    }

    @Override
    public double get() {
        return value;
    }

    @Override
    public Double get(int... idx) {
        return value;
    }

    @Override
    public Primitive primitiveType() {
        return Real.INSTANCE;
    }

    public Double getUpper() {
        return upper;
    }

    public Double getLower() {
        return lower;
    }


    //++++++++ StateNode ++++++++

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
