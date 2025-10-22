package beast.base.spec.inference.operator;



import java.text.DecimalFormat;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;


@Description("A random walk operator that selects a random dimension of the real parameter and perturbs the value a " +
        "random amount according to a Bactrian distribution (Yang & Rodriguez, 2013), which is a mixture of two Gaussians:"
        + "p(x) = 1/2*N(x;-m,1-m^2) + 1/2*N(x;+m,1-m^2) and more efficient than RealRandomWalkOperator")
public class RealRandomWalkOperator extends KernelOperator {
    final public Input<RealVectorParam<Real>> parameterInput = new Input<>("parameter", "the vector parameter to operate a random walk on.");
    final public Input<RealScalarParam<Real>> scalarInput = new Input<>("scalar", "the scalar parameter to operate a random walk on.", Validate.XOR, parameterInput);
    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor", "deprecated -- use windowSize instead");
    public final Input<Double> windowSizeInput = new Input<>("windowSize", "window size: larger means more bold proposals");
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);

    double windowSize;

    @Override
	public void initAndValidate() {
    	super.initAndValidate();

    	if (scaleFactorInput.get() != null && windowSizeInput.get() != null) {
    		throw new IllegalArgumentException("Specify at most one of windowSize and scaleFactor, not both -- note scaleFactor is deprecated");
    	}

    	// windowSize is windowSizeInput or scaleFactorInput if specified, otherwise defaults to 1.0
        windowSize = scaleFactorInput.get() != null ? scaleFactorInput.get(): 
        			 windowSizeInput.get() != null ? windowSizeInput.get() : 
        				1.0;
    }

    @Override
    public double proposal() {

    	if (parameterInput.get() != null) {
        	RealVectorParam<Real> param = parameterInput.get();

	        int i = Randomizer.nextInt(param.size());
	        double value = param.get(i);
	        double newValue = value + kernelDistribution.getRandomDelta(i, value, windowSize);
	        
	        if (newValue < param.getLower() || newValue > param.getUpper()) {
	            return Double.NEGATIVE_INFINITY;
	        }
	
	        param.set(i, newValue);
    	} else {

    		RealScalarParam<Real> param = scalarInput.get();

	        double value = param.get();
	        double newValue = value + kernelDistribution.getRandomDelta(0, value, windowSize);
	        
	        if (newValue < param.getLower() || newValue > param.getUpper()) {
	            return Double.NEGATIVE_INFINITY;
	        }
	
	        param.set(newValue);
    	}

        return 0;
    }


    @Override
    public double getCoercableParameterValue() {
        return windowSize;
    }

    @Override
    public void setCoercableParameterValue(double value) {
    	windowSize = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(double logAlpha) {
    	if (optimiseInput.get()) {
	        // must be overridden by operator implementation to have an effect
	        double delta = calcDelta(logAlpha);
	
	        delta += Math.log(windowSize);
	        windowSize = Math.exp(delta);
    	}
    }

    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.3;
    }

    @Override
    public final String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = windowSize * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }
    
} // class BactrianRandomWalkOperator