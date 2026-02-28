package beast.base.spec.type;

import beast.base.spec.domain.Real;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the RealScalar interface
 */
public class RealScalarTest {

    // Example implementation for testing
    private static class TestRealScalar implements RealScalar<Real> {
        private final double value;
        private final Real domain;

        public TestRealScalar(double value, Real domain) {
            this.value = value;
            this.domain = domain;
        }

        @Override
        public Real getDomain() {
            return domain;
        }

        @Override
        public double get() {
            return value;
        }
    }

    @Test
    public void testGet() {
        Real domain = Real.INSTANCE;
        RealScalar<Real> scalar = new TestRealScalar(5.5, domain);

        assertEquals(5.5, scalar.get(), 1e-10);
    }

    @Test
    public void testGetWithIndices() {
        Real domain = Real.INSTANCE;
        RealScalar<Real> scalar = new TestRealScalar(3.1415, domain);

        assertEquals(Double.valueOf(3.1415), scalar.get(0)); // Boxed value
    }

    @Test
    public void testGetLower() {
        Real domain = Real.INSTANCE;
        RealScalar<Real> scalar = new TestRealScalar(0.0, domain);

        assertEquals(Double.NEGATIVE_INFINITY, scalar.getLower(), 0.0);
    }

    @Test
    public void testGetUpper() {
        Real domain = Real.INSTANCE;
        RealScalar<Real> scalar = new TestRealScalar(0.0, domain);

        assertEquals(Double.POSITIVE_INFINITY, scalar.getUpper(), 0.0);
    }

    @Test
    public void testLowerInclusive() {
        Real domain = Real.INSTANCE;
        RealScalar<Real> scalar = new TestRealScalar(0.0, domain);

        assertTrue(scalar.lowerInclusive());
    }

    @Test
    public void testUpperInclusive() {
        Real domain = Real.INSTANCE;
        RealScalar<Real> scalar = new TestRealScalar(0.0, domain);

        assertTrue(scalar.upperInclusive());
    }

    @Test
    public void testIsValid() {
        Real domain = Real.INSTANCE;
        RealScalar<Real> scalar = new TestRealScalar(1.23, domain);

        assertTrue(scalar.isValid(1.0)); // Valid value
        assertTrue(scalar.isValid(Double.NEGATIVE_INFINITY)); // Lower bound
        assertTrue(scalar.isValid(Double.POSITIVE_INFINITY)); // Upper bound
        assertFalse(scalar.isValid(Double.NaN)); // Invalid value
        assertFalse(scalar.isValid(null)); // Null is not valid
    }
}