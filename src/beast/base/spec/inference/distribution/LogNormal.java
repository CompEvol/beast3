package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.LogNormalDistribution;


/**
 * @author Alexei Drummond
 */
@Description("A log-normal distribution with mean and variance parameters.")
public class LogNormal extends RealTensorDistribution<RealScalar<PositiveReal>, PositiveReal> {

    final public Input<RealScalar<Real>> MParameterInput = new Input<>("M",
            "M parameter of lognormal distribution. " +
                    "Equal to the mean of the log-transformed distribution.");
    final public Input<RealScalar<PositiveReal>> SParameterInput = new Input<>("S",
            "S parameter of lognormal distribution. " +
                    "Equal to the standard deviation of the log-transformed distribution.");
    final public Input<Boolean> hasMeanInRealSpaceInput = new Input<>("meanInRealSpace",
            "Whether the M parameter is in real space, or in log-transformed space. " +
                    "Default false = log-transformed.", false);

    protected boolean hasMeanInRealSpace;
    protected LogNormalDistribution dist = LogNormalDistribution.of(0, 1);

    @Override
	public void initAndValidate() {
        hasMeanInRealSpace = hasMeanInRealSpaceInput.get();
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double mean  = (MParameterInput.get() != null) ? MParameterInput.get().get() : 0.0;
        double sigma = (SParameterInput.get() != null) ? SParameterInput.get().get() : 1.0;

        if (hasMeanInRealSpace)
            mean = Math.log(mean) - (0.5 * sigma * sigma);

        if (Math.abs(dist.getMean() - mean) > EPS ||  Math.abs(dist.getSigma() - sigma) > EPS)
            dist = LogNormalDistribution.of(mean, sigma);
    }

    @Override
    public ContinuousDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    protected RealScalar<PositiveReal> valueToTensor(double value) {
        return new RealScalarParam<>(value, PositiveReal.INSTANCE);
    }

    @Override
    public double getMeanWithoutOffset() {
    	if (hasMeanInRealSpace) {
            return (MParameterInput.get() != null) ? MParameterInput.get().get() : 0.0;
    	} else {
    		double s = (SParameterInput.get() != null) ? SParameterInput.get().get() : 1.0;
    		double m = (MParameterInput.get() != null) ? MParameterInput.get().get() : 0.0;
    		return Math.exp(m + s * s/2.0);
    		//throw new RuntimeException("Not implemented yet");
    	}
    }

}
