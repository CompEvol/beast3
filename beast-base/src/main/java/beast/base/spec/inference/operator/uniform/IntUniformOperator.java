package beast.base.spec.inference.operator.uniform;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Operator;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.type.Tensor;
import beast.base.util.Randomizer;

/**
 * Uniform operator for integer parameters. Assigns one or more randomly chosen
 * elements to a uniformly selected value within the parameter's valid range.
 */
@Description("Assign one or more parameter values to a uniformly selected value in its range.")
public class IntUniformOperator extends Operator {

    final public Input<Tensor<? extends Int, Integer>> parameterInput = new Input<>("parameter",
            "an integer vector parameter to sample individual values for",
            Validate.REQUIRED, Tensor.class);

    final public Input<Integer> howManyInput = new Input<>("howMany",
            "number of items to sample, default 1, must be less than the dimension of the parameter",
            1);

    int howMany;
    Tensor<? extends Int, Integer> parameter;
    int lowerIndex, upperIndex;

    @Override
    public void initAndValidate() {
        parameter = parameterInput.get();
        if (parameter instanceof IntScalarParam<? extends Int> intScalarParam) {
            lowerIndex = intScalarParam.getLower();
            upperIndex = intScalarParam.getUpper();
        } else if (parameter instanceof IntVectorParam<? extends Int> intVectorParam) {
            lowerIndex = intVectorParam.getLower();
            upperIndex = intVectorParam.getUpper();
        } else
            throw new IllegalArgumentException("Unsupported parameter type : " + parameter.getClass());

        howMany = howManyInput.get();
        if (howMany > parameter.size()) {
            throw new IllegalArgumentException("howMany it too large: must be less than the dimension of the parameter");
        }
    }

    @Override
    public double proposal() {

        if (parameter instanceof IntScalarParam<? extends Int> intScalarParam) {
            int newValue = Randomizer.nextInt(upperIndex - lowerIndex + 1) + lowerIndex; // from 0 to n-1, n must > 0,
            intScalarParam.set(newValue);

        } else if (parameter instanceof IntVectorParam<? extends Int> intVectorParam) {
            for (int n = 0; n < howMany; ++n) {
                // do not worry about duplication, does not matter
                int index = Randomizer.nextInt(intVectorParam.size());

                int newValue = Randomizer.nextInt(upperIndex - lowerIndex + 1) + lowerIndex; // from 0 to n-1, n must > 0,
                intVectorParam.set(index, newValue);
            }
        }

        return 0.0;
    }

}
