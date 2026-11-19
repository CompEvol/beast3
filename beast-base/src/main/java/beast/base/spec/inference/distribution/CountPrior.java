package beast.base.spec.inference.distribution;

import java.util.List;

import org.apache.commons.numbers.combinatorics.LogBinomialCoefficient;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.StateNode;
import beast.base.spec.type.BoolVector;
import beast.base.spec.type.IntScalar;

/**
 * A prior on a vector of indicators (booleans) that puts a distribution on the number of
 * them that are on, with every placement of that number equally probable:
 * <pre>
 *   log p(I) = log p_count(s) - log C(K, s) - log P_count(S &le; K)
 * </pre>
 * where K is the number of indicators and s the number that are on. So the number of
 * indicators on has the count distribution restricted to 0..K, e.g. Poisson for the number
 * of rate changes of a random local clock, or of population size changes in the EBSP. The
 * normaliser P(S &le; K) matters when the count distribution's own parameters are estimated.
 * <p>
 * The whole prior is in the model, so any correct operator on the indicators samples it,
 * e.g. {@code BitFlipOperator}, which is symmetric ({@code uniform="false"}) by default for
 * indicators with this prior. Use it in place of a count distribution on
 * {@code IntSum(indicators)} combined with {@code BitFlipOperator}'s {@code uniform="true"},
 * whose Hastings ratio supplies the -log C(K, s) term instead (#231); with this prior,
 * {@code uniform="true"} would apply that term twice, and is an error.
 */
@Description("Prior on a vector of indicators: the number of indicators that are on has the count " +
        "distribution restricted to 0..K, and every placement of that number is equally probable, " +
        "so log p = log p_count(s) - log C(K, s) - log P_count(S <= K) for s of the K indicators on.")
public class CountPrior extends TensorDistribution<BoolVector, Boolean> implements IndicatorPrior {

    // any integer distribution on the count, e.g. Poisson, whatever the domain of its own parameter
    final public Input<ScalarDistribution<? extends IntScalar<?>, Integer>> countInput = new Input<>("count",
            "distribution of the number of indicators that are on, e.g. Poisson; its param is not used",
            Validate.REQUIRED);

    /** log C(K, s) for s = 0..K, for the current K */
    private double[] logBinomial = new double[0];

    /** whether the count distribution's parameters are constants, so the normaliser is too */
    private boolean countIsFixed;
    /** log P(S &le; K) for K = normaliserK, kept while the count is fixed */
    private double logNormaliser;
    private int normaliserK = -1;

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
        countIsFixed = hasConstantInputs(countInput.get());
        normaliserK = -1;
        super.initAndValidate();
    }

    /** whether every input of a distribution is a constant: a state node not estimated */
    private static boolean hasConstantInputs(BEASTInterface distribution) {
        for (BEASTInterface o : distribution.listActiveBEASTObjects()) {
            if (!(o instanceof StateNode node) || node.isEstimatedInput.get()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The count distribution refreshes itself when its probabilities are computed; this
     * forgets the normaliser, e.g. after a fixed parameter of the count is edited in BEAUti.
     */
    @Override
    public void refresh() {
        normaliserK = -1;
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

    /** log p_count(s) - log C(k, s) - log P(S &le; k) */
    private double logP(int s, int k) {
        double logNormaliser = logNormaliser(k);
        if (logNormaliser == Double.NEGATIVE_INFINITY) {
            // the count puts no probability on 0..k
            return Double.NEGATIVE_INFINITY;
        }
        return countInput.get().calcLogP(s) - logBinomial(k, s) - logNormaliser;
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

    /** log P(S &le; k) under the count distribution; computed once while the count is fixed */
    private double logNormaliser(int k) {
        if (countIsFixed && normaliserK == k) {
            return logNormaliser;
        }
        ScalarDistribution<? extends IntScalar<?>, Integer> count = countInput.get();
        double log;
        try {
            // P(S <= k) is often close to 1: use the survival function to keep its precision
            double survival = count.survivalProbability(k);
            if (survival < 0.5) {
                log = Math.log1p(-survival);
            } else {
                double cdf = count.cumulativeProbability(k);
                // far in the lower tail, e.g. a large Poisson mean, the CDF underflows
                log = cdf > 1e-100 ? Math.log(cdf) : logSumOfProbabilities(count, k);
            }
        } catch (RuntimeException e) {
            // a count distribution without a CDF
            log = logSumOfProbabilities(count, k);
        }
        logNormaliser = log;
        normaliserK = k;
        return log;
    }

    /** log of P(S = 0) + ... + P(S = k), summed on the log scale so it does not underflow */
    private static double logSumOfProbabilities(ScalarDistribution<? extends IntScalar<?>, Integer> count, int k) {
        double max = Double.NEGATIVE_INFINITY;
        double[] logPs = new double[k + 1];
        for (int i = 0; i <= k; i++) {
            logPs[i] = count.calcLogP(i);
            max = Math.max(max, logPs[i]);
        }
        if (max == Double.NEGATIVE_INFINITY) {
            return max;
        }
        double sum = 0;
        for (double l : logPs) {
            sum += Math.exp(l - max);
        }
        return max + Math.log(sum);
    }

    /**
     * A sample of the indicators: the number on from the count distribution restricted to
     * 0..K (by its inverse CDF on 0..K), placed uniformly at random.
     */
    @Override
    public List<Boolean> sample() {
        BoolVector indicators = paramInput.get();
        if (indicators == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " needs its param to know how many indicators to sample");
        }
        int k = indicators.size();
        ScalarDistribution<? extends IntScalar<?>, Integer> count = countInput.get();
        double[] logPs = new double[k + 1];
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= k; i++) {
            logPs[i] = count.calcLogP(i);
            max = Math.max(max, logPs[i]);
        }
        if (max == Double.NEGATIVE_INFINITY) {
            throw new IllegalStateException("The count distribution gives no number of indicators from 0 to " + k
                    + " a positive probability");
        }
        double[] cumulative = new double[k + 1];
        double total = 0;
        for (int i = 0; i <= k; i++) {
            total += Math.exp(logPs[i] - max);
            cumulative[i] = total;
        }
        double u = rng.nextDouble() * total;
        int s = 0;
        while (s < k && cumulative[s] <= u) {
            s++;
        }
        // s positions uniformly at random: the first s of a partial Fisher-Yates shuffle
        int[] positions = new int[k];
        for (int i = 0; i < k; i++) {
            positions[i] = i;
        }
        Boolean[] values = new Boolean[k];
        java.util.Arrays.fill(values, Boolean.FALSE);
        for (int i = 0; i < s; i++) {
            int j = i + rng.nextInt(k - i);
            int tmp = positions[i];
            positions[i] = positions[j];
            positions[j] = tmp;
            values[positions[i]] = Boolean.TRUE;
        }
        return java.util.Arrays.asList(values);
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
