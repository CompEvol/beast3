package beast.base.spec.type;


import beast.base.core.BEASTInterface;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.ScalarDistribution;

/**
 * Vector type for integer-valued parameters with bounded domain constraints.
 * Combines the {@link Vector} value semantics with {@link Bounded} range checking,
 * where effective bounds are tightened by any attached distributions
 * (including {@link ScalarDistribution} and {@link IID}).
 *
 * @param <D> the integer domain type, e.g. {@link beast.base.spec.domain.Int},
 *            {@link beast.base.spec.domain.NonNegativeInt}, or {@link beast.base.spec.domain.PositiveInt}
 */
public interface IntVector<D extends Int> extends Vector<D, Integer>, Bounded<Integer> {

    /**
     * @param i index
     * @return the unboxed domain value at ith element, which is faster than boxed.
     */
    int get(int i);

    /**
     * Use boxed value T only when required for API or nullability.
     *
     * @param idx  index/indices, depending on the dimension.
     * @return  the boxed domain value at ith element.
     */
    default Integer get(int... idx) {
        if (idx.length != 1)
            throw new IndexOutOfBoundsException("Vector access requires exactly 1 index, but got " + idx.length);
        return get(idx[0]);
    }

//    default int[] getIntArray() {
//        int length = Math.toIntExact(size());
//        int[] arr = new int[length];
//        for (int i = 0; i < length; i++) {
//            arr[i] = get(i);
//        }
//        return arr;
//    }

    /**
     * Returns the effective lower bound, starting from the domain lower bound
     * and tightening it with any attached {@link ScalarDistribution} or {@link IID} constraints.
     *
     * @return the effective lower bound
     */
    @Override
    default Integer getLower() {
        D domain = getDomain();
        Integer lower = domain.getLower();
        if (this instanceof BEASTInterface b) {
            for (BEASTInterface o : b.getOutputs()) {
                if (o instanceof ScalarDistribution d) {
                    lower = BoundUtils.updateLower(lower, d, b);
                } else if (o instanceof IID iid) {
                    lower = BoundUtils.updateLower(lower, iid, b);
                }
            }
        }
        return lower;
    }

    /**
     * Returns the effective upper bound, starting from the domain upper bound
     * and tightening it with any attached {@link ScalarDistribution} or {@link IID} constraints.
     *
     * @return the effective upper bound
     */
    @Override
    default Integer getUpper() {
        D domain = getDomain();
        Integer upper = domain.getUpper();
        if (this instanceof BEASTInterface b) {
            for (BEASTInterface o : b.getOutputs()) {
                if (o instanceof ScalarDistribution d) {
                    upper = BoundUtils.updateUpper(upper, d, b);
                } else if (o instanceof IID iid) {
                    upper = BoundUtils.updateUpper(upper, iid, b);
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
    default boolean isValid() {
        for (int i = 0; i < size(); i++)
            if ( !isValid(get(i)))
                return false;
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
    default boolean isValid(Integer value) {
        // 1st check domain constraints, 2nd check if value is in the real scalar range
        // Note: these bounds can be the subset of domain bounds.
        D d = getDomain();
        return d.isValid(value) && withinBounds(value);
    }
}