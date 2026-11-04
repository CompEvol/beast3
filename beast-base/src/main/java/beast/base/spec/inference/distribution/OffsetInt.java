package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.type.IntScalar;
import java.util.List;

/**
 * Wrapper that shifts an integer-valued distribution by a fixed offset,
 * so that the effective parameter is {@code x - offset}.
 */
@Description("Offsets a integer valued distribution.")
public class OffsetInt extends ScalarDistribution<IntScalar<Int>, Integer> {

    final public Input<ScalarDistribution<IntScalar<Int>, Integer>> distributionInput = new Input<>("distribution",
            "precision of the normal distribution, defaults to 1", Validate.REQUIRED);
    
	public final Input<IntScalar<Int>> offsetInput = new Input<>("offset", "offset of origin (defaults to 0)", new IntScalarParam<Int>(0, Int.INSTANCE));

    private ScalarDistribution<IntScalar<Int>, Integer> dist;
    IntScalar<Int> offset;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public OffsetInt() {
    }

    public OffsetInt(
        ScalarDistribution<IntScalar<Int>, Integer> dist,
        int offset
    ) {

        try {
            initByName(
                "distribution", dist, 
                "offset", new IntScalarParam<Int>(offset, Int.INSTANCE)
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
        dist.refresh();
        offset = offsetInput.get();
    }

     /**
      * @return  offset of distribution.
      */
     public int getOffset() {
         return offset.get();
     }

    @Override
    public double calculateLogP() {
        logP = logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Integer value) {
        // refresh(); should have been called in dist.calcLogP
        return dist.calcLogP(value - offset.get());
    }

    @Override
    public double logDensity(double x) {
        return dist.calcLogP((int)x - offset.get());
    }

    @Override
    public double density(double x) {
        return Math.exp(dist.calcLogP((int)x - offset.get()));
    }
    
    @Override
    public Integer inverseCumulativeProbability(double p) {
    	return dist.inverseCumulativeProbability(p) + offset.get();
    }
    
    @Override
	public List<Integer> sample() {
    	List<Integer> samples = dist.sample();
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

    boolean isValid(int value) {
        int y = value - getOffset();
        return Int.INSTANCE.isValid(y);
    }
    
    @Override
    public boolean isIntegerDistribution() {
    	return true;
    }

	@Override
	public Integer getLowerBoundOfParameter() {
    	if (dist == null) { // assuming bounds are not estimated during MCMC
    		refresh();
    	}
		int lower = dist.getLowerBoundOfParameter();
		// Integer.MIN_VALUE means "no lower limit" (like -Infinity in OffsetReal), so keep it unbounded
		if (lower == Integer.MIN_VALUE) {
			return lower;
		}
		// saturate at Integer.MIN_VALUE instead of wrapping around, as BoundUtils does with Math.max
		return (int) Math.max(Integer.MIN_VALUE, (long) lower + offset.get());
	}

	@Override
	public Integer getUpperBoundOfParameter() {
    	if (dist == null) {
    		refresh();
    	}
		int upper = dist.getUpperBoundOfParameter();
		// Integer.MAX_VALUE means "no upper limit" (e.g. Poisson; like +Infinity in OffsetReal), so keep it unbounded
		if (upper == Integer.MAX_VALUE) {
			return upper;
		}
		// saturate at Integer.MAX_VALUE instead of wrapping around, as BoundUtils does with Math.min
		return (int) Math.min(Integer.MAX_VALUE, (long) upper + offset.get());
	}

} // class OffsetInt
