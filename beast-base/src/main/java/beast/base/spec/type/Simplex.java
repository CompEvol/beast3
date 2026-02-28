package beast.base.spec.type;


import beast.base.spec.domain.UnitInterval;

/**
 * Simplex type - vector of probabilities that sum to 1.
 *
 * Represents a probability distribution over a finite set of outcomes.
 * Common uses in phylogenetics:
 * - Base frequencies (DNA: 4 values, Protein: 20 values)
 * - State frequencies in discrete trait models
 * - Mixture weights in mixture models
 * - Category probabilities in rate heterogeneity models
 */
public interface Simplex extends RealVector<UnitInterval> {

    /**
     * Computes the sum of all elements. For a valid simplex this should equal 1.0.
     *
     * @return the sum of all elements
     */
    default double sum() {
        double s = 0;
        for(int i = 0; i < size(); i++) {
            s += get(i);
        }
        return s;
    }

    /**
     * Validates that all elements are within [0,1] and that they sum to 1.0
     * (within a tolerance of 1e-10).
     *
     * @return {@code true} if all elements are valid and their sum equals 1.0
     */
    default boolean isValid() {
        for (int i = 0; i < size(); i++)
            if ( !isValid(get(i)))
                return false;

        double s = sum();
        if (Math.abs(s - 1.0) > 1e-10)
            return false;

        // this is duplicated with Domain validation
//        for(int i = 0; i < size(); i++) {
//            double v = get(i);
//            if (v < 0 || v > 1)
//                return false;
//        }
        return true;
    }

}
