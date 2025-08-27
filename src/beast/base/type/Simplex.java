package beast.base.type;

import beast.base.type.domain.UnitInterval;

/**
 * Simplex type - vector of probabilities that sum to 1.
 *
 * Represents a probability distribution over a finite set of outcomes.
 * Common uses in phylogenetics:
 * - Base frequencies (DNA: 4 values, Protein: 20 values)
 * - State frequencies in discrete trait models
 * - Mixture weights in mixture models
 * - Category probabilities in rate heterogeneity models
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public interface Simplex<P extends UnitInterval> extends Vector<P> {

    // Invariant: sum(elements) = 1.0, all elements in [0,1]
    default double sum() {
        double s = 0;
        for(int i = 0; i < size(); i++) {
            s += getDoubleValue(i);
        }
        return s;
    }

    default boolean isValid() {
        double s = sum();
        if (Math.abs(s - 1.0) > 1e-10)
            return false;

        for(int i = 0; i < size(); i++) {
            double v = getDoubleValue(i);
            if (v < 0 || v > 1)
                return false;
        }
        return true;
    }

}
