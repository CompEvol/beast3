package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.LogNormalDistribution;

import java.util.List;


/**
 * @author Alexei Drummond
 */
@Description("A log-normal distribution with mean and variance parameters.")
public class LogNormal extends ScalarDistribution<RealScalar<PositiveReal>, Double> {

    final public Input<RealScalar<Real>> MParameterInput = new Input<>("M",
            "M parameter of lognormal distribution. " +
                    "Equal to the mean of the log-transformed distribution.");
    final public Input<RealScalar<PositiveReal>> SParameterInput = new Input<>("S",
            "S parameter of lognormal distribution. " +
                    "Equal to the standard deviation of the log-transformed distribution.");
    final public Input<Boolean> hasMeanInRealSpaceInput = new Input<>("meanInRealSpace",
            "Whether the M parameter is in real space, or in log-transformed space. " +
                    "Default false = log-transformed.", false);

    private boolean hasMeanInRealSpace;
    private LogNormalDistribution dist = LogNormalDistribution.of(0, 1);
    private ContinuousDistribution.Sampler sampler;

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

        // Floating point comparison:
        if (isNotEqual(dist.getMean(), mean) ||  isNotEqual(dist.getSigma(), sigma) ) {
            dist = LogNormalDistribution.of(mean, sigma);
            sampler = dist.createSampler(rng);
        } else if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
    }

    @Override
    public double calculateLogP() {
        return dist.logDensity(param.get()); // unbox value, faster
    }

    @Override
    protected double calcLogP(Double value) {
        return dist.logDensity(value); // scalar
    }

    @Override
    protected List<Double> sample() {
        final double x = sampler.sample();
        return List.of(x);
    }

}
