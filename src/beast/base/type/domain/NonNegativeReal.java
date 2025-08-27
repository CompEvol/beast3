package beast.base.type.domain;

/**
 * Non-negative real number type (>= 0).
 *
 * Represents a real number that must be non-negative.
 * Common uses include distances, time durations, and count data.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public interface NonNegativeReal extends Real {

    @Override
    default public boolean isValid(Double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0;
    }

}
