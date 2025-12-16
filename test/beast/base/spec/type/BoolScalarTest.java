package beast.base.spec.type;

import beast.base.spec.domain.Bool;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for the BoolScalar interface
 */
public class BoolScalarTest {

    // Example implementation for testing
    private static class TestBoolScalar implements BoolScalar {
        private final boolean value;

        public TestBoolScalar(boolean value) {
            this.value = value;
        }

        @Override
        public boolean get() {
            return value;
        }

		@Override
		public Bool getDomain() {
			return Bool.INSTANCE;
		}
    }

    @Test
    public void testGet() {
        BoolScalar trueScalar = new TestBoolScalar(true);
        BoolScalar falseScalar = new TestBoolScalar(false);

        assertTrue(trueScalar.get()); // Test true value
        assertFalse(falseScalar.get()); // Test false value
    }

    @Test
    public void testGetWithIndices() {
        BoolScalar trueScalar = new TestBoolScalar(true);
        BoolScalar falseScalar = new TestBoolScalar(false);

        assertEquals(Boolean.TRUE, trueScalar.get()); // Test true value
        assertEquals(Boolean.FALSE, falseScalar.get()); // Test false value
    }
}