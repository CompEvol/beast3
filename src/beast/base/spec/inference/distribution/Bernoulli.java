package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.BoolScalar;
import beast.base.spec.type.RealScalar;
import beast.base.util.Randomizer;
import org.apache.commons.math3.util.FastMath;

import java.util.List;

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
    void refresh() {
        p = (pInput.get() != null) ? pInput.get().get() : 1.0;
    }

    @Override
    public double calculateLogP() {
        // FastMath : faster performance with tiny accuracy cost
        return param.get() ? FastMath.log(p) : FastMath.log(1 - p);
    }

    @Override
    protected double calcLogP(Boolean value) {
        return value ? FastMath.log(p) : FastMath.log(1 - p);
    }

    @Override
    protected List<Boolean> sample() {
        boolean success = (Randomizer.nextDouble() < p);
        // Returning an immutable result
        return List.of(success);
    }

} // class
