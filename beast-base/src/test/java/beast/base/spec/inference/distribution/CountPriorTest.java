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
 * placement of that number is equally probable: log p(I) = log p_count(s) - log C(K, s).
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
        return PoissonDistribution.of(LAMBDA).logProbability(s) - logC;
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
        for (int s = 0; s <= k; s++) {
            assertEquals(poisson.probability(s), mass[s], 1e-12, "P(" + s + " on)");
        }
    }

    @Test
    void lambdaChangesAreSeen() {
        RealScalarParam<NonNegativeReal> lambda = new RealScalarParam<>(LAMBDA, NonNegativeReal.INSTANCE);
        BoolVectorParam indicators = new BoolVectorParam(new boolean[] {true, false, false, false});
        CountPrior prior = new CountPrior(indicators, new Poisson(null, lambda));
        double before = prior.calculateLogP();
        lambda.set(2.0);
        double after = prior.calculateLogP();
        assertEquals(PoissonDistribution.of(2.0).logProbability(1) - Math.log(4), after, 1e-12);
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
                () -> uniform.initByName("parameter", indicators, "weight", 1.0));
        StringBuilder messages = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            messages.append(t.getMessage()).append('\n');
        }
        assertTrue(messages.toString().contains("Set uniform=\"false\" on the operator"), messages.toString());

        BitFlipOperator symmetric = new BitFlipOperator();
        assertDoesNotThrow(() -> symmetric.initByName("parameter", indicators, "weight", 1.0, "uniform", false));
    }
}
