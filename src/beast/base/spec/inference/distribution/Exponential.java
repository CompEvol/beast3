package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.ExponentialDistribution;

import java.util.List;


@Description("Exponential distribution.  f(x;\\theta) = 1/\\theta e^{-x/\\theta}, if x >= 0 " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Exponential extends TensorDistribution<RealScalar<NonNegativeReal>, NonNegativeReal, Double> {

    final public Input<RealScalar<PositiveReal>> meanInput = new Input<>("mean",
            "mean parameter, defaults to 1");

    protected ExponentialDistribution dist = ExponentialDistribution.of(1);

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
	void refresh() {
        double mean = (meanInput.get() != null) ? meanInput.get().get() : 1.0;

        // Floating point comparison:
        if (Math.abs(dist.getMean() - mean) > EPS)
            dist = ExponentialDistribution.of(mean);
    }

    @Override
    protected double calcLogP(Double... value) {
        return dist.logDensity(value[0]); // scalar
    }

    @Override
    protected List<RealScalar<NonNegativeReal>> sample() {
        ContinuousDistribution.Sampler sampler = dist.createSampler(rng);
        double x = sampler.sample();
        RealScalarParam<NonNegativeReal> param = new RealScalarParam<>(x, NonNegativeReal.INSTANCE);
        return List.of(param);
    }

} // class Exponential
