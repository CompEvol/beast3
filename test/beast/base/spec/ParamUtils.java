package beast.base.spec;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;

import java.util.Arrays;

public class ParamUtils {

    public static double[] filledArray(int size, double value) {
        double[] a = new double[size];
        Arrays.fill(a, value);
        return a;
    }

    public static <T extends Real> RealVectorParam<T> createRealVector(int size, double value, T realDomain) {
        return new RealVectorParam<>(filledArray(size, value), realDomain);
    }

}
