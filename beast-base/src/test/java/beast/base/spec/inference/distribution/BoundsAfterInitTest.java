package beast.base.spec.inference.distribution;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The effective bounds of a parameter are computed from the priors in its outputs.
 * BEAUti changes a prior after it has been initialised: it creates the new prior from a
 * template (which runs initAndValidate) and only then attaches the parameter, or it swaps
 * the inner distribution of a wrapper. The bounds must follow these changes without
 * another call to initAndValidate (issue #230).
 */
public class BoundsAfterInitTest {

    private static Uniform uniform(double lower, double upper) {
        return new Uniform(null,
                new RealScalarParam<>(lower, Real.INSTANCE),
                new RealScalarParam<>(upper, Real.INSTANCE));
    }

    private static Normal normal() {
        Normal normal = new Normal();
        normal.initByName(
                "mean", new RealScalarParam<>(0.0, Real.INSTANCE),
                "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        return normal;
    }

    /** param attached after initAndValidate, as BEAUti does when selecting another prior **/
    @Test
    public void testParamAttachedAfterInit() {
        RealScalarParam<Real> rate = new RealScalarParam<>(0.5, Real.INSTANCE);
        rate.setID("rate");
        Uniform uniform = uniform(0.0, 1.0); // initialised without param

        uniform.paramInput.setValue(rate, uniform);

        assertEquals("rate", uniform.getArguments().get(0));
        assertEquals(0.0, rate.getLower(), 0.0);
        assertEquals(1.0, rate.getUpper(), 0.0);
    }

    /** the base distribution of an IID swapped after initAndValidate **/
    @Test
    public void testIIDBaseDistributionSwappedAfterInit() {
        RealVectorParam<Real> rates = new RealVectorParam<>(new double[] {0.5, 0.5}, Real.INSTANCE);
        rates.setID("rates");
        IID<?, ?, ?> iid = new IID<>();
        iid.initByName("param", rates, "distr", normal());
        assertEquals(Double.NEGATIVE_INFINITY, rates.getLower(), 0.0);

        iid.distInput.setValue(uniform(0.0, 1.0), iid);

        assertEquals(0.0, rates.getLower(), 0.0);
        assertEquals(1.0, rates.getUpper(), 0.0);
    }

    /** the inner distribution and offset of an OffsetReal replaced after initAndValidate **/
    @Test
    public void testOffsetRealInnerDistributionSwappedAfterInit() {
        RealScalarParam<Real> x = new RealScalarParam<>(1.5, Real.INSTANCE);
        x.setID("x");
        OffsetReal offsetReal = new OffsetReal();
        offsetReal.initByName("distribution", uniform(0.0, 1.0),
                "offset", new RealScalarParam<>(1.0, Real.INSTANCE),
                "param", x);
        assertEquals(1.0, x.getLower(), 0.0);
        assertEquals(2.0, x.getUpper(), 0.0);

        offsetReal.distributionInput.setValue(uniform(0.0, 2.0), offsetReal);
        offsetReal.offsetInput.setValue(new RealScalarParam<>(-1.0, Real.INSTANCE), offsetReal);

        assertEquals(-1.0, x.getLower(), 0.0);
        assertEquals(1.0, x.getUpper(), 0.0);
    }

    /** the inner distribution of a TruncatedReal (BEAUti's "Bounded" prior) replaced after initAndValidate **/
    @Test
    public void testTruncatedRealInnerDistributionSwappedAfterInit() {
        RealScalarParam<Real> x = new RealScalarParam<>(0.5, Real.INSTANCE);
        x.setID("x");
        TruncatedReal truncated = new TruncatedReal();
        truncated.initByName("distribution", normal(),
                "lower", new RealScalarParam<>(0.0, Real.INSTANCE),
                "upper", new RealScalarParam<>(10.0, Real.INSTANCE),
                "param", x);
        assertEquals(0.0, x.getLower(), 0.0);
        assertEquals(10.0, x.getUpper(), 0.0);

        truncated.distributionInput.setValue(uniform(0.0, 1.0), truncated);

        assertEquals(0.0, x.getLower(), 0.0);
        assertEquals(1.0, x.getUpper(), 0.0);
    }

    /**
     * TruncatedInt: the upper bound is the smaller of the truncation upper and the inner upper
     * bound (it used to ignore the truncation upper), and follows a truncation upper replaced
     * after initAndValidate.
     */
    @Test
    public void testTruncatedIntUpperBound() {
        IntScalarParam<Int> n = new IntScalarParam<>(3, Int.INSTANCE);
        n.setID("n");
        TruncatedInt truncated = new TruncatedInt();
        truncated.initByName("distribution", new Poisson(null, new RealScalarParam<>(5.0, NonNegativeReal.INSTANCE)),
                "lower", new IntScalarParam<>(0, Int.INSTANCE),
                "upper", new IntScalarParam<>(10, Int.INSTANCE),
                "param", n);
        assertEquals(0, (int) n.getLower());
        assertEquals(10, (int) n.getUpper());

        truncated.upperInput.setValue(new IntScalarParam<>(5, Int.INSTANCE), truncated);

        assertEquals(5, (int) n.getUpper());
    }
}
