package beast.base.inference.parameter;

import beast.base.type.Matrix;
import beast.base.type.domain.Domain;
import beast.base.type.domain.NonNegativeReal;

public class NonNegativeRealMatrix extends RealParameter implements Matrix<NonNegativeReal> {
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();
		if (!isValid()) {
			throw new IllegalArgumentException("starting value must be a non-negative numbers");
		}
	}

	@Override
	public int[] shape() {
		return new int[] {getMinorDimension1(), getMinorDimension2()};
	}

	@Override
	public double get(int... idx) {
		return getArrayValue(idx[0] * minorDimension + idx[1]);
	}

	@Override
	public double getDoubleValue(int... idx) {
		return getArrayValue(idx[0] * minorDimension + idx[1]);
	}

	@Override
	public boolean isValid() {
		for (int i = 0; i < getDimension(); i++) {
			if (!Domain.isNonNegativeReal(getArrayValue(i))) {
				return false;
			}
		}
		return true;
	}
}
