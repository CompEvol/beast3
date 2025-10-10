package beast.base.spec.type;


import beast.base.spec.domain.Bool;

public interface BoolScalar extends Scalar<Bool, Boolean> {

    /**
     * Get a single value.
     *
     * @return the unboxed domain value, which is faster than boxed.
     */
    boolean get();

    /**
     * Use boxed value T only when required for API or nullability.
     *
     * @param idx  index/indices, depending on the dimension.
     * @return  the boxed domain value.
     */
    default Boolean get(int... idx) {
        return get();
    }

}