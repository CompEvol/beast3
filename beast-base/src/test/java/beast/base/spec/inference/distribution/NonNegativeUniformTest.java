package beast.base.spec.inference.distribution;

import beast.base.parser.XMLParser;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.Randomizer;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.UniformContinuousDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link NonNegativeUniform}: [lower, upper] with lower >= 0.
 */
public class NonNegativeUniformTest {

    static RealScalarParam<NonNegativeReal> nonNeg(double v) {
        return new RealScalarParam<>(v, NonNegativeReal.INSTANCE);
    }

    static RealScalarParam<PositiveReal> pos(double v) {
        return new RealScalarParam<>(v, PositiveReal.INSTANCE);
    }

    private static NonNegativeUniform uniform(double lower, double upper) {
        NonNegativeUniform d = new NonNegativeUniform();
        d.initByName("lower", nonNeg(lower), "upper", pos(upper));
        return d;
    }

    static void assertBoundsRejected(Runnable r, String condition) {
        RuntimeException e = assertThrows(RuntimeException.class, r::run);
        assertTrue(e.getMessage().contains("requires " + condition), "unexpected: " + e.getMessage());
    }

    /** a generator whose nextDouble() returns the given values in turn, then 0.5 */
    static UniformRandomProvider fixedDoubles(double... values) {
        return new UniformRandomProvider() {
            int i = 0;
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException("only nextDouble() is used");
            }
            @Override
            public double nextDouble() {
                return i < values.length ? values[i++] : 0.5;
            }
        };
    }

    @BeforeEach
    public void setUp() {
        Randomizer.setSeedReproducible(42);
    }

    @Test
    public void testDensityInsideSupport() {
        NonNegativeUniform d = uniform(0.0, 1000.0);
        for (double x : new double[]{0.0, 1.0e-9, 0.5, 10.0, 1000.0}) {
            assertEquals(-Math.log(1000.0), d.logDensity(x), 1e-12, "x=" + x);
            assertEquals(1.0 / 1000.0, d.density(x), 1e-15, "x=" + x);
        }
    }

    @Test
    public void testRejectsNegativeLower() {
        // The lower input is typed RealScalar<NonNegativeReal>, but XML can still pass a
        // parameter with domain Real, so the distribution must check the value itself.
        assertBoundsRejected(() -> new NonNegativeUniform().initByName(
                "lower", new RealScalarParam<>(-1.0, Real.INSTANCE), "upper", pos(10.0)), "0 <= lower");
    }

    @Test
    public void testRejectsDegenerateBounds() {
        assertBoundsRejected(() -> uniform(5.0, 5.0), "0 <= lower");
        assertBoundsRejected(() -> uniform(10.0, 1.0), "0 <= lower");
        assertBoundsRejected(() -> uniform(0.0, Double.POSITIVE_INFINITY), "0 <= lower");
    }

    @Test
    public void testDefaultBounds() {
        NonNegativeUniform d = new NonNegativeUniform();
        d.initAndValidate();
        assertEquals(0.0, d.getLower(), 0.0);
        assertEquals(1.0, d.getUpper(), 0.0);
    }

    @Test
    public void testBothEndsIncluded() {
        NonNegativeUniform d = uniform(2.0, 10.0);
        assertTrue(d.lowerInclusive());
        assertTrue(d.upperInclusive());
        assertEquals("[2.0,10.0]", d.boundsToString());
        assertTrue(d.withinBounds(2.0));
        assertEquals(-Math.log(8.0), d.logDensity(2.0), 1e-12);
        assertEquals(-Math.log(8.0), d.logDensity(10.0), 1e-12);
    }

    @Test
    public void testOutsideSupportHasZeroDensity() {
        NonNegativeUniform d = uniform(0.0, 10.0);
        assertEquals(Double.NEGATIVE_INFINITY, d.logDensity(-1e-9), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, d.logDensity(10.0 + 1e-9), 0.0);
        assertEquals(0.0, d.density(-1.0), 0.0);
    }

    @Test
    public void testMatchesApacheUniform() {
        UniformContinuousDistribution apache = UniformContinuousDistribution.of(2.0, 7.0);
        NonNegativeUniform d = uniform(2.0, 7.0);
        for (double x : new double[]{1.0, 2.0, 3.5, 7.0, 8.0}) {
            assertEquals(apache.density(x), d.density(x), 1e-15, "x=" + x);
            assertEquals(apache.logDensity(x), d.logDensity(x), 1e-12, "x=" + x);
            assertEquals(apache.cumulativeProbability(x), d.cumulativeProbability(x), 1e-15, "x=" + x);
        }
        assertEquals(4.5, d.getMean(), 1e-12);
        assertEquals(5.75, d.inverseCumulativeProbability(0.75), 1e-12);
    }

    @Test
    public void testSamplesStayInSupport() {
        NonNegativeUniform d = uniform(0.0, 3.0);
        double sum = 0;
        int count = 20000;
        for (int i = 0; i < count; i++) {
            double x = d.sample().get(0);
            assertTrue(x >= 0.0 && x <= 3.0, "sample " + x);
            sum += x;
        }
        assertEquals(1.5, sum / count, 0.03);
    }

    @Test
    public void testMaySampleZero() {
        // the sampler's lowest possible draw (u = 0) gives exactly lower, here 0
        UniformRandomProvider saved = TensorDistribution.rng;
        try {
            TensorDistribution.rng = fixedDoubles(0.0);
            assertEquals(0.0, uniform(0.0, 10.0).sample().get(0), 0.0);
        } finally {
            TensorDistribution.rng = saved;
        }
    }

    @Test
    public void testCalculateLogPViaParam() {
        NonNegativeUniform d = new NonNegativeUniform(nonNeg(0.0), nonNeg(0.0), pos(100.0));
        assertEquals(-Math.log(100.0), d.calculateLogP(), 1e-12);

        NonNegativeUniform below = new NonNegativeUniform(nonNeg(0.5), nonNeg(1.0), pos(10.0));
        assertEquals(Double.NEGATIVE_INFINITY, below.calculateLogP(), 0.0);
    }

    @Test
    public void testBoundsChangedDuringMCMC() {
        // lower and upper may be estimated, so a change must rebuild the distribution and sampler
        RealScalarParam<NonNegativeReal> lo = nonNeg(0.5);
        RealScalarParam<PositiveReal> up = pos(1.0);
        NonNegativeUniform d = new NonNegativeUniform();
        d.initByName("lower", lo, "upper", up);
        assertTrue(d.sample().get(0) <= 1.0); // creates the sampler on [0.5, 1]

        lo.set(5.0);
        up.set(9.0);
        assertEquals(-Math.log(4.0), d.logDensity(6.0), 1e-12);
        assertEquals(Double.NEGATIVE_INFINITY, d.logDensity(0.75), 0.0);
        for (int i = 0; i < 1000; i++) {
            double x = d.sample().get(0);
            assertTrue(x >= 5.0 && x <= 9.0, "sample " + x);
        }

        // lower reaching 0 is valid
        lo.set(0.0);
        assertEquals(-Math.log(9.0), d.logDensity(0.0), 1e-12);

        // invalid bounds set during MCMC are reported, not silently used
        lo.set(10.0);
        assertThrows(IllegalArgumentException.class, () -> d.logDensity(6.0));
    }

    @Test
    public void testXMLConstruction() throws Exception {
        final String xml = "<distribution spec='beast.base.spec.inference.distribution.NonNegativeUniform'>\n" +
                "  <lower spec='beast.base.spec.inference.parameter.RealScalarParam' domain='NonNegativeReal' value='0.0'/>\n" +
                "  <upper spec='beast.base.spec.inference.parameter.RealScalarParam' domain='PositiveReal' value='1000'/>\n" +
                "</distribution>\n";
        NonNegativeUniform d = (NonNegativeUniform) new XMLParser().parseBareFragment(xml, true);
        assertEquals(-Math.log(1000.0), d.logDensity(1.0), 1e-12);
    }
}
