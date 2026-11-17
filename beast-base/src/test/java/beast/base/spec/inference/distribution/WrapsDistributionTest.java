package beast.base.spec.inference.distribution;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The support a wrapper (Offset, Bounded) would have with another inner distribution, as
 * BEAUti asks it to tell which inner distributions fit a parameter.
 */
public class WrapsDistributionTest {

    private static Poisson poisson() {
        return new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE));
    }

    private static IntUniform intUniform(int lower, int upper) {
        IntUniform u = new IntUniform();
        u.initByName("lower", new IntScalarParam<>(lower, Int.INSTANCE), "upper", new IntScalarParam<>(upper, Int.INSTANCE));
        return u;
    }

    private static Normal normal() {
        Normal n = new Normal();
        n.initByName("mean", new RealScalarParam<>(0.0, Real.INSTANCE), "sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        return n;
    }

    private static Exponential exponential() {
        Exponential e = new Exponential();
        e.initByName("mean", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        return e;
    }

    @Test
    public void offsetIntShiftsTheCandidate() {
        OffsetInt<Int> zero = new OffsetInt<>(intUniform(1, 5), 0);
        OffsetInt<Int> one = new OffsetInt<>(intUniform(1, 5), 1);
        assertEquals("below 1", zero.supportOutsideDomainWith(poisson(), PositiveInt.INSTANCE));
        assertNull(one.supportOutsideDomainWith(poisson(), PositiveInt.INSTANCE));
        // the wrapper itself is unchanged
        assertEquals(1, zero.getLowerBoundOfParameter());
    }

    @Test
    public void offsetRealShiftsTheCandidate() {
        OffsetReal<Real> offset = new OffsetReal<>();
        offset.initByName("distribution", exponential(), "offset", new RealScalarParam<>(-1.0, Real.INSTANCE));
        assertEquals("below 0.0", offset.supportOutsideDomainWith(exponential(), NonNegativeReal.INSTANCE));
        OffsetReal<Real> zero = new OffsetReal<>();
        zero.initByName("distribution", exponential(), "offset", new RealScalarParam<>(0.0, Real.INSTANCE));
        assertNull(zero.supportOutsideDomainWith(exponential(), NonNegativeReal.INSTANCE));
        assertEquals("below 0.0", zero.supportOutsideDomainWith(normal(), NonNegativeReal.INSTANCE));
        assertEquals("above 1.0", zero.supportOutsideDomainWith(exponential(), UnitInterval.INSTANCE));
    }

    @Test
    public void truncatedIntersectsTheCandidate() {
        TruncatedInt<Int> bounded = new TruncatedInt<>();
        bounded.initByName("distribution", poisson(),
                "lower", new IntScalarParam<>(1, Int.INSTANCE), "upper", new IntScalarParam<>(10, Int.INSTANCE));
        // Poisson from 0, cut to [1, 10]
        assertNull(bounded.supportOutsideDomainWith(poisson(), PositiveInt.INSTANCE));
        TruncatedInt<Int> fromZero = new TruncatedInt<>();
        fromZero.initByName("distribution", poisson(),
                "lower", new IntScalarParam<>(0, Int.INSTANCE), "upper", new IntScalarParam<>(10, Int.INSTANCE));
        assertEquals("below 1", fromZero.supportOutsideDomainWith(poisson(), PositiveInt.INSTANCE));
        // IntUniform [2, 5] stays within [0, 10] and above 0
        assertNull(fromZero.supportOutsideDomainWith(intUniform(2, 5), PositiveInt.INSTANCE));

        TruncatedReal<Real> unit = new TruncatedReal<>();
        unit.initByName("distribution", normal(),
                "lower", new RealScalarParam<>(0.0, Real.INSTANCE), "upper", new RealScalarParam<>(1.0, Real.INSTANCE));
        assertNull(unit.supportOutsideDomainWith(normal(), UnitInterval.INSTANCE));
        assertNull(unit.supportOutsideDomainWith(exponential(), UnitInterval.INSTANCE));
    }

    @Test
    public void truncatingAndShiftingWrappers() {
        assertEquals(false, new OffsetInt<Int>(poisson(), 1).truncates());
        OffsetReal<Real> offsetReal = new OffsetReal<>();
        offsetReal.initByName("distribution", exponential(), "offset", new RealScalarParam<>(0.0, Real.INSTANCE));
        assertEquals(false, offsetReal.truncates());
        TruncatedInt<Int> truncatedInt = new TruncatedInt<>();
        truncatedInt.initByName("distribution", poisson(),
                "lower", new IntScalarParam<>(0, Int.INSTANCE), "upper", new IntScalarParam<>(10, Int.INSTANCE));
        assertEquals(true, truncatedInt.truncates());
        TruncatedReal<Real> truncatedReal = new TruncatedReal<>();
        truncatedReal.initByName("distribution", normal(),
                "lower", new RealScalarParam<>(0.0, Real.INSTANCE), "upper", new RealScalarParam<>(1.0, Real.INSTANCE));
        assertEquals(true, truncatedReal.truncates());
    }

    /** after the distribution input changes (as in BEAUti), the new inner distribution is returned, before any refresh() */
    @Test
    public void innerDistributionIsCurrent() {
        TruncatedInt<Int> bounded = new TruncatedInt<>();
        bounded.initByName("distribution", poisson(),
                "lower", new IntScalarParam<>(0, Int.INSTANCE), "upper", new IntScalarParam<>(10, Int.INSTANCE));
        OffsetInt<Int> offset = new OffsetInt<>(poisson(), 1);
        bounded.distributionInput.setValue(offset, bounded);
        assertEquals(offset, bounded.getInnerDistribution());

        TruncatedReal<Real> boundedReal = new TruncatedReal<>();
        boundedReal.initByName("distribution", normal(),
                "lower", new RealScalarParam<>(0.0, Real.INSTANCE), "upper", new RealScalarParam<>(1.0, Real.INSTANCE));
        Exponential e = exponential();
        boundedReal.distributionInput.setValue(e, boundedReal);
        assertEquals(e, boundedReal.getInnerDistribution());
    }
}
