package beast.base.spec.type;

import beast.base.core.BEASTInterface;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.distribution.ScalarDistribution;

public interface IntScalar<D extends Int> extends Scalar<D, Integer>, Bounded<Integer> {

    /**
     * Get a single value.
     *
     * @return the unboxed domain value, which is faster than boxed.
     */
    int get();

    /**
     * Use boxed value T only when required for API or nullability.
     *
     * @param idx  index/indices, depending on the dimension.
     * @return  the boxed domain value.
     */
    default Integer get(int... idx) {
        return get();
    }

    @Override
    default Integer getLower() {
        D domain = getDomain();
        Integer lower = domain.getLower();
        if (this instanceof BEASTInterface b) {
        	for (BEASTInterface o : b.getOutputs()) {
        		if (o instanceof ScalarDistribution d) {
        			lower = BoundUtils.updateLower(lower, d, b);
        		}
        	}
        }
        return lower;
    }

    @Override
    default Integer getUpper() {
        D domain = getDomain();
        Integer upper = domain.getUpper();
        if (this instanceof BEASTInterface b) {
        	for (BEASTInterface o : b.getOutputs()) {
        		if (o instanceof ScalarDistribution d) {
                    upper = BoundUtils.updateUpper(upper, d, b);
        		}
        	}
        }
        return upper;
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