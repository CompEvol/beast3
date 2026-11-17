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
public class TruncatedInt<D extends Int> extends ScalarDistribution<IntScalar<D>, Integer> {

    // any int distribution, whatever the domain of its own parameter
    final public Input<ScalarDistribution<? extends IntScalar<?>, Integer>> distributionInput = new Input<>("distribution",
            "the distribution to truncate", Validate.REQUIRED);
    final public Input<IntScalar<?>> lowerInput = new Input<>("lower", "Lower end of the truncation interval.");
    final public Input<IntScalar<?>> upperInput = new Input<>("upper", "Upper end of the truncation interval.");

    public final Input<? extends Int> domainInput = new Input<>("domain",
            "the domain of the values this distribution puts probability on, e.g. PositiveInt for Poisson " +
            "truncated to [1, 10] (optional). The truncated support must lie within it: it is checked when " +
            "initialised, and the log probability is -Infinity while estimated bounds put it outside.");

    private ScalarDistribution<? extends IntScalar<?>, Integer> dist;
    IntScalar<?> lower, upper;
    private Int declaredDomain;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public TruncatedInt() {
    }

    public TruncatedInt(
        ScalarDistribution<? extends IntScalar<?>, Integer> dist,
        int lower,
        int upper
    ) {
        // lower == upper is allowed: all the mass is then on that one value
        assert (Int.INSTANCE.getLower() <= lower) && (lower <= upper) && (upper <= Int.INSTANCE.getUpper());

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
        declaredDomain = domainInput.get();
        if (declaredDomain != null) {
            String outside = supportOutsideDeclaredDomain();
            if (outside != null) {
                throw new IllegalArgumentException(DeclaredDomain.message(this, getID(), declaredDomain, outside));
            }
        }
        super.initAndValidate();
    }

    /**
     * @return the declared domain of the values this distribution puts probability on, or
     * Int when none is declared
     */
    @SuppressWarnings("unchecked")
    public D getParameterDomain() {
        Int domain = domainInput.get();
        return (D) (domain != null ? domain : Int.INSTANCE);
    }

    /** @return where the truncated support reaches outside the declared domain, or null */
    private String supportOutsideDeclaredDomain() {
        return DeclaredDomain.outside(declaredDomain, getLowerBoundOfParameter(), getUpperBoundOfParameter());
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
        if (declaredDomain != null && (DeclaredDomain.canMove(lowerInput.get()) || DeclaredDomain.canMove(upperInput.get())) && supportOutsideDeclaredDomain() != null) {
            // estimated bounds have moved the support outside the declared domain
            logP = Double.NEGATIVE_INFINITY;
            return logP;
        }
        logP = logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

	@Override
	protected double calcLogP(Integer value) {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        if (declaredDomain != null && (DeclaredDomain.canMove(lowerInput.get()) || DeclaredDomain.canMove(upperInput.get())) && supportOutsideDeclaredDomain() != null) {
            return Double.NEGATIVE_INFINITY;
        }
		return logDensity(value);
	}

    @Override
    public double logDensity(double x) {
        if (!isValid((int) x)) {
            return Double.NEGATIVE_INFINITY;
        }
        double mass = mass();
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
        }
        Interval iv = interval();
        ScalarDistribution<? extends IntScalar<?>, Integer> inner = getInnerDistribution();
        if (iv.upperTail) {
            // P(lower <= X <= x) = P(X >= lower) - P(X > x)
            return (iv.atLower - inner.survivalProbability(x)) / iv.mass;
        }
        // P(lower <= X <= x) = P(X <= x) - P(X < lower)
        return (inner.cumulativeProbability(x) - iv.atLower) / iv.mass;
    }

    @Override
    public double survivalProbability(double x) {
        x = Math.floor(x);
        if (x < lower.get()) {
            return 1.0;
        } else if (x >= upper.get()) {
            return 0.0;
        }
        Interval iv = interval();
        ScalarDistribution<? extends IntScalar<?>, Integer> inner = getInnerDistribution();
        if (iv.upperTail) {
            // P(x < X <= upper) = P(X > x) - P(X > upper)
            return (inner.survivalProbability(x) - iv.atUpper) / iv.mass;
        }
        return (iv.atUpper - inner.cumulativeProbability(x)) / iv.mass;
    }

    @Override
    public Integer inverseCumulativeProbability(double p) {
        Interval iv = interval();
        ScalarDistribution<? extends IntScalar<?>, Integer> inner = getInnerDistribution();
        int x;
        if (iv.upperTail) {
            // smallest x with P(X >= lower) - P(X > x) >= p * mass
            x = inner.inverseSurvivalProbability(iv.atLower - p * iv.mass);
        } else {
            // smallest x with P(X <= x) - P(X < lower) >= p * mass
            x = inner.inverseCumulativeProbability(iv.atLower + p * iv.mass);
        }
        return clamp(x);
    }

    @Override
    public Integer inverseSurvivalProbability(double p) {
        Interval iv = interval();
        ScalarDistribution<? extends IntScalar<?>, Integer> inner = getInnerDistribution();
        int x;
        if (iv.upperTail) {
            // smallest x with P(X > x) - P(X > upper) <= p * mass
            x = inner.inverseSurvivalProbability(iv.atUpper + p * iv.mass);
        } else {
            // smallest x with P(X <= upper) - P(X <= x) <= p * mass
            x = inner.inverseCumulativeProbability(iv.atUpper - p * iv.mass);
        }
        return clamp(x);
    }

    /**
     * p = 0 maps to a value below lower, and rounding near the ends can map just
     * past them, so keep the result inside [lower, upper]
     */
    private int clamp(int x) {
        return Math.max(lower.get(), Math.min(upper.get(), x));
    }

    public ScalarDistribution<? extends IntScalar<?>, Integer> getInnerDistribution() {
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

    /** @return P(X >= lower) = P(X > lower - 1) for the inner distribution */
    double getLowerSurvival() {
        int l = lower.get();
        if (l == Integer.MIN_VALUE) {
            return 1.0;
        }
        return getInnerDistribution().survivalProbability(l - 1);
    }

    /** @return P(X > upper) for the inner distribution */
    double getUpperSurvival() {
        return getInnerDistribution().survivalProbability(upper.get());
    }

    /**
     * Whether [lower, upper] lies in the upper tail of the inner distribution. There
     * P(X <= x) is close to 1 and differences of it lose their precision, down to 0
     * (Poisson(2) from 25 on: P(X <= 24) is 1.0 in double precision), so the mass,
     * CDF and inverse are computed from the survival function P(X > x) instead.
     */
    private static boolean isUpperTail(double lowerCDF) {
        return lowerCDF > 0.5;
    }

    /**
     * @return the probability mass of the inner distribution on [lower, upper], both
     * ends included. Used by logDensity() on every MCMC step, so it does not allocate.
     */
    double mass() {
        double lowerCDF = getLowerCDF();
        if (isUpperTail(lowerCDF)) {
            return getLowerSurvival() - getUpperSurvival();
        }
        return getUpperCDF() - lowerCDF;
    }

    /**
     * The inner distribution's probabilities at both ends of [lower, upper]: cumulative
     * probabilities P(X < lower) and P(X <= upper), or in the upper tail survival
     * probabilities P(X >= lower) and P(X > upper).
     */
    private static final class Interval {
        final boolean upperTail;
        final double atLower, atUpper, mass;

        Interval(boolean upperTail, double atLower, double atUpper) {
            this.upperTail = upperTail;
            this.atLower = atLower;
            this.atUpper = atUpper;
            this.mass = upperTail ? atLower - atUpper : atUpper - atLower;
        }
    }

    /**
     * @return the interval, for the CDF, the inverses, sample() and getMean()
     * @throws IllegalStateException if [lower, upper] carries no probability, so that
     * the truncated distribution is not defined (logDensity() is then -Infinity)
     */
    private Interval interval() {
        refresh();
        double lowerCDF = getLowerCDF();
        Interval iv = isUpperTail(lowerCDF)
                ? new Interval(true, getLowerSurvival(), getUpperSurvival())
                : new Interval(false, lowerCDF, getUpperCDF());
        if (!(iv.mass > 0)) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + (getID() == null ? "" : " '" + getID() + "'") + ": the interval ["
                    + lower.get() + ", " + upper.get() + "] carries no probability under "
                    + dist.getClass().getSimpleName() + ", so the truncated distribution is not defined");
        }
        return iv;
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
        // sum over the values that carry all but a negligible part of the truncated mass;
        // the tails beyond these quantiles cannot shift the mean noticeably. The upper
        // quantile comes from the survival function: 1 - 1e-12 relative to the interval
        // can round to 1.0 when it is measured with P(X <= x), and the inverse at 1.0 is
        // Integer.MAX_VALUE.
        long from = inverseCumulativeProbability(0.0);
        long to = inverseSurvivalProbability(1e-12);
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
