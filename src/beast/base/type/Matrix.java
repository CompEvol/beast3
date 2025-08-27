package beast.base.type;

import beast.base.type.domain.Domain;

public interface Matrix<D extends Domain<?>> extends Tensor<D> {
	
	@Override
	default int rank() {
		return 2;
	}

}
