package beast.base.spec.type;


import beast.base.spec.domain.Bool;

public interface BoolVector extends Vector<Bool, Boolean> {

    Boolean get(int i);

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