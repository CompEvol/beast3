package beast.base.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.ContinuousDistribution;


/**
 * @deprecated replaced by {@link beast.base.spec.inference.distribution.Uniform}
 */
@Deprecated
@Description("Uniform distribution over a given interval (including lower and upper values)")
public class Uniform extends ParametricDistribution {
    final public Input<Double> lowerInput = new Input<>("lower", "lower bound on the interval, default 0", 0.0);
    final public Input<Double> upperInput = new Input<>("upper", "lower bound on the interval, default 1", 1.0);

    protected UniformImpl distr = new UniformImpl();

    protected double _lower, _upper, density;

    private boolean infiniteSupport;

    @Override
    public void initAndValidate() {
        _lower = lowerInput.get();
        _upper = upperInput.get();
        if (_lower >= _upper) {
            throw new IllegalArgumentException("Upper value should be higher than lower value");
        }
        distr.setBounds(_lower, _upper);
        infiniteSupport = Double.isInfinite(_lower) || Double.isInfinite(_upper);
        if (infiniteSupport) {
            density = 1.0;
        } else {
            density = 1.0 / (_upper - _lower);
        }
    }


    public class UniformImpl implements ContinuousDistribution {
        private double lower;
        private double upper;

        public void setBounds(final double lower, final double upper) {
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public double cumulativeProbability(double x) {
            x = Math.max(x, lower);
            return (x - lower) / (upper - lower);
        }

        @Override
        public double inverseCumulativeProbability(final double p) {
            if (p < 0.0 || p > 1.0) {
                throw new RuntimeException("inverseCumulativeProbability::argument out of range [0...1]");
            }
            if( p == 0 ) {
                // works even when one bound is infinite
                return _lower;
            }
            if( p == 1 ) {
                // works even when one bound is infinite
                return _upper;
            }
            if( infiniteSupport ) {
                 throw new RuntimeException("Inverse Cumulative Probability for 0 < p < 1 and infinite support") ;
            }
            return (upper - lower) * p + lower;
        }

        @Override
        public double density(final double x) {
            if (x >= lower && x <= upper) {
                return density;
            } else {
                return 0;
            }
        }

        @Override
        public double logDensity(final double x) {
            return Math.log(density(x));
        }

        @Override
        public double getMean() {
            if (Double.isInfinite(lower) || Double.isInfinite(upper)) {
                return Double.NaN;
            }
            return (lower + upper) / 2.0;
        }

        @Override
        public double getVariance() {
            if (Double.isInfinite(lower) || Double.isInfinite(upper)) {
                return Double.NaN;
            }
            double range = upper - lower;
            return range * range / 12.0;
        }

        @Override
        public double getSupportLowerBound() {
            return lower;
        }

        @Override
        public double getSupportUpperBound() {
            return upper;
        }

        @Override
        public Sampler createSampler(UniformRandomProvider rng) {
            return () -> inverseCumulativeProbability(rng.nextDouble());
        }
    } // class UniformImpl


    @Override
    public Object getDistribution() {
        return distr;
    }

    @Override
    public double density(final double x) {
        if (x >= _lower && x <= _upper) {
            // (BUG)?? why does this not return this.density??? (JH)
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    protected double getMeanWithoutOffset() {
    	if (Double.isInfinite(_lower) || Double.isInfinite(_upper)) {
    		return Double.NaN;
    	}
    	return (_upper + _lower)/2;
    }
}
