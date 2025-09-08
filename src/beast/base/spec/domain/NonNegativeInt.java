package beast.base.spec.domain;

/**
 * Non-negative integer number type (>= 0).
 * 
 * Represents an integer number that must be non-negative.
 * Common uses include distances, time durations, and count data.
 * 
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public class NonNegativeInt extends Int {
    public static final NonNegativeInt INSTANCE = new NonNegativeInt();

    protected NonNegativeInt() {}

    @Override
    public boolean isValid(Integer value) {
        return Int.INSTANCE.isValid(value) && value >= 0;
    }
}
