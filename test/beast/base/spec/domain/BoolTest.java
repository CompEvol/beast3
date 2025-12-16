package beast.base.spec.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the Bool class
 */
public class BoolTest {

    @Test
    public void testIsValid() {
        Bool boolDomain = Bool.INSTANCE;

        // Valid inputs
        assertTrue(boolDomain.isValid(true)); // Test valid true value
        assertTrue(boolDomain.isValid(false)); // Test valid false value

        // Invalid inputs
        assertFalse(boolDomain.isValid(null)); // Test null value
    }

    @Test
    public void testGetTypeClass() {
        Bool boolDomain = Bool.INSTANCE;
        assertEquals(Boolean.class, boolDomain.getTypeClass()); // Ensure type is Boolean
    }
}