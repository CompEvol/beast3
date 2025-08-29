package phylospec.base.types;

import org.phylospec.primitives.Bool;
import org.phylospec.types.BoolScalar;

public class BoolScalarImpl implements BoolScalar {

    private final boolean value;

    /**
     * Constructs a Boolean with the given value.
     *
     * @param value the boolean value
     */
    public BoolScalarImpl(boolean value) {
        this.value = value;
        if (!isValid()) {
            throw new IllegalArgumentException(
                    "..., but was: " + value);
        }
    }

    @Override
    public boolean get() {
        return value;
    }

    @Override
    public Boolean get(int... idx) {
        // a scalar (rank 0 or size 1), only index 0 is valid.
        if (idx[0] != 0) throw new IllegalArgumentException();
        return get();
    }

    @Override
    public Bool primitiveType() {
        return Bool.INSTANCE;
    }
}
