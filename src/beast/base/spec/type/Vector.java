package beast.base.spec.type;

import beast.base.spec.domain.Domain;

import java.util.List;

public interface Vector<D extends Domain<T>, T> {//extends List<D> {
    
    /**
     * Get all elements in the vector.
     *
     * @return an unmodifiable list of all elements
     */
    List<T> getElements();

    /**
     * Get the domain value with the type T.
     *
     * @param idx  index/indices, depending on the dimension
     * @return  the domain value with the type T in Java.
     */
    T get(int idx);

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

    /**
     * The order or degree of a tensor.
     * Vector → rank = 1 (needs 1 index),
     *
     * @return  the number of indices needed to uniquely identify one of its elements.
     */
    default int rank(){ return 1; }

    /**
     * The size(s) along each dimension.
     *
     * @return one integer for Vector
     */
    default int[] shape(){
        return new int[]{Math.toIntExact(size())};
    }

    /**TODO
     * Validate that this instance satisfies the type constraints.
     *
     * @return true if this instance is valid according to its type constraints, false otherwise
     */
    default boolean isValid() {
        D d = domainType();
        for (int i=0; i<Math.toIntExact(size()); i++)
            if (!d.isValid(get(i)))
                return false;
        return true;
    }

}
