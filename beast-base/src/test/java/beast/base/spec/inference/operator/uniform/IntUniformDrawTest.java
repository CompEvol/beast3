package beast.base.spec.inference.operator.uniform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link IntUniformOperator#uniform(int, int)}: upper - lower + 1 overflows an int for a wide
 * range such as a domain's [0, Integer.MAX_VALUE], which made Randomizer.nextInt() throw
 * "n must be positive".
 */
public class IntUniformDrawTest {

    @Test
    public void testNarrowRange() {
        int[] counts = new int[4];
        for (int i = 0; i < 4000; i++) {
            int x = IntUniformOperator.uniform(1, 3);
            assertTrue(x >= 1 && x <= 3, "drew " + x);
            counts[x]++;
        }
        for (int x = 1; x <= 3; x++) {
            assertTrue(counts[x] > 1000, "value " + x + " drawn " + counts[x] + " times");
        }
        assertEquals(5, IntUniformOperator.uniform(5, 5));
    }

    @Test
    public void testRangesThatOverflowAnInt() {
        int[][] ranges = {{0, Integer.MAX_VALUE}, {Integer.MIN_VALUE, Integer.MAX_VALUE}, {-1, Integer.MAX_VALUE}};
        for (int[] r : ranges) {
            for (int i = 0; i < 1000; i++) {
                int x = assertDoesNotThrow(() -> IntUniformOperator.uniform(r[0], r[1]));
                assertTrue(x >= r[0] && x <= r[1], "drew " + x + " from [" + r[0] + ", " + r[1] + "]");
            }
        }
    }

    @Test
    public void testEmptyRange() {
        assertThrows(IllegalArgumentException.class, () -> IntUniformOperator.uniform(5, 4));
    }
}
