package phylospec.base.inference;

import org.phylospec.primitives.NonNegativeReal;
import org.phylospec.primitives.PositiveReal;
import org.phylospec.types.Scalar;
import phylospec.base.core.Input;
import phylospec.base.core.TensorInput;

public class NonNegativeRealScalarParam extends ScalarParam<NonNegativeReal> {

    final public TensorInput<Scalar<NonNegativeReal>> valueInput = new TensorInput<>(
            "value", "start value for this scalar parameter",
            new NonNegativeRealScalarParam(0.0), Input.Validate.REQUIRED, NonNegativeReal.INSTANCE);

    final public TensorInput<Scalar<NonNegativeReal>> lowerValueInput = new TensorInput<>(
            "lower","lower value for this parameter (default 0)");


    public NonNegativeRealScalarParam(Double startValue) {
        super(startValue);
    }

    public NonNegativeRealScalarParam() {
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

        if (lowerValueInput.get() != null) {
            lower = lowerValueInput.get().get();
            //TODO need safe way to setPrimitiveType
            lowerValueInput.setPrimitiveType(primitiveType());

            if (!primitiveType().isValid(lower))
                throw new IllegalArgumentException("lower value " + lower + " is invalid ! ");
        } else {
            lower = 0.0;
        }

    }

    @Override
    public NonNegativeReal primitiveType() {
        return NonNegativeReal.INSTANCE;
    }

}