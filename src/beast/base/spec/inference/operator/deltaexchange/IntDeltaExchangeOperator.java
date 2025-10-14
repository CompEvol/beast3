package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.util.Randomizer;

@Description("Delta exchange operator that proposes through a Bactrian distribution for integer valued parameters")
public class IntDeltaExchangeOperator extends AbstractDeltaExchange {

    public final Input<IntVectorParam<? extends Int>> intparameterInput = new Input<>(
            "parameter", "if specified, this parameter is operated on",
            Input.Validate.REQUIRED, IntVectorParam.class);

    @Override
    int getDimension() {
        return intparameterInput.get().size();
    }

    @Override
    double getNextDouble(int i) {
        throw new IllegalArgumentException();
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        //TODO this is the reason to use strong typing
        if (delta != Math.round(delta)) { // isIntegerOperator &&
            throw new IllegalArgumentException("Can't be an integer operator if delta is not integer");
        }
    }

    @Override
    public final double proposal() {
        final int dim = getDimension();
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
        int scalar1 = intVectorParam.get(dim1);
        int scalar2 = intVectorParam.get(dim2);

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
