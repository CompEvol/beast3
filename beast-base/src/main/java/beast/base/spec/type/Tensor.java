package beast.base.spec.type;

import beast.base.spec.domain.Domain;

/**
 * Tensor type - ordered value or values.
 *
 * @param <D> the type of {@link Domain <T>}.
 * @param <T> the primitive type in Java.
 */
public interface Tensor<D extends Domain<T>, T> {

    /**
     * Calling <code>get()</code> when idx is an empty array <code>idx.length == 0</code>.
     * Calling <code>get(0)</code> when idx is a single-element array,
     * where <code>idx.length == 1; idx[0] == 0</code>.
     * Calling <code>get(1,2)</code> for multidimensional indexing.
     * <p>
     * Get the primitive value with the type T.
     * Primitive numeric types (e.g. double) are typically 10×–50× faster than
     * boxed types (e.g. Double) in computation and 3×–4× smaller in memory.
     * Use boxed value T only when required for API or nullability.
     *
     * @param idx  index/indices, depending on the dimension
     * @return  the primitive value with the type T in Java.
     */
    T get(int... idx);

    /**
     * Get the domain type D.
     *
     * @return the domain type, e.g. Real, Int, ...
     */
    D getDomain();

    /**
     * The order or degree of a tensor.
     * For example, Scalar → rank = 0 (no indices),
     * Vector → rank = 1 (needs 1 index),
     * Matrix → rank = 2 (needs 2 indices).
     *
     * @return  the number of indices needed to uniquely identify one of its elements.
     */
    int rank();

    /**
     * The size(s) along each dimension.
     *
     * @return empty for Scalar, one integer for Vector, two integers for Matrix, e.g. [3,4].
     */
    int[] shape();

    default int size(){
        int s=1;
        for(int d:shape())
            s*=d;
        return s;
    }

    /**
     * Validate that this instance satisfies the type constraints.
     *
     * @return true if this instance is valid according to its type constraints, false otherwise
     */
    boolean isValid(T value);


    default boolean isValid(){ return isValid(get()); }
}