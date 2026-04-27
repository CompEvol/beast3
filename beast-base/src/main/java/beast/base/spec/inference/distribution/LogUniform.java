package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.Bounded;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.ContinuousDistribution;

import java.util.List;

/**
 * Log-uniform (reciprocal) distribution on [lower, upper] with lower > 0.
 * Equivalent to a uniform distribution on log(x). The density is
 * p(x) = 1 / (x * log(upper/lower)) on [lower, upper], zero elsewhere.
 * Commonly used as a scale-invariant proper prior for strictly positive
 * quantities (population sizes, rates, variances), replacing the improper
 * OneOnX prior when a finite support is acceptable.
 */
@Description("Log-uniform distribution on [lower, upper], lower > 0. " +
        "Density is 1/(x log(upper/lower)). Equivalent to a uniform distribution " +
        "on log(x). Provides a proper, scale-invariant prior for strictly positive " +
        "quantities over many orders of magnitude.")
public class LogUniform extends ScalarDistribution<RealScalar<PositiveReal>, Double>
        implements Bounded<Double> {

    final public Input<RealScalar<PositiveReal>> lowerInput = new Input<>("lower",
            "lower bound of the support, strictly positive. Defaults to 1.");
    final public Input<RealScalar<PositiveReal>> upperInput = new Input<>("upper",
            "upper bound of the support, must exceed lower. Defaults to e.");

    private LogUniformImpl dist = new LogUniformImpl(1.0, Math.E);
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public LogUniform() {}

    public LogUniform(RealScalar<PositiveReal> param,
                      RealScalar<PositiveReal> lower, RealScalar<PositiveReal> upper) {
        try {
            initByName("param", param, "lower", lower, "upper", upper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e);
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate();
    }

    @Override
    public void refresh() {
        double lower = (lowerInput.get() != null) ? lowerInput.get().get() : 1.0;
        double upper = (upperInput.get() != null) ? upperInput.get().get() : Math.E;

        if (!(lower > 0.0) || !Double.isFinite(upper) || !(upper > lower)) {
            throw new IllegalArgumentException(
                    "LogUniform requires 0 < lower < upper < infinity, got [" +
                            lower + ", " + upper + "]");
        }

        if (isNotEqual(dist.lower, lower) || isNotEqual(dist.upper, upper)) {
            dist = new LogUniformImpl(lower, upper);
            sampler = null;
        }
    }

    @Override
    public double calculateLogP() {
        logP = getApacheDistribution().logDensity(param.get());
        return logP;
    }

    @Override
    public List<Double> sample() {
        if (sampler == null) {
            sampler = dist.createSampler(rng);
        }
        final double x = sampler.sample();
        return List.of(x);
    }

    @Override
    protected LogUniformImpl getApacheDistribution() {
        refresh();
        return dist;
    }

    @Override
    public boolean lowerInclusive() {
        return true;
    }

    @Override
    public boolean upperInclusive() {
        return true;
    }

    @Override
    public Double getLower() {
        return (lowerInput.get() != null) ? lowerInput.get().get() : 1.0;
    }

    @Override
    public Double getUpper() {
        return (upperInput.get() != null) ? upperInput.get().get() : Math.E;
    }

    /**
     * Apache Commons Statistics does not provide a LogUniform/reciprocal
     * distribution, so implement the interface directly.
     */
    static final class LogUniformImpl implements ContinuousDistribution {

        final double lower;
        final double upper;
        final double logRange; // log(upper / lower), strictly positive

        LogUniformImpl(double lower, double upper) {
            this.lower = lower;
            this.upper = upper;
            this.logRange = Math.log(upper / lower);
        }

        @Override
        public double density(double x) {
            if (x < lower || x > upper) return 0.0;
            return 1.0 / (x * logRange);
        }

        @Override
        public double logDensity(double x) {
            if (x < lower || x > upper) return Double.NEGATIVE_INFINITY;
            return -Math.log(x) - Math.log(logRange);
        }

        @Override
        public double cumulativeProbability(double x) {
            if (x <= lower) return 0.0;
            if (x >= upper) return 1.0;
            return Math.log(x / lower) / logRange;
        }

        @Override
        public double inverseCumulativeProbability(double p) {
            if (p < 0.0 || p > 1.0) {
                throw new IllegalArgumentException("p must be in [0,1], got " + p);
            }
            if (p == 0.0) return lower;
            if (p == 1.0) return upper;
            return lower * Math.exp(p * logRange);
        }

        @Override
        public double getMean() {
            // E[X] = (upper - lower) / log(upper/lower)
            return (upper - lower) / logRange;
        }

        @Override
        public double getVariance() {
            // E[X^2] = (upper^2 - lower^2) / (2 log(upper/lower))
            double ex2 = (upper * upper - lower * lower) / (2.0 * logRange);
            double mean = getMean();
            return ex2 - mean * mean;
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
    }
}
