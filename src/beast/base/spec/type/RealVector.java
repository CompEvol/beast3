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
        D domain = domainType();
        return domain.getLower();
    }

    @Override
    default Double getUpper() {
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