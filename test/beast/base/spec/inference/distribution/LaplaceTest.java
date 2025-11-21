package beast.base.spec.inference.distribution;


import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.Randomizer;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.beast.BEASTTestCase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/*
 * @author Louis du Plessis
 *         Date: 2018/08/06
 */
public class LaplaceTest {

    Laplace laplace;

    @BeforeEach 
    public void setUp() {
        laplace = new Laplace();
        laplace.initAndValidate();
        Randomizer.setSeed(123);
    }


    @Test
    public void testPdf() {

        System.out.println("Testing 10000 random pdf calls");


        for (int i = 0; i < 10000; i++) {
            double mu = Randomizer.nextDouble() * 10.0 - 5.0;
            double scale = Randomizer.nextDouble() * 10;

            double x = Randomizer.nextDouble() * 10;

            laplace.muInput.setValue(new RealScalarParam<>(mu, Real.INSTANCE), laplace);
            laplace.scaleInput.setValue(new RealScalarParam<>(scale, PositiveReal.INSTANCE), laplace);
            laplace.initAndValidate();

            double c = 1.0/(2*scale);
            double pdf = c*Math.exp(-Math.abs(x-mu)/scale);

            //System.out.println(x+"\t"+mu+"\t"+scale+"\t"+pdf+"\t"+laplace.density(x));

            assertEquals(pdf, laplace.density(x), BEASTTestCase.PRECISION);
        }


        /* Test with an example using R */
        laplace.muInput.setValue(new RealScalarParam<>(0, Real.INSTANCE), laplace);
        laplace.scaleInput.setValue(new RealScalarParam<>(2.14567, PositiveReal.INSTANCE), laplace);
        laplace.initAndValidate();
        assertEquals(0.07267657, laplace.density(2.5), BEASTTestCase.PRECISION);

        laplace.muInput.setValue(new RealScalarParam<>(5, Real.INSTANCE), laplace);
        laplace.scaleInput.setValue(new RealScalarParam<>(3, PositiveReal.INSTANCE), laplace);
        laplace.initAndValidate();
        assertEquals(0.06131324, laplace.density(2), BEASTTestCase.PRECISION);
    }

    @Test
    public void testMean() {

        for (int i = 0; i < 1000; i++) {
            double mu = Randomizer.nextDouble() * 10.0 - 5.0;

            laplace.muInput.setValue(new RealScalarParam<>(mu, Real.INSTANCE), laplace);
            laplace.initAndValidate();

            assertEquals(mu, laplace.getMean(), BEASTTestCase.PRECISION);
        }
    }

    @Test
    public void testMedian() {

        System.out.println("Testing 10000 random quantile(0.5) calls");

        for (int i = 0; i < 10000; i++) {
            double mu = Randomizer.nextDouble() * 10.0 - 5.0;
            double scale = Randomizer.nextDouble() * 10;

            laplace.muInput.setValue(new RealScalarParam<>(mu, Real.INSTANCE), laplace);
            laplace.scaleInput.setValue(new RealScalarParam<>(scale, PositiveReal.INSTANCE), laplace);
            laplace.initAndValidate();

            double median = mu;
            if (laplace.getApacheDistribution() instanceof ContinuousDistribution cd) {
                assertEquals(median, cd.inverseCumulativeProbability(0.5), BEASTTestCase.PRECISION);
            } else
                fail("Incorrect ContinuousDistribution : " + laplace.getApacheDistribution().toString());
        }
    }

    @Test
    public void testCDFAndQuantile() {

        System.out.println("Testing 10000 random quantile/cdf pairs");

        for (int i = 0; i < 10000; i++) {

            double mu = Randomizer.nextDouble() * 10.0 - 5.0;
            double scale = Randomizer.nextDouble() * 10;

            laplace.muInput.setValue(new RealScalarParam<>(mu, Real.INSTANCE), laplace);
            laplace.scaleInput.setValue(new RealScalarParam<>(scale, PositiveReal.INSTANCE), laplace);
            laplace.initAndValidate();

            double p = Randomizer.nextDouble();
            if (laplace.getApacheDistribution() instanceof ContinuousDistribution cd) {
                double quantile = cd.inverseCumulativeProbability(p);
                double cdf = cd.cumulativeProbability(quantile);
                assertEquals(p, cdf, BEASTTestCase.PRECISION);
            } else
                fail("Incorrect ContinuousDistribution : " + laplace.getApacheDistribution().toString());
        }

    }

    @Test
    public void testLogPdf() {

        System.out.println("Testing log pdf calls");

        /* Test with an example using R */
        laplace.muInput.setValue(new RealScalarParam<>(0, Real.INSTANCE), laplace);
        laplace.scaleInput.setValue(new RealScalarParam<>(2.14567, PositiveReal.INSTANCE), laplace);
        laplace.initAndValidate();
        assertEquals(-2.621736, laplace.logDensity(2.5), BEASTTestCase.PRECISION);

        laplace.muInput.setValue(new RealScalarParam<>(5, Real.INSTANCE), laplace);
        laplace.scaleInput.setValue(new RealScalarParam<>(3, PositiveReal.INSTANCE), laplace);
        laplace.initAndValidate();
        assertEquals(-2.791759, laplace.logDensity(2), BEASTTestCase.PRECISION);
    }

}
