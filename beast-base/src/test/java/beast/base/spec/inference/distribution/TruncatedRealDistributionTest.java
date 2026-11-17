package beast.base.spec.inference.distribution;

import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.Randomizer;
import org.apache.commons.statistics.distribution.TruncatedNormalDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TruncatedRealDistribution
 */
public class TruncatedRealDistributionTest {

    @BeforeEach
    public void setUp() {
        Randomizer.setSeedReproducible(42);
    }

    @Test
    public void testTruncatedBeta() {
        System.out.println("Testing truncated Beta density");

        // Beta extends ScalarDistribution<RealScalar<UnitInterval>, Double>
        Beta beta = new Beta();
        beta.initByName(
                "alpha", new RealScalarParam<>(1.0, PositiveReal.INSTANCE),
                "beta", new RealScalarParam<>(1.0, PositiveReal.INSTANCE)
        );

        assertEquals(1.0, beta.density(0.6), 1e-10);

        // Truncate to [0, 2]
        TruncatedReal<Real> truncNorm = new TruncatedReal<>();
        truncNorm.initByName(
                "distribution", beta,
                "lower", new RealScalarParam<>(0.5, Real.INSTANCE),
                "upper", new RealScalarParam<>(1.0, Real.INSTANCE)
        );

        // For Beta(1,1) (i.e. Uniform(0,1)) truncated to [0.5,1.0],
        // the density is renormalized on that interval.
        assertEquals(2.0, truncNorm.density(0.6), 1e-10);
        assertEquals(2.0, truncNorm.density(0.8), 1e-10);

        // Test that density is outside bounds
        assertEquals(0, truncNorm.density(0.1), 1e-10);
        assertEquals(0, truncNorm.density(-1.0), 1e-10);
        assertEquals(0, truncNorm.density(2.6), 1e-10);

        // Test that density integrates approximately to 1 (numerical check)
        double sum = 0.0;
        int n = 1_000;
        double step = 2.0 / n;
        for (int i = 0; i < n; i++) {
            double x = i * step + step / 2;
            sum += truncNorm.density(x) * step;
        }
        assertEquals(1.0, sum, 0.01, "Density should integrate to approximately 1");
    }


    @Test
    public void testTruncatedNormalDensity() {
        System.out.println("Testing truncated normal density");

        // Create a standard normal distribution
        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(0.0, Real.INSTANCE),
            "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE)
        );

        // Truncate to [0, 2]
        TruncatedReal<Real> truncNorm = new TruncatedReal<>();
        truncNorm.initByName(
            "distribution", baseNorm,
            "lower", new RealScalarParam<>(0.0, Real.INSTANCE),
            "upper", new RealScalarParam<>(2.0, Real.INSTANCE)
        );

        // Test that density is 0 outside bounds
        assertEquals(0.0, truncNorm.density(-0.5), 1e-10);
        assertEquals(0.0, truncNorm.density(2.5), 1e-10);

        // Test that density is positive inside bounds
        assertTrue(truncNorm.density(0.5) > 0);
        assertTrue(truncNorm.density(1.0) > 0);
        assertTrue(truncNorm.density(1.5) > 0);

        // Test that density integrates approximately to 1 (numerical check)
        double sum = 0.0;
        int n = 1_000;
        double step = 2.0 / n;
        for (int i = 0; i < n; i++) {
            double x = i * step + step / 2;
            sum += truncNorm.density(x) * step;
        }
        assertEquals(1.0, sum, 0.01, "Density should integrate to approximately 1");
    }

    @Test
    public void testTruncatedNormalLogDensity() {
        System.out.println("Testing truncated normal log density");

        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(5.0, Real.INSTANCE),
            "sigma", new RealScalarParam<>(2.0, PositiveReal.INSTANCE)
        );

        TruncatedReal<Real> truncNorm = new TruncatedReal<>();
        truncNorm.initByName(
            "distribution", baseNorm,
            "lower", new RealScalarParam<>(3.0, Real.INSTANCE),
            "upper", new RealScalarParam<>(7.0, Real.INSTANCE)
        );

        // Test log density outside bounds
        assertEquals(Double.NEGATIVE_INFINITY, truncNorm.logDensity(2.0), 1e-10);
        assertEquals(Double.NEGATIVE_INFINITY, truncNorm.logDensity(8.0), 1e-10);

        // Test log density inside bounds
        assertTrue(truncNorm.logDensity(4.0) > Double.NEGATIVE_INFINITY);
        assertTrue(truncNorm.logDensity(5.0) > Double.NEGATIVE_INFINITY);
        assertTrue(truncNorm.logDensity(6.0) > Double.NEGATIVE_INFINITY);

        // Check consistency: log(density(x)) == logDensity(x)
        double x = 5.5;
        assertEquals(Math.log(truncNorm.density(x)), truncNorm.logDensity(x), 1e-10);
    }

    @Test
    public void testSampling() {
        System.out.println("Testing sampling from truncated distribution");

        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(0.0, Real.INSTANCE),
            "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE)
        );

        double lower = -1.0;
        double upper = 1.5;

        TruncatedReal<Real> truncNorm = new TruncatedReal<>();
        truncNorm.initByName(
            "distribution", baseNorm,
            "lower", new RealScalarParam<>(lower, Real.INSTANCE),
            "upper", new RealScalarParam<>(upper, Real.INSTANCE)
        );

        // Sample multiple times and check all samples are within bounds
        for (int i = 0; i < 1000; i++) {
            double sample = truncNorm.sample().get(0);
            assertTrue(sample >= lower, "Sample should be >= lower bound");
            assertTrue(sample <= upper, "Sample should be <= upper bound");
        }
    }

    @Test
    public void testSamplingMean() {
        System.out.println("Testing sample mean approximates computed mean");

        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(0.0, Real.INSTANCE),
            "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE)
        );

        TruncatedReal<Real> truncNorm = new TruncatedReal<>();
        truncNorm.initByName(
            "distribution", baseNorm,
            "lower", new RealScalarParam<>(0.0, Real.INSTANCE),
            "upper", new RealScalarParam<>(2.0, Real.INSTANCE)
        );

        // Compute sample mean
        double sampleMean = 0.0;
        int nSamples = 10000;
        for (int i = 0; i < nSamples; i++) {
            sampleMean += truncNorm.sample().get(0);
        }
        sampleMean /= nSamples;

        // Compare with computed mean
        double computedMean = truncNorm.getMean();
        
        System.out.println("Sample mean: " + sampleMean);
        System.out.println("Computed mean: " + computedMean);
        
        // They should be close (within Monte Carlo error)
        assertEquals(computedMean, sampleMean, 0.02);
    }

    @Test
    public void testMeanComputation() {
        testMeanComputation(0, 1, 0, 10);
        testMeanComputation(5, 1, 3, 7);
        testMeanComputation(1, 5, -3, -2);
    }

    public void testMeanComputation(double mu, double sigma, double lower, double upper) {
        System.out.println("Testing mean computation for TruncatedNormal("+ mu + "," + sigma + "," + lower + "," + upper + ")");

        // Test 1: Truncated standard normal [0, Inf) should have mean > 0
        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(mu, Real.INSTANCE),
            "sigma", new RealScalarParam<>(sigma, PositiveReal.INSTANCE)
        );

        TruncatedReal<Real> truncNorm1 = new TruncatedReal<>();
        truncNorm1.initByName(
            "distribution", baseNorm,
            "lower", new RealScalarParam<>(lower, Real.INSTANCE),
            "upper", new RealScalarParam<>(upper, Real.INSTANCE)
        );

        TruncatedNormalDistribution truncNormApache = TruncatedNormalDistribution.of(mu, sigma, lower, upper);
        System.out.println(truncNorm1.getMean());
        assertEquals(truncNorm1.getMean(), truncNormApache.getMean(), 2e-4,
            "Computed mean should match Apache Commons Statistics for TruncatedNormal(" + mu + "," + sigma + "," + lower + "," + upper + ")"
        );
    }

    @Test
    public void testIsValid() {
        System.out.println("Testing isValid method");

        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(0.0, Real.INSTANCE),
            "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE)
        );

        TruncatedReal<Real> truncNorm = new TruncatedReal<>();
        truncNorm.initByName(
            "distribution", baseNorm,
            "lower", new RealScalarParam<>(-1.0, Real.INSTANCE),
            "upper", new RealScalarParam<>(1.0, Real.INSTANCE)
        );

        // Test boundary conditions
        assertTrue(truncNorm.isValid(-1.0), "Lower bound should be valid");
        assertTrue(truncNorm.isValid(1.0), "Upper bound should be valid");
        assertTrue(truncNorm.isValid(0.0), "Midpoint should be valid");

        // Test outside bounds
        assertFalse(truncNorm.isValid(-1.1), "Below lower bound should be invalid");
        assertFalse(truncNorm.isValid(1.1), "Above upper bound should be invalid");
    }

    @Test
    public void testConstructorWithPrimitives() {
        System.out.println("Testing constructor with primitive values");

        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(0.0, Real.INSTANCE),
            "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE)
        );

        TruncatedReal<Real> truncNorm =
            new TruncatedReal<>(baseNorm, -2.0, 2.0);

        // Check that it was initialized correctly
        assertTrue(truncNorm.isValid(0.0));
        assertFalse(truncNorm.isValid(-3.0));
        assertFalse(truncNorm.isValid(3.0));
    }

    @Test
    public void testDefaultBounds() {
        System.out.println("Testing default bounds (no explicit bounds provided)");

        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(0.0, Real.INSTANCE),
            "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE)
        );

        // Create truncated distribution without explicit bounds
        TruncatedReal<Real> truncNorm = new TruncatedReal<>();
        truncNorm.initByName("distribution", baseNorm);

        // Should use domain bounds (Real = (-Inf, +Inf))
        assertTrue(truncNorm.isValid(-1000.0));
        assertTrue(truncNorm.isValid(1000.0));

    }

    @Test
    public void testWithPositiveRealDomain() {
        System.out.println("Testing with PositiveReal domain");

        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(2.0, Real.INSTANCE),
            "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE)
        );

        TruncatedReal<Real> truncNorm = new TruncatedReal<>();
        truncNorm.initByName(
            "distribution", baseNorm,
            "lower", new RealScalarParam<>(1.0, Real.INSTANCE),
            "upper", new RealScalarParam<>(3.0, Real.INSTANCE)
        );

        // Samples should be in [1, 3]
        for (int i = 0; i < 100; i++) {
            double sample = truncNorm.sample().get(0);
            assertTrue(sample >= 1.0 && sample <= 3.0);
        }
    }

    @Test
    public void testProbOutOfBounds() {
        System.out.println("Testing probability out of bounds calculation");

        Normal baseNorm = new Normal();
        baseNorm.initByName(
            "mean", new RealScalarParam<>(0.0, Real.INSTANCE),
            "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE)
        );

        TruncatedReal<Real> truncNorm = new TruncatedReal<>();
        truncNorm.initByName(
            "distribution", baseNorm,
            "lower", new RealScalarParam<>(-1.0, Real.INSTANCE),
            "upper", new RealScalarParam<>(1.0, Real.INSTANCE)
        );

        double probOOB = truncNorm.probOutOfBounds();
        
        // For standard normal truncated to [-1, 1], about 68% is inside
        // So about 32% is outside
        assertTrue(probOOB > 0.0 && probOOB < 1.0);
        assertEquals(0.317, probOOB, 0.01, "Probability out of bounds should be ~0.32 for N(0,1) truncated to [-1,1]");
    }


    //*** right tail ***//

    private static TruncatedReal<Real> truncatedExp(double lower, Double upper) {
        Exponential exp = new Exponential(null, new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        TruncatedReal<Real> t = new TruncatedReal<>();
        if (upper == null) {
            t.initByName("distribution", exp, "lower", new RealScalarParam<>(lower, Real.INSTANCE));
        } else {
            t.initByName("distribution", exp, "lower", new RealScalarParam<>(lower, Real.INSTANCE),
                    "upper", new RealScalarParam<>(upper, Real.INSTANCE));
        }
        return t;
    }

    /**
     * Exp(1) truncated from L is L + Exp(1): density e^-(x - L), CDF 1 - e^-(x - L),
     * mean L + 1. With the normaliser 1 - P(X <= L) - (1 - P(X <= upper)) the density was
     * off by 1.7e-4 in log at L = 30, and +Infinity at L = 40, where P(X <= 40) is 1.0
     * in double precision; samples were NaN there.
     */
    @Test
    public void testRightTailExponential() {
        for (double lower : new double[]{10, 30, 40, 100}) {
            TruncatedReal<Real> t = truncatedExp(lower, null);
            assertEquals(-0.5, t.logDensity(lower + 0.5), 1e-9, "logDensity from " + lower);
            assertEquals(1 - Math.exp(-1), t.cumulativeProbability(lower + 1), 1e-9, "CDF from " + lower);
            assertEquals(Math.exp(-1), t.survivalProbability(lower + 1), 1e-9);
            assertEquals(lower + Math.log(2), t.inverseCumulativeProbability(0.5), 1e-9, "median from " + lower);
            assertEquals(lower + 1, t.getMean(), 1e-3, "mean from " + lower);
            double sum = 0;
            int n = 20_000;
            for (int i = 0; i < n; i++) {
                double x = t.sample().get(0);
                assertTrue(x >= lower && Double.isFinite(x), "sample " + x + " from " + lower);
                sum += x;
            }
            // the sample mean of Exp(1) has standard error 1/sqrt(n) = 0.007
            assertEquals(lower + 1, sum / n, 0.05, "sample mean from " + lower);
        }
    }

    /** [40, 41]: both ends in the right tail */
    @Test
    public void testRightTailInterval() {
        TruncatedReal<Real> t = truncatedExp(40, 41.0);
        assertEquals(-0.5 - Math.log(1 - Math.exp(-1)), t.logDensity(40.5), 1e-9);
        assertEquals(1.0, t.cumulativeProbability(41), 1e-12);
        assertEquals((1 - Math.exp(-0.5)) / (1 - Math.exp(-1)), t.cumulativeProbability(40.5), 1e-9);
    }

    /** Normal(0, 1) from 10: the mean is the inverse Mills ratio phi(10) / (1 - Phi(10)) */
    @Test
    public void testRightTailNormalMean() {
        Normal normal = new Normal();
        normal.initByName("mean", new RealScalarParam<>(0.0, Real.INSTANCE),
                "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        TruncatedReal<Real> t = new TruncatedReal<>();
        t.initByName("distribution", normal, "lower", new RealScalarParam<>(10.0, Real.INSTANCE));
        org.apache.commons.statistics.distribution.NormalDistribution n =
                org.apache.commons.statistics.distribution.NormalDistribution.of(0, 1);
        double expected = n.density(10) / n.survivalProbability(10);
        assertEquals(10.098, expected, 1e-3);
        assertEquals(expected, t.getMean(), 1e-3);
        assertEquals(n.logDensity(10.5) - Math.log(n.survivalProbability(10)), t.logDensity(10.5), 1e-9);
    }

    /** An interval without probability: logDensity is -Infinity, the rest says why it is undefined */
    @Test
    public void testEmptyInterval() {
        TruncatedReal<Real> t = truncatedExp(-5, -1.0);
        assertEquals(Double.NEGATIVE_INFINITY, t.logDensity(-3));
        assertThrows(IllegalStateException.class, () -> t.cumulativeProbability(-3));
        assertThrows(IllegalStateException.class, t::getMean);
        assertThrows(IllegalStateException.class, t::sample);
    }
}
