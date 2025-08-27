package beast.base.type.domain;

/**
 * Probability type (value in [0, 1]).
 *
 * Represents a probability value that must be between 0 and 1 inclusive.
 * Used for transition probabilities, mixture weights, and other probabilistic parameters.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public interface UnitInterval extends NonNegativeReal{

    @Override
    default public boolean isValid(Double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0 && value <= 1.0;
    }
}
