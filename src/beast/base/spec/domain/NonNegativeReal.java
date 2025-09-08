package beast.base.spec.domain;

/**
 * Non-negative real number type (>= 0).
 *
 * Represents a real number that must be non-negative.
 * Common uses include distances, time durations, and count data.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public class NonNegativeReal extends Real {
    public static final NonNegativeReal INSTANCE = new NonNegativeReal();

    protected NonNegativeReal() {}

    @Override
    public boolean isValid(Double value) {
        return Real.INSTANCE.isValid(value) && value >= 0.0;
    }

}
