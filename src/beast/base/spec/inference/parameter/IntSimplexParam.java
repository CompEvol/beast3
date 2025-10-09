package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.type.IntSimplex;


@Description("A scalar real-valued parameter with domain constraints")
public class IntSimplexParam<D extends NonNegativeInt> extends IntVectorParam<D> implements IntSimplex<D>, VectorParam<D, Integer> {

    final public Input<Integer> sumInput = new Input<>("sum",
            "the expected sum of a simplex of integers, default to the size",
            size());

    public IntSimplexParam() {
        super();
        dimensionInput.setValue(this.getDomain(), this); // must set Input as well
    }

    public IntSimplexParam(int[] values, D domain) {
        super(values, domain);
    }

    public IntSimplexParam(int[] values, D domain, Integer lower, Integer upper) {
        super(values, domain, lower, upper);
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();
    }

    @Override
    public int expectedSum() {
        return sumInput.get();
    }
}