package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;

import java.util.List;

import org.apache.commons.math.MathException;

@Description("Offsets a real valued distribution.")
public class OffsetReal extends ScalarDistribution<RealScalar<Real>, Double> {

    final public Input<ScalarDistribution<RealScalar<Real>, Double>> distributionInput = new Input<>("distribution",
            "precision of the normal distribution, defaults to 1", Validate.REQUIRED);
    
	public final Input<RealScalar<Real>> offsetInput = new Input<>("offset", "offset of origin (defaults to 0)", new RealScalarParam<Real>(0.0, Real.INSTANCE));

    private ScalarDistribution<RealScalar<Real>, Double> dist;
    RealScalar<Real> offset;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public OffsetReal() {
    }

    public OffsetReal(
        ScalarDistribution<RealScalar<Real>, Double> dist,
        double offset
    ) {

        try {
            initByName(
                "distribution", dist, 
                "offset", new RealScalarParam<Real>(offset, Real.INSTANCE)
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
        offset = offsetInput.get();
    }

     /**
      * @return  offset of distribution.
      */
     public double getOffset() {
         return offset.get();
     }

    @Override
    public double calculateLogP() {
        logP = logDensity(param.get() - offset.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Double value) {
        return dist.calcLogP(value - offset.get());
    }

    @Override
    public double logDensity(double x) {
        return dist.calcLogP(x - offset.get());
    }

    @Override
    public double density(double x) {
        return Math.exp(dist.calcLogP(x - offset.get()));
    }
    
    @Override
    public Double inverseCumulativeProbability(double p) throws MathException {
    	return dist.inverseCumulativeProbability(p) + offset.get();
    }

    
    @Override
    protected List<Double> sample() {
    	List<Double> samples = dist.sample();
    	for (int i = 0; i < samples.size(); i++) {
    		samples.set(i, samples.get(i) + offset.get());
    	}
    	return samples;
    }

    @Override
    public double getMean() {
        refresh();
        double mean = dist.getMean() + offset.get();
        return mean;
    }

    @Override
	protected Object getApacheDistribution() {
    	if (dist == null) {
    		refresh();
    	}
    	return dist.getApacheDistribution();
    }

    boolean isValid(double value) {
        double y = value;
        return Real.INSTANCE.isValid(y);
    }
    
    @Override
    public boolean isIntegerDistribution() {
    	return false;
    }

	@Override
	public Double getLower() {
    	if (dist == null) {
    		refresh();
    	}
		return dist.getLower() + offset.get();
	}

	@Override
	public Double getUpper() {
    	if (dist == null) {
    		refresh();
    	}
		return dist.getUpper() + offset.get();
	}

} // class OffsetReal
