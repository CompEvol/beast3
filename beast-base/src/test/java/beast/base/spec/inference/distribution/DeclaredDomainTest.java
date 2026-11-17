package beast.base.spec.inference.distribution;

import beast.base.parser.XMLParser;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The optional declared domain of OffsetInt, TruncatedInt, OffsetReal and TruncatedReal:
 * the wrapper's support must lie within it, e.g. Poisson + 1 is PositiveInt, Poisson - 1 is not.
 */
public class DeclaredDomainTest {

    private static Poisson poisson() {
        return new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE));
    }

    private static Normal standardNormal() {
        return new Normal(null, new RealScalarParam<>(0.0, Real.INSTANCE), new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
    }

    private static void setValue(IntScalarParam<?> p, int value) {
        p.valuesInput.setValue(value, p);
        p.initAndValidate();
    }

    private static void setValue(RealScalarParam<?> p, double value) {
        p.valuesInput.setValue(value, p);
        p.initAndValidate();
    }

    /** put the parameters in an MCMC state, as estimated ones are */
    private static void inState(beast.base.inference.StateNode... nodes) {
        beast.base.inference.State state = new beast.base.inference.State();
        state.initByName("stateNode", java.util.List.of(nodes));
        // as MCMC does when the run starts: this is what links the nodes to the state
        state.initialise();
        for (beast.base.inference.StateNode n : nodes) {
            assertNotNull(n.getState(), "not in the state: " + n);
        }
    }

    /** initByName wraps the IllegalArgumentException from initAndValidate in a RuntimeException */
    private static void assertDomainError(org.junit.jupiter.api.function.Executable e) {
        RuntimeException thrown = assertThrows(RuntimeException.class, e);
        Throwable cause = thrown;
        while (!(cause instanceof IllegalArgumentException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertTrue(cause.getMessage().contains("declares the domain"), cause.getMessage());
    }

    //*** OffsetInt ***//

    /** typed Java construction: a NonNegativeInt distribution, no cast */
    @Test
    public void testOffsetIntTyped() {
        OffsetInt<PositiveInt> o = new OffsetInt<>(PositiveInt.INSTANCE, poisson(), 1);
        assertSame(PositiveInt.INSTANCE, o.getParameterDomain());
        assertEquals(1, o.getLowerBoundOfParameter());
        // the declared domain does not change the density
        assertEquals(new OffsetInt<>(poisson(), 1).logDensity(3), o.logDensity(3), 1e-15);
    }

    /** Poisson - 1 starts at -1, Poisson + 0 at 0: neither fits PositiveInt */
    @Test
    public void testOffsetIntOutsideDomainIsAnError() {
        for (int offset : new int[]{-1, 0}) {
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> new OffsetInt<>(PositiveInt.INSTANCE, poisson(), offset), "offset " + offset);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            assertTrue(cause.getMessage().contains("declares the domain PositiveInt"), cause.getMessage());
            assertTrue(cause.getMessage().contains("below 1"), cause.getMessage());
        }
        // Poisson + 0 fits NonNegativeInt
        assertSame(NonNegativeInt.INSTANCE, new OffsetInt<>(NonNegativeInt.INSTANCE, poisson(), 0).getParameterDomain());
    }

    /** without a declared domain nothing changes: offset -1 is allowed, the domain reads Int */
    @Test
    public void testOffsetIntWithoutDomain() {
        OffsetInt<Int> o = new OffsetInt<>(poisson(), -1);
        assertSame(Int.INSTANCE, o.getParameterDomain());
        assertEquals(-1, o.getLowerBoundOfParameter());
    }

    /** an estimated offset that moves the support outside the declared domain gives -Infinity */
    @Test
    public void testOffsetIntEstimatedOffset() {
        IntScalarParam<Int> offset = new IntScalarParam<>(1, Int.INSTANCE);
        IntScalarParam<PositiveInt> x = new IntScalarParam<>(3, PositiveInt.INSTANCE);
        OffsetInt<PositiveInt> o = new OffsetInt<>();
        o.initByName("domain", PositiveInt.INSTANCE, "distribution", poisson(), "offset", offset, "param", x);
        inState(offset, x);
        double logP = o.calculateLogP();
        assertTrue(Double.isFinite(logP));
        setValue(offset, 0);
        assertEquals(Double.NEGATIVE_INFINITY, o.calculateLogP());
        setValue(offset, 1);
        assertEquals(logP, o.calculateLogP(), 1e-15);
    }

    /**
     * A fixed offset (not an estimated parameter in the state) cannot move the support, so it
     * is checked at initialisation only, not on every evaluation.
     */
    @Test
    public void testFixedOffsetNotCheckedEachEvaluation() {
        IntScalarParam<Int> offset = new IntScalarParam<>(1, Int.INSTANCE);
        IntScalarParam<PositiveInt> x = new IntScalarParam<>(3, PositiveInt.INSTANCE);
        OffsetInt<PositiveInt> o = new OffsetInt<>();
        o.initByName("domain", PositiveInt.INSTANCE, "distribution", poisson(), "offset", offset, "param", x);
        double logP = o.calculateLogP();
        // not in a state: changing it (which MCMC cannot do) is not checked per evaluation
        setValue(offset, 0);
        assertNotEquals(Double.NEGATIVE_INFINITY, o.calculateLogP());
        assertTrue(Double.isFinite(logP));
    }

    //*** TruncatedInt ***//

    @Test
    public void testTruncatedIntDeclaredDomain() {
        TruncatedInt<PositiveInt> t = new TruncatedInt<>();
        t.initByName("domain", PositiveInt.INSTANCE, "distribution", poisson(),
                "lower", new IntScalarParam<>(1, Int.INSTANCE), "upper", new IntScalarParam<>(10, Int.INSTANCE));
        assertSame(PositiveInt.INSTANCE, t.getParameterDomain());

        TruncatedInt<PositiveInt> t0 = new TruncatedInt<>();
        assertDomainError(() -> t0.initByName("domain", PositiveInt.INSTANCE,
                "distribution", poisson(),
                "lower", new IntScalarParam<>(0, Int.INSTANCE), "upper", new IntScalarParam<>(10, Int.INSTANCE)));
    }

    /** an estimated lower bound that moves to 0 under PositiveInt gives -Infinity */
    @Test
    public void testTruncatedIntEstimatedBound() {
        IntScalarParam<Int> lower = new IntScalarParam<>(1, Int.INSTANCE);
        IntScalarParam<PositiveInt> x = new IntScalarParam<>(3, PositiveInt.INSTANCE);
        TruncatedInt<PositiveInt> t = new TruncatedInt<>();
        t.initByName("domain", PositiveInt.INSTANCE, "distribution", poisson(),
                "lower", lower, "upper", new IntScalarParam<>(10, Int.INSTANCE), "param", x);
        inState(lower, x);
        assertTrue(Double.isFinite(t.calculateLogP()));
        setValue(lower, 0);
        assertEquals(Double.NEGATIVE_INFINITY, t.calculateLogP());
    }

    //*** OffsetReal and TruncatedReal ***//

    @Test
    public void testOffsetRealDeclaredDomain() {
        LogNormal logNormal = new LogNormal(null, new RealScalarParam<>(0.0, Real.INSTANCE),
                new RealScalarParam<>(1.0, PositiveReal.INSTANCE), false);
        OffsetReal<PositiveReal> o = new OffsetReal<>(PositiveReal.INSTANCE, logNormal, 1.0);
        assertSame(PositiveReal.INSTANCE, o.getParameterDomain());
        // a Normal shifted by 5 still reaches below 0
        assertThrows(RuntimeException.class, () -> new OffsetReal<>(PositiveReal.INSTANCE, standardNormal(), 5.0));
        // Exp(1) - 1 starts at -1
        Exponential exp = new Exponential(null, new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        assertThrows(RuntimeException.class, () -> new OffsetReal<>(PositiveReal.INSTANCE, exp, -1.0));
    }

    @Test
    public void testOffsetRealEstimatedOffset() {
        RealScalarParam<Real> offset = new RealScalarParam<>(1.0, Real.INSTANCE);
        RealScalarParam<PositiveReal> x = new RealScalarParam<>(3.0, PositiveReal.INSTANCE);
        Exponential exp = new Exponential(null, new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
        OffsetReal<PositiveReal> o = new OffsetReal<>();
        o.initByName("domain", PositiveReal.INSTANCE, "distribution", exp, "offset", offset, "param", x);
        inState(offset, x);
        assertTrue(Double.isFinite(o.calculateLogP()));
        setValue(offset, -0.5);
        assertEquals(Double.NEGATIVE_INFINITY, o.calculateLogP());
    }

    @Test
    public void testTruncatedRealDeclaredDomain() {
        TruncatedReal<PositiveReal> t = new TruncatedReal<>();
        t.initByName("domain", PositiveReal.INSTANCE, "distribution", standardNormal(),
                "lower", new RealScalarParam<>(0.0, Real.INSTANCE), "upper", new RealScalarParam<>(10.0, Real.INSTANCE));
        assertSame(PositiveReal.INSTANCE, t.getParameterDomain());

        TruncatedReal<PositiveReal> below = new TruncatedReal<>();
        assertDomainError(() -> below.initByName("domain", PositiveReal.INSTANCE,
                "distribution", standardNormal(),
                "lower", new RealScalarParam<>(-1.0, Real.INSTANCE), "upper", new RealScalarParam<>(10.0, Real.INSTANCE)));
    }

    //*** XML ***//

    private static final String POISSON = "<distribution spec='beast.base.spec.inference.distribution.Poisson'>"
            + "<lambda spec='beast.base.spec.inference.parameter.RealScalarParam' domain='NonNegativeReal' value='2.0'/>"
            + "</distribution>";

    private static Object parse(String domain, int offset) throws Exception {
        String xml = "<beast version='2.8'>"
                + "<distribution spec='beast.base.spec.inference.distribution.OffsetInt' domain='" + domain + "'>"
                + POISSON
                + "<offset spec='beast.base.spec.inference.parameter.IntScalarParam' domain='Int' value='" + offset + "'/>"
                + "</distribution></beast>";
        return new XMLParser().parseBareFragment(xml, true);
    }

    @Test
    public void testDomainFromXML() throws Exception {
        OffsetInt<?> o = (OffsetInt<?>) parse("PositiveInt", 1);
        assertSame(PositiveInt.INSTANCE, o.getParameterDomain());
        assertThrows(Exception.class, () -> parse("PositiveInt", -1));
    }
}
