package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;

import java.util.List;

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
        C = alpha * Math.log(beta) - org.apache.commons.math.special.Gamma.logGamma(alpha);

        // Floating point comparison
        if (isNotEqual(dist.getShape(), alpha) ||  isNotEqual(dist.getScale(), 1.0 / beta)) {
            dist = GammaDistribution.of(alpha, 1.0 / beta);
            sampler = dist.createSampler(rng);
        } else if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
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
        double y = x - getOffset();
        return -(alpha + 1.0) * Math.log(y) - (beta / y) + C;
    }

    public double density(double x) {
        double logP = logDensity(x);
        return Math.exp(logP);
    }

    @Override
    protected List<Double> sample() {
        final double y = sampler.sample();  // sample from Gamma
        final double x = 1.0 / y + getOffset(); // sample from Gamma
        return List.of(x);
    }

    @Override
    public Object getApacheDistribution() {
    	return dist;
    }
} // class InverseGamma
