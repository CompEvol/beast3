package beast.base.spec.type;

import beast.base.spec.Bounded;
import beast.base.spec.domain.Real;

public interface RealScalar<D extends Real> extends Scalar<D, Double>, Bounded<Double> {

    Double get();

    @Override
    default Double getLower() {
        D domain = getDomain();
        return domain.getLower();
    }

    @Override
    default Double getUpper() {
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
    default boolean isValid(Double value) {
        // 1st check domain constraints, 2nd check if value is in the real scalar range
        // Note: these bounds can be the subset of domain bounds.
        return Scalar.super.isValid(value) && withinBounds(value);
    }
}