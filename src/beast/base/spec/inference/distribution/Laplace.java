package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.LaplaceDistribution;

@Description("Laplace distribution.    f(x|\\mu,b) = \\frac{1}{2b} \\exp \\left( -\\frac{|x-\\mu|}{b} \\right)" +
        "The probability density function of the Laplace distribution is also reminiscent of the normal distribution; " +
        "however, whereas the normal distribution is expressed in terms of the squared difference from the mean ?, " +
        "the Laplace density is expressed in terms of the absolute difference from the mean. Consequently the Laplace " +
        "distribution has fatter tails than the normal distribution.")
public class Laplace extends RealTensorDistribution<RealScalar<Real>, Real> {

    final public Input<RealScalar<Real>> muInput = new Input<>("mu",
            "location parameter, defaults to 0");
    final public Input<RealScalar<PositiveReal>> scaleInput = new Input<>("scale",
            "scale parameter, defaults to 1");

    protected LaplaceDistribution dist = LaplaceDistribution.of(0, 1);

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Laplace() {}

    public Laplace(RealScalar<Real> param,
                   RealScalar<Real> mu, RealScalar<PositiveReal> scale) {

        try {
            initByName("param", param, "mu", mu, "scale", scale);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
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
        double mu = (muInput.get() != null) ? muInput.get().get() : 0.0;
        double scale  = (scaleInput.get()  != null) ? scaleInput.get().get()  : 1.0;

        // Floating point comparison
        if (Math.abs(dist.getLocation() - mu) > EPS ||  Math.abs(dist.getScale() - scale) > EPS)
            dist = LaplaceDistribution.of(mu, scale);
    }

    @Override
    protected LaplaceDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    protected RealScalar<Real> valueToTensor(double value) {
        return new RealScalarParam<>(value, Real.INSTANCE);
    }
    
    @Override
    protected double getMeanWithoutOffset() {
    	return (muInput.get() != null) ? muInput.get().get() : 0.0;
    }

} // class