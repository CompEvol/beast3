package beast.base.spec.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.*;
import beast.base.spec.type.RealScalar;


@Description("A scalar real-valued parameter with domain constraints")
public class RealScalarParam<D extends Real> extends RealParameter implements RealScalar<D> {

    // Domain instance to enforce constraints
    protected D domain;

    // Additional input to specify the domain type
    public final Input<Domain> domainTypeInput = new Input<>("domain",
            "Domain type: Real, PositiveReal, NonNegativeReal, or UnitInterval", 
            Real.INSTANCE);
    
    public RealScalarParam() {
        super();
    }
    
    public RealScalarParam(Double value, D domain) {
        super(new Double[]{value});
//        this.domain = domain; // TODO must set through Input
        domainTypeInput.setValue(domain, this);

        // Override dimension to ensure scalar
        dimensionInput.setValue(1, this);
    }
    
    @Override
    public void initAndValidate() {
        // Ensure scalar dimension
        if (dimensionInput.get() != 1 || valuesInput.get().size() > 1) {
            throw new IllegalArgumentException("ScalarReal must have dimension 1");
        }
        
        // Initialize domain based on type or bounds
        domain = (D) domainTypeInput.get();

        // Let parent handle basic initialization
        super.initAndValidate();

        // TODO static method, e.g. resolveBounds() ?
        // adjust bound to the Domain range
        setBounds(Math.max(getLower(), domain.getLower()),
                Math.min(getUpper(), domain.getUpper()));

        // Validate against domain constraints
        if (! isValid(getValue())) {
            throw new IllegalArgumentException("Initial value of " + this +
                    " is not valid for domain " + domain.getClass().getName());
        }
    }

    @Override
    public void setValue(Double value) {
        if (! isValid(value)) {
            throw new IllegalArgumentException("Value " + value + 
                    " is not valid for domain " + domain.getClass().getName());
        }
        super.setValue(value);
    }
    
    @Override
    public void setValue(int i, Double value) {
        if (i != 0)
            throw new IllegalArgumentException("RealScalar only has index 0");

        setValue(value);
    }
    
    @Override
    public int scale(double scale) {
        Double newValue = getValue() * scale;
        if (! isValid(newValue)) {
            throw new IllegalArgumentException("Scaled value " + newValue +
                    " is not valid for domain " + domain.getClass().getName());
        }
        return super.scale(scale);
    }
    
    // Implement Scalar<D> interface methods
    @Override
    public D domainType() {
        return domain;
    }

    // Prevent dimension changes
    @Override
    public void setDimension(int dimension) {
        if (dimension != 1) {
            throw new IllegalArgumentException("Cannot change dimension of ScalarReal");
        }
        super.setDimension(dimension);
    }
    
    // Override to ensure scalar behavior
    @Override
    public int getDimension() {
        return 1;
    }
    
    // Hide matrix-related methods by throwing exceptions
    @Override
    public void setMinorDimension(int dimension) {
        throw new UnsupportedOperationException("ScalarReal does not support matrix operations");
    }
    
    @Override
    public int getMinorDimension1() {
        return 1;
    }
    
    @Override
    public int getMinorDimension2() {
        return 1;
    }

    // TODO 1. merge Bound and Parameter, 2. Scalar and Parameter

    @Override
    public Double get() {
        return getValue();
    }

    //TODO setValue ?

    @Override
    public void setLower(Double lower) {
        if (lower < domain.getLower())
            throw new IllegalArgumentException("Lower bound " + lower +
                    " is not valid for domain " + domain.getClass().getName());
        super.setLower(lower);
    }

    @Override
    public void setUpper(Double upper) {
        if (upper > domain.getUpper())
            throw new IllegalArgumentException("Upper bound " + upper +
                    " is not valid for domain " + domain.getClass().getName());
        super.setUpper(upper);
    }

}