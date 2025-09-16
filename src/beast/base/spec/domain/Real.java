package beast.base.spec.domain;

import beast.base.spec.Bounded;

/**
 * Real number type.
 *
 * Represents a floating-point number that must be finite (not NaN or Infinity).
 * This is the base type for all continuous numeric types in PhyloSpec.
 * Note: this uses the fully qualified java.lang.Double when needed to avoid conflicts.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public class Real implements Domain<Double>, Bounded<Double> {

    public static final Real INSTANCE = new Real();

    protected Real() {}

    /**
     * Checks whether a given value is valid {@code Real}
     * and also lies within the defined bounds.
     *
     * @param value the Double value to validate
     * @return {@code true} if the value is not null or NaN, and also within the range,
     *         {@code false} otherwise.
     */
    @Override
    public boolean isValid(Double value) {
        // Bound requires Inf
        return value != null && !Double.isNaN(value) && Bounded.super.isValid(value);
    }

    @Override
    public Class<Double> getTypeClass() {
        return Double.class;
    }

    @Override
    public Double getLower() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public Double getUpper() {
        return Double.POSITIVE_INFINITY;
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