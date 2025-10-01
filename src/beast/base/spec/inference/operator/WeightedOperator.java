package beast.base.spec.inference.operator;

import beast.base.core.Input;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.inference.parameter.IntVectorParam;

public abstract class WeightedOperator extends KernelOperator {

    public final Input<IntVectorParam<? extends PositiveInt>> parameterWeightsInput = new Input<>(
            "weightvector", "weights on a vector parameter");

//    @Override
//    public void initAndValidate() {
//        super.initAndValidate();
//    }


    /**
     * get weights from input or use the default equal weights.
     * @param paramDim  parameter dimension
     * @return   an int array of weights on a vector parameter
     */
    protected int[] getWeights(int paramDim) {
        int[] weights = new int[paramDim];

        if (parameterWeightsInput.get() != null) {
            if (weights.length != parameterWeightsInput.get().size())
                throw new IllegalArgumentException(
                        "Weights vector should have the same length as parameter dimension");

            for (int i = 0; i < weights.length; i++) {
                weights[i] = parameterWeightsInput.get().getValue(i);
            }
        } else {
            for (int i = 0; i < weights.length; i++) {
                weights[i] = 1;
            }
        }
        return weights;
    }
}
