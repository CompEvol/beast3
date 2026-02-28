package beast.base.spec.domain;

/**
 * Positive integer type (> 0).
 * 
 * Represents an integer that must be strictly positive.
 * Common uses include population sizes, sample counts, and dimensions.
 * 
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public class PositiveInt extends NonNegativeInt {
    public static final PositiveInt INSTANCE = new PositiveInt();

    protected PositiveInt() {}

//    @Override
//    public boolean isValid(Integer value) {
//        return Int.INSTANCE.isValid(value) && value > getLower();
//    }

    @Override
    public boolean lowerInclusive() {
        return false;
    }
}
