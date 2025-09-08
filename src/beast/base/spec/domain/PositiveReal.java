package beast.base.spec.domain;

/**
 * Positive real number type (> 0).
 *
 * Represents a real number that must be strictly positive.
 * Common uses include rates, branch lengths, and variance parameters.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public class PositiveReal extends NonNegativeReal{
    public static final PositiveReal INSTANCE = new PositiveReal();

    protected PositiveReal() {}

    @Override
    public boolean isValid(Double value) { return Real.INSTANCE.isValid(value) && value > 0.0; }
}
