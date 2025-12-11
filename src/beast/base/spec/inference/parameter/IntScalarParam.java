package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Int;
import beast.base.spec.type.IntScalar;
import org.w3c.dom.Node;

import java.io.PrintStream;

@Description("A scalar int-valued parameter with domain constraints")
public class IntScalarParam<D extends Int> extends StateNode implements IntScalar<D> {

    final public Input<Integer> valuesInput = new Input<>("value",
            "starting value for this real scalar parameter.",
            0, Input.Validate.REQUIRED, Integer.class);

    // Additional input to specify the domain type
    public final Input<? extends Int> domainTypeInput = new Input<>("domain",
            "The domain type (default: Int; alternatives: NonNegativeInt, or PositiveInt) " +
                    "specifies the permissible range of values.", Int.INSTANCE);

//    @Deprecated
//    final public Input<Integer> lowerValueInput = new Input<>("lower",
//            "lower value for this parameter (default Integer.MIN_VALUE + 1)");
//    @Deprecated
//    final public Input<Integer> upperValueInput = new Input<>("upper",
//            "upper value for this parameter (default Integer.MAX_VALUE - 1)");

    /**
     * the actual values of this parameter
     */
    protected int value;
    protected int storedValue;

    // Domain instance to enforce constraints
    protected D domain;

//    // default
//    @Deprecated
//    protected int lower = Integer.MIN_VALUE + 1;
//    @Deprecated
//    protected int upper = Integer.MAX_VALUE - 1;


    public IntScalarParam() {
    }

    public IntScalarParam(int value, D domain) {
        // default bounds
//        this(value, domain, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);

        // Note sync Input which will assign value in initAndValidate()
        valuesInput.setValue(value, this);
        setDomain(domain); // this set Input as well

        // always validate
        initAndValidate();
    }

//    /**
//     * This constructor centralizes logic in one place,
//     * and guarantees initAndValidate() runs once.
//     * @param value    scalar value
//     * @param domain   scalar {@link Domain}
//     * @param lower    lower bound
//     * @param upper    upper bound
//     */
//    public IntScalarParam(int value, D domain, int lower, int upper) {
//        // Note sync Input which will assign value in initAndValidate()
//        valuesInput.setValue(value, this);
//        setDomain(domain); // this set Input as well
//
//        if (this.lower != lower || this.upper != upper)
//            // adjust bounds to the Domain range
//            adjustBounds(lower, upper, domain.getLower(), domain.getUpper());
//
//        // always validate
//        initAndValidate();
//    }


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

//        initBounds(lowerValueInput, upperValueInput, domain.getLower(), domain.getUpper());

        if (!isValid(value)) {
            throw new IllegalArgumentException("Value " + value +
                    " is not valid for domain " + getDomain().getClass().getName());
        }

    }

    // Fast (no boxing)
    @Override
    public int get() {
        return value;
    }

    // Implement Scalar<D> interface methods
    @Override
    public D getDomain() {
        if (domain == null) return (D) domainTypeInput.get(); // used before init
        return domain;
    }

//    @Override
//    public Integer getLower() {
//        return lower;
//    }
//
//    @Override
//    public Integer getUpper() {
//        return upper;
//    }

    //*** setValue ***

    // Fast (no boxing)
    public void set(int value) {
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

//    @Deprecated
//    public void setLower(Integer lower) {
//        if (lower < getDomain().getLower())
//            throw new IllegalArgumentException("Lower bound " + lower +
//                    " is not valid for domain " + getDomain().getClass().getName());
//        this.lower = lower;
//        lowerValueInput.setValue(lower, this);
//    }
//
//    @Deprecated
//    public void setUpper(Integer upper) {
//        if (upper > getDomain().getUpper())
//            throw new IllegalArgumentException("Upper bound " + upper +
//                    " is not valid for domain " + getDomain().getClass().getName());
//        this.upper = upper;
//        upperValueInput.setValue(upper, this);
//    }

    //*** StateNode methods ***

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
    public StateNode copy() {
        try {
            @SuppressWarnings("unchecked") final IntScalarParam copy = (IntScalarParam) this.clone();
            copy.set(value);
            copy.setDomain(domain);
//            copy.setBounds(getLower(), getUpper());
            return copy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void assignFromFragile(final StateNode other) {
        @SuppressWarnings("unchecked") final IntScalarParam source = (IntScalarParam) other;
        set(source.get());
//        setBounds(source.getLower(), source.getUpper());
    }

    @Override
    public void assignTo(StateNode other) {
        @SuppressWarnings("unchecked") final IntScalarParam<D> copy = (IntScalarParam<D>) other;
        copy.setID(getID());
        copy.index = index;
        copy.setDomain(getDomain());
        copy.set(get());
//        copy.setBounds(getLower(), getUpper());
    }

    @Override
    public void assignFrom(final StateNode other) {
        @SuppressWarnings("unchecked") final IntScalarParam<D> source = (IntScalarParam<D>) other;
        setID(source.getID());
        set(source.get());
        storedValue = source.storedValue;
        setDomain(source.getDomain());
//        setBounds(source.getLower(), source.getUpper());
    }

    //*** for resume ***

    @Override
    public void fromXML(Node node) {
        ParameterUtils.parseParameter(node, this);
    }

//    @Override
    public void fromXML(final String shape, final String... valuesStr) {
//        setLower(Integer.parseInt(lower));
//        setUpper(Integer.parseInt(upper));
        if (shape != null)
            throw new IllegalArgumentException("Shape not supported for Scalar ! " + shape);
        set(Integer.parseInt(valuesStr[0]));
    }

    @Override
    public String toString() {
        return ParameterUtils.paramToString(this);
    }

}