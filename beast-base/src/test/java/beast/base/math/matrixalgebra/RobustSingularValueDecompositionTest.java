package beast.base.math.matrixalgebra;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for RobustSingularValueDecomposition.
 * Verifies singular values, reconstruction (A = U * S * V^T),
 * orthogonality of U and V, rank, and ordering properties.
 */
public class RobustSingularValueDecompositionTest {

    private static final Algebra ALG = new Algebra();

    @Test
    public void testDiagonal3x3() {
        // Singular values of a diagonal matrix are the absolute values of diagonal entries
        double[][] A = {{3, 0, 0}, {0, -5, 0}, {0, 0, 2}};
        RobustSingularValueDecomposition svd = decompose(A);

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
        RobustSingularValueDecomposition svd = decompose(A);

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
        RobustSingularValueDecomposition svd = decompose(A);
        assertReconstruction(DoubleFactory2D.dense.make(A), svd, 1e-10);
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
        RobustSingularValueDecomposition svd = decompose(A);
        assertReconstruction(DoubleFactory2D.dense.make(A), svd, 1e-10);
    }

    @Test
    public void testOrthogonalV() {
        // V^T * V = I
        double[][] A = {{1, 2, 3}, {4, 5, 6}, {7, 8, 10}};
        RobustSingularValueDecomposition svd = decompose(A);

        DoubleMatrix2D V = svd.getV();
        DoubleMatrix2D VtV = ALG.mult(ALG.transpose(V), V);
        assertIdentity(VtV, 1e-12);
    }

    @Test
    public void testRankFullRank() {
        double[][] A = {{1, 0, 0}, {0, 2, 0}, {0, 0, 3}};
        RobustSingularValueDecomposition svd = decompose(A);
        assertEquals(3, svd.rank());
    }

    @Test
    public void testRankDeficient() {
        // All rows are multiples of [1, 2, 3] — rank 1
        double[][] A = {{1, 2, 3}, {2, 4, 6}, {3, 6, 9}};
        RobustSingularValueDecomposition svd = decompose(A);
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
        RobustSingularValueDecomposition svd = decompose(A);

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
        RobustSingularValueDecomposition svd = decompose(A);
        assertEquals(6.0 / 2.0, svd.cond(), 1e-12);
    }

    @Test
    public void testNorm2() {
        // norm2 = max singular value
        double[][] A = {{3, 0, 0}, {0, 6, 0}, {0, 0, 2}};
        RobustSingularValueDecomposition svd = decompose(A);
        assertEquals(6.0, svd.norm2(), 1e-12);
    }

    @Test
    public void test4x4RateMatrix() {
        // Use the same rate matrix as RobustEigenDecompositionTest
        double[] f = {0.25, 0.25, 0.25, 0.25};
        double[] r = {
                0.0, 1.0, 1.0,
                1.0, 0.5, 0.5,
                0.25, 0.75, 1.0,
                2.0, 0.25, 0.25
        };
        double[][] Q = RobustEigenDecompositionTest.buildNormalizedRateMatrix(f, r);
        DoubleMatrix2D A = DoubleFactory2D.dense.make(Q);
        RobustSingularValueDecomposition svd = new RobustSingularValueDecomposition(A);
        assertReconstruction(A, svd, 1e-10);
    }

    // ---- Helpers ----

    private static RobustSingularValueDecomposition decompose(double[][] data) {
        return new RobustSingularValueDecomposition(DoubleFactory2D.dense.make(data));
    }

    /**
     * Verify A = U * S * V^T.
     */
    private static void assertReconstruction(DoubleMatrix2D A, RobustSingularValueDecomposition svd, double tol) {
        DoubleMatrix2D U = svd.getU();
        DoubleMatrix2D S = svd.getS();
        DoubleMatrix2D V = svd.getV();
        DoubleMatrix2D US = ALG.mult(U, S);
        DoubleMatrix2D reconstructed = ALG.mult(US, ALG.transpose(V));

        for (int i = 0; i < A.rows(); i++)
            for (int j = 0; j < A.columns(); j++)
                assertEquals(A.get(i, j), reconstructed.get(i, j), tol,
                        "A != U*S*V^T at [" + i + "][" + j + "]");
    }

    private static void assertIdentity(DoubleMatrix2D M, double tol) {
        int n = M.rows();
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                assertEquals(i == j ? 1.0 : 0.0, M.get(i, j), tol,
                        "Not identity at [" + i + "][" + j + "]");
    }
}
