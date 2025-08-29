package phylospec.base.inference;

import beast.base.inference.StateNode;
import org.phylospec.primitives.Primitive;
import org.phylospec.primitives.Real;
import org.phylospec.types.Vector;
import org.w3c.dom.Node;
import phylospec.base.core.Input;
import phylospec.base.core.PrimitiveInput;
import phylospec.base.types.RealVectorImpl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VectorParam<P extends Real> extends StateNode implements Vector<P> {

    final public Input<Vector<Real>> valueInput = new PrimitiveInput<>("value",
            "start value for this scalar parameter", new RealVectorImpl<>(), //TODO ? new ArrayList()
            Input.Validate.REQUIRED, Real.INSTANCE);


    //++++++++ Scalar ++++++++

    private List<Double> values;

    /**
     * Constructs a Vector from a list of elements.
     *
     * @param values the elements for this vector
     * @throws IllegalArgumentException if elements is null or contains invalid elements
     */
    public VectorParam(List<Double> values) {
        if (values == null) {
            throw new IllegalArgumentException("Vector elements cannot be null");
        }
        this.values = new ArrayList<>(values);
        if (!isValid()) {
            throw new IllegalArgumentException("Vector contains invalid elements");
        }
    }

    /**
     * Constructs a Vector from an array of elements.
     *
     * @param values the elements for this vector
     * @throws IllegalArgumentException if any element is invalid
     */
    @SafeVarargs
    public VectorParam(Double... values) {
        this(Arrays.asList(values));
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

    @Override
    public double get(int i) {
        return values.get(i);
    }

    @Override
    public List<Double> getElements() {
        return values;
    }

    @Override
    public Double get(int... idx) {
        if (idx[0] != 1) throw new IllegalArgumentException();
        return values.get(idx[0]);
    }

    @Override
    public Primitive primitiveType() {
        return Real.INSTANCE;
    }
}
