package beast.base.spec.type;


import beast.base.spec.domain.Bool;

/**
 * Vector type for boolean-valued parameters.
 * The domain is fixed to {@link Bool} and has no configurable bounds.
 */
public interface BoolVector extends Vector<Bool, Boolean> {

    /**
     * @param i index
     * @return the unboxed domain value at ith element, which is faster than boxed.
     */
    boolean get(int i);

    /**
     * Use boxed value T only when required for API or nullability.
     *
     * @param idx  index/indices, depending on the dimension.
     * @return  the boxed domain value at ith element.
     */
    default Boolean get(int... idx) {
        if (idx.length != 1)
            throw new IndexOutOfBoundsException("Vector access requires exactly 1 index, but got " + idx.length);
        return get(idx[0]);
    }

//    default boolean[] getBooleanArray() {
//        int length = Math.toIntExact(size());
//        boolean[] arr = new boolean[length];
//        for (int i = 0; i < length; i++) {
//            arr[i] = get(i);
//        }
//        return arr;
//    }

    /**
     * Validates a value against the {@link Bool} domain.
     *
     * @param value the value to validate
     * @return {@code true} if the value is valid for the boolean domain
     */
    @Override
    default boolean isValid(Boolean value) {
        return Bool.INSTANCE.isValid(value);
    }
}