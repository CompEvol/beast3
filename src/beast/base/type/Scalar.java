package beast.base.type;

import beast.base.type.domain.Domain;

public interface Scalar<D extends Domain<?>> extends Tensor<D> {
	
	@Override
	default int rank() {
		return 0;
	}
	
	@Override
	default int[] shape() {
		return new int[]{};
	}
	
	@Override
	default long size() {
		return 1;
	}

}
