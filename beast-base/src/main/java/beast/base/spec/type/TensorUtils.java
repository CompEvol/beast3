package beast.base.spec.type;

/**
 * Utility methods for extracting values from {@link Tensor} instances
 * into Java arrays.
 */
public class TensorUtils {

    /**
     * Extracts all values from a tensor into an {@code Object[]} array.
     * Supports {@link Scalar} (single element) and {@link Vector} tensors.
     *
     * @param tensor the tensor to extract values from
     * @return an array of boxed values
     * @throws UnsupportedOperationException if the tensor is not a Scalar or Vector
     */
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


    /**
     * Extracts all values from a tensor into a {@code double[]} array,
     * converting each element via {@link #toDouble(Object)}.
     * Supports {@link Scalar} (single element) and {@link Vector} tensors.
     *
     * @param tensor the tensor to extract values from
     * @return an array of double values
     * @throws UnsupportedOperationException if the tensor is not a Scalar or Vector
     */
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


    /**
     * Converts a boxed value to a primitive double.
     * {@link Number} types use {@link Number#doubleValue()};
     * {@link Boolean} maps to 1.0 (true) or 0.0 (false).
     *
     * @param value the value to convert
     * @return the double representation
     * @throws UnsupportedOperationException if the value is not a Number or Boolean
     */
    public static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        } else if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        throw new UnsupportedOperationException("Only support Number or Boolean ! But get " + value.getClass());
    }


}
