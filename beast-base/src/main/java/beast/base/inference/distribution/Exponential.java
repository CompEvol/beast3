package beast.base.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Log;
import org.apache.commons.statistics.distribution.ExponentialDistribution;

/**
 * @deprecated replaced by {@link beast.base.spec.inference.distribution.Exponential}
 */
@Deprecated
@Description("Exponential distribution.  f(x;\\lambda) = 1/\\lambda e^{-x/\\lambda}, if x >= 0 " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Exponential extends ParametricDistribution {
    final public Input<Function> lambdaInput = new Input<>("mean", "mean parameter, defaults to 1");

    ExponentialDistribution m_dist = ExponentialDistribution.of(1);

    // cached mean for getMeanWithoutOffset
    private double currentMean = 1;

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double lambda;
        if (lambdaInput.get() == null) {
            lambda = 1;
        } else {
            lambda = lambdaInput.get().getArrayValue();
            if (lambda < 0) {
                Log.err.println("Exponential::Lambda should be positive not " + lambda + ". Assigning default value.");
                lambda = 1;
            }
        }
        currentMean = lambda;
        m_dist = ExponentialDistribution.of(lambda);
    }

    @Override
    public Object getDistribution() {
        refresh();
        return m_dist;
    }

    @Override
    protected double getMeanWithoutOffset() {
    	return currentMean;
    }

} // class Exponential
