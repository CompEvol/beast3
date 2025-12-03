package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;

import java.util.List;

import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.UniformContinuousDistribution;


@Description("Truncates a real valued distribution to the interval [lower,upper].")
public class TruncatedRealDistribution<D extends Real> extends ScalarDistribution<RealScalar<D>, Double> {


    final public Input<ScalarDistribution<RealScalar<Real>, Double>> distributionInput = new Input<>("distribution",
            "precision of the normal distribution, defaults to 1", Validate.REQUIRED);
    final public Input<RealScalar<Real>> lowerInput = new Input<RealScalar<Real>>("lower", "Lower end of the truncation interval.");
    final public Input<RealScalar<Real>> upperInput = new Input<RealScalar<Real>>("upper", "Upper end of the truncation interval.");

    private ScalarDistribution<RealScalar<Real>, Double> dist;
    RealScalar<Real> lower, upper;


    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public TruncatedRealDistribution() {
    }

    public TruncatedRealDistribution(
        ScalarDistribution<RealScalar<Real>, Double> dist,
        double lower,
        double upper
    ) {
        assert (D.INSTANCE.getLower() <= lower) && (lower < upper) && (upper <= D.INSTANCE.getUpper());

        try {
            initByName("distribution", dist, "lower", lower, "upper", upper);
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
        dist = distributionInput.get();
        lower = lowerInput.get();
        if (lower == null)
            lower = new RealScalarParam<>(D.INSTANCE.getLower(), PositiveReal.INSTANCE);

        upper = upperInput.get();
        if (upper == null)
            upper = new RealScalarParam<>(D.INSTANCE.getUpper(), PositiveReal.INSTANCE);
    }

    @Override
    public double calculateLogP() {
        return calcLogP(param.get());
    }

    @Override
    protected double calcLogP(Double value) {
        if (isValid(value))
            return dist.calcLogP(value); // scalar
        else
            return Double.NEGATIVE_INFINITY;
    }
    
    @Override
    protected List<Double> sample() {
        // Get the Apache distribution of the inner distribution object (provides inverse CDF)
        ContinuousDistribution innerDist = (ContinuousDistribution) dist.getApacheDistribution();
        
        // Sample CDF value that is compativle with the valid interval [lower, upper] 
        double uLower = innerDist.inverseCumulativeProbability(lower.get());
        double uUpper = innerDist.inverseCumulativeProbability(upper.get());
        double u = UniformContinuousDistribution.of(uLower, uUpper).createSampler(rng).sample();

        // Transform using inverse CDF of the inner distribution
        double x = innerDist.inverseCumulativeProbability(u);

        // Alternative implementation using rejection sampling (less efficient, but simpler logic):
        // double x = dist.sample().get(0);
        // int iterations = 0;
        // while (!isValid(x)) {
        //     if (iterations >= 10000) {
        //     throw new RuntimeException(
        //         "Failed to sample from truncated distribution after " + maxIterations + 
        //         " attempts. The truncation interval [" + lower.get() + ", " + upper.get() + 
        //         "] may have negligible probability mass.");
        //     }
        //     x = dist.sample().get(0);
        //     iterations++;
        // }

        // Assert that the resulting sample is valid
        assert isValid(x);

        // Returning an immutable result
        return List.of(x);
    }

    @Override
    public Object getApacheDistribution() {
        throw new UnsupportedOperationException("Not implemented for TruncatedRealDistribution");
    }

    boolean isValid(double value) {
        return D.INSTANCE.isValid(value) && lower.get() <= value && value <= upper.get();
    }

    /* Implementations for some common domains */
    
    class TruncatedPositiveRealDistribution extends TruncatedRealDistribution<PositiveReal> {}
    class TruncatedNonNegativeRealDistribution extends TruncatedRealDistribution<NonNegativeReal> {}
    class TruncatedUnitIntervalRealDistribution extends TruncatedRealDistribution<UnitInterval> {}

} // class TruncatedRealDistribution
