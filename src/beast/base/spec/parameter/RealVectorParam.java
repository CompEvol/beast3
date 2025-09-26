package beast.base.spec.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.*;
import beast.base.spec.type.RealVector;

import java.util.Arrays;
import java.util.List;


@Description("A scalar real-valued parameter with domain constraints")
public class RealVectorParam<D extends Real> extends RealParameter implements RealVector<D> {

    // Domain instance to enforce constraints
    protected D domain;

    // Additional input to specify the domain type
    public final Input<Domain> domainTypeInput = new Input<>("domain",
            "Domain type: Real, PositiveReal, NonNegativeReal, or UnitInterval",
            Real.INSTANCE);

    public RealVectorParam() {
        super();
    }

    public RealVectorParam(final Double[] values, D domain) {
        super(values);
//        this.domain = domain; // TODO must set through Input
        domainTypeInput.setValue(domain, this);
    }
    
    @Override
    public void initAndValidate() {
        // Ensure vector dimension
        if (dimensionInput.get() <= 1 || valuesInput.get().size() <= 1) {
            throw new IllegalArgumentException("Vector must have dimension > 1");
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
        if (! isValid()) {
            throw new IllegalArgumentException("Initial value of " + this +
                    " is not valid for domain " + domain.getClass().getName());
        }
    }

    @Override
    public D domainType() {
        return domain;
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
    public List<Double> getElements() {
        return Arrays.stream(values).toList();
    }

    @Override
    public Double get(int i) {
        return getArrayValue(i);
    }

    //====== bounds ======//

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