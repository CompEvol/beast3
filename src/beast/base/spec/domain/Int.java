package beast.base.spec.domain;

import beast.base.spec.Bounded;

/**
 * Integer type.
 *
 * Represents a whole number (positive, negative, or zero).
 * Note: This interface is named Integer to match PhyloSpec conventions,
 * but uses the fully qualified java.lang.Integer when needed to avoid conflicts.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public class Int implements Domain<Integer>, Bounded<Integer> {
    public static final Int INSTANCE = new Int();

    protected Int() {}

    /**
     * Checks whether a given value is valid {@code Int}
     * and also lies within the defined bounds.
     *
     * @param value the Integer value to validate
     * @return {@code true} if the value is not null, and also within the range,
     *         {@code false} otherwise.
     */
    @Override
    public boolean isValid(Integer value) {
        return value != null && Bounded.super.isValid(value);
    }

    @Override
    public Class<Integer> getTypeClass() {
        return Integer.class;
    }

    @Override
    public Integer getLower() {
        return Integer.MIN_VALUE;
    }

    @Override
    public Integer getUpper() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean lowerInclusive() {
        return true;
    }

    @Override
    public boolean upperInclusive() {
        return true;
    }
}