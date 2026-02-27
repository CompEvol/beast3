
package beast.base.spec.evolution.operator;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Scalable;
import beast.base.inference.operator.kernel.KernelDistribution;
import beast.base.inference.util.InputUtil;


/**
 * When the tree is ultrametric it behaves the same as for the classic scale operator 
 * that scaled all node heights with the same factor.
 * When trees are not ultrametric, it always produces valid trees (unlike the classic scale operator).
 * Performs always at least as good as the classic scale operator, as well as the tree flexer and stretcher combo
 * See https://www.biorxiv.org/content/10.1101/2025.06.18.660471v1.abstract for details
 * 
 * TODO: make sure it is compatible with sampled ancestor trees.
 */
/**
 * Operator that scales the intervals between consecutive node heights in a tree,
 * rather than scaling absolute heights. This preserves the tree topology while
 * changing relative branch lengths.
 */
@Description("Performs a scale move on the intervals between nodes.")
public class IntervalScaleOperator extends TreeOperator {
    public final Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals", 
    		KernelDistribution.newDefaultKernelDistribution());

    protected KernelDistribution kernelDistribution;


    final public Input<Double> scaleUpperLimit = new Input<>("upper", "Upper Limit of scale factor", 1.0 - 1e-8);
    final public Input<Double> scaleLowerLimit = new Input<>("lower", "Lower limit of scale factor", 1e-8);

    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: range from 0 to 1. Close to zero is very large jumps, close to 1.0 is very small jumps.", 0.75);

    final public Input<Boolean> optimiseInput = new Input<>("optimise",
			"flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)",
			true);

	public Input<List<Scalable>> downInput = new Input<>("down", "down parameter to scale", new ArrayList<>());

	public Input<List<Scalable>> upInput = new Input<>("up", "up parameter to scale", new ArrayList<>());
	
    private double scaleFactor;

    private double upper, lower;
    
	@Override
	public void initAndValidate() {
        scaleFactor = scaleFactorInput.get();
        upper = scaleUpperLimit.get();
        lower = scaleLowerLimit.get();

        kernelDistribution = kernelDistributionInput.get();
	}

	@Override
	public double proposal() {
		
		final Tree tree = (Tree) InputUtil.get(treeInput, this);

		double scaler = getScaler();
		double lengthBefore = getTreeLength(tree);
		int numbers = resampleNodeHeight(tree.getRoot(), scaler);
		double lengthAfter = getTreeLength(tree);
		double actualScaler = lengthAfter / lengthBefore;
		
		double logHR = Math.log(scaler) * (numbers);

		for (Scalable down : downInput.get()) {
			int dim = down.scale(1.0/actualScaler);//setValue(down.getValue() / actualScaler);
			logHR -= dim * Math.log(actualScaler);
		}
		for (Scalable up : upInput.get()) {
			int dim = up.scale(actualScaler);//setValue(up.getValue() * actualScaler);
			logHR += dim * Math.log(actualScaler);
		}
		return logHR;
	}

	private int resampleNodeHeight(Node node, double scaler) {
		if (node.isLeaf()) {
			return 0;
		}
		
		// deal with sampled ancestors
		if (node.isFake()) {
			if (node.getLeft().isDirectAncestor()) {
				return resampleNodeHeight(node.getRight(), scaler);
			} else {
				// node.getRight() must be direct ancestor
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
	
	private double getTreeLength(Tree tree) {
		double length = 0;
		for (Node node : tree.getNodesAsArray()) {
			length += node.getLength();
		}
		return length;
	}
	
	private double getScaler() {
    	return kernelDistribution.getScaler(0, getCoercableParameterValue());
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
    	if (optimiseInput.get()) {
	        double delta = calcDelta(logAlpha);
	        double scaleFactor = getCoercableParameterValue();
	        delta += Math.log(scaleFactor);
	        scaleFactor = Math.exp(delta);
	        setCoercableParameterValue(scaleFactor);
    	}
    }
    
    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.3;
    }
    

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        scaleFactor = Math.max(Math.min(value, upper), lower);
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

}
