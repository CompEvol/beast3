package beast.base.spec.type;


import beast.base.spec.Bounded;
import beast.base.spec.domain.Real;

public interface RealVector<D extends Real> extends Vector<D, Double>, Bounded<Double> {

    /**
     * @param i index
     * @return the unboxed domain value at ith element, which is faster than boxed.
     */
    double get(int i);

    /**
     * Use boxed value T only when required for API or nullability.
     *
     * @param idx  index/indices, depending on the dimension.
     * @return  the boxed domain value at ith element.
     */
    default Double get(int... idx) {
        if (idx.length != 1)
            throw new IndexOutOfBoundsException("Vector access requires exactly 1 index, but got " + idx.length);
        return get(idx[0]);
    }

//    default double[] getDoubleArray() {
//        int length = Math.toIntExact(size());
//        double[] arr = new double[length];
//        for (int i = 0; i < length; i++) {
//            arr[i] = get(i);
//        }
//        return arr;
//    }

    @Override
    default Double getLower() {
        D domain = getDomain();
        return domain.getLower();
    }

    @Override
    default Double getUpper() {
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
    default boolean isValid(Double value) {
        // 1st check domain constraints, 2nd check if value is in the real scalar range
        // Note: these bounds can be the subset of domain bounds.
        D d = getDomain();
        return d.isValid(value) && withinBounds(value);
    }
}