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

    /**
     * the actual values of this parameter
     */
    protected int value;
    protected int storedValue;

    // Domain instance to enforce constraints
    protected D domain;


    public IntScalarParam() {
    }

    public IntScalarParam(int value, D domain) {
        // Note sync Input which will assign value in initAndValidate()
        valuesInput.setValue(value, this);
        setDomain(domain); // this set Input as well

        // always validate
        initAndValidate();
    }



    @Override
    public void initAndValidate() {
        this.value = valuesInput.get();
        this.storedValue = value;
        // Initialize domain from input
        this.domain = (D) domainTypeInput.get();

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
            copy.setDomain(domain);
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
    }

    @Override
    public void assignTo(StateNode other) {
        @SuppressWarnings("unchecked") final IntScalarParam<D> copy = (IntScalarParam<D>) other;
        copy.setID(getID());
        copy.index = index;
        copy.setDomain(getDomain());
        copy.set(get());
    }

    @Override
    public void assignFrom(final StateNode other) {
        @SuppressWarnings("unchecked") final IntScalarParam<D> source = (IntScalarParam<D>) other;
        setID(source.getID());
        set(source.get());
        storedValue = source.storedValue;
        setDomain(source.getDomain());
    }

    //*** for resume ***

    @Override
    public void fromXML(Node node) {
        ParameterUtils.parseParameter(node, this);
    }

//    @Override
    public void fromXML(final String shape, final String... valuesStr) {
        if (shape != null)
            throw new IllegalArgumentException("Shape not supported for Scalar ! " + shape);
        set(Integer.parseInt(valuesStr[0]));
    }

    @Override
    public String toString() {
        return ParameterUtils.paramToString(this);
    }

}