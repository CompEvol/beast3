package beast.base.spec.type;

import beast.base.spec.domain.Int;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class IntVectorTest {

    // Test implementation of IntVector
    private static class TestIntVector implements IntVector<Int> {
        private final List<Integer> elements;
        private final Int domain;

        public TestIntVector(List<Integer> elements, Int domain) {
            this.elements = elements;
            this.domain = domain;
        }

        @Override
        public int get(int index) {
            return elements.get(index);
        }

        @Override
        public List<Integer> getElements() {
            return elements;
        }

        @Override
        public Int getDomain() {
            return domain;
        }
    }

    @Test
    public void testGetElement() {
        IntVector<Int> vector = new TestIntVector(Arrays.asList(1, 2, 3), Int.INSTANCE);
        assertEquals(1, (int) vector.get(0));
        assertEquals(3, (int) vector.get(2));
    }

    @Test
    public void testShapeAndRank() {
        IntVector<Int> vector = new TestIntVector(Arrays.asList(1, 2, 3), Int.INSTANCE);
        assertArrayEquals(new int[]{3}, vector.shape());
        assertEquals(1, vector.rank());
    }

    @Test
    public void testValidation() {
        IntVector<Int> vector = new TestIntVector(Arrays.asList(1, 2, 3), Int.INSTANCE);
        assertTrue(vector.isValid());
    }
}