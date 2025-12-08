package beast.base.spec.inference.operator.uniform;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Operator;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.util.Randomizer;

@Description("Assign one or more parameter values to a uniformly selected value in its range.")
public class IntVectorUniformOperator extends Operator {

    final public Input<IntVectorParam<? extends Int>> parameterInput = new Input<>("parameter",
            "an integer vector parameter to sample individual values for",
            Validate.REQUIRED, IntVectorParam.class);

    final public Input<Integer> howManyInput = new Input<>("howMany",
            "number of items to sample, default 1, must be less than the dimension of the parameter",
            1);

    int howMany;
    IntVectorParam<? extends Int> parameter;
    int lowerIndex, upperIndex;

    @Override
    public void initAndValidate() {
        parameter = parameterInput.get();
        lowerIndex = parameter.getLower();
        upperIndex = parameter.getUpper();

        howMany = howManyInput.get();
        if (howMany > parameter.size()) {
            throw new IllegalArgumentException("howMany it too large: must be less than the dimension of the parameter");
        }
    }

    @Override
    public double proposal() {
        for (int n = 0; n < howMany; ++n) {
            // do not worry about duplication, does not matter
            int index = Randomizer.nextInt(parameter.size());

            int newValue = Randomizer.nextInt(upperIndex - lowerIndex + 1) + lowerIndex; // from 0 to n-1, n must > 0,
            parameter.set(index, newValue);
        }
        return 0.0;
    }

}
