package beast.base.spec.inference.distribution;

import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import org.apache.commons.statistics.distribution.GammaDistribution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test for inverse gamma distribution.
 *
 * @author Joseph Heled
 *         Date: 24/04/2009
 */
public class InverseGammaTest {
    interface TestData {
        double getShape();

        double getScale();

        double[] getPDF();

        double[] getCDF();
    }

    // test data generated from this python code:
// import scipy.stats
//
//print """TestData[] tests = {"""
//for shape,scale in ((3,2), (3,1)) :
//  d = scipy.stats.invgamma(shape, scale=scale)
//  print """
//        new TestData() {
//            public double getShape() {
//                return %d;
//            }
//
//            public double getScale() {
//               return %d;
//            }""" % (shape, scale)
//  x = (0.5, 1, 2)
//  print """
//            public double[] getPDF() {
//                return new double[]{%s};
//            }""" % " , ".join(["%g,%.14lf" % (z,d.pdf(z)) for z in x])
//
//  print """
//            public double[] getCDF() {
//                return new double[]{%s};
//            }
//        } ,""" % " , ".join(["%g,%.14lf" % (z,d.cdf(z)) for z in x])
//

    TestData[] tests = {

            new TestData() {
                public double getShape() {
                    return 3;
                }

                public double getScale() {
                    return 2;
                }

                public double[] getPDF() {
                    return new double[]{0.5, 1.17220088887899, 1, 0.54134113294645, 2, 0.09196986029286};
                }

                public double[] getCDF() {
                    return new double[]{0.5, 0.23810330555354, 1, 0.67667641618306, 2, 0.91969860292861};
                }
            },

            new TestData() {
                public double getShape() {
                    return 3;
                }

                public double getScale() {
                    return 1;
                }

                public double[] getPDF() {
                    return new double[]{0.5, 1.08268226589290, 1, 0.18393972058572, 2, 0.01895408311602};
                }

                public double[] getCDF() {
                    return new double[]{0.5, 0.67667641618306, 1, 0.91969860292861, 2, 0.98561232203303};
                }
            },
    };

    @Test
    public void testInvGamma() {
        for( TestData td : tests ) {
            InverseGamma d = new InverseGamma();
            d.initByName("alpha", new RealScalarParam<>(td.getShape(), PositiveReal.INSTANCE),
                    "beta", new RealScalarParam<>(td.getScale(), PositiveReal.INSTANCE));

            {
                double[] p = td.getPDF();
                for(int k = 0; k < p.length; k += 2) {
                    assertEquals(d.density(p[k]), p[k + 1], 1e-10);

                    assertEquals(d.logDensity(p[k]), Math.log(p[k + 1]), 1e-10);
                }
            }

            double[] cdf = td.getCDF();
            for(int k = 0; k < cdf.length; k += 2) {
                assertEquals(d.cumulativeProbability(cdf[k]), cdf[k + 1], 1e-10);
            }
        }
    }

    /**
     * getApacheDistribution() returns the internal helper GammaDistribution (used to draw
     * samples via x = 1/y, y ~ Gamma(alpha, rate=beta)). Its own cumulativeProbability describes
     * Gamma, not InverseGamma, so InverseGamma.cumulativeProbability(x) must apply the x = 1/y
     * change of variables rather than delegate straight to the Gamma object: if Y = 1/X ~
     * Gamma(alpha, rate=beta), then P(X &le; x) = P(Y &ge; 1/x) = 1 - GammaCDF(1/x). This checks
     * that against independently-generated reference values (see the python snippet above) and
     * against the Gamma distribution directly, and that it is NOT simply the raw Gamma CDF.
     */
    @Test
    public void testCumulativeProbabilityMatchesInverseGammaNotGamma() {
        for (TestData td : tests) {
            double alpha = td.getShape();
            double beta = td.getScale();

            InverseGamma d = new InverseGamma();
            d.initByName("alpha", new RealScalarParam<>(alpha, PositiveReal.INSTANCE),
                    "beta", new RealScalarParam<>(beta, PositiveReal.INSTANCE));

            // the Gamma(alpha, scale=1/beta) distribution used internally for sampling
            GammaDistribution gamma = GammaDistribution.of(alpha, 1.0 / beta);

            double[] cdf = td.getCDF();
            for (int k = 0; k < cdf.length; k += 2) {
                double x = cdf[k];
                double referenceInverseGammaCdf = cdf[k + 1]; // independently generated reference value

                assertEquals(referenceInverseGammaCdf, d.cumulativeProbability(x), 1e-10);
                assertEquals(gamma.survivalProbability(1.0 / x), d.cumulativeProbability(x), 1e-10);

                // it must no longer be the raw Gamma CDF (the pre-fix bug)
                assertNotEquals(gamma.cumulativeProbability(x), d.cumulativeProbability(x), 1e-3);
            }
        }
    }

    /**
     * Mirrors testCumulativeProbabilityMatchesInverseGammaNotGamma() but for the inverse CDF
     * (quantile function), which is what direct/inverse-transform samplers actually call (see
     * e.g. RandomTree, CalibratedYuleModel, UCRelaxedClockModel, SampleOffValues, which all call
     * distribution.inverseCumulativeProbability(p) directly). For an InverseGamma X with
     * Y = 1/X ~ Gamma(alpha, rate=beta), the correct relationship is
     *   invGammaICDF(p) = 1 / gammaICDF(1 - p)
     * because P(X &le; x) = P(Y &ge; 1/x) = 1 - GammaCDF(1/x). This checks that
     * inverseCumulativeProbability round-trips through cumulativeProbability (both against each
     * other and against the reference CDF values) and that it is NOT simply the raw Gamma ICDF.
     */
    @Test
    public void testInverseCumulativeProbabilityMatchesInverseGammaNotGamma() {
        for (TestData td : tests) {
            double alpha = td.getShape();
            double beta = td.getScale();

            InverseGamma d = new InverseGamma();
            d.initByName("alpha", new RealScalarParam<>(alpha, PositiveReal.INSTANCE),
                    "beta", new RealScalarParam<>(beta, PositiveReal.INSTANCE));

            GammaDistribution gamma = GammaDistribution.of(alpha, 1.0 / beta);

            // round-trip through the reference CDF values themselves
            double[] cdf = td.getCDF();
            for (int k = 0; k < cdf.length; k += 2) {
                double x = cdf[k];
                double p = cdf[k + 1];
                assertEquals(x, d.inverseCumulativeProbability(p), 1e-6);
            }

            for (double p : new double[]{0.1, 0.25, 0.5, 0.75, 0.9}) {
                double actual = d.inverseCumulativeProbability(p);

                double correctInverseGammaIcdf = 1.0 / gamma.inverseCumulativeProbability(1.0 - p);
                assertEquals(correctInverseGammaIcdf, actual, 1e-9);

                // it must no longer be the raw Gamma ICDF (the pre-fix bug)
                assertNotEquals(gamma.inverseCumulativeProbability(p), actual, 1e-6);

                // and it must round-trip through the (now-correct) CDF
                assertEquals(p, d.cumulativeProbability(actual), 1e-9);
            }
        }

        // boundary behaviour matches the distribution's support, (0, +Inf)
        InverseGamma d = new InverseGamma();
        d.initByName("alpha", new RealScalarParam<>(3.0, PositiveReal.INSTANCE),
                "beta", new RealScalarParam<>(2.0, PositiveReal.INSTANCE));
        assertEquals(0.0, d.inverseCumulativeProbability(0.0), 1e-10);
        assertEquals(Double.POSITIVE_INFINITY, d.inverseCumulativeProbability(1.0));
    }

    /**
     * ScalarDistribution.getMean() also delegates to getApacheDistribution().getMean(), i.e. the
     * internal Gamma(alpha, rate=beta) helper's mean (alpha/beta), not InverseGamma's own mean of
     * beta/(alpha-1), which diverges to +Infinity (never negative or NaN, since X &gt; 0 always)
     * as alpha -&gt; 1+ and stays there for alpha &le; 1.
     */
    @Test
    public void testGetMeanMatchesInverseGammaNotGamma() {
        for (TestData td : tests) {
            double alpha = td.getShape();
            double beta = td.getScale();

            InverseGamma d = new InverseGamma();
            d.initByName("alpha", new RealScalarParam<>(alpha, PositiveReal.INSTANCE),
                    "beta", new RealScalarParam<>(beta, PositiveReal.INSTANCE));

            GammaDistribution gamma = GammaDistribution.of(alpha, 1.0 / beta);

            assertEquals(beta / (alpha - 1), d.getMean(), 1e-10);

            // it must no longer be the raw Gamma mean (the pre-fix bug)
            assertNotEquals(gamma.getMean(), d.getMean(), 1e-6);
        }

        // alpha = 2 is the boundary where the mean is still finite (beta/(alpha-1) = beta) but
        // the variance (beta^2 / ((alpha-1)^2 (alpha-2))) is not.
        InverseGamma alphaTwo = new InverseGamma();
        alphaTwo.initByName("alpha", new RealScalarParam<>(2.0, PositiveReal.INSTANCE),
                "beta", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        assertEquals(1.0, alphaTwo.getMean(), 1e-10);
        assertNotEquals(GammaDistribution.of(2.0, 1.0).getMean(), alphaTwo.getMean(), 1e-6);

        // mean diverges to +Infinity, continuously, as alpha approaches 1 from above ...
        InverseGamma nearOne = new InverseGamma();
        nearOne.initByName("alpha", new RealScalarParam<>(1.0 + 1e-6, PositiveReal.INSTANCE),
                "beta", new RealScalarParam<>(2.0, PositiveReal.INSTANCE));
        assertEquals(2.0 / 1e-6, nearOne.getMean(), 1.0);

        // ... and is +Infinity, not NaN, at and below alpha = 1
        for (double alpha : new double[]{1.0, 0.9, 0.5}) {
            InverseGamma d = new InverseGamma();
            d.initByName("alpha", new RealScalarParam<>(alpha, PositiveReal.INSTANCE),
                    "beta", new RealScalarParam<>(2.0, PositiveReal.INSTANCE));
            assertEquals(Double.POSITIVE_INFINITY, d.getMean());
        }
    }

    /**
     * getApacheDistribution() exposes the internal Gamma(alpha, rate=beta) helper, which is only
     * used for sampling via x = 1/y and does not represent InverseGamma's own density/CDF/mean --
     * all of which are overridden above without consulting it. It should therefore return null
     * (per its own documented contract), so any ScalarDistribution method added later that isn't
     * also overridden here fails loudly rather than silently returning a Gamma-distributed answer.
     * getLowerBoundOfParameter()/getUpperBoundOfParameter() must therefore also be overridden
     * directly (not derived from getApacheDistribution()) to keep returning the correct support,
     * (0, +Infinity), and inverseCumulativeProbability's p<=0/p>=1 boundary handling (which calls
     * them) must keep working.
     */
    @Test
    public void testGetApacheDistributionIsNull() {
        InverseGamma d = new InverseGamma();
        d.initByName("alpha", new RealScalarParam<>(3.0, PositiveReal.INSTANCE),
                "beta", new RealScalarParam<>(2.0, PositiveReal.INSTANCE));

        assertNull(d.getApacheDistribution());

        assertEquals(0.0, d.getLowerBoundOfParameter(), 1e-10);
        assertEquals(Double.POSITIVE_INFINITY, d.getUpperBoundOfParameter());

        // inverseCumulativeProbability's boundary handling still works without getApacheDistribution()
        assertEquals(0.0, d.inverseCumulativeProbability(0.0), 1e-10);
        assertEquals(Double.POSITIVE_INFINITY, d.inverseCumulativeProbability(1.0));
    }

    /**
     * sample() draws y from the internal Gamma(alpha, rate=beta) helper and returns x = 1/y --
     * this is the original "direct sampler" code path the whole InverseGamma investigation started
     * from, but it had no test exercising it at all. Checks the empirical mean and empirical CDF
     * of a large sample against getMean() and the reference CDF values (both independently
     * verified correct elsewhere in this class), with tolerances set from the distribution's own
     * (finite, for alpha=3) variance/binomial sampling error so the test isn't flaky.
     */
    @Test
    public void testSampleDrawsFromInverseGamma() {
        double alpha = 3;
        double beta = 2;

        InverseGamma d = new InverseGamma();
        d.initByName("alpha", new RealScalarParam<>(alpha, PositiveReal.INSTANCE),
                "beta", new RealScalarParam<>(beta, PositiveReal.INSTANCE));

        final int n = 200_000;
        double sum = 0;
        int belowOne = 0;
        for (int i = 0; i < n; i++) {
            double x = d.sample().getFirst();
            sum += x;
            if (x < 1.0) {
                belowOne++;
            }
        }
        double sampleMean = sum / n;
        double empiricalCdfAtOne = belowOne / (double) n;

        // Var(X) = beta^2 / ((alpha-1)^2 (alpha-2)) = 1 for alpha=3, beta=2, so SE(mean) ~ 0.0022;
        // 0.05 is a >20-sigma margin.
        assertEquals(d.getMean(), sampleMean, 0.05);

        // reference value from TestData for (alpha=3, beta=2) at x=1; SE(proportion) ~ 0.001,
        // 0.02 is a similarly generous margin.
        assertEquals(0.67667641618306, empiricalCdfAtOne, 0.02);
    }
}
