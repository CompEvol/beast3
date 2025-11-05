package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.UniformContinuousDistribution;


@Description("Uniform distribution over a given interval (including lower and upper values)")
public class Uniform extends RealTensorDistribution<RealScalar<Real>, Real>
        implements Bounded<Double> {

    final public Input<RealScalar<Real>> lowerInput = new Input<>("lower",
            "lower bound on the interval, default 0");
    final public Input<RealScalar<Real>> upperInput = new Input<>("upper",
            "upper bound on the interval, default 1");

    // if (!Double.isFinite(upper - lower)) {
    //    throw new DistributionException("Range %s is not finite", upper - lower);
    protected UniformContinuousDistribution dist = UniformContinuousDistribution.of(0.0, 1.0);

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Uniform() {}

    public Uniform(RealScalar<Real> param,
                RealScalar<Real> lower, RealScalar<Real> upper) {

        try {
            initByName("param", param, "lower", lower, "upper", upper);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double lower  = (lowerInput.get() != null) ? lowerInput.get().get() : 0.0;
        double upper  = (upperInput.get()  != null) ? upperInput.get().get()  : 1.0;

        // Floating point comparison
        if (Math.abs(dist.getSupportLowerBound() - lower) > EPS
                || Math.abs(dist.getSupportUpperBound() - upper) > EPS)
            dist = UniformContinuousDistribution.of(lower, upper);
    }

    @Override
    protected UniformContinuousDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    protected RealScalar<Real> valueToTensor(double... value) {
        return new RealScalarParam<>(value[0], Real.INSTANCE);
    }

    @Override
    public Double getLower() {
        return dist.getSupportLowerBound();
    }

    @Override
    public Double getUpper() {
        return dist.getSupportUpperBound();
    }

    @Override
    public boolean lowerInclusive() {
        return true;
    }

    @Override
    public boolean upperInclusive() {
        return true;
    }
}
