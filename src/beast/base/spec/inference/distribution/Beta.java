package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.BetaDistribution;


@Description("Beta distribution, used as prior.  p(x;alpha,beta) = \frac{x^{alpha-1}(1-x)^{beta-1}} {B(alpha,beta)} " +
        "where B() is the beta function. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Beta extends RealTensorDistribution<RealScalar<UnitInterval>, UnitInterval> {

    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "first shape parameter, defaults to 1");
    final public Input<RealScalar<PositiveReal>> betaInput = new Input<>("beta",
            "the other shape parameter, defaults to 1");

    protected BetaDistribution dist = BetaDistribution.of(1, 1);

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Beta() {}

    public Beta(RealScalar<UnitInterval> param,
                RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> beta) {

        try {
            initByName("param", param, "alpha", alpha, "beta", beta);
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
        double alpha = (alphaInput.get() != null) ? alphaInput.get().get() : 1.0;
        double beta  = (betaInput.get()  != null) ? betaInput.get().get()  : 1.0;

        // Floating point comparison
        if (Math.abs(dist.getAlpha() - alpha) > EPS ||  Math.abs(dist.getBeta() - beta) > EPS)
            dist = BetaDistribution.of(alpha, beta);
    }

    @Override
    protected BetaDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    protected RealScalar<UnitInterval> valueToTensor(double value) {
        return new RealScalarParam<>(value, UnitInterval.INSTANCE);
    }

    @Override
    protected double getMeanWithoutOffset() {
    	return dist.getAlpha() / (dist.getAlpha() + dist.getBeta());
    }
} // class Beta
