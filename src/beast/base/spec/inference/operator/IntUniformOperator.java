package beast.base.spec.inference.operator;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Operator;
import beast.base.spec.domain.Int;
import beast.base.spec.parameter.IntScalarParam;
import beast.base.util.Randomizer;

@Description("Assign one or more parameter values to a uniformly selected value in its range.")
public class IntUniformOperator extends Operator {

    final public Input<IntScalarParam<? extends Int>> parameterInput = new Input<>(
            "parameter", "a real or integer parameter to sample individual values for",
            Validate.REQUIRED, IntScalarParam.class);
//    final public Input<Integer> howManyInput = new Input<>(
//            "howMany",
//            "number of items to sample, default 1, must be less than the dimension of the parameter",
//            1);

//    int howMany;
    IntScalarParam parameter; //TODO
    double lower, upper;
//    int lowerIndex, upperIndex;

    @Override
    public void initAndValidate() {
        parameter = parameterInput.get();
//        if (parameter instanceof RealParameter) {
            lower = parameter.getLower();
            upper = parameter.getUpper();
//        } else if (parameter instanceof IntegerParameter) {
//            lowerIndex = (Integer) parameter.getLower();
//            upperIndex = (Integer) parameter.getUpper();
//        } else {
//            throw new IllegalArgumentException("parameter should be a RealParameter or IntergerParameter, not " + parameter.getClass().getName());
//        }

//        howMany = howManyInput.get();
//        if (howMany > parameter.getDimension()) {
//            throw new IllegalArgumentException("howMany it too large: must be less than the dimension of the parameter");
//        }
    }

    @Override
    public double proposal() {
//        for (int n = 0; n < howMany; ++n) {
            // do not worry about duplication, does not matter
            int index = Randomizer.nextInt(((Function)parameter).getDimension());

//            if (parameter instanceof IntegerParameter) {
//                int newValue = Randomizer.nextInt(upperIndex - lowerIndex + 1) + lowerIndex; // from 0 to n-1, n must > 0,
            int newValue = Randomizer.nextInt();
            parameter.set(newValue);
//            } else {
//                double newValue = Randomizer.nextDouble() * (upper - lower) + lower;
//                ((RealParameter) parameter).setValue(index, newValue);
//            }

//        }

        return 0.0;
    }

}
