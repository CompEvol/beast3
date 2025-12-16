package beast.base.spec.type;

import beast.base.spec.domain.Real;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RealVectorTest {

    // Test implementation of RealVector
    private static class TestRealVector implements RealVector<Real> {
        private final List<Double> elements;
        private final Real domain;

        public TestRealVector(List<Double> elements, Real domain) {
            this.elements = elements;
            this.domain = domain;
        }

        @Override
        public double get(int index) {
            return elements.get(index);
        }

        @Override
        public List<Double> getElements() {
            return elements;
        }

        @Override
        public Real getDomain() {
            return domain;
        }
    }

    @Test
    public void testGetElement() {
        RealVector<Real> vector = new TestRealVector(Arrays.asList(1.1, 2.2, 3.3), Real.INSTANCE);
        assertEquals(1.1, vector.get(0), 0.001);
        assertEquals(3.3, vector.get(2), 0.001);
    }

    @Test
    public void testShapeAndRank() {
        RealVector<Real> vector = new TestRealVector(Arrays.asList(1.1, 2.2, 3.3), Real.INSTANCE);
        assertArrayEquals(new int[]{3}, vector.shape());
        assertEquals(1, vector.rank());
    }

    @Test
    public void testValidation() {
        RealVector<Real> vector = new TestRealVector(Arrays.asList(1.0, 2.0, 3.0), Real.INSTANCE);
        assertTrue(vector.isValid());
    }
}