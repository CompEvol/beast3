package beast.base.spec.evolution.operator;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.TreeInterface;
import beast.base.core.Input.Validate;
import beast.base.inference.Scalable;
import beast.base.inference.StateNode;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.Tensor;
import beast.base.util.Randomizer;


/**
 * Operator that scales one group of parameters up and another group down
 * by the same factor, maintaining their ratio. Uses a Bactrian proposal distribution.
 */
@Description("This element represents an operator that scales "
		+ "one or more Scalables (like parameters or trees) in different directions, "
		+ "but uses a Bactrian proposal distribution for the scale value. "
        + "The up parameter is multiplied by this scale and the down parameter is divided by this scale.")
public class UpDownOperator extends KernelOperator {
    final public Input<Double> scaleFactorInput = new Input<>("scaleFactor",
            "magnitude factor used for scaling", Validate.REQUIRED);
    final public Input<List<Scalable>> upInput = new Input<>("up",
            "zero or more items to scale upwards", new ArrayList<>());
    final public Input<List<Scalable>> downInput = new Input<>("down",
            "zero or more items to scale downwards", new ArrayList<>());
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);
    final public Input<Boolean> elementWiseInput = new Input<>("elementWise", "flag to indicate that the scaling is applied to a random index in multivariate parameters (default false)", false);

    final public Input<Double> scaleUpperLimit = new Input<>("upper", "Upper Limit of scale factor", 10.0);
    final public Input<Double> scaleLowerLimit = new Input<>("lower", "Lower limit of scale factor", 0.0);

    double scaleFactor;
    private double upper,lower;


    private List<TreeInterface> treesUp, treesDown;
    private List<Scalable> otherUp, otherDown;

    @Override
    public void initAndValidate() {
    	super.initAndValidate();
        scaleFactor = scaleFactorInput.get();
        // sanity checks
        if (upInput.get().size() + downInput.get().size() == 0) {
        	Log.warning.println("WARNING: At least one up or down item must be specified");
        }
        if (upInput.get().size() == 0 || downInput.get().size() == 0) {
        	Log.warning.println("WARNING: no " + (upInput.get().size() == 0 ? "up" : "down") + " item specified in UpDownOperator");
        }
        upper = scaleUpperLimit.get();
        lower = scaleLowerLimit.get();
        
        // separate out trees from other Scalables
        treesUp = new ArrayList<>();
        treesDown = new ArrayList<>();
        otherUp = new ArrayList<>();
        otherDown = new ArrayList<>();
        for (Scalable s : upInput.get()) {
        	if (s instanceof TreeInterface t) {
        		treesUp.add(t);
        	} else {
        		otherUp.add(s);
        	}
        }
        for (Scalable s : downInput.get()) {
        	if (s instanceof TreeInterface t) {
        		treesDown.add(t);
        	} else {
        		otherDown.add(s);
        	}
        }
        
        if (treesUp.size() + treesDown.size() > 0) {
        	if(elementWiseInput.get()) {
        		throw new IllegalArgumentException("Cannot do element wise scaling of trees");
        	}
        	if (treesUp.size() > 0 && treesDown.size() > 0) {
            	Log.warning("WARNING: trees found in up and down input -- usually");
        	}
        }
    }
    
	protected double getScaler(int i) {
		return kernelDistribution.getScaler(i, Double.NaN, getCoercableParameterValue());
	}
    

    /**
     * override this for proposals,
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
     *         should not be accepted
     */
    @Override
    public final double proposal() {

        double scale = getScaler(0);
        int goingUp = 0, goingDown = 0;

        double logHR = 0;        

        if (elementWiseInput.get()) {
            int size = 0;
            for (Scalable up : upInput.get()) {
                if (size == 0) size = ((Tensor<?,?>)up).size();
                if (size > 0 && ((Tensor<?,?>)up).size() != size) {
                    throw new RuntimeException("elementWise=true but parameters of differing lengths!");
                }
                goingUp += 1;
            }

            for (Scalable down : downInput.get()) {
                if (size == 0) size = ((Tensor<?,?>)down).size();
                if (size > 0 && ((Tensor<?,?>)down).size() != size) {
                    throw new RuntimeException("elementWise=true but parameters of differing lengths!");
                }
                goingDown += 1;
            }

            int index = Randomizer.nextInt(size);

            for (Scalable up : upInput.get()) {
                if (up instanceof RealVectorParam<?> p) {
                    p.set(index, p.get(index) * scale);
                }
                if (outsideBounds(up)) {
                    return Double.NEGATIVE_INFINITY;
                }
            }

            for (Scalable down : downInput.get()) {
                if (down instanceof RealVectorParam<?> p) {
                    p.set(index, p.get(index) / scale);
                }
                if (outsideBounds(down)) {
                    return Double.NEGATIVE_INFINITY;
                }
            }
            logHR = (goingUp - goingDown) * Math.log(scale);
        } else {

            try {
            	if (treesUp.size() > 0) {
            		
            		// scale trees up and adjust scale factor
	            	double lengthBefore =0, lenghtAfter = 0;
	                for (TreeInterface up : treesUp) {
	                	lengthBefore += treeLength(up);
	                	logHR += scaleTree(up, scale);
	                	lenghtAfter += treeLength(up);
	                }
	                scale = lenghtAfter / lengthBefore;
	                
            	} else if (treesDown.size() > 0) {
            		
            		// scale trees down and adjust scale factor
	            	double lengthBefore =0, lenghtAfter = 0;
	                for (TreeInterface down : treesDown) {
	                	lengthBefore += treeLength(down);
	                	logHR += scaleTree(down, 1.0 / scale);
	                	lenghtAfter += treeLength(down);
	                }
	                scale = 1.0/(lenghtAfter / lengthBefore);
	                
            	}
                
                for (Scalable up : otherUp) {
                	up = (Scalable) ((StateNode) up).getCurrentEditable(this);
                	goingUp += up.scale(scale);
                }
                // separated this into second loop because the outsideBounds might return true transiently with
                // related variables which would be BAD. Note current implementation of outsideBounds isn't dynamic,
                // so not currently a problem, but this became a problem in BEAST1 so this is preemptive strike.
                // Same below for down
                for (Scalable up : otherUp) {
                    if (outsideBounds(up)) {
                        return Double.NEGATIVE_INFINITY;
                    }
                }

                for (Scalable down : otherDown) {
            		down = (Scalable) ((StateNode) down).getCurrentEditable(this);
            		goingDown += down.scale(1.0 / scale);
                }
                for (Scalable down : otherDown) {
                    if (outsideBounds(down)) {
                        return Double.NEGATIVE_INFINITY;
                    }
                }
            } catch (Exception e) {
                // scale resulted in invalid StateNode, abort proposal
                return Double.NEGATIVE_INFINITY;
            }
            logHR += (goingUp - goingDown) * Math.log(scale);
        }
        return logHR;
    }
    
	private double treeLength(TreeInterface tree) {
		double length = 0;
		for (Node node : tree.getNodesAsArray()) {
			length += node.getLength();
		}
		return length;
	}


	private double scaleTree(TreeInterface tree, double scale) {
		int dim = resampleNodeHeight(tree.getRoot(), scale);
		return dim * Math.log(scale);
	}

	private int resampleNodeHeight(Node node, double scaler) {
		if (node.isLeaf()) {
			return 0;
		}
		
		if (node.isFake()) {
			if (node.getLeft().isDirectAncestor()) {
				return resampleNodeHeight(node.getRight(), scaler);
			} else {
				return resampleNodeHeight(node.getLeft(), scaler);
			}
		}
		
		double oldHeights = node.getHeight() - Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
		int scaledNodeCount = 1;
		scaledNodeCount += resampleNodeHeight(node.getLeft(), scaler);
		scaledNodeCount += resampleNodeHeight(node.getRight(), scaler);

		// resample the height
		double minHeight = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
		double newHeight = oldHeights * scaler;
		node.setHeight(newHeight + minHeight);
		
		return scaledNodeCount;
	}
	
    private boolean outsideBounds(final Scalable node) {
        if (node instanceof RealScalarParam<?> p) {
            if (!p.withinBounds(p.get())) {
            	return true;
            }
        }
        if (node instanceof RealVectorParam<?> p) {
        	for (int i = 0; i < p.size(); i++) {
        		if (!p.withinBounds(p.get(i))) {
        			return true;
        		}
            }
        }
        return false;
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(final double logAlpha) {
    	if (optimiseInput.get()) {
	        double delta = calcDelta(logAlpha);
	        double scaleFactor = getCoercableParameterValue();
	        delta += Math.log(scaleFactor);
	        scaleFactor = Math.exp(delta);
	        setCoercableParameterValue(scaleFactor);
    	}
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        scaleFactor = Math.max(Math.min(value,upper),lower);
    }

    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.3;
    }

    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = getCoercableParameterValue() * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }
    
} // class UpDownOperator
