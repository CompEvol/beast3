package beast.base.inference.distribution;


import beast.base.core.Description;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.ContinuousDistribution;


/**
 * @deprecated 1/x can be simulated. Use other distributions.
 */
@Deprecated
@Description("OneOnX distribution.  f(x) = C/x for some normalizing constant C. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class OneOnX extends ParametricDistribution {

    ContinuousDistribution dist = new OneOnXImpl();

    @Override
    public void initAndValidate() {
    }

    @Override
    public Object getDistribution() {
        return dist;
    }

    class OneOnXImpl implements ContinuousDistribution {

        @Override
        public double cumulativeProbability(double x) {
            throw new UnsupportedOperationException("Not implemented for improper prior");
        }

        @Override
        public double inverseCumulativeProbability(double p) {
            throw new UnsupportedOperationException("Not implemented for improper prior");
        }

        @Override
        public double density(double x) {
            return 1 / x;
        }

        @Override
        public double logDensity(double x) {
            return -Math.log(x);
        }

        @Override
        public double getMean() {
            return Double.NaN;
        }

        @Override
        public double getVariance() {
            return Double.NaN;
        }

        @Override
        public double getSupportLowerBound() {
            return 0;
        }

        @Override
        public double getSupportUpperBound() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public Sampler createSampler(UniformRandomProvider rng) {
            throw new UnsupportedOperationException("Sampling not supported for improper prior");
        }
    } // class OneOnXImpl


} // class OneOnX
