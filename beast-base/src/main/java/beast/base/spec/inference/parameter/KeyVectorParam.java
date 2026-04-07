package beast.base.spec.inference.parameter;

import beast.base.core.Input;
import beast.base.inference.StateNode;

import java.util.*;

/**
 * Abstract base class for vector parameters that support named dimensions (keys).
 * Each dimension can optionally be assigned a unique string key, allowing
 * lookup by name rather than index. Subclasses implement the concrete
 * value storage for specific types (Double, Integer, Boolean).
 *
 * @param <T> the boxed element type
 */
public abstract class KeyVectorParam<T> extends StateNode {

    public final Input<String> keysInput = new Input<>("keys",
            "the keys (unique dimension names) for the dimensions of this parameter", (String) null);

    public final Input<List<Integer>> shapeInput = new Input<>("shape",
            "Interpret the flat storage as a multi-dimensional array with these " +
            "axis sizes (e.g. shape='3 4' for a 3-row, 4-column matrix). " +
            "Product of shape must equal dimension. Default: flat vector.",
            new ArrayList<>(), Integer.class);

    protected List<String> keys = null; // unmodifiableList
    protected Map<String, Integer> keyToIndexMap = null;

    /**
     * Shape of the parameter. Null means flat vector (rank 1).
     * For a 4x3 matrix, shape = {4, 3}.
     */
    protected int[] tensorShape = null;

    /**
     * Row-major strides for multi-dimensional indexing.
     * For shape {4, 3}, strides = {3, 1}.
     */
    protected int[] strides = null;

    /**
     * Retrieves the value associated with a named key.
     *
     * @param key the unique key for a dimension
     * @return the value, or {@code null} if the key is not found
     */
    public abstract T get(String key);

    /**
     * Same method signature with {@link beast.base.spec.type.Vector}.
     * @return the dimension of this parameter
     */
    public abstract int size();

    @Override
    public void initAndValidate() {

        // parse shape early (before keys validation) so nrows() is available.
        // product-vs-size validation is deferred to initShape() which runs after
        // values are allocated in the subclass.
        parseShape();

        // keys
        if (keysInput.get() != null) {
            String[] keysArr = keysInput.get().split(" ");
            List<String> keys = Collections.unmodifiableList(Arrays.asList(keysArr));

            // For a shaped vector, keys can match either total size or nrows (row-level keys).
            boolean validLength = keys.size() == this.size() ||
                    (tensorShape != null && tensorShape.length >= 2 && keys.size() == tensorShape[0]);
            if (!validLength)
                throw new IllegalArgumentException("Keys length must equal dimension (" + this.size() + ")" +
                        (tensorShape != null ? " or nrows (" + tensorShape[0] + ")" : "") +
                        ", but got " + keys.size());
            initKeys(keys);
        }

    }

    protected void initKeys(List<String> keys) {
        this.keys = keys;

        if (keys != null) {
            keyToIndexMap = new TreeMap<>();
            for (int i = 0; i < keys.size(); i++) {
                keyToIndexMap.put(keys.get(i), i);
            }
            if (keyToIndexMap.keySet().size() != keys.size()) {
                throw new IllegalArgumentException("All keys must be unique! Found " +
                        keyToIndexMap.keySet().size() + " unique keys for " + size() + " dimensions.");
            }
        }
    }

    /**
     * @param i index
     * @return the unique key for the i'th value.
     * if no key, it will return a string representing the zero-based index,
     * (i.e. a string representation of the argument).
     */
    public String getKey(int i) {
        if (keys != null) return keys.get(i);
        if (size() == 1) return "0";
        else if (i < size()) return "" + (i + 1);
        throw new IllegalArgumentException("Invalid index " + i);
    }

    /**
     * @return the unmodifiable list of keys (a unique string for each dimension)
     * that parallels the parameter index.
     * It will throw UnsupportedOperationException if attempting to modify
     */
    public List<String> getKeysList() {
        if (keys != null)
            return keys; // unmodifiable list
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < size(); i++) {
            keys.add(getKey(i));
        }
        return Collections.unmodifiableList(keys);
    }
    
    /**
     * swap values of element i1 and i2
     *
     * @param i1   index one
     * @param i2   index two
     */
    abstract public void swap(final int i1, final int i2);

    // --- Shape support ---

    /**
     * Parse shape input into tensorShape and strides. Does NOT validate
     * against size() -- call this before values are allocated if needed
     * (e.g. for keys validation in initAndValidate).
     */
    private void parseShape() {
        if (shapeInput.get() != null && !shapeInput.get().isEmpty()) {
            tensorShape = shapeInput.get().stream().mapToInt(Integer::intValue).toArray();
            // row-major strides
            strides = new int[tensorShape.length];
            strides[tensorShape.length - 1] = 1;
            for (int k = tensorShape.length - 2; k >= 0; k--)
                strides[k] = strides[k + 1] * tensorShape[k + 1];
        }
    }

    /**
     * Validate that the product of shape equals the total number of elements.
     * Must be called after values are allocated (so that size() returns the
     * correct total element count). Subclasses call this from initAndValidate.
     */
    protected void initShape() {
        // parseShape() already ran in super.initAndValidate(); just validate product
        if (tensorShape != null) {
            int product = 1;
            for (int s : tensorShape) product *= s;
            if (product != size())
                throw new IllegalArgumentException("Product of shape " + Arrays.toString(tensorShape) +
                        " (" + product + ") must equal dimension " + size());
        }
    }

    /**
     * Convert multi-dimensional indices to a flat index using row-major order.
     *
     * @param idx indices, one per dimension
     * @return flat index into the values array
     */
    protected int toFlatIndex(int... idx) {
        if (tensorShape == null || idx.length == 1)
            return idx[0];
        if (idx.length != tensorShape.length)
            throw new IndexOutOfBoundsException(
                    "Expected " + tensorShape.length + " indices, got " + idx.length);
        int flat = 0;
        for (int k = 0; k < idx.length; k++)
            flat += idx[k] * strides[k];
        return flat;
    }

    /**
     * @return the tensor rank: 1 for a flat vector, 2 for a matrix, etc.
     */
    public int getTensorRank() {
        return tensorShape != null ? tensorShape.length : 1;
    }

    /**
     * @return the tensor shape: {size} for a flat vector, {rows, cols} for a matrix, etc.
     */
    public int[] getTensorShape() {
        return tensorShape != null ? tensorShape.clone() : new int[]{size()};
    }

    /**
     * @return number of rows (first axis) for a shaped parameter, or size() for a flat vector
     */
    public int nrows() {
        return tensorShape != null && tensorShape.length >= 2 ? tensorShape[0] : size();
    }

    /**
     * @return number of columns (second axis) for a shaped parameter, or 1 for a flat vector
     */
    public int ncols() {
        return tensorShape != null && tensorShape.length >= 2 ? tensorShape[1] : 1;
    }

}
