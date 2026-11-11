package beast.base.spec.type;

import beast.base.spec.domain.NonNegativeInt;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IntSimplexTest {

    // Test implementation of IntSimplex
    private static class TestIntSimplex implements IntSimplex<NonNegativeInt> {
        private final List<Integer> elements;
        private final int expectedSum;

        public TestIntSimplex(List<Integer> elements, int expectedSum) {
            this.elements = elements;
            this.expectedSum = expectedSum;
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
        public int expectedSum() {
            return expectedSum;
        }

        @Override
        public NonNegativeInt getDomain() {
            return NonNegativeInt.INSTANCE;
        }

        @Override
        public int size() {
            return elements.size();
        }
    }

    @Test
    public void testSum() {
        IntSimplex<NonNegativeInt> simplex = new TestIntSimplex(Arrays.asList(1, 2, 3), 6);
        assertEquals(6, simplex.sum(), 1e-10);
    }

    @Test
    public void testIsValid() {
        IntSimplex<NonNegativeInt> validSimplex = new TestIntSimplex(Arrays.asList(1, 2, 3), 6);
        assertTrue(validSimplex.isValid());

        IntSimplex<NonNegativeInt> invalidSimplex = new TestIntSimplex(Arrays.asList(1, 2, 3), 7);
        assertFalse(invalidSimplex.isValid());
    }

    @Test
    public void testElementOutOfDomainIsNotValidEvenIfSumIsRight() {
        // issue #255: the element check was ignored when the sum was right
        IntSimplex<NonNegativeInt> simplex = new TestIntSimplex(Arrays.asList(-1, 3, 4), 6);
        assertFalse(simplex.isValid());
        String why = simplex.whyNotValid();
        assertNotNull(why);
        assertTrue(why.contains("Element 1 (-1)"), why);
    }

    @Test
    public void testWhyNotValidGivesSum() {
        IntSimplex<NonNegativeInt> simplex = new TestIntSimplex(Arrays.asList(1, 2, 3), 7);
        assertEquals("The values must sum to 7, but they sum to 6.", simplex.whyNotValid());
        assertNull(new TestIntSimplex(Arrays.asList(1, 2, 3), 6).whyNotValid());
    }
}
