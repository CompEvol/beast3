package beast.base.spec.inference.distribution;



import beast.base.core.Description;
import beast.base.inference.Distribution;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.DiscreteDistribution;

/**
 * Strong typed {@link Distribution} for {@link Scalar}.
 * @param <S> the shape for sampled value, which could be {@link Scalar}
 * @param <T> the Java primitive type for sampled value, either Double or Integer.
 */
@Description("The BEAST Distribution over a scalar.")
public abstract class ScalarDistribution<S extends Scalar<?,T>, T>
        extends TensorDistribution<S,T> {		

    //*** abstract methods ***//

    /**
     * Used by {@link IID} when computing the log probability/density from the base distribution.
     * @param value T in Java type
     * @return  the normalised probability (density) for this distribution.
     */
    protected double calcLogP(T value) {
        Object dist = getApacheDistribution();

        if (dist == null) {
            throw new RuntimeException("not implemented yet");
        }

        if (dist instanceof ContinuousDistribution cd) {
//TODO Remco review:  return cd.density(y);
            final double x = ((Number) value).doubleValue();
            return cd.logDensity(x);
        } else  if (dist instanceof DiscreteDistribution dd) {
//TODO Remco review:  return dd.probability((int) y);
            final int x = ((Number) value).intValue();
            return dd.logProbability(x);
        }
        return 0.0;
    }

    @Override
    protected double calcLogP(T... value) {
        throw new IllegalArgumentException("Illegal operation !");
    }

    @Override
    public double calculateLogP() {
        // refresh(); has been called in getApacheDistribution();
        logP = calcLogP(param.get());
        return logP;
    }

    /**
     * @return true if the distribution is an integer distribution
     * false if it is a continuous distribution
     */
    public boolean isIntegerDistribution() {
    	return false;
    }

    /**
     * Return the probability density for a particular point.
     *
     * @param x The point at which the density should be computed.
     * @return The pdf at point x.
     */
    public double density(double x) {
    	Object dist = getApacheDistribution();
    	
        if (dist == null) {
       	 throw new RuntimeException("not implemented yet");
        }
        
    	if (dist instanceof ContinuousDistribution cd) {
    		return cd.density(x);
    	} else  if (dist instanceof DiscreteDistribution dd) {
    		return dd.probability((int) x);
    	}
    	return 0.0;
    }

    /**
     * @see #density(double)
     * @param x The point at which the density should be computed.
     * @return The log-scale pdf at point x.
     */
    public double logDensity(double x) {
    	return Math.log(density(x));
    }


    public double cumulativeProbability(double x) {
        // Attempt to get the Apache distribution
        Object dist = getApacheDistribution();
        if (dist == null) {
            refresh();
            dist = getApacheDistribution();
        }

        // If there is no Apache distribution, subclass needs to implement CDF manually
        if (dist == null) {
            throw new RuntimeException("not implemented yet");
        }
         
        // Compute CDF based on distribution type
        if (dist instanceof ContinuousDistribution cd) {
            return cd.cumulativeProbability(x);
        } else if (dist instanceof DiscreteDistribution dd) {
            return dd.cumulativeProbability((int) x);
        }
        throw new RuntimeException("Unknown distribution type");
    }

    /**
     * @return org.apache.commons.statistics.distribution.ContinuousDistribution or 
     *    org.apache.commons.statistics.distribution.DiscreteDistribution if available
     *    or null otherwise
     *    
     * Should only be used inside ScalarDistribution -- for external access use the
     * rest of the ScalarDistribution methods.
     */
    protected Object getApacheDistribution() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
    	return null;
    }

    /**
     * For this distribution, X, this method returns x such that P(X &lt; x) = p.
     *
     * @param p the cumulative probability.
     * @return x.
     * @throws RuntimeException if the inverse cumulative probability can not be
     *                       computed due to convergence or other numerical errors.
     */
    public T inverseCumulativeProbability(double p) {
        if (p <= 0) {
        	return getLowerBoundOfParameter();
        } else if (p >= 1) {
            return getUpperBoundOfParameter();
        }

         Object dist = getApacheDistribution();
         
         if (dist == null) {
        	 refresh();
        	 dist = getApacheDistribution();
         }
         if (dist == null) {
        	 throw new RuntimeException("not implemented yet");
         }
         
         if (dist instanceof ContinuousDistribution cd) {
             return (T) Double.valueOf(cd.inverseCumulativeProbability(p));
         } else if (dist instanceof DiscreteDistribution dd) {
             return (T) Integer.valueOf(dd.inverseCumulativeProbability(p));
         }
         throw new RuntimeException("Unknown distribution type");
     }
     
     @Override
     public T getLowerBoundOfParameter() {
         Object dist = getApacheDistribution();
         
         if (dist == null) {
        	 refresh();
        	 dist = getApacheDistribution();
         }
         if (dist == null) {
        	 throw new RuntimeException("not implemented yet");
         }
         
         if (dist instanceof ContinuousDistribution cd) {
             Double lower = cd.getSupportLowerBound();
             return (T) lower;
         } else if (dist instanceof DiscreteDistribution dd) {
             Integer lower = dd.getSupportLowerBound();
             return (T) lower;
         }
    	 return null;
     }
     
     @Override
     public T getUpperBoundOfParameter() {
         Object dist = getApacheDistribution();
         
         if (dist == null) {
        	 refresh();
        	 dist = getApacheDistribution();
         }
         if (dist == null) {
        	 throw new RuntimeException("not implemented yet");
         }
         
         if (dist instanceof ContinuousDistribution cd) {
             Double lower = cd.getSupportUpperBound();
             return (T) lower;
         } else if (dist instanceof DiscreteDistribution dd) {
             Integer lower =  dd.getSupportUpperBound();
             return (T) lower;
         }
    	 return null;
     }
     
    /** returns mean of distribution, if implemented 
     **/
     public double getMean() {
    	 Object dist = getApacheDistribution();
    	 
         if (dist == null) {
        	 throw new RuntimeException("not implemented yet");
         }
         
    	 if (dist instanceof ContinuousDistribution cd) {
    		 return cd.getMean();
    	 }
    	 throw new IllegalArgumentException("not implemented yet");
     }

     
     public boolean isCompatible(Tensor<?,?> t) {
    	 Class thisType = paramInput.getType();
    	 return  (t.getClass().isAssignableFrom(thisType));
     }
     
}

