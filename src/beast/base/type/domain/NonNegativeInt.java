package beast.base.type.domain;

/**
 * Non-negative integer number type (>= 0).
 * 
 * Represents an integer number that must be non-negative.
 * Common uses include distances, time durations, and count data.
 * 
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public interface NonNegativeInt extends Int {

	@Override
    default public boolean isValid(Integer value) {
        return value != null && value >= 0;
    }
}
