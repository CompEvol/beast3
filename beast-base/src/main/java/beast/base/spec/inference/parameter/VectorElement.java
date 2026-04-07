package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.RealVector;

/**
 * Scalar view of a single element in a {@link RealVector}.
 * Allows a vector parameter to be used wherever a {@link RealScalar} is required,
 * e.g. providing per-partition mutation rates from a single vector state node.
 *
 * @param <D> the real domain type, inherited from the underlying vector
 */
@Description("Scalar view of a single element in a RealVector")
public class VectorElement<D extends Real> extends CalculationNode implements RealScalar<D> {

    final public Input<RealVector<?>> vectorInput = new Input<>("vector",
            "the vector to extract an element from", Input.Validate.REQUIRED);
    final public Input<Integer> indexInput = new Input<>("index",
            "index of the element to extract", Input.Validate.REQUIRED);

    private RealVector<?> vector;
    private int index;

    public VectorElement() {}

    public VectorElement(RealVector<D> vector, int index) {
        initByName("vector", vector, "index", index);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initAndValidate() {
        vector = vectorInput.get();
        index = indexInput.get();
        if (index < 0 || index >= vector.size())
            throw new IndexOutOfBoundsException(
                    "index " + index + " out of range [0, " + vector.size() + ")");
    }

    @Override
    public double get() {
        return vector.get(index);
    }

    @SuppressWarnings("unchecked")
    @Override
    public D getDomain() {
        return (D) vector.getDomain();
    }
}
