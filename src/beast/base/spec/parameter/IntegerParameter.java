package beast.base.spec.parameter;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.StateNode;
import beast.base.spec.Bounded;

import java.io.PrintStream;


/**
 */
@Description("An integer-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class IntegerParameter extends Parameter<Integer> implements Bounded<Integer> {

    final public Input<Integer> lowerValueInput = new Input<>("lower",
            "lower value for this parameter (default -infinity)");
    final public Input<Integer> upperValueInput = new Input<>("upper",
            "upper value for this parameter (default +infinity)");

    // default
    protected Integer lower = Integer.MIN_VALUE + 1;;
    protected Integer upper = Integer.MAX_VALUE - 1;


    public IntegerParameter() {
    }

    public IntegerParameter(final Integer[] values) {
        super(values);
    }

    /**
     * Constructor used by Input.setValue(String) *
     */
    public IntegerParameter(final String value) {
//        init(0.0, 0.0, value, 1); // TODO 0.0 ?
        initByName("value", value, "dimension", 1);
    }

    @Override
    public void initAndValidate() {
        if (lowerValueInput.get() == null)
            lowerValueInput.setValue(getLower(), this);
        if (upperValueInput.get() == null)
            upperValueInput.setValue(getUpper(), this);

        super.initAndValidate();

        // Validate against domain constraints
        for (Integer v : values) {
            if (!withinBounds(v)) {
                throw new IllegalArgumentException("Initial value of " + v +
                        " is not valid for bound " + boundsToString());
            }
        }
    }

    @Override
    public Class<Integer> getPrimitiveClass() {
        return Integer.class;
    }


    /** Valuable implementation follows **/

    /**
     * RRB: we need this here, because the base implementation (public T getValue()) fails
     * for some reason. Why?
     */
    @Deprecated
    @Override
    public Integer getValue() {
        return values[0];
    }

    @Deprecated
    @Override
    public double getArrayValue() {
        return values[0];
    }

    @Deprecated
    @Override
    public double getArrayValue(final int index) {
        return values[index];
    }

    @Deprecated
    public int getNativeValue(final int value) {
        return values[value];
    }

    @Override
    public Integer getLower() {
        return lower;
    }

    @Override
    public Integer getUpper() {
        return upper;
    }

    public void setLower(final Integer lower) {
        this.lower = lower;
    }

    public void setUpper(final Integer upper) {
        this.upper = upper;
    }

    @Override
    public boolean lowerInclusive() {
        return false;
    }

    @Override
    public boolean upperInclusive() {
        return false;
    }

    public void setBounds(Integer lower, Integer upper) {
        setLower(lower);
        setUpper(upper);
    }

    /*** state node ***/

    /**
     * Loggable implementation *
     */
    @Override
    public void log(final long sample, final PrintStream out) {
        final IntegerParameter var = (IntegerParameter) getCurrent();
        final int values = var.getDimension();
        for (int value = 0; value < values; value++) {
            out.print(var.getValue(value) + "\t");
        }
    }

    /**
     * StateNode methods *
     */
    @Override
    public int scale(final double scale) {
        // nothing to do
        Log.warning.println("Attempt to scale Integer parameter " + getID() + "  has no effect");
        return 0;
    }

    @Override
    public void assignTo(StateNode other) {
        super.assignTo(other);
        if (other instanceof IntegerParameter copy)
            copy.setBounds(getLower(), getUpper());
    }

    @Override
    public void assignFrom(StateNode other) {
        super.assignFrom(other);
        @SuppressWarnings("unchecked") final IntegerParameter source = (IntegerParameter) other;
        setBounds(source.getLower(), source.getUpper());
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(super.toString());
        buf.append(" ").append(boundsToString());
        return buf.toString();
    }

    @Override
    void fromXML(final int dimension, final String lower, final String upper, final String[] valuesString) {
        setLower(Integer.parseInt(lower));
        setUpper(Integer.parseInt(upper));
        values = new Integer[dimension];
        for (int i = 0; i < valuesString.length; i++) {
            values[i] = Integer.parseInt(valuesString[i]);
        }
    }

}
