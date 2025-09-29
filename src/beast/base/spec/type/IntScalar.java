package beast.base.spec.type;

import beast.base.spec.Bounded;
import beast.base.spec.domain.Int;

public interface IntScalar<D extends Int> extends Scalar<D, Integer>, Bounded<Integer> {

    Integer get();

    @Override
    default Integer getLower() {
        D domain = getDomain();
        return domain.getLower();
    }

    @Override
    default Integer getUpper() {
        D domain = getDomain();
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

    @Override
    default boolean isValid(Integer value) {
        // 1st check domain constraints, 2nd check if value is in the real scalar range
        // Note: these bounds can be the subset of domain bounds.
        return Scalar.super.isValid(value) && withinBounds(value);
    }
}