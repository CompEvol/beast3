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
            "probability of success on each trial, greater than 0 (at 0 every count has probability 0); defaults to 0.5");

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

    /** true while p = 0: no success ever, so no count has positive probability */
    private boolean pIsZero;

    @Override
    public void refresh() {
        double p = (pInput.get() != null) ? pInput.get().get() : 0.5;
        // p = 0 is in the UnitInterval domain of p, but is not a distribution (Apache's
        // GeometricDistribution throws for it): every count gets probability 0 instead
        pIsZero = p == 0;
        if (!pIsZero && (isNotEqual(dist.getProbabilityOfSuccess(), p) || p < EPS)) {
            dist = GeometricDistribution.of(p);
            sampler = null;
        }
    }

    @Override
    public double calculateLogP() {
        logP = calcLogP(param.get());
        return logP;
    }

    /** log P(X = value); -Infinity for every value when p = 0, so MCMC rejects such a state */
    @Override
    protected double calcLogP(Integer value) {
        refresh(); // p may have been changed by MCMC
        return pIsZero ? Double.NEGATIVE_INFINITY : dist.logProbability(value);
    }

    /** the support, 0, 1, 2, ..., whatever p is (also for p = 0, so bounds checks never fail) */
    @Override
    public Integer getLowerBoundOfParameter() {
        return 0;
    }

    @Override
    public Integer getUpperBoundOfParameter() {
        return Integer.MAX_VALUE;
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

    /**
     * @throws IllegalArgumentException when p = 0, for which there is no distribution to
     * compute a CDF, quantile, mean or sample from
     */
    @Override
    protected GeometricDistribution getApacheDistribution() {
        refresh(); // parameters may have been changed by MCMC
        if (pIsZero) {
            throw new IllegalArgumentException(getClass().getSimpleName() + (getID() == null ? "" : " '" + getID() + "'")
                    + ": p is 0, but the probability of success must be greater than 0.");
        }
        return dist;
    }

} // class Geometric
