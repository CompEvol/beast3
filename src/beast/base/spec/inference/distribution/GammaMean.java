package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;

import java.util.List;


@Description("Gamma distribution using a different parameterization.")
public class GammaMean extends ScalarDistribution<RealScalar<PositiveReal>, Double> {

    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "shape parameter, defaults to 1", Input.Validate.REQUIRED);
    final public Input<RealScalar<PositiveReal>> meanInput = new Input<>("mean",
            "the expected mean of Gamma distribution, which equals shape * scale, default to 1.",
            Input.Validate.REQUIRED);

    private GammaDistribution dist = GammaDistribution.of(1.0, 1.0);
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public GammaMean() {}

    // default to use theta
    public GammaMean(RealScalar<PositiveReal> param,
                     RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> mean) {
        try {
            initByName("param", param, "alpha", alpha, "mean", mean);
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
        double alpha = (alphaInput.get() != null) ? alphaInput.get().get() : 2.0;
        double mean = (meanInput.get() != null) ? meanInput.get().get() : 1.0; // default

        double scale = mean / alpha;

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
