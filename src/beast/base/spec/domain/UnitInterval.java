package beast.base.spec.domain;

/**
 * Probability type (value in [0, 1]).
 *
 * Represents a probability value that must be between 0 and 1 inclusive.
 * Used for transition probabilities, mixture weights, and other probabilistic parameters.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public class UnitInterval extends NonNegativeReal{
    public static final UnitInterval INSTANCE = new UnitInterval();

    protected UnitInterval() {}

    @Override
    public boolean isValid(Double value) {
        return Real.INSTANCE.isValid(value) && value >= getLower() && value <= getUpper();
    }

    @Override
    public Double getUpper() {
        return 1.0;
    }
}
