	package beast.base.spec.inference.distribution;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.MathException;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.*;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.spec.Bounded;
import beast.base.spec.parameter.IntScalarParam;
import beast.base.spec.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.RealVector;


@Description("Produces prior (log) probability of value x." +
        "If x is multidimensional, the components of x are assumed to be independent, " +
        "so the sum of log probabilities of all elements of x is returned as the prior.")
public class Prior extends Distribution {
    final public Input<RealScalar<?>> scalarInput = new Input<>("scalar", "point at which the density is calculated");
    final public Input<RealVector<?>> vectorInput = new Input<>("vector", "point at which the density is calculated");
    final public Input<ParametricDistribution> distInput = new Input<>("distr", "distribution used to calculate prior, e.g. normal, beta, gamma.", Validate.REQUIRED);

    /**
     * shadows distInput *
     */
    protected ParametricDistribution dist;

    @Override
    public void initAndValidate() {
        dist = distInput.get();
        
        int inputCount = ((scalarInput.get() != null) ? 1 : 0) +
						 ((vectorInput.get() != null) ? 1 : 0);
        if (inputCount != 1) {
        	throw new IllegalArgumentException("Exactly one of 'x', 'scalar' and 'vector' must be specified, not " + inputCount);
        }
        
        calculateLogP();
    }

    @Override
    public double calculateLogP() {
        logP = 0;
        
        if (scalarInput.get() != null) {
        	RealScalar<?> scalar = scalarInput.get();
        	if (scalar instanceof Bounded b) {
        		if (!b.withinBounds(scalar.get())) {
    	            logP = Double.NEGATIVE_INFINITY;        			
    	            return logP;
        		}
        	}
			Constant c = new Constant(scalar.get()+"");
			logP = dist.calcLogP(c);
        }
        
        if (vectorInput.get() != null) {
        	RealVector<?> vector = vectorInput.get();
        	if (vector instanceof Bounded b) {
        		for (int i = 0; i < vector.size(); i++) {
        			if (!b.withinBounds(vector.get(i))) {
        				logP = Double.NEGATIVE_INFINITY;
        				return logP;
        			}
        		}
        	}
    		for (int i = 0; i < vector.size(); i++) {
    			Constant c = new Constant(vector.get(i)+"");
    			logP += dist.calcLogP(c);
    		}
        }
        return logP;
    }

    /**
     * return name of the parameter this prior is applied to *
     */
    public String getParameterName() {
        if (scalarInput.get() != null) {
        	RealScalar<?> scalar = scalarInput.get();
        	return ((BEASTInterface) scalar).getID();
        }
    	RealVector<?> vector = vectorInput.get();
    	return ((BEASTInterface) vector).getID();
    }

    @Override
    public void sample(State state, Random random) {

        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled
        sampleConditions(state, random);

        // sample distribution parameters
        Double[] newx;
        try {
        	newx = dist.sample(1)[0];

            
            if (scalarInput.get() != null) {
            	RealScalar<?> scalar = scalarInput.get();
            	if (scalar instanceof Bounded b) {
            		while (!b.withinBounds(newx[0])) {
            			newx = dist.sample(1)[0];
            		}
            	}
            	if (scalar instanceof RealScalarParam rs) {
            		rs.set(newx[0]);
            		return;
            	}
            	if (scalar instanceof IntScalarParam is) {
            		is.set((int) (double) newx[0]);
            		return;
            	}
            	throw new RuntimeException("sample is not implemented yet for scalar that is not a RealScalarParam or IntScalarParam");
            }
            
            if (vectorInput.get() != null) {
            	throw new RuntimeException("sample is not implemented yet for vector parameters");
            }
            
            
//            if (newx.length == x.getDimension()) {            	
//	            if (x instanceof RealParameter) {
//	            	RealParameter p = (RealParameter) x;
//	                for (int i = 0; i < newx.length; i++) {
//	                	while (p.getLower() > newx[i] || p.getUpper() < newx[i]) {
//	                		newx = dist.sample(1)[0];
//	                	}
//	                    ((RealParameter) x).setValue(i, newx[i]);
//	                }
//	            } else if (x instanceof IntegerParameter) {
//	                IntegerParameter p = (IntegerParameter) x;
//	                for (int i = 0; i < newx.length; i++) {
//	                	while (p.getLower() > newx[i] || p.getUpper() < newx[i]) {
//	                		newx = dist.sample(1)[0];
//	                	}
//	                    p.setValue(i, (int)Math.round(newx[i]));
//	                }
//	            }
//            } else if (newx.length == 1) {
//            	// assume it is a multi dimensional distribution with iid components
//            	for (int k = 0; k < x.getDimension(); k++) {
//		            if (x instanceof RealParameter) {
//		            	RealParameter p = (RealParameter) x;
//		            	while (p.getLower() > newx[0] || p.getUpper() < newx[0]) {
//		            		newx = dist.sample(1)[0];
//		            	}
//		                p.setValue(k, newx[0]);
//		            } else if (x instanceof IntegerParameter) {
//		                IntegerParameter p = (IntegerParameter) x;
//		            	while (p.getLower() > newx[0] || p.getUpper() < newx[0]) {
//		            		newx = dist.sample(1)[0];
//		            	}
//		                p.setValue(k, (int)Math.round(newx[0]));
//		            }
//		            if (k < x.getDimension()-1) {
//		            	newx = dist.sample(1)[0];
//		            }
//            	}
//            }
        } catch (MathException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to sample!");
        }
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<>();
        conditions.add(dist.getID());
        return conditions;
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();

        String id = getParameterName();
        arguments.add(id);

        return arguments;
    }
}
