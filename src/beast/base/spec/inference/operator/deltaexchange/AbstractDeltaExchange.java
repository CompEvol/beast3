package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.inference.operator.WeightedOperator;

import java.text.DecimalFormat;


@Description("Operator with a flexible kernel distribution")
public abstract class AbstractDeltaExchange extends WeightedOperator {

    public final Input<Double> deltaInput = new Input<>("delta",
            "Magnitude of change for two randomly picked values.", 1.0);
    public final Input<Boolean> autoOptimizeiInput = new Input<>("autoOptimize",
            "if true, window size will be adjusted during the MCMC run to improve mixing.",
            true);

    protected boolean autoOptimize;
    protected double delta;

	@Override
	public void initAndValidate() {
        autoOptimize = autoOptimizeiInput.get();
        delta = deltaInput.get();
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
