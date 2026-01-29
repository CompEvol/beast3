package beast.base.spec.inference.util;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Int;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.Tensor;

@Description("Cast the type of a tensor to IntScalar of a particular domain")
public class AsIntScalar<D extends Int> extends CalculationNode implements IntScalar<D> {
    final public Input<Tensor<?,?>> argumentInput = new Input<>("arg", "argument to be converted", Validate.REQUIRED);

    // Additional input to specify the domain type
    final public Input<? extends Int> domainTypeInput = new Input<>("domain",
            "The domain type (default: Real; alternatives: NonNegativeReal, PositiveReal, or UnitInterval) " +
                    "specifies the permissible range of values.", Int.INSTANCE);
    
    protected D domain;
    protected Tensor<?,?> argument;
	
    public AsIntScalar() { }

    /**
     * This constructor centralises logic in one place,
     * and guarantees initAndValidate() runs once.
     * @param argument tensor to be type case
     * @param domain   scalar {@link Domain}
     */
    public AsIntScalar(Tensor<?,?> argument, D domain) {
        // Note set value to Input which will assign value in initAndValidate()
    	argumentInput.setValue(argument, this);

    	// always validate --  this set Input as well
        initAndValidate();
    }
    
	@Override
	public void initAndValidate() {
		argument = argumentInput.get();
        //TODO Remco to confirm
//        setDomain(domain); // this sets domainTypeInput as well
        // Initialize domain from input
        this.domain = (D) domainTypeInput.get();
	}

	@Override
	public D getDomain() {
        if (domain == null) {
        	this.domain = (D) domainTypeInput.get(); // used before init
        }
		return domain;
	}

	@Override
	public int get() {
		Object o = argument.get(0);
		if (o instanceof Integer i) {
			return i;
		}
		if (o instanceof Double d) {
			return d.intValue();
		}
		if (o instanceof Boolean b) {
			return b ? 1 : 0;
		}
		throw new IllegalArgumentException("Should not get here, since only Tensors of Double, Integer and Boolean are expected");
	}

    private void setDomain(D domain) {
        this.domain = domain;
        domainTypeInput.setValue(domain, this);
    }

}
