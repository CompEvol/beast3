package beast.base.spec.type;


import beast.base.spec.Bounded;
import beast.base.spec.domain.Int;

public interface IntVector<D extends Int> extends Vector<D, Integer>, Bounded<Integer> {

    Integer get(int i);

//    default int[] getIntArray() {
//        int length = Math.toIntExact(size());
//        int[] arr = new int[length];
//        for (int i = 0; i < length; i++) {
//            arr[i] = get(i);
//        }
//        return arr;
//    }

}