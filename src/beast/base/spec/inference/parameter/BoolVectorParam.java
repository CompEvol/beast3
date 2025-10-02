package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Bool;
import beast.base.spec.type.BoolVector;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Description("A scalar real-valued parameter with domain constraints")
public class BoolVectorParam extends KeyVectorParam<Boolean> implements BoolVector {

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

    // TODO Boolean?
    public BoolVectorParam(final boolean[] values) {
        this.values = values.clone();
        this.storedValues = values.clone();
        isDirty = new boolean[values.length];

        // validate value after domain and bounds are set
        for (Boolean value : values) {
            if (! isValid(value))
                throw new IllegalArgumentException("Value " + value +
                        " is not valid for domain " + getDomain().getClass().getName());

            valuesInput.get().add(value);
        }
    }

    @Override
    public void initAndValidate() {
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

        // keys
        if (keysInput.get() != null) {
            String[] keysArr = keysInput.get().split(" ");
            // unmodifiable list : UnsupportedOperationException if attempting to modify
            List<String> keys = Collections.unmodifiableList(Arrays.asList(keysArr));

            if (keys.size() != this.size())
                throw new IllegalArgumentException("For vector, keys must have the same length as dimension ! " +
                        "Dimension = " + this.size() + ", but keys.size() = " + keys.size());
            initKeys(keys);
        }

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

    @Override
    public Boolean get(int i) {
        return getValue(i); // unboxed
    }

    public boolean getValue(final int i) {
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

    //*** setValue ***

    public void set(final Boolean value) {
        this.set(0, value);
    }

    public void set(final int i, final Boolean value) {
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

    public void setDimension(final int dimension) {
        startEditing(null);

        if (this.size() != dimension) {
            values = new boolean[dimension];
            storedValues = new boolean[dimension];
            isDirty = new boolean[dimension];
        }
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
            out.print(var.getValue(value) + "\t");
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
        // nothing to do
        Log.warning.println("Attempt to scale Boolean parameter " + getID() + "  has no effect");
        return 0;
    }

    /**
     * Note that changing toString means fromXML needs to be changed as
     * well, since it parses the output of toString back into a parameter.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(getID()).append("[").append(values.length);
        buf.append("] ").append(": ");
        for (final boolean value : values)
            buf.append(value).append(" ");
        return buf.toString();
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
        values = new boolean[dimension];
        for (int i = 0; i < valuesString.length; i++) {
            values[i] = Boolean.parseBoolean(valuesString[i]);
        }
    }

}