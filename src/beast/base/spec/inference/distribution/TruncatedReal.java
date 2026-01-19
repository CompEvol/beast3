package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.math.MathException;
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
    public double cumulativeProbability(double x) {
        if (x < lower.get()) {
            return 0.0;
        } else if (x > upper.get()) {
            return 1.0;
        } else {
            double lowerP = getLowerCDF();
            double upperP = getUpperCDF();
            double p = getInnerDistribution().cumulativeProbability(x);
            return (p - lowerP) / (upperP - lowerP);
        }
    }

    @Override
    public Double inverseCumulativeProbability(double p) throws MathException {
        if (p < 0.0 || p > 1.0) {
            throw new MathException("Probability p must be in [0, 1]");
        }
        double lowerP = getLowerCDF();
        double upperP = getUpperCDF();
        double adjustedP = lowerP + p * (upperP - lowerP);
        return getInnerDistribution().inverseCumulativeProbability(adjustedP);
    }

    public ScalarDistribution<RealScalar<Real>, Double> getInnerDistribution() {
    	if (dist == null) {
    		refresh();
    	}
        return dist;
    }

    @Override
    public Double getLowerBoundOfParameter() {
        return Math.max(lower.get(), getInnerDistribution().getLowerBoundOfParameter());
    }

    @Override
    public Double getUpperBoundOfParameter() {
        return Math.min(upper.get(), getInnerDistribution().getUpperBoundOfParameter());
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
        double x;
        try {
            x = getInnerDistribution().inverseCumulativeProbability(u);
        } catch (MathException e) {
            throw new RuntimeException("Failed to sample from truncated distribution", e);
            // TODO use rejection sampling as fallback?
        }

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
        ScalarDistribution<RealScalar<Real>, Double> innerDist = getInnerDistribution();
        double cdfLower = innerDist.cumulativeProbability(lower.get()) + EPS;  // avoid CDF of 0
        double cdfUpper = innerDist.cumulativeProbability(upper.get()) - EPS;  // avoid CDF of 1
        
        // Numerical integration using Simpson's rule
        return simpsonIntegration(innerDist, cdfLower, cdfUpper, 20_000);
    }

    private double simpsonIntegration(ScalarDistribution<RealScalar<Real>, Double> dist, double cdfLower, double cdfUpper, int n) {
        if (n % 2 != 0)  // Ensure even number of intervals
            n++;
        
        double stepSize = (cdfUpper - cdfLower) / n;
        double Z = cdfUpper - cdfLower;
        
        double sum = 0.0;
        for (int i = 0; i <= n; i++) {
            // Even spacing in quantile space
            double u = cdfLower + i * stepSize;
            double x;
            try {
                x = dist.inverseCumulativeProbability(u);
            } catch (MathException e) {
                throw new RuntimeException("Failed to compute inverse CDF during mean calculation", e);
            }
            
            // Weight for Simpson's rule
            double weight = (i == 0 || i == n) ? 1.0 : ((i % 2 == 1) ? 4.0 : 2.0);
            
            // Integrand is just x
            sum += weight * x;
        }
        return sum * stepSize / Z / 3.0;
    }

    @Override
	protected Object getApacheDistribution() {
    	throw new RuntimeException("Not implemented for TruncatedReal distribution");
    }

    boolean isValid(double value) {
        return Real.INSTANCE.isValid(value) && lower.get() <= value && value <= upper.get();
    }

} // class TruncatedReal
