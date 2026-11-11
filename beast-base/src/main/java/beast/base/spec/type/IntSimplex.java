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
     * their sum equals {@link #expectedSum()}.
     *
     * @return {@code true} if all elements are valid and their sum matches the expected total
     */
    default boolean isValid() {
        return IntVector.super.isValid() && sumIsExpected();
    }

    /**
     * Says why the value is not valid: an element that is not valid, or else the sum.
     *
     * @return the reason, or {@code null} if the value is valid
     */
    default String whyNotValid() {
        String why = IntVector.super.whyNotValid();
        if (why != null)
            return why;
        return sumIsExpected() ? null
                : "The values must sum to " + expectedSum() + ", but they sum to " + (long) sum() + ".";
    }

    private boolean sumIsExpected() {
        return sum() == expectedSum();
    }

}
