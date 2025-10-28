package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.Int;
import beast.base.util.Randomizer;
import org.apache.commons.statistics.distribution.DiscreteDistribution;

/**
 * The BEAST Distribution over an Int tensor.
 * @param <D> domain extends {@link Int}
 */
@Description("The BEAST Distribution over an Int tensor.")
public abstract class IntTensorDistribution<D extends Int> extends TensorDistribution<D, Integer> {

    public final Input<Integer> offsetInput = new Input<>("offset",
            "offset of origin (defaults to 0)", 0);

    abstract DiscreteDistribution getDistribution();

    /*
     * This implementation is only suitable for univariate distributions.
     * Must be overwritten for multivariate ones.
     * @size sample size = number of samples to produce
     */
    @Override
    public Integer[][] sample(final int size) {
        final Integer[][] sample = new Integer[size][];
        for (int i = 0; i < sample.length; i++) {
            final int p = Randomizer.nextInt();
            sample[i] = new Integer[]{inverseCumulativeProbability(p)};
        }
        return sample;

    }

    /**
     * Used by BEAST {@link #calculateLogP()}
     * @param x Point at which the PMF is evaluated.
     * @return  the logarithm of the value of the probability mass function at x after offset.
     */
    @Override
    public double logProb(Integer x) {
        return this.logProbability(x);
    }

    //*** wrap Apache Stats methods to handle offset ***//

    public double probability(int x) {
        x -= getOffset();
        return getDistribution().probability(x);
    }

    public double probability(int x0, int x1) {
        x0 -= getOffset();
        x1 -= getOffset();
        return getDistribution().probability(x0, x1);
    }

    public double logProbability(int x) {
        x -= getOffset();
        return getDistribution().logProbability(x);
    }

    public double cumulativeProbability(int x) {
        x -= getOffset();
        return getDistribution().cumulativeProbability(x);
    }

    public double survivalProbability(int x) {
        x -= getOffset();
        return getDistribution().survivalProbability(x);
    }

    public int inverseCumulativeProbability(double p) {
        int offset = getOffset();
        return offset + getDistribution().inverseCumulativeProbability(p);
    }

    public int inverseSurvivalProbability(double p) {
        int offset = getOffset();
        return offset + getDistribution().inverseSurvivalProbability(p);
    }

    public Integer getOffset() {
        return offsetInput.get();
    }

    public double getMeanWithoutOffset() {
        return getDistribution().getMean();
    }

    /** returns mean of distribution, if implemented **/
    public double getMean() {
        return getMeanWithoutOffset() + getOffset();
    }

    public double getVariance(){
        return getDistribution().getVariance();
    }

}

