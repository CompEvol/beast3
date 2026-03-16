package test.beast.evolution.substmodel;

import beast.base.evolution.substitutionmodel.ColtEigenSystem;
import beast.base.evolution.substitutionmodel.ComplexColtEigenSystem;
import beast.base.evolution.substitutionmodel.EigenDecomposition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct tests for ColtEigenSystem and ComplexColtEigenSystem.
 * Verifies eigendecomposition and matrix exponentiation against
 * known GTR transition probabilities computed externally (scilab).
 */
public class ColtEigenSystemTest {

    private static final double EPSILON = 1e-10;

    /**
     * Build a GTR rate matrix Q from rates and frequencies, normalized
     * so that the expected number of substitutions per unit time is 1.
     */
    private static double[][] buildGTRRateMatrix(double[] rates, double[] pi) {
        // rates: AC, AG, AT, CG, CT, GT (6 values)
        // pi: piA, piC, piG, piT
        int n = 4;
        double[][] Q = new double[n][n];

        // Off-diagonal: Q[i][j] = rate[i][j] * pi[j]
        // Rate matrix (symmetric part):
        //       A    C    G    T
        //  A    -   r0   r1   r2
        //  C   r0    -   r3   r4
        //  G   r1   r3    -   r5
        //  T   r2   r4   r5    -
        double[][] rateMatrix = {
            {0,        rates[0], rates[1], rates[2]},
            {rates[0], 0,        rates[3], rates[4]},
            {rates[1], rates[3], 0,        rates[5]},
            {rates[2], rates[4], rates[5], 0       }
        };

        double totalRate = 0;
        for (int i = 0; i < n; i++) {
            double rowSum = 0;
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    Q[i][j] = rateMatrix[i][j] * pi[j];
                    rowSum += Q[i][j];
                }
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

    @Test
    public void testGTREqualFrequencies() {
        // Equal frequencies, symmetric rates with ts/tv bias
        double[] pi = {0.25, 0.25, 0.25, 0.25};
        double[] rates = {0.5, 1.0, 0.5, 0.5, 1.0, 0.5};
        double distance = 0.1;

        // Expected from scilab: expm(Q * 0.1)
        double[] expected = {
            0.906563342722, 0.023790645491, 0.045855366296, 0.023790645491,
            0.023790645491, 0.906563342722, 0.023790645491, 0.045855366296,
            0.045855366296, 0.023790645491, 0.906563342722, 0.023790645491,
            0.023790645491, 0.045855366296, 0.023790645491, 0.906563342722
        };

        verifyEigenSystem(new ColtEigenSystem(4), pi, rates, distance, expected);
    }

    @Test
    public void testGTRUnequalFrequencies() {
        double[] pi = {0.50, 0.20, 0.2, 0.1};
        double[] rates = {0.5, 1.0, 0.5, 0.5, 1.0, 0.5};
        double distance = 0.1;

        double[] expected = {
            0.928287993055, 0.021032136637, 0.040163801989, 0.010516068319,
            0.052580341593, 0.906092679369, 0.021032136637, 0.020294842401,
            0.100409504972, 0.021032136637, 0.868042290072, 0.010516068319,
            0.052580341593, 0.040589684802, 0.021032136637, 0.885797836968
        };

        verifyEigenSystem(new ColtEigenSystem(4), pi, rates, distance, expected);
    }

    @Test
    public void testGTRAsymmetricRates() {
        double[] pi = {0.20, 0.30, 0.25, 0.25};
        double[] rates = {0.2, 10.0, 0.3, 0.4, 5.0, 0.5};
        double distance = 0.1;

        double[] expected = {
            0.8780963047046206, 0.0033252855682803723, 0.11461112844510626, 0.003967281281992822,
            0.002216857045520258, 0.9327483979953872, 0.005055665025823634, 0.05997907993326873,
            0.09168890275608481, 0.006066798030988321, 0.8959983003009074, 0.0062459989120190644,
            0.0031738250255942332, 0.07197489591992245, 0.006245998912019033, 0.9186052801424642
        };

        verifyEigenSystem(new ColtEigenSystem(4), pi, rates, distance, expected);
    }

    @Test
    public void testTransitionProbabilitiesAreValid() {
        // Any GTR parameterization should produce valid P matrix:
        // all entries >= 0, rows sum to 1
        double[] pi = {0.1, 0.4, 0.3, 0.2};
        double[] rates = {1.0, 5.0, 2.0, 0.5, 3.0, 1.0};

        ColtEigenSystem eigenSystem = new ColtEigenSystem(4);
        double[][] Q = buildGTRRateMatrix(rates, pi);
        EigenDecomposition decomp = eigenSystem.decomposeMatrix(Q);
        assertNotNull(decomp);

        for (double distance : new double[]{0.001, 0.01, 0.1, 1.0, 10.0}) {
            double[] P = new double[16];
            eigenSystem.computeExponential(decomp, distance, P);

            for (int i = 0; i < 4; i++) {
                double rowSum = 0;
                for (int j = 0; j < 4; j++) {
                    double pij = P[i * 4 + j];
                    assertTrue(pij >= 0, "P[" + i + "][" + j + "] = " + pij + " < 0 at distance " + distance);
                    rowSum += pij;
                }
                assertEquals(1.0, rowSum, 1e-8, "Row " + i + " does not sum to 1 at distance " + distance);
            }
        }
    }

    @Test
    public void testComplexEigenSystemValidProbabilities() {
        // ComplexColtEigenSystem handles non-reversible models
        // Use a non-symmetric rate matrix
        double[][] Q = {
            {-1.0,  0.3,  0.5,  0.2},
            { 0.4, -1.2,  0.3,  0.5},
            { 0.1,  0.6, -1.0,  0.3},
            { 0.5,  0.2,  0.4, -1.1}
        };

        ComplexColtEigenSystem eigenSystem = new ComplexColtEigenSystem(4);
        EigenDecomposition decomp = eigenSystem.decomposeMatrix(Q);
        assertNotNull(decomp);

        for (double distance : new double[]{0.01, 0.1, 1.0}) {
            double[] P = new double[16];
            eigenSystem.computeExponential(decomp, distance, P);

            for (int i = 0; i < 4; i++) {
                double rowSum = 0;
                for (int j = 0; j < 4; j++) {
                    double pij = P[i * 4 + j];
                    assertTrue(pij >= 0, "P[" + i + "][" + j + "] = " + pij + " < 0 at distance " + distance);
                    rowSum += pij;
                }
                assertEquals(1.0, rowSum, 1e-8, "Row " + i + " does not sum to 1 at distance " + distance);
            }
        }
    }

    private void verifyEigenSystem(ColtEigenSystem eigenSystem, double[] pi, double[] rates,
                                    double distance, double[] expected) {
        double[][] Q = buildGTRRateMatrix(rates, pi);
        EigenDecomposition decomp = eigenSystem.decomposeMatrix(Q);
        assertNotNull(decomp);

        double[] P = new double[16];
        eigenSystem.computeExponential(decomp, distance, P);

        for (int k = 0; k < P.length; k++) {
            assertEquals(expected[k], P[k], EPSILON,
                "Mismatch at position " + k + ": expected " + expected[k] + " got " + P[k]);
        }
    }
}
