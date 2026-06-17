package beast.base.spec.inference.operator;

import beast.base.core.Input;
import beast.base.inference.operator.kernel.KernelOperator;

import java.text.DecimalFormat;

/**
 * Abstract base class for scale-based MCMC operators.
 * Provides the common scale factor management, auto-tuning, and performance
 * suggestion logic shared by {@link beast.base.spec.inference.operator.ScaleOperator}
 * and {@link beast.base.spec.evolution.operator.ScaleTreeOperator}.
 */
public abstract class AbstractScale extends KernelOperator {

    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor",
            "scale of the Bactrian kernel: close to zero is very small jumps, " +
                    "larger values give larger jumps.", 0.75);
    final public Input<Double> scaleUpperLimit = new Input<>("upper",
            "Upper Limit of scale factor", 10.0);
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

    /**
     * Draws a random scale factor from the kernel distribution for the given
     * element index and current value.
     *
     * @param i     the element index
     * @param value the current value being scaled
     * @return the random scale factor
     */
    protected double getScaler(int i, double value) {
        return kernelDistribution.getScaler(i, value, getCoercableParameterValue());
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(final double logAlpha) {
        if (optimiseInput.get()) {
            // The scaleFactor here is the Bactrian kernel scale: larger scaleFactor means
            // larger jumps (lower acceptance), the opposite of the legacy ScaleOperator
            // where scaleFactor close to 0 meant large jumps. Tune multiplicatively so the
            // direction matches the kernel: acceptance too high (delta > 0) increases the
            // scaleFactor, acceptance too low (delta < 0) decreases it. The legacy
            // 1/(exp(delta + log(1/sf - 1)) + 1) transform inverted this and broke (NaN)
            // for scaleFactor >= 1.
            double delta = calcDelta(logAlpha);
            delta += Math.log(scaleFactor);
            setCoercableParameterValue(Math.exp(delta));
        }
    }

    /**
     * Returns the current scale factor, which is the tunable parameter for this operator.
     *
     * @return the scale factor
     */
    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    /**
     * Sets the scale factor, clamping it within the configured lower and upper limits.
     *
     * @param value the new scale factor
     */
    @Override
    public void setCoercableParameterValue(final double value) {
        scaleFactor = Math.max(Math.min(value, upper), lower);
    }

    /**
     * Returns the target acceptance probability (0.3) for auto-tuning.
     *
     * @return the target acceptance probability
     */
    @Override
    public double getDefaultTargetAcceptanceProbability() {
        return Target_Acceptance_Probability;
    }

    /**
     * Returns a suggestion for improving operator performance based on current
     * acceptance rate relative to the target. Empty string if performance is acceptable.
     *
     * @return a performance suggestion, or empty string if acceptance rate is within range
     */
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
