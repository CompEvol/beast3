package beast.base.spec.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the UnitInterval class
 */
public class UnitIntervalTest {

    @Test
    public void testIsValid() {
        UnitInterval unitIntervalDomain = UnitInterval.INSTANCE;

        // Valid inputs
        assertTrue(unitIntervalDomain.isValid(0.0)); // Lower boundary, inclusive
        assertTrue(unitIntervalDomain.isValid(1.0)); // Upper boundary, inclusive
        assertTrue(unitIntervalDomain.isValid(0.5)); // Valid value within the range

        // Invalid inputs
        assertFalse(unitIntervalDomain.isValid(-0.1)); // Below lower boundary
        assertFalse(unitIntervalDomain.isValid(1.1)); // Above upper boundary
        assertFalse(unitIntervalDomain.isValid(null)); // Null value
        assertFalse(unitIntervalDomain.isValid(Double.NaN)); // NaN value
    }

    @Test
    public void testGetTypeClass() {
        UnitInterval unitIntervalDomain = UnitInterval.INSTANCE;
        assertEquals(Double.class, unitIntervalDomain.getTypeClass()); // Ensure type is Double
    }

    @Test
    public void testGetLower() {
        UnitInterval unitIntervalDomain = UnitInterval.INSTANCE;
        assertEquals(0.0, unitIntervalDomain.getLower(), 0.0); // Lower bound should be 0
    }

    @Test
    public void testGetUpper() {
        UnitInterval unitIntervalDomain = UnitInterval.INSTANCE;
        assertEquals(1.0, unitIntervalDomain.getUpper(), 0.0); // Upper bound should be 1
    }

    @Test
    public void testLowerInclusive() {
        UnitInterval unitIntervalDomain = UnitInterval.INSTANCE;
        assertTrue(unitIntervalDomain.lowerInclusive()); // Lower boundary is inclusive
    }

    @Test
    public void testUpperInclusive() {
        UnitInterval unitIntervalDomain = UnitInterval.INSTANCE;
        assertTrue(unitIntervalDomain.upperInclusive()); // Upper boundary is inclusive
    }
}