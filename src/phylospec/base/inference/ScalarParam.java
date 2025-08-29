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

    final public Input<Scalar<Real>> valueInput = new PrimitiveInput<>("value",
            "start value for this scalar parameter", 0.0,
            Input.Validate.REQUIRED, Real.INSTANCE);


    //++++++++ Scalar ++++++++

    private double value;

    /**
     * Constructs a Boolean with the given value.
     *
     * @param value the boolean value
     */
    public ScalarParam(double value) {
        this.value = value;
        if (!isValid()) {
            throw new IllegalArgumentException(
                    "..., but was: " + value);
        }
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

    }

    @Override
    public void restore() {

    }

    @Override
    public void initAndValidate() {

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
