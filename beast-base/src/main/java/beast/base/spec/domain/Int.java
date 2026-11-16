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

    /**
     * The smallest and largest integers this domain allows, e.g. 1 for PositiveInt,
     * which is stored as the exclusive lower bound 0. Computed once, as integer
     * parameters read them on every proposal. The bound methods of the subclasses
     * return constants, so calling them from the constructor is safe.
     */
    private final int inclusiveLower;
    private final int inclusiveUpper;

    protected Int() {
        int lower = getLower();
        inclusiveLower = lowerInclusive() || lower == Integer.MAX_VALUE ? lower : lower + 1;
        int upper = getUpper();
        inclusiveUpper = upperInclusive() || upper == Integer.MIN_VALUE ? upper : upper - 1;
    }

    /** @return the smallest integer this domain allows */
    public final int getInclusiveLower() {
        return inclusiveLower;
    }

    /** @return the largest integer this domain allows */
    public final int getInclusiveUpper() {
        return inclusiveUpper;
    }

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
        return value != null && Bounded.super.withinBounds(value);
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