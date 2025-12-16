package beast.base.spec.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the PositiveInt class
 */
public class PositiveIntTest {

    @Test
    public void testIsValid() {
        PositiveInt positiveIntDomain = PositiveInt.INSTANCE;

        assertTrue(positiveIntDomain.isValid(1));  // Test lower boundary (exclusive zero)
        assertTrue(positiveIntDomain.isValid(42)); // Test a positive integer
        assertFalse(positiveIntDomain.isValid(0)); // Test zero (should be invalid)
        assertFalse(positiveIntDomain.isValid(-1)); // Test a negative value
        assertFalse(positiveIntDomain.isValid(null)); // Test null
    }

    @Test
    public void testGetTypeClass() {
        PositiveInt positiveIntDomain = PositiveInt.INSTANCE;
        assertEquals(Integer.class, positiveIntDomain.getTypeClass()); // Test type class
    }

    @Test
    public void testGetLower() {
        PositiveInt positiveIntDomain = PositiveInt.INSTANCE;
        assertEquals(0, (int) positiveIntDomain.getLower()); // Test lower bound is zero but exclusive
    }

    @Test
    public void testGetUpper() {
        PositiveInt positiveIntDomain = PositiveInt.INSTANCE;
        assertEquals(Integer.MAX_VALUE, (int) positiveIntDomain.getUpper()); // Test upper bound
    }

    @Test
    public void testLowerInclusive() {
        PositiveInt positiveIntDomain = PositiveInt.INSTANCE;
        assertFalse(positiveIntDomain.lowerInclusive()); // Test lower inclusivity (should be false)
    }

    @Test
    public void testUpperInclusive() {
        PositiveInt positiveIntDomain = PositiveInt.INSTANCE;
        assertTrue(positiveIntDomain.upperInclusive()); // Test upper inclusivity
    }
}