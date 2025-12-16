package beast.base.spec.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the PositiveReal class
 */
public class PositiveRealTest {

    @Test
    public void testIsValid() {
        PositiveReal positiveRealDomain = PositiveReal.INSTANCE;

        // Valid inputs
        assertTrue(positiveRealDomain.isValid(0.0001)); // Very small positive value
        assertTrue(positiveRealDomain.isValid(42.0)); // Positive value

        // Invalid inputs
        assertFalse(positiveRealDomain.isValid(0.0)); // Zero is invalid in PositiveReal
        assertFalse(positiveRealDomain.isValid(-0.0001)); // Negative value
        assertFalse(positiveRealDomain.isValid(-42.0)); // Large negative value
        assertFalse(positiveRealDomain.isValid(null)); // Null is invalid
        assertFalse(positiveRealDomain.isValid(Double.NaN)); // NaN is invalid
    }

    @Test
    public void testGetTypeClass() {
        PositiveReal positiveRealDomain = PositiveReal.INSTANCE;
        assertEquals(Double.class, positiveRealDomain.getTypeClass()); // Ensure type is Double
    }

    @Test
    public void testGetLower() {
        PositiveReal positiveRealDomain = PositiveReal.INSTANCE;
        assertEquals(0.0, positiveRealDomain.getLower(), 0.0); // Lower bound should be 0
    }

    @Test
    public void testGetUpper() {
        PositiveReal positiveRealDomain = PositiveReal.INSTANCE;
        assertEquals(Double.POSITIVE_INFINITY, positiveRealDomain.getUpper(), 0.0); // Upper bound should be infinity
    }

    @Test
    public void testLowerInclusive() {
        PositiveReal positiveRealDomain = PositiveReal.INSTANCE;
        assertFalse(positiveRealDomain.lowerInclusive()); // Lower boundary is exclusive
    }

    @Test
    public void testUpperInclusive() {
        PositiveReal positiveRealDomain = PositiveReal.INSTANCE;
        assertTrue(positiveRealDomain.upperInclusive()); // Upper boundary is inclusive
    }
}