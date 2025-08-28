package beast.base.inference.parameter;


import beast.base.core.Description;
import beast.base.type.Scalar;
import beast.base.type.domain.Domain;
import beast.base.type.domain.NonNegativeReal;

@Description("Non-negative real valued parameter representing a single scalar value")
public class NonNegativeRealScalar extends RealParameter implements Scalar<NonNegativeReal> {

	@Override
	public void initAndValidate() {
		super.initAndValidate();
		if (getDimension() != 1) {
			throw new IllegalArgumentException("dimension must be 1");
		}
		if (!isValid()) {
			throw new IllegalArgumentException("starting value must not be negative");
		}
	}
	
	@Override
	public void setDimension(int dimension) {
		if (dimension != 1) {
			throw new IllegalArgumentException("dimension must be 1");
		}
		super.setDimension(dimension);
	}
	
	@Override
	public Double get(int... idx) {
		return getValue();
	}

	@Override
	public double getDoubleValue(int... idx) {
		return getArrayValue();
	}

	@Override
	public boolean isValid() {
		if (getDimension() != 1) {
			return false;
		}
		double v = getValue();
		return Domain.isNonNegativeReal(v);
	}

}
