package beast.base.spec.inference.distribution;

import beast.base.spec.domain.*;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.BoundUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the check that a prior's support fits the domain of its parameter,
 * {@link BoundUtils#supportOutsideDomain} and
 * {@link TensorDistribution#supportOutsideDomainMessage()}.
 */
public class SupportOutsideDomainTest {

    private static final double INF = Double.POSITIVE_INFINITY;
    private static final int MAX = Integer.MAX_VALUE;

    //*** BoundUtils.supportOutsideDomain ***//

    @Test
    public void testIntDomains() {
        // Poisson: support from 0
        assertNull(BoundUtils.supportOutsideDomain(Int.INSTANCE, 0, MAX));
        assertNull(BoundUtils.supportOutsideDomain(NonNegativeInt.INSTANCE, 0, MAX));
        // PositiveInt is stored as lower 0, exclusive: its first value is 1
        assertEquals("below 1", BoundUtils.supportOutsideDomain(PositiveInt.INSTANCE, 0, MAX));
        // Poisson + 1
        assertNull(BoundUtils.supportOutsideDomain(PositiveInt.INSTANCE, 1, MAX));
        assertEquals("below 0", BoundUtils.supportOutsideDomain(NonNegativeInt.INSTANCE, -5, 5));
        // no lower limit at all must not wrap around
        assertEquals("below 0", BoundUtils.supportOutsideDomain(NonNegativeInt.INSTANCE, Integer.MIN_VALUE, MAX));
    }

    @Test
    public void testRealDomains() {
        // Normal
        assertNull(BoundUtils.supportOutsideDomain(Real.INSTANCE, -INF, INF));
        assertEquals("below 0.0", BoundUtils.supportOutsideDomain(PositiveReal.INSTANCE, -INF, INF));
        // LogNormal, Exponential: support from 0 fits PositiveReal although 0 itself is excluded,
        // as a single point carries no mass
        assertNull(BoundUtils.supportOutsideDomain(PositiveReal.INSTANCE, 0.0, INF));
        assertNull(BoundUtils.supportOutsideDomain(NonNegativeReal.INSTANCE, 0.0, INF));
        // Beta
        assertNull(BoundUtils.supportOutsideDomain(UnitInterval.INSTANCE, 0.0, 1.0));
        // Gamma on a unit interval
        assertEquals("above 1.0", BoundUtils.supportOutsideDomain(UnitInterval.INSTANCE, 0.0, INF));
        assertEquals("below 0.0 and above 1.0", BoundUtils.supportOutsideDomain(UnitInterval.INSTANCE, -INF, INF));
    }

    /**
     * PositiveInt is stored as the exclusive lower bound 0, but integer parameter bounds
     * are inclusive, so the parameter's lower bound must be 1: an operator that proposes
     * up to getLower() would otherwise propose 0, which set() rejects with an exception.
     */
    @Test
    public void testPositiveIntParameterLowerIsOne() {
        IntScalarParam<PositiveInt> x = new IntScalarParam<>(3, PositiveInt.INSTANCE);
        assertEquals(1, x.getLower());
        assertTrue(x.lowerInclusive());
        IntScalarParam<NonNegativeInt> y = new IntScalarParam<>(3, NonNegativeInt.INSTANCE);
        assertEquals(0, y.getLower());
        IntVectorParam<PositiveInt> v = new IntVectorParam<>(new int[]{1, 2}, PositiveInt.INSTANCE);
        assertEquals(1, v.getLower());
        // a prior with support from 0 (Poisson) must not loosen it
        Poisson p = poisson();
        x.setID("x");
        p.initByName("param", x);
        assertEquals(1, x.getLower());
    }

    //*** TensorDistribution.supportOutsideDomainMessage ***//

    private static Poisson poisson() {
        return new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE));
    }

    @Test
    public void testPoissonOnPositiveInt() {
        IntScalarParam<PositiveInt> x = new IntScalarParam<>(3, PositiveInt.INSTANCE);
        x.setID("x");
        Poisson p = poisson();
        p.initByName("param", x);
        String message = p.supportOutsideDomainMessage();
        assertNotNull(message);
        assertTrue(message.startsWith("Poisson puts probability on values below 1, but the domain PositiveInt of x"),
                message);

        IntScalarParam<NonNegativeInt> y = new IntScalarParam<>(3, NonNegativeInt.INSTANCE);
        p = poisson();
        p.initByName("param", y);
        assertNull(p.supportOutsideDomainMessage());
    }

    @Test
    public void testOffsetPoissonOnPositiveInt() {
        IntScalarParam<PositiveInt> x = new IntScalarParam<>(3, PositiveInt.INSTANCE);
        OffsetInt offset1 = new OffsetInt();
        offset1.initByName("distribution", poisson(), "offset", new IntScalarParam<>(1, Int.INSTANCE), "param", x);
        assertNull(offset1.supportOutsideDomainMessage());

        x = new IntScalarParam<>(3, PositiveInt.INSTANCE);
        OffsetInt offset0 = new OffsetInt();
        offset0.initByName("distribution", poisson(), "offset", new IntScalarParam<>(0, Int.INSTANCE), "param", x);
        assertNotNull(offset0.supportOutsideDomainMessage());
    }

    @Test
    public void testBoundedPoissonOnPositiveInt() {
        IntScalarParam<PositiveInt> x = new IntScalarParam<>(3, PositiveInt.INSTANCE);
        TruncatedInt bounded1 = new TruncatedInt();
        bounded1.initByName("distribution", poisson(), "lower", new IntScalarParam<>(1, Int.INSTANCE),
                "upper", new IntScalarParam<>(10, Int.INSTANCE), "param", x);
        assertNull(bounded1.supportOutsideDomainMessage());

        x = new IntScalarParam<>(3, PositiveInt.INSTANCE);
        TruncatedInt bounded0 = new TruncatedInt();
        bounded0.initByName("distribution", poisson(), "lower", new IntScalarParam<>(0, Int.INSTANCE),
                "upper", new IntScalarParam<>(10, Int.INSTANCE), "param", x);
        assertNotNull(bounded0.supportOutsideDomainMessage());
    }

    @Test
    public void testIIDNormalOnPositiveRealVector() {
        RealVectorParam<PositiveReal> v = new RealVectorParam<>(new double[]{1.0, 2.0}, PositiveReal.INSTANCE);
        Normal normal = new Normal(null, new RealScalarParam<>(0.0, Real.INSTANCE),
                new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        IID<?, ?, ?> iid = new IID<>(v, normal);
        assertNotNull(iid.supportOutsideDomainMessage());

        v = new RealVectorParam<>(new double[]{1.0, 2.0}, PositiveReal.INSTANCE);
        Exponential exp = new Exponential(null, new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        iid = new IID<>(v, exp);
        assertNull(iid.supportOutsideDomainMessage());
    }

    @Test
    public void testNoParameter() {
        // an inner distribution of a wrapper has no param: nothing to check
        assertNull(poisson().supportOutsideDomainMessage());
    }
}
