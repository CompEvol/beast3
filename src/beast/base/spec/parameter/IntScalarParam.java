package beast.base.spec.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.*;
import beast.base.spec.type.IntScalar;


@Description("A scalar real-valued parameter with domain constraints")
public class IntScalarParam<D extends Int> extends IntegerParameter implements IntScalar<D> {

    // Domain instance to enforce constraints
    private D domain;

    // Additional input to specify the domain type
    public final Input<String> domainTypeInput = new Input<>("domainType",
            "Domain type: Int, PositiveInt, NonNegativeInt",
            "Int");

    public IntScalarParam() {
        super();
    }

    public IntScalarParam(Integer value, D domain) {
        super(new Integer[]{value});
        this.domain = domain;
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
        initializeDomain();
        
        // Let parent handle basic initialization
        super.initAndValidate();
        
        // Validate against domain constraints
        if (! isValid(getValue())) {
            throw new IllegalArgumentException("Initial value " + getValue() + 
                    " is not valid for domain " + domain.getClass().getName());
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeDomain() {
        // If domain not already set, create based on inputs
        if (domain == null) {
            String domainType = domainTypeInput.get();
            
            // This would require a factory or registry pattern
            // For illustration, showing the concept:
            switch (domainType) {
                case "PositiveInt":
                    domain = (D) PositiveInt.INSTANCE;
                    break;
                case "NonNegativeInt":
                    domain = (D) NonNegativeInt.INSTANCE;
                    break;
                default:
                    domain = (D) Int.INSTANCE;
            }
            
            // Override bounds from domain if not explicitly set
            if (lowerValueInput.get() == null) {
                m_fLower = domain.getLower();
            }
            if (upperValueInput.get() == null) {
                m_fUpper = domain.getUpper();
            }
        }
    }
    
    @Override
    public void setValue(Integer value) {
        if (! isValid(value)) {
            throw new IllegalArgumentException("Value " + value + 
                    " is not valid for domain " + domain.getClass().getName());
        }
        super.setValue(value);
    }
    
    @Override
    public void setValue(int param, Integer value) {
        if (param != 0) {
            throw new IllegalArgumentException("ScalarReal only has index 0");
        }
        setValue(value);
    }

//    @Override
//    public int scale(double scale) {
//        Double newValue = getValue() * scale;
//        if (!domain.isValid(newValue)) {
//            throw new IllegalArgumentException("Scaled value " + newValue +
//                    " is not valid for domain " + domain.getClass().getName());
//        }
//        return super.scale(scale);
//    }
    
    // Implement Scalar<D> interface methods
    @Override
    public D domainType() {
        return domain;
    }
    
    @Override
    public Integer getValue() {
        return super.getValue();
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

    @Override
    public Integer get() {
        return getValue();
    }



}