package beast.base.type;

import beast.base.type.domain.Domain;

public interface Vector<D extends Domain<?>> extends Tensor<D> {
	
	@Override
	default int rank() {
		return 1;
	}

}
