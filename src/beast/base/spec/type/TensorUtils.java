package beast.base.spec.type;

public class TensorUtils {

    public static Object[] valuesToObjectArray(Tensor tensor) {
        Object[] values = new Object[tensor.size()];
        if (tensor instanceof Scalar scalar) {
            values[0] = scalar.get();
        } else if (tensor instanceof Vector vector) {
            for (int i = 0; i < values.length; i++) {
                values[i] = vector.get(i);
            }
        } else
            throw new UnsupportedOperationException("Only support Vector or Scalar ! But get " + tensor.getClass());

        return values;
    }


    public static double[] valuesToDoubleArray(Tensor tensor) {
        double[] values = new double[tensor.size()];

        if (tensor instanceof Scalar scalar) {
            values[0] = toDouble(scalar.get());
        } else if (tensor instanceof Vector vector) {
            for (int i = 0; i < values.length; i++) {
                values[i] = toDouble(vector.get(i));
            }
        } else
            throw new UnsupportedOperationException("Only support Vector or Scalar ! But get " + tensor.getClass());

        return values;
    }


    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        } else if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        throw new UnsupportedOperationException("Only support Number or Boolean ! But get " + value.getClass());
    }


}
