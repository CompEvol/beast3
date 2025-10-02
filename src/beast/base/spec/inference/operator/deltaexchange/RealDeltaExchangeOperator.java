package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.operator.AutoOptimized;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;

@Description("Delta exchange operator that proposes through a Bactrian distribution for real valued parameters")
public class RealDeltaExchangeOperator extends KernelOperator implements Weighted, AutoOptimized {

    public final Input<RealVectorParam<? extends Real>> parameterInput = new Input<>("parameter",
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

    public RealDeltaExchangeOperator() {
        super();

    }

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
//        final int dim = parameterWeights.length;

        // Find the number of weights that are nonzero
        int nonZeroWeights = findNonZeroWeight(parameterWeights);

        if (nonZeroWeights <= 1) {
            // it is impossible to select two distinct entries in this case, so there is nothing to propose
            return 0.0;
        }

        // Generate indices for the values to be modified
        IntPair dims = getPairedDim(nonZeroWeights, parameterWeights);
        int dim1 = dims.first();
        int dim2 = dims.second();

        double logq = 0.0;

        RealVectorParam realparameter = parameterInput.get();

        // operate on real parameter
        double scalar1 = realparameter.getValue(dim1);
        double scalar2 = realparameter.getValue(dim2);

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


        if (scalar1 < realparameter.getLower() || scalar1 > realparameter.getUpper() ||
                scalar2 < realparameter.getLower() || scalar2 > realparameter.getUpper()) {
            logq = Double.NEGATIVE_INFINITY;
        } else {
            realparameter.set(dim1, scalar1);
            realparameter.set(dim2, scalar2);
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
            _delta += Math.log(delta);
            delta = Math.exp(_delta);
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
