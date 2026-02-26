package beast.base.spec.inference.distribution;

import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.Randomizer;
import org.apache.commons.math.MathException;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * adapted from BEAST 1
 * @author Wai Lok Sibon Li
 * 
 */
public class NormalTest {
    Normal norm;

    @BeforeEach
    public void setUp() {
        norm = new Normal();
        norm.initAndValidate();
        Randomizer.setSeed(123);
    }

    @Test
    void testPdf() {

        System.out.println("Testing 10000 random pdf calls");

        for (int i = 0; i < 10000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            double x = Randomizer.nextDouble() * 10;

            norm.initByName("mean", new RealScalarParam<>(M, Real.INSTANCE),
                    "sigma", new RealScalarParam<>(S, PositiveReal.INSTANCE));
            
            double a = 1.0 / (Math.sqrt(2.0 * Math.PI) * S);
            double b = -(x - M) * (x - M) / (2.0 * S * S);
            double pdf =  a * Math.exp(b);

            assertEquals(pdf, norm.density(x), 1e-10);
        }

        /* Test with an example using R */
        norm.initByName("mean", new RealScalarParam<>(2.835202292812448, Real.INSTANCE),
                "sigma", new RealScalarParam<>(3.539139491639669, PositiveReal.INSTANCE));
        assertEquals(0.1123318, norm.density(2.540111), 1e-6);
    }

    @Test
    void testMean() {

        for (int i = 0; i < 1000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;

            norm.meanInput.setValue(new RealScalarParam<>(M, Real.INSTANCE), norm);
            norm.initAndValidate();

            assertEquals(M, norm.getMean(), 1e-10);
        }
    }

//    public void testVariance() {
//
//        for (int i = 0; i < 1000; i++) {
//            double S = Randomizer.nextDouble() * 10;
//            norm.sigmaInput.setValue(S + "", norm);
//            norm.initAndValidate();
//
//            double variance = S * S;
//
//            assertEquals(variance, norm.variance(), 1e-10);
//        }
//    }


    @Test
    void testMedian() throws MathException {

        System.out.println("Testing 10000 random quantile(0.5) calls");

        for (int i = 0; i < 10000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            norm.meanInput.setValue(new RealScalarParam<>(M, Real.INSTANCE), norm);
            norm.sdInput.setValue(new RealScalarParam<>(S, PositiveReal.INSTANCE), norm);
            norm.initAndValidate();

            double median = M;

            assertEquals(median, norm.inverseCumulativeProbability(0.5), 1e-6);
        }
    }

    @Test
    void testCDFAndQuantile() throws MathException {

        System.out.println("Testing 10000 random quantile/cdf pairs");

        for (int i = 0; i < 10000; i++) {

            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            norm.meanInput.setValue(new RealScalarParam<>(M, Real.INSTANCE), norm);
            norm.sdInput.setValue(new RealScalarParam<>(S, PositiveReal.INSTANCE), norm);
            norm.initAndValidate();

            double p = Randomizer.nextDouble();
            double quantile = norm.inverseCumulativeProbability(p);
            if (norm.getApacheDistribution() instanceof ContinuousDistribution cd) {
                double cdf = cd.cumulativeProbability(quantile);
                assertEquals(p, cdf, 1e-7);
            } else
                fail("Incorrect ContinuousDistribution : " + norm.getApacheDistribution().toString());
        }

    }

//    public void testCDFAndQuantile2() {
//        for(int i=0; i<10000; i++) {
//            double x =Randomizer.nextDouble();
//            double m = Randomizer.nextDouble() * 10;
//            double s = Randomizer.nextDouble() * 10;
//            
//            double a = NormalDistribution.cdf(x, m, s, false);
//            double b =NormalDistribution.cdf(x, m, s);
//            
//            assertEquals(a, b, 1.0e-8);
//        }
//    }


}
