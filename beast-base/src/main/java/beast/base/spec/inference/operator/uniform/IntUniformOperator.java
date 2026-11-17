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
    /**
     * The parameter's bounds, read on the first proposal rather than in initAndValidate():
     * they come from the parameter's priors, which may not be initialised yet when this
     * operator is (an operator listed before its prior in the XML, issue #286). Reading
     * them on every proposal instead was 1.76x slower on a model with only this operator.
     */
    private int lower, upper;
    private boolean boundsRead;

    @Override
    public void initAndValidate() {
        parameter = parameterInput.get();
        if (!(parameter instanceof IntScalarParam<? extends Int>) && !(parameter instanceof IntVectorParam<? extends Int>)) {
            throw new IllegalArgumentException("Unsupported parameter type : " + parameter.getClass());
        }
        boundsRead = false;

        howMany = howManyInput.get();
        if (howMany > parameter.size()) {
            throw new IllegalArgumentException("howMany it too large: must be less than the dimension of the parameter");
        }
    }

    @Override
    public double proposal() {

        if (!boundsRead) {
            if (parameter instanceof IntScalarParam<? extends Int> p) {
                lower = p.getLower();
                upper = p.getUpper();
            } else if (parameter instanceof IntVectorParam<? extends Int> p) {
                lower = p.getLower();
                upper = p.getUpper();
            }
            boundsRead = true;
        }

        if (parameter instanceof IntScalarParam<? extends Int> intScalarParam) {
            intScalarParam.set(uniform(lower, upper));

        } else if (parameter instanceof IntVectorParam<? extends Int> intVectorParam) {
            for (int n = 0; n < howMany; ++n) {
                // do not worry about duplication, does not matter
                int index = Randomizer.nextInt(intVectorParam.size());
                intVectorParam.set(index, uniform(lower, upper));
            }
        }

        return 0.0;
    }

    /**
     * @return a value drawn uniformly from [lower, upper]. The number of values is computed
     * as a long, since upper - lower + 1 overflows an int for a wide range such as a domain's
     * [0, Integer.MAX_VALUE].
     */
    static int uniform(int lower, int upper) {
        long count = (long) upper - lower + 1;
        if (count <= 0) {
            throw new IllegalArgumentException("Cannot draw a value: the upper bound " + upper
                    + " is below the lower bound " + lower);
        }
        if (count <= Integer.MAX_VALUE) {
            return lower + Randomizer.nextInt((int) count);
        }
        // more values than an int can count: draw the offset from lower as a long
        long offset = (long) (Randomizer.nextDouble() * count);
        return (int) Math.min(upper, lower + offset);
    }

}
