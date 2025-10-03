package beast.base.spec.inference.operator.deltaexchange;

import beast.base.core.Input;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.CompoundIntScalarParam;
import beast.base.util.Randomizer;

public class CompoundIntDeltaExchangeOperator extends AbstractDeltaExchange {

    public final Input<CompoundIntScalarParam<? extends Int>> intparameterInput = new Input<>("parameter",
            "if specified, this parameter is operated on",
            Input.Validate.REQUIRED, CompoundIntScalarParam.class);

    @Override
    int getDimension() {
        return intparameterInput.get().size();
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

        // compound real parameter case
        CompoundIntScalarParam compoundIntParam = intparameterInput.get();
        int scalar1 = compoundIntParam.get(dim1);
        int scalar2 = compoundIntParam.get(dim2);

        final int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

        if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
        scalar1 = Math.round(scalar1 - d);
        scalar2 = Math.round(scalar2 + d);

        if ( !compoundIntParam.isValid(dim1, scalar1) || !compoundIntParam.isValid(dim2, scalar2) ) {
            logq = Double.NEGATIVE_INFINITY;
        } else {
            compoundIntParam.set(dim1, scalar1);
            compoundIntParam.set(dim2, scalar2);
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
            optimizeDelta(delta, _delta, false);
        }
    }

}
