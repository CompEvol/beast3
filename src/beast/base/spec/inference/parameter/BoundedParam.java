package beast.base.spec.inference.parameter;

import beast.base.core.Input;
import beast.base.spec.Bounded;

/**
 * This defines a lower and upper limit, with the option
 * to specify whether each bound is inclusive or exclusive.
 * It also provides a validation method to check whether a given value
 * lies within the defined range.
 *
 * @param <T> the numeric or comparable type of the value (e.g. {@link Double}, {@link Integer})
 */
public interface BoundedParam<T extends Comparable<T>> extends Bounded<T> {

    /**
     * set the lower bound of the range.
     */
    void setLower(T value);

    /**
     * set the upper bound of the range.
     */
    void setUpper(T value);

    /**
     * set the lower and upper bound of the range.
     */
    default void setBounds(T lower, T upper) {
        setLower(lower);
        setUpper(upper);
    }

    default void initBounds(Input<T> lowerInput, Input<T> upperInput,
                            T domainLower, T domainUpper) {
        // if input is given, then use it, otherwise use the default
        T lower = (lowerInput.get() != null) ? lowerInput.get() : getLower();
        T upper = (upperInput.get() != null) ? upperInput.get() : getUpper();

        // adjust bounds to the Domain range
        adjustBounds(lower, upper, domainLower, domainUpper);
    }

    // adjust bounds to the Domain range
    default void adjustBounds(T lower, T upper, T domainLower, T domainUpper) {
        setBounds(BoundedParam.max(lower, domainLower),
                BoundedParam.min(upper, domainUpper) );
    }

    static <T extends Comparable<? super T>> T max(T a, T b) {
        return (a.compareTo(b) >= 0) ? a : b;
    }

    static <T extends Comparable<? super T>> T min(T a, T b) {
        return (a.compareTo(b) <= 0) ? a : b;
    }
}
