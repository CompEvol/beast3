package beast.base.spec.inference.distribution;

import beast.base.core.Log;
import beast.base.inference.StateNode;
import beast.base.spec.Bounded;
import beast.base.spec.type.BoundUtils;

/**
 * Checks for the optional {@code domain} of {@link OffsetInt}, {@link TruncatedInt},
 * {@link OffsetReal} and {@link TruncatedReal}: the domain the wrapper declares for the
 * values it puts probability on, e.g. PositiveInt for Poisson + 1. The wrapper's support
 * must lie within it, so Offset(Poisson, -1) declared PositiveInt is an error.
 * <p>
 * When an input that sets the support (an offset, or a truncation bound) is estimated, the
 * support must lie within the domain for every value that input can take, not just its
 * starting value. Otherwise the model gives positive probability to values the parameter
 * cannot take. Giving such states log density -Infinity instead would restrict the
 * estimated input to the values that fit without normalising its prior: with offset ~
 * Poisson(1) and Offset(Poisson, offset) declared PositiveInt, the joint prior would only
 * sum to P(offset >= 1) ~ 0.63, biasing marginal likelihood estimates. So the range is
 * checked once all objects are initialised, on the first evaluation, from the estimated
 * input's own bounds (its domain, tightened by its prior), and a range that does not fit
 * is an error: give the input a prior that only allows values that fit.
 * <p>
 * An input calculated from other values (not a parameter) has no known range. Only then
 * is the support checked on every evaluation, with log density -Infinity outside the
 * domain, and a warning that this restriction is not normalised.
 */
final class DeclaredDomain {

    private DeclaredDomain() {}

    /**
     * How an input that sets the support can change during MCMC. Ordered so that, for two
     * inputs (truncation bounds), the later one decides how both are checked: once one is
     * ESTIMATED, MCMC has linked the state, so the other, if NOT_YET_KNOWN, is a constant.
     */
    enum Range {
        /** a constant, or a parameter not estimated */
        FIXED,
        /**
         * a parameter not (yet) in a state: a constant, or one MCMC has not linked to the
         * state yet. Its current value is used, and it is asked again on the next evaluation.
         */
        NOT_YET_KNOWN,
        /** a parameter estimated in the state: it stays within its bounds (getLower()/getUpper()) */
        ESTIMATED,
        /** calculated from other values: its range is not known in advance */
        CALCULATED
    }

    static Range range(Object input) {
        if (input == null) {
            return Range.FIXED;
        }
        if (input instanceof StateNode node) {
            if (!node.isEstimatedInput.get()) {
                return Range.FIXED;
            }
            // not in a state: a constant, or MCMC has not linked it to the state yet
            return node.getState() == null ? Range.NOT_YET_KNOWN : Range.ESTIMATED;
        }
        return Range.CALCULATED;
    }

    /**
     * @return null when [lower, upper] lies within the domain, otherwise where it does not,
     * e.g. "below 1" (see {@link BoundUtils#supportOutsideDomain})
     */
    static String outside(Bounded<?> domain, Object lower, Object upper) {
        return BoundUtils.supportOutsideDomain(domain, lower, upper);
    }

    /**
     * Checks the support at initialisation, for the inputs' current values.
     * @throws IllegalArgumentException when [lower, upper] reaches outside the domain
     */
    static void check(Object distribution, String id, Bounded<?> domain, Object lower, Object upper) {
        String outside = outside(domain, lower, upper);
        if (outside != null) {
            throw new IllegalArgumentException(name(distribution, id) + " declares the domain "
                    + domain.getClass().getSimpleName() + ", but its support, [" + lower + ", " + upper
                    + "], reaches " + outside + ".");
        }
    }

    /**
     * Checks the widest support that estimated inputs allow, given their bounds.
     * @param inputs the estimated inputs, e.g. "offset" or "lower and upper bounds"
     * @throws IllegalArgumentException when [lower, upper] reaches outside the domain
     */
    static void checkEstimated(Object distribution, String id, Bounded<?> domain, Object lower, Object upper, String inputs) {
        String outside = outside(domain, lower, upper);
        if (outside != null) {
            throw new IllegalArgumentException(name(distribution, id) + " declares the domain "
                    + domain.getClass().getSimpleName() + ", but its estimated " + inputs + " can move its support"
                    + " to [" + lower + ", " + upper + "], which reaches " + outside + ". An estimated parameter ranges"
                    + " over its domain, tightened by its prior: give the " + inputs + " a prior (or domain) that keeps"
                    + " the support within " + domain.getClass().getSimpleName() + ".");
        }
    }

    /** warn, once per distribution, that a calculated input is checked on every evaluation */
    static void warnCalculated(Object distribution, String id, String input) {
        Log.warning(name(distribution, id) + " declares a domain, but its " + input + " is calculated, so its range"
                + " is not known in advance. States where it puts the support outside the declared domain get"
                + " log density -Infinity: this restricts the " + input + " without normalising its prior, which"
                + " biases marginal likelihood estimates.");
    }

    private static String name(Object distribution, String id) {
        return distribution.getClass().getSimpleName() + (id == null ? "" : " '" + id + "'");
    }

    /** x + d, keeping Integer.MIN_VALUE and MAX_VALUE as "no limit", saturating instead of wrapping around */
    static int add(int x, int d) {
        if (x == Integer.MIN_VALUE || x == Integer.MAX_VALUE) {
            return x;
        }
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) x + d));
    }
}
