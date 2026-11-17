package beast.base.spec.inference.distribution;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import org.apache.commons.statistics.distribution.PoissonDistribution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TruncatedInt}: both ends of [lower, upper] are inside the
 * truncation interval, so the normaliser is P(lower <= X <= upper).
 */
public class TruncatedIntTest {

    private static TruncatedInt<Int> truncatedPoisson(double lambda, Integer lower, Integer upper) {
        Poisson poisson = new Poisson(null, new RealScalarParam<>(lambda, NonNegativeReal.INSTANCE));
        TruncatedInt<Int> t = new TruncatedInt<>();
        if (lower == null) {
            t.initByName("distribution", poisson, "upper", new IntScalarParam<>(upper, Int.INSTANCE));
        } else if (upper == null) {
            t.initByName("distribution", poisson, "lower", new IntScalarParam<>(lower, Int.INSTANCE));
        } else {
            t.initByName("distribution", poisson,
                    "lower", new IntScalarParam<>(lower, Int.INSTANCE),
                    "upper", new IntScalarParam<>(upper, Int.INSTANCE));
        }
        return t;
    }

    /** P(lower <= X <= upper) for X ~ Poisson(lambda) */
    private static double mass(double lambda, int lower, int upper) {
        PoissonDistribution p = PoissonDistribution.of(lambda);
        return p.cumulativeProbability(upper) - (lower > 0 ? p.cumulativeProbability(lower - 1) : 0.0);
    }

    /**
     * dpois(3, 2) / ppois(5, 2) = (e^-2 * 8/6) / (e^-2 * (1 + 2 + 2 + 4/3 + 2/3 + 4/15)) = 0.1834862
     */
    @Test
    public void testDensityExactValue() {
        TruncatedInt<Int> t = truncatedPoisson(2.0, 0, 5);
        assertEquals(0.1834862, t.density(3), 1e-7);
    }

    @Test
    public void testPmfSumsToOne() {
        int[][] intervals = {{0, 5}, {2, 5}, {0, 1}, {1, 1}, {3, 10}};
        for (int[] iv : intervals) {
            TruncatedInt<Int> t = truncatedPoisson(2.0, iv[0], iv[1]);
            double sum = 0;
            for (int x = iv[0]; x <= iv[1]; x++) {
                sum += t.density(x);
            }
            assertEquals(1.0, sum, 1e-12, "pmf over [" + iv[0] + "," + iv[1] + "]");
            assertEquals(0.0, t.density(iv[0] - 1));
            assertEquals(0.0, t.density(iv[1] + 1));
        }
    }

    @Test
    public void testPmfSumsToOneOneSided() {
        // only a lower end: [2, infinity)
        TruncatedInt<Int> t = truncatedPoisson(2.0, 2, null);
        double sum = 0;
        for (int x = 2; x <= 100; x++) {
            sum += t.density(x);
        }
        assertEquals(1.0, sum, 1e-12);

        // only an upper end: no lower end must not wrap around at Integer.MIN_VALUE - 1
        t = truncatedPoisson(2.0, null, 4);
        sum = 0;
        for (int x = 0; x <= 4; x++) {
            sum += t.density(x);
        }
        assertEquals(1.0, sum, 1e-12);
    }

    @Test
    public void testCalculateLogP() {
        double lambda = 2.0;
        IntScalarParam<Int> param = new IntScalarParam<>(2, Int.INSTANCE);
        TruncatedInt<Int> t = truncatedPoisson(lambda, 2, 5);
        t.initByName("param", param);
        double expected = PoissonDistribution.of(lambda).logProbability(2) - Math.log(mass(lambda, 2, 5));
        assertEquals(expected, t.calculateLogP(), 1e-12);
    }

    @Test
    public void testCumulativeProbability() {
        double lambda = 2.0;
        TruncatedInt<Int> t = truncatedPoisson(lambda, 2, 5);
        assertEquals(0.0, t.cumulativeProbability(1));
        assertEquals(0.0, t.cumulativeProbability(1.9));
        // the lower end itself carries mass
        assertEquals(t.density(2), t.cumulativeProbability(2), 1e-12);
        assertEquals(t.density(2), t.cumulativeProbability(2.5), 1e-12);
        assertEquals(t.density(2) + t.density(3), t.cumulativeProbability(3), 1e-12);
        assertEquals(1.0, t.cumulativeProbability(5), 1e-12);
        assertEquals(1.0, t.cumulativeProbability(6));
    }

    @Test
    public void testInverseCumulativeProbability() {
        TruncatedInt<Int> t = truncatedPoisson(2.0, 2, 5);
        assertEquals(2, t.inverseCumulativeProbability(0.0));
        assertEquals(5, t.inverseCumulativeProbability(1.0));
        for (int k = 2; k <= 5; k++) {
            assertEquals(k, t.inverseCumulativeProbability(t.cumulativeProbability(k) - 1e-9));
        }
    }

    @Test
    public void testMean() {
        double lambda = 2.0;
        TruncatedInt<Int> t = truncatedPoisson(lambda, 2, 5);
        PoissonDistribution p = PoissonDistribution.of(lambda);
        double expected = 0;
        for (int x = 2; x <= 5; x++) {
            expected += x * p.probability(x);
        }
        expected /= mass(lambda, 2, 5);
        assertEquals(expected, t.getMean(), 1e-12);

        // one-sided: E[X | X >= 1] = lambda / (1 - e^-lambda)
        t = truncatedPoisson(lambda, 1, null);
        assertEquals(lambda / (1 - Math.exp(-lambda)), t.getMean(), 1e-9);
    }

    @Test
    public void testSample() {
        TruncatedInt<Int> t = truncatedPoisson(2.0, 2, 5);
        int n = 100_000;
        int[] counts = new int[6];
        for (int i = 0; i < n; i++) {
            List<Integer> s = t.sample();
            int x = s.get(0);
            assertTrue(x >= 2 && x <= 5, "sample " + x + " outside [2,5]");
            counts[x]++;
        }
        for (int x = 2; x <= 5; x++) {
            double expected = t.density(x);
            double se = Math.sqrt(expected * (1 - expected) / n);
            assertEquals(expected, (double) counts[x] / n, 5 * se, "frequency of " + x);
        }
    }

    //*** right tail: Walter's review of #284 ***//

    /** P(X >= lower) for X ~ Poisson(lambda), from the survival function */
    private static double upperTailMass(double lambda, int lower) {
        return PoissonDistribution.of(lambda).survivalProbability(lower - 1);
    }

    /**
     * Lower bound in the right tail: P(X <= lower - 1) is 1.0 or close to it in double
     * precision, so the normaliser has to come from the survival function. With
     * F(upper) - F(lower - 1) the density was -0.0989 at 20 (should be -0.091),
     * -Infinity at 25 and -19.13 at 30.
     */
    @Test
    public void testRightTailDensity() {
        double lambda = 2.0;
        PoissonDistribution p = PoissonDistribution.of(lambda);
        for (int lower : new int[]{10, 20, 25, 30}) {
            TruncatedInt<Int> t = truncatedPoisson(lambda, lower, null);
            double expected = p.logProbability(lower) - Math.log(upperTailMass(lambda, lower));
            assertEquals(expected, t.logDensity(lower), 1e-9, "logDensity at lower = " + lower);
            double sum = 0;
            for (int x = lower; x <= lower + 200; x++) {
                sum += t.density(x);
            }
            assertEquals(1.0, sum, 1e-9, "pmf from " + lower);
            assertEquals(t.density(lower), t.cumulativeProbability(lower), 1e-12);
            assertFalse(Double.isNaN(t.cumulativeProbability(lower + 3)));
        }
    }

    /** [25, 30]: both ends in the right tail */
    @Test
    public void testRightTailInterval() {
        TruncatedInt<Int> t = truncatedPoisson(2.0, 25, 30);
        double sum = 0;
        for (int x = 25; x <= 30; x++) {
            sum += t.density(x);
        }
        assertEquals(1.0, sum, 1e-9);
        assertEquals(1.0, t.cumulativeProbability(30), 1e-12);
        int[] counts = new int[31];
        for (int i = 0; i < 20_000; i++) {
            int x = t.sample().get(0);
            assertTrue(x >= 25 && x <= 30, "sample " + x + " outside [25,30]");
            counts[x]++;
        }
        // P(25 | [25, 30]) is about 0.9, not "always 30"
        assertEquals(t.density(25), counts[25] / 20_000.0, 0.01);
    }

    /** Sampling with the lower end in the right tail stays at lower and above */
    @Test
    public void testRightTailSample() {
        for (int lower : new int[]{10, 25, 30}) {
            TruncatedInt<Int> t = truncatedPoisson(2.0, lower, null);
            for (int i = 0; i < 1000; i++) {
                int x = t.sample().get(0);
                assertTrue(x >= lower && x < lower + 50, "sample " + x + " for lower = " + lower);
            }
        }
    }

    /**
     * Poisson(2) truncated from 10: the mean is 10.213 (by hand: P(10) = e^-2 2^10 / 10!, each
     * next term times 2 / (x + 1), mean 47.49 / 4.65 over the first five). It was 10.0, because the
     * upper quantile 1 - 1e-12 rounded to 1.0 and the sum stepped from 10 to 225.
     */
    @Test
    public void testRightTailMean() {
        double lambda = 2.0;
        PoissonDistribution p = PoissonDistribution.of(lambda);
        for (int lower : new int[]{10, 25}) {
            double expected = 0;
            for (int x = lower; x <= lower + 200; x++) {
                expected += x * p.probability(x);
            }
            expected /= upperTailMass(lambda, lower);
            assertEquals(expected, truncatedPoisson(lambda, lower, null).getMean(), 1e-9, "mean from " + lower);
        }
        assertEquals(10.213, truncatedPoisson(lambda, 10, null).getMean(), 0.001);
    }

    /** An interval without probability: logDensity is -Infinity, the rest says why it is undefined */
    @Test
    public void testEmptyInterval() {
        TruncatedInt<Int> t = truncatedPoisson(2.0, -5, -1);
        assertEquals(Double.NEGATIVE_INFINITY, t.logDensity(-3));
        assertThrows(IllegalStateException.class, () -> t.cumulativeProbability(-3));
        assertThrows(IllegalStateException.class, t::getMean);
        assertThrows(IllegalStateException.class, t::sample);
    }

    /** lower == upper puts all the mass on that value; the constructor accepts it */
    @Test
    public void testSingleValueConstructor() {
        Poisson poisson = new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE));
        // the constructor takes a distribution of any integer domain, here NonNegativeInt
        TruncatedInt<Int> t = new TruncatedInt<>(poisson, 3, 3);
        assertEquals(1.0, t.density(3), 1e-12);
        assertEquals(3, t.sample().get(0));
        assertEquals(3.0, t.getMean(), 1e-12);
    }

    /**
     * Bounded(Offset(...)), which BEAUti offers: the normaliser uses the Offset's CDF, which
     * ignored the offset, so the pmf of Bounded(Offset(Poisson(2), 5), [6, 8]) summed to 44.
     */
    @Test
    public void testBoundedOffset() {
        Poisson poisson = new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE));
        OffsetInt offset = new OffsetInt();
        offset.initByName("distribution", poisson, "offset", new IntScalarParam<>(5, Int.INSTANCE));
        TruncatedInt<Int> t = new TruncatedInt<>();
        t.initByName("distribution", offset,
                "lower", new IntScalarParam<>(6, Int.INSTANCE),
                "upper", new IntScalarParam<>(8, Int.INSTANCE));
        PoissonDistribution p = PoissonDistribution.of(2.0);
        double mass = p.probability(1) + p.probability(2) + p.probability(3);
        assertEquals(p.probability(1) / mass, t.density(6), 1e-12);
        double sum = 0;
        for (int x = 6; x <= 8; x++) {
            sum += t.density(x);
        }
        assertEquals(1.0, sum, 1e-12);
        assertEquals((1 * p.probability(1) + 2 * p.probability(2) + 3 * p.probability(3)) / mass + 5,
                t.getMean(), 1e-9);
    }
}
