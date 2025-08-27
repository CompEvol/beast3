package beast.base.inference.parameter;


import beast.base.core.Description;
import beast.base.type.Scalar;
import beast.base.type.domain.Domain;
import beast.base.type.domain.PositiveReal;

@Description("Positive real valued parameter (excluding zero) representing a single scalar value")
public class PositiveRealScalar extends RealParameter implements Scalar<PositiveReal> {

	@Override
	public void initAndValidate() {
		super.initAndValidate();
		if (getDimension() != 1) {
			throw new IllegalArgumentException("dimension must be 1");
		}
		if (!isValid()) {
			throw new IllegalArgumentException("starting value must be positive");
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
	public double get(int... idx) {
		return getArrayValue();
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
		return Domain.isPositiveReal(v);
	}

}
