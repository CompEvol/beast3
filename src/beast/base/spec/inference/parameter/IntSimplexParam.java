package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.type.IntSimplex;


@Description("A scalar real-valued parameter with domain constraints")
public class IntSimplexParam<D extends NonNegativeInt> extends IntVectorParam<D> implements IntSimplex<D> {// VectorParam<D, Integer> {

    final public Input<Integer> sumInput = new Input<>("sum",
            "the expected sum of a simplex of integers, default to the size");

    public IntSimplexParam() {
        super();
        super.setDomain((D)PositiveInt.INSTANCE); // correct domain using setter
    }

    public IntSimplexParam(int[] values, D domain, int expectedSum) {
        // use Domain bounds
        this(values, domain, expectedSum,domain.getLower(), domain.getUpper());
    }

    public IntSimplexParam(int[] values, D domain, int expectedSum, int lower, int upper) {
        setInputsNoValidation(values, domain, lower, upper);
        sumInput.setValue(expectedSum, this);

        // always validate
        initAndValidate();
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();
    }

    /**
     * @return the expected sum of a simplex of integers, default to the size
     */
    @Override
    public int expectedSum() {
        return sumInput.get() == null ? size() : sumInput.get() ;
    }
}