package beast.base.spec.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.*;
import beast.base.spec.type.IntScalar;
import beast.base.inference.StateNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Description("A scalar real-valued parameter with domain constraints")
public class IntScalarParam<D extends Int> extends StateNode implements IntScalar<D> {

    final public Input<Integer> valuesInput = new Input<>("value",
            "starting value for this real scalar parameter.",
            0, Input.Validate.REQUIRED, Integer.class);

    // Additional input to specify the domain type
    public final Input<Domain> domainTypeInput = new Input<>("domain",
            "The domain type (default: Real; alternatives: PositiveReal, NonNegativeReal, or UnitInterval) " +
                    "specifies the permissible range of values.", Real.INSTANCE);

    final public Input<Integer> lowerValueInput = new Input<>("lower",
            "lower value for this parameter (default -infinity)");
    final public Input<Integer> upperValueInput = new Input<>("upper",
            "upper value for this parameter (default +infinity)");

    /**
     * the actual values of this parameter
     */
    protected int value;
    protected int storedValue;

    // Domain instance to enforce constraints
    protected D domain;

    // default
    protected Integer lower = Integer.MIN_VALUE + 1;
    protected Integer upper = Integer.MAX_VALUE - 1;


    public IntScalarParam() {
    }

    public IntScalarParam(int value, D domain) {
        this.value = value;
        setDomain(domain); // must set Input as well
    }

    public IntScalarParam(int value, D domain, Integer lower, Integer upper) {
        this(value, domain);
        setLower(lower);
        setUpper(upper);
    }

    @Override
    public void initAndValidate() {

        // Initialize domain based on type or bounds
        setDomain((D) domainTypeInput.get());
        // adjust bound to the Domain range
        setBounds(Math.max(getLower(), domain.getLower()),
                Math.min(getUpper(), domain.getUpper()));

        // contain validation, and it must be after domain and bounds are set
        set(valuesInput.get());

    }

    //    @Override
    public void set(Integer value) {
        if (! isValid(value)) {
            throw new IllegalArgumentException("Value " + value +
                    " is not valid for domain " + domain.getClass().getName());
        }
        this.value = value;
    }

    @Override
    public Integer get() {
        return value;
    }

    // Implement Scalar<D> interface methods
    @Override
    public D domainType() {
        return domain;
    }

    //*** setValue ***

    //    @Override
    public void setDomain(D domain) {
        this.domain = domain;
        domainTypeInput.setValue(domain, this);
    }

    //    @Override
    public void setLower(Integer lower) {
        if (lower < domain.getLower())
            throw new IllegalArgumentException("Lower bound " + lower +
                    " is not valid for domain " + domain.getClass().getName());
        this.lower = lower;
        lowerValueInput.setValue(lower, this);
    }

    //    @Override
    public void setUpper(Integer upper) {
        if (upper > domain.getUpper())
            throw new IllegalArgumentException("Upper bound " + upper +
                    " is not valid for domain " + domain.getClass().getName());
        this.upper = upper;
        upperValueInput.setValue(upper, this);
    }

    public void setBounds(Integer lower, Integer upper) {
        setLower(lower);
        setUpper(upper);
    }

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
    public void fromXML(Node node) {
//        ParameterUtils.parseScalarParameter(node, this);

        //TODO this need to sync with toString

        final NamedNodeMap atts = node.getAttributes();
        setID(atts.getNamedItem("id").getNodeValue());
        final String str = node.getTextContent();
        // need to sync with toString
        Pattern pattern = Pattern.compile(".*[\\[(](.*),(.*)[\\])]: (.*) ");
        // (?: ... )? makes the whole bracketed section optional.
//        Pattern pattern = Pattern.compile(".*(?:[\\[(](.*),(.*)[\\])] )?: (.*) ");
        Matcher matcher = pattern.matcher(str);
        if (matcher.matches()) {
//            final String dimension = matcher.group(1);
            final String lower = matcher.group(1);
            final String upper = matcher.group(2);
            final String valuesAsString = matcher.group(3);
//            final String[] values = valuesAsString.split(" ");
//            minorDimension = 0;
            fromXML(valuesAsString, lower, upper);
        } else {
            throw new RuntimeException("String could not be parsed to parameter : " + str);
        }
    }

    public void fromXML(final String valuesString, final String lower, final String upper) {
        set(Integer.parseInt(valuesString));
        setLower(Integer.parseInt(lower));
        setUpper(Integer.parseInt(upper));
    }

    @Override
    public String toString() {
//        return ParameterUtils.scalarParamToString(this);
        return getID() + boundsToString() + ": " + get() + " ";
    }

    @Override
    public int scale(double scale) {
//        return 1;
        throw new UnsupportedOperationException();
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
            copy.setBounds(getLower(), getUpper());
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
        copy.setDomain(domainType());
        copy.set(get());
        copy.setBounds(getLower(), getUpper());
    }

    @Override
    public void assignFrom(final StateNode other) {
        @SuppressWarnings("unchecked") final IntScalarParam<D> source = (IntScalarParam<D>) other;
        setID(source.getID());
        set(source.get());
        storedValue = source.storedValue;
        setDomain(source.domainType());
        setBounds(source.getLower(), source.getUpper());
    }

}