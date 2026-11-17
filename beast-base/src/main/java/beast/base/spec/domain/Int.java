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
     * The smallest and largest integers this domain allows, e.g. 1 for PositiveInt, which
     * is stored as the exclusive lower bound 0. Integer parameters read them on every
     * proposal, so they are cached, but only on first use: computing them in the
     * constructor would read the bounds of a subclass that keeps them in fields before
     * those fields are set. The cache assumes a domain's bounds do not change; a domain
     * whose bounds do can override the getters.
     */
    private Integer inclusiveLower;
    private Integer inclusiveUpper;

    protected Int() {}

    /** @return the smallest integer this domain allows */
    public Integer getInclusiveLower() {
        Integer l = inclusiveLower;
        if (l == null) {
            // a race between threads only computes the same immutable value twice
            l = getLower();
            if (!lowerInclusive() && l != Integer.MAX_VALUE) {
                l = l + 1;
            }
            inclusiveLower = l;
        }
        return l;
    }

    /** @return the largest integer this domain allows */
    public Integer getInclusiveUpper() {
        Integer u = inclusiveUpper;
        if (u == null) {
            u = getUpper();
            if (!upperInclusive() && u != Integer.MIN_VALUE) {
                u = u - 1;
            }
            inclusiveUpper = u;
        }
        return u;
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