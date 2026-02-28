package beast.base.spec.inference.distribution;

import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import beast.base.spec.type.RealVector;
import beast.base.spec.type.Simplex;
import org.apache.commons.numbers.gamma.LogGamma;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class DirichletTest {
    @Test
    void normalisedTest() {
        // Valid Dirichlet vector: sum to 1
        double[] x = new double[]{0.2, 0.2, 0.2, 0.2, 0.2};
        Simplex simplexParam = new SimplexParam(x);

        double[] a = new double[]{1.0, 1.0, 1.0, 1.0, 1.0};
        RealVector<PositiveReal> alpha = new RealVectorParam<>(a, PositiveReal.INSTANCE);
//        d.initByName("alpha", alpha, "param", simplexParam);

        Dirichlet d = new Dirichlet(simplexParam, alpha);

        int n = a.length;
        double f0 = d.calculateLogP();

        // Compute expected log density
        double sumAlpha = 0.0;
        for (int i = 0; i < n; i++) {
            sumAlpha += a[i];
        }

        double logGammaSumAlpha = LogGamma.value(sumAlpha);

        double sumLogGammaAlpha = 0.0;
        for (int i = 0; i < n; i++) {
            sumLogGammaAlpha += LogGamma.value(a[i]);
        }

        double sumLogX = 0.0;
        for (int i = 0; i < n; i++) {
            sumLogX += (a[i] - 1.0) * Math.log(x[i]);
        }

        double exp = logGammaSumAlpha - sumLogGammaAlpha + sumLogX;

        assertEquals(exp, f0, 1e-6);
    }


    @Test
    void validationTest() {
        // fail to set sum
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> {
                    // Define x whose sum is sumX (not 1)
                    double[] x = new double[]{1.0, 1.0, 1.0, 1.0, 1.0}; // sum = 5 (sumX=5)
                    Simplex simplexParam = new SimplexParam(x);
                }
        );
        // assert message
        assertTrue(exception.getMessage().contains("not valid for domain beast.base.spec.domain.UnitInterval"));

        double[] x = new double[]{0.25, 0.25, 0.25, 0.25};
        Simplex param = new SimplexParam(x);
        double[] a = new double[]{1.0, 1.0, 1.0, 1.0, 1.0};
        RealVector<PositiveReal> alpha = new RealVectorParam<>(a, PositiveReal.INSTANCE);
        Dirichlet d = new Dirichlet();

        exception = assertThrows(
                RuntimeException.class,
                () -> {
                    d.initByName("param", param, "alpha", alpha);
                }
        );
        // assert message
        assertTrue(exception.getMessage().contains("Dimensions of alpha and param should be the same"));

    }


    // Dirichlet must sum to 1
//    @Test
//    void notNormalisedTest() {
//        Dirichlet d = new Dirichlet();
//
//        Double[] alpha = new Double[]{1.0, 1.0, 1.0, 1.0, 1.0};
//        RealParameter a = new RealParameter(alpha);
//        d.alphaInput.setValue(a, d );
//        d.sumInput.setValue(5.0, d );
//        d.initAndValidate();
//
//        int n = alpha.length;
//
//        // Define x whose sum is sumX (not 1)
//        Double[] x = new Double[]{1.0, 1.0, 1.0, 1.0, 1.0}; // sum = 5 (sumX=5)
//        RealParameter p = new RealParameter(x);
//        double f0 = d.calcLogP(p);
//
//        // Compute sumX = sum(x)
//        double sumX = 0.0;
//        for (int i = 0; i < n; i++) {
//            sumX += x[i];
//        }
//
//        // Compute standard log density for x_normalised
//        double sumAlpha = 0.0;
//        for (int i = 0; i < n; i++) {
//            sumAlpha += alpha[i];
//        }
//
//        double logGammaSumAlpha = LogGamma.value(sumAlpha);
//
//        double sumLogGammaAlpha = 0.0;
//        for (int i = 0; i < n; i++) {
//            sumLogGammaAlpha += LogGamma.value(alpha[i]);
//        }
//
//        // Normalised x (so xi / sumX)
//        double sumLogX = 0.0;
//        for (int i = 0; i < n; i++) {
//            sumLogX += (alpha[i] - 1.0) * Math.log(x[i] / sumX);
//        }
//
//        double log_density_standard = logGammaSumAlpha - sumLogGammaAlpha + sumLogX;
//
//        // Apply Jacobian correction: -(n-1) * log(sumX)
//        double log_density_scaled = log_density_standard - (n - 1) * Math.log(sumX);
//
//        assertEquals(log_density_scaled, f0, 1e-6);
//    }

}
