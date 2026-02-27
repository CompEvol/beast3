package beast.base.spec.inference.util;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Loggable;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.Tensor;

import java.io.PrintStream;

/**
 * Type adapter that casts a {@link Tensor} to {@link RealScalar} with a specified
 * real domain. If the tensor is a vector, only the first element is used.
 *
 * @param <D> the target real domain type
 */
@Description("Cast the type of a tensor to RealScalar of a particular domain. "
		+ "If the tensor is a Vector, only the first value is used.")
public class AsRealScalar<D extends Real> extends CalculationNode implements RealScalar<D>, Loggable {
    final public Input<Tensor<?,?>> argumentInput = new Input<>("arg", "argument to be converted", Validate.REQUIRED);

    // Additional input to specify the domain type
    final public Input<? extends Real> domainTypeInput = new Input<>("domain",
            "The domain type (default: Real; alternatives: NonNegativeReal, PositiveReal, or UnitInterval) " +
                    "specifies the permissible range of values.", Real.INSTANCE);
    
    protected D domain;
    protected Tensor<?,?> argument;
	
    public AsRealScalar() { }

    /**
     * This constructor centralises logic in one place,
     * and guarantees initAndValidate() runs once.
     * @param argument tensor to be type case
     * @param domain   scalar {@link Domain}
     */
    public AsRealScalar(Tensor<?,?> argument, D domain) {
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
	public double get() {
		Object o = argument.get(0);
		if (o instanceof Double d) {
			return d;
		}
		if (o instanceof Integer i) {
			return i;
		}
		if (o instanceof Boolean b) {
			return b ? 1.0 : 0.0;
		}
		throw new IllegalArgumentException("Should not get here, since only Tensors of Double, Integer and Boolean are expected");
	}

    private void setDomain(D domain) {
        this.domain = domain;
        domainTypeInput.setValue(domain, this);
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
}
