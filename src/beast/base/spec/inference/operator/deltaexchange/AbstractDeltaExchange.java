package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.Operator;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.inference.operator.AutoOptimized;
import beast.base.spec.inference.parameter.IntVectorParam;

public abstract class AbstractDeltaExchange extends Operator implements Weighted, AutoOptimized {

    public final Input<IntVectorParam<? extends PositiveInt>> parameterWeightsInput = new Input<>(
            "weightvector", "weights on a vector parameter");
    public final Input<Double> deltaInput = new Input<>("delta",
            "Magnitude of change for two randomly picked values.", 1.0);
    public final Input<Boolean> autoOptimizeiInput = new Input<>("autoOptimize",
            "if true, window size will be adjusted during the MCMC run to improve mixing.",
            true);

    protected boolean autoOptimize;
    protected double delta;

    @Override
    public IntVectorParam<? extends PositiveInt> getWeights() {
        return parameterWeightsInput.get();
    }

    @Override
    public void initAndValidate() {
        delta = deltaInput.get();
        autoOptimize = autoOptimizeiInput.get();

        // dimension sanity check
        int dim = getDimension();
        if (dim <= 1) {
            //TODO why not Exception?
            Log.warning.println("WARNING: the dimension of the parameter is " + dim + " at the start of the run.\n"
                    + "         The operator " + getID() + " has no effect (if this does not change).");
        }

    }

    abstract int getDimension();

    @Override
    public double getCoercableParameterValue() {
        return delta;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        delta = value;
    }

    @Override
    public double getTargetAcceptanceProbability() {
        return AutoOptimized.Target_Acceptance_Probability;
    }

    @Override
    public final String getPerformanceSuggestion() {
        return getPerformanceSuggestion(this, "delta");
    }
}
