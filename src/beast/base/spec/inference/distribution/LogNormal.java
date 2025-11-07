package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.LogNormalDistribution;

import java.util.List;


/**
 * @author Alexei Drummond
 */
@Description("A log-normal distribution with mean and variance parameters.")
public class LogNormal extends TensorDistribution<RealScalar<PositiveReal>, PositiveReal, Double> {

    final public Input<RealScalar<Real>> MParameterInput = new Input<>("M",
            "M parameter of lognormal distribution. " +
                    "Equal to the mean of the log-transformed distribution.");
    final public Input<RealScalar<PositiveReal>> SParameterInput = new Input<>("S",
            "S parameter of lognormal distribution. " +
                    "Equal to the standard deviation of the log-transformed distribution.");
    final public Input<Boolean> hasMeanInRealSpaceInput = new Input<>("meanInRealSpace",
            "Whether the M parameter is in real space, or in log-transformed space. " +
                    "Default false = log-transformed.", false);

    protected boolean hasMeanInRealSpace;
    protected LogNormalDistribution dist = LogNormalDistribution.of(0, 1);

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public LogNormal() {}

    public LogNormal(RealScalar<PositiveReal> param,
                     RealScalar<Real> M, RealScalar<PositiveReal> S) {
        this(param, M, S, false);
    }

    public LogNormal(RealScalar<PositiveReal> param,
                     RealScalar<Real> M, RealScalar<PositiveReal> S, boolean meanInRealSpace) {

        try {
            initByName("param", param, "M", M, "S", S, "meanInRealSpace", meanInRealSpace);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
	public void initAndValidate() {
        hasMeanInRealSpace = hasMeanInRealSpaceInput.get();
        refresh();
        super.initAndValidate();
    }

    /**
     * make sure internal state is up to date *
     */
    void refresh() {
        double mean  = (MParameterInput.get() != null) ? MParameterInput.get().get() : 0.0;
        double sigma = (SParameterInput.get() != null) ? SParameterInput.get().get() : 1.0;

        if (hasMeanInRealSpace)
            mean = Math.log(mean) - (0.5 * sigma * sigma);

        if (Math.abs(dist.getMean() - mean) > EPS ||  Math.abs(dist.getSigma() - sigma) > EPS)
            dist = LogNormalDistribution.of(mean, sigma);
    }

    @Override
    protected double calcLogP(Double... value) {
        return dist.logDensity(value[0]); // scalar
    }

    @Override
    protected List<RealScalar<PositiveReal>> sample() {
        ContinuousDistribution.Sampler sampler = dist.createSampler(rng);
        double x = sampler.sample();
        RealScalarParam<PositiveReal> param = new RealScalarParam<>(x, PositiveReal.INSTANCE);
        return List.of(param);
    }

}
