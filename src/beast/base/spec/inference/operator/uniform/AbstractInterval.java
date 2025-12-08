package beast.base.spec.inference.operator.uniform;

import beast.base.core.Input;
import beast.base.inference.operator.kernel.KernelOperator;

import java.text.DecimalFormat;

@Deprecated
public abstract class AbstractInterval extends KernelOperator {

    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor",
            "scaling factor: larger means more bold proposals", 1.0);
    final public Input<Boolean> optimiseInput = new Input<>("optimise",
            "flag to indicate that the scale factor is automatically changed in order to " +
                    "achieve a good acceptance rate (default true)", true);
    final public Input<Boolean> inclusiveInput = new Input<>("inclusive",
            "are the upper and lower limits inclusive i.e. should limit values be accepted (default true)",
            true);

    double scaleFactor;
    double lower, upper;
    boolean inclusive;

    // default
    protected final double Target_Acceptance_Probability = 0.3;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        scaleFactor = scaleFactorInput.get();

        lower = getPamaLower();
        upper = getPamaUpper();
        inclusive = inclusiveInput.get();

        if (Double.isInfinite(lower)) {
            throw new IllegalArgumentException("Lower bound should be finite");
        }
        if (Double.isInfinite(upper)) {
            throw new IllegalArgumentException("Upper bound should be finite");
        }

    }

    abstract double getPamaLower();

    abstract double getPamaUpper();

    protected double getScaler(int i, double value) {
        return kernelDistribution.getScaler(i, value, getCoercableParameterValue());
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        scaleFactor = value;
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
        return Target_Acceptance_Probability;
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

} // class BactrianIntervalOperator
