package beast.base.spec;

/**
 * This defines a lower and upper limit, with the option
 * to specify whether each bound is inclusive or exclusive.
 * It also provides a validation method to check whether a given value
 * lies within the defined range.
 *
 * @param <T> the numeric or comparable type of the value (e.g. {@link Double}, {@link Integer})
 */
public interface Bounded<T extends Comparable<T>> {

    /**
     * Returns the lower bound of the range.
     *
     * @return the lower bound value
     */
    T getLower();

    /**
     * Returns the upper bound of the range.
     *
     * @return the upper bound value
     */
    T getUpper();

    /**
     * Indicates whether the lower bound is inclusive.
     *
     * @return {@code true} if the lower bound is inclusive, {@code false} otherwise
     */
    boolean lowerInclusive();

    /**
     * Indicates whether the upper bound is inclusive.
     *
     * @return {@code true} if the upper bound is inclusive, {@code false} otherwise
     */
    boolean upperInclusive();

    /**
     * Checks whether a given value lies within the defined bounds.
     * <p>
     * The inclusivity of the lower and upper bounds is respected.
     *
     * @param value the value to validate
     * @return {@code true} if the value is within the range, {@code false} otherwise
     */
    default boolean withinBounds(T value) {
        boolean lowerCheck = lowerInclusive()
                ? value.compareTo(getLower()) >= 0
                : value.compareTo(getLower()) > 0;

        boolean upperCheck = upperInclusive()
                ? value.compareTo(getUpper()) <= 0
                : value.compareTo(getUpper()) < 0;

        return lowerCheck && upperCheck;
    }

    default String boundsToString() {
        String lowerBracket = lowerInclusive() ? "[" : "(";
        String upperBracket = upperInclusive() ? "]" : ")";
        return lowerBracket + getLower() + ", " + getUpper() + upperBracket;
    }

}
