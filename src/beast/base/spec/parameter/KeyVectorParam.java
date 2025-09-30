package beast.base.spec.parameter;

import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.spec.domain.Real;

import java.util.*;

public abstract class KeyVectorParam<T> extends StateNode {

    public final Input<String> keysInput = new Input<>("keys",
            "the keys (unique dimension names) for the dimensions of this parameter", (String) null);

    protected List<String> keys = null; // unmodifiableList
    protected Map<String, Integer> keyToIndexMap = null;

    public abstract T get(String key);

    public abstract int getDimension();

    protected void initKeys(List<String> keys) {
        this.keys = keys;

        if (keys != null) {
            keyToIndexMap = new TreeMap<>();
            for (int i = 0; i < keys.size(); i++) {
                keyToIndexMap.put(keys.get(i), i);
            }
            if (keyToIndexMap.keySet().size() != keys.size()) {
                throw new IllegalArgumentException("All keys must be unique! Found " +
                        keyToIndexMap.keySet().size() + " unique keys for " + getDimension() + " dimensions.");
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
        if (getDimension() == 1) return "0";
        else if (i < getDimension()) return "" + (i + 1);
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
        for (int i = 0; i < getDimension(); i++) {
            keys.add(getKey(i));
        }
        return Collections.unmodifiableList(keys);
    }
}
