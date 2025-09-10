package beast.base.spec.type;

import beast.base.spec.domain.Domain;

public interface Scalar<D extends Domain<T>, T> {

    /**
     * Get a single value.
     *
     * @return  the domain value with the Java type T.
     */
    T get();

//    T getLower();
//
//    T getUpper();

    /**
     * Get the domain type D.
     *
     * @return the domain type, e.g. Real, Int, ...
     */
    D domainType();

    default long size(){
        long s=1;
        for(int d:shape())
            s*=d;
        return s;
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
     * Validate that this instance satisfies the type constraints.
     *
     * @return true if this instance is valid according to its type constraints, false otherwise
     */
    default boolean isValid() {
        D d = domainType();
        return d.isValid(get());
    }

}