package phylospec.base.inference;

import org.phylospec.primitives.Real;
import org.phylospec.types.Scalar;
import phylospec.base.core.Input;
import phylospec.base.core.TensorInput;

public class RealScalarParam extends ScalarParam<Real> {
//public class RealScalarParam extends StateNode implements Scalar<Real> {

    final public TensorInput<Scalar<Real>> valueInput = new TensorInput<>(
            "value","start value for this scalar parameter",
            new RealScalarParam(0.0), Input.Validate.REQUIRED, Real.INSTANCE);

    //++++++++ Scalar ++++++++

    //    protected P primitive;

    public RealScalarParam() {
        super();
    }

    public RealScalarParam(Double startValue) {
        super(startValue);
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
    public Real primitiveType() {
        return Real.INSTANCE;
    }


    //++++++++ StateNode ++++++++

}
