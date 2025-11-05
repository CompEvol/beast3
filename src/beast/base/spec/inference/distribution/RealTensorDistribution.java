package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.Real;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;
import beast.base.spec.type.Vector;
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

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        calculateLogP();
    }

    protected abstract ContinuousDistribution getDistribution();

    protected abstract S valueToTensor(double... value);

    /**
     * Override {@link beast.base.inference.Distribution#calculateLogP()}.
     * Parameter value is wrapped by tensor S.
     * @return the normalized probability (density) for this distribution.
     */
    @Override
    public double calculateLogP() {
        logP = 0;
        param = paramInput.get();
        switch (param) {
            case Scalar scalar -> {
                if (!scalar.isValid(scalar.get())) return Double.NEGATIVE_INFINITY;
                final double x = ((S) scalar).get();
                logP += this.logDensity(x);
            }
            case Vector vector -> {
                if (!vector.isValid())
                    return Double.NEGATIVE_INFINITY;
                for (int i = 0; i < vector.size(); i++) {
                    final double x = ((S) vector).get(i);
                    logP += this.logDensity(x);
                }
            }
            default -> throw new IllegalStateException("Unexpected tensor type");
        }
        return logP;
    }

    /*
     * This implementation is only suitable for univariate distributions.
     * Must be overwritten for multivariate ones.
     * @size sample size = number of samples to produce
     */
    @Override
    public List<S> sample(final int size) {
        param = paramInput.get();
        ContinuousDistribution.Sampler sampler = getDistribution().createSampler(rng);
        List<S> samples = new ArrayList<>(size);

        for (int s = 0; s < size; s++) {
            switch (param) {
                case Scalar scalar -> {
                    final double x = sampler.sample() + getOffset();
                    if (!scalar.isValid(x))
                        throw new IllegalStateException("Invalid sample value : " + x);
                    samples.add(valueToTensor(x));
                }
                case Vector vector -> {
                    final double[] x = new double[vector.size()];
                    for (int i = 0; i < vector.size(); i++) {
                        x[i] = sampler.sample() + getOffset();
                        if (!vector.isValid(x))
                            throw new IllegalStateException("Invalid sample value : " + x[i] +
                                    " at " + i);
                    }
                    samples.add(valueToTensor(x));
                }
                default -> throw new IllegalStateException("Unexpected tensor type");
            }
        } // end for loop
        return samples;
    }


    //*** wrap Apache Stats methods to handle offset ***//

    public double density(double... x) {
        double logPdf = logDensity(x);
        return Math.exp(logPdf);
    }

    /**
     * This is used by {@link #calculateLogP()}, which overrides the method
     * in {@link beast.base.inference.Distribution}.
     * @param x   value of scalar or vector
     * @return    the natural logarithm of the probability density function (PDF)
     *            of this distribution evaluated at the specified point x.
     *            If the offset is given, this will be log-density after taking offset from x.
     */
    public double logDensity(double... x) {
        double logP = 0;
        for (int i = 0; i < x.length; i++) {
            x[i] -= getOffset();
            logP += getDistribution().logDensity(x[i]);
        }
        return logP;
    }

//    public double probability(double x0, double x1) {
//        x0 -= getOffset();
//        x1 -= getOffset();
//        return getDistribution().probability(x0, x1);
//    }
//
//    public double cumulativeProbability(double x) {
//        x -= getOffset();
//        return getDistribution().cumulativeProbability(x);
//    }
//
//    public double survivalProbability(double x) {
//        x -= getOffset();
//        return getDistribution().survivalProbability(x);
//    }
//
//    public double inverseCumulativeProbability(double p) {
//        double offset = getOffset();
//        return offset + getDistribution().inverseCumulativeProbability(p);
//    }
//
//    public double inverseSurvivalProbability(double p) {
//        double offset = getOffset();
//        return offset + getDistribution().inverseSurvivalProbability(p);
//    }

    public Double getOffset() {
        return offsetInput.get();
    }

    protected double getMeanWithoutOffset() {
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

