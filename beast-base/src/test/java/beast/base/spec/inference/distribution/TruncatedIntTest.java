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

    private static TruncatedInt truncatedPoisson(double lambda, Integer lower, Integer upper) {
        Poisson poisson = new Poisson(null, new RealScalarParam<>(lambda, NonNegativeReal.INSTANCE));
        TruncatedInt t = new TruncatedInt();
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
        TruncatedInt t = truncatedPoisson(2.0, 0, 5);
        assertEquals(0.1834862, t.density(3), 1e-7);
    }

    @Test
    public void testPmfSumsToOne() {
        int[][] intervals = {{0, 5}, {2, 5}, {0, 1}, {1, 1}, {3, 10}};
        for (int[] iv : intervals) {
            TruncatedInt t = truncatedPoisson(2.0, iv[0], iv[1]);
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
        TruncatedInt t = truncatedPoisson(2.0, 2, null);
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
        TruncatedInt t = truncatedPoisson(lambda, 2, 5);
        t.initByName("param", param);
        double expected = PoissonDistribution.of(lambda).logProbability(2) - Math.log(mass(lambda, 2, 5));
        assertEquals(expected, t.calculateLogP(), 1e-12);
    }

    @Test
    public void testCumulativeProbability() {
        double lambda = 2.0;
        TruncatedInt t = truncatedPoisson(lambda, 2, 5);
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
        TruncatedInt t = truncatedPoisson(2.0, 2, 5);
        assertEquals(2, t.inverseCumulativeProbability(0.0));
        assertEquals(5, t.inverseCumulativeProbability(1.0));
        for (int k = 2; k <= 5; k++) {
            assertEquals(k, t.inverseCumulativeProbability(t.cumulativeProbability(k) - 1e-9));
        }
    }

    @Test
    public void testMean() {
        double lambda = 2.0;
        TruncatedInt t = truncatedPoisson(lambda, 2, 5);
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
        TruncatedInt t = truncatedPoisson(2.0, 2, 5);
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
}
