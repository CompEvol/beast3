package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.ExponentialDistribution;

import java.util.List;


@Description("Exponential distribution.  f(x;\\theta) = 1/\\theta e^{-x/\\theta}, if x >= 0 " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Exponential extends ScalarDistribution<RealScalar<NonNegativeReal>, Double> {

    final public Input<RealScalar<PositiveReal>> meanInput = new Input<>("mean",
            "mean parameter, defaults to 1");

    private ExponentialDistribution dist = ExponentialDistribution.of(1);
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Exponential() {
    }

    public Exponential(RealScalar<NonNegativeReal> param, RealScalar<PositiveReal> mean) {

        try {
            initByName("param", param, "mean", mean);
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
        double mean = (meanInput.get() != null) ? meanInput.get().get() : 1.0;

        // Floating point comparison:
        if (isNotEqual(dist.getMean(), mean)) {
            dist = ExponentialDistribution.of(mean);
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
        return List.of(x); // Returning an immutable result
    }

    @Override
    public Object getApacheDistribution() {
    	return dist;
    }
    
} // class Exponential
