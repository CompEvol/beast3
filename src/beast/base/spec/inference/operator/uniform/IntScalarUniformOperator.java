package beast.base.spec.inference.operator.uniform;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Operator;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.util.Randomizer;

/**
 * @deprecated every random var should have a prior, this is replaced by {@link beast.base.spec.inference.distribution.IntUniform}
 */
@Deprecated
@Description("Assign one or more parameter values to a uniformly selected value in its range.")
public class IntScalarUniformOperator extends Operator {

    final public Input<IntScalarParam<? extends Int>> parameterInput = new Input<>(
            "parameter", "an integer scalar parameter to sample individual values for",
            Validate.REQUIRED, IntScalarParam.class);

    IntScalarParam<? extends Int> parameter;
    int lowerIndex, upperIndex;

//    public IntScalarUniformOperator() {
//        super();
//
//    }

    @Override
    public void initAndValidate() {
        parameter = parameterInput.get();
        lowerIndex = parameter.getLower();
        upperIndex = parameter.getUpper();
    }

    @Override
    public double proposal() {

        int newValue = Randomizer.nextInt(upperIndex - lowerIndex + 1) + lowerIndex; // from 0 to n-1, n must > 0,
        parameter.set(newValue);

        return 0.0;
    }

}
