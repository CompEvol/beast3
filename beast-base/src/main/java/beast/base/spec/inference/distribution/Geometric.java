package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.DiscreteDistribution;
import org.apache.commons.statistics.distribution.GeometricDistribution;

import java.util.List;


/**
 * Geometric distribution: the number of failures before the first success,
 * with probability of success p. Its support is 0, 1, 2, ...
 */
@Description("Geometric distribution, the number of failures before the first success, " +
        "f(k; p) = (1 - p)^k p for k = 0, 1, 2, ... It has mean (1 - p) / p. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Geometric extends ScalarDistribution<IntScalar<NonNegativeInt>, Integer> {

    final public Input<RealScalar<UnitInterval>> pInput = new Input<>("p",
            "probability of success on each trial, greater than 0; defaults to 0.5");

    private GeometricDistribution dist = GeometricDistribution.of(0.5);
    private DiscreteDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Geometric() {
    }

    public Geometric(IntScalar<NonNegativeInt> param, RealScalar<UnitInterval> p) {
        try {
            initByName("param", param, "p", p);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e);
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate();
    }

    @Override
    public void refresh() {
        double p = (pInput.get() != null) ? pInput.get().get() : 0.5;
        if (isNotEqual(dist.getProbabilityOfSuccess(), p)) {
            // throws IllegalArgumentException for p = 0, which is not a distribution
            dist = GeometricDistribution.of(p);
            sampler = null;
        }
    }

    @Override
    public double calculateLogP() {
        logP = getApacheDistribution().logProbability(param.get());
        return logP;
    }

    @Override
    public List<Integer> sample() {
        GeometricDistribution d = getApacheDistribution();
        if (sampler == null) {
            sampler = d.createSampler(rng);
        }
        return List.of(sampler.sample());
    }

    @Override
    public double getMean() {
        return getApacheDistribution().getMean();
    }

    @Override
    public boolean isIntegerDistribution() {
        return true;
    }

    @Override
    protected GeometricDistribution getApacheDistribution() {
        refresh(); // parameters may have been changed by MCMC
        return dist;
    }

} // class Geometric
