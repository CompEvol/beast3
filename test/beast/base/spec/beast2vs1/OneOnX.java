package beast.base.spec.beast2vs1;


import beast.base.core.Description;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.type.RealScalar;
import org.apache.commons.math3.util.FastMath;

import java.util.List;


/**
 * Only used by test.
 */
@Description("OneOnX distribution.  f(x) = C/x for some normalizing constant C. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class OneOnX extends ScalarDistribution<RealScalar<Real>, Double> {

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public OneOnX() {}

    public OneOnX(RealScalar<Real> param) {
        try {
            initByName("param", param);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        // param or iid
        super.initAndValidate();
    }

    @Override
    public double calculateLogP() {
        // FastMath : faster performance with tiny accuracy cost
        logP = - FastMath.log(param.get());
        return logP;
    }

    @Override
	public void refresh() {

    }

    @Override
    public double density(double x) {
        return 1 / x;
    }

    @Override
    protected double calcLogP(Double value) {
        return -Math.log(value);
    }

    @Override
    protected List<Double> sample() {
        throw new UnsupportedOperationException("OneOnX does not support sampling.");
    }

} // class OneOnX
