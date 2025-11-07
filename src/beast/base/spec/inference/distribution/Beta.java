package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.BetaDistribution;
import org.apache.commons.statistics.distribution.ContinuousDistribution;

import java.util.List;


@Description("Beta distribution, used as prior.  p(x;alpha,beta) = \frac{x^{alpha-1}(1-x)^{beta-1}} {B(alpha,beta)} " +
        "where B() is the beta function. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Beta extends TensorDistribution<RealScalar<UnitInterval>, UnitInterval, Double> {

    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "first shape parameter, defaults to 1");
    final public Input<RealScalar<PositiveReal>> betaInput = new Input<>("beta",
            "the other shape parameter, defaults to 1");

    private BetaDistribution dist = BetaDistribution.of(1, 1);

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

//    public Beta(List<RealScalar<UnitInterval>> iidparam,
//                RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> beta) {
//        try {
//            initByName("iidparam", iidparam, "alpha", alpha, "beta", beta);
//        } catch (Exception e) {
//            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
//                    " via initByName in constructor.", e );
//        }
//    }

    @Override
    public void initAndValidate() {
        // only call refresh() here when init, which will update the dist args.
        refresh();
        // param or iid
        super.initAndValidate();
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
    protected double calcLogP(Double... value) {
        return dist.logDensity(value[0]); // scalar
    }

    @Override
    protected List<RealScalar<UnitInterval>> sample() {
        ContinuousDistribution.Sampler sampler = dist.createSampler(rng);
        double x = sampler.sample();
        RealScalarParam<UnitInterval> param = new RealScalarParam<>(x, UnitInterval.INSTANCE);
        return List.of(param);
    }

} // class Beta
