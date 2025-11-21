package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.NormalDistribution;

import java.util.List;


@Description("Normal distribution.  f(x) = frac{1}{\\sqrt{2\\pi\\sigma^2}} e^{ -\\frac{(x-\\mu)^2}{2\\sigma^2} } " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Normal extends ScalarDistribution<RealScalar<Real>, Double> {

    final public Input<RealScalar<Real>> meanInput = new Input<>("mean",
            "mean of the normal distribution, defaults to 0");
    final public Input<RealScalar<PositiveReal>> sdInput = new Input<>("sigma",
            "standard deviation of the normal distribution, defaults to 1");
    // tau is reciprocal of variance.
    final public Input<RealScalar<PositiveReal>> tauInput = new Input<>("tau",
            "precision of the normal distribution, defaults to 1", Validate.XOR, sdInput);

    private NormalDistribution dist = NormalDistribution.of(0, 1);
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Normal() {
    }

    public Normal(RealScalar<Real> param,
                  RealScalar<Real> mean, RealScalar<PositiveReal> sigma) {

        try {
            initByName("param", param, "mean", mean, "sigma", sigma);
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
        double mean = (meanInput.get() != null) ? meanInput.get().get() : 0.0;
        double sd = 1.0; // default
        // Use sdInput if provided; otherwise compute from tauInput if provided;
        if (sdInput.get() != null) {
            sd = sdInput.get().get();
        } else if (tauInput.get() != null) {
            sd = Math.sqrt(1.0 / tauInput.get().get());
        }

        // Floating point comparison:
        if (isNotEqual(dist.getMean(), mean) || isNotEqual(dist.getStandardDeviation(), sd) ) {
            dist = NormalDistribution.of(mean, sd);
            sampler = dist.createSampler(rng);
        } else if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
    }

    @Override
    public double calculateLogP() {
        logP = dist.logDensity(param.get() - getOffset()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Double value) {
        return dist.logDensity(value - getOffset()); // scalar
    }

    @Override
    protected List<Double> sample() {
        final double x = sampler.sample() + getOffset();
        return List.of(x); // Returning an immutable result
    }

    @Override
    public Object getApacheDistribution() {
    	return dist;
    }
} // class Normal
