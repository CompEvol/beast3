package beast.base.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;
import org.apache.commons.statistics.distribution.LogNormalDistribution;



/**
 * @author Alexei Drummond
 * @deprecated replaced by {@link beast.base.spec.inference.distribution.LogNormal}
 */
@Deprecated
@Description("A log-normal distribution with mean and variance parameters.")
public class LogNormalDistributionModel extends ParametricDistribution {
    final public Input<Function> MParameterInput = new Input<>("M", "M parameter of lognormal distribution. Equal to the mean of the log-transformed distribution.");
    final public Input<Function> SParameterInput = new Input<>("S", "S parameter of lognormal distribution. Equal to the standard deviation of the log-transformed distribution.");
    final public Input<Boolean> hasMeanInRealSpaceInput = new Input<>("meanInRealSpace", "Whether the M parameter is in real space, or in log-transformed space. Default false = log-transformed.", false);

    boolean hasMeanInRealSpace;
    protected LogNormalDistribution dist = LogNormalDistribution.of(0, 1);

    // cached values for getMeanWithoutOffset
    private double currentMu = 0;
    private double currentSigma = 1;

    @Override
	public void initAndValidate() {
        hasMeanInRealSpace = hasMeanInRealSpaceInput.get();
        if (MParameterInput.get() != null && MParameterInput.get() instanceof RealParameter) {
        	RealParameter M = (RealParameter) MParameterInput.get();
            if (M.getLower() == null) {
                M.setLower(Double.NEGATIVE_INFINITY);
            }
            if (M.getUpper() == null) {
                M.setUpper(Double.POSITIVE_INFINITY);
            }
        }

        if (SParameterInput.get() != null && SParameterInput.get() instanceof RealParameter) {
        	RealParameter S = (RealParameter) SParameterInput.get();
            if (S.getLower() == null) {
                S.setLower(0.0);
            }
            if (S.getUpper() == null) {
                S.setUpper(Double.POSITIVE_INFINITY);
            }
        }
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double mean;
        double sigma;
        if (SParameterInput.get() == null) {
            sigma = 1;
        } else {
            sigma = SParameterInput.get().getArrayValue();
        }
        if (MParameterInput.get() == null) {
            mean = 0;
        } else {
            mean = MParameterInput.get().getArrayValue();
        }
        if (hasMeanInRealSpace) {
            mean = Math.log(mean) - (0.5 * sigma * sigma);
        }
        currentMu = mean;
        currentSigma = sigma;
        dist = LogNormalDistribution.of(mean, sigma);
    }

    @Override
    public Object getDistribution() {
        refresh();
        return dist;
    }

    @Override
    protected double getMeanWithoutOffset() {
    	if (hasMeanInRealSpace) {
    		if (MParameterInput.get() != null) {
    			return MParameterInput.get().getArrayValue();
    		} else {
    			return 0.0;
    		}
    	} else {
    		double s = SParameterInput.get().getArrayValue();
    		double m = MParameterInput.get().getArrayValue();
    		return Math.exp(m + s * s/2.0);
    	}
    }

}
