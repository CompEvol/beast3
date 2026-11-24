package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;

import java.util.List;


/**
 * Gamma distribution parameterised by shape (alpha) and either scale (theta) or rate (rate = 1/theta).
 * The scale and rate inputs are mutually exclusive (XOR). When applied to a
 * multidimensional parameter, each dimension is treated as an independent component.
 * <p>
 * Not the same inputs as BEAST 2's {@code beast.base.inference.distribution.Gamma}: there,
 * {@code beta} is the scale by default and its meaning depends on {@code mode}, and the defaults
 * are alpha 2, scale 2. This class has no {@code beta} or {@code mode} input, so a copied
 * {@code beta=} or {@code mode=} fails when the XML is loaded, and its defaults are alpha 1,
 * scale 1 (#49).
 * <p>
 * The rate input was called {@code lambda} in the 2.8.0 pre-releases. The Java field is still
 * {@code betaInput}, so code that sets it keeps compiling.
 */
@Description("Gamma distribution. for x>0  g(x;alpha,theta) = 1/{Gamma(alpha) theta^alpha} x^{alpha - 1} e^{-\frac{x}{theta}}" +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Gamma extends ScalarDistribution<RealScalar<PositiveReal>, Double> {

    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "shape parameter, defaults to 1");
    final public Input<RealScalar<PositiveReal>> thetaInput = new Input<>("theta",
            "scale parameter for Shape–Scale form, which equals to 1/rate, defaults to 1.");
    final public Input<RealScalar<PositiveReal>> betaInput = new Input<>("rate",
            "rate parameter for Shape–Rate form, which equals to 1/scale, defaults to 1.", Input.Validate.XOR, thetaInput);

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
                 RealScalar<PositiveReal> rate) {

        try {
            if (theta != null && rate == null) {
                initByName("param", param, "alpha", alpha, "theta", theta);
            } else if (rate != null && theta == null) {
                initByName("param", param, "alpha", alpha, "rate", rate);
            } else
                throw new IllegalArgumentException("Must have either theta (scale) or rate, not both");
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
    @Override
	public void refresh() {
        double alpha = (alphaInput.get() != null) ? alphaInput.get().get() : 1.0;

        double scale = 1.0; // default
        if (thetaInput.get() != null && betaInput.get()  == null) {
            // θ provided directly
            scale  = thetaInput.get().get();
        } else if (betaInput.get() != null && thetaInput.get()  == null) {
            // rate provided: θ = 1 / rate
            scale  = 1.0 / betaInput.get().get() ;

        } else if (thetaInput.get() == null && betaInput.get()  == null) {
            // both null, use default
        } else
            throw new IllegalArgumentException("Must have either theta (scale) or rate, not both");

        // Floating point comparison
        if (isNotEqual(dist.getShape(), alpha) ||  isNotEqual(dist.getScale(), scale)) {
            dist = GammaDistribution.of(alpha, scale);
        }
    }

    @Override
    public double calculateLogP() {
        logP = getApacheDistribution().logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
	public List<Double> sample() {
        if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
        final double x = sampler.sample();
        return List.of(x); // Returning an immutable result
    }

    @Override
	protected GammaDistribution getApacheDistribution() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        return dist;
    }
} // class Gamma
