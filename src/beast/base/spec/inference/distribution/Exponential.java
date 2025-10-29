package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ExponentialDistribution;


@Description("Exponential distribution.  f(x;\\theta) = 1/\\theta e^{-x/\\theta}, if x >= 0 " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Exponential extends RealTensorDistribution<NonNegativeReal> {

    final public Input<RealScalar<PositiveReal>> meanInput = new Input<>("mean",
            "mean parameter, defaults to 1");

    private ExponentialDistribution dist = ExponentialDistribution.of(1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double mean = (meanInput.get() != null) ? meanInput.get().get() : 1.0;

        // Floating point comparison:
        if (Math.abs(dist.getMean() - mean) > EPS)
            dist = ExponentialDistribution.of(mean);
    }

    @Override
    public ExponentialDistribution getDistribution() {
        refresh();
        return dist;
    }

} // class Exponential
