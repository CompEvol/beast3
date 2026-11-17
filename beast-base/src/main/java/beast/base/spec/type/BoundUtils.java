package beast.base.spec.type;

import beast.base.core.BEASTInterface;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.distribution.TensorDistribution;

import java.util.List;

/**
 * Utility methods for tightening parameter bounds based on attached distributions.
 * Used by the bounded type interfaces ({@link RealScalar}, {@link RealVector},
 * {@link IntScalar}, {@link IntVector}) to compute effective bounds that incorporate
 * constraints imposed by {@link TensorDistribution} priors.
 */
public final class BoundUtils {

    /**
     * Tightens a lower bound for a {@code Double} parameter using the distribution's
     * support lower bound, if the parameter is an argument of the distribution.
     *
     * @param current the current lower bound
     * @param dist    the distribution that may impose a tighter bound
     * @param b       the parameter whose bound is being updated
     * @return the tightened lower bound (the maximum of current and distribution bound)
     */
    public static Double updateLower(Double current, TensorDistribution dist, BEASTInterface b) {
        if (dist.isAwaitingInit()) {
            // created by the XML parser, whose bound inputs may not be initialised yet (#286);
            // it checks the bounds itself when it is initialised
            return current;
        }
        List<String> arguments = dist.getArguments();
        if (arguments.contains(b.getID()) && b.getID() != null) {
            try {
                return Math.max(current, (Double) dist.getLowerBoundOfParameter());
            } catch (Throwable e) {
                // ignore
            }
        }
        return current;
    }

    /**
     * Tightens an upper bound for a {@code Double} parameter using the distribution's
     * support upper bound, if the parameter is an argument of the distribution.
     *
     * @param current the current upper bound
     * @param dist    the distribution that may impose a tighter bound
     * @param b       the parameter whose bound is being updated
     * @return the tightened upper bound (the minimum of current and distribution bound)
     */
    public static Double updateUpper(Double current, TensorDistribution dist, BEASTInterface b) {
        if (dist.isAwaitingInit()) {
            // created by the XML parser, whose bound inputs may not be initialised yet (#286);
            // it checks the bounds itself when it is initialised
            return current;
        }
        List<String> arguments = dist.getArguments();
        if (arguments.contains(b.getID()) && b.getID() != null) {
            try {
                return Math.min(current, (Double) dist.getUpperBoundOfParameter());
            } catch (Throwable e) {
                // ignore
            }
        }
        return current;
    }

    /**
     * Tightens a lower bound for an {@code Integer} parameter using the distribution's
     * support lower bound, if the parameter is an argument of the distribution.
     *
     * @param current the current lower bound
     * @param dist    the distribution that may impose a tighter bound
     * @param b       the parameter whose bound is being updated
     * @return the tightened lower bound (the maximum of current and distribution bound)
     */
    public static Integer updateLower(Integer current, TensorDistribution dist, BEASTInterface b) {
        if (dist.isAwaitingInit()) {
            // created by the XML parser, whose bound inputs may not be initialised yet (#286);
            // it checks the bounds itself when it is initialised
            return current;
        }
        List<String> arguments = dist.getArguments();
        if (arguments.contains(b.getID()) && b.getID() != null) {
            try {
                return Math.max(current, (Integer) dist.getLowerBoundOfParameter());
            } catch (Throwable e) {
                // ignore
            }
        }
        return current;
    }

    /**
     * Tightens an upper bound for an {@code Integer} parameter using the distribution's
     * support upper bound, if the parameter is an argument of the distribution.
     *
     * @param current the current upper bound
     * @param dist    the distribution that may impose a tighter bound
     * @param b       the parameter whose bound is being updated
     * @return the tightened upper bound (the minimum of current and distribution bound)
     */
    public static Integer updateUpper(Integer current, TensorDistribution dist, BEASTInterface b) {
        if (dist.isAwaitingInit()) {
            // created by the XML parser, whose bound inputs may not be initialised yet (#286);
            // it checks the bounds itself when it is initialised
            return current;
        }
        List<String> arguments = dist.getArguments();
        if (arguments.contains(b.getID()) && b.getID() != null) {
            try {
                return Math.min(current, (Integer) dist.getUpperBoundOfParameter());
            } catch (Throwable e) {
                // ignore
            }
        }
        return current;
    }

    /**
     * Describes where a distribution's support reaches outside a parameter's domain,
     * e.g. Poisson (support from 0) on a PositiveInt parameter, or Normal on a
     * PositiveReal one. The parameter cannot take those values, so the prior is not
     * normalised over the values it can take: the missing mass is a constant while the
     * distribution's own parameters are fixed, but it biases them when they are
     * estimated, and it biases marginal likelihood estimates either way.
     * <p>
     * Called at initialisation only, so it sees the current values of the distribution's
     * inputs: with an estimated bound, e.g. Bounded(Poisson, [L, 10]) on a PositiveInt
     * parameter, it does not warn when L starts at 1 although L may move to 0 later.
     * <p>
     * Integer bounds are compared as inclusive bounds, so a domain whose lower bound 0
     * is exclusive (PositiveInt) starts at 1. Real bounds that are equal fit whether or
     * not they are inclusive, since a single point carries no mass.
     *
     * @param domainBounds the bounds of the parameter's domain
     * @param supportLower the lower bound of the distribution's support (inclusive)
     * @param supportUpper the upper bound of the distribution's support (inclusive)
     * @return null when the support lies within the domain, otherwise a description
     *         such as "below 1" or "above 1.0"
     */
    public static String supportOutsideDomain(Bounded<?> domainBounds, Object supportLower, Object supportUpper) {
        String below = null;
        String above = null;
        if (supportLower instanceof Integer lower && domainBounds instanceof Int intDomain) {
            int first = intDomain.getInclusiveLower();
            if (lower < first) {
                below = "below " + first;
            }
        } else if (supportLower instanceof Double lower && domainBounds.getLower() instanceof Double domainLower) {
            if (lower < domainLower) {
                below = "below " + domainLower;
            }
        }
        if (supportUpper instanceof Integer upper && domainBounds instanceof Int intDomain) {
            int last = intDomain.getInclusiveUpper();
            if (upper > last) {
                above = "above " + last;
            }
        } else if (supportUpper instanceof Double upper && domainBounds.getUpper() instanceof Double domainUpper) {
            if (upper > domainUpper) {
                above = "above " + domainUpper;
            }
        }
        if (below != null && above != null) {
            return below + " and " + above;
        }
        return below != null ? below : above;
    }

}
