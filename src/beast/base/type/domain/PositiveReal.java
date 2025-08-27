package beast.base.type.domain;

/**
 * Positive real number type (> 0).
 *
 * Represents a real number that must be strictly positive.
 * Common uses include rates, branch lengths, and variance parameters.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public interface PositiveReal extends NonNegativeReal{

    @Override
    default public boolean isValid(Double value) { 
    	return !Double.isNaN(value) && !Double.isInfinite(value) && value > 0.0; 
    }
}
