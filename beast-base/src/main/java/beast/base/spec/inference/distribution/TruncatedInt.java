package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.type.IntScalar;
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
        if (!isValid((int) x)) {
            return Double.NEGATIVE_INFINITY;
        }
        // the mass of the inner distribution on [lower, upper], both ends included
        double mass = getUpperCDF() - getLowerCDF();
        if (mass <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        return dist.calcLogP((int) x) - Math.log(mass);
    }

    @Override
    public double density(double x) {
        return Math.exp(logDensity(x));
    }

    @Override
    public double cumulativeProbability(double x) {
        // P(X <= x) only changes at integers
        x = Math.floor(x);
        if (x < lower.get()) {
            return 0.0;
        } else if (x >= upper.get()) {
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
        int x = getInnerDistribution().inverseCumulativeProbability(adjustedP);
        // adjustedP = lowerP (p = 0) maps to a value below lower, and rounding near
        // upperP can map just past upper, so keep the result inside [lower, upper]
        return Math.max(lower.get(), Math.min(upper.get(), x));
    }

    public ScalarDistribution<IntScalar<Int>, Integer> getInnerDistribution() {
    	if (dist == null) {
    		refresh();
    	}
        return dist;
    }

    @Override
    public Integer getLowerBoundOfParameter() {
        // Always refresh() first (getInnerDistribution() only does when dist is null), like
        // ScalarDistribution's default via getApacheDistribution(), so the bounds follow an inner
        // distribution or a truncation lower/upper replaced after initialisation (e.g. in BEAUti).
        refresh();
        return Math.max(lower.get(), dist.getLowerBoundOfParameter());
    }

    @Override
    public Integer getUpperBoundOfParameter() {
        // Always refresh() first (getInnerDistribution() only does when dist is null), like
        // ScalarDistribution's default via getApacheDistribution(), so the bounds follow an inner
        // distribution or a truncation lower/upper replaced after initialisation (e.g. in BEAUti).
        refresh();
        // the smaller of the truncation upper and the inner upper bound
        // (this used to return Math.max(lower, inner upper), ignoring the truncation upper)
        return Math.min(upper.get(), dist.getUpperBoundOfParameter());
    }

    /**
     * @return P(X < lower) = P(X <= lower - 1) for the inner distribution, so that
     * lower itself is inside the truncation interval
     */
    double getLowerCDF() {
        int l = lower.get();
        if (l == Integer.MIN_VALUE) {
            // nothing lies below the smallest int, and l - 1 would wrap around
            return 0.0;
        }
        return getInnerDistribution().cumulativeProbability(l - 1);
    }

    double getUpperCDF() {
        return getInnerDistribution().cumulativeProbability(upper.get());
    }

    
    @Override
	public List<Integer> sample() {
        refresh();
        // inverse transform sampling: a uniform draw on [0, 1) mapped through the
        // inverse CDF of the truncated distribution lands in [lower, upper]
        double u = rng.nextDouble();
        int x = inverseCumulativeProbability(u);
        assert isValid(x);
        // Returning an immutable result
        return List.of(x);
    }

    @Override
    public double getMean() {
        refresh();
        // sum over the values that carry all but a negligible part of the truncated mass;
        // the tails beyond these quantiles cannot shift the mean noticeably
        long from = inverseCumulativeProbability(0.0);
        long to = inverseCumulativeProbability(1.0 - 1e-12);
        // exact for up to MAX_MEAN_TERMS values; beyond that, approximate by visiting
        // every step-th value (shown in BEAUti, so it should not fail)
        long step = 1 + (to - from) / MAX_MEAN_TERMS;
        double mean = 0;
        double mass = 0;
        for (long i = from; i <= to; i += step) {
            double p = dist.density(i);
            mean += i * p;
            mass += p;
        }
        return mean / mass;
    }

    /** largest number of values getMean() sums over exactly */
    private static final long MAX_MEAN_TERMS = 10_000_000L;

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
