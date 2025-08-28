package beast.base.inference.parameter;


import beast.base.core.Description;
import beast.base.type.Scalar;
import beast.base.type.domain.Domain;
import beast.base.type.domain.Real;

@Description("Real valued parameter representing a single scalar value")
public class RealScalar extends RealParameter implements Scalar<Real> {

	@Override
	public void initAndValidate() {
		super.initAndValidate();
		if (getDimension() != 1) {
			throw new IllegalArgumentException("dimension must be 1");
		}
		if (!isValid()) {
			throw new IllegalArgumentException("starting value must be a finite number");
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
		return Domain.isReal(v);
	}

}
