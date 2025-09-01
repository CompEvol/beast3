package phylospec.base.inference;

import org.phylospec.primitives.UnitInterval;
import org.phylospec.types.Simplex;

import java.util.List;

public class SimplexParam<P extends UnitInterval> extends VectorParam<P> implements Simplex<P> {

    private List<Double> values;

    /**
     * Constructs a Boolean with the given value.
     *
     * @param value the boolean value
     */
    public SimplexParam(double value) {
        super(value);
    }


}
