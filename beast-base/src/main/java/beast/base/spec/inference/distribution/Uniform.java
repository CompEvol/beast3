package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.UniformContinuousDistribution;

import java.util.List;

/**
 * Continuous uniform distribution over a finite interval [lower, upper].
 * Both endpoints are included. Infinite bounds are not supported.
 */
@Description("Uniform distribution over a given interval (including lower and upper values). " +
        "An exception is thrown if any bound is set to infinity.")
public class Uniform extends ScalarDistribution<RealScalar<Real>, Double> implements Bounded<Double> {

    final public Input<RealScalar<Real>> lowerInput = new Input<>("lower",
            "lower bound on the interval, default 0. An exception is thrown if set to -Infinity");
    final public Input<RealScalar<Real>> upperInput = new Input<>("upper",
            "upper bound on the interval, default 1. An exception is thrown if set to Infinity");

    // if (!Double.isFinite(upper - lower)) {
    //    throw new DistributionException("Range %s is not finite", upper - lower);
    private UniformContinuousDistribution dist = UniformContinuousDistribution.of(0.0, 1.0);
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Uniform() {}

    /**
     * Uniform distribution over a given interval (including lower and upper values)
     * @param param
     * @param lower An exception is thrown if set to -Infinity.
     * @param upper An exception is thrown if set to Infinity.
     */
    public Uniform(RealScalar<Real> param,
                RealScalar<Real> lower, RealScalar<Real> upper) {

        try {
            initByName("param", param, "lower", lower, "upper", upper);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate();
    }

    /**
     * make sure internal state is up to date *
     */
    @Override
    public void refresh() {
        double lower  = (lowerInput.get() != null) ? lowerInput.get().get() : 0.0;
        double upper  = (upperInput.get()  != null) ? upperInput.get().get()  : 1.0;

        // Floating point comparison
        if (isNotEqual(dist.getSupportLowerBound(), lower)
                || isNotEqual(dist.getSupportUpperBound(), upper)) {
            dist = UniformContinuousDistribution.of(lower, upper);
        }
    }

    @Override
    public double calculateLogP() {
        logP = getApacheDistribution().logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
	public List<Double> sample() {
        if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
        final double x = sampler.sample();
        return List.of(x); // Returning an immutable result
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
	protected UniformContinuousDistribution getApacheDistribution() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        return dist;
    }

	@Override
	public Double getLower() {
        double lower  = (lowerInput.get() != null) ? lowerInput.get().get() : 0.0;
        return lower;
	}

	@Override
	public Double getUpper() {
        double upper  = (upperInput.get()  != null) ? upperInput.get().get()  : 1.0;
		return upper;
	}
    
}
