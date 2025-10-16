package beast.base.spec.inference.operator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.core.ProgramStatus;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.IntVector;
import beast.base.spec.type.Tensor;
import beast.base.util.Randomizer;

@Description("A generic operator for use with a sum-constrained (possibly weighted) parameter.")
public class DeltaExchangeOperator extends KernelOperator {
	public final Input<List<Tensor<?,?>>> parameterInput = new Input<>("parameter",
            "one or more parameters to be operated on by exchanging values so the sum remains the same", new ArrayList<>());
	public final Input<RealVectorParam<?>> rvparameterInput = new Input<>("rvparameter",
            "real vector parameter to be operated on", Validate.REQUIRED);
	public final Input<IntVectorParam<?>> ivparameterInput = new Input<>("ivparameter",
            "int vector parameter to be operated on", Validate.REQUIRED);
	public final Input<List<RealScalarParam<?>>> rsparameterInput = new Input<>("rsparameter",
            "real sclara parameters to be operated on", Validate.REQUIRED);
	public final Input<List<IntScalarParam<?>>> isparameterInput = new Input<>("isparameter",
            "int scalar parameters to be operated on", Validate.REQUIRED);


	List<Tensor<?,?>> getParameters() {
		List<Tensor<?,?>> list = new ArrayList<>();
		list.addAll(parameterInput.get());
		list.add(rvparameterInput.get());
		list.add(ivparameterInput.get());
		list.addAll(rsparameterInput.get());
		list.addAll(isparameterInput.get());
		return list;
	}

	
    public final Input<Double> deltaInput = new Input<>("delta", "Magnitude of change for two randomly picked values.", 1.0);
    public final Input<Boolean> autoOptimizeiInput =
            new Input<>("autoOptimize", "if true, window size will be adjusted during the MCMC run to improve mixing.", true);
    public final Input<Boolean> integerOperatorInput = new Input<>("integer", "if true, changes are all integers.", false);
    public final Input<IntVector<?>> parameterWeightsInput = new Input<>("weightvector", "weights on a items in parameter vector");

    private boolean autoOptimize;
    private double delta;
    private boolean isIntegerOperator;
    
    /** maps dimension to tensor in list **/
    private int [] map;
    /** offset of tensor **/
    private int [] offset;

    public void initAndValidate() {
    	super.initAndValidate();
    	
    	if (ProgramStatus.name.equals("BEAUti")) {
    		return;
    	}

        autoOptimize = autoOptimizeiInput.get();
        delta = deltaInput.get();
        isIntegerOperator = integerOperatorInput.get();

        List<Tensor<?,?>> params = getParameters();
        if (params.size() > 1) {
        	// sanity check
        	for (int i = 0; i < params.size(); i++) {
        		for (int j = i + 1; j < params.size(); j++) {
        			if (params.get(i) == params.get(j)) {
        				throw new RuntimeException("Duplicate intparameter (" + ((BEASTInterface)params.get(j)).getID() + ") found in operator " + getID());
        			}
        		}
        	}
        }
        // input type check
        for (Tensor<?,?> t : params) {
        	if (!(t instanceof RealScalarParam || t instanceof RealVectorParam || t instanceof IntScalarParam || t instanceof IntVectorParam)) {
         		throw new IllegalArgumentException("Only Real/Int Scalar/Vector Parameers are allowed as parameter inputs");
        	}
        }
        
        int dim = createMap(params);

        if (isIntegerOperator && delta != Math.round(delta)) {
            throw new IllegalArgumentException("Can't be an integer operator if delta is not integer");
        }

        // dimension sanity check
    	if (dim <= 1) {
    		Log.warning.println("WARNING: the dimension of the parameter is " + dim + " at the start of the run.\n"
    				+ "         The operator " + getID() + " has no effect (if this does not change).");
    	}

    }

    /** create mapping to parameters in list
     * after creating the map
     * map[i] = index of parameter in list associated with the i-th dimension
     * offset[i] = offset of parameter map[i] 
     * **/
    private int createMap(List<Tensor<?,?>> list) {
		int size = 0;
		for (Tensor<?,?> t : list) {
			size += t.size();
		}
		map = new int[size];
		offset = new int[size];
		int paramID = 0, k = 0;
		for (Tensor<?,?> t : list) {
			for (int i = 0; i < t.size(); i++) {
				map[k] = paramID;
				offset[k] = i;
				k++;
			}
			
			paramID++;
		}
		return size;
	}

    
	private int[] weights() {
		int[] weights = new int[map.length];

		if (parameterWeightsInput.get() != null) {
			if (weights.length != parameterWeightsInput.get().size())
				throw new IllegalArgumentException(
						"Weights vector should have the same length as parameter dimension");

			for (int i = 0; i < weights.length; i++) {
				weights[i] = parameterWeightsInput.get().get(i);
			}
		} else {
			Arrays.fill(weights, 1);
		}
		return weights;
	}


    
	@Override
    public final double proposal() {
    	int[] parameterWeights = weights();
    	final int dim = parameterWeights.length;
    	
    	// Find the number of weights that are nonzero
    	int nonZeroWeights = 0;
    	for (int i: parameterWeights) {
    		if (i != 0) {
    			++nonZeroWeights;
    		}
    	}
    	
        if (nonZeroWeights <= 1) {
        	// it is impossible to select two distinct entries in this case, so there is nothing to propose 
        	return 0.0;
        }
    	
        // Generate indices for the values to be modified
        int dim1 = Randomizer.nextInt(nonZeroWeights);
        int dim2 = Randomizer.nextInt(nonZeroWeights-1);
        if (dim2 >= dim1) {
        	++dim2;
        }
        if (nonZeroWeights<dim) {
        	// There are zero weights, so we need to increase dim1 and dim2 accordingly.
        	int nonZerosBeforeDim1 = dim1;
        	int nonZerosBeforeDim2 = dim2;
        	dim1 = 0;
        	dim2 = 0;
        	while (nonZerosBeforeDim1 > 0 | parameterWeights[dim1] == 0 ) {
        		if (parameterWeights[dim1] != 0) {
        			--nonZerosBeforeDim1;
        		}
        		++dim1;
        	}
        	while (nonZerosBeforeDim2 > 0 | parameterWeights[dim2] == 0 ) {
        		if (parameterWeights[dim2] != 0) {
        			--nonZerosBeforeDim2;
        		}
        		++dim2;
        	}
        }

        double logq = 0.0;

    	Tensor<?,?> p1 = getParameters().get(map[dim1]);
    	Tensor<?,?> p2 = getParameters().get(map[dim2]);

        if (p1.getDomain() instanceof Real) {
            // operate on real parameter
            double scalar1 = (Double) p1.get(offset[dim1]);
            double scalar2 = (Double) p2.get(offset[dim2]);

            if (isIntegerOperator) {
                final int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

                if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
                scalar1 = Math.round(scalar1 - d);
                scalar2 = Math.round(scalar2 + d);
            } else {

                // exchange a random delta
                final double d = kernelDistribution.getRandomDelta(0, Double.NaN, delta);

                if (parameterWeights[dim1] != parameterWeights[dim2]) {
                    final double sumW = parameterWeights[dim1] + parameterWeights[dim2];
                    scalar1 -= d * parameterWeights[dim2] / sumW;
                    scalar2 += d * parameterWeights[dim1] / sumW;
                } else {
                    scalar1 -= d / 2; // for equal weights
                    scalar2 += d / 2;
                }

            }

            if (((Real)p1).isValid(scalar1) && ((Real)p2).isValid(scalar2)) {
            	if (p1 instanceof RealScalarParam p) {
            		p.set(scalar1);
            	} else if (p1 instanceof RealVectorParam p) {
            		p.set(offset[dim1], scalar1);
            	}
            	if (p2 instanceof RealScalarParam p) {
            		p.set(scalar2);
            	} else if (p2 instanceof RealVectorParam p) {
            		p.set(offset[dim2], scalar2);
            	}
            } else {
            	logq = Double.NEGATIVE_INFINITY;
            }
        } else {
            // operate on int parameter
            int scalar1 = (Integer) p1.get(offset[dim1]);
            int scalar2 = (Integer) p2.get(offset[dim2]);

            final int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

            if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
            scalar1 = Math.round(scalar1 - d);
            scalar2 = Math.round(scalar2 + d);

            if (((Int)p1).isValid(scalar1) && ((Int)p2).isValid(scalar2)) {
            	if (p1 instanceof IntScalarParam p) {
            		p.set(scalar1);
            	} else if (p1 instanceof IntVectorParam p) {
            		p.set(offset[dim1], scalar1);
            	}
            	if (p2 instanceof IntScalarParam p) {
            		p.set(scalar2);
            	} else if (p2 instanceof IntVectorParam p) {
            		p.set(offset[dim2], scalar2);
            	}
            } else {
                logq = Double.NEGATIVE_INFINITY;            	
            }
        }

        // symmetrical move so return a zero hasting ratio
        return logq;
    }

	@Override
    public double getCoercableParameterValue() {
        return delta;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        delta = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(final double logAlpha) {
        // must be overridden by operator implementation to have an effect
        if (autoOptimize) {
            double _delta = calcDelta(logAlpha);
            _delta += Math.log(delta);
            delta = Math.exp(_delta);
            if (isIntegerOperator) {
            	// when delta < 0.5
            	// Randomizer.nextInt((int) Math.round(delta)) becomes
            	// Randomizer.nextInt(0) which results in an exception
            	delta = Math.max(0.5000000001, delta);
            }
        }

    }

    @Override
    public final String getPerformanceSuggestion() {
        final double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        final double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        final double newDelta = delta * ratio;

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else if (prob > 0.40) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else return "";
    }
    
	
    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.3;
    }

}
