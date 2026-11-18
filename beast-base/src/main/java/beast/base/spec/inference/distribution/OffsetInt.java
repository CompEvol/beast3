package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.type.IntScalar;
import java.util.List;

/**
 * Wrapper that shifts an integer-valued distribution by a fixed offset,
 * so that the effective parameter is {@code x - offset}.
 */
@Description("Offsets a integer valued distribution.")
public class OffsetInt<D extends Int> extends ScalarDistribution<IntScalar<D>, Integer> implements WrapsDistribution {

    // any integer distribution, whatever the domain of its own parameter, e.g. Poisson (NonNegativeInt)
    final public Input<ScalarDistribution<? extends IntScalar<?>, Integer>> distributionInput = new Input<>("distribution",
            "the distribution to shift by the offset", Validate.REQUIRED);

	public final Input<IntScalar<?>> offsetInput = new Input<>("offset", "offset of origin (defaults to 0)", new IntScalarParam<Int>(0, Int.INSTANCE));

    public final Input<? extends Int> domainInput = new Input<>("domain",
            "the domain of the values this distribution puts probability on, e.g. PositiveInt for Poisson " +
            "with offset 1 (optional). The shifted support must lie within it, for every value an estimated " +
            "offset can take: an error otherwise.");

    private ScalarDistribution<? extends IntScalar<?>, Integer> dist;
    IntScalar<?> offset;
    private Int declaredDomain;
    /** how the offset can change, once known (see outsideDeclaredDomain()) */
    private DeclaredDomain.Range offsetRange;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public OffsetInt() {
    }

    public OffsetInt(
        ScalarDistribution<? extends IntScalar<?>, Integer> dist,
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

    /**
     * @param domain the domain of the shifted values, e.g. PositiveInt.INSTANCE for Poisson + 1
     * @throws RuntimeException if the shifted support reaches outside the domain, e.g. Poisson - 1
     */
    public OffsetInt(
        D domain,
        ScalarDistribution<? extends IntScalar<?>, Integer> dist,
        int offset
    ) {
        try {
            initByName(
                "domain", domain,
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
        declaredDomain = domainInput.get();
        offsetRange = null;
        if (declaredDomain != null) {
            DeclaredDomain.check(this, getID(), declaredDomain, getLowerBoundOfParameter(), getUpperBoundOfParameter());
        }
        super.initAndValidate();
    }

    /**
     * @return the declared domain of the values this distribution puts probability on, or
     * Int when none is declared
     */
    @Override
    public Int getParameterDomain() {
        Int domain = domainInput.get();
        return domain != null ? domain : Int.INSTANCE;
    }

    /**
     * Whether the offset has put the support outside the declared domain. On the first
     * evaluation, when all objects are initialised, an estimated offset's range is checked
     * once (an error when it does not fit, see {@link DeclaredDomain}); only a calculated
     * offset, whose range is not known, is checked on every evaluation.
     */
    private boolean outsideDeclaredDomain() {
        if (declaredDomain == null) {
            return false;
        }
        if (offsetRange == null || offsetRange == DeclaredDomain.Range.NOT_YET_KNOWN) {
            offsetRange = DeclaredDomain.range(offset);
            if (offsetRange == DeclaredDomain.Range.ESTIMATED) {
                refresh();
                DeclaredDomain.checkEstimated(this, getID(), declaredDomain,
                        DeclaredDomain.add(dist.getLowerBoundOfParameter(), offset.getLower()),
                        DeclaredDomain.add(dist.getUpperBoundOfParameter(), offset.getUpper()), "offset");
            } else if (offsetRange == DeclaredDomain.Range.CALCULATED) {
                DeclaredDomain.warnCalculated(this, getID(), "offset");
            }
        }
        return offsetRange == DeclaredDomain.Range.CALCULATED
                && DeclaredDomain.outside(declaredDomain, getLowerBoundOfParameter(), getUpperBoundOfParameter()) != null;
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
        if (outsideDeclaredDomain()) {
            // a calculated offset has moved the support outside the declared domain
            logP = Double.NEGATIVE_INFINITY;
            return logP;
        }
        logP = logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Integer value) {
        if (outsideDeclaredDomain()) {
            return Double.NEGATIVE_INFINITY;
        }
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
    
    // The CDF, survival function and their inverses must shift by the offset: the inherited
    // versions use getApacheDistribution(), which is the inner distribution's, unshifted.
    // TruncatedInt uses them for its normaliser when it wraps an OffsetInt.

    @Override
    public double cumulativeProbability(double x) {
        if (dist == null) {
            refresh();
        }
        return dist.cumulativeProbability(x - offset.get());
    }

    @Override
    public double survivalProbability(double x) {
        if (dist == null) {
            refresh();
        }
        return dist.survivalProbability(x - offset.get());
    }

    @Override
    public Integer inverseCumulativeProbability(double p) {
        if (dist == null) {
            refresh();
        }
        return shift(dist.inverseCumulativeProbability(p));
    }

    @Override
    public Integer inverseSurvivalProbability(double p) {
        if (dist == null) {
            refresh();
        }
        return shift(dist.inverseSurvivalProbability(p));
    }

    /**
     * @return x + offset, keeping Integer.MIN_VALUE and Integer.MAX_VALUE as "no limit",
     * as the bounds below do, and saturating instead of wrapping around
     */
    private int shift(int x) {
        return DeclaredDomain.add(x, offset.get());
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
		// Always refresh() (not only when dist is null), like ScalarDistribution's default via
		// getApacheDistribution(), so the bounds follow an inner distribution or offset replaced
		// after initialisation.
		refresh();
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
		// Always refresh() (not only when dist is null), like ScalarDistribution's default via
		// getApacheDistribution(), so the bounds follow an inner distribution or offset replaced
		// after initialisation.
		refresh();
		int upper = dist.getUpperBoundOfParameter();
		// Integer.MAX_VALUE means "no upper limit" (e.g. Poisson; like +Infinity in OffsetReal), so keep it unbounded
		if (upper == Integer.MAX_VALUE) {
			return upper;
		}
		// saturate at Integer.MAX_VALUE instead of wrapping around, as BoundUtils does with Math.min
		return (int) Math.min(Integer.MAX_VALUE, (long) upper + offset.get());
	}


    @Override
    public ScalarDistribution<? extends IntScalar<?>, Integer> getInnerDistribution() {
        return distributionInput.get();
    }

    @Override
    public boolean truncates() {
        return false;
    }

    @Override
    public String supportOutsideDomainWith(ScalarDistribution<?, ?> inner, Bounded<?> domain) {
        int shift = offsetInput.get().get();
        return DeclaredDomain.outside(domain,
                DeclaredDomain.add((Integer) inner.getLowerBoundOfParameter(), shift),
                DeclaredDomain.add((Integer) inner.getUpperBoundOfParameter(), shift));
    }

} // class OffsetInt
