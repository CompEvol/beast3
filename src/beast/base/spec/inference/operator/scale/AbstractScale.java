package beast.base.spec.inference.operator.scale;

import beast.base.core.Input;
import beast.base.inference.operator.kernel.KernelOperator;

import java.text.DecimalFormat;

public abstract class AbstractScale extends KernelOperator {

    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor",
            "scaling factor: range from 0 to 1. Close to zero is very large jumps, " +
                    "close to 1.0 is very small jumps.", 0.75);
    final public Input<Double> scaleUpperLimit = new Input<>("upper",
            "Upper Limit of scale factor", 1.0 - 1e-8);
    final public Input<Double> scaleLowerLimit = new Input<>("lower",
            "Lower limit of scale factor", 1e-8);

    final public Input<Boolean> optimiseInput = new Input<>("optimise",
            "flag to indicate that the scale factor is automatically changed in order to " +
                    "achieve a good acceptance rate (default true)", true);

    /**
     * shadows input *
     */
    protected double scaleFactor;
    protected double upper;
    protected double lower;

    // default
    protected final double Target_Acceptance_Probability = 0.3;


    @Override
    public void initAndValidate() {
        super.initAndValidate();
        scaleFactor = scaleFactorInput.get();
        //TODO why?
        if (scaleUpperLimit.get() == 1 - 1e-8) {
            scaleUpperLimit.setValue(10.0, this);
        }
        upper = scaleUpperLimit.get();
        lower = scaleLowerLimit.get();
    }

    //TODO replaced by isValid()
//    protected boolean outsideBounds(final double value, final RealScalarParam param) {
//        final Double l = param.getLower();
//        final Double h = param.getUpper();
//
//        return (value < l || value > h);
        //return (l != null && value < l || h != null && value > h);
//    }

//    protected double getScaler() {
//        return (scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));
//    }

    protected double getScaler(int i, double value) {
        return kernelDistribution.getScaler(i, value, getCoercableParameterValue());
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(final double logAlpha) {
        if (optimiseInput.get()) {
            double delta = calcDelta(logAlpha);
            delta += Math.log(1.0 / scaleFactor - 1.0);
            setCoercableParameterValue(1.0 / (Math.exp(delta) + 1.0));
        }
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
}
