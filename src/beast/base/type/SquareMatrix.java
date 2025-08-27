package beast.base.type;

import beast.base.type.domain.Domain;

public interface SquareMatrix<D extends Domain<?>> extends Matrix<D> {
	
	@Override
	default boolean isValid() {
		int [] dimensions = shape();
		return dimensions.length == 2 &&
				dimensions[0] == dimensions[1];
	}

}
