package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealVector;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Description("A scalar real-valued parameter with domain constraints")
public class RealVectorParam<D extends Real> extends KeyVectorParam<Double> implements RealVector<D>{ //VectorParam<D, Double> {

    final public Input<List<Double>> valuesInput = new Input<>("value",
            "starting value for this real scalar parameter.",
            new ArrayList<>(), Input.Validate.REQUIRED, Double.class);
    // Additional input to specify the domain type
    public final Input<Domain> domainTypeInput = new Input<>("domain",
            "The domain type (default: Real; alternatives: NonNegativeReal, PositiveReal, or UnitInterval) " +
                    "specifies the permissible range of values.", Real.INSTANCE);

    public final Input<Integer> dimensionInput = new Input<>("dimension",
            "dimension of the parameter (default 1, i.e scalar)", 1);

    final public Input<Double> lowerValueInput = new Input<>("lower",
            "lower value for this parameter (default -infinity)");
    final public Input<Double> upperValueInput = new Input<>("upper",
            "upper value for this parameter (default +infinity)");

    /**
     * the actual values of this parameter
     */
    protected double[] values;
    protected double[] storedValues;

    // Domain instance to enforce constraints
    protected D domain;

    // default
    protected double lower = Double.NEGATIVE_INFINITY;
    protected double upper = Double.POSITIVE_INFINITY;

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

    // TODO Double?
    public RealVectorParam(final double[] values, D domain) {
        this.values = values.clone();
        this.storedValues = values.clone();
        setDomain(domain); // must set Input as well
        isDirty = new boolean[values.length];
    }

    public RealVectorParam(final double[] values, D domain, double lower, double upper) {
        this(values, domain);
        // adjust bound to the Domain range
        setLower(Math.max(lower, domain.getLower()));
        setUpper(Math.min(upper, domain.getUpper()));

        // always validate in initAndValidate()
    }
    
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

        if (lowerValueInput.get() != null)
            this.lower = lowerValueInput.get();
        if (upperValueInput.get() != null)
            this.upper = upperValueInput.get();
        // adjust bound to the Domain range
        setBounds(Math.max(getLower(), domain.getLower()),
                Math.min(getUpper(), domain.getUpper()));

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
                    " is not valid for domain " + domain.getClass().getName());
        }
    }

    @Override
    public D getDomain() {
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

    @Override
    public Double getLower() {
        return lower;
    }

    @Override
    public Double getUpper() {
        return upper;
    }

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

    public void setDimension(final int dimension) {
        startEditing(null);

        if (this.size() != dimension) {
            values = new double[dimension];
            storedValues = new double[dimension];
            isDirty = new boolean[dimension];
        }
        try {
            dimensionInput.setValue(dimension, this);
        } catch (Exception e) {
            // ignore
        }
    }

    public void setLower(double lower) {
        if (lower < domain.getLower())
            throw new IllegalArgumentException("Lower bound " + lower +
                    " is not valid for domain " + getDomain().getClass().getName());
        this.lower = lower;
        lowerValueInput.setValue(lower, this);
    }

    public void setUpper(double upper) {
        if (upper > domain.getUpper())
            throw new IllegalArgumentException("Upper bound " + upper +
                    " is not valid for domain " + getDomain().getClass().getName());
        this.upper = upper;
        upperValueInput.setValue(upper, this);
    }

    public void setBounds(double lower, double upper) {
        //TODO ? setLower(Math.max(getLower(), domain.getLower()));
        //       setUpper(Math.min(getUpper(), domain.getUpper()));
        setLower(lower);
        setUpper(upper);
    }

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

    /**
     * Note that changing toString means fromXML needs to be changed as
     * well, since it parses the output of toString back into a parameter.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(getID()).append("[").append(values.length);
        buf.append("] ");
        buf.append(boundsToString()).append(": ");
        for (final double value : values) {
            buf.append(value).append(" ");
        }
        return buf.toString();
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
        copy.setBounds(getLower(), getUpper());
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
        setBounds(source.getLower(), source.getUpper());
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

    /**
     * StateNode implementation *
     */
    @Override
    public void fromXML(final Node node) {

        //TODO

        final NamedNodeMap atts = node.getAttributes();
        setID(atts.getNamedItem("id").getNodeValue());
        final String str = node.getTextContent();
        Pattern pattern = Pattern.compile(".*\\[(.*) (.*)\\].*\\((.*),(.*)\\): (.*) ");
        Matcher matcher = pattern.matcher(str);

        if (matcher.matches()) {
            final String dimension = matcher.group(1);
            final String stride = matcher.group(2);
            final String lower = matcher.group(3);
            final String upper = matcher.group(4);
            final String valuesAsString = matcher.group(5);
            final String[] values = valuesAsString.split(" ");
//            minorDimension = Integer.parseInt(stride);
            fromXML(Integer.parseInt(dimension), lower, upper, values);
        } else {
            pattern = Pattern.compile(".*\\[(.*)\\].*\\((.*),(.*)\\): (.*) ");
            matcher = pattern.matcher(str);
            if (matcher.matches()) {
                final String dimension = matcher.group(1);
                final String lower = matcher.group(2);
                final String upper = matcher.group(3);
                final String valuesAsString = matcher.group(4);
                final String[] values = valuesAsString.split(" ");
//                minorDimension = 0;
                fromXML(Integer.parseInt(dimension), lower, upper, values);
            } else {
                throw new RuntimeException("parameter could not be parsed");
            }
        }
    }

    //TODO
    void fromXML(final int dimension, final String lower, final String upper, final String[] valuesString) {
        setLower(Double.parseDouble(lower));
        setUpper(Double.parseDouble(upper));
        values = new double[dimension];
        for (int i = 0; i < valuesString.length; i++) {
            values[i] = Double.parseDouble(valuesString[i]);
        }
    }

}