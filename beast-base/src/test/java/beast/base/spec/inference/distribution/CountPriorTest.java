package beast.base.spec.inference.distribution;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.commons.statistics.distribution.PoissonDistribution;
import org.junit.jupiter.api.Test;

import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.inference.operator.BitFlipOperator;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;

/**
 * CountPrior (#231): the number of indicators on has the count distribution, and every
 * placement of that number is equally probable:
 * log p(I) = log p_count(s) - log C(K, s) - log P_count(S <= K).
 */
class CountPriorTest {

    private static final double LAMBDA = Math.log(2);

    private static Poisson poisson() {
        return new Poisson(null, new RealScalarParam<>(LAMBDA, NonNegativeReal.INSTANCE));
    }

    private static boolean[] bits(int pattern, int k) {
        boolean[] b = new boolean[k];
        for (int i = 0; i < k; i++) {
            b[i] = ((pattern >> i) & 1) == 1;
        }
        return b;
    }

    /** log p for the closed form, for s of k on */
    private static double expected(int s, int k) {
        double logC = org.apache.commons.numbers.combinatorics.LogBinomialCoefficient.value(k, s);
        return PoissonDistribution.of(LAMBDA).logProbability(s) - logC
                - Math.log(PoissonDistribution.of(LAMBDA).cumulativeProbability(k));
    }

    @Test
    void logPIsCountProbabilityOverNumberOfPlacements() {
        int k = 6;
        for (int pattern = 0; pattern < (1 << k); pattern++) {
            BoolVectorParam indicators = new BoolVectorParam(bits(pattern, k));
            CountPrior prior = new CountPrior(indicators, poisson());
            int s = Integer.bitCount(pattern);
            assertEquals(expected(s, k), prior.calculateLogP(), 1e-12, "pattern " + Integer.toBinaryString(pattern));
        }
    }

    /**
     * Summed over all 2^K indicator vectors, the probability of s on is the count
     * distribution's: the number of changes is Poisson, restricted to 0..K.
     */
    @Test
    void numberOnHasTheCountDistribution() {
        int k = 8;
        double[] mass = new double[k + 1];
        BoolVectorParam indicators = new BoolVectorParam(new boolean[k]);
        CountPrior prior = new CountPrior(indicators, poisson());
        for (int pattern = 0; pattern < (1 << k); pattern++) {
            boolean[] b = bits(pattern, k);
            for (int i = 0; i < k; i++) {
                indicators.set(i, b[i]);
            }
            mass[Integer.bitCount(pattern)] += Math.exp(prior.calculateLogP());
        }
        PoissonDistribution poisson = PoissonDistribution.of(LAMBDA);
        double total = 0;
        for (int s = 0; s <= k; s++) {
            assertEquals(poisson.probability(s) / poisson.cumulativeProbability(k), mass[s], 1e-12, "P(" + s + " on)");
            total += mass[s];
        }
        assertEquals(1.0, total, 1e-12, "not normalised over 0..K");
    }

    @Test
    void lambdaChangesAreSeen() {
        RealScalarParam<NonNegativeReal> lambda = new RealScalarParam<>(LAMBDA, NonNegativeReal.INSTANCE);
        BoolVectorParam indicators = new BoolVectorParam(new boolean[] {true, false, false, false});
        CountPrior prior = new CountPrior(indicators, new Poisson(null, lambda));
        double before = prior.calculateLogP();
        lambda.set(2.0);
        double after = prior.calculateLogP();
        assertEquals(PoissonDistribution.of(2.0).logProbability(1) - Math.log(4)
                - Math.log(PoissonDistribution.of(2.0).cumulativeProbability(4)), after, 1e-12);
        assertTrue(before != after);
    }

    @Test
    void sampleHasCountDistributedNumberOn() {
        int k = 22;
        BoolVectorParam indicators = new BoolVectorParam(new boolean[k]);
        CountPrior prior = new CountPrior(indicators, poisson());
        int n = 40000;
        double sum = 0;
        int zeros = 0;
        for (int i = 0; i < n; i++) {
            List<Boolean> sample = prior.sample();
            assertEquals(k, sample.size());
            int s = (int) sample.stream().filter(b -> b).count();
            sum += s;
            if (s == 0) zeros++;
        }
        // Poisson(ln 2): mean 0.693, P(0) = 0.5 (truncation at 22 is negligible)
        assertEquals(LAMBDA, sum / n, 0.02);
        assertEquals(0.5, (double) zeros / n, 0.015);
    }

    /**
     * With lambda estimated, the normaliser P(S <= K | lambda) depends on lambda: summed over
     * all indicator vectors the prior is 1 for every lambda, including large ones.
     */
    @Test
    void normalisedForEveryLambda() {
        int k = 5;
        RealScalarParam<NonNegativeReal> lambda = new RealScalarParam<>(LAMBDA, NonNegativeReal.INSTANCE);
        BoolVectorParam indicators = new BoolVectorParam(new boolean[k]);
        CountPrior prior = new CountPrior(indicators, new Poisson(null, lambda));
        // at 2000, P(S <= 5) underflows to 0 in double precision
        for (double l : new double[] {0.1, LAMBDA, 3.0, 20.0, 2000.0}) {
            lambda.set(l);
            double total = 0;
            for (int pattern = 0; pattern < (1 << k); pattern++) {
                boolean[] b = bits(pattern, k);
                for (int i = 0; i < k; i++) {
                    indicators.set(i, b[i]);
                }
                total += Math.exp(prior.calculateLogP());
            }
            assertEquals(1.0, total, 1e-10, "lambda " + l);
        }
    }

    /** a fixed count's normaliser is computed once, and again after refresh(), e.g. BEAUti edits */
    @Test
    void fixedCountNormaliserFollowsRefresh() {
        RealScalarParam<NonNegativeReal> lambda = new RealScalarParam<>(LAMBDA, NonNegativeReal.INSTANCE);
        lambda.isEstimatedInput.setValue(false, lambda);
        BoolVectorParam indicators = new BoolVectorParam(new boolean[] {true, false, false});
        CountPrior prior = new CountPrior(indicators, new Poisson(null, lambda));
        prior.calculateLogP();
        lambda.set(4.0);
        prior.refresh();
        assertEquals(PoissonDistribution.of(4.0).logProbability(1) - Math.log(3)
                - Math.log(PoissonDistribution.of(4.0).cumulativeProbability(3)), prior.calculateLogP(), 1e-12);
    }

    /** sampling works where the count is rarely <= K: K = 10, lambda = 50, P(S <= 10) ~ 1e-11 */
    @Test
    void sampleWhenCountIsRarelyAtMostK() {
        int k = 10;
        BoolVectorParam indicators = new BoolVectorParam(new boolean[k]);
        CountPrior prior = new CountPrior(indicators, new Poisson(null, new RealScalarParam<>(50.0, NonNegativeReal.INSTANCE)));
        int n = 2000;
        int all = 0;
        for (int i = 0; i < n; i++) {
            int s = (int) prior.sample().stream().filter(b -> b).count();
            if (s == k) all++;
        }
        // P(S = 10 | S <= 10) = 1 - 10/50 - ... ~ 0.8
        PoissonDistribution p = PoissonDistribution.of(50.0);
        double expected = p.probability(k) / p.cumulativeProbability(k);
        assertEquals(expected, (double) all / n, 0.03);
    }

    /** uniform="true" adds -log C(K, s) through the Hastings ratio: with CountPrior that is twice */
    @Test
    void bitFlipWithUniformIsRejectedWithCountPrior() {
        BoolVectorParam indicators = new BoolVectorParam(new boolean[5]);
        indicators.setID("indicators");
        CountPrior prior = new CountPrior(indicators, poisson());
        prior.setID("indicatorsPrior");

        BitFlipOperator uniform = new BitFlipOperator();
        uniform.setID("bitflip");
        // initByName wraps the IllegalArgumentException from initAndValidate()
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> uniform.initByName("parameter", indicators, "weight", 1.0, "uniform", true));
        StringBuilder messages = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            messages.append(t.getMessage()).append('\n');
        }
        assertTrue(messages.toString().contains("Set uniform=\"false\" on the operator"), messages.toString());

        BitFlipOperator symmetric = new BitFlipOperator();
        assertDoesNotThrow(() -> symmetric.initByName("parameter", indicators, "weight", 1.0, "uniform", false));
    }

    /** without uniform, the operator is symmetric for indicators with a CountPrior, and uniform otherwise */
    @Test
    void bitFlipDefaultFollowsThePrior() {
        BoolVectorParam indicators = new BoolVectorParam(new boolean[5]);
        new CountPrior(indicators, poisson());
        BitFlipOperator op = new BitFlipOperator();
        op.initByName("parameter", indicators, "weight", 1.0);
        // symmetric: the Hastings ratio of a flip is 0
        for (int i = 0; i < 20; i++) {
            assertEquals(0.0, op.proposal(), 0.0);
        }

        BoolVectorParam onSum = new BoolVectorParam(new boolean[5]);
        BitFlipOperator onSumOp = new BitFlipOperator();
        onSumOp.initByName("parameter", onSum, "weight", 1.0);
        // uniform: turning on one of five bits from none has Hastings ratio -log(5)
        assertEquals(-Math.log(5), onSumOp.proposal(), 1e-12);
    }

    /** an operator built in Java before the prior is checked again on its first proposal */
    @Test
    void bitFlipBuiltBeforeThePriorIsChecked() {
        BoolVectorParam indicators = new BoolVectorParam(new boolean[5]);
        BitFlipOperator uniform = new BitFlipOperator();
        uniform.initByName("parameter", indicators, "weight", 1.0, "uniform", true);
        BitFlipOperator byDefault = new BitFlipOperator();
        byDefault.initByName("parameter", indicators, "weight", 1.0);
        new CountPrior(indicators, poisson());
        assertThrows(IllegalArgumentException.class, uniform::proposal);
        assertEquals(0.0, byDefault.proposal(), 0.0);
    }
}
