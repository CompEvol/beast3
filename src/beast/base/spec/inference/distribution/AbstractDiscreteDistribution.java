package beast.base.spec.inference.distribution;

import beast.base.core.Input;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.Int;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Vector;
import beast.base.util.Randomizer;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.exception.OutOfRangeException;


public abstract class AbstractDiscreteDistribution<D extends Int> extends CalculationNode implements ParamDistInterface<D, Integer> {

    public final Input<Integer> offsetInput = new Input<>("offset", "offset of origin (defaults to 0)", 0);

    abstract IntegerDistribution getDistribution();

//    @Override
//    double calcLogP(Scalar<D, Integer> scalar);
//
//    @Override
//    double calcLogP(Vector<D, Integer> vector);

    /**
     * Calculate log probability of a valuable x for this distribution.
     * If x is multidimensional, the components of x are assumed to be independent,
     * so the sum of log probabilities of all elements of x is returned as the prior.
     */


    public double calcLogP(final Scalar<D, Integer> scalar) {
        double logP = 0;
        final int x = scalar.get();
        logP += logProb(x);
        return logP;
    }

    public double calcLogP(final Vector<D, Integer> vector) {
        double logP = 0;
        for (int i = 0; i < vector.size(); i++) {
            final int x = vector.get(i);
            logP += logProb(x);
        }
        return logP;
    }
    /*
     * This implementation is only suitable for univariate distributions.
     * Must be overwritten for multivariate ones.
     * @size sample size = number of samples to produce
     */
    public Integer[][] sample(final int size) throws OutOfRangeException {
        final Integer[][] sample = new Integer[size][];
        for (int i = 0; i < sample.length; i++) {
            final int p = Randomizer.nextInt();
            sample[i] = new Integer[]{inverseCumulativeProbability(p)};
        }
        return sample;

    }

//    int[] sample(int size);


    public double probability(int x) {
        x -= getOffset();
        return getDistribution().probability(x);
    }

    public double logProb(int x) {
        x -= getOffset();
        final double probability = getDistribution().probability(x);
        if( probability > 0 )
            return Math.log(probability);

        return Double.NEGATIVE_INFINITY;
    }



    double cumulativeProbability(int x) throws OutOfRangeException {
        return getDistribution().cumulativeProbability(x);
    }

    double cumulativeProbability(int x0, int x1) throws OutOfRangeException {
        return getDistribution().cumulativeProbability(x0, x1);
    }

    int inverseCumulativeProbability(int p) throws OutOfRangeException {
        return getDistribution().inverseCumulativeProbability(p);
    }


    public int getOffset() {
        return offsetInput.get();
    }

    //TODO ???
    public double getMeanWithoutOffset() {
        throw new RuntimeException("Not implemented yet");
    }

    public abstract double getMean();

    public abstract double getVariance();
}
