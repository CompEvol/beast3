package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.Bounded;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.UniformContinuousDistribution;

import java.util.List;

/**
 * Uniform distribution on [lower, upper] for a strictly positive parameter, such as a
 * birth rate. {@link Uniform} cannot be used for this, because its parameter is typed
 * {@code RealScalar<Real>}.
 * <p>
 * The lower bound must be greater than 0, so lower = 0 is rejected and every value in the
 * support, and every sample, is positive. For a parameter that may be 0, use
 * {@link NonNegativeUniform}.
 */
@Description("Uniform distribution on [lower, upper] for a positive parameter (> 0). " +
        "The lower bound must be greater than 0. Both ends are included.")
public class PositiveUniform extends ScalarDistribution<RealScalar<PositiveReal>, Double>
        implements Bounded<Double> {

    // 0 is not allowed, so the default is the same small value as LogUniform's in BEAUti
    private static final double DEFAULT_LOWER = 1.0E-8;
    private static final double DEFAULT_UPPER = 1.0;

    final public Input<RealScalar<PositiveReal>> lowerInput = new Input<>("lower",
            "lower bound on the interval, greater than 0. Defaults to " + DEFAULT_LOWER + ".",
            new RealScalarParam<>(DEFAULT_LOWER, PositiveReal.INSTANCE));
    final public Input<RealScalar<PositiveReal>> upperInput = new Input<>("upper",
            "upper bound on the interval, must be finite and greater than lower. Defaults to " + DEFAULT_UPPER + ".",
            new RealScalarParam<>(DEFAULT_UPPER, PositiveReal.INSTANCE));

    private UniformContinuousDistribution dist;
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public PositiveUniform() {}

    public PositiveUniform(RealScalar<PositiveReal> param,
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

    /**
     * Make sure internal state is up to date. The bounds are checked here, rather than only in
     * initAndValidate(), because lower and upper may be estimated and change during MCMC.
     */
    @Override
    public void refresh() {
        final double lower = getLower();
        final double upper = getUpper();

        // The domains of the lower and upper inputs are not checked when the XML is parsed,
        // so check the values here. !(x) also rejects NaN.
        if (!(lower > 0.0) || !Double.isFinite(upper) || !(upper > lower)) {
            throw new IllegalArgumentException(getClass().getSimpleName() +
                    " requires 0 < lower < upper < infinity, got [" + lower + ", " + upper + "]");
        }

        if (dist == null || isNotEqual(dist.getSupportLowerBound(), lower)
                || isNotEqual(dist.getSupportUpperBound(), upper)) {
            dist = UniformContinuousDistribution.of(lower, upper);
            sampler = null; // the old sampler would keep drawing from the old interval
        }
    }

    @Override
    public double calculateLogP() {
        logP = getApacheDistribution().logDensity(param.get());
        return logP;
    }

    /**
     * Samples lie in [lower, upper] with lower > 0, so a sample is never 0.
     */
    @Override
    public List<Double> sample() {
        UniformContinuousDistribution d = getApacheDistribution();
        if (sampler == null) {
            sampler = d.createSampler(rng);
        }
        return List.of(sampler.sample());
    }

    @Override
    protected UniformContinuousDistribution getApacheDistribution() {
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

    /**
     * lower is greater than 0, so including it keeps the support positive.
     */
    @Override
    public boolean lowerInclusive() {
        return true;
    }

    @Override
    public boolean upperInclusive() {
        return true;
    }
}
