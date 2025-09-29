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
        D domain = getDomain();
        return domain.getLower();
    }

    @Override
    default Integer getUpper() {
        D domain = getDomain();
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

    @Override
    default boolean isValid() {
        for (int i = 0; i < size(); i++)
            if ( !isValid(get(i)))
                return false;
        return true;
    }

    @Override
    default boolean isValid(Integer value) {
        // 1st check domain constraints, 2nd check if value is in the real scalar range
        // Note: these bounds can be the subset of domain bounds.
        return Vector.super.isValid(value) && withinBounds(value);
    }
}