package beast.base.spec.type;

import beast.base.spec.domain.Int;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for the Vector interface
 */
public class VectorTest {

    // Example implementation of Vector for testing
    private static class TestIntVector implements Vector<Int, Integer> {
        private final List<Integer> elements;
        private final Int domain;

        public TestIntVector(List<Integer> elements, Int domain) {
            this.elements = Collections.unmodifiableList(elements);
            this.domain = domain;
        }

        @Override
        public List<Integer> getElements() {
            return elements;
        }

        @Override
        public Int getDomain() {
            return domain;
        }

        @Override
        public boolean isValid(Integer value) {
            return domain.isValid(value);
        }

		@Override
		public Integer get(int... idx) {
            return elements.get(idx[0]);
		}
    }

    @Test
    public void testGetElements() {
        List<Integer> elements = Arrays.asList(1, 2, 3);
        Vector<Int, Integer> vector = new TestIntVector(elements, Int.INSTANCE);

        assertEquals(elements, vector.getElements());
    }

    @Test
    public void testSize() {
        List<Integer> elements = Arrays.asList(1, 2, 3, 4);
        Vector<Int, Integer> vector = new TestIntVector(elements, Int.INSTANCE);

        assertEquals(4, vector.size());
    }

    @Test
    public void testRank() {
        List<Integer> elements = Arrays.asList(1, 2, 3);
        Vector<Int, Integer> vector = new TestIntVector(elements, Int.INSTANCE);

        assertEquals(1, vector.rank());
    }

    @Test
    public void testShape() {
        List<Integer> elements = Arrays.asList(1, 2, 3, 4, 5);
        Vector<Int, Integer> vector = new TestIntVector(elements, Int.INSTANCE);

        assertArrayEquals(new int[]{5}, vector.shape());
    }

    @Test
    public void testIsValid() {
        List<Integer> elements = Arrays.asList(1, 2, -1, 3);
        Vector<Int, Integer> vector = new TestIntVector(elements, Int.INSTANCE);

        assertTrue(vector.isValid());
    }

    @Test
    public void testIsValidWithInvalidElement() {
        List<Integer> elements = Arrays.asList(1, 2, null, 3);
        Vector<Int, Integer> vector = new TestIntVector(elements, Int.INSTANCE);

        assertFalse(vector.isValid());
    }
}