package beast.base.spec.type;

import beast.base.spec.domain.Int;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the IntScalar interface
 */
public class IntScalarTest {

    // Example implementation for testing
    private static class TestIntScalar implements IntScalar<Int> {
        private final int value;
        private final Int domain;

        public TestIntScalar(int value, Int domain) {
            this.value = value;
            this.domain = domain;
        }

        @Override
        public Int getDomain() {
            return domain;
        }

        @Override
        public int get() {
            return value;
        }
    }

    @Test
    public void testGet() {
        Int domain = Int.INSTANCE;
        IntScalar<Int> scalar = new TestIntScalar(10, domain);

        assertEquals(10, scalar.get());
    }

    @Test
    public void testGetWithIndices() {
        Int domain = Int.INSTANCE;
        IntScalar<Int> scalar = new TestIntScalar(20, domain);

        assertEquals(Integer.valueOf(20), scalar.get(0));
    }

    @Test
    public void testGetLower() {
        Int domain = Int.INSTANCE;
        IntScalar<Int> scalar = new TestIntScalar(10, domain);

        assertEquals(Integer.MIN_VALUE, (int) scalar.getLower());
    }

    @Test
    public void testGetUpper() {
        Int domain = Int.INSTANCE;
        IntScalar<Int> scalar = new TestIntScalar(10, domain);

        assertEquals(Integer.MAX_VALUE, (int) scalar.getUpper());
    }

    @Test
    public void testLowerInclusive() {
        Int domain = Int.INSTANCE;
        IntScalar<Int> scalar = new TestIntScalar(10, domain);

        assertTrue(scalar.lowerInclusive());
    }

    @Test
    public void testUpperInclusive() {
        Int domain = Int.INSTANCE;
        IntScalar<Int> scalar = new TestIntScalar(10, domain);

        assertTrue(scalar.upperInclusive());
    }

    @Test
    public void testIsValid() {
        Int domain = Int.INSTANCE;
        IntScalar<Int> scalar = new TestIntScalar(10, domain);

        assertTrue(scalar.isValid(0)); // Valid value within range
        assertTrue(scalar.isValid(Integer.MIN_VALUE)); // Valid at lower bound
        assertTrue(scalar.isValid(Integer.MAX_VALUE)); // Valid at upper bound
        assertFalse(scalar.isValid(null)); // Invalid null value
    }
}