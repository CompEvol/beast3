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

@Description("Converts a tensor to a Function -- will be deleted in the future")
/**
 * @deprecated once all code has been adapted to the PhyloType stuff, this will be removed
 */
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
