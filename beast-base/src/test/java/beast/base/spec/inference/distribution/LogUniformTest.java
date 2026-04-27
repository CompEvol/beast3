package beast.base.spec.inference.distribution;

import beast.base.parser.XMLParser;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.Randomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogUniformTest {

    private LogUniform newLogUniform(double lower, double upper) {
        LogUniform d = new LogUniform();
        d.initByName(
                "lower", new RealScalarParam<>(lower, PositiveReal.INSTANCE),
                "upper", new RealScalarParam<>(upper, PositiveReal.INSTANCE));
        return d;
    }

    @BeforeEach
    public void setUp() {
        Randomizer.setSeed(42);
    }

    @Test
    public void testLogDensityAgainstClosedForm() {
        LogUniform d = newLogUniform(1.0e-3, 1.0e3);
        double logRange = Math.log(1.0e3 / 1.0e-3); // = 6 log 10
        for (double x : new double[]{1.0e-2, 1.0, 50.0, 500.0}) {
            double expected = -Math.log(x) - Math.log(logRange);
            assertEquals(expected, d.logDensity(x), 1e-12);
            assertEquals(Math.exp(expected), d.density(x), 1e-12);
        }
    }

    @Test
    public void testDensityZeroOutsideSupport() {
        LogUniform d = newLogUniform(1.0, 10.0);
        assertEquals(0.0, d.density(0.5), 0.0);
        assertEquals(0.0, d.density(11.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, d.logDensity(0.5), 0.0);
    }

    @Test
    public void testCDFAndInverseRoundTrip() {
        LogUniform d = newLogUniform(1.0e-6, 1.0e6);
        for (int i = 0; i < 1000; i++) {
            double p = Randomizer.nextDouble();
            double x = d.inverseCumulativeProbability(p);
            assertTrue(x >= 1.0e-6 && x <= 1.0e6);
            assertEquals(p, d.cumulativeProbability(x), 1e-12);
        }
    }

    @Test
    public void testSupportIsUniformInLogSpace() {
        // Median of LogUniform[a,b] is sqrt(a*b)
        LogUniform d = newLogUniform(1.0, 100.0);
        assertEquals(10.0, d.inverseCumulativeProbability(0.5), 1e-12);
    }

    @Test
    public void testMean() {
        // E[X] = (b-a) / log(b/a)
        LogUniform d = newLogUniform(1.0, Math.E);
        assertEquals(Math.E - 1.0, d.getMean(), 1e-12);
    }

    @Test
    public void testCalcLogPViaParam() {
        LogUniform d = new LogUniform();
        d.initByName(
                "param", new RealScalarParam<>(10.0, PositiveReal.INSTANCE),
                "lower", new RealScalarParam<>(1.0, PositiveReal.INSTANCE),
                "upper", new RealScalarParam<>(100.0, PositiveReal.INSTANCE));
        double expected = -Math.log(10.0) - Math.log(Math.log(100.0));
        assertEquals(expected, d.calculateLogP(), 1e-12);
    }

    @Test
    public void testXMLConstruction() throws Exception {
        final String xml = "<distribution spec='beast.base.spec.inference.distribution.LogUniform'>\n" +
                "  <lower spec='beast.base.spec.inference.parameter.RealScalarParam' " +
                "         domain='PositiveReal' value='0.001'/>\n" +
                "  <upper spec='beast.base.spec.inference.parameter.RealScalarParam' " +
                "         domain='PositiveReal' value='1000'/>\n" +
                "</distribution>\n";
        XMLParser parser = new XMLParser();
        LogUniform d = (LogUniform) parser.parseBareFragment(xml, true);
        double expected = -Math.log(1.0) - Math.log(Math.log(1.0e6));
        assertEquals(expected, d.logDensity(1.0), 1e-12);
    }

    @Test
    public void testRejectsDegenerateBounds() {
        // PositiveReal blocks lower <= 0 at parameter construction; distribution must
        // also reject equal and inverted bounds. initAndValidate wraps our IAE into a
        // bare RuntimeException with no cause, so we match on the message.
        RuntimeException e1 = assertThrows(RuntimeException.class, () -> newLogUniform(5.0, 5.0));
        assertTrue(e1.getMessage().contains("LogUniform requires"), "unexpected: " + e1.getMessage());
        RuntimeException e2 = assertThrows(RuntimeException.class, () -> newLogUniform(10.0, 1.0));
        assertTrue(e2.getMessage().contains("LogUniform requires"), "unexpected: " + e2.getMessage());
    }
}
