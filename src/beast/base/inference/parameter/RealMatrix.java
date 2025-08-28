package beast.base.inference.parameter;

import beast.base.type.Matrix;
import beast.base.type.domain.Domain;
import beast.base.type.domain.Real;

public class RealMatrix extends RealParameter implements Matrix<Real> {
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();
		if (!isValid()) {
			throw new IllegalArgumentException("Invalid starting values found");
		}
	}
	
	@Override
	public int[] shape() {
		return new int[] {getMinorDimension1(), getMinorDimension2()};
	}

	@Override
	public Double get(int... idx) {
		return getValue(idx[0] * minorDimension + idx[1]);
	}

	@Override
	public double getDoubleValue(int... idx) {
		return getArrayValue(idx[0] * minorDimension + idx[1]);
	}

	@Override
	public boolean isValid() {
		for (int i = 0; i < getDimension(); i++) {
			if (!Domain.isReal(getArrayValue(i))) {
				return false;
			}
		}
		return true;
	}
}
