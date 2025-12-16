package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.math.MathException;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.UniformContinuousDistribution;

import java.util.List;


@Description("Truncates a real valued distribution to the interval [lower,upper]. " +
             "The base distribution itself should not define any parameters. " +
        "All parameters should be passed via TruncatedRealDistribution.")
public class TruncatedReal extends ScalarDistribution<RealScalar<Real>, Double> {


    final public Input<ScalarDistribution<RealScalar<Real>, Double>> distributionInput = new Input<>("distribution",
            "precision of the normal distribution, defaults to 1", Validate.REQUIRED);
    final public Input<RealScalar<Real>> lowerInput = new Input<>("lower", "Lower end of the truncation interval.");
    final public Input<RealScalar<Real>> upperInput = new Input<>("upper", "Upper end of the truncation interval.");

    private ScalarDistribution<RealScalar<Real>, Double> dist;
    RealScalar<Real> lower, upper;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public TruncatedReal() {
    }

    public TruncatedReal(
        ScalarDistribution<RealScalar<Real>, Double> dist,
        double lower,
        double upper
    ) {
        assert (Real.INSTANCE.getLower() <= lower) && (lower < upper) && (upper <= Real.INSTANCE.getUpper());

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
        dist.initAndValidate();
        super.initAndValidate();

//        if (offsetInput.get() != 0.0)
//            throw new IllegalArgumentException("Non-zero offset not allowed for " + getClass().getName() + 
//                    " distribution. Set offset in base distribution (distributionInput) instead");

        // Set bounds in parameter in the future (for early reject in operators)
//        RealScalar<Real> param = paramInput.get();
//        if (param instanceof RealScalarParam<Real> p) {
//            p.setLower(lower.get());
//            p.setUpper(upper.get());
//        }
    }

    /**
     * make sure internal state is up to date *
     */
    @Override
    public void refresh() {
        dist = distributionInput.get();
        lower = lowerInput.get();
        if (lower == null)
            lower = new RealScalarParam<>(Real.INSTANCE.getLower(), Real.INSTANCE);

        upper = upperInput.get();
        if (upper == null)
            upper = new RealScalarParam<>(Real.INSTANCE.getUpper(), Real.INSTANCE);
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

    @Override
    public double logDensity(double x) {
        if (isValid(x))
            return dist.calcLogP(x) - Math.log(1 - probOutOfBounds());
        else
            return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double density(double x) {
        return Math.exp(logDensity(x));
    }
    
    @Override
    public double inverseCumulativeProbability(double p) throws MathException {
        double lowerP = getLowerCDF();
        double upperP = (1-getUpperCDF());
        p = lowerP + (1 - upperP - lowerP) * p;
    	return super.inverseCumulativeProbability(p);
    }

    // Get the Apache distribution of the inner distribution object (provides inverse CDF)
    ContinuousDistribution getInnerDistribution() {
    	if (dist == null) {
    		refresh();
    	}
        return (ContinuousDistribution) dist.getApacheDistribution();
    }

    @Override
    public Double getLower() {
    	ContinuousDistribution dist = getInnerDistribution();
    	if (dist == null) {
    		refresh();
    		dist = getInnerDistribution();
    	}
        return Math.max(lower.get(), dist.getSupportLowerBound());
    }

    @Override
    public Double getUpper() {
    	ContinuousDistribution dist = getInnerDistribution();
    	if (dist == null) {
    		refresh();
    		dist = getInnerDistribution();
    	}
        return Math.min(upper.get(), dist.getSupportUpperBound());
    }

    double getLowerCDF() {
        return getInnerDistribution().cumulativeProbability(lower.get());
    }

    double getUpperCDF() {
        return getInnerDistribution().cumulativeProbability(upper.get());
    }

    double probOutOfBounds() {
        double probOOB = getLowerCDF() + (1-getUpperCDF());
        assert 0.0 <= probOOB && probOOB <= 1.0;
        // System.out.println("*" + probOOB);
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
        double x = getInnerDistribution().inverseCumulativeProbability(u);

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
        double cdfLower = innerDist.cumulativeProbability(lower.get()) + EPS;  // avoid CDF of 0
        double cdfUpper = innerDist.cumulativeProbability(upper.get()) - EPS;  // avoid CDF of 1
        
        // Numerical integration using Simpson's rule
        return simpsonIntegration(innerDist, cdfLower, cdfUpper, 20_000);
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
    	return distributionInput.get().getApacheDistribution();
    }

    boolean isValid(double value) {
        return Real.INSTANCE.isValid(value) && lower.get() <= value && value <= upper.get();
    }

} // class TruncatedReal
