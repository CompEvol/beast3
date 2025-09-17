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

    @Override
    default Integer getLower() {
        D domain = domainType();
        return domain.getLower();
    }

    @Override
    default Integer getUpper() {
        D domain = domainType();
        return domain.getUpper();
    }

    @Override
    default boolean lowerInclusive() {
        return true;
    }

    @Override
    default boolean upperInclusive() {
        return true;
    }
}