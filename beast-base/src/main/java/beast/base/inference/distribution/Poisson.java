package beast.base.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;
import org.apache.commons.statistics.distribution.PoissonDistribution;

/**
 * @deprecated replaced by {@link beast.base.spec.inference.distribution.Poisson}
 */
@Deprecated
@Description("Poisson distribution, used as prior  f(k; lambda)=\\frac{lambda^k e^{-lambda}}{k!}  " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Poisson extends ParametricDistribution {
    final public Input<Function> lambdaInput = new Input<>("lambda", "rate parameter, defaults to 1");

    PoissonDistribution dist = PoissonDistribution.of(1);


    // Must provide empty constructor for construction by XML. Note that this constructor DOES NOT call initAndValidate();
    public Poisson() {
    }

    public Poisson(RealParameter lambda) {

        try {
            initByName("lambda", lambda);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initByName lambda parameter when constructing Poisson instance.");
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double m_fLambda;
        if (lambdaInput.get() == null) {
            m_fLambda = 1;
        } else {
            m_fLambda = lambdaInput.get().getArrayValue();
            if (m_fLambda < 0) {
                m_fLambda = 1;
            }
        }
        dist = PoissonDistribution.of(m_fLambda);
    }

    @Override
    public Object getDistribution() {
        refresh();
        return dist;
    }

    @Override
    public double getMeanWithoutOffset() {
    	return lambdaInput.get().getArrayValue();
    }

} // class Poisson
