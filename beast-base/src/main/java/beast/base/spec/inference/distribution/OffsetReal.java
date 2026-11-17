package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.DeclaresDomain;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import java.util.List;

/**
 * Wrapper that shifts a real-valued distribution by a fixed offset,
 * so that the effective parameter is {@code x - offset}.
 */
@Description("Offsets a real valued distribution.")
public class OffsetReal<D extends Real> extends ScalarDistribution<RealScalar<D>, Double> implements DeclaresDomain {

    // any real distribution, whatever the domain of its own parameter, e.g. LogNormal (PositiveReal)
    final public Input<ScalarDistribution<? extends RealScalar<?>, Double>> distributionInput = new Input<>("distribution",
            "the distribution to shift by the offset", Validate.REQUIRED);

	public final Input<RealScalar<?>> offsetInput = new Input<>("offset", "offset of origin (defaults to 0)", new RealScalarParam<Real>(0.0, Real.INSTANCE));

    public final Input<? extends Real> domainInput = new Input<>("domain",
            "the domain of the values this distribution puts probability on, e.g. PositiveReal for LogNormal " +
            "with offset 1 (optional). The shifted support must lie within it, for every value an estimated " +
            "offset can take: an error otherwise.");

    private ScalarDistribution<? extends RealScalar<?>, Double> dist;
    RealScalar<?> offset;
    private Real declaredDomain;
    /** how the offset can change, once known (see outsideDeclaredDomain()) */
    private DeclaredDomain.Range offsetRange;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public OffsetReal() {
    }

    public OffsetReal(
        ScalarDistribution<? extends RealScalar<?>, Double> dist,
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

    /**
     * @param domain the domain of the shifted values, e.g. PositiveReal.INSTANCE for LogNormal + 1
     * @throws RuntimeException if the shifted support reaches outside the domain
     */
    public OffsetReal(
        D domain,
        ScalarDistribution<? extends RealScalar<?>, Double> dist,
        double offset
    ) {
        try {
            initByName(
                "domain", domain,
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
        declaredDomain = domainInput.get();
        offsetRange = null;
        if (declaredDomain != null) {
            DeclaredDomain.check(this, getID(), declaredDomain, getLowerBoundOfParameter(), getUpperBoundOfParameter());
        }
        super.initAndValidate();
    }

    /**
     * @return the declared domain of the values this distribution puts probability on, or
     * Real when none is declared
     */
    @Override
    public Real getParameterDomain() {
        Real domain = domainInput.get();
        return domain != null ? domain : Real.INSTANCE;
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
                        dist.getLowerBoundOfParameter() + offset.getLower(),
                        dist.getUpperBoundOfParameter() + offset.getUpper(), "offset");
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
     public double getOffset() {
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
    protected double calcLogP(Double value) {
        if (outsideDeclaredDomain()) {
            return Double.NEGATIVE_INFINITY;
        }
        // refresh(); should have been called in dist.calcLogP
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
    
    // The CDF, survival function and their inverses must shift by the offset: the inherited
    // versions use getApacheDistribution(), which is the inner distribution's, unshifted.
    // TruncatedReal uses the CDF for its normaliser when it wraps an OffsetReal.

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
    public Double inverseCumulativeProbability(double p) {
        if (dist == null) {
            refresh();
        }
        return dist.inverseCumulativeProbability(p) + offset.get();
    }

    @Override
    public Double inverseSurvivalProbability(double p) {
        if (dist == null) {
            refresh();
        }
        return dist.inverseSurvivalProbability(p) + offset.get();
    }

    
    @Override
	public List<Double> sample() {
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
	public Double getLowerBoundOfParameter() {
		// Always refresh() (not only when dist is null), like ScalarDistribution's default via
		// getApacheDistribution(), so the bounds follow an inner distribution or offset replaced
		// after initialisation.
		refresh();
		return dist.getLowerBoundOfParameter() + offset.get();
	}

	@Override
	public Double getUpperBoundOfParameter() {
		// Always refresh() (not only when dist is null), like ScalarDistribution's default via
		// getApacheDistribution(), so the bounds follow an inner distribution or offset replaced
		// after initialisation.
		refresh();
		return dist.getUpperBoundOfParameter() + offset.get();
	}

} // class OffsetReal
