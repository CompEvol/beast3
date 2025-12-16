package beast.base.spec.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the NonNegativeReal class
 */
public class NonNegativeRealTest {

    @Test
    public void testIsValid() {
        NonNegativeReal nonNegativeRealDomain = NonNegativeReal.INSTANCE;

        // Valid inputs
        assertTrue(nonNegativeRealDomain.isValid(0.0)); // Test boundary case: value = 0
        assertTrue(nonNegativeRealDomain.isValid(42.0)); // Test positive value
        assertTrue(nonNegativeRealDomain.isValid(0.0001)); // Test small positive value

        // Invalid inputs
        assertFalse(nonNegativeRealDomain.isValid(-0.0001)); // Test small negative value
        assertFalse(nonNegativeRealDomain.isValid(-42.0)); // Test negative value
        assertFalse(nonNegativeRealDomain.isValid(null)); // Test null
        assertFalse(nonNegativeRealDomain.isValid(Double.NaN)); // Test NaN
    }

    @Test
    public void testGetTypeClass() {
        NonNegativeReal nonNegativeRealDomain = NonNegativeReal.INSTANCE;
        assertEquals(Double.class, nonNegativeRealDomain.getTypeClass()); // Ensure type is Double
    }

    @Test
    public void testGetLower() {
        NonNegativeReal nonNegativeRealDomain = NonNegativeReal.INSTANCE;
        assertEquals(0.0, nonNegativeRealDomain.getLower(), 0.0); // Lower bound should be 0
    }

    @Test
    public void testGetUpper() {
        NonNegativeReal nonNegativeRealDomain = NonNegativeReal.INSTANCE;
        assertEquals(Double.POSITIVE_INFINITY, nonNegativeRealDomain.getUpper(), 0.0); // Upper bound should be infinity
    }

    @Test
    public void testLowerInclusive() {
        NonNegativeReal nonNegativeRealDomain = NonNegativeReal.INSTANCE;
        assertTrue(nonNegativeRealDomain.lowerInclusive()); // Test that lower bound is inclusive
    }

    @Test
    public void testUpperInclusive() {
        NonNegativeReal nonNegativeRealDomain = NonNegativeReal.INSTANCE;
        assertTrue(nonNegativeRealDomain.upperInclusive()); // Test that upper bound is inclusive
    }
}