package beast.base.math.matrixalgebra;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for RobustEigenDecomposition.
 * Verifies eigenvalues and eigenvectors against known matrices and
 * rate matrices from transprob.R ground truth.
 *
 * The fundamental property verified is A * V = V * D, where V is the
 * eigenvector matrix and D is the block diagonal eigenvalue matrix.
 */
public class RobustEigenDecompositionTest {

    // ---- Symmetric matrices with known eigenvalues ----

    @Test
    public void testSymmetric2x2() {
        // [[3, 1], [1, 3]] has eigenvalues 2, 4
        double[][] A = {{3, 1}, {1, 3}};
        RobustEigenDecomposition eig = new RobustEigenDecomposition(A);

        double[] lambda = sorted(eig.getRealEigenvalues());
        assertEquals(2.0, lambda[0], 1e-12);
        assertEquals(4.0, lambda[1], 1e-12);
        assertAllZero(eig.getImagEigenvalues(), 1e-12);
        assertEigenProperty(A, eig, 1e-12);
    }

    @Test
    public void testSymmetric3x3Tridiagonal() {
        // Eigenvalues: 2 - sqrt(2), 2, 2 + sqrt(2)
        double[][] A = {{2, -1, 0}, {-1, 2, -1}, {0, -1, 2}};
        RobustEigenDecomposition eig = new RobustEigenDecomposition(A);

        double[] lambda = sorted(eig.getRealEigenvalues());
        assertEquals(2 - Math.sqrt(2), lambda[0], 1e-12);
        assertEquals(2.0, lambda[1], 1e-12);
        assertEquals(2 + Math.sqrt(2), lambda[2], 1e-12);
        assertAllZero(eig.getImagEigenvalues(), 1e-12);
        assertEigenProperty(A, eig, 1e-12);
    }

    @Test
    public void testSymmetricOrthogonalEigenvectors() {
        // For symmetric matrices, V should be orthogonal: V^T * V = I
        double[][] A = {{4, 1, 2}, {1, 3, 0}, {2, 0, 5}};
        RobustEigenDecomposition eig = new RobustEigenDecomposition(A);

        double[][] V = eig.getV();
        double[][] VtV = multiply(transpose(V), V);
        assertIdentity(VtV, 1e-12);
        assertEigenProperty(A, eig, 1e-12);
    }

    // ---- Non-symmetric rate matrices from transprob.R ----

    @Test
    public void testNonSymmetric4x4RateMatrix() {
        // transprob.R test 1: 4-state equal frequencies, non-reversible rates
        double[] f = {0.25, 0.25, 0.25, 0.25};
        double[] r = {
                0.0, 1.0, 1.0,
                1.0, 0.5, 0.5,
                0.25, 0.75, 1.0,
                2.0, 0.25, 0.25
        };
        double[][] Q = buildNormalizedRateMatrix(f, r);
        RobustEigenDecomposition eig = new RobustEigenDecomposition(Q);
        assertEigenProperty(Q, eig, 1e-10);
    }

    @Test
    public void testExtremeRates4x4() {
        // transprob.R test 2: extreme rates (1E6, 1E-6)
        double[] f = {0.25, 0.25, 0.25, 0.25};
        double[] r = {
                0.0, 1E6, 1E-6,
                0.0, 1E-6, 0.0,
                1E6, 1E-6, 1E-6,
                1E6, 0.0, 0.0
        };
        double[][] Q = buildNormalizedRateMatrix(f, r);
        RobustEigenDecomposition eig = new RobustEigenDecomposition(Q);
        assertEigenProperty(Q, eig, 1e-6);
    }

    @Test
    public void testNonSymmetric20x20RateMatrix() {
        // transprob.R test 3: 20-state amino acid scale, equal frequencies
        double[] f = new double[20];
        Arrays.fill(f, 0.05);
        double[] r = {
                // row 1 (i=0): j=1..19
                0.024839, 0.021702, 0.057358, 0.009841, 0.153331, 0.008822, 0.011772, 0.022558, 0.036913, 0.021756,
                0.008938, 0.044982, 0.032825, 0.025296, 0.293716, 0.137408, 0.148024, 0.002333, 0.007072,
                // row 2 (i=1): j=0, j=2..19
                0.232486, 0.004676, 0.000546, 0.056320, 0.054099, 0.018577, 0.035820, 0.001309, 0.070488, 0.022833,
                0.026613, 0.004951, 0.004423, 0.025881, 0.269618, 0.088302, 0.127071, 0.012551, 0.055425,
                // row 3 (i=2): j=0..1, j=3..19
                0.044710, 0.001005, 0.326706, 0.001080, 0.052222, 0.021840, 0.000896, 0.020318, 0.002286, 0.000989,
                0.191135, 0.022948, 0.027123, 0.009358, 0.077216, 0.027947, 0.004353, 0.000420, 0.004721,
                // row 4 (i=3): j=0..2, j=4..19
                0.118220, 0.000059, 0.270615, 0.000582, 0.021725, 0.007838, 0.003626, 0.102520, 0.007130, 0.003952,
                0.021301, 0.016134, 0.155617, 0.021300, 0.038580, 0.037116, 0.021735, 0.000479, 0.002812,
                // row 5 (i=4): j=0..3, j=5..19
                0.026441, 0.018883, 0.001150, 0.001353, 0.008862, 0.012430, 0.070699, 0.001389, 0.252441, 0.044106,
                0.003831, 0.005741, 0.001947, 0.002412, 0.021129, 0.011973, 0.055028, 0.032868, 0.235871,
                // row 6 (i=5): j=0..4, j=6..19
                0.127508, 0.006487, 0.071880, 0.037516, 0.004095, 0.012548, 0.001225, 0.028883, 0.005257, 0.003396,
                0.072902, 0.016073, 0.018332, 0.028838, 0.099294, 0.012391, 0.006240, 0.002352, 0.002807,
                // row 7 (i=6): j=0..5, j=7..19
                0.039332, 0.015673, 0.047205, 0.035568, 0.039262, 0.017984, 0.008240, 0.042932, 0.048364, 0.010573,
                0.188167, 0.025292, 0.204985, 0.143719, 0.068742, 0.032930, 0.016729, 0.009791, 0.191292,
                // row 8 (i=7): j=0..6, j=8..19
                0.005603, 0.004834, 0.000261, 0.000678, 0.059498, 0.000877, 0.001160, 0.005215, 0.436284, 0.096978,
                0.005410, 0.002106, 0.001036, 0.002475, 0.003338, 0.041873, 0.793911, 0.001628, 0.012843,
                // row 9 (i=8): j=0..7, j=9..19
                0.048771, 0.000546, 0.014790, 0.102560, 0.000983, 0.013086, 0.016882, 0.013869, 0.015160, 0.016086,
                0.082824, 0.012811, 0.131025, 0.307891, 0.052139, 0.062271, 0.017249, 0.000324, 0.004069,
                // row 10 (i=9): j=0..8, j=10..19
                0.037235, 0.009588, 0.000786, 0.003274, 0.109896, 0.005132, 0.007518, 0.237423, 0.005256, 0.140795,
                0.002342, 0.011801, 0.017910, 0.013407, 0.009778, 0.015587, 0.134209, 0.009406, 0.012618,
                // row 11 (i=10): j=0..9, j=11..19
                0.107191, 0.015205, 0.002928, 0.021256, 0.071490, 0.013208, 0.011163, 0.281442, 0.053575, 0.627472,
                0.021210, 0.006398, 0.080762, 0.039819, 0.037128, 0.132351, 0.148243, 0.012371, 0.028934,
                // row 12 (i=11): j=0..10, j=12..19
                0.028963, 0.006533, 0.239745, 0.045019, 0.004546, 0.071780, 0.092101, 0.012715, 0.140837, 0.007891,
                0.007312, 0.007416, 0.074468, 0.038588, 0.251906, 0.098966, 0.008540, 0.000576, 0.018855,
                // row 13 (i=12): j=0..11, j=13..19
                0.125761, 0.001238, 0.028954, 0.043969, 0.003864, 0.016844, 0.013177, 0.006316, 0.032264, 0.027607,
                0.002930, 0.009735, 0.032387, 0.024990, 0.090102, 0.035664, 0.022493, 0.001447, 0.003449,
                // row 14 (i=13): j=0..12, j=14..19
                0.105642, 0.002059, 0.031175, 0.287997, 0.001766, 0.016920, 0.101499, 0.007944, 0.184128, 0.074342,
                0.031984, 0.063599, 0.027689, 0.159619, 0.073855, 0.064808, 0.023335, 0.002524, 0.008306,
                // row 15 (i=14): j=0..13, j=15..19
                0.049545, 0.011113, 0.007193, 0.023148, 0.002563, 0.024400, 0.063797, 0.008438, 0.286621, 0.037014,
                0.011105, 0.023926, 0.013835, 0.115653, 0.047041, 0.033298, 0.016122, 0.007866, 0.009876,
                // row 16 (i=15): j=0..14, j=16..19
                0.279843, 0.025942, 0.077126, 0.044293, 0.014468, 0.123647, 0.023280, 0.004996, 0.044453, 0.021951,
                0.007648, 0.149098, 0.079233, 0.052127, 0.043941, 0.303425, 0.007812, 0.003200, 0.013761,
                // row 17 (i=16): j=0..15, j=17..19
                0.152268, 0.012648, 0.028807, 0.057316, 0.006905, 0.013361, 0.015207, 0.071103, 0.075085, 0.031373,
                0.040898, 0.088614, 0.025966, 0.046415, 0.043048, 0.392905, 0.137154, 0.001680, 0.010344,
                // row 18 (i=17): j=0..16, j=18..19
                0.269526, 0.029277, 0.002328, 0.017800, 0.029561, 0.010870, 0.004927, 0.613175, 0.010642, 0.193074,
                0.050350, 0.004876, 0.017336, 0.009091, 0.009273, 0.009078, 0.139110, 0.002774, 0.004810,
                // row 19 (i=18): j=0..17, j=19
                0.019550, 0.010755, 0.002233, 0.004350, 0.088395, 0.015920, 0.011099, 0.009252, 0.004386, 0.057245,
                0.013427, 0.002448, 0.008117, 0.008078, 0.027421, 0.015532, 0.007637, 0.013981, 0.084264,
                // row 20 (i=19): j=0..18
                0.016996, 0.020894, 0.007098, 0.007687, 0.333270, 0.003692, 0.113502, 0.013404, 0.008829, 0.030010,
                0.009942, 0.023276, 0.003999, 0.010515, 0.013447, 0.022690, 0.012800, 0.019909, 0.038235
        };
        double[][] Q = buildNormalizedRateMatrix(f, r);
        RobustEigenDecomposition eig = new RobustEigenDecomposition(Q);
        assertEigenProperty(Q, eig, 1e-8);
    }

    // ---- Helpers ----

    private static double[] sorted(double[] values) {
        double[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
    }

    /**
     * Verify A * V = V * D (the fundamental eigendecomposition property).
     */
    private static void assertEigenProperty(double[][] A, RobustEigenDecomposition eig, double tol) {
        double[][] V = eig.getV();
        double[][] D = eig.getD();
        double[][] AV = multiply(A, V);
        double[][] VD = multiply(V, D);

        int n = A.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                assertEquals(AV[i][j], VD[i][j], tol,
                        "A*V != V*D at [" + i + "][" + j + "]");
    }

    private static void assertAllZero(double[] values, double tol) {
        for (int i = 0; i < values.length; i++)
            assertEquals(0.0, values[i], tol, "Expected zero imaginary part at index " + i);
    }

    private static void assertIdentity(double[][] M, double tol) {
        int n = M.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                assertEquals(i == j ? 1.0 : 0.0, M[i][j], tol,
                        "Not identity at [" + i + "][" + j + "]");
    }

    static double[][] multiply(double[][] A, double[][] B) {
        int m = A.length, n = B[0].length, p = B.length;
        double[][] C = new double[m][n];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                for (int k = 0; k < p; k++)
                    C[i][j] += A[i][k] * B[k][j];
        return C;
    }

    static double[][] transpose(double[][] A) {
        int m = A.length, n = A[0].length;
        double[][] T = new double[n][m];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                T[j][i] = A[i][j];
        return T;
    }

    /**
     * Build a normalized rate matrix following transprob.R conventions.
     * Rates are ordered: for each row i, first columns j &lt; i, then j &gt; i.
     */
    static double[][] buildNormalizedRateMatrix(double[] f, double[] r) {
        int n = f.length;
        double[][] Q = new double[n][n];
        int x = 0;
        double subst = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                Q[i][j] = r[x++] * f[j];
                Q[i][i] -= Q[i][j];
            }
            for (int j = i + 1; j < n; j++) {
                Q[i][j] = r[x++] * f[j];
                Q[i][i] -= Q[i][j];
            }
            subst -= Q[i][i] * f[i];
        }
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                Q[i][j] /= subst;
        return Q;
    }
}
