package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.type.IntScalar;

import java.util.List;

import org.apache.commons.math.MathException;
import org.apache.commons.statistics.distribution.DiscreteDistribution;
import org.apache.commons.statistics.distribution.UniformDiscreteDistribution;


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

//        if (offsetInput.get() != 0.0)
//            throw new IllegalArgumentException("Non-zero offset not allowed for " + getClass().getName() + 
//                    " distribution. Set offset in base distribution (distributionInput) instead");

        // Set bounds in parameter in the future (for early reject in operators)
//        IntScalar<Int> param = paramInput.get();
//        if (param instanceof IntScalarParam<Int> p) {
//            p.setLower(lower.get());
//            p.setUpper(upper.get());
//        }
    }

    /**
     * make sure internal state is up to date *
     */
    @Override
    public void refresh() {
        dist = distributionInput.get();
        lower = lowerInput.get();
        if (lower == null)
            lower = new IntScalarParam<>(Int.INSTANCE.getLower(), Int.INSTANCE);

        upper = upperInput.get();
        if (upper == null)
            upper = new IntScalarParam<>(Int.INSTANCE.getUpper(), Int.INSTANCE);
    }

    @Override
    public double calculateLogP() {
        logP = logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

	@Override
	protected double calcLogP(Integer value) {
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
    public double inverseCumulativeProbability(double p) throws MathException {
        double lowerP = getLowerCDF();
        double upperP = getUpperCDF();
        p = lowerP + (upperP - lowerP) * p;
    	double x = super.inverseCumulativeProbability(p);
    	x = Math.max(x, lower.get());
    	x = Math.min(x, upper.get());
    	return x;
    }

    // Get the Apache distribution of the inner distribution object (provides inverse CDF)
    DiscreteDistribution getInnerDistribution() {
        return (DiscreteDistribution) dist.getApacheDistribution();
    }

    double getLowerCDF() {
        return getInnerDistribution().cumulativeProbability(lower.get());// - dist.getOffset());
    }

    double getUpperCDF() {
        return getInnerDistribution().cumulativeProbability(upper.get());// - dist.getOffset());
    }

    double probOutOfBounds() {
        double probOOB = getLowerCDF() + (1-getUpperCDF());
        assert 0.0 <= probOOB && probOOB <= 1.0;
        // System.out.println("*" + probOOB);
        return probOOB;
    }
    
    @Override
    protected List<Integer> sample() {
        // Sample CDF value that is compatible with the valid interval [lower, upper] 
        int uLower = lower.get();
        int uUpper = upper.get();
        UniformDiscreteDistribution uDist = UniformDiscreteDistribution.of(uLower, uUpper);
        double u = uDist.createSampler(rng).sample();

        // Transform using inverse CDF of the inner distribution
        int x = (int) (getInnerDistribution().inverseCumulativeProbability(u));

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
        DiscreteDistribution dist = getInnerDistribution();
        //double offset = dist.getOffset();
        int from = lower.get();// - offset) + EPS;  // avoid CDF of 0
        int to = upper.get();// - offset) - EPS;  // avoid CDF of 1
        
        int diff = to - from;
        int step = 1 + diff / 1000;
        
        int i = from;
        double mean = 0;
        while (i <= to) {
        	mean += i *  dist.probability(i) * step;
        	i += step;
        }
        return mean;
    }

    @Override
    public Object getApacheDistribution() {
    	if (dist == null) {
    		refresh();
    	}
    	return distributionInput.get().getApacheDistribution();
    }

    boolean isValid(int value) {
        return Int.INSTANCE.isValid(value) && lower.get() <= value && value <= upper.get();
    }


    @Override
    public boolean isIntegerDistribution() {
    	return true;
    }

    @Override
    public Integer getLower() {
    	DiscreteDistribution dist = getInnerDistribution();
    	if (dist == null) {
    		refresh();
    		dist = getInnerDistribution();
    	}
        return (int) (Math.max(lower.get(), dist.getSupportLowerBound()));
    }

    @Override
    public Integer getUpper() {
    	DiscreteDistribution dist = getInnerDistribution();
    	if (dist == null) {
    		refresh();
    		dist = getInnerDistribution();
    	}
        return (int) (Math.max(lower.get(), dist.getSupportUpperBound()));
    }

    
} // class TruncatedIntDistribution
