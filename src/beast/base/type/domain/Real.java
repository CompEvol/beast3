package beast.base.type.domain;

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
public interface Real extends Domain<Double> {

	default public boolean isValid(Double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}