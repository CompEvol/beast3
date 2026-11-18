package beast.base.spec.inference.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.numbers.combinatorics.LogBinomialCoefficient;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.type.BoolVector;
import beast.base.spec.type.IntScalar;

/**
 * A prior on a vector of indicators (booleans) that puts a distribution on the number of
 * them that are on, with every placement of that number equally probable:
 * <pre>
 *   log p(I) = log p_count(s) - log C(K, s)
 * </pre>
 * where K is the number of indicators and s the number that are on. So the number of
 * indicators on has the count distribution (restricted to 0..K), e.g. Poisson for the number
 * of rate changes of a random local clock, or of population size changes in the EBSP.
 * <p>
 * The whole prior is in the model, so any correct operator on the indicators samples it,
 * e.g. {@code BitFlipOperator} with {@code uniform="false"}. Use it in place of a count
 * distribution on {@code IntSum(indicators)} combined with {@code BitFlipOperator}'s default
 * {@code uniform="true"}, whose Hastings ratio supplies the -log C(K, s) term instead (#231);
 * with this prior, {@code uniform="true"} would apply that term twice.
 */
@Description("Prior on a vector of indicators: the number of indicators that are on has the count " +
        "distribution, and every placement of that number is equally probable, " +
        "so log p = log p_count(s) - log C(K, s) for s of the K indicators on.")
public class CountPrior extends TensorDistribution<BoolVector, Boolean> {

    // any integer distribution on the count, e.g. Poisson, whatever the domain of its own parameter
    final public Input<ScalarDistribution<? extends IntScalar<?>, Integer>> countInput = new Input<>("count",
            "distribution of the number of indicators that are on, e.g. Poisson; its param is not used",
            Validate.REQUIRED);

    private ScalarDistribution<? extends IntScalar<?>, Integer> count;

    /** log C(K, s) for s = 0..K, for the current K */
    private double[] logBinomial = new double[0];

    public CountPrior() {
    }

    public CountPrior(BoolVector param, ScalarDistribution<? extends IntScalar<?>, Integer> count) {
        try {
            initByName("param", param, "count", count);
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
        count = countInput.get();
        count.refresh();
    }

    @Override
    public double calculateLogP() {
        BoolVector indicators = paramInput.get();
        int k = indicators.size();
        int s = 0;
        for (int i = 0; i < k; i++) {
            if (indicators.get(i)) {
                s++;
            }
        }
        logP = logP(s, k);
        return logP;
    }

    @Override
    protected double calcLogP(Boolean... value) {
        int s = 0;
        for (Boolean b : value) {
            if (b) {
                s++;
            }
        }
        return logP(s, value.length);
    }

    /** log p_count(s) - log C(k, s) */
    private double logP(int s, int k) {
        refresh(); // the count distribution's own parameters may be sampled
        return count.calcLogP(s) - logBinomial(k, s);
    }

    private double logBinomial(int k, int s) {
        if (logBinomial.length != k + 1) {
            logBinomial = new double[k + 1];
            for (int i = 0; i <= k; i++) {
                logBinomial[i] = LogBinomialCoefficient.value(k, i);
            }
        }
        return logBinomial[s];
    }

    /**
     * A sample of the indicators: the number on from the count distribution (restricted to
     * 0..K, by drawing again), placed uniformly at random.
     */
    @Override
    public List<Boolean> sample() {
        BoolVector indicators = paramInput.get();
        if (indicators == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " needs its param to know how many indicators to sample");
        }
        int k = indicators.size();
        refresh();
        int s;
        int tries = 0;
        do {
            s = count.sample().get(0);
            if (++tries > 100000) {
                throw new IllegalStateException("The count distribution rarely gives a number of indicators from 0 to " + k);
            }
        } while (s < 0 || s > k);
        List<Boolean> values = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            values.add(i < s);
        }
        Collections.shuffle(values, new java.util.Random(rng.nextLong()));
        return values;
    }

    @Override
    public Boolean getLowerBoundOfParameter() {
        return false;
    }

    @Override
    public Boolean getUpperBoundOfParameter() {
        return true;
    }

    /** @return the distribution of the number of indicators that are on */
    public ScalarDistribution<? extends IntScalar<?>, Integer> getCountDistribution() {
        return countInput.get();
    }
}
