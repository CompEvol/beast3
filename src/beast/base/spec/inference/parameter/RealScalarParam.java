package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import org.w3c.dom.Node;

import java.io.PrintStream;

@Description("A scalar real-valued parameter with domain constraints")
public class RealScalarParam<D extends Real> extends StateNode implements RealScalar<D>, BoundedParam<Double> {

    final public Input<Double> valuesInput = new Input<>("value",
            "starting value for this real scalar parameter.",
            0.0, Input.Validate.REQUIRED, Double.class);

    // Additional input to specify the domain type
    public final Input<Domain> domainTypeInput = new Input<>("domain",
            "The domain type (default: Real; alternatives: NonNegativeReal, PositiveReal, or UnitInterval) " +
                    "specifies the permissible range of values.", Real.INSTANCE);

    final public Input<Double> lowerValueInput = new Input<>("lower",
            "lower value for this parameter (default -infinity)");
    final public Input<Double> upperValueInput = new Input<>("upper",
            "upper value for this parameter (default +infinity)");

    /**
     * the actual values of this parameter
     */
    protected double value;
    protected double storedValue;

    // Domain instance to enforce constraints
    protected D domain;

    // default
    protected double lower = Double.NEGATIVE_INFINITY;
    protected double upper = Double.POSITIVE_INFINITY;


    public RealScalarParam() { }

    public RealScalarParam(double value, D domain) {
        this.value = value;
        setDomain(domain); // this set Input as well
    }

    public RealScalarParam(double value, D domain, double lower, double upper) {
        this(value, domain); // this set Input as well
        // adjust bounds to the Domain range
        adjustBounds(lower, upper, domain.getLower(), domain.getUpper());

        // always validate in initAndValidate()
    }

    @Override
    public void initAndValidate() {
        this.value = valuesInput.get();
        this.storedValue = value;
        // Initialize domain from input
        this.domain = (D) domainTypeInput.get();

//        if (lowerValueInput.get() != null)
//            this.lower = lowerValueInput.get();
//        if (upperValueInput.get() != null)
//            this.upper = upperValueInput.get();
//        // adjust bound to the Domain range
//        setBounds(Math.max(getLower(), domain.getLower()),
//                Math.min(getUpper(), domain.getUpper()));
//        Bounded<Double> bounded = ParameterUtils.initBounds(lowerValueInput, upperValueInput,
//                domain, lower, upper);
//        lower = bounded.getLower();
//        upper = bounded.getUpper();

        initBounds(lowerValueInput, upperValueInput, domain.getLower(), domain.getUpper());

        if (!isValid(value)) {
            throw new IllegalArgumentException("Value " + value +
                    " is not valid for domain " + getDomain().getClass().getName());
        }

    }

    // Fast (no boxing)
    @Override
    public double get() {
        return value;
    }

    // Implement Scalar<D> interface methods
    @Override
    public D getDomain() {
        return domain;
    }

    @Override
    public Double getLower() {
        return lower;
    }

    @Override
    public Double getUpper() {
        return upper;
    }

    //*** setters ***

    // Fast (no boxing)
    public void set(double value) {
        startEditing(null);

        if (! isValid(value)) {
            throw new IllegalArgumentException("Value " + value +
                    " is not valid for domain " + getDomain().getClass().getName());
        }
        this.value = value; // a primitive value
    }

    private void setDomain(D domain) {
        this.domain = domain;
        domainTypeInput.setValue(domain, this);
    }

    public void setLower(Double lower) {
        if (lower < domain.getLower())
            throw new IllegalArgumentException("Lower bound " + lower +
                    " is not valid for domain " + getDomain().getClass().getName());
        this.lower = lower;
        lowerValueInput.setValue(lower, this);
    }

    public void setUpper(Double upper) {
        if (upper > domain.getUpper())
            throw new IllegalArgumentException("Upper bound " + upper +
                    " is not valid for domain " + getDomain().getClass().getName());
        this.upper = upper;
        upperValueInput.setValue(upper, this);
    }

    //*** StateNode methods ***

    @Override
    protected void store() {
        storedValue = value;
    }

    @Override
    public void restore() {
        value = storedValue;
        setEverythingDirty(false);
    }


    @Override
    public void setEverythingDirty(boolean isDirty) {
        setSomethingIsDirty(isDirty);
    }

    @Override
    public void init(PrintStream out) {
        out.print(getID() + "\t");
    }

    @Override
    public void log(long sample, PrintStream out) {
        out.print(get() + "\t");
    }

    @Override
    public void close(PrintStream out) {
        // nothing to do
    }

    @Override
    public int scale(double scale) {
//        return 1;
        throw new UnsupportedOperationException();
    }

    /**
     * @return a deep copy of this node in the state.
     *         This will generally be called only for stochastic nodes.
     */
    @Override
    public StateNode copy() {
        try {
            @SuppressWarnings("unchecked") final RealScalarParam copy = (RealScalarParam) this.clone();
            // value is primitive field
            copy.setDomain(domain);
            copy.setBounds(getLower(), getUpper());
            return copy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void assignFromFragile(final StateNode other) {
        @SuppressWarnings("unchecked") final RealScalarParam source = (RealScalarParam) other;
        set(source.get());
//        setBounds(source.getLower(), source.getUpper());
    }

    @Override
    public void assignTo(StateNode other) {
        @SuppressWarnings("unchecked") final RealScalarParam<D> copy = (RealScalarParam<D>) other;
        copy.setID(getID());
        copy.index = index;
        copy.setDomain(getDomain());
        copy.set(get());
        copy.setBounds(getLower(), getUpper());
    }

    @Override
    public void assignFrom(final StateNode other) {
        @SuppressWarnings("unchecked") final RealScalarParam<D> source = (RealScalarParam<D>) other;
        setID(source.getID());
        setDomain(source.getDomain());
        setBounds(source.getLower(), source.getUpper());
        set(source.get());
        storedValue = source.storedValue;
    }

    //*** for resume ***

    @Override
    public void fromXML(Node node) {
        ParameterUtils.parseParameter(node, this);
    }

    @Override
    public void fromXML(final String lower, final String upper, final String shape, final String... valuesStr) {
        setLower(Double.parseDouble(lower));
        setUpper(Double.parseDouble(upper));
        if (shape != null)
            throw new IllegalArgumentException("Shape not supported for Scalar ! " + shape);
        set(Double.parseDouble(valuesStr[0]));
    }

    @Override
    public String toString() {
        return ParameterUtils.paramToString(this);
    }

}