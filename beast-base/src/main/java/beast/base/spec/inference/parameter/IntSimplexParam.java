package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.type.IntSimplex;

import java.util.stream.IntStream;


/**
 * An integer-valued simplex parameter in the MCMC state whose non-negative
 * elements must sum to a specified total (defaulting to the vector size).
 *
 * @param <D> the non-negative integer domain type
 */
@Description("A int-valued vector whose elements sum to a given sum")
public class IntSimplexParam<D extends NonNegativeInt> extends IntVectorParam<D> implements IntSimplex<D> {// VectorParam<D, Integer> {

    final public Input<Integer> sumInput = new Input<>("sum",
            "the expected sum of a simplex of integers, default to the size");

    public IntSimplexParam() {
        super();
        // the default domain, set on the input only: until initAndValidate(), getDomain()
        // reads the input, so a domain given in XML (e.g. PositiveInt for BSP group sizes)
        // is seen before then too, as for the other parameter types
        domainTypeInput.setValue(NonNegativeInt.INSTANCE, this);
    }

    // Note: for group size of BSP, need to set domain="PositiveInt"
    public IntSimplexParam(int[] values, D domain, int expectedSum) {
        // use Domain bounds
        valuesInput.setValue(IntStream.of(values).boxed().toList(), this);
        domainTypeInput.setValue(domain, this);
        isDirty = new boolean[values.length];
        sumInput.setValue(expectedSum, this);

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