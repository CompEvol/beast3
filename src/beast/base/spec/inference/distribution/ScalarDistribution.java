package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;
import org.apache.commons.math.MathException;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.DiscreteDistribution;

/**
 * Strong typed {@link Distribution} for {@link Scalar}.
 * @param <S> the shape for sampled value, which could be {@link Scalar}
 * @param <T> the Java primitive type for sampled value, either Double or Integer.
 */
@Description("The BEAST Distribution over a tensor.")
public abstract class ScalarDistribution<S extends Scalar<?,T>, T>
        extends TensorDistribution<S,T> {

	public final Input<Double> offsetInput = new Input<>("offset", "offset of origin (defaults to 0)", 0.0);
		

    //*** abstract methods ***//

    /**
     * Used by {@link IID} when computing the log probability/density from the base distribution.
     * @param value T in Java type
     * @return  the normalised probability (density) for this distribution.
     */
    protected abstract double calcLogP(T value);

    @Override
    protected double calcLogP(T... value) {
        throw new IllegalArgumentException("Illegal operation !");
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
     * NB this does not take offset in account
     *
     * @param x The point at which the density should be computed.
     * @return The pdf at point x.
     */
    public double density(double x) {
    	Object dist = getApacheDistribution();
    	
        if (dist == null) {
       	 throw new RuntimeException("not implemented yet");
        }
        
    	double y = x - getOffset();
    	if (dist instanceof ContinuousDistribution cd) {
    		return cd.density(y);
    	} else  if (dist instanceof DiscreteDistribution dd) {
    		return dd.probability((int) y);
    	}
    	return 0.0;
    }

    public double logDensity(double x) {
    	return Math.log(density(x));
    }

    /**
     * @return org.apache.commons.statistics.distribution.ContinuousDistribution or 
     *    org.apache.commons.statistics.distribution.DiscreteDistribution if available
     *    or null otherwise
     */
    public Object getApacheDistribution() {
    	return null;
    }

    /**
     * For this distribution, X, this method returns x such that P(X &lt; x) = p.
     *
     * @param p the cumulative probability.
     * @return x.
     * @throws MathException if the inverse cumulative probability can not be
     *                       computed due to convergence or other numerical errors.
     */
     public double inverseCumulativeProbability(double p) throws MathException {
         Object dist = getApacheDistribution();
         
         if (dist == null) {
        	 throw new RuntimeException("not implemented yet");
         }
         
         double offset = getOffset();
         if (dist instanceof ContinuousDistribution cd) {
             return offset + cd.inverseCumulativeProbability(p);
         } else if (dist instanceof DiscreteDistribution dd) {
             return offset + dd.inverseCumulativeProbability(p);
         }
         return 0.0;
     }

     
    /** returns mean of distribution, if implemented 
     * taking offset (if any) in account 
     **/
     public double getMean() {
    	 Object dist = getApacheDistribution();
    	 
         if (dist == null) {
        	 throw new RuntimeException("not implemented yet");
         }
         
    	 if (dist instanceof ContinuousDistribution cd) {
    		 double offset = getOffset();
    		 return offset + cd.getMean();
    	 }
    	 throw new IllegalArgumentException("not implemented yet");
     }

     /**
      * @return  offset of distribution.
      */
     public double getOffset() {
         return offsetInput.get();
     }

     
     public boolean isCompatible(Tensor<?,?> t) {
    	 Class thisType = paramInput.getType();
    	 return  (t.getClass().isAssignableFrom(thisType));
     }
     
}

