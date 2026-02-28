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

//    @Override
//    public boolean isValid(Double value) {
//        return Real.INSTANCE.isValid(value) && value >= 0.0;
//    }

    /*
     * To customize how value ranges are validated in isValid(Double value),
     * override the four bound-related methods in Bounded interface.
     */

    @Override
    public Double getLower() {
        return 0.0;
    }

}
