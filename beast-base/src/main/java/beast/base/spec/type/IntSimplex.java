package beast.base.spec.type;

import beast.base.spec.domain.NonNegativeInt;

/**
 * Integer simplex type - vector of non-negative integers that sum to a fixed total.
 * Used for count-based allocations such as group sizes in Bayesian skyline plots.
 *
 * @param <D> the non-negative integer domain type
 */
public interface IntSimplex<D extends NonNegativeInt> extends IntVector<D> {

    /**
     * Returns the required sum that all elements must total.
     *
     * @return the expected sum
     */
    int expectedSum();

    /**
     * Computes the sum of all elements. For a valid integer simplex
     * this should equal {@link #expectedSum()}.
     *
     * @return the sum of all elements
     */
    default double sum() {
        int s = 0;
        for(int i = 0; i < size(); i++) {
            s += get(i);
        }
        return s;
    }

    /**
     * Validates that all elements satisfy domain/bound constraints and that
     * their sum equals {@link #expectedSum()} (within a tolerance of 1e-10).
     *
     * @return {@code true} if all elements are valid and their sum matches the expected total
     */
    default boolean isValid() {
        IntVector.super.isValid();

        double s = sum();
        return !(Math.abs(s - expectedSum()) > 1e-10);
    }

}
