package phylospec.base.inference;

import beast.base.inference.StateNode;
import org.phylospec.primitives.PositiveInt;
import org.phylospec.primitives.Primitive;
import org.phylospec.primitives.Real;
import org.phylospec.types.IntScalar;
import org.phylospec.types.Scalar;
import org.phylospec.types.Vector;
import org.w3c.dom.Node;
import phylospec.base.core.Input;
import phylospec.base.core.PrimitiveInput;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VectorParam<P extends Real> extends StateNode implements Vector<P> {

    final public PrimitiveInput<List<Double>, Vector<Real>> valueInput = new PrimitiveInput<>(
            "value","start value for this scalar parameter", new ArrayList<>(),
            Input.Validate.REQUIRED, Real.INSTANCE);
    public final PrimitiveInput<Integer, IntScalar<PositiveInt>> dimensionInput =
            new PrimitiveInput<>("dimension", "dimension of the parameter (default 1, i.e scalar)",
                    1, PositiveInt.INSTANCE);

    // TODO diff bound for each element?

    // same bound for each element
    final public PrimitiveInput<Double, Scalar<Real>> lowerValueInput = new PrimitiveInput<>(
            "lower","lower value for this parameter (default -infinity)",
            Double.NEGATIVE_INFINITY, Real.INSTANCE);
    final public PrimitiveInput<Double, Scalar<Real>> upperValueInput = new PrimitiveInput<>(
            "upper","upper value for this parameter (default +infinity)",
            Double.POSITIVE_INFINITY, Real.INSTANCE);

    //++++++++ Scalar ++++++++

    /**
     * the actual values of this parameter
     */
    protected double[] values;
    protected double[] storedValues;

    protected Double upper;
    protected Double lower;

    /**
     * isDirty flags for individual elements in high dimensional parameters
     */
    protected boolean[] isDirty;
    /**
     * last element to be changed *
     */
    protected int lastDirty;

    /**
     * Constructs a Vector from a list of elements.
     *
     * @param values the elements for this vector
     * @throws IllegalArgumentException if elements is null or contains invalid elements
     */
    public VectorParam(List<Double> values) {
        initByName("value", values);
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
    public void initAndValidate() {
//        T[] valuesString = valuesInput.get().toArray((T[]) Array.newInstance(getMax().getClass(), 0));
//TODO
        values = valueInput.getJValue().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        if (!isValid()) {
            throw new IllegalArgumentException("Vector contains invalid elements");
        }
        for (int i = 0; i < values.length; i++) {
            if (! isValidBound(values[i]) )
                throw new IllegalArgumentException("value [" + i + "] = " + values[i] +
                        ", which should be within [" + lower + ", " + upper + "]");
        }

        int dimension = Math.max(dimensionInput.getJValue(), values.length);
        dimensionInput.setValue(dimension, this);

        this.storedValues = values.clone();
        isDirty = new boolean[dimension];

    }

    public boolean isValidBound(double value) {
        return this.lower.compareTo(value) <= 0 && this.upper.compareTo(value) >= 0;
    }

    /**
     * @param index dimension to check
     * @return true if the param-th element has changed
     */
    public boolean isDirty(final int index) {
        return isDirty[index];
    }

    /**
     * Returns index of entry that was changed last. Useful if it is known
     * only a single value has changed in the array. *
     */
    public int getLastDirty() {
        return lastDirty;
    }

    @Override
    public void setEverythingDirty(final boolean isDirty) {
        setSomethingIsDirty(isDirty);
        Arrays.fill(this.isDirty, isDirty);
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
        int nScaled = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0.0)
                continue;

            values[i] *= scale;
            nScaled += 1;

            if (values[i] < lower || values[i] > upper) {
                throw new IllegalArgumentException("parameter scaled out of range");
            }
        }

        return nScaled;
    }

    @Override
    protected void store() {
        if (storedValues.length != values.length) {
            storedValues = new double[values.length];
        }
        System.arraycopy(values, 0, storedValues, 0, values.length);
    }

    @Override
    public void restore() {
        final double[] tmp = storedValues;
        storedValues = values;
        values = tmp;
        hasStartedEditing = false;
        if (isDirty.length != values.length) {
            isDirty = new boolean[values.length];
        }
    }

    @Override
    public int getDimension() {
        return values.length;
    }

    @Deprecated
    @Override
    public double getArrayValue(int dim) {
        return get(dim);
    }

    /**
     * Loggable interface implementation follows (partly, the actual logging
     * of values happens in derived classes) *
     */
    @Override
    public void init(final PrintStream out) {
        final int valueCount = getDimension();
        if (valueCount == 1) {
            out.print(getID() + "\t");
        } else {
            for (int value = 0; value < valueCount; value++) {
//                out.print(getID() + "." + getKey(value) + "\t");
            }
        }
    }

    @Override
    public void log(long sample, PrintStream out) {

    }

    @Override
    public void close(PrintStream out) {

    }

    @Override
    public double get(int i) {
        return values[i];
    }

    @Override
    public List<Double> getElements() {
        return Arrays.stream(values).boxed().toList();
    }

    @Override
    public Double get(int... idx) {
        if (idx.length != 1 || idx[0] < 0) throw new IllegalArgumentException();
        return values[idx[0]];
    }

    @Override
    public Primitive primitiveType() {
        return Real.INSTANCE;
    }
}
