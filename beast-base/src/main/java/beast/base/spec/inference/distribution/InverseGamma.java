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

    @Override
	protected Object getApacheDistribution() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        return dist;
    }
} // class InverseGamma
