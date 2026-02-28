package beast.base.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.parameter.IntegerParameter;
import org.apache.commons.statistics.distribution.ChiSquaredDistribution;


/**
 * @deprecated replaced by {@link beast.base.spec.inference.distribution.ChiSquare}
 */
@Deprecated
@Description("Chi square distribution, f(x; k) = \\frac{1}{2^{k/2}Gamma(k/2)} x^{k/2-1} e^{-x/2} " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class ChiSquare extends ParametricDistribution {
    final public Input<IntegerParameter> dfInput = new Input<>("df", "degrees if freedin, defaults to 1");

    ChiSquaredDistribution m_dist = ChiSquaredDistribution.of(1);

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        int dF;
        if (dfInput.get() == null) {
            dF = 1;
        } else {
            dF = dfInput.get().getValue();
            if (dF <= 0) {
                dF = 1;
            }
        }
        m_dist = ChiSquaredDistribution.of(dF);
    }

    @Override
    public Object getDistribution() {
        refresh();
        return m_dist;
    }

} // class ChiSquare
