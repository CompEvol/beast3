package beast.base.spec.type;

import beast.base.core.BEASTInterface;
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

}
