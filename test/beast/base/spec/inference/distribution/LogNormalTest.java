package beast.base.spec.inference.distribution;

import beast.base.parser.XMLParser;
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

public class LogNormalTest {

    // with offset

    @Test
    public void testCalcLogP() {
        LogNormal logNormal = new LogNormal();
        logNormal.hasMeanInRealSpaceInput.setValue("true", logNormal);
        logNormal.offsetInput.setValue("1200", logNormal);
        logNormal.MParameterInput.setValue(new RealScalarParam<>(2000, Real.INSTANCE), logNormal);
        logNormal.SParameterInput.setValue(new RealScalarParam<>(0.6, PositiveReal.INSTANCE), logNormal);
        logNormal.initAndValidate();

        double f0 = logNormal.calcLogP(2952.6747000000014);
        assertEquals(-7.880210654973873, f0, 1e-10);

        logNormal.paramInput.setValue(new RealScalarParam<>(2952.6747000000014, PositiveReal.INSTANCE), logNormal);
        logNormal.initAndValidate();
        assertEquals(-7.880210654973873, logNormal.calculateLogP(), 1e-10);
    }

    @Test
    public void testCalcLogP2() throws Exception {
        // does the same as testCalcLogP(), but with by constructing object through XML
        final String xml = "<distribution spec='beast.base.spec.inference.distribution.LogNormal' " +
                "offset='1200' meanInRealSpace='true'>\n" +
                "  <M spec='beast.base.spec.inference.parameter.RealScalarParam' " +
                "     domain='Real' value='2000'/>\n" +
                "  <S spec='beast.base.spec.inference.parameter.RealScalarParam' " +
                "    domain='PositiveReal' value='0.6'/>\n" +
                "</distribution>\n";
        XMLParser parser = new XMLParser();
        LogNormal logNormal = (LogNormal) parser.parseBareFragment(xml, true);

        double f0 = logNormal.calcLogP(2952.6747000000014);
        assertEquals(-7.880210654973873, f0, 1e-10);
    }

    @Test
    public void testCalcLogP3() {
        // does the same as testCalcLogP(), but with by constructing object through init
        LogNormal logNormal = new LogNormal();
        logNormal.initByName("M", new RealScalarParam<>(2000, Real.INSTANCE),
                "S", new RealScalarParam<>(0.6, PositiveReal.INSTANCE),
                "meanInRealSpace", true, "offset", "1200");

        double f0 = logNormal.calcLogP(2952.6747000000014);
        assertEquals(-7.880210654973873, f0, 1e-10);
    }


    // remainder is adapted from Alexei's LogNormalDistributionTest from BEAST 1
    LogNormal logNormal;

    @BeforeEach
    public void setUp() {

        logNormal = new LogNormal();
        logNormal.initByName("M", new RealScalarParam<>(1.0, Real.INSTANCE),
                "S", new RealScalarParam<>(2.0, PositiveReal.INSTANCE));
        Randomizer.setSeed(123);
    }

    @Test
    public void testPDF() {
        System.out.println("Testing 10000 random pdf calls");

        for (int i = 0; i < 10000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            double x = -1;
            while( x < 0 ) {
                x = Math.log(Randomizer.nextDouble() * 10);
            }

            logNormal.MParameterInput.setValue(new RealScalarParam<>(M, Real.INSTANCE), logNormal);
            logNormal.SParameterInput.setValue(new RealScalarParam<>(S, PositiveReal.INSTANCE), logNormal);
            logNormal.initAndValidate();

            double pdf = 1.0 / (x * S * Math.sqrt(2 * Math.PI)) * Math.exp(-Math.pow(Math.log(x) - M, 2) / (2 * S * S));

            System.out.println("Testing logNormal[M=" + M + " S=" + S + "].pdf(" + x + ")");
            double f = logNormal.density(x);

            assertEquals(pdf, f, 1e-10);
        }
    }

//    @Test
//    public void testPdf() {
//
//        System.out.println("Testing 10000 random pdf calls");
//
//        for (int i = 0; i < 10000; i++) {
//            double M = Randomizer.nextDouble() * 10.0 - 5.0;
//            double S = Randomizer.nextDouble() * 10;
//
//            double x = Math.log(Randomizer.nextDouble() * 10);
//
//            logNormal.MParameterInput.setValue(new RealScalarParam<>(M, Real.INSTANCE), logNormal);
//            logNormal.SParameterInput.setValue(new RealScalarParam<>(S, PositiveReal.INSTANCE), logNormal);
//            logNormal.initAndValidate();
//
//            double pdf = 1.0 / (x * S * Math.sqrt(2 * Math.PI)) * Math.exp(-Math.pow(Math.log(x) - M, 2) / (2 * S * S));
//            if (x <= 0) pdf = 0; // see logNormal.pdf(x)
//
//            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].pdf(" + x + ")");
//
//            assertEquals(pdf, logNormal.density(x), 1e-10);
//        }
//    }

    @Test
    public void testMean() {

        for (int i = 0; i < 1000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            logNormal.MParameterInput.setValue(new RealScalarParam<>(M, Real.INSTANCE), logNormal);
            logNormal.SParameterInput.setValue(new RealScalarParam<>(S, PositiveReal.INSTANCE), logNormal);
            logNormal.initAndValidate();
            
            double mean = Math.exp(M + S * S / 2);

            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].mean()");

            assertEquals(mean, logNormal.getMean(), 1e-10);
        }
    }

//    public void testVariance() {
//
//        for (int i = 0; i < 1000; i++) {
//            double M = Randomizer.nextDouble() * 10.0 - 5.0;
//            double S = Randomizer.nextDouble() * 10;
//
//            logNormal.MParameterInput.setValue(M, logNormal);
//            logNormal.SParameterInput.setValue(S, logNormal);
//            logNormal.initAndValidate();
//
//            double variance = (Math.exp(S * S) - 1) * Math.exp(2 * M + S * S);
//
//            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].variance()");
//
//            assertEquals(variance, logNormal.getVariance(), 1e-10);
//        }
//    }

    @Test
    public void testMedian() throws MathException {

        System.out.println("Testing 10000 random quantile(0.5) calls");

        for (int i = 0; i < 10000; i++) {
            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            logNormal.MParameterInput.setValue(new RealScalarParam<>(M, Real.INSTANCE), logNormal);
            logNormal.SParameterInput.setValue(new RealScalarParam<>(S, PositiveReal.INSTANCE), logNormal);
            logNormal.initAndValidate();

            double median = Math.exp(M);

            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].median()");

            assertEquals(median, logNormal.inverseCumulativeProbability(0.5), median / 1e6);
        }
    }
    @Test
    public void testCDFAndQuantile() throws MathException {

        System.out.println("Testing 10000 random quantile/cdf pairs");

        for (int i = 0; i < 10000; i++) {

            double M = Randomizer.nextDouble() * 10.0 - 5.0;
            double S = Randomizer.nextDouble() * 10;

            logNormal.MParameterInput.setValue(new RealScalarParam<>(M, Real.INSTANCE), logNormal);
            logNormal.SParameterInput.setValue(new RealScalarParam<>(S, PositiveReal.INSTANCE), logNormal);
            logNormal.initAndValidate();

            double p = Randomizer.nextDouble();
            double quantile = logNormal.inverseCumulativeProbability(p);

            if (logNormal.getApacheDistribution() instanceof ContinuousDistribution cd) {
                double cdf = cd.cumulativeProbability(quantile);
                assertEquals(p, cdf, 1e-7);
            } else
                fail("Incorrect ContinuousDistribution : " + logNormal.getApacheDistribution().toString());

        }
    }

//    public void testCDFAndQuantile2() {
//
//        final LogNormal f = new LogNormal();
//        logNormal.initByName("M", "1.0", "S", "1.0");
//        for (double i = 0.01; i < 0.95; i += 0.01) {
//            final double y = i;
//
//            BisectionZeroFinder zeroFinder = new BisectionZeroFinder(new OneVariableFunction() {
//                public double value(double x) {
//                    return f.cdf(x) - y;
//                }
//            }, 0.01, 100);
//            zeroFinder.evaluate();
//
//            assertEquals(f.quantile(i), zeroFinder.getResult(), 1e-6);
//        }
//    }

}

