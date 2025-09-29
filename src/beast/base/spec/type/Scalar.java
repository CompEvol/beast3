package beast.base.spec.type;

import beast.base.spec.domain.Domain;

public interface Scalar<D extends Domain<T>, T> extends Tensor<D, T> {

    /**
     * Get a single value.
     *
     * @return  the domain value with the Java type T.
     */
    T get();

    default T get(int... idx) {
        return get();
    }

    // rank() == 0 but size == 1

    /**
     * The order or degree of a tensor.
     * Scalar → rank = 0 (no indices),
     *
     * @return  the number of indices needed to uniquely identify one of its elements.
     */
    default int rank(){ return 0; }

    /**
     * The size(s) along each dimension.
     *
     * @return empty for Scalar
     */
    default int[] shape(){ return new int[]{}; }

    /**
     * Validate that this instance satisfies the domain constraints.
     *
     * @return true if this instance is valid according to its domain constraints, false otherwise
     */
    default boolean isValid(T value) {
        D d = getDomain();
        return d.isValid(value);
    }

}