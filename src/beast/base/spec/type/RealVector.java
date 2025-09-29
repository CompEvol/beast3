package beast.base.spec.type;


import beast.base.spec.Bounded;
import beast.base.spec.domain.Real;

public interface RealVector<D extends Real> extends Vector<D, Double>, Bounded<Double> {


    Double get(int i);

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
        return Vector.super.isValid(value) && withinBounds(value);
    }
}