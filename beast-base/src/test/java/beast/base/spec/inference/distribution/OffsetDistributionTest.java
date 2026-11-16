package beast.base.spec.inference.distribution;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import org.apache.commons.statistics.distribution.PoissonDistribution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OffsetInt} and {@link OffsetReal}.
 */
public class OffsetDistributionTest {

    private OffsetInt offsetPoisson(double lambda, int offset, IntScalarParam<Int> param) {
        Poisson poisson = new Poisson(null, new RealScalarParam<>(lambda, NonNegativeReal.INSTANCE));
        OffsetInt offsetInt = new OffsetInt();
        offsetInt.initByName("distribution", poisson,
                "offset", new IntScalarParam<>(offset, Int.INSTANCE),
                "param", param);
        return offsetInt;
    }

    private OffsetReal offsetExponential(double mean, double offset, RealScalarParam<Real> param) {
        Exponential exp = new Exponential(null, new RealScalarParam<>(mean, PositiveReal.INSTANCE));
        OffsetReal offsetReal = new OffsetReal();
        offsetReal.initByName("distribution", exp,
                "offset", new RealScalarParam<>(offset, Real.INSTANCE),
                "param", param);
        return offsetReal;
    }

    //*** OffsetInt ***//

    /**
     * log P(X = x) for X = Poisson(lambda) + offset must equal log P(Poisson(lambda) = x - offset).
     * R: dpois(3 - 1, 5, log = TRUE) = -2.474271
     */
    @Test
    public void testOffsetIntCalculateLogP() {
        double lambda = 5.0;
        int offset = 1;
        IntScalarParam<Int> param = new IntScalarParam<>(3, Int.INSTANCE);
        OffsetInt offsetInt = offsetPoisson(lambda, offset, param);

        double expected = PoissonDistribution.of(lambda).logProbability(3 - offset);
        assertEquals(-2.474271, expected, 1e-6);
        assertEquals(expected, offsetInt.calculateLogP(), 1e-10);
        // calculateLogP must agree with logDensity of the same value
        assertEquals(offsetInt.logDensity(3), offsetInt.calculateLogP(), 1e-10);
    }

    /**
     * Values below the offset have probability zero.
     */
    @Test
    public void testOffsetIntCalculateLogPBelowOffset() {
        IntScalarParam<Int> param = new IntScalarParam<>(1, Int.INSTANCE);
        OffsetInt offsetInt = offsetPoisson(5.0, 2, param);
        assertEquals(Double.NEGATIVE_INFINITY, offsetInt.calculateLogP());
    }

    /**
     * Poisson has no upper limit (Integer.MAX_VALUE), so adding the offset must not
     * overflow to a negative upper bound, which would make every value invalid.
     */
    @Test
    public void testOffsetIntBounds() {
        IntScalarParam<Int> param = new IntScalarParam<>(16, Int.INSTANCE);
        OffsetInt offsetInt = offsetPoisson(0.693, 14, param);

        assertEquals(14, offsetInt.getLowerBoundOfParameter());
        assertEquals(Integer.MAX_VALUE, offsetInt.getUpperBoundOfParameter(),
                "upper bound overflowed when adding the offset");
    }

    /**
     * Mirror of {@link #testOffsetIntBounds()} for the lower bound.
     * IntUniform with lower = Integer.MIN_VALUE has no lower limit, so subtracting the offset
     * must not underflow to a large positive lower bound (Integer.MIN_VALUE - 14 = 2147483634),
     * which would make every value invalid.
     */
    @Test
    public void testOffsetIntLowerBound() {
        IntUniform uniform = new IntUniform(null,
                new IntScalarParam<>(Integer.MIN_VALUE, Int.INSTANCE),
                new IntScalarParam<>(0, Int.INSTANCE));
        IntScalarParam<Int> param = new IntScalarParam<>(-20, Int.INSTANCE);
        OffsetInt offsetInt = new OffsetInt();
        offsetInt.initByName("distribution", uniform,
                "offset", new IntScalarParam<>(-14, Int.INSTANCE),
                "param", param);

        assertEquals(Integer.MIN_VALUE, offsetInt.getLowerBoundOfParameter(),
                "lower bound underflowed when adding the offset");
        assertEquals(-14, offsetInt.getUpperBoundOfParameter());
    }


    //*** OffsetReal ***//

    /**
     * log p(x) for X = Exp(mean=1) + offset must equal -(x - offset).
     * R: dexp(4 - 3, rate = 1, log = TRUE) = -1
     */
    @Test
    public void testOffsetRealCalculateLogP() {
        RealScalarParam<Real> param = new RealScalarParam<>(4.0, Real.INSTANCE);
        OffsetReal offsetReal = offsetExponential(1.0, 3.0, param);

        assertEquals(-1.0, offsetReal.calculateLogP(), 1e-10);
        // calculateLogP must agree with logDensity of the same value
        assertEquals(offsetReal.logDensity(4.0), offsetReal.calculateLogP(), 1e-10);
    }

    /**
     * Values below the offset have density zero.
     */
    @Test
    public void testOffsetRealCalculateLogPBelowOffset() {
        RealScalarParam<Real> param = new RealScalarParam<>(2.0, Real.INSTANCE);
        OffsetReal offsetReal = offsetExponential(1.0, 3.0, param);
        assertEquals(Double.NEGATIVE_INFINITY, offsetReal.calculateLogP());
    }


    //*** CDF, survival function and inverses shift by the offset ***//

    /**
     * They used to come from the inner distribution unshifted: P(X <= 4) for
     * Poisson(2) + 5 was 0.947 instead of 0.
     */
    @Test
    public void testOffsetIntCDF() {
        OffsetInt o = offsetPoisson(2.0, 5, new IntScalarParam<>(6, Int.INSTANCE));
        PoissonDistribution p = PoissonDistribution.of(2.0);
        assertEquals(0.0, o.cumulativeProbability(4), 1e-15);
        assertEquals(p.cumulativeProbability(0), o.cumulativeProbability(5), 1e-15);
        assertEquals(p.cumulativeProbability(3), o.cumulativeProbability(8), 1e-15);
        assertEquals(p.survivalProbability(3), o.survivalProbability(8), 1e-15);
        assertEquals(1.0, o.survivalProbability(4), 1e-15);
        assertEquals(p.inverseCumulativeProbability(0.5) + 5, o.inverseCumulativeProbability(0.5));
        assertEquals(p.inverseSurvivalProbability(0.1) + 5, o.inverseSurvivalProbability(0.1));
        // no upper limit stays no upper limit instead of wrapping around
        assertEquals(Integer.MAX_VALUE, o.inverseCumulativeProbability(1.0));
    }

    @Test
    public void testOffsetRealCDF() {
        OffsetReal o = offsetExponential(1.0, 3.0, new RealScalarParam<>(4.0, Real.INSTANCE));
        assertEquals(0.0, o.cumulativeProbability(3.0), 1e-15);
        assertEquals(1 - Math.exp(-1), o.cumulativeProbability(4.0), 1e-12);
        assertEquals(Math.exp(-1), o.survivalProbability(4.0), 1e-12);
        assertEquals(3.0 + Math.log(2), o.inverseCumulativeProbability(0.5), 1e-12);
        assertEquals(3.0 + Math.log(10), o.inverseSurvivalProbability(0.1), 1e-12);
    }
}
