package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.type.IntScalar;
import org.apache.commons.statistics.distribution.UniformDiscreteDistribution;

import java.util.List;


/**
 * Wrapper that truncates an integer-valued distribution to the interval [lower, upper].
 */
@Description("Distribution that truncates a integer valued distribution to the interval [lower,upper].")
public class TruncatedInt extends ScalarDistribution<IntScalar<Int>, Integer> {

    final public Input<ScalarDistribution<IntScalar<Int>, Integer>> distributionInput = new Input<>("distribution",
            "precision of the normal distribution, defaults to 1", Validate.REQUIRED);
    final public Input<IntScalar<Int>> lowerInput = new Input<IntScalar<Int>>("lower", "Lower end of the truncation interval.");
    final public Input<IntScalar<Int>> upperInput = new Input<IntScalar<Int>>("upper", "Upper end of the truncation interval.");

    private ScalarDistribution<IntScalar<Int>, Integer> dist;
    IntScalar<Int> lower, upper;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public TruncatedInt() {
    }

    public TruncatedInt(
        ScalarDistribution<IntScalar<Int>, Integer> dist,
        int lower,
        int upper
    ) {
        assert (Int.INSTANCE.getLower() <= lower) && (lower < upper) && (upper <= Int.INSTANCE.getUpper());

        try {
            initByName(
                "distribution", dist, 
                "lower", new IntScalarParam<Int>(lower, Int.INSTANCE),
                "upper", new IntScalarParam<Int>(upper, Int.INSTANCE)
            );
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        dist.initAndValidate();
        super.initAndValidate();
    }

    /**
     * make sure internal state is up to date *
     */
    @Override
    public void refresh() {
        dist = distributionInput.get();
        dist.refresh();

        lower = lowerInput.get();
        if (lower == null)
            lower = new IntScalarParam<>(Int.INSTANCE.getLower(), Int.INSTANCE);

        upper = upperInput.get();
        if (upper == null)
            upper = new IntScalarParam<>(Int.INSTANCE.getUpper(), Int.INSTANCE);
    }

    @Override
    public double calculateLogP() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        logP = logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

	@Override
	protected double calcLogP(Integer value) {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
		return logDensity(value);
	}

    @Override
    public double logDensity(double x) {
        if (isValid((int) x))
            return dist.calcLogP((int) x) - Math.log(1 - probOutOfBounds());
        else
            return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double density(double x) {
        return Math.exp(logDensity(x));
    }

    @Override
    public double cumulativeProbability(double x) {
        if (x < lower.get()) {
            return 0.0;
        } else if (x > upper.get()) {
            return 1.0;
        } else {
            double lowerP = getLowerCDF();
            double upperP = getUpperCDF();
            double p = getInnerDistribution().cumulativeProbability(x);
            return (p - lowerP) / (upperP - lowerP);
        }
    }
    
    @Override
    public Integer inverseCumulativeProbability(double p) {
        double lowerP = getLowerCDF();
        double upperP = getUpperCDF();
        double adjustedP = lowerP + p * (upperP - lowerP);
        return getInnerDistribution().inverseCumulativeProbability(adjustedP);
    }

    public ScalarDistribution<IntScalar<Int>, Integer> getInnerDistribution() {
    	if (dist == null) {
    		refresh();
    	}
        return dist;
    }

    @Override
    public Integer getLowerBoundOfParameter() {
        return Math.max(lower.get(), getInnerDistribution().getLowerBoundOfParameter());
    }

    @Override
    public Integer getUpperBoundOfParameter() {
        return Math.max(lower.get(), getInnerDistribution().getUpperBoundOfParameter());
    }

    double getLowerCDF() {
        return getInnerDistribution().cumulativeProbability(lower.get());
    }

    double getUpperCDF() {
        return getInnerDistribution().cumulativeProbability(upper.get());
    }

    double probOutOfBounds() {
        double probOOB = getLowerCDF() + (1-getUpperCDF());
        assert 0.0 <= probOOB && probOOB <= 1.0;
        return probOOB;
    }
    
    @Override
	public List<Integer> sample() {
        // Sample CDF value that is compatible with the valid interval [lower, upper] 
        int uLower = lower.get();
        int uUpper = upper.get();
        UniformDiscreteDistribution uDist = UniformDiscreteDistribution.of(uLower, uUpper);
        double u = uDist.createSampler(rng).sample();

        // Transform using inverse CDF of the inner distribution
        int x;
        try {
            x = getInnerDistribution().inverseCumulativeProbability(u);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sample from truncated distribution", e);
            // TODO use rejection sampling as fallback?
        }

        // Alternative implementation using rejection sampling (less efficient, but simpler logic):
        // double x = dist.sample().get(0);
        // int iterations = 0;
        // while (!isValid(x)) {
        //     if (iterations >= 10000) {
        //     throw new RuntimeException(
        //         "Failed to sample from truncated distribution after " + maxIterations + 
        //         " attempts. The truncation interval [" + lower.get() + ", " + upper.get() + 
        //         "] may have negligible probability mass.");
        //     }
        //     x = dist.sample().get(0);
        //     iterations++;
        // }

        // Assert that the resulting sample is valid
        assert isValid(x);

        // Returning an immutable result
        return List.of(x);
    }

    @Override
    public double getMean() {
        refresh();
        ScalarDistribution<IntScalar<Int>, Integer> dist = getInnerDistribution();
        int from = lower.get();
        int to = upper.get();
        int diff = to - from;
        int step = 1 + diff / 1000;
        
        int i = from;
        double mean = 0;
        while (i <= to) {
        	mean += i *  dist.density(i) * step;
        	i += step;
        }
        return mean;
    }

    @Override
	protected Object getApacheDistribution() {
    	throw new RuntimeException("Not implemented for TruncatedInt distribution");
    }

    boolean isValid(int value) {
        return Int.INSTANCE.isValid(value) && lower.get() <= value && value <= upper.get();
    }

    @Override
    public boolean isIntegerDistribution() {
    	return true;
    }

    
} // class TruncatedInt
