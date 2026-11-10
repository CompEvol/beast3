package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.Bounded;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.UniformContinuousDistribution;

import java.util.List;

/**
 * Uniform distribution on [lower, upper] for a non-negative or positive parameter, such as a
 * birth rate. {@link Uniform} cannot be used for this, because its parameter is typed
 * {@code RealScalar<Real>}.
 * <p>
 * The lower bound must be at least 0, so the whole support is non-negative. Both ends are
 * included. A positive parameter can use it too: the single point 0 has probability zero, and
 * the parameter's own domain keeps its value above 0.
 * <p>
 * lower and upper may be estimated. A state where upper &lt;= lower has log density -infinity,
 * so MCMC rejects it rather than stopping.
 */
@Description("Uniform distribution on [lower, upper] for a non-negative or positive parameter. " +
        "The lower bound must be at least 0. Both ends are included.")
public class NonNegativeUniform extends ScalarDistribution<RealScalar<NonNegativeReal>, Double>
        implements Bounded<Double> {

    private static final double DEFAULT_LOWER = 0.0;
    private static final double DEFAULT_UPPER = 1.0;

    final public Input<RealScalar<NonNegativeReal>> lowerInput = new Input<>("lower",
            "lower bound on the interval, at least 0. Defaults to " + DEFAULT_LOWER + ".",
            new RealScalarParam<>(DEFAULT_LOWER, NonNegativeReal.INSTANCE));
    final public Input<RealScalar<PositiveReal>> upperInput = new Input<>("upper",
            "upper bound on the interval, must be finite and greater than lower. Defaults to " + DEFAULT_UPPER + ".",
            new RealScalarParam<>(DEFAULT_UPPER, PositiveReal.INSTANCE));

    private UniformContinuousDistribution dist;
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public NonNegativeUniform() {}

    public NonNegativeUniform(RealScalar<NonNegativeReal> param,
                              RealScalar<NonNegativeReal> lower, RealScalar<PositiveReal> upper) {
        try {
            initByName("param", param, "lower", lower, "upper", upper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e);
        }
    }

    @Override
    public void initAndValidate() {
        final double lower = getLower();
        // The lower input is typed NonNegativeReal, but Input does not enforce a tensor's
        // domain yet (#226), so check the value once, at load. !(x) also rejects NaN.
        // TODO remove the lower >= 0 check once #226 is fixed
        if (!(lower >= 0.0)) {
            throw boundsError(lower, getUpper());
        }
        checkInterval();
        refresh();
        super.initAndValidate();
    }

    /**
     * Rebuilds the distribution when the bounds have changed. Never throws, because wrappers
     * such as {@link IID} call it during MCMC: if the bounds are invalid, the old distribution
     * is kept and {@link #calcLogP(Double)} returns -infinity.
     */
    @Override
    public void refresh() {
        if (!hasValidInterval()) {
            return;
        }
        final double lower = getLower();
        final double upper = getUpper();
        if (dist == null || isNotEqual(dist.getSupportLowerBound(), lower)
                || isNotEqual(dist.getSupportUpperBound(), upper)) {
            dist = UniformContinuousDistribution.of(lower, upper);
            sampler = null; // the old sampler would keep drawing from the old interval
        }
    }

    /**
     * Used by {@link #calculateLogP()} and by wrappers such as {@link IID}. Returns -infinity
     * when an MCMC proposal has moved upper to or below lower, so the proposal is rejected.
     */
    @Override
    protected double calcLogP(Double value) {
        if (!hasValidInterval()) {
            return Double.NEGATIVE_INFINITY;
        }
        return getApacheDistribution().logDensity(value);
    }

    @Override
    public List<Double> sample() {
        UniformContinuousDistribution d = getApacheDistribution();
        if (sampler == null) {
            sampler = d.createSampler(rng);
        }
        return List.of(sampler.sample());
    }

    /**
     * @throws IllegalArgumentException if upper &lt;= lower: there is no distribution to
     *         sample from or evaluate. MCMC uses {@link #calcLogP(Double)}, which does not throw.
     */
    @Override
    protected UniformContinuousDistribution getApacheDistribution() {
        checkInterval();
        refresh(); // parameters may have been sampled during MCMC
        return dist;
    }

    @Override
    public Double getLower() {
        return lowerInput.get().get(); // never null: the input has a default
    }

    @Override
    public Double getUpper() {
        return upperInput.get().get(); // never null: the input has a default
    }

    @Override
    public boolean lowerInclusive() {
        return true;
    }

    @Override
    public boolean upperInclusive() {
        return true;
    }

    /** true if upper is finite and greater than lower; false if either is NaN */
    private boolean hasValidInterval() {
        final double upper = getUpper();
        return Double.isFinite(upper) && upper > getLower();
    }

    private void checkInterval() {
        if (!hasValidInterval()) {
            throw boundsError(getLower(), getUpper());
        }
    }

    private IllegalArgumentException boundsError(double lower, double upper) {
        return new IllegalArgumentException(getClass().getSimpleName() +
                " requires 0 <= lower < upper < infinity, got [" + lower + ", " + upper + "]");
    }
}
