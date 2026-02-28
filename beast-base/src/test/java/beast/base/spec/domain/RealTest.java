package beast.base.spec.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the Real class
 */
public class RealTest {

    @Test
    public void testIsValid() {
        Real realDomain = Real.INSTANCE;

        // Valid inputs
        assertTrue(realDomain.isValid(0.0));
        assertTrue(realDomain.isValid(-1.0));
        assertTrue(realDomain.isValid(1.0));
        assertTrue(realDomain.isValid(Double.NEGATIVE_INFINITY));
        assertTrue(realDomain.isValid(Double.POSITIVE_INFINITY));

        // Invalid inputs
        assertFalse(realDomain.isValid(null));
        assertFalse(realDomain.isValid(Double.NaN));
    }

    @Test
    public void testGetTypeClass() {
        Real realDomain = Real.INSTANCE;
        assertEquals(Double.class, realDomain.getTypeClass());
    }

    @Test
    public void testGetLower() {
        Real realDomain = Real.INSTANCE;
        assertEquals(Double.NEGATIVE_INFINITY, realDomain.getLower(), 0.0);
    }

    @Test
    public void testGetUpper() {
        Real realDomain = Real.INSTANCE;
        assertEquals(Double.POSITIVE_INFINITY, realDomain.getUpper(), 0.0);
    }

    @Test
    public void testLowerInclusive() {
        Real realDomain = Real.INSTANCE;
        assertTrue(realDomain.lowerInclusive());
    }

    @Test
    public void testUpperInclusive() {
        Real realDomain = Real.INSTANCE;
        assertTrue(realDomain.upperInclusive());
    }
}