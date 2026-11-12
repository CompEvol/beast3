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

    /** the sum is right, but one element is outside the domain (issue #255) */
    @Test
    public void testIsValidRejectsElementOutsideDomain() {
        IntSimplex<NonNegativeInt> negativeElement = new TestIntSimplex(Arrays.asList(-1, 3, 4), 6);
        assertEquals(6, negativeElement.sum(), 1e-10);
        assertFalse(negativeElement.isValid());
    }

    @Test
    public void testWhyNotValid() {
        assertNull(new TestIntSimplex(Arrays.asList(1, 2, 3), 6).whyNotValid());

        assertEquals("The values must sum to 7, but they sum to 6.",
                new TestIntSimplex(Arrays.asList(1, 2, 3), 7).whyNotValid());

        // an element outside the domain is reported before the sum
        String why = new TestIntSimplex(Arrays.asList(-1, 3, 4), 6).whyNotValid();
        assertTrue(why.startsWith("Element 1 (-1)"), why);
    }
}