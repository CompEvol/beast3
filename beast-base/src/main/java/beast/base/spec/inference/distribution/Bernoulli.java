package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.BoolScalar;
import beast.base.spec.type.RealScalar;
import beast.base.util.Randomizer;


import java.util.List;

/**
 * Bernoulli distribution for boolean-valued parameters, parameterised by
 * the probability of success (p).
 */
@Description("Bernoulli distribution, coin toss distribution prior.")
public class Bernoulli extends ScalarDistribution<BoolScalar, Boolean> {

    final public Input<RealScalar<UnitInterval>> pInput = new Input<>("p",
            "the probability of success.");

    private double p;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Bernoulli() {}

    public Bernoulli(BoolScalar param, RealScalar<UnitInterval> p) {
        try {
            initByName("param", param, "p", p);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        // only call refresh() here when init, which will update the dist args.
        refresh();
        // param or iid
        super.initAndValidate();
    }

    /**
     * make sure internal state is up to date *
     */
    @Override
	public void refresh() {
        p = (pInput.get() != null) ? pInput.get().get() : 1.0;
    }

    @Override
    public double calculateLogP() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
    	logP = param.get() ? Math.log(p) : Math.log(1 - p);
    	return logP;
    }

    @Override
    protected double calcLogP(Boolean value) {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        return value ? Math.log(p) : Math.log(1 - p);
    }

    @Override
    public Boolean getLowerBoundOfParameter() {
        throw new IllegalStateException(getClass().getName() + " does not support lower bounds.");
    }

    @Override
    public Boolean getUpperBoundOfParameter() {
        throw new IllegalStateException(getClass().getName() + " does not support upper bounds.");
    }

    @Override
	public List<Boolean> sample() {
        boolean success = (Randomizer.nextDouble() < p);
        // Returning an immutable result
        return List.of(success);
    }

} // class
