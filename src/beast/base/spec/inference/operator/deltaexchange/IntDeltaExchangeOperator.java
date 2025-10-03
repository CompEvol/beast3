package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.Operator;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.inference.operator.AutoOptimized;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.util.Randomizer;

@Description("Delta exchange operator that proposes through a Bactrian distribution for integer valued parameters")
public class IntDeltaExchangeOperator extends Operator implements Weighted, AutoOptimized {

    public final Input<IntVectorParam<? extends Int>> intparameterInput = new Input<>(
            "parameter", "if specified, this parameter is operated on",
            Input.Validate.REQUIRED, IntVectorParam.class);

        public final Input<IntVectorParam<? extends PositiveInt>> parameterWeightsInput = new Input<>(
            "weightvector", "weights on a vector parameter");

//    public final Input<Integer> deltaInput = new Input<>("delta",
//            "Magnitude of change for two randomly picked values.", 1);
    public final Input<Double> deltaInput = new Input<>("delta",
        "Magnitude of change for two randomly picked values.", 1.0);
    public final Input<Boolean> autoOptimizeiInput = new Input<>("autoOptimize",
            "if true, window size will be adjusted during the MCMC run to improve mixing.",
            true);

    private boolean autoOptimize;
    // TODO possible to update Operator to Operator<Number> ?, so take int delta
//    private int delta;
    private double delta;

    @Override
    public IntVectorParam<? extends PositiveInt> getWeights() {
        return parameterWeightsInput.get();
    }

    public void initAndValidate() {
        delta = deltaInput.get();
        autoOptimize = autoOptimizeiInput.get();

        //TODO this is the reason to use strong typing
        if (delta != Math.round(delta)) { // isIntegerOperator &&
            throw new IllegalArgumentException("Can't be an integer operator if delta is not integer");
        }

        // dimension sanity check
        int dim = intparameterInput.get().size();
        if (dim <= 1) {
            //TODO why not Exception?
            Log.warning.println("WARNING: the dimension of the parameter is " + dim + " at the start of the run.\n"
                    + "         The operator " + getID() + " has no effect (if this does not change).");
        }

    }

    @Override
    public final double proposal() {
        final int dim = intparameterInput.get().size();
        int[] parameterWeights = getWeights(dim);

        // Generate indices for the values to be modified
        IntPair dims = getPairedDim(parameterWeights);
        // it is impossible to select two distinct entries in this case, so there is nothing to propose
        if (dims == null) return 0.0;

        int dim1 = dims.first();
        int dim2 = dims.second();

        double logq = 0.0;

        IntVectorParam intVectorParam = intparameterInput.get();

        // operate on int parameter
        int scalar1 = intVectorParam.getValue(dim1);
        int scalar2 = intVectorParam.getValue(dim2);

        final int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

        if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
        scalar1 = Math.round(scalar1 - d);
        scalar2 = Math.round(scalar2 + d);


        if ( !intVectorParam.isValid(scalar1) ||  !intVectorParam.isValid(scalar2) ) {
            logq = Double.NEGATIVE_INFINITY;
        } else {
            intVectorParam.set(dim1, scalar1);
            intVectorParam.set(dim2, scalar2);
        }

        //System.err.println("apply deltaEx");
        // symmetrical move so return a zero hasting ratio
        return logq;
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
            // when delta < 0.5
            // Randomizer.nextInt((int) Math.round(delta)) becomes
            // Randomizer.nextInt(0) which results in an exception
            optimizeDelta(delta, _delta, true);
        }
    }

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
