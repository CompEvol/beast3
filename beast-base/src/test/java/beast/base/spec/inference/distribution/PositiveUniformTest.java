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

import static beast.base.spec.inference.distribution.NonNegativeUniformTest.assertBoundsRejected;
import static beast.base.spec.inference.distribution.NonNegativeUniformTest.fixedDoubles;
import static beast.base.spec.inference.distribution.NonNegativeUniformTest.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PositiveUniform}: [lower, upper] with lower > 0, so lower = 0 is rejected.
 */
public class PositiveUniformTest {

    private static PositiveUniform uniform(double lower, double upper) {
        PositiveUniform d = new PositiveUniform();
        d.initByName("lower", pos(lower), "upper", pos(upper));
        return d;
    }

    @BeforeEach
    public void setUp() {
        Randomizer.setSeedReproducible(42);
    }

    @Test
    public void testIsNotANonNegativeUniform() {
        // standalone class, so BEAUti reads its PositiveReal domain from its own superclass
        assertFalse(NonNegativeUniform.class.isAssignableFrom(PositiveUniform.class));
    }

    @Test
    public void testDensityInsideSupport() {
        PositiveUniform d = uniform(1.0e-8, 1000.0);
        for (double x : new double[]{1.0e-8, 0.5, 10.0, 1000.0}) {
            assertEquals(-Math.log(1000.0 - 1.0e-8), d.logDensity(x), 1e-12, "x=" + x);
        }
        // 0 is never in the support
        assertEquals(Double.NEGATIVE_INFINITY, d.logDensity(0.0), 0.0);
    }

    @Test
    public void testRejectsZeroOrNegativeLower() {
        // The lower input is typed RealScalar<PositiveReal>, but XML can still pass a
        // parameter with another domain, so the distribution must check the value itself.
        assertBoundsRejected(() -> new PositiveUniform().initByName(
                "lower", new RealScalarParam<>(0.0, NonNegativeReal.INSTANCE), "upper", pos(10.0)), "0 < lower");
        assertBoundsRejected(() -> new PositiveUniform().initByName(
                "lower", new RealScalarParam<>(-1.0, Real.INSTANCE), "upper", pos(10.0)), "0 < lower");
    }

    @Test
    public void testRejectsDegenerateBounds() {
        assertBoundsRejected(() -> uniform(5.0, 5.0), "0 < lower");
        assertBoundsRejected(() -> uniform(10.0, 1.0), "0 < lower");
        assertBoundsRejected(() -> uniform(1.0, Double.POSITIVE_INFINITY), "0 < lower");
    }

    @Test
    public void testDefaultBounds() {
        // 0 is not allowed, so the default lower bound is positive
        PositiveUniform d = new PositiveUniform();
        d.initAndValidate();
        assertEquals(1.0E-8, d.getLower(), 0.0);
        assertEquals(1.0, d.getUpper(), 0.0);
        assertTrue(d.lowerInput.getTipText().contains("greater than 0"), d.lowerInput.getTipText());
    }

    @Test
    public void testBothEndsIncluded() {
        PositiveUniform d = uniform(2.0, 10.0);
        assertTrue(d.lowerInclusive());
        assertTrue(d.upperInclusive());
        assertEquals("[2.0,10.0]", d.boundsToString());
        assertTrue(d.withinBounds(2.0));
        assertEquals(-Math.log(8.0), d.logDensity(2.0), 1e-12);
        assertEquals(-Math.log(8.0), d.logDensity(10.0), 1e-12);
    }

    @Test
    public void testMatchesApacheUniform() {
        UniformContinuousDistribution apache = UniformContinuousDistribution.of(2.0, 7.0);
        PositiveUniform d = uniform(2.0, 7.0);
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
        PositiveUniform d = uniform(1.0e-8, 2.0);
        double sum = 0;
        int count = 20000;
        for (int i = 0; i < count; i++) {
            double x = d.sample().get(0);
            assertTrue(x > 0.0 && x >= 1.0e-8 && x <= 2.0, "sample " + x);
            sum += x;
        }
        assertEquals(1.0, sum / count, 0.02);
    }

    @Test
    public void testNeverSamplesZero() {
        // The sampler's lowest possible draw (u = 0) gives exactly lower, which is > 0.
        UniformRandomProvider saved = TensorDistribution.rng;
        try {
            TensorDistribution.rng = fixedDoubles(0.0);
            double x = uniform(1.0e-8, 10.0).sample().get(0);
            assertEquals(1.0e-8, x, 0.0);
            assertTrue(x > 0.0, "sampled " + x);
        } finally {
            TensorDistribution.rng = saved;
        }
    }

    @Test
    public void testCalculateLogPViaParam() {
        PositiveUniform d = new PositiveUniform(pos(3.0), pos(1.0), pos(101.0));
        assertEquals(-Math.log(100.0), d.calculateLogP(), 1e-12);

        PositiveUniform below = new PositiveUniform(pos(0.5), pos(1.0), pos(10.0));
        assertEquals(Double.NEGATIVE_INFINITY, below.calculateLogP(), 0.0);
    }

    @Test
    public void testBoundsChangedDuringMCMC() {
        // lower and upper may be estimated, so a change must rebuild the distribution and sampler
        RealScalarParam<PositiveReal> lo = pos(0.5);
        RealScalarParam<PositiveReal> up = pos(1.0);
        PositiveUniform d = new PositiveUniform();
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

        // invalid bounds set during MCMC are reported, not silently used
        lo.set(10.0);
        assertThrows(IllegalArgumentException.class, () -> d.logDensity(6.0));
    }

    @Test
    public void testLowerMovedToZeroDuringMCMCIsRejected() {
        RealScalarParam<NonNegativeReal> lo = new RealScalarParam<>(0.5, NonNegativeReal.INSTANCE);
        PositiveUniform d = new PositiveUniform();
        d.initByName("lower", lo, "upper", pos(1.0));
        lo.set(0.0);
        assertBoundsRejected(() -> d.logDensity(0.5), "0 < lower");
        assertBoundsRejected(d::sample, "0 < lower");
    }

    @Test
    public void testXMLConstruction() throws Exception {
        final String xml = "<distribution spec='beast.base.spec.inference.distribution.PositiveUniform'>\n" +
                "  <lower spec='beast.base.spec.inference.parameter.RealScalarParam' domain='PositiveReal' value='1.0E-8'/>\n" +
                "  <upper spec='beast.base.spec.inference.parameter.RealScalarParam' domain='PositiveReal' value='1000'/>\n" +
                "</distribution>\n";
        PositiveUniform d = (PositiveUniform) new XMLParser().parseBareFragment(xml, true);
        assertEquals(-Math.log(1000.0 - 1.0e-8), d.logDensity(1.0), 1e-12);
    }
}
