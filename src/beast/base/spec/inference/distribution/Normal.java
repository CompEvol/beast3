package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.NormalDistribution;


@Description("Normal distribution.  f(x) = frac{1}{\\sqrt{2\\pi\\sigma^2}} e^{ -\\frac{(x-\\mu)^2}{2\\sigma^2} } " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Normal extends RealTensorDistribution<RealScalar<PositiveReal>, PositiveReal> {

    final public Input<RealScalar<Real>> meanInput = new Input<>("mean",
            "mean of the normal distribution, defaults to 0");
    final public Input<RealScalar<PositiveReal>> sdInput = new Input<>("sigma",
            "standard deviation of the normal distribution, defaults to 1");
    // tau is reciprocal of variance.
    final public Input<RealScalar<PositiveReal>> tauInput = new Input<>("tau",
            "precision of the normal distribution, defaults to 1", Validate.XOR, sdInput);

    protected NormalDistribution dist = NormalDistribution.of(0, 1);

    @Override
    public void initAndValidate() {
        refresh();
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

        if (Math.abs(dist.getMean() - mean) > EPS ||  Math.abs(dist.getStandardDeviation() - sd) > EPS)
            dist = NormalDistribution.of(mean, sd);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    protected RealScalar<PositiveReal> valueToTensor(double value) {
        return null;
    }

    @Override
    public double getMeanWithoutOffset() {
        return (meanInput.get() != null) ? meanInput.get().get() : 0.0;
    }
} // class Normal
