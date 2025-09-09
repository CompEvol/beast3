package beast.base.spec.type;

import beast.base.spec.domain.Int;

public interface IntScalar<P extends Int> extends Scalar<P, Integer> {

    Integer get();

}