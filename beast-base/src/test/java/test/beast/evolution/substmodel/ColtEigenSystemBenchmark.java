package test.beast.evolution.substmodel;

import beast.base.evolution.substitutionmodel.ColtEigenSystem;
import beast.base.evolution.substitutionmodel.ComplexColtEigenSystem;
import beast.base.evolution.substitutionmodel.EigenDecomposition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * Benchmark for ColtEigenSystem eigendecomposition and matrix exponentiation.
 * Tagged as "slow" so it doesn't run in normal CI.
 *
 * Run with: mvn test -pl beast-base -Dtest=test.beast.evolution.substmodel.ColtEigenSystemBenchmark -Dsurefire.excludedGroups=
 */
@Tag("slow")
public class ColtEigenSystemBenchmark {

    /**
     * Build a random reversible rate matrix of given dimension.
     */
    private static double[][] buildRandomReversibleQ(int n, Random rng) {
        double[] pi = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            pi[i] = rng.nextDouble() + 0.01;
            sum += pi[i];
        }
        for (int i = 0; i < n; i++) pi[i] /= sum;

        double[][] Q = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double rate = rng.nextDouble() * 5 + 0.1;
                Q[i][j] = rate * pi[j];
                Q[j][i] = rate * pi[i];
            }
        }
        // Set diagonal
        double totalRate = 0;
        for (int i = 0; i < n; i++) {
            double rowSum = 0;
            for (int j = 0; j < n; j++) {
                if (i != j) rowSum += Q[i][j];
            }
            Q[i][i] = -rowSum;
            totalRate += pi[i] * rowSum;
        }
        // Normalize
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Q[i][j] /= totalRate;
            }
        }
        return Q;
    }

    /**
     * Build a random non-reversible rate matrix.
     */
    private static double[][] buildRandomNonReversibleQ(int n, Random rng) {
        double[][] Q = new double[n][n];
        for (int i = 0; i < n; i++) {
            double rowSum = 0;
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    Q[i][j] = rng.nextDouble() * 2 + 0.01;
                    rowSum += Q[i][j];
                }
            }
            Q[i][i] = -rowSum;
        }
        return Q;
    }

    @Test
    public void benchmarkEigenDecomposition() {
        System.out.println("=== EigenDecomposition Benchmark ===");
        System.out.println();

        int[] sizes = {4, 20, 61};
        int[] iterations = {100000, 10000, 1000};

        Random rng = new Random(42);

        for (int s = 0; s < sizes.length; s++) {
            int n = sizes[s];
            int iters = iterations[s];

            // Pre-generate matrices
            double[][][] matrices = new double[iters][][];
            for (int i = 0; i < iters; i++) {
                matrices[i] = buildRandomReversibleQ(n, rng);
            }

            ColtEigenSystem eigenSystem = new ColtEigenSystem(n);

            // Warmup
            for (int i = 0; i < Math.min(1000, iters); i++) {
                eigenSystem.decomposeMatrix(matrices[i]);
            }

            // Timed: decompose
            long start = System.nanoTime();
            for (int i = 0; i < iters; i++) {
                eigenSystem.decomposeMatrix(matrices[i]);
            }
            long decompTime = System.nanoTime() - start;

            // Timed: decompose + exponentiate
            start = System.nanoTime();
            double[] P = new double[n * n];
            for (int i = 0; i < iters; i++) {
                EigenDecomposition decomp = eigenSystem.decomposeMatrix(matrices[i]);
                eigenSystem.computeExponential(decomp, 0.1, P);
            }
            long totalTime = System.nanoTime() - start;

            System.out.printf("%dx%d reversible (ColtEigenSystem, %d iters):%n", n, n, iters);
            System.out.printf("  decompose:              %8.1f us/iter%n", decompTime / 1000.0 / iters);
            System.out.printf("  decompose + expm:       %8.1f us/iter%n", totalTime / 1000.0 / iters);
            System.out.println();
        }

        // Non-reversible with ComplexColtEigenSystem
        for (int s = 0; s < sizes.length; s++) {
            int n = sizes[s];
            int iters = iterations[s];

            double[][][] matrices = new double[iters][][];
            for (int i = 0; i < iters; i++) {
                matrices[i] = buildRandomNonReversibleQ(n, rng);
            }

            ComplexColtEigenSystem eigenSystem = new ComplexColtEigenSystem(n);

            // Warmup
            for (int i = 0; i < Math.min(1000, iters); i++) {
                eigenSystem.decomposeMatrix(matrices[i]);
            }

            // Timed
            long start = System.nanoTime();
            double[] P = new double[n * n];
            for (int i = 0; i < iters; i++) {
                EigenDecomposition decomp = eigenSystem.decomposeMatrix(matrices[i]);
                eigenSystem.computeExponential(decomp, 0.1, P);
            }
            long totalTime = System.nanoTime() - start;

            System.out.printf("%dx%d non-reversible (ComplexColtEigenSystem, %d iters):%n", n, n, iters);
            System.out.printf("  decompose + expm:       %8.1f us/iter%n", totalTime / 1000.0 / iters);
            System.out.println();
        }
    }
}
