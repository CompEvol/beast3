package beast.base.spec.inference.operator.deltaexchange;

import beast.base.spec.domain.PositiveInt;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.util.Randomizer;

import java.util.Arrays;

public interface Weighted {

//    public final Input<IntVectorParam<? extends PositiveInt>> parameterWeightsInput = new Input<>(
//            "weightvector", "weights on a vector parameter");

    IntVectorParam<? extends PositiveInt> getWeights();

    /**
     * get weights from input or use the default equal weights.
     * @param paramDim  parameter dimension
     * @return   an int array of weights on a vector parameter
     */
    default int[] getWeights(int paramDim) {
        int[] weights = new int[paramDim];

        if (getWeights() != null) {
            if (weights.length != getWeights().size())
                throw new IllegalArgumentException(
                        "Weights vector should have the same length as parameter dimension");

            for (int i = 0; i < weights.length; i++) {
                weights[i] = getWeights().get(i);
            }
        } else {
            Arrays.fill(weights, 1);
        }
        return weights;
    }


    record IntPair(int first, int second) {}

    /**
     * Generate a pair of indices for the values to be modified.
     * @param weights  parameter weights
     * @return  a {@link IntPair} of indices of weights,
     *          and use {@link IntPair#first()} and {@link IntPair#second()} to get the value.
     *          If it is impossible to select two distinct entries in this case,
     *          then return null.
     */
    default IntPair getPairedDim(int[] weights) {

        // Find the number of weights that are nonzero
        int nonZeroWeights = 0;
        for (int i: weights) {
            if (i != 0) {
                ++nonZeroWeights;
            }
        }

        if (nonZeroWeights <= 1) {
            // it is impossible to select two distinct entries in this case, so there is nothing to propose
            return null;
        }

        final int dim = weights.length;
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
            while (nonZerosBeforeDim1 > 0 | weights[dim1] == 0 ) {
                if (weights[dim1] != 0) {
                    --nonZerosBeforeDim1;
                }
                ++dim1;
            }
            while (nonZerosBeforeDim2 > 0 | weights[dim2] == 0 ) {
                if (weights[dim2] != 0) {
                    --nonZerosBeforeDim2;
                }
                ++dim2;
            }
        }

        return new IntPair(dim1, dim2);
    }
}
