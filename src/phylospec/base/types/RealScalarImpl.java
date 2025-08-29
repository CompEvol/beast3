package phylospec.base.types;

import org.phylospec.primitives.Bool;
import org.phylospec.primitives.Real;
import org.phylospec.types.BoolScalar;
import org.phylospec.types.Scalar;

public class RealScalarImpl<P extends Real> implements Scalar<P> {

    private final double value;

    /**
     * Constructs a Boolean with the given value.
     *
     * @param value the boolean value
     */
    public RealScalarImpl(double value) {
        this.value = value;
        if (!isValid()) {
            throw new IllegalArgumentException(
                    "..., but was: " + value);
        }
    }

    @Override
    public double get() {
        return value;
    }

    @Override
    public Double get(int... idx) {
        // a scalar (rank 0 or size 1), only index 0 is valid.
        if (idx[0] != 0) throw new IllegalArgumentException();
        return get();
    }

    @Override
    public Real primitiveType() {
        return Real.INSTANCE;
    }
}
