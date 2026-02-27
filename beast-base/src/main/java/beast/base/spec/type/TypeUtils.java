package beast.base.spec.type;

import java.util.Arrays;

/**
 * Utility methods for formatting {@link Tensor} type information as strings.
 */
public final class TypeUtils {

    /**
     * Converts a tensor's shape to a human-readable string.
     * Returns an empty string for scalars (rank 0), the size for vectors (rank 1),
     * or array notation (e.g. {@code [3, 4]}) for higher-rank tensors.
     *
     * @param tensor the tensor whose shape to format
     * @return the shape as a string
     */
    public static String shapeToString(Tensor tensor) {
        int[] shape = tensor.shape();
        if (shape.length == 0) return ""; // empty
        else if (shape.length == 1) return shape[0] + "";  // size of vector
        else return Arrays.toString(shape); // [3, 4]
    }

}
