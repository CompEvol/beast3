package beast.base.spec.type;

import java.util.Arrays;

public final class TypeUtils {

    public static String shapeToString(Tensor tensor) {
        int[] shape = tensor.shape();
        if (shape.length == 0) return ""; // empty
        else if (shape.length == 1) return shape[0] + "";  // size of vector
        else return Arrays.toString(shape); // [3, 4]
    }

}
