package beast.base.spec.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the NonNegativeInt class
 */
public class NonNegativeIntTest {

    @Test
    public void testIsValid() {
        NonNegativeInt nonNegativeIntDomain = NonNegativeInt.INSTANCE;
        assertTrue(nonNegativeIntDomain.isValid(0)); // Test zero (boundary case)
        assertTrue(nonNegativeIntDomain.isValid(42)); // Test a positive integer
        assertFalse(nonNegativeIntDomain.isValid(-1)); // Test a negative value
        assertFalse(nonNegativeIntDomain.isValid(null)); // Test null
    }

    @Test
    public void testGetTypeClass() {
        NonNegativeInt nonNegativeIntDomain = NonNegativeInt.INSTANCE;
        assertEquals(Integer.class, nonNegativeIntDomain.getTypeClass()); // Test type class
    }

    @Test
    public void testGetLower() {
        NonNegativeInt nonNegativeIntDomain = NonNegativeInt.INSTANCE;
        assertEquals(0, (int) nonNegativeIntDomain.getLower()); // Test lower bound is zero
    }

    @Test
    public void testGetUpper() {
        NonNegativeInt nonNegativeIntDomain = NonNegativeInt.INSTANCE;
        assertEquals(Integer.MAX_VALUE, (int) nonNegativeIntDomain.getUpper()); // Test upper bound
    }

    @Test
    public void testLowerInclusive() {
        NonNegativeInt nonNegativeIntDomain = NonNegativeInt.INSTANCE;
        assertTrue(nonNegativeIntDomain.lowerInclusive()); // Test lower inclusivity
    }

    @Test
    public void testUpperInclusive() {
        NonNegativeInt nonNegativeIntDomain = NonNegativeInt.INSTANCE;
        assertTrue(nonNegativeIntDomain.upperInclusive()); // Test upper inclusivity
    }
}