package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.Real;
import beast.base.spec.type.Tensor;
import org.apache.commons.statistics.distribution.ContinuousDistribution;

import java.util.ArrayList;
import java.util.List;

/**
 * The BEAST Distribution over a Real tensor.
 * @param <D> domain extends {@link Real}
 */
@Description("The BEAST Distribution over a Real tensor.")
public abstract class RealTensorDistribution<S extends Tensor<D, Double>, D extends Real>
        extends TensorDistribution<S, D, Double> {

    public final Input<Double> offsetInput = new Input<>("offset",
            "offset of origin (defaults to 0)", 0.0);

    abstract ContinuousDistribution getDistribution();

    protected abstract S valueToTensor(double value);

    /*
     * This implementation is only suitable for univariate distributions.
     * Must be overwritten for multivariate ones.
     * @size sample size = number of samples to produce
     */
    @Override
    public List<S> sample(final int size) {
        ContinuousDistribution.Sampler sampler = getDistribution().createSampler(rng);
        List<S> samples = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            final double x = sampler.sample() + getOffset();
            samples.add(valueToTensor(x));
        }
        return samples;
    }

    /**
     * Used by BEAST {@link #calculateLogP()}
     *
     * @param x Point at which the PMF is evaluated.
     * @return the logarithm of the value of the probability mass function at x after offset.
     */
    @Override
    public double logProb(Double x) {
        return this.logDensity(x);
    }

    //*** wrap Apache Stats methods to handle offset ***//

    public double density(int x) {
        x -= getOffset();
        return getDistribution().density(x);
    }

    public double probability(double x0, double x1) {
        x0 -= getOffset();
        x1 -= getOffset();
        return getDistribution().probability(x0, x1);
    }

    public double logDensity(double x) {
        x -= getOffset();
        return getDistribution().logDensity(x);
    }

    public double cumulativeProbability(double x) {
        x -= getOffset();
        return getDistribution().cumulativeProbability(x);
    }

    public double survivalProbability(double x) {
        x -= getOffset();
        return getDistribution().survivalProbability(x);
    }

    public double inverseCumulativeProbability(double p) {
        double offset = getOffset();
        return offset + getDistribution().inverseCumulativeProbability(p);
    }

    public double inverseSurvivalProbability(double p) {
        double offset = getOffset();
        return offset + getDistribution().inverseSurvivalProbability(p);
    }

    public Double getOffset() {
        return offsetInput.get();
    }

    public double getMeanWithoutOffset() {
        return getDistribution().getMean();
    }

    /**
     * returns mean of distribution, if implemented
     **/
    public double getMean() {
        return getMeanWithoutOffset() + getOffset();
    }

    public double getVariance() {
        return getDistribution().getVariance();
    }

}

