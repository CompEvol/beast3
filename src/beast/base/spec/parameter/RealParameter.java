package beast.base.spec.parameter;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.spec.Bounded;

import java.io.PrintStream;


/**
 * @deprecated use {@link RealScalarParam} or {@link RealVectorParam}
 */
@Deprecated
@Description("A real-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class RealParameter extends Parameter<Double> implements Bounded<Double> {

    final public Input<Double> lowerValueInput = new Input<>("lower",
            "lower value for this parameter (default -infinity)");
    final public Input<Double> upperValueInput = new Input<>("upper",
            "upper value for this parameter (default +infinity)");

    // default
    protected Double lower = Double.NEGATIVE_INFINITY;
    protected Double upper = Double.POSITIVE_INFINITY;


    public RealParameter() {
    }

    public RealParameter(final Double[] values) {
        super(values);
    }

    /**
     * Constructor used by Input.setValue(String) *
     */
    public RealParameter(final String value) {
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
        for (Double v : values) {
            if (!withinBounds(v)) {
                throw new IllegalArgumentException("Initial value of " + v +
                        " is not valid for bound " + boundsToString());
            }
        }
    }

    @Override
    public Class<Double> getPrimitiveClass() {
        return Double.class;
    }

//    @Override
//    Double getMax() {
//        return Double.POSITIVE_INFINITY;
//    }
//
//    @Override
//    Double getMin() {
//        return Double.NEGATIVE_INFINITY;
//    }
    /** Valuable implementation follows **/

    /**
     * RRB: we need this here, because the base implementation (public T getValue()) fails
     * for some reason. Why?
     */
    @Deprecated
    @Override
    public Double getValue() {
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

    @Override
    public Double getLower() {
        return lower;
    }

    @Override
    public Double getUpper() {
        return upper;
    }

    public void setLower(final Double lower) {
        this.lower = lower;
    }

    public void setUpper(final Double upper) {
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

    public void setBounds(Double lower, Double upper) {
        setLower(lower);
        setUpper(upper);
    }

    /*** state node ***/

    /**
     * Loggable implementation *
     */
    @Override
    public void log(final long sample, final PrintStream out) {
        final RealParameter var = (RealParameter) getCurrent();
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
        int nScaled = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0.0)
                continue;

            values[i] *= scale;
            nScaled += 1;

            if (values[i] < getLower() || values[i] > getUpper()) {
                throw new IllegalArgumentException("parameter scaled out of range");
            }
        }

        return nScaled;
    }

    @Override
    public void assignTo(StateNode other) {
        super.assignTo(other);
        if (other instanceof RealParameter copy)
            copy.setBounds(getLower(), getUpper());
    }

    @Override
    public void assignFrom(StateNode other) {
        super.assignFrom(other);
        @SuppressWarnings("unchecked") final RealParameter source = (RealParameter) other;
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
        setLower(Double.parseDouble(lower));
        setUpper(Double.parseDouble(upper));
        values = new Double[dimension];
        for (int i = 0; i < valuesString.length; i++) {
            values[i] = Double.parseDouble(valuesString[i]);
        }
    }

}


