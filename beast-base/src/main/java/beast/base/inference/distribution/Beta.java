package beast.base.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import org.apache.commons.statistics.distribution.BetaDistribution;

/**
 * @deprecated replaced by {@link beast.base.spec.inference.distribution.Beta}
 */
@Deprecated
@Description("Beta distribution, used as prior.  p(x;alpha,beta) = \frac{x^{alpha-1}(1-x)^{beta-1}} {B(alpha,beta)} " +
        "where B() is the beta function. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Beta extends ParametricDistribution {
    final public Input<Function> alphaInput = new Input<>("alpha", "first shape parameter, defaults to 1");
    final public Input<Function> betaInput = new Input<>("beta", "the other shape parameter, defaults to 1");

    BetaDistribution m_dist = BetaDistribution.of(1, 1);

    // cached values for getMeanWithoutOffset
    private double currentAlpha = 1;
    private double currentBeta = 1;

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double alpha;
        double beta;
        if (alphaInput.get() == null) {
            alpha = 1;
        } else {
            alpha = alphaInput.get().getArrayValue();
        }
        if (betaInput.get() == null) {
            beta = 1;
        } else {
            beta = betaInput.get().getArrayValue();
        }
        currentAlpha = alpha;
        currentBeta = beta;
        m_dist = BetaDistribution.of(alpha, beta);
    }

    @Override
    public Object getDistribution() {
        refresh();
        return m_dist;
    }

    @Override
    protected double getMeanWithoutOffset() {
    	return currentAlpha / (currentAlpha + currentBeta);
    }
} // class Beta
