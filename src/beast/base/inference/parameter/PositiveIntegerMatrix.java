package beast.base.inference.parameter;

import beast.base.type.Matrix;
import beast.base.type.domain.Domain;
import beast.base.type.domain.PositiveInt;

public class PositiveIntegerMatrix extends IntegerParameter implements Matrix<PositiveInt> {
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();

		if (!isValid()) {
			throw new IllegalArgumentException("starting value must be a positive whole numbers");
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
			if (!Domain.isPositiveInt(getArrayValue(i))) {
				return false;
			}
		}
		return true;
	}
}
