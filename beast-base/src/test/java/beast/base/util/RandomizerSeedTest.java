package beast.base.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests the seeding behaviour of {@link Randomizer#setSeed(long)} and
 * {@link Randomizer#setSeedReproducible(long)}.
 * <p>
 * Randomizer keeps one generator per thread name. {@code setSeed} reseeds the
 * existing generators by their position in an internal map, so a fixed seed
 * can give a different stream depending on which threads used the Randomizer
 * earlier in the JVM (e.g. previous tests). {@code setSeedReproducible} drops
 * the per-thread generators first, so a fixed seed always gives the same stream.
 */
public class RandomizerSeedTest {

    private static final long SEED = 127;
    private static final int N = 10;

    private static double[] draw() {
        double[] v = new double[N];
        for (int i = 0; i < N; i++) {
            v[i] = Randomizer.nextDouble();
        }
        return v;
    }

    /** Make the Randomizer create generators for other thread names, as earlier tests would. */
    private static void useRandomizerFromOtherThreads(String... names) throws InterruptedException {
        for (String name : names) {
            Thread t = new Thread(Randomizer::nextDouble, name);
            t.start();
            t.join();
        }
    }

    @Test
    public void setSeedReproducibleGivesSameSequence() {
        Randomizer.setSeedReproducible(SEED);
        double[] first = draw();
        Randomizer.setSeedReproducible(SEED);
        double[] second = draw();
        assertArrayEquals(first, second);
    }

    @Test
    public void setSeedReproducibleIgnoresOtherThreads() throws InterruptedException {
        Randomizer.setSeedReproducible(SEED);
        double[] expected = draw();

        useRandomizerFromOtherThreads("seed-test-worker-1", "seed-test-worker-2", "seed-test-worker-3");

        Randomizer.setSeedReproducible(SEED);
        assertArrayEquals(expected, draw(),
                "fixed seed must give the same stream whatever threads used the Randomizer before");
    }

    @Test
    public void setSeedReproducibleDifferentSeedsDiffer() {
        Randomizer.setSeedReproducible(SEED);
        double[] a = draw();
        Randomizer.setSeedReproducible(SEED + 1);
        double[] b = draw();
        assertFalse(java.util.Arrays.equals(a, b));
    }

    @Test
    public void setSeedReproducibleGivesReproducibleWorkerThreads() throws InterruptedException {
        double[][] runs = new double[2][];
        for (int r = 0; r < 2; r++) {
            Randomizer.setSeedReproducible(SEED);
            double[] result = new double[N];
            Thread t = new Thread(() -> System.arraycopy(draw(), 0, result, 0, N), "seed-test-worker");
            t.start();
            t.join();
            runs[r] = result;
        }
        assertArrayEquals(runs[0], runs[1]);
    }

    /**
     * Documents the known limitation of {@link Randomizer#setSeed(long)}: with
     * the same seed, the stream differs once the calling thread or other
     * threads already have generators. If this test starts failing, setSeed
     * has become reproducible and setSeedReproducible may no longer be needed.
     */
    @Test
    public void setSeedDependsOnThreadHistory() throws InterruptedException {
        // fresh state: the calling thread has no generator yet
        Randomizer.setSeedReproducible(SEED);
        double[] fresh = draw();

        // calling thread and other threads now have generators
        useRandomizerFromOtherThreads("seed-test-worker-1", "seed-test-worker-2");
        Randomizer.setSeed(SEED);
        double[] afterHistory = draw();

        assertFalse(java.util.Arrays.equals(fresh, afterHistory),
                "setSeed is expected to depend on which threads used the Randomizer before");
    }
}
