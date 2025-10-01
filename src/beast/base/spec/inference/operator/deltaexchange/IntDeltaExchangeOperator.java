package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.util.Randomizer;

@Description("Delta exchange operator that proposes through a Bactrian distribution for integer valued parameters")
public class IntDeltaExchangeOperator extends AbstractDeltaExchange {

    //TODO rename to "parameter"
    public final Input<IntVectorParam<? extends Int>> intparameterInput = new Input<>(
            "parameter", "if specified, this parameter is operated on",
            Input.Validate.REQUIRED, IntVectorParam.class);

//    public final Input<Integer> deltaInput = new Input<>("delta",
//            "Magnitude of change for two randomly picked values.", 1);

    // TODO possible to update Operator to Operator<Number> ?, so take int delta
//    private int delta;

    public void initAndValidate() {
        super.initAndValidate();

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
//        final int dim = parameterWeights.length;

        // Find the number of weights that are nonzero
        int nonZeroWeights = 0;
        for (int i: parameterWeights) {
            if (i != 0) {
                ++nonZeroWeights;
            }
        }

        if (nonZeroWeights <= 1) {
            // it is impossible to select two distinct entries in this case, so there is nothing to propose
            return 0.0;
        }

        // Generate indices for the values to be modified
        int dim1 = Randomizer.nextInt(nonZeroWeights);
        int dim2 = Randomizer.nextInt(nonZeroWeights-1);
        if (dim2 >= dim1) {
            ++dim2;
        }
        if (nonZeroWeights<dim) {
            // There are zero weights, so we need to increase dim1 and dim2 accordingly.
            int nonZerosBeforeDim1 = dim1;
            int nonZerosBeforeDim2 = dim2;
            dim1 = 0;
            dim2 = 0;
            while (nonZerosBeforeDim1 > 0 | parameterWeights[dim1] == 0 ) {
                if (parameterWeights[dim1] != 0) {
                    --nonZerosBeforeDim1;
                }
                ++dim1;
            }
            while (nonZerosBeforeDim2 > 0 | parameterWeights[dim2] == 0 ) {
                if (parameterWeights[dim2] != 0) {
                    --nonZerosBeforeDim2;
                }
                ++dim2;
            }
        }

        double logq = 0.0;

        IntVectorParam intparameter = intparameterInput.get();

        // operate on int parameter
        int scalar1 = intparameter.getValue(dim1);
        int scalar2 = intparameter.getValue(dim2);

        final int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

        if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
        scalar1 = Math.round(scalar1 - d);
        scalar2 = Math.round(scalar2 + d);


        if (scalar1 < intparameter.getLower() || scalar1 > intparameter.getUpper() ||
                scalar2 < intparameter.getLower() || scalar2 > intparameter.getUpper()) {
            logq = Double.NEGATIVE_INFINITY;
        } else {
            intparameter.setValue(dim1, scalar1);
            intparameter.setValue(dim2, scalar2);
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
            _delta += Math.log(delta);
            delta = Math.exp(_delta);
//            if (isIntegerOperator) {
            // when delta < 0.5
            // Randomizer.nextInt((int) Math.round(delta)) becomes
            // Randomizer.nextInt(0) which results in an exception
            delta = Math.max(0.5000000001, delta);
//            }
        }
    }

}
