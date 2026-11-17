package beast.base.spec.inference.distribution;

import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.Randomizer;
import org.apache.commons.statistics.distribution.PascalDistribution;
import org.apache.commons.statistics.distribution.PoissonDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Geometric, NegativeBinomial (mean and dispersion) and Binomial priors on counts.
 */
public class CountDistributionsTest {

    @BeforeEach
    public void setUp() {
        Randomizer.setSeedReproducible(42);
    }

    private static IntScalarParam<NonNegativeInt> k(int value) {
        return new IntScalarParam<>(value, NonNegativeInt.INSTANCE);
    }

    private static Geometric geometric(int x, double p) {
        return new Geometric(k(x), new RealScalarParam<>(p, UnitInterval.INSTANCE));
    }

    private static Binomial binomial(int x, int n, double p) {
        return new Binomial(k(x), k(n), new RealScalarParam<>(p, UnitInterval.INSTANCE));
    }

    private static NegativeBinomial negBinomial(int x, double mean, double r) {
        return new NegativeBinomial(k(x), new RealScalarParam<>(mean, PositiveReal.INSTANCE),
                new RealScalarParam<>(r, PositiveReal.INSTANCE));
    }

    // ---- Geometric ----

    @Test
    public void geometricAgainstClosedForm() {
        double p = 0.3;
        for (int x = 0; x < 20; x++) {
            double expected = x * Math.log(1 - p) + Math.log(p);
            assertEquals(expected, geometric(x, p).calculateLogP(), 1e-12, "k=" + x);
        }
        Geometric g = geometric(0, p);
        assertEquals((1 - p) / p, g.getMean(), 1e-12);
        // P(X <= k) = 1 - (1-p)^(k+1)
        assertEquals(1 - Math.pow(1 - p, 5), g.cumulativeProbability(4), 1e-12);
        assertEquals(0, g.getLowerBoundOfParameter());
        assertEquals(Integer.MAX_VALUE, g.getUpperBoundOfParameter());
    }

    @Test
    public void geometricFollowsChangedP() {
        RealScalarParam<UnitInterval> p = new RealScalarParam<>(0.5, UnitInterval.INSTANCE);
        Geometric g = new Geometric(k(2), p);
        assertEquals(3 * Math.log(0.5), g.calculateLogP(), 1e-12);
        p.set(0.2);
        assertEquals(2 * Math.log(0.8) + Math.log(0.2), g.calculateLogP(), 1e-12);
    }

    @Test
    public void geometricWithPZeroGivesZeroProbability() {
        // p = 0 is in UnitInterval but is not a distribution: every count gets probability 0,
        // so MCMC rejects the state instead of stopping with an exception
        RealScalarParam<UnitInterval> p = new RealScalarParam<>(0.3, UnitInterval.INSTANCE);
        Geometric g = new Geometric(k(2), p);
        p.set(0.0);
        assertEquals(Double.NEGATIVE_INFINITY, g.calculateLogP());
        IID iid = new IID(new IntVectorParam<>(new int[]{0, 3}, NonNegativeInt.INSTANCE), g);
        assertEquals(Double.NEGATIVE_INFINITY, iid.calculateLogP());
        // the support does not depend on p
        assertEquals(0, g.getLowerBoundOfParameter());
        assertEquals(Integer.MAX_VALUE, g.getUpperBoundOfParameter());
        // what needs an actual distribution says why it cannot
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> g.cumulativeProbability(1));
        assertTrue(e.getMessage().contains("p is 0"), e.getMessage());
        // and it recovers when p moves away from 0, also to a tiny p
        p.set(1e-300);
        assertTrue(Double.isFinite(g.calculateLogP()));
        p.set(0.3);
        assertEquals(2 * Math.log(0.7) + Math.log(0.3), g.calculateLogP(), 1e-12);
    }

    // ---- Binomial ----

    @Test
    public void binomialAgainstClosedForm() {
        int n = 7;
        double p = 0.35;
        double total = 0;
        for (int x = 0; x <= n; x++) {
            double expected = Math.log(choose(n, x)) + x * Math.log(p) + (n - x) * Math.log(1 - p);
            double logP = binomial(x, n, p).calculateLogP();
            assertEquals(expected, logP, 1e-12, "k=" + x);
            total += Math.exp(logP);
        }
        assertEquals(1.0, total, 1e-12);
        assertEquals(Double.NEGATIVE_INFINITY, binomial(n + 1, n, p).calculateLogP());
        Binomial b = binomial(0, n, p);
        assertEquals(n * p, b.getMean(), 1e-12);
        assertEquals(0, b.getLowerBoundOfParameter());
        assertEquals(n, b.getUpperBoundOfParameter());
    }

    @Test
    public void binomialRequiresN() {
        Binomial b = new Binomial();
        assertThrows(RuntimeException.class, () -> b.initByName("param", k(1)));
    }

    // ---- NegativeBinomial ----

    @Test
    public void negBinomialAgainstHandComputedValue() {
        // R: dnbinom(3, size = 2.5, mu = 4)
        // = Gamma(5.5) / (Gamma(2.5) 3!) (2.5/6.5)^2.5 (4/6.5)^3, Gamma(5.5)/Gamma(2.5) = 4.5 * 3.5 * 2.5
        double expected = 4.5 * 3.5 * 2.5 / 6.0 * Math.pow(2.5 / 6.5, 2.5) * Math.pow(4 / 6.5, 3);
        assertEquals(Math.log(expected), negBinomial(3, 4, 2.5).calculateLogP(), 1e-12);
        // P(X = 0) = (r / (r + mean))^r
        assertEquals(2.5 * Math.log(2.5 / 6.5), negBinomial(0, 4, 2.5).calculateLogP(), 1e-12);
    }

    @Test
    public void negBinomialMatchesPascalForIntegerR() {
        // Pascal(r, p) counts failures before the r-th success: mean r (1 - p) / p
        int r = 3;
        double mean = 5.0;
        double p = r / (r + mean);
        PascalDistribution pascal = PascalDistribution.of(r, p);
        NegativeBinomial nb = negBinomial(0, mean, r);
        for (int x = 0; x < 60; x++) {
            assertEquals(pascal.logProbability(x), nb.getApacheDistribution().logProbability(x), 1e-10, "k=" + x);
            assertEquals(pascal.cumulativeProbability(x), nb.cumulativeProbability(x), 1e-12, "k=" + x);
            assertEquals(pascal.survivalProbability(x), nb.survivalProbability(x), 1e-12, "k=" + x);
        }
    }

    @Test
    public void negBinomialTendsToPoissonForLargeR() {
        double mean = 3.0;
        PoissonDistribution poisson = PoissonDistribution.of(mean);
        NegativeBinomial nb = negBinomial(0, mean, 1e8);
        for (int x = 0; x < 20; x++) {
            // log NB - log Poisson = (k(k-1) - 2k mean + mean^2) / (2r) + O(1/r^2)
            double correction = (x * (x - 1) - 2 * x * mean + mean * mean) / (2 * 1e8);
            assertEquals(poisson.logProbability(x) + correction, nb.getApacheDistribution().logProbability(x), 1e-11, "k=" + x);
        }
    }

    @Test
    public void negBinomialCdfIsSumOfPmfAndQuantilesRoundTrip() {
        for (double[] mr : new double[][]{{0.5, 0.3}, {4, 2.5}, {100, 0.8}, {2, 50}}) {
            NegativeBinomial nb = negBinomial(0, mr[0], mr[1]);
            var d = nb.getApacheDistribution();
            double cdf = 0;
            for (int x = 0; x < 200; x++) {
                cdf += d.probability(x);
                assertEquals(cdf, d.cumulativeProbability(x), 1e-10, "mean,r=" + mr[0] + "," + mr[1] + " k=" + x);
            }
            for (double q : new double[]{1e-9, 0.01, 0.25, 0.5, 0.75, 0.99, 1 - 1e-9}) {
                int x = d.inverseCumulativeProbability(q);
                assertTrue(d.cumulativeProbability(x) >= q);
                assertTrue(x == 0 || d.cumulativeProbability(x - 1) < q, "smallest k for q=" + q);
                int y = d.inverseSurvivalProbability(1 - q);
                assertTrue(Math.abs(x - y) <= 1, "q=" + q + ": " + x + " vs " + y);
            }
        }
    }

    @Test
    public void negBinomialFarRightTailIsAccurate() {
        // a tiny r gives a very long tail: survival keeps its precision where 1 - CDF would not
        var d = negBinomial(0, 10, 0.1).getApacheDistribution();
        double s = d.survivalProbability(5_000);
        assertTrue(s > 0 && s < 1e-15, "survival " + s);
        int x = d.inverseSurvivalProbability(s);
        assertTrue(Math.abs(x - 5_000) <= 1, "inverse survival " + x);
    }

    @Test
    public void negBinomialRejectsInvalidParameters() {
        assertThrows(RuntimeException.class, () -> negBinomial(0, 0, 1));
        assertThrows(RuntimeException.class, () -> negBinomial(0, 1, 0));
    }

    @Test
    public void negBinomialIID() {
        NegativeBinomial base = negBinomial(0, 4, 2.5);
        IntVectorParam<NonNegativeInt> xs = new IntVectorParam<>(new int[]{0, 3, 7}, NonNegativeInt.INSTANCE);
        IID iid = new IID(xs, base);
        double expected = 0;
        for (int x : new int[]{0, 3, 7}) {
            expected += negBinomial(x, 4, 2.5).calculateLogP();
        }
        assertEquals(expected, iid.calculateLogP(), 1e-12);
    }

    // ---- sampling ----

    @Test
    public void samplesHaveTheRightMeanAndVariance() {
        checkMoments("Geometric", geometric(0, 0.3), 0.7 / 0.3, 0.7 / 0.09);
        checkMoments("Binomial", binomial(0, 20, 0.35), 7, 20 * 0.35 * 0.65);
        checkMoments("NegativeBinomial", negBinomial(0, 4, 2.5), 4, 4 + 16 / 2.5);
        checkMoments("NegativeBinomial small r", negBinomial(0, 2, 0.5), 2, 2 + 4 / 0.5);
    }

    private static void checkMoments(String name, ScalarDistribution<?, Integer> d, double mean, double var) {
        int n = 200_000;
        double sum = 0, sum2 = 0;
        for (int i = 0; i < n; i++) {
            int x = d.sample().get(0);
            assertTrue(x >= 0, name + " drew " + x);
            sum += x;
            sum2 += (double) x * x;
        }
        double m = sum / n;
        double v = sum2 / n - m * m;
        // 5 standard errors of the mean; variance within 5%
        assertEquals(mean, m, 5 * Math.sqrt(var / n), name + " mean");
        assertEquals(var, v, 0.05 * var, name + " variance");
    }

    private static double choose(int n, int k) {
        double c = 1;
        for (int i = 1; i <= k; i++) {
            c = c * (n - k + i) / i;
        }
        return c;
    }
}
