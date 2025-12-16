package beast.base.spec.type;

import beast.base.spec.domain.UnitInterval;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SimplexTest {

    // Test implementation of Simplex
    private static class TestSimplex implements Simplex {
        private final List<Double> elements;

        public TestSimplex(List<Double> elements) {
            this.elements = elements;
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
        public UnitInterval getDomain() {
            return UnitInterval.INSTANCE;
        }

        @Override
        public int size() {
            return elements.size();
        }
    }

    @Test
    public void testSum() {
        Simplex simplex = new TestSimplex(Arrays.asList(0.3, 0.3, 0.4));
        assertEquals(1.0, simplex.sum(), 1e-10);
    }

    @Test
    public void testIsValid() {
        Simplex validSimplex = new TestSimplex(Arrays.asList(0.3, 0.3, 0.4));
        assertTrue(validSimplex.isValid());

        Simplex invalidSimplex = new TestSimplex(Arrays.asList(0.3, 0.3, 0.5));
        assertFalse(invalidSimplex.isValid());
    }
}