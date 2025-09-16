package beast.base.spec.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;
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
        initializeDomain();
        
        // Let parent handle basic initialization
        super.initAndValidate();
        
        // Validate against domain constraints
        if (!domain.withinBounds(getValue())) {
            throw new IllegalArgumentException("Initial value " + getValue() + 
                    " is not valid for domain " + domain.getClass().getName());
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeDomain() {
        domain = (D) domainTypeInput.get();
        // If domain not already set, create based on inputs
//        if (domain == null) {
//            String domainType = domainTypeInput.get();
//
//            // This would require a factory or registry pattern
//            // For illustration, showing the concept:
//            switch (domainType) {
//                case "PositiveReal":
//                    domain = (D) PositiveReal.INSTANCE;
//                    break;
//                case "NonNegativeReal":
//                    domain = (D) NonNegativeReal.INSTANCE;
//                    break;
//                case "UnitInterval":
//                    domain = (D) UnitInterval.INSTANCE;
//                    break;
//                default:
//                    domain = (D) Real.INSTANCE;
//            }
//
            // Override bounds from domain if not explicitly set
            if (lowerValueInput.get() == null) {
                m_fLower = domain.getLower();
            }
            if (upperValueInput.get() == null) {
                m_fUpper = domain.getUpper();
            }
//        }
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
}