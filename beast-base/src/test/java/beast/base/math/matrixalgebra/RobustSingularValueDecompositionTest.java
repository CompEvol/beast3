package beast.base.math.matrixalgebra;

import org.junit.jupiter.api.Test;

import static beast.base.math.matrixalgebra.RobustEigenDecompositionTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for RobustSingularValueDecomposition.
 * Verifies singular values, reconstruction (A = U * S * V^T),
 * orthogonality of U and V, rank, and ordering properties.
 */
public class RobustSingularValueDecompositionTest {

    @Test
    public void testDiagonal3x3() {
        // Singular values of a diagonal matrix are the absolute values of diagonal entries
        double[][] A = {{3, 0, 0}, {0, -5, 0}, {0, 0, 2}};
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);

        double[] sv = svd.getSingularValues();
        assertEquals(5.0, sv[0], 1e-12);
        assertEquals(3.0, sv[1], 1e-12);
        assertEquals(2.0, sv[2], 1e-12);
    }

    @Test
    public void testKnownSingularValues2x2() {
        // A = [[4, 0], [3, -5]]
        // A^T*A = [[25, -15], [-15, 25]], eigenvalues 40, 10
        // Singular values: sqrt(40), sqrt(10)
        double[][] A = {{4, 0}, {3, -5}};
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);

        double[] sv = svd.getSingularValues();
        assertEquals(Math.sqrt(40), sv[0], 1e-10);
        assertEquals(Math.sqrt(10), sv[1], 1e-10);
    }

    @Test
    public void testReconstructionSquare() {
        // Verify A = U * S * V^T for a 4x4 matrix
        double[][] A = {
                {1.5, 2.3, 0.7, 4.1},
                {3.2, 0.8, 5.6, 1.9},
                {0.4, 6.1, 2.2, 3.7},
                {5.5, 1.0, 3.3, 0.6}
        };
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);
        assertReconstruction(A, svd, 1e-10);
    }

    @Test
    public void testReconstructionRectangular() {
        // Verify A = U * S * V^T for a tall matrix (m > n)
        double[][] A = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 10},
                {2, 1, 4}
        };
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);
        assertReconstruction(A, svd, 1e-10);
    }

    @Test
    public void testOrthogonalV() {
        // V^T * V = I
        double[][] A = {{1, 2, 3}, {4, 5, 6}, {7, 8, 10}};
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);

        double[][] V = svd.getV();
        double[][] VtV = multiply(transpose(V), V);
        assertIdentity(VtV, 1e-12);
    }

    @Test
    public void testRankFullRank() {
        double[][] A = {{1, 0, 0}, {0, 2, 0}, {0, 0, 3}};
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);
        assertEquals(3, svd.rank());
    }

    @Test
    public void testRankDeficient() {
        // All rows are multiples of [1, 2, 3] — rank 1
        double[][] A = {{1, 2, 3}, {2, 4, 6}, {3, 6, 9}};
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);
        assertEquals(1, svd.rank());
    }

    @Test
    public void testSingularValuesNonNegativeDescending() {
        double[][] A = {
                {3.1, 0.5, 2.7, 1.2},
                {0.8, 4.2, 1.1, 3.6},
                {2.4, 1.7, 5.0, 0.3},
                {1.9, 3.3, 0.6, 2.8}
        };
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);

        double[] sv = svd.getSingularValues();
        for (int i = 0; i < sv.length; i++) {
            assertTrue(sv[i] >= 0, "Singular value negative at index " + i + ": " + sv[i]);
            if (i > 0)
                assertTrue(sv[i - 1] >= sv[i],
                        "Not descending: sv[" + (i - 1) + "]=" + sv[i - 1] + " < sv[" + i + "]=" + sv[i]);
        }
    }

    @Test
    public void testConditionNumber() {
        // cond = max(S) / min(S)
        double[][] A = {{3, 0, 0}, {0, 6, 0}, {0, 0, 2}};
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);
        assertEquals(6.0 / 2.0, svd.cond(), 1e-12);
    }

    @Test
    public void testNorm2() {
        // norm2 = max singular value
        double[][] A = {{3, 0, 0}, {0, 6, 0}, {0, 0, 2}};
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);
        assertEquals(6.0, svd.norm2(), 1e-12);
    }

    @Test
    public void test4x4RateMatrix() {
        double[] f = {0.25, 0.25, 0.25, 0.25};
        double[] r = {
                0.0, 1.0, 1.0,
                1.0, 0.5, 0.5,
                0.25, 0.75, 1.0,
                2.0, 0.25, 0.25
        };
        double[][] Q = buildNormalizedRateMatrix(f, r);
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(Q);
        assertReconstruction(Q, svd, 1e-10);
    }

    // ---- Helpers ----

    /**
     * Verify A = U * S * V^T.
     */
    private static void assertReconstruction(double[][] A, RobustSingularValueDecomposition svd, double tol) {
        double[][] U = svd.getU();
        double[][] S = svd.getS();
        double[][] V = svd.getV();
        double[][] reconstructed = multiply(multiply(U, S), transpose(V));

        for (int i = 0; i < A.length; i++)
            for (int j = 0; j < A[0].length; j++)
                assertEquals(A[i][j], reconstructed[i][j], tol,
                        "A != U*S*V^T at [" + i + "][" + j + "]");
    }

    private static void assertIdentity(double[][] M, double tol) {
        int n = M.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                assertEquals(i == j ? 1.0 : 0.0, M[i][j], tol,
                        "Not identity at [" + i + "][" + j + "]");
    }
}
