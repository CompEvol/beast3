package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;

import java.util.List;

/**
 * Inverse gamma distribution parameterised by shape (alpha) and scale (beta).
 * When applied to a multidimensional parameter, each dimension is treated as an
 * independent component.
 */
// TODO need to test
@Description("Inverse Gamma distribution, used as prior.    for x>0  f(x; alpha, beta) = \frac{beta^alpha}{Gamma(alpha)} (1/x)^{alpha + 1}exp(-beta/x) " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class InverseGamma extends ScalarDistribution<RealScalar<PositiveReal>, Double> {

    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "shape parameter, defaults to 1");
    final public Input<RealScalar<PositiveReal>> betaInput = new Input<>("beta",
            "scale parameter, defaults to 1");

    private GammaDistribution dist = GammaDistribution.of(1, 1);
    private ContinuousDistribution.Sampler sampler;

    private double alpha;
    private double beta;
    // log of the constant beta^alpha/Gamma(alpha)
    private double C;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public InverseGamma() {}

    public InverseGamma(RealScalar<UnitInterval> param,
                RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> beta) {

        try {
            initByName("param", param, "alpha", alpha, "beta", beta);
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
     * ensure internal state is up to date *
     */
    @Override
    public void refresh() {
        alpha = (alphaInput.get() != null) ? alphaInput.get().get() : 1.0;
        beta  = (betaInput.get()  != null) ? betaInput.get().get()  : 1.0;
        C = alpha * Math.log(beta) - org.apache.commons.numbers.gamma.LogGamma.value(alpha);

        // Floating point comparison
        if (isNotEqual(dist.getShape(), alpha) ||  isNotEqual(dist.getScale(), 1.0 / beta)) {
            dist = GammaDistribution.of(alpha, 1.0 / beta);
        }
    }

    @Override
    public double calculateLogP() {
        logP = logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Double value) {
        return logDensity(value); // scalar
    }

    // handle offset in one place
    public double logDensity(double x) {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        double y = x;
        return -(alpha + 1.0) * Math.log(y) - (beta / y) + C;
    }

    public double density(double x) {
        double logP = logDensity(x);
        return Math.exp(logP);
    }

    /**
     * {@link #getApacheDistribution()} exposes the internal Gamma(alpha, rate=beta) helper used
     * for sampling via x = 1/y. That object's own cumulativeProbability/inverseCumulativeProbability
     * describe Gamma, not InverseGamma, so ScalarDistribution's generic implementations (which just
     * delegate to getApacheDistribution()) are wrong here and must be overridden with the x = 1/y
     * change of variables: if Y = 1/X ~ Gamma(alpha, rate=beta), then
     *   P(X &le; x) = P(Y &ge; 1/x) = 1 - GammaCDF(1/x) = GammaSurvival(1/x).
     */
    @Override
    public double cumulativeProbability(double x) {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        return dist.survivalProbability(1.0 / x);
    }

    /**
     * Inverse of {@link #cumulativeProbability(double)}: solving GammaSurvival(1/x) = p for x gives
     * 1/x = GammaInverseSurvival(p), i.e. x = 1 / GammaInverseSurvival(p) = 1 / GammaICDF(1 - p).
     */
    @Override
    public Double inverseCumulativeProbability(double p) {
        if (p <= 0) {
            return getLowerBoundOfParameter();
        } else if (p >= 1) {
            return getUpperBoundOfParameter();
        }
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        return 1.0 / dist.inverseSurvivalProbability(p);
    }

    /**
     * ScalarDistribution.getMean() delegates to getApacheDistribution().getMean(), i.e. the
     * internal Gamma(alpha, rate=beta) helper's mean (alpha/beta), not InverseGamma's own mean
     * of beta/(alpha-1). For alpha &le; 1 the defining integral diverges; since X &gt; 0 always,
     * that divergence is to +Infinity (never negative or indeterminate), so +Infinity -- not NaN
     * -- is returned there, consistent with beta/(alpha-1)'s own limit as alpha -&gt; 1+.
     */
    @Override
    public double getMean() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        return alpha > 1 ? beta / (alpha - 1) : Double.POSITIVE_INFINITY;
    }

    @Override
	public List<Double> sample() {
        if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
        final double y = sampler.sample();  // sample from Gamma
        final double x = 1.0 / y; // sample from Gamma
        return List.of(x);
    }

    /**
     * Support is (0, +Infinity), same as the internal Gamma helper's [0, +Infinity) -- but
     * stated explicitly here (rather than relying on ScalarDistribution's default, which reads
     * it off getApacheDistribution()) so it stays correct independent of what that returns.
     */
    @Override
    public Double getLowerBoundOfParameter() {
        return 0.0;
    }

    @Override
    public Double getUpperBoundOfParameter() {
        return Double.POSITIVE_INFINITY;
    }

    /**
     * The internal Gamma helper only exists to draw samples via x = 1/y; it does not represent
     * InverseGamma's own density/CDF/mean, all of which are overridden above without consulting
     * it. Returning null (rather than the Gamma object) means any ScalarDistribution method added
     * in future that is not also overridden here will fail loudly instead of silently returning a
     * Gamma-distributed answer dressed up as an InverseGamma one.
     */
    @Override
	protected Object getApacheDistribution() {
        return null;
    }
} // class InverseGamma
