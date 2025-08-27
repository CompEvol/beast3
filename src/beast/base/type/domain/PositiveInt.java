package beast.base.type.domain;

/**
 * Positive integer type (> 0).
 * 
 * Represents an integer that must be strictly positive.
 * Common uses include population sizes, sample counts, and dimensions.
 * 
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public interface PositiveInt extends NonNegativeInt {

    @Override
    default public boolean isValid(Integer value) {
        return value != null && value > 0;
    }
}
