package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.operator.AutoOptimized;
import beast.base.spec.inference.parameter.*;

public class CompoundRealDeltaExchangeOperator extends KernelOperator implements Weighted, AutoOptimized {

    public final Input<CompoundRealScalarParam<? extends Real>> parameterInput = new Input<>("parameter",
            "if specified, this parameter is operated on",
            Input.Validate.REQUIRED, RealVectorParam.class);

    public final Input<IntVectorParam<? extends PositiveInt>> parameterWeightsInput = new Input<>(
            "weightvector", "weights on a vector parameter");

    public final Input<Double> deltaInput = new Input<>("delta",
            "Magnitude of change for two randomly picked values.", 1.0);
    public final Input<Boolean> autoOptimizeiInput = new Input<>("autoOptimize",
            "if true, window size will be adjusted during the MCMC run to improve mixing.",
            true);

    private boolean autoOptimize;
    private double delta;

    @Override
    public IntVectorParam<? extends PositiveInt> getWeights() {
        return parameterWeightsInput.get();
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        delta = deltaInput.get();
        autoOptimize = autoOptimizeiInput.get();

        // dimension sanity check
        int dim = parameterInput.get().size();
        if (dim <= 1) {
            //TODO why not Exception?
            Log.warning.println("WARNING: the dimension of the parameter is " + dim + " at the start of the run.\n"
                    + "         The operator " + getID() + " has no effect (if this does not change).");
        }

    }

    @Override
    public final double proposal() {
        final int dim = parameterInput.get().size();
        int[] parameterWeights = getWeights(dim);

        // Generate indices for the values to be modified
        IntPair dims = getPairedDim(parameterWeights);
        // it is impossible to select two distinct entries in this case, so there is nothing to propose
        if (dims == null) return 0.0;

        int dim1 = dims.first();
        int dim2 = dims.second();

        double logq = 0.0;

        // compound real parameter case
        CompoundRealScalarParam compoundRealParam = parameterInput.get();
        double scalar1 = compoundRealParam.get(dim1);
        double scalar2 = compoundRealParam.get(dim2);

        // exchange a random delta
        final double d = getNextDouble(0);

        if (parameterWeights[dim1] != parameterWeights[dim2]) {
            final double sumW = parameterWeights[dim1] + parameterWeights[dim2];
            scalar1 -= d * parameterWeights[dim2] / sumW;
            scalar2 += d * parameterWeights[dim1] / sumW;
        } else {
            scalar1 -= d / 2; // for equal weights
            scalar2 += d / 2;
        }

        if ( !compoundRealParam.isValid(dim1, scalar1) || !compoundRealParam.isValid(dim2, scalar2) ) {
            logq = Double.NEGATIVE_INFINITY;
        } else {
            compoundRealParam.set(dim1, scalar1);
            compoundRealParam.set(dim2, scalar2);
        }

        //System.err.println("apply deltaEx");
        // symmetrical move so return a zero hasting ratio
        return logq;
    }

    private double getNextDouble(int i) {
        return kernelDistribution.getRandomDelta(i, Double.NaN, delta);
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
            optimizeDelta(delta, _delta, false);
        }
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
