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

}