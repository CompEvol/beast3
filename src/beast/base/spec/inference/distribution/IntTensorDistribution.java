package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.Int;
import beast.base.spec.type.Tensor;
import org.apache.commons.statistics.distribution.DiscreteDistribution;

import java.util.ArrayList;
import java.util.List;

/**
 * The BEAST Distribution over an Int tensor.
 * @param <D> domain extends {@link Int}
 */
@Description("The BEAST Distribution over an Int tensor.")
public abstract class IntTensorDistribution<S extends Tensor<D, Integer>, D extends Int>
        extends TensorDistribution<S, D, Integer> {

    public final Input<Integer> offsetInput = new Input<>("offset",
            "offset of origin (defaults to 0)", 0);

    public abstract DiscreteDistribution getDistribution();

    protected abstract S valueToTensor(int value);

    /*
     * This implementation is only suitable for univariate distributions.
     * Must be overwritten for multivariate ones.
     * @size sample size = number of samples to produce
     */
    @Override
    public List<S> sample(final int size) {
        DiscreteDistribution.Sampler sampler = getDistribution().createSampler(rng);
        List<S> samples = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            final int x = sampler.sample() + getOffset();
            samples.add(valueToTensor(x));
        }
        return samples;
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

