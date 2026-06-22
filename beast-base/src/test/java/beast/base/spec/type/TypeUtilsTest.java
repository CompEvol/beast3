package beast.base.spec.type;

import beast.base.spec.domain.Domain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for TypeUtils
 */
public class TypeUtilsTest {

    // Mock Tensor implementation for testing
    private static class TestTensor implements Tensor {
        private final int[] shape;

        public TestTensor(int[] shape) {
            this.shape = shape;
        }

        @Override
        public int[] shape() {
            return shape;
        }

		@Override
		public Object get(int... idx) {
			return null;
		}

		@Override
		public Domain getDomain() {
			return null;
		}

		@Override
		public int rank() {
			return shape.length;
		}

		@Override
		public boolean isValid(Object value) {
			return true; // test validation in other tests
		}
    }

    @Test
    public void testShapeToStringEmptyShape() {
        Tensor tensor = new TestTensor(new int[] {});
        String result = TypeUtils.shapeToString(tensor);
        assertEquals("", result); // Expecting an empty string
    }

    @Test
    public void testShapeToStringVector() {
        Tensor tensor = new TestTensor(new int[] {5});
        String result = TypeUtils.shapeToString(tensor);
        assertEquals("5", result); // Expecting single dimension as string
    }

    @Test
    public void testShapeToStringMatrix() {
        Tensor tensor = new TestTensor(new int[] {3, 4});
        String result = TypeUtils.shapeToString(tensor);
        assertEquals("[3, 4]", result); // Expecting matrix dimensions as a string
    }

    @Test
    public void testShapeToStringHigherDimensions() {
        Tensor tensor = new TestTensor(new int[] {2, 3, 4});
        String result = TypeUtils.shapeToString(tensor);
        assertEquals("[2, 3, 4]", result); // Expecting higher dimensional array as a string
    }
}