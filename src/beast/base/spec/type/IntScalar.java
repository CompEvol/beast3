package beast.base.spec.type;

import beast.base.spec.Bounded;
import beast.base.spec.domain.Int;

public interface IntScalar<D extends Int> extends Scalar<D, Integer>, Bounded<Integer> {

    Integer get();

    @Override
    default Integer getLower() {
        D domain = domainType();
        return domain.getLower();
    }

    @Override
    default Integer getUpper() {
        D domain = domainType();
        return domain.getUpper();
    }

    @Override
    default boolean lowerInclusive() {
        return true;
    }

    @Override
    default boolean upperInclusive() {
        return true;
    }
}