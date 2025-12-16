package beast.base.spec.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the Int class
 */
public class IntTest {

    @Test
    public void testIsValid() {
        Int integerDomain = Int.INSTANCE;
        assertTrue(integerDomain.isValid(0)); // Test zero
        assertTrue(integerDomain.isValid(42)); // Test a positive integer
        assertTrue(integerDomain.isValid(-42)); // Test a negative integer
        assertFalse(integerDomain.isValid(null)); // Test null
    }

    @Test
    public void testGetTypeClass() {
        Int integerDomain = Int.INSTANCE;
        assertEquals(Integer.class, integerDomain.getTypeClass()); // Test type class
    }

    @Test
    public void testGetLower() {
        Int integerDomain = Int.INSTANCE;
        assertEquals(Integer.MIN_VALUE, (int) integerDomain.getLower()); // Test lower bound
    }

    @Test
    public void testGetUpper() {
        Int integerDomain = Int.INSTANCE;
        assertEquals(Integer.MAX_VALUE, (int) integerDomain.getUpper()); // Test upper bound
    }

    @Test
    public void testLowerInclusive() {
        Int integerDomain = Int.INSTANCE;
        assertTrue(integerDomain.lowerInclusive()); // Test lower inclusivity
    }

    @Test
    public void testUpperInclusive() {
        Int integerDomain = Int.INSTANCE;
        assertTrue(integerDomain.upperInclusive()); // Test upper inclusivity
    }
}