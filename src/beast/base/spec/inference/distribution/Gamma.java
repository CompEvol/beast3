package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;

import java.util.List;


@Description("Gamma distribution. for x>0  g(x;alpha,beta) = 1/Gamma(alpha) beta^alpha} x^{alpha - 1} e^{-\frac{x}{beta}}" +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Gamma extends ScalarDistribution<RealScalar<PositiveReal>, Double> {

    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "shape parameter, defaults to 1");
    final public Input<RealScalar<PositiveReal>> thetaInput = new Input<>("theta",
            "scale parameter for Shape–Scale form, defaults to 1.");
    final public Input<RealScalar<PositiveReal>> betaInput = new Input<>("beta",
            "rate parameter for Shape–Rate form, defaults to 1.", Input.Validate.XOR, thetaInput);

    private GammaDistribution dist = GammaDistribution.of(1.0, 1.0);
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Gamma() {}

    // default to use theta
    public Gamma(RealScalar<PositiveReal> param,
                 RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> theta) {
        this(param, alpha, theta, null);
    }

    public Gamma(RealScalar<PositiveReal> param,
                 RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> theta,
                 RealScalar<PositiveReal> beta) {

        try {
            if (theta != null && beta == null) {
                initByName("param", param, "alpha", alpha, "theta", theta);
            } else if (beta != null && theta == null) {
                initByName("param", param, "alpha", alpha, "beta", beta);
            } else
                throw new IllegalArgumentException("Must have either theta or beta ! ");
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
     * make sure internal state is up to date *
     */
	void refresh() {
        double alpha = (alphaInput.get() != null) ? alphaInput.get().get() : 1.0;

        double scale = 1.0; // default
        if (thetaInput.get() != null && betaInput.get()  == null) {
            // θ provided directly
            scale  = thetaInput.get().get();
        } else if (betaInput.get() != null && thetaInput.get()  == null) {
            // β provided : θ = 1 / β
            scale  = 1.0 / betaInput.get().get() ;

        } else if (thetaInput.get() == null && betaInput.get()  == null) {
            // both null, use default
        } else
            throw new IllegalArgumentException("Must have either theta or beta ! ");

        // Floating point comparison
        if (isNotEqual(dist.getShape(), alpha) ||  isNotEqual(dist.getScale(), scale)) {
            dist = GammaDistribution.of(alpha, scale);
            sampler = dist.createSampler(rng);
        } else if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
    }

    @Override
    public double calculateLogP() {
        logP = dist.logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Double value) {
        return dist.logDensity(value); // scalar
    }

    @Override
    protected List<Double> sample() {
        final double x = sampler.sample();
        return List.of(x);
    }

    @Override
    public Object getApacheDistribution() {
    	return dist;
    }
} // class Gamma
