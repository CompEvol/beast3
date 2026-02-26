package beast.base.spec.type;

import beast.base.spec.domain.Bool;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class BoolVectorTest {

    // Test implementation of BoolVector
    private static class TestBoolVector implements BoolVector {
        private final List<Boolean> elements;

        public TestBoolVector(List<Boolean> elements) {
            this.elements = elements;
        }

        @Override
        public boolean get(int index) {
            return elements.get(index);
        }

        @Override
        public List<Boolean> getElements() {
            return elements;
        }

        @Override
        public Bool getDomain() {
            return Bool.INSTANCE;
        }
    }

    @Test
    public void testGetElement() {
        BoolVector vector = new TestBoolVector(Arrays.asList(true, false, true));
        assertTrue(vector.get(0));
        assertFalse(vector.get(1));
    }

    @Test
    public void testShapeAndRank() {
        BoolVector vector = new TestBoolVector(Arrays.asList(true, false, true, false));
        assertArrayEquals(new int[]{4}, vector.shape());
        assertEquals(1, vector.rank());
    }

    @Test
    public void testValidation() {
        BoolVector vector = new TestBoolVector(Arrays.asList(true, false, true));
        assertTrue(vector.isValid());
    }
}