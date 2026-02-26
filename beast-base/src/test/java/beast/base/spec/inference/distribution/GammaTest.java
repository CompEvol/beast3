package beast.base.spec.inference.distribution;


import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.util.GammaFunction;
import beast.base.util.Randomizer;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.statistics.distribution.GammaDistribution;
import org.junit.jupiter.api.Test;

import static org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator.DEFAULT_MIN_ITERATIONS_COUNT;
import static org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator.DEFAULT_RELATIVE_ACCURACY;
import static org.junit.jupiter.api.Assertions.*;

public class GammaTest  {

	@Test
	public void testGammaCumulative() throws Exception {
		Gamma dist = new Gamma();
        // old version BEAST 2 uses mode.ShapeScale as default,
        // so beta was confusing, it should be theta.
        dist.initByName("alpha", new RealScalarParam<>(0.001, PositiveReal.INSTANCE),
                "theta", new RealScalarParam<>(1000.0, PositiveReal.INSTANCE));
		double v = dist.inverseCumulativeProbability(0.5);
		assertEquals(5.244206e-299, v, 1e-304);
		v = dist.inverseCumulativeProbability(0.05);
		assertEquals(0.0, v, 1e-304);
		v = dist.inverseCumulativeProbability(0.025);
		assertEquals(0.0, v, 1e-304);

		v = dist.inverseCumulativeProbability(0.95);
		assertEquals(2.973588e-20, v, 1e-24);
		v = dist.inverseCumulativeProbability(0.975);
		assertEquals(5.679252e-09, v, 1e-13);
	}

    @Test
    public void testGammaCumulative2() throws Exception {
        Gamma dist = new Gamma();
        dist.initByName("alpha", new RealScalarParam<>(0.001, PositiveReal.INSTANCE),
                "lambda", new RealScalarParam<>((1.0/1000.0), PositiveReal.INSTANCE));
        double v = dist.inverseCumulativeProbability(0.5);
        assertEquals(5.244206e-299, v, 1e-304);
        v = dist.inverseCumulativeProbability(0.05);
        assertEquals(0.0, v, 1e-304);
        v = dist.inverseCumulativeProbability(0.025);
        assertEquals(0.0, v, 1e-304);

        v = dist.inverseCumulativeProbability(0.95);
        assertEquals(2.973588e-20, v, 1e-24);
        v = dist.inverseCumulativeProbability(0.975);
        assertEquals(5.679252e-09, v, 1e-13);
    }

	/** The code below is adapted from GammaDistributionTest from BEAST 1
	 * This test stochastically draws gamma
	 * variates and compares the coded pdf
	 * with the actual pdf.
	 * The tolerance is required to be at most 1e-10.
	 */

    static double mypdf(double value, double shape, double scale) {
        return Math.exp((shape-1) * Math.log(value) - value/scale - GammaFunction.lnGamma(shape) - shape * Math.log(scale) );
    }
//TODO not working yet
    @Test
	public void testPdf()  {

        final int numberOfTests = 300;
        double totErr = 0;
        double ptotErr = 0; int np = 0;
        double qtotErr = 0;

        Randomizer.setSeed(551);

        for(int i = 0; i < numberOfTests; i++){
            final double mean = .01 + (3-0.01) * Randomizer.nextDouble();
            final double var = .01 + (3-0.01) * Randomizer.nextDouble();

            final double scale = var / mean;
            final double shape = mean / scale;

            final GammaDistribution gammaDist;
            final int index = 1;//TODO Randomizer.nextInt(4);
        	switch (index) {
                case 0: { // ShapeScale
                    Gamma gamma = new Gamma();
                    gamma.initByName("alpha", new RealScalarParam<>(shape, PositiveReal.INSTANCE),
                            "theta", new RealScalarParam<>(scale, PositiveReal.INSTANCE));
                    gammaDist = (GammaDistribution) gamma.getApacheDistribution();
                    break;
                }
                case 1: { // ShapeRate
                    Gamma gamma = new Gamma();
                    gamma.initByName("alpha", new RealScalarParam<>(shape, PositiveReal.INSTANCE),
                            "lambda", new RealScalarParam<>(1 / scale, PositiveReal.INSTANCE));
                    gammaDist = (GammaDistribution) gamma.getApacheDistribution();
                    break;
                }
                case 2: { // ShapeMean
                    GammaMean gamma = new GammaMean();
                    gamma.initByName("alpha", new RealScalarParam<>(shape, PositiveReal.INSTANCE),
                            "mean", new RealScalarParam<>(scale * shape, PositiveReal.INSTANCE));
                    gammaDist = (GammaDistribution) gamma.getApacheDistribution();
                    break;
                }
                case 3: { // OneParameter
                    GammaMean gamma = new GammaMean();
                    gamma.initByName("alpha",
                            new RealScalarParam<>(1 / shape, PositiveReal.INSTANCE));
                    gammaDist = (GammaDistribution) gamma.getApacheDistribution();
                    break;
                }
                default:
                    throw new IllegalStateException("Unexpected value: " + index);
            }

            // lambda is rate
            final double value = Randomizer.nextGamma(shape, 1/scale);

            final double mypdf = mypdf(value, shape, scale);
            final double pdf = gammaDist.density(value);
            if ( Double.isInfinite(mypdf) && Double.isInfinite(pdf)) {
                continue;
            }

            assertFalse(Double.isNaN(mypdf));
            assertFalse(Double.isNaN(pdf));

            totErr +=  mypdf != 0 ? Math.abs((pdf - mypdf)/mypdf) : pdf;

            assertFalse(Double.isNaN(totErr), "nan");
            assertEquals(mypdf, gammaDist.density(value), Math.max(1e-10, Math.abs(mypdf) * 1e-10),
                    shape + "," + scale + "," + value + ", mode = " + index);

            final double cdf = gammaDist.cumulativeProbability(value);

            // PDF wrapper
            UnivariateFunction f = x -> mypdf(x, shape, scale);
            // Integrator from Math3
            UnivariateIntegrator integrator = new RombergIntegrator(
                    DEFAULT_RELATIVE_ACCURACY, 1e-14,
                    DEFAULT_MIN_ITERATIONS_COUNT, 16);
            double x;
            try {
                x = integrator.integrate(Integer.MAX_VALUE, f, 0.0, value);
                ptotErr += cdf != 0.0 ? Math.abs(x - cdf) / cdf : x;
                np++;

                // Inverse CDF
                double q = gammaDist.inverseCumulativeProbability(cdf);
                qtotErr += q != 0.0 ? Math.abs(q - value) / q : value;
            } catch (MaxCountExceededException e) {
                // can't integrate, skip test
            }
            //System.out.println(shape + ","  + scale + " " + value);

           // assertEquals("" + shape + "," + scale + "," + value + " " + Math.abs(q-value)/value, q, value, 1e-6);
           // System.out.print("\n" + np + ": " + mode + " " + totErr/np + " " + qtotErr/np + " " + ptotErr/np);
        }
        //System.out.println( !Double.isNaN(totErr) );
       // System.out.println(totErr);
        // bad test, but I can't find a good threshold that works for all individual cases
        assertTrue(totErr/numberOfTests < 1e-7, "failed " + totErr/numberOfTests);
        assertTrue(qtotErr/numberOfTests < 1e-10, "failed " + qtotErr/numberOfTests);
        assertTrue(np > 0 ? (ptotErr/np < 2e-7) : true, "failed " + ptotErr/np);
	}

}
