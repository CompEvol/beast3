package beast.base.spec.type;

import beast.base.core.BEASTInterface;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.ScalarDistribution;

/**
 * Scalar type for real-valued (double) parameters with bounded domain constraints.
 * Combines the {@link Scalar} value semantics with {@link Bounded} range checking,
 * where effective bounds are tightened by any attached distributions.
 *
 * @param <D> the real domain type, e.g. {@link beast.base.spec.domain.Real},
 *            {@link beast.base.spec.domain.PositiveReal}, or {@link beast.base.spec.domain.UnitInterval}
 */
public interface RealScalar<D extends Real> extends Scalar<D, Double>, Bounded<Double> {

    /**
     * Get a single value.
     *
     * @return the unboxed domain value, which is faster than boxed.
     */
    double get();

    /**
     * Use boxed value T only when required for API or nullability.
     *
     * @param idx  index/indices, depending on the dimension.
     * @return  the boxed domain value.
     */
    default Double get(int... idx) {
        return get();
    }

    /**
     * Returns the effective lower bound, starting from the domain lower bound
     * and tightening it with any attached {@link ScalarDistribution} constraints.
     *
     * @return the effective lower bound
     */
    @Override
    default Double getLower() {
        D domain = getDomain();
        Double lower = domain.getLower();
        if (this instanceof BEASTInterface b) {
        	for (BEASTInterface o : b.getOutputs()) {
        		if (o instanceof ScalarDistribution d) {
                    lower = BoundUtils.updateLower(lower, d, b);
        		}
        	}
        }
        return lower;
    }

    /**
     * Returns the effective upper bound, starting from the domain upper bound
     * and tightening it with any attached {@link ScalarDistribution} constraints.
     *
     * @return the effective upper bound
     */
    @Override
    default Double getUpper() {
        D domain = getDomain();
        Double upper = domain.getUpper();
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

    /**
     * Validates a value against both domain constraints and effective bounds.
     * The effective bounds may be a subset of the domain bounds when distributions
     * impose tighter restrictions.
     *
     * @param value the value to validate
     * @return {@code true} if the value satisfies both domain and bound constraints
     */
    @Override
    default boolean isValid(Double value) {
        // 1st check domain constraints, 2nd check if value is in the real scalar range
        // Note: these bounds can be the subset of domain bounds.
        return Scalar.super.isValid(value) && withinBounds(value);
    }
    
}