package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Int;
import beast.base.spec.type.IntVector;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * An integer-valued ({@code int[]}) vector parameter in the MCMC state.
 * Implements {@link IntVector} for typed access.
 * The domain (e.g. {@link beast.base.spec.domain.NonNegativeInt}) constrains the permissible range.
 * Supports named dimensions via {@link KeyVectorParam}.
 *
 * @param <D> the integer domain type
 */
@Description("A int-valued vector with domain constraints")
public class IntVectorParam<D extends Int> extends KeyVectorParam<Integer> implements IntVector<D> { //VectorParam<D, Integer> {

    final public Input<List<Integer>> valuesInput = new Input<>("value",
            "starting value for this real scalar parameter.",
            new ArrayList<>(), Input.Validate.REQUIRED, Integer.class);
    // Additional input to specify the domain type
    public final Input<? extends Int> domainTypeInput = new Input<>("domain",
            "The domain type (default: Int; alternatives: NonNegativeInt, or PositiveInt) " +
                    "specifies the permissible range of values.", Int.INSTANCE);
    public final Input<Integer> dimensionInput = new Input<>("dimension",
            "dimension of the parameter (default 1, i.e scalar)", 1);

    @Deprecated
    final public Input<Integer> lowerValueInput = new Input<>("lower",
            "lower value for this parameter (default Integer.MIN_VALUE + 1)");
    @Deprecated
    final public Input<Integer> upperValueInput = new Input<>("upper",
            "upper value for this parameter (default Integer.MAX_VALUE - 1)");

    /**
     * the actual values of this parameter
     */
    protected int[] values;
    protected int[] storedValues;

    // Domain instance to enforce constraints
    protected D domain;

    // default
    @Deprecated
    protected int lower = Integer.MIN_VALUE + 1;
    @Deprecated
    protected int upper = Integer.MAX_VALUE - 1;

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
    public IntVectorParam() {
    }

    public IntVectorParam(final int[] values, D domain) {
        // default bounds
        this(values, domain, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
    }

    // if values.length < dim, then extend values to the same dim
    public IntVectorParam(final int dimension, final int[] values, D domain) {
        int[] newValues = new int[dimension];
        for (int i = 0; i < dimension; i++) {
            newValues[i] = values[i % values.length];
        }

        valuesInput.setValue(IntStream.of(newValues).boxed().toList(), this);
        domainTypeInput.setValue(domain, this);
        isDirty = new boolean[values.length];

        // always validate
        initAndValidate();
    }

    /**
     * This constructor centralizes logic in one place,
     * and guarantees initAndValidate() runs once.
     * @param values   vector values
     * @param domain   vector {@link Domain}
     * @param lower    lower bound
     * @param upper    upper bound
     */
    public IntVectorParam(final int[] values, D domain, int lower, int upper) {
      valuesInput.setValue(IntStream.of(values).boxed().toList(), this);
      domainTypeInput.setValue(domain, this);
      isDirty = new boolean[values.length];

        // always validate
        initAndValidate();
    }

//    // this is only used by inherited class
//    protected void setInputsNoValidation(final int[] values, D domain, int lower, int upper) {
//        // Note set value to Input which will assign value in initAndValidate()
//        valuesInput.setValue(IntStream.of(values).boxed().toList(), this);
//        domainTypeInput.setValue(domain, this);
//        isDirty = new boolean[values.length];
//
//        if (this.lower != lower || this.upper != upper)
//            // adjust bounds to the Domain range
//            adjustBounds(lower, upper, domain.getLower(), domain.getUpper());
//    }


    @Override
    public void initAndValidate() {
        //keys
        super.initAndValidate();

        // allow value=1.0 dimension=4 to create a vector of four 1.0
        int[] valuesString = valuesInput.get().stream()
                .mapToInt(Integer::intValue)
                .toArray();
        int dimension = Math.max(dimensionInput.get(), valuesString.length);
        dimensionInput.setValue(dimension, this);

        values = new int[dimension];
        for (int i = 0; i < values.length; i++) {
            values[i] = valuesString[i % valuesString.length];
        }

        this.storedValues = values.clone();
        isDirty = new boolean[dimension];

        // Initialize domain from input
        this.domain = (D) domainTypeInput.get();

//        if (lowerValueInput.get() != null)
//            this.lower = lowerValueInput.get();
//        if (upperValueInput.get() != null)
//            this.upper = upperValueInput.get();
//        // adjust bound to the Domain range
//        setBounds(Math.max(getLower(), domain.getLower()),
//                Math.min(getUpper(), domain.getUpper()));

//        initBounds(lowerValueInput, upperValueInput, domain.getLower(), domain.getUpper());

        // validate value after domain and bounds are set
//        for (Integer value : values) {
//            if (! isValid(value))
//                throw new IllegalArgumentException("Value " + value +
//                        " is not valid for domain " + domain.getClass().getName());
//
//            valuesInput.get().add(value);
//        }

        // Validate against domain and bounds constraints
        if (! isValid()) {
            throw new IllegalArgumentException("Initial value of " + this +
                    " is not valid for domain " + domain.getClass().getName());
        }
    }

    /**
     * Returns the domain that constrains this parameter's value range.
     *
     * @return the domain instance
     */
    @Override
    public D getDomain() {
        if (domain == null) return (D) domainTypeInput.get(); // used before init
        return domain;
    }

    /** {@inheritDoc} */
    @Override
    public List<Integer> getElements() {
        // TODO unmodified ?
        return Arrays.stream(values).boxed().collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public int get(final int i) {
        return values[i];
    }

    public int getStoredValue(final int i) {
        return storedValues[i];
    }

    public int[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public int[] getStoredValues() {
        return Arrays.copyOf(storedValues, storedValues.length);
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return values.length;
    }

    @Override
    public Integer getLower() {
        return lower;
    }

    @Override
    public Integer getUpper() {
        return upper;
    }

    /**
     * @param key unique key for a value
     * @return the value associated with that key, or null
     */
    @Override
    public Integer get(String key) {
        if (keys != null)
            return get(keyToIndexMap.get(key));

        try {
            int index = Integer.parseInt(key);
            return get(index);
        } catch (NumberFormatException nfe) {
            return null; //TODO ?
        }
    }

    //*** setValue ***

    /**
     * Sets the first element's value.
     *
     * @param value the new value
     */
    public void set(final int value) {
        set(0, value);
    }

    /**
     * Sets the value at the given index, validating against domain and bound constraints.
     *
     * @param i     the index
     * @param value the new value
     * @throws IllegalArgumentException if the value is outside the valid range
     */
    public void set(final int i, final int value) {
        startEditing(null);
        if (! isValid(value)) {
            throw new IllegalArgumentException("Value " + value +
                    " is not valid for domain " + domain.getClass().getName());
        }
        values[i] = value;
        isDirty[i] = true;
        lastDirty = i;
    }

    /**
     * swap values of element i1 and i2
     *
     * @param i1   index one
     * @param i2   index two
     */
    public void swap(final int i1, final int i2) {
        startEditing(null);
        final int tmp = values[i1];
        values[i1] = values[i2];
        values[i2] = tmp;
        isDirty[i1] = true;
        isDirty[i2] = true;
    }

    protected void setDomain(D domain) {
        this.domain = domain;
        domainTypeInput.setValue(domain, this);
    }

    /**
     * If the new dimension is greater than current, uses the current values to supplement the rest.
     * If the new dimension is less than current, truncates the current values.
     * @param dimension
     */
    public void setDimension(final int dimension) {
        startEditing(null);

        if (this.size() != dimension) {
            final int[] values2 = new int[dimension];
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

    @Deprecated
    public void setLower(Integer lower) {
        if (lower < getDomain().getLower())
            throw new IllegalArgumentException("Lower bound " + lower +
                    " is not valid for domain " + domain.getClass().getName());
        this.lower = lower;
        lowerValueInput.setValue(lower, this);
    }

    @Deprecated
    public void setUpper(Integer upper) {
        if (upper > getDomain().getUpper())
            throw new IllegalArgumentException("Upper bound " + upper +
                    " is not valid for domain " + domain.getClass().getName());
        this.upper = upper;
        upperValueInput.setValue(upper, this);
    }

    //*** StateNode methods ***

    @SuppressWarnings("unchecked")
    @Override
    protected void store() {
        if (storedValues.length != values.length)
            storedValues = new int[values.length];
        System.arraycopy(values, 0, storedValues, 0, values.length);
    }

    @Override
    public void restore() {
        final int[] tmp = storedValues;
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
        final IntVectorParam var = (IntVectorParam) getCurrent();
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
    public IntVectorParam copy() {
        try {
            @SuppressWarnings("unchecked") final IntVectorParam<D> copy = (IntVectorParam<D>) this.clone();
            copy.values = values.clone();
            copy.setDomain(domain);
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
        @SuppressWarnings("unchecked") final IntVectorParam<D> copy = (IntVectorParam<D>) other;
        copy.setID(getID());
        copy.index = index;
        copy.values = values.clone();
        copy.setDomain(getDomain());
//        copy.setBounds(getLower(), getUpper());
        copy.isDirty = new boolean[values.length];
    }

    /**
     * this := other
     * Assign all values of other to this
     * NB: Should only be used for initialisation!
     */
    @Override
    public void assignFrom(final StateNode other) {
        @SuppressWarnings("unchecked") final IntVectorParam<D> source = (IntVectorParam<D>) other;
        setID(source.getID());
        values = source.values.clone();
        storedValues = source.storedValues.clone();
        System.arraycopy(source.values, 0, values, 0, values.length);
        setDomain(source.getDomain());
//        setBounds(source.getLower(), source.getUpper());
        isDirty = new boolean[source.values.length];
    }

    /**
     * As assignFrom, but without copying the ID
     * NB: Should only be used for initialisation!
     */
    @Override
    public void assignFromFragile(final StateNode other) {
        @SuppressWarnings("unchecked") final IntVectorParam<D> source = (IntVectorParam<D>) other;
        this.setDimension(source.values.length);
        System.arraycopy(source.values, 0, values, 0, source.values.length);
        Arrays.fill(isDirty, false);
    }

    //*** for resume ***

    /**
     * Note that changing toString means fromXML needs to be changed as
     * well, since it parses the output of toString back into a parameter.
     */
    @Override
    public String toString() {
        return ParameterUtils.paramToString(this);
    }

    @Override
    public void fromXML(final Node node) {
        ParameterUtils.parseParameter(node, this);
    }

//    @Override
    public void fromXML(final String shape, final String... valuesStr) {
//        setLower(Integer.parseInt(lower));
//        setUpper(Integer.parseInt(upper));
        // validate shape
        try {
            int dim = Integer.parseInt(shape);
            if (dim != valuesStr.length)
                throw new IllegalArgumentException("The dimension " + dim +
                        " must equal to the size of values " + valuesStr.length + " !");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal size " + shape + " for vector " + this);
        }
        // this may change dimension
        values = new int[valuesStr.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.parseInt(valuesStr[i]);
        }
    }
}