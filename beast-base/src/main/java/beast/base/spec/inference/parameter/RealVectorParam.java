package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Scalable;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealVector;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;


@Description("A real-valued vector with domain constraints")
public class RealVectorParam<D extends Real> extends KeyVectorParam<Double> implements RealVector<D>, Scalable { //VectorParam<D, Double> {

    final public Input<List<Double>> valuesInput = new Input<>("value",
            "starting value for this real scalar parameter.",
            new ArrayList<>(), Input.Validate.REQUIRED, Double.class);
    // Additional input to specify the domain type
    public final Input<? extends Real> domainTypeInput = new Input<>("domain",
            "The domain type (default: Real; alternatives: NonNegativeReal, PositiveReal, or UnitInterval) " +
                    "specifies the permissible range of values.", Real.INSTANCE);

    public final Input<Integer> dimensionInput = new Input<>("dimension",
            "dimension of the parameter (default 1, i.e scalar)", 1);
//    @Deprecated
//    final public Input<Double> lowerValueInput = new Input<>("lower",
//            "lower value for this parameter (default -infinity)");
//    @Deprecated
//    final public Input<Double> upperValueInput = new Input<>("upper",
//            "upper value for this parameter (default +infinity)");

    /**
     * the actual values of this parameter
     */
    protected double[] values;
    protected double[] storedValues;

    // Domain instance to enforce constraints
    protected D domain;

    // default
//    @Deprecated
//    protected double lower = Double.NEGATIVE_INFINITY;
//    @Deprecated
//    protected double upper = Double.POSITIVE_INFINITY;

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
    public RealVectorParam() {
    }

    public RealVectorParam(final double[] values, D domain) {
      valuesInput.setValue(DoubleStream.of(values).boxed().toList(), this);
      domainTypeInput.setValue(domain, this);
      isDirty = new boolean[values.length];
      
      // always validate
      initAndValidate();
    }

    // if values.length < dim, then extend values to the same dim
    public RealVectorParam(final int dimension, final double[] values, D domain) {
        double[] newValues = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            newValues[i] = values[i % values.length];
        }

        valuesInput.setValue(DoubleStream.of(newValues).boxed().toList(), this);
        domainTypeInput.setValue(domain, this);
        isDirty = new boolean[values.length];

        // always validate
        initAndValidate();
    }

//    /**
//     * This constructor centralizes logic in one place,
//     * and guarantees initAndValidate() runs once.
//     * @param values   vector values
//     * @param domain   vector {@link Domain}
//     * @param lower    lower bound
//     * @param upper    upper bound
//     */
//    public RealVectorParam(final double[] values, D domain, double lower, double upper) {
//        setInputsNoValidation(values, domain, lower, upper);
//
//        // always validate
//        initAndValidate();
//    }

//    // this is only used by inherited class
//    protected void setInputsNoValidation(final double[] values, D domain, double lower, double upper) {
//        // Note set value to Input which will assign value in initAndValidate()
//        valuesInput.setValue(DoubleStream.of(values).boxed().toList(), this);
//        domainTypeInput.setValue(domain, this);
//        isDirty = new boolean[values.length];
//
//        if (this.lower != lower || this.upper != upper)
//            // adjust bounds to the Domain range
//            adjustBounds(lower, upper, domain.getLower(), domain.getUpper());
//    }

    @Override
    public void initAndValidate() {
        // keys
        super.initAndValidate();

        // allow value=1.0 dimension=4 to create a vector of four 1.0
        double[] valuesString = valuesInput.get().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        int dimension = Math.max(dimensionInput.get(), valuesString.length);
        dimensionInput.setValue(dimension, this);

        values = new double[dimension];
        for (int i = 0; i < values.length; i++) {
            values[i] = valuesString[i % valuesString.length];
        }
        this.storedValues = values.clone();
        isDirty = new boolean[dimension];

        // Initialize domain based on type or bounds
        domain = (D) domainTypeInput.get();

//        if (lowerValueInput.get() != null)
//            this.lower = lowerValueInput.get();
//        if (upperValueInput.get() != null)
//            this.upper = upperValueInput.get();
//        // adjust bound to the Domain range
//        setBounds(Math.max(getLower(), domain.getLower()),
//                Math.min(getUpper(), domain.getUpper()));

//        initBounds(lowerValueInput, upperValueInput, domain.getLower(), domain.getUpper());

        // validate value after domain and bounds are set
//        for (Double value : values) {
//            if (! isValid(value))
//                throw new IllegalArgumentException("Value " + value +
//                        " is not valid for domain " + getDomain().getClass().getName());
//
//            valuesInput.get().add(value);
//        }

        // Validate against domain and bounds constraints
        if (! isValid()) {
            throw new IllegalArgumentException("Initial value of " + this +
                    " is either not valid for domain " + domain.getClass().getName() +
                    " or bounds " + boundsToString());
        }
    }

    @Override
    public D getDomain() {
        if (domain == null) return (D) domainTypeInput.get(); // used before init
        return domain;
    }

    @Override
    public List<Double> getElements() {
        // TODO unmodified ?
        return Arrays.stream(values).boxed().collect(Collectors.toList());
    }

    // Fast (no boxing)
    @Override
    public double get(int i) {
        return values[i]; // unboxed
    }

    public double getStoredValue(final int i) {
        return storedValues[i];
    }

    public double[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public double[] getStoredValues() {
        return Arrays.copyOf(storedValues, storedValues.length);
    }

    @Override
    public int size() {
        return values.length;
    }

//    @Override
//    public Double getLower() {
//        return lower;
//    }
//
//    @Override
//    public Double getUpper() {
//        return upper;
//    }

    /**
     * @param key unique key for a value
     * @return the value associated with that key, or null
     */
    @Override
    public Double get(String key) {
        if (keys != null)
            return get(keyToIndexMap.get(key));

        try {
            int index = Integer.parseInt(key);
            return get(index);
        } catch (NumberFormatException nfe) {
            return null; //TODO ?
        }
    }

    //*** setters ***

    public void set(final double value) {
        set(0, value);
    }
    // when knowing the class, use setValue (fast), otherwise use set (boxed).
    // Fast (no boxing)
    public void set(final int i, final double value) {
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
     * swap values of element i1 and i2
     *
     * @param i1   index one
     * @param i2   index two
     */
    public void swap(final int i1, final int i2) {
        startEditing(null);
        final double tmp = values[i1];
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
     * If the new dimension > current, then use the current values to supplement the rest empty elements.
     * If the new dimension < current, then cut the current values.
     * @param dimension
     */
    public void setDimension(final int dimension) {
        startEditing(null);

        if (this.size() != dimension) {
            final double[] values2 = new double[dimension];
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

//    @Deprecated
//    public void setLower(Double lower) {
//        if (lower < getDomain().getLower())
//            throw new IllegalArgumentException("Lower bound " + lower +
//                    " is not valid for domain " + getDomain().getClass().getName());
//        this.lower = lower;
//        lowerValueInput.setValue(lower, this);
//    }
//
//    @Deprecated
//    public void setUpper(Double upper) {
//        if (upper > getDomain().getUpper())
//            throw new IllegalArgumentException("Upper bound " + upper +
//                    " is not valid for domain " + getDomain().getClass().getName());
//        this.upper = upper;
//        upperValueInput.setValue(upper, this);
//    }

    //*** StateNode methods ***

    @SuppressWarnings("unchecked")
    @Override
    protected void store() {
        if (storedValues.length != values.length)
            storedValues = new double[values.length];
        System.arraycopy(values, 0, storedValues, 0, values.length);
    }

    @Override
    public void restore() {
        final double[] tmp = storedValues;
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
        final RealVectorParam var = (RealVectorParam) getCurrent();
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
     * StateNode methods *
     */
    @Override
    public int scale(final double scale) {
    	startEditing(null);
        int nScaled = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0.0)
                continue;

            values[i] *= scale;
            nScaled += 1;

            if (! isValid(values[i]))
                throw new IllegalArgumentException("Parameter " + getID() + " scaled out of range !");

        }

        return nScaled;
    }

    @Override
    public void scaleOne(int i, double scale) {
    	startEditing(null);
    	values[i] *= scale;
    }

    /**
     * @return a deep copy of this node in the state.
     *         This will generally be called only for stochastic nodes.
     */
    @Override
    public RealVectorParam copy() {
        try {
            @SuppressWarnings("unchecked") final RealVectorParam<D> copy = (RealVectorParam<D>) this.clone();
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
        @SuppressWarnings("unchecked") final RealVectorParam<D> copy = (RealVectorParam<D>) other;
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
        @SuppressWarnings("unchecked") final RealVectorParam<D> source = (RealVectorParam<D>) other;
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
        @SuppressWarnings("unchecked") final RealVectorParam<D> source = (RealVectorParam<D>) other;
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
//        setLower(Double.parseDouble(lower));
//        setUpper(Double.parseDouble(upper));
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
        values = new double[valuesStr.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Double.parseDouble(valuesStr[i]);
        }
    }

}