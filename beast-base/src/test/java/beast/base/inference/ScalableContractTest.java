package beast.base.inference;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reusable test helper that asserts the three {@link Scalable} contract
 * invariants on a supplied implementation.
 *
 * <p>The contract:</p>
 *
 * <pre>
 *   // (1) scale-equivariance
 *   double v0 = x.getScalableValue();
 *   x.scale(s);
 *   assert x.getScalableValue() == s * v0;
 *
 *   // (2) set is a fixed point of get
 *   x.setScalableValue(V);
 *   assert x.getScalableValue() == V;
 *
 *   // (3) set ∘ get×s ≡ scale
 *   //     x.setScalableValue(x.getScalableValue() * s)
 *   //     produces the same state as
 *   //     x.scale(s)
 * </pre>
 *
 * <p>Per-class tests should call {@link #assertContract} with a factory that
 * produces a fresh, identically-initialised instance on each call, and an
 * {@code assertSameState} comparator that checks full internal state (not just
 * the dilation summary).</p>
 */
public class ScalableContractTest {

    public static final double EPSILON = 1e-9;

    /**
     * Assert the three contract invariants on instances produced by {@code factory}
     * with scale factor {@code s} and a state-comparison callback.
     *
     * @param factory          produces a fresh Scalable on each call (state must be deterministic)
     * @param s                positive scale factor to test (different from 1.0)
     * @param assertSameState  asserts that two Scalables have identical internal state
     */
    public static <T extends Scalable> void assertContract(
            Supplier<T> factory,
            double s,
            BiConsumer<T, T> assertSameState) {

        // (1) scale-equivariance: getScalableValue() after scale(s) equals s × original
        T a = factory.get();
        double v0 = a.getScalableValue();
        double logJacobian = a.scale(s);
        assertEquals(s * v0, a.getScalableValue(), Math.abs(s * v0) * EPSILON + EPSILON,
                "scale(" + s + ") should make getScalableValue() return s × " + v0
                + " = " + (s * v0) + ", got " + a.getScalableValue());
        // sanity: logJacobian should be finite and have correct sign for nontrivial moves
        assertEquals(true, Double.isFinite(logJacobian),
                "scale should return a finite log Jacobian, got " + logJacobian);

        // (2) set is a fixed point of get: getScalableValue() after setScalableValue(V) equals V
        T b = factory.get();
        double v1 = b.getScalableValue();
        double targetV = v1 * 1.7;
        b.setScalableValue(targetV);
        assertEquals(targetV, b.getScalableValue(), Math.abs(targetV) * EPSILON + EPSILON,
                "setScalableValue(" + targetV + ") should make getScalableValue() return "
                + targetV + ", got " + b.getScalableValue());

        // (3) set ∘ get×s ≡ scale: the two paths produce the same internal state
        T c = factory.get();
        double vc = c.getScalableValue();
        c.scale(s);

        T d = factory.get();
        d.setScalableValue(vc * s);

        assertSameState.accept(c, d);
    }

    /**
     * Convenience overload that asserts the contract for several scale factors
     * including values both less than and greater than 1.0.
     */
    public static <T extends Scalable> void assertContractAcrossScales(
            Supplier<T> factory,
            BiConsumer<T, T> assertSameState) {
        for (double s : new double[] { 0.5, 0.9, 1.1, 1.5, 2.0, 3.7 }) {
            assertContract(factory, s, assertSameState);
        }
    }
}
