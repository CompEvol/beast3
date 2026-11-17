package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.BinomialDistribution;
import org.apache.commons.statistics.distribution.DiscreteDistribution;

import java.util.List;


/**
 * Binomial distribution: the number of successes in numTrials = n independent trials,
 * each with probability of success p. Its support is 0, 1, ..., n.
 * <p>
 * When numTrials is estimated, nothing keeps it at or above the current count: a proposal
 * that moves it below the count gets log density -Infinity and is rejected, not prevented.
 */
@Description("Binomial distribution, the number of successes in numTrials = n independent trials, " +
        "f(k; n, p) = C(n, k) p^k (1 - p)^(n - k) for k = 0, 1, ..., n. It has mean n p. " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Binomial extends ScalarDistribution<IntScalar<NonNegativeInt>, Integer> {

    final public Input<IntScalar<NonNegativeInt>> numTrialsInput = new Input<>("numTrials",
            "number of independent trials, at least 0", Input.Validate.REQUIRED);
    final public Input<RealScalar<UnitInterval>> pInput = new Input<>("p",
            "probability of success on each trial; defaults to 0.5");

    private BinomialDistribution dist = BinomialDistribution.of(1, 0.5);
    private DiscreteDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Binomial() {
    }

    public Binomial(IntScalar<NonNegativeInt> param, IntScalar<NonNegativeInt> numTrials, RealScalar<UnitInterval> p) {
        try {
            initByName("param", param, "numTrials", numTrials, "p", p);
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
        int n = numTrialsInput.get().get();
        double p = (pInput.get() != null) ? pInput.get().get() : 0.5;
        if (dist.getNumberOfTrials() != n || isNotEqual(dist.getProbabilityOfSuccess(), p)) {
            dist = BinomialDistribution.of(n, p);
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
        BinomialDistribution d = getApacheDistribution();
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
    protected BinomialDistribution getApacheDistribution() {
        refresh(); // parameters may have been changed by MCMC
        return dist;
    }

} // class Binomial
