package beast.base.spec.type;

import beast.base.core.BEASTInterface;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.ScalarDistribution;

import java.util.List;

public interface RealScalar<D extends Real> extends Scalar<D, Double>, Bounded<Double> {

    /**
     * Get a single value.
     *
     * @return the unboxed domain value, which is faster than boxed.
     */
    double get();

    /**
     * Use boxed value T only when required for API or nullability.
     *
     * @param idx  index/indices, depending on the dimension.
     * @return  the boxed domain value.
     */
    default Double get(int... idx) {
        return get();
    }

    @Override
    default Double getLower() {
        D domain = getDomain();
        Double lower = domain.getLower();
        if (this instanceof BEASTInterface b) {
        	for (BEASTInterface o : b.getOutputs()) {
        		if (o instanceof ScalarDistribution d) {
        			List<String> arguments = d.getArguments();
        			if (arguments.contains(b.getID()) && b.getID() != null) {
        				try {
        					lower = Math.max(lower, (Double) d.getLower());
        				} catch (Throwable e) {
        					// ignore
        				}
        			}
        		}
        	}
        }
        return lower;
    }

    @Override
    default Double getUpper() {
        D domain = getDomain();
        Double upper = domain.getUpper();
        if (this instanceof BEASTInterface b) {
        	for (BEASTInterface o : b.getOutputs()) {
        		if (o instanceof ScalarDistribution d) {
        			List<String> arguments = d.getArguments();
        			if (arguments.contains(b.getID()) && b.getID() != null) {
        				try {
        					upper = Math.min(upper, (Double) d.getUpper());
        				} catch (Throwable e) {
        					// ignore
        				}
        			}
        		}
        	}
        }
        return upper;
    }

    @Override
    default boolean lowerInclusive() {
        return true;
    }

    @Override
    default boolean upperInclusive() {
        return true;
    }

    @Override
    default boolean isValid(Double value) {
        // 1st check domain constraints, 2nd check if value is in the real scalar range
        // Note: these bounds can be the subset of domain bounds.
        return Scalar.super.isValid(value) && withinBounds(value);
    }
    
}