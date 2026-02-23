package beast.base.spec.type;

import beast.base.core.BEASTInterface;
import beast.base.spec.inference.distribution.TensorDistribution;

import java.util.List;

public final class BoundUtils {

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
