package beast.base.spec.inference.distribution;

import beast.base.inference.StateNode;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.Real;

/**
 * Checks for the optional {@code domain} of {@link OffsetInt}, {@link TruncatedInt},
 * {@link OffsetReal} and {@link TruncatedReal}: the domain the wrapper declares for the
 * values it puts probability on, e.g. PositiveInt for Poisson + 1. The wrapper's support
 * must lie within it, so Offset(Poisson, -1) declared PositiveInt is an error.
 */
final class DeclaredDomain {

    private DeclaredDomain() {}

    /**
     * @param domain the declared domain
     * @param lower  the smallest value of the support (inclusive)
     * @param upper  the largest value of the support (inclusive)
     * @return null when [lower, upper] lies within the domain, otherwise where it does not,
     *         e.g. "from 0, below 1"
     */
    static String outside(Int domain, int lower, int upper) {
        // integer bounds compared inclusively: PositiveInt is stored as exclusive 0, so starts at 1
        long first = domain.lowerInclusive() ? domain.getLower() : (long) domain.getLower() + 1;
        long last = domain.upperInclusive() ? domain.getUpper() : (long) domain.getUpper() - 1;
        String below = lower < first ? "from " + lower + ", below " + first : null;
        String above = upper > last ? "up to " + upper + ", above " + last : null;
        return join(below, above);
    }

    /**
     * @return null when [lower, upper] lies within the domain, otherwise where it does not.
     * Equal real bounds fit whether or not the domain's bound is inclusive, since a single
     * point carries no probability (LogNormal from 0 fits PositiveReal).
     */
    static String outside(Real domain, double lower, double upper) {
        String below = lower < domain.getLower() ? "from " + lower + ", below " + domain.getLower() : null;
        String above = upper > domain.getUpper() ? "up to " + upper + ", above " + domain.getUpper() : null;
        return join(below, above);
    }

    private static String join(String below, String above) {
        if (below != null && above != null) {
            return below + " and " + above;
        }
        return below != null ? below : above;
    }

    /**
     * Whether an input that sets the wrapper's support (an offset, or a truncation bound)
     * can change during MCMC: a parameter in the state with estimate="true", or anything
     * that is not a parameter, such as a value calculated from parameters. Only then is the
     * support checked on every evaluation; otherwise the check at initialisation suffices.
     * (A support that moves through the inner distribution's own estimated bounds, e.g. an
     * inner Uniform with an estimated upper, is checked at initialisation only.)
     */
    static boolean canMove(Object input) {
        if (input == null) {
            return false;
        }
        if (input instanceof StateNode node) {
            return node.getState() != null && node.isEstimatedInput.get();
        }
        return true;
    }

    /** the message for a support outside the declared domain */
    static String message(Object distribution, String id, Object domain, String outside) {
        return distribution.getClass().getSimpleName() + (id == null ? "" : " '" + id + "'")
                + " declares the domain " + domain.getClass().getSimpleName()
                + ", but its support reaches outside it (" + outside + ").";
    }
}
