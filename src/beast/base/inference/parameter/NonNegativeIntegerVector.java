package beast.base.inference.parameter;

import beast.base.type.Vector;
import beast.base.type.domain.Domain;
import beast.base.type.domain.NonNegativeInt;

public class NonNegativeIntegerVector extends IntegerParameter implements Vector<NonNegativeInt> {
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();
		
		if (getMinorDimension1() != 1) {
			throw new IllegalArgumentException("minor dimension must be 1");
		}

		if (!isValid()) {
			throw new IllegalArgumentException("starting value must be a non-negative numbers");
		}
	}
	
	@Override
	public int[] shape() {
		return new int[] {getDimension()};
	}

	@Override
	public Integer get(int... idx) {
		return getValue(idx[0]);
	}

	@Override
	public double getDoubleValue(int... idx) {
		return getArrayValue(idx[0]);
	}

	@Override
	public boolean isValid() {
		if (getMinorDimension1() != 1) {
			return false;
		}
		
		for (int i = 0; i < getDimension(); i++) {
			if (!Domain.isNonNegativeInt(getArrayValue(i))) {
				return false;
			}
		}
		return true;
	}

	
	@Override
	public void setMinorDimension(int dimension) {
		if (dimension != 1) {
			throw new IllegalArgumentException("minor dimension must be 1");
		}
		super.setMinorDimension(dimension);
	}
}
