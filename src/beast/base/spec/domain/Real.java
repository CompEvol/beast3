package beast.base.spec.domain;

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
public class Real implements Domain<Double> {
    public static final Real INSTANCE = new Real();

    protected Real() {}

    @Override
    public boolean isValid(Double value) {
        // Bound requires Inf
        return !Double.isNaN(value); //&& !Double.isInfinite(value);
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
}