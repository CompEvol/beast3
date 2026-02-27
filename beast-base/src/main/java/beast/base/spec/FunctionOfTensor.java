package beast.base.spec;

import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.CalculationNode;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;

/**
 * Adapter that wraps a {@link Tensor} to implement the legacy {@link Function} interface,
 * enabling tensor values to be used with older code that expects {@link Function}.
 *
 * @deprecated This bridge class will be removed once all code has been adapted
 *             to use the strongly-typed tensor API directly.
 */
@Description("Converts a tensor to a Function -- will be deleted in the future")
@Deprecated
public class FunctionOfTensor extends CalculationNode implements Function {
    final public Input<List<Tensor<?,?>>> tensorInput = new Input<>("arg", "argument to be summed", new ArrayList<>(), Validate.REQUIRED);
    //final public Input<Tensor<?,?>> tensorInput = new Input<>("arg", "argument to be summed", Validate.REQUIRED);

    Tensor<?,?> tensor;
	
	@Override
	public void initAndValidate() {
		tensor = tensorInput.get().get(0);
	}

	@Override
	public int getDimension() {
		return tensor.size();
	}

	@Override
	public double getArrayValue(int dim) {
    	Object o = tensor instanceof Scalar ? tensor.get() : tensor.get(dim);
    	if (o instanceof Double x) {
    		return x;
    	} else if (o instanceof Integer x) {
    		return x;
    	} else if (o instanceof Boolean x) {
    		return x ? 1 : 0;
    	}
		return 0;
	}

}
