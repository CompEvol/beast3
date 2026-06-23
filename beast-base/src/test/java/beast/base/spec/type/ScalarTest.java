package beast.base.spec.type;

import beast.base.spec.domain.Int;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the Scalar interface
 */
public class ScalarTest {

    // Example implementation of Scalar for testing
    private static class TestIntScalar implements Scalar<Int, Integer> {
        private final Int domain;
        private final int value;

        public TestIntScalar(int value, Int domain) {
            this.value = value;
            this.domain = domain;
        }

        @Override
        public Int getDomain() {
            return domain;
        }

		@Override
		public Integer get(int... idx) {
			return null;
		}
    }

    @Test
    public void testRank() {
        Scalar<Int, Integer> scalar = new TestIntScalar(42, Int.INSTANCE);
        assertEquals(0, scalar.rank());
    }

    @Test
    public void testShape() {
        Scalar<Int, Integer> scalar = new TestIntScalar(42, Int.INSTANCE);
        assertArrayEquals(new int[] {}, scalar.shape());
    }

    @Test
    public void testValidation() {
        Scalar<Int, Integer> scalar = new TestIntScalar(42, Int.INSTANCE);
        assertTrue(scalar.isValid(42)); // Valid integer
        assertTrue(scalar.isValid(Integer.MIN_VALUE)); // Acceptable lower bound
        assertTrue(scalar.isValid(Integer.MAX_VALUE)); // Acceptable upper bound
        assertFalse(scalar.isValid(null)); // Invalid value (null)
    }
}