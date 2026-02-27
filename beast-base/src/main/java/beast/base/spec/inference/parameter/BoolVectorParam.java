package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Bool;
import beast.base.spec.type.BoolVector;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A boolean-valued ({@code boolean[]}) vector parameter in the MCMC state.
 * Implements {@link BoolVector} for typed access.
 * The domain is fixed to {@link Bool}.
 * Supports named dimensions via {@link KeyVectorParam}.
 */
@Description("A boolean-valued vector")
public class BoolVectorParam extends KeyVectorParam<Boolean> implements BoolVector{ //VectorParam<Bool, Boolean> {

    final public Input<List<Boolean>> valuesInput = new Input<>("value",
            "starting value for this real scalar parameter.",
            new ArrayList<>(), Input.Validate.REQUIRED, Boolean.class);

    public final Input<Integer> dimensionInput = new Input<>("dimension",
            "dimension of the parameter (default 1, i.e scalar)", 1);

    /**
     * the actual values of this parameter
     */
    protected boolean[] values;
    protected boolean[] storedValues;

    // domain is fixed
//    final private Bool domain = Bool.INSTANCE;

    /**
     * isDirty flags for individual elements in high dimensional parameters
     */
    protected boolean[] isDirty;
    /**
     * last element to be changed *
     */
    protected int lastDirty;

    /**
     * constructors *
     */
    public BoolVectorParam() {
    }

    public BoolVectorParam(final boolean[] values) {
        // Note set value to Input which will assign value in initAndValidate()
        List<Boolean> boolList = new ArrayList<>();
        for (boolean v : values)
            boolList.add(v);
        valuesInput.setValue(boolList, this);
        isDirty = new boolean[values.length];

        // always validate
        initAndValidate();
    }

    // if values.length < dim, then extend values to the same dim
    public BoolVectorParam(final int dimension, boolean[] values) {
        List<Boolean> boolList = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            boolList.add(values[i % values.length]);
        }

        valuesInput.setValue(boolList, this);
        isDirty = new boolean[values.length];

        // always validate
        initAndValidate();
    }


    @Override
    public void initAndValidate() {
        // keys
        super.initAndValidate();

        // allow value=true dimension=4 to create a vector of four true
        List<Boolean> valuesList = valuesInput.get();
        boolean[] valuesString = new boolean[valuesList.size()];
        for (int i = 0; i < valuesList.size(); i++)
            valuesString[i] = valuesList.get(i);

        int dimension = Math.max(dimensionInput.get(), valuesString.length);
        dimensionInput.setValue(dimension, this);

        values = new boolean[dimension];
        for (int i = 0; i < values.length; i++) {
            values[i] = valuesString[i % valuesString.length];
        }
        this.storedValues = values.clone();
        isDirty = new boolean[dimension];


        // validate value after domain and bounds are set
//        for (Boolean value : values) {
//            if (! isValid(value))
//                throw new IllegalArgumentException("Value " + value +
//                        " is not valid for domain " + getDomain().getClass().getName());
//
//            valuesInput.get().add(value);
//        }

        // Validate against domain and bounds constraints
        if (! isValid()) {
            throw new IllegalArgumentException("Initial value of " + this +
                    " is not valid for domain " + getDomain().getClass().getName());
        }
    }

    @Override
    public Bool getDomain() {
        return Bool.INSTANCE;
    }

    @Override
    public List<Boolean> getElements() {
        // Java has no BooleanStream
        List<Boolean> list = new ArrayList<>(values.length);
        for (boolean b : values) list.add(b);
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public boolean get(final int i) {
        return values[i];
    }

    public boolean getStoredValue(final int i) {
        return storedValues[i];
    }

    public boolean[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public boolean[] getStoredValues() {
        return Arrays.copyOf(storedValues, storedValues.length);
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return values.length;
    }

    /**
     * @param key unique key for a value
     * @return the value associated with that key, or null
     */
    @Override
    public Boolean get(String key) {
        if (keys != null)
            return get(keyToIndexMap.get(key));

        try {
            int index = Integer.parseInt(key);
            return get(index);
        } catch (NumberFormatException nfe) {
            return null; //TODO ?
        }
    }

    /**
     * Sets the value at the given index.
     *
     * @param i     the index
     * @param value the new boolean value
     * @throws IllegalArgumentException if the value fails domain validation
     */
    public void set(final int i, final boolean value) {
        startEditing(null);
        if (! isValid(value)) {
            throw new IllegalArgumentException("Value " + value +
                    " is not valid for domain " + getDomain().getClass().getName());
        }
        values[i] = value;
        isDirty[i] = true;
        lastDirty = i;
    }

    /**
     * Sets the first element's value.
     *
     * @param value the new boolean value
     */
    public void set(final boolean value) {
        set(0, value);
    }

    /**
     * swap values of element i1 and i2
     *
     * @param i1   index one
     * @param i2   index two
     */
    public void swap(final int i1, final int i2) {
        startEditing(null);
        final boolean tmp = values[i1];
        values[i1] = values[i2];
        values[i2] = tmp;
        isDirty[i1] = true;
        isDirty[i2] = true;
    }

//    public void setDomain(Bool domain) {
//        if (! domain.equals(Bool.INSTANCE))
//            throw new IllegalArgumentException();
//    }

    /**
     * If the new dimension > current, then use the current values to supplement the rest empty elements.
     * If the new dimension < current, then cut the current values.
     * @param dimension
     */
    public void setDimension(final int dimension) {
        startEditing(null);

        if (this.size() != dimension) {
            final boolean[] values2 = new boolean[dimension];
            for (int i = 0; i < dimension; i++) {
                values2[i] = values[i % this.size()];
            }
            values = values2;
            //storedValues = (T[]) Array.newInstance(m_fUpper.getClass(), dimension);
        }
        isDirty = new boolean[dimension];
        try {
            dimensionInput.setValue(dimension, this);
        } catch (Exception e) {
            // ignore
        }
    }

    //*** StateNode methods ***

    @SuppressWarnings("unchecked")
    @Override
    protected void store() {
        if (storedValues.length != values.length)
            storedValues = new boolean[values.length];
        System.arraycopy(values, 0, storedValues, 0, values.length);
    }

    @Override
    public void restore() {
        final boolean[] tmp = storedValues;
        storedValues = values;
        values = tmp;
        setEverythingDirty(false);
        if (isDirty.length != values.length)
            isDirty = new boolean[values.length];
    }

    /**
     * @param index dimension to check
     * @return true if the param-th element has changed
     */
    public boolean isDirty(final int index) {
        return isDirty[index];
    }

    @Override
    public void setEverythingDirty(final boolean dirty) {
        setSomethingIsDirty(dirty);
        Arrays.fill(this.isDirty, dirty);
    }

    /**
     * Loggable interface implementation follows (partly, the actual logging
     * of values happens in derived classes) *
     */
    @Override
    public void init(final PrintStream out) {
        final int valueCount = this.size();
        if (valueCount == 1) {
            out.print(getID() + "\t");
        } else {
            for (int value = 0; value < valueCount; value++) {
                out.print(getID() + "." + getKey(value) + "\t");
            }
        }
    }

    /**
     * Loggable implementation *
     */
    @Override
    public void log(final long sample, final PrintStream out) {
        //TODO why not use getValues() directly ?
        final BoolVectorParam var = (BoolVectorParam) getCurrent();
        final int values = var.size();
        for (int value = 0; value < values; value++) {
            out.print(var.get(value) + "\t");
        }
    }

    @Override
    public void close(PrintStream out) {
        // nothing to do
    }


    /**
     * @return a deep copy of this node in the state.
     *         This will generally be called only for stochastic nodes.
     */
    @Override
    public BoolVectorParam copy() {
        try {
            @SuppressWarnings("unchecked") final BoolVectorParam copy = (BoolVectorParam) this.clone();
            copy.values = values.clone();
            copy.isDirty = new boolean[values.length];
            return copy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * other := this
     * Assign all values of this to other
     * NB: Should only be used for initialisation!
     */
    @Override
    public void assignTo(final StateNode other) {
        @SuppressWarnings("unchecked") final BoolVectorParam copy = (BoolVectorParam) other;
        copy.setID(getID());
        copy.index = index;
        copy.values = values.clone();
        copy.isDirty = new boolean[values.length];
    }

    /**
     * this := other
     * Assign all values of other to this
     * NB: Should only be used for initialisation!
     */
    @Override
    public void assignFrom(final StateNode other) {
        @SuppressWarnings("unchecked") final BoolVectorParam source = (BoolVectorParam) other;
        setID(source.getID());
        values = source.values.clone();
        storedValues = source.storedValues.clone();
        System.arraycopy(source.values, 0, values, 0, values.length);
        isDirty = new boolean[source.values.length];
    }

    /**
     * As assignFrom, but without copying the ID
     * NB: Should only be used for initialisation!
     */
    @Override
    public void assignFromFragile(final StateNode other) {
        @SuppressWarnings("unchecked") final BoolVectorParam source = (BoolVectorParam) other;
        this.setDimension(source.values.length);
        System.arraycopy(source.values, 0, values, 0, source.values.length);
        Arrays.fill(isDirty, false);
    }

    //*** resume ***

    @Override
    public String toString() {
        return ParameterUtils.paramToString(this);
    }

    @Override
    public void fromXML(final Node node) {
        ParameterUtils.parseParameter(node, this);
    }

    //TODO
    void fromXML(final String[] valuesStr) {
        values = new boolean[valuesStr.length];
        for (int i = 0; i < valuesStr.length; i++) {
            values[i] = Boolean.parseBoolean(valuesStr[i]);
        }
    }

}