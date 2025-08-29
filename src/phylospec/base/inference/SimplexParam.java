package phylospec.base.inference;

import org.phylospec.primitives.Primitive;
import org.phylospec.primitives.Real;
import org.phylospec.types.Simplex;

public class SimplexParam<P extends Simplex> extends ScalarParam<P> {


    /**
     * Constructs a Boolean with the given value.
     *
     * @param value the boolean value
     */
    public SimplexParam(double value) {
        super(value);
    }


}
