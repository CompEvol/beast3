package beast.base.spec.type;


import beast.base.spec.domain.Bool;

public interface BoolVector extends Vector<Bool, Boolean> {

    boolean get(int i);

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

    @Override
    default boolean isValid(Boolean value) {
        return Bool.INSTANCE.isValid(value);
    }
}