package beast.base.inference;

import beast.base.core.Description;

/**
 * A {@code Scalable} represents a state component that can be moved along a
 * single positive-real dilation axis. The interface defines three operations
 * that must be mutually consistent &mdash; together they form the
 * <em>Scalable contract</em>:
 *
 * <ol>
 *     <li>{@link #scale(double)} dilates the component by a factor {@code s}
 *         and returns the log Jacobian determinant of that move
 *         (i.e. {@code log |det(∂new/∂old)|}).</li>
 *     <li>{@link #getScalableValue()} reads the component's current position
 *         on its dilation axis.</li>
 *     <li>{@link #setScalableValue(double)} moves the component so that
 *         {@code getScalableValue()} returns the supplied target {@code V}, and
 *         returns the log Jacobian for that move.</li>
 * </ol>
 *
 * <p>The log Jacobian is the move's contribution to the Metropolis-Hastings
 * acceptance ratio from the change-of-variables formula. The proposal density
 * ratio (the "Hastings ratio" proper, {@code q(reverse)/q(forward)}) lives in
 * the calling operator's kernel and is <em>not</em> part of the Scalable's
 * return.</p>
 *
 * <p>The contract requires the following three invariants to hold for any
 * valid {@code Scalable x} and any positive scale factor {@code s}:</p>
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
 *   // (3) set composes with scale
 *   //     x.setScalableValue(x.getScalableValue() * s)
 *   //     produces the same state as
 *   //     x.scale(s)
 * </pre>
 *
 * <p>The choice of dilation axis (and therefore the meaning of
 * {@code getScalableValue}) is bound to the implementation of {@code scale}.
 * For example, an affine-scaling parameter exposes its value directly. A tree
 * whose {@code scale} is interval-scaling exposes its sum-of-margins. A custom
 * {@code Scalable} chooses whichever summary is exactly multiplied by {@code s}
 * under its own {@code scale} operation.</p>
 *
 * <p>{@code scale(s)} is expected to succeed for any positive {@code s} that
 * leaves the component in a valid state. Implementations may throw
 * {@link IllegalArgumentException} for moves that produce an invalid state;
 * such throws act as rejection signals for the calling operator. The contract
 * invariants apply when {@code scale} does not throw.</p>
 *
 * @see <a href="https://github.com/CompEvol/beast3/issues/20">beast3 issue #20</a>
 */
@Description("State component that can be dilated along a 1-D axis by scale or up-down operators.")
public interface Scalable {

    /**
     * Dilate this component by factor {@code s} along its scaling axis.
     * After the call, {@link #getScalableValue()} returns
     * {@code s * (its previous value)}.
     *
     * @param s positive scale factor
     * @return log Jacobian determinant of this move
     * @throws IllegalArgumentException if the move would produce an invalid state
     */
    double scale(double s);

    /**
     * Read the component's current position on its dilation axis.
     * The contract requires this to be exactly {@code s}-equivariant under
     * {@link #scale(double)}.
     */
    double getScalableValue();

    /**
     * Move the component so that {@link #getScalableValue()} returns {@code V}.
     * Defined as {@code scale(V / getScalableValue())}.
     * <p>
     * Implementations rarely need to override this; the default expresses the
     * contract identity {@code setScalableValue(V) ≡ scale(V / getScalableValue())}
     * directly. Override only if the dilation axis cannot be reached by a
     * single multiplicative scale (rare).
     *
     * @param V target value (must be positive for typical multiplicative axes)
     * @return log Jacobian determinant of this move
     * @throws IllegalArgumentException if the current value is zero (no
     *     multiplicative scale can land at {@code V}) or if the resulting
     *     state would be invalid
     */
    default double setScalableValue(double V) {
        double current = getScalableValue();
        if (current == 0.0) {
            throw new IllegalArgumentException(
                    "Cannot set scalable value: current value is zero "
                    + "(no multiplicative scale lands at " + V + ")");
        }
        return scale(V / current);
    }

}
