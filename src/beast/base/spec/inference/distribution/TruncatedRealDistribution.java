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
            initByName(
                "distribution", dist, 
                "lower", new RealScalarParam<Real>(lower, Real.INSTANCE),
                "upper", new RealScalarParam<Real>(upper, Real.INSTANCE)
            );
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate();

        if (offsetInput.get() != 0.0)
            throw new IllegalArgumentException("Non-zero offset not allowed for " + getClass().getName() + 
                    " distribution. Set offset in base distribution (distributionInput) instead");

        // Set bounds in parameter in the future (for early reject in operators)
        RealScalar<D> param = paramInput.get();
        if (param instanceof RealScalarParam<D> p) {
            p.setLower(lower.get() + getOffset());
            p.setUpper(upper.get() + getOffset());
        }
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        dist = distributionInput.get();
        lower = lowerInput.get();
        if (lower == null)
            lower = new RealScalarParam<>(D.INSTANCE.getLower(), D.INSTANCE);

        upper = upperInput.get();
        if (upper == null)
            upper = new RealScalarParam<>(D.INSTANCE.getUpper(), D.INSTANCE);
    }

     /**
      * @return  offset of distribution.
      */
     @Override
     public double getOffset() {
         return 0.0;
         // TODO: do we want to allow an offset in the TruncatedRealDistribution?
         // It seems redundant, since the inner distribution itself has an offset, 
         // but I'm not sure whether assuming offset==0 breaks things.
         // TODO: If we allow an offset, adjust logDensity accordingly.
     }

    @Override
    public double calculateLogP() {
        logP = logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Double value) {
        return logDensity(value);
    }

    public double logDensity(double x) {
        if (isValid(x))
            return dist.calcLogP(x) - Math.log(1 - probOutOfBounds());
        else
            return Double.NEGATIVE_INFINITY;
    }

    public double density(double x) {
        return Math.exp(logDensity(x));
    }

    // Get the Apache distribution of the inner distribution object (provides inverse CDF)
    ContinuousDistribution getInnerDistribution() {
        return (ContinuousDistribution) dist.getApacheDistribution();
    }

    double getLowerCDF() {
        return getInnerDistribution().cumulativeProbability(lower.get() - dist.getOffset());
    }

    double getUpperCDF() {
        return getInnerDistribution().cumulativeProbability(upper.get() - dist.getOffset());
    }

    double probOutOfBounds() {
        double probOOB = getLowerCDF() + (1-getUpperCDF());
        assert 0.0 <= probOOB && probOOB <= 1.0;
        System.out.println("*" + probOOB);
        return probOOB;
    }
    
    @Override
    protected List<Double> sample() {
        // Sample CDF value that is compativle with the valid interval [lower, upper] 
        double uLower = getLowerCDF();
        double uUpper = getUpperCDF();
        UniformContinuousDistribution uDist = UniformContinuousDistribution.of(uLower, uUpper);
        double u = uDist.createSampler(rng).sample();

        // Transform using inverse CDF of the inner distribution
        double x = getInnerDistribution().inverseCumulativeProbability(u) + dist.getOffset();

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
    public double getMean() {
        refresh();
        ContinuousDistribution innerDist = getInnerDistribution();
        double offset = dist.getOffset();
        double cdfLower = innerDist.cumulativeProbability(lower.get() - offset) + EPS;  // avoid CDF of 0
        double cdfUpper = innerDist.cumulativeProbability(upper.get() - offset) - EPS;  // avoid CDF of 1
        
        // Numerical integration using Simpson's rule
        return simpsonIntegration(innerDist, cdfLower, cdfUpper, 20_000) + offset;
    }

    private double simpsonIntegration(ContinuousDistribution dist, double cdfLower, double cdfUpper, int n) {
        if (n % 2 != 0)  // Ensure even number of intervals
            n++;
        
        double stepSize = (cdfUpper - cdfLower) / n;
        double Z = cdfUpper - cdfLower;
        
        double sum = 0.0;
        for (int i = 0; i <= n; i++) {
            // Even spacing in quantile space
            double u = cdfLower + i * stepSize;
            double x = dist.inverseCumulativeProbability(u);
            
            // Weight for Simpson's rule
            double weight = (i == 0 || i == n) ? 1.0 : ((i % 2 == 1) ? 4.0 : 2.0);
            
            // Integrand is just x
            sum += weight * x;
        }
        return sum * stepSize / Z / 3.0;
    }

    @Override
    public Object getApacheDistribution() {
        throw new UnsupportedOperationException("Not implemented for TruncatedRealDistribution");
    }

    boolean isValid(double value) {
        double y = value - getOffset();
        return D.INSTANCE.isValid(y) && lower.get() <= y && y <= upper.get();
    }


    /* Implementations for some common domains */
    
    class TruncatedPositiveRealDistribution extends TruncatedRealDistribution<PositiveReal> {}
    class TruncatedNonNegativeRealDistribution extends TruncatedRealDistribution<NonNegativeReal> {}
    class TruncatedUnitIntervalRealDistribution extends TruncatedRealDistribution<UnitInterval> {}

} // class TruncatedRealDistribution
