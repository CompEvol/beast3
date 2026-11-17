package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.Bounded;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;

import java.util.List;


/**
 * Wrapper that truncates a real-valued distribution to the interval [lower, upper].
 * The base distribution should not define its own parameter; all parameters are
 * passed through this wrapper.
 */
@Description("Truncates a real valued distribution to the interval [lower,upper]. " +
             "The base distribution itself should not define any parameters. " +
        "All parameters should be passed via TruncatedRealDistribution.")
public class TruncatedReal<D extends Real> extends ScalarDistribution<RealScalar<D>, Double> implements WrapsDistribution {


    // any real distribution, whatever the domain of its own parameter
    final public Input<ScalarDistribution<? extends RealScalar<?>, Double>> distributionInput = new Input<>("distribution",
            "the distribution to truncate", Validate.REQUIRED);
    final public Input<RealScalar<?>> lowerInput = new Input<>("lower", "Lower end of the truncation interval.");
    final public Input<RealScalar<?>> upperInput = new Input<>("upper", "Upper end of the truncation interval.");

    public final Input<? extends Real> domainInput = new Input<>("domain",
            "the domain of the values this distribution puts probability on, e.g. PositiveReal for Normal " +
            "truncated to [0, 10] (optional). The truncated support must lie within it, for every value " +
            "estimated bounds can take: an error otherwise.");

    private ScalarDistribution<? extends RealScalar<?>, Double> dist;
    RealScalar<?> lower, upper;
    private Real declaredDomain;
    /** how the truncation bounds can change, once known (see outsideDeclaredDomain()) */
    private DeclaredDomain.Range boundsRange;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public TruncatedReal() {
    }

    public TruncatedReal(
        ScalarDistribution<? extends RealScalar<?>, Double> dist,
        double lower,
        double upper
    ) {
        assert (Real.INSTANCE.getLower() <= lower) && (lower < upper) && (upper <= Real.INSTANCE.getUpper());

        try {
            initByName(
                "distribution", dist, 
                "lower", new RealScalarParam<Real>(lower, Real.INSTANCE),
                "upper", new RealScalarParam<Real>(upper, Real.INSTANCE)
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
        boundsRange = null;
        if (declaredDomain != null) {
            DeclaredDomain.check(this, getID(), declaredDomain, getLowerBoundOfParameter(), getUpperBoundOfParameter());
        }
        super.initAndValidate();
    }

    /**
     * @return the declared domain of the values this distribution puts probability on, or
     * Real when none is declared
     */
    @Override
    public Real getParameterDomain() {
        Real domain = domainInput.get();
        return domain != null ? domain : Real.INSTANCE;
    }

    /**
     * Whether the truncation bounds have put the support outside the declared domain. On
     * the first evaluation, when all objects are initialised, the range of estimated bounds
     * is checked once (an error when it does not fit, see {@link DeclaredDomain}); only
     * calculated bounds, whose range is not known, are checked on every evaluation.
     */
    private boolean outsideDeclaredDomain() {
        if (declaredDomain == null) {
            return false;
        }
        if (boundsRange == null || boundsRange == DeclaredDomain.Range.NOT_YET_KNOWN) {
            DeclaredDomain.Range l = DeclaredDomain.range(lowerInput.get());
            DeclaredDomain.Range u = DeclaredDomain.range(upperInput.get());
            boundsRange = l.compareTo(u) >= 0 ? l : u;
            if (boundsRange == DeclaredDomain.Range.ESTIMATED) {
                refresh();
                // the lowest and highest values the support can reach
                DeclaredDomain.checkEstimated(this, getID(), declaredDomain,
                        Math.max(dist.getLowerBoundOfParameter(), l == DeclaredDomain.Range.ESTIMATED ? lower.getLower() : lower.get()),
                        Math.min(dist.getUpperBoundOfParameter(), u == DeclaredDomain.Range.ESTIMATED ? upper.getUpper() : upper.get()),
                        "truncation bounds");
            } else if (boundsRange == DeclaredDomain.Range.CALCULATED) {
                DeclaredDomain.warnCalculated(this, getID(), "truncation bounds");
            }
        }
        return boundsRange == DeclaredDomain.Range.CALCULATED
                && DeclaredDomain.outside(declaredDomain, getLowerBoundOfParameter(), getUpperBoundOfParameter()) != null;
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
            lower = new RealScalarParam<>(Real.INSTANCE.getLower(), Real.INSTANCE);

        upper = upperInput.get();
        if (upper == null)
            upper = new RealScalarParam<>(Real.INSTANCE.getUpper(), Real.INSTANCE);
    }

    @Override
    public double calculateLogP() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        if (outsideDeclaredDomain()) {
            // calculated bounds have moved the support outside the declared domain
            logP = Double.NEGATIVE_INFINITY;
            return logP;
        }
        logP = logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Double value) {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        if (outsideDeclaredDomain()) {
            return Double.NEGATIVE_INFINITY;
        }
        return logDensity(value);
    }

    @Override
    public double logDensity(double x) {
        if (!isValid(x)) {
            return Double.NEGATIVE_INFINITY;
        }
        double mass = mass();
        if (mass <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        return dist.calcLogP(x) - Math.log(mass);
    }

    @Override
    public double density(double x) {
        return Math.exp(logDensity(x));
    }
    
    @Override
    public double cumulativeProbability(double x) {
        if (x < lower.get()) {
            return 0.0;
        } else if (x >= upper.get()) {
            return 1.0;
        }
        Interval iv = interval();
        ScalarDistribution<? extends RealScalar<?>, Double> inner = getInnerDistribution();
        if (iv.upperTail) {
            // P(lower < X <= x) = P(X > lower) - P(X > x)
            return (iv.atLower - inner.survivalProbability(x)) / iv.mass;
        }
        return (inner.cumulativeProbability(x) - iv.atLower) / iv.mass;
    }

    @Override
    public double survivalProbability(double x) {
        if (x < lower.get()) {
            return 1.0;
        } else if (x >= upper.get()) {
            return 0.0;
        }
        Interval iv = interval();
        ScalarDistribution<? extends RealScalar<?>, Double> inner = getInnerDistribution();
        if (iv.upperTail) {
            // P(x < X <= upper) = P(X > x) - P(X > upper)
            return (inner.survivalProbability(x) - iv.atUpper) / iv.mass;
        }
        return (iv.atUpper - inner.cumulativeProbability(x)) / iv.mass;
    }

    @Override
    public Double inverseCumulativeProbability(double p) {
        if (p < 0.0 || p > 1.0) {
            throw new IllegalArgumentException("Probability p must be in [0, 1]");
        }
        Interval iv = interval();
        ScalarDistribution<? extends RealScalar<?>, Double> inner = getInnerDistribution();
        double x;
        if (iv.upperTail) {
            x = inner.inverseSurvivalProbability(iv.atLower - p * iv.mass);
        } else {
            x = inner.inverseCumulativeProbability(iv.atLower + p * iv.mass);
        }
        return clamp(x);
    }

    @Override
    public Double inverseSurvivalProbability(double p) {
        if (p < 0.0 || p > 1.0) {
            throw new IllegalArgumentException("Probability p must be in [0, 1]");
        }
        Interval iv = interval();
        ScalarDistribution<? extends RealScalar<?>, Double> inner = getInnerDistribution();
        double x;
        if (iv.upperTail) {
            x = inner.inverseSurvivalProbability(iv.atUpper + p * iv.mass);
        } else {
            x = inner.inverseCumulativeProbability(iv.atUpper - p * iv.mass);
        }
        return clamp(x);
    }

    /** rounding near the ends can map just past them, so keep the result inside [lower, upper] */
    private double clamp(double x) {
        return Math.max(lower.get(), Math.min(upper.get(), x));
    }

    public ScalarDistribution<? extends RealScalar<?>, Double> getInnerDistribution() {
    	if (dist == null) {
    		refresh();
    	}
        return dist;
    }

    @Override
    public Double getLowerBoundOfParameter() {
        // Always refresh() first (getInnerDistribution() only does when dist is null), like
        // ScalarDistribution's default via getApacheDistribution(), so the bounds follow an inner
        // distribution or a truncation lower/upper replaced after initialisation (e.g. in BEAUti).
        refresh();
        Double innerLower = dist.getLowerBoundOfParameter();
        if (innerLower == null)
            return lower.get();
        return Math.max(lower.get(), innerLower);
    }

    @Override
    public Double getUpperBoundOfParameter() {
        // Always refresh() first (getInnerDistribution() only does when dist is null), like
        // ScalarDistribution's default via getApacheDistribution(), so the bounds follow an inner
        // distribution or a truncation lower/upper replaced after initialisation (e.g. in BEAUti).
        refresh();
        // check the inner upper bound (this used to check the lower one)
        Double innerUpper = dist.getUpperBoundOfParameter();
        if (innerUpper == null)
            return upper.get();
        return Math.min(upper.get(), innerUpper);
    }

    double getLowerCDF() {
        return getInnerDistribution().cumulativeProbability(lower.get());
    }

    double getUpperCDF() {
        return getInnerDistribution().cumulativeProbability(upper.get());
    }

    /** @return P(X > lower) for the inner distribution */
    double getLowerSurvival() {
        return getInnerDistribution().survivalProbability(lower.get());
    }

    /** @return P(X > upper) for the inner distribution */
    double getUpperSurvival() {
        return getInnerDistribution().survivalProbability(upper.get());
    }

    /**
     * Whether [lower, upper] lies in the upper tail of the inner distribution. There
     * P(X <= x) is close to 1 and differences of it lose their precision, down to 0
     * (Exp(1) from 40: P(X <= 40) is 1.0 in double precision, and the density was
     * +Infinity), so the mass, CDF and inverse are computed from the survival function
     * P(X > x) instead.
     */
    private static boolean isUpperTail(double lowerCDF) {
        return lowerCDF > 0.5;
    }

    /**
     * @return the probability mass of the inner distribution on [lower, upper]. Used by
     * logDensity() on every MCMC step, so it does not allocate.
     */
    double mass() {
        double lowerCDF = getLowerCDF();
        if (isUpperTail(lowerCDF)) {
            return getLowerSurvival() - getUpperSurvival();
        }
        return getUpperCDF() - lowerCDF;
    }

    /** @return the probability of the inner distribution outside [lower, upper] */
    double probOutOfBounds() {
        return 1.0 - mass();
    }

    /**
     * The inner distribution's probabilities at both ends of [lower, upper]: cumulative
     * probabilities P(X <= lower) and P(X <= upper), or in the upper tail survival
     * probabilities P(X > lower) and P(X > upper).
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
	public List<Double> sample() {
        refresh();
        // inverse transform sampling: a uniform draw on [0, 1) mapped through the inverse
        // CDF of the truncated distribution lands in [lower, upper]. This used to draw
        // between P(X <= lower) and P(X <= upper) and invert the inner CDF, which gave NaN
        // where both are 1.0 in double precision.
        double x = inverseCumulativeProbability(rng.nextDouble());
        assert isValid(x);
        // Returning an immutable result
        return List.of(x);
    }

    @Override
    public double getMean() {
        refresh();
        // E[X] is the integral of the truncated quantile function over (0, 1). It used to
        // integrate the inner quantile function between P(X <= lower) and P(X <= upper),
        // which collapses where both are 1.0 in double precision. The ends are kept EPS
        // away from 0 and 1, where the quantile can be infinite.
        return simpsonIntegration(EPS, 1.0 - EPS, 20_000);
    }

    /**
     * Simpson's rule for the integral of the truncated quantile function over [from, to],
     * divided by (to - from).
     */
    private double simpsonIntegration(double from, double to, int n) {
        if (n % 2 != 0)  // Ensure even number of intervals
            n++;
        double stepSize = (to - from) / n;
        double sum = 0.0;
        for (int i = 0; i <= n; i++) {
            double u = from + i * stepSize;
            // in the upper half, invert the survival function: 1 - u loses precision there
            double x = u <= 0.5 ? inverseCumulativeProbability(u) : inverseSurvivalProbability(1.0 - u);
            double weight = (i == 0 || i == n) ? 1.0 : ((i % 2 == 1) ? 4.0 : 2.0);
            sum += weight * x;
        }
        return sum * stepSize / (to - from) / 3.0;
    }

    @Override
	protected Object getApacheDistribution() {
    	throw new RuntimeException("Not implemented for TruncatedReal distribution");
    }

    boolean isValid(double value) {
        return Real.INSTANCE.isValid(value) && lower.get() <= value && value <= upper.get();
    }


    @Override
    public String supportOutsideDomainWith(ScalarDistribution<?, ?> inner, Bounded<?> domain) {
        double lo = lowerInput.get() != null ? lowerInput.get().get() : Double.NEGATIVE_INFINITY;
        double hi = upperInput.get() != null ? upperInput.get().get() : Double.POSITIVE_INFINITY;
        return DeclaredDomain.outside(domain,
                Math.max(lo, DeclaredDomain.lowerOrInfinite(inner)),
                Math.min(hi, DeclaredDomain.upperOrInfinite(inner)));
    }

} // class TruncatedReal
