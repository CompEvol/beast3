package phylospec.base.inference;

import org.phylospec.primitives.PositiveReal;
import org.phylospec.types.Scalar;
import phylospec.base.core.Input;
import phylospec.base.core.TensorInput;

public class PositiveRealScalarParam extends ScalarParam<PositiveReal> {

    final public TensorInput<Scalar<PositiveReal>> valueInput = new TensorInput<>(
            "value", "start value for this scalar parameter",
            new PositiveRealScalarParam(0.0), Input.Validate.REQUIRED, PositiveReal.INSTANCE);

    public PositiveRealScalarParam(Double startValue) {
        super(startValue);
    }

    public PositiveRealScalarParam() {
        super();
    }

    @Override
    public void initAndValidate() {
        this.value = valueInput.get().get();
        //TODO duplicate validation
        if (!primitiveType().isValid(value))
            throw new IllegalArgumentException("start value " + value + " is invalid ! ");

        // check if value is in bound
        super.initAndValidate();
    }

    @Override
    public PositiveReal primitiveType() {
        return PositiveReal.INSTANCE;
    }

}