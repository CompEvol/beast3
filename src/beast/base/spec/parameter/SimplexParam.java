package beast.base.spec.parameter;

import beast.base.core.Description;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.Simplex;


@Description("A scalar real-valued parameter with domain constraints")
public class SimplexParam extends RealVectorParam<UnitInterval> implements Simplex {

    public SimplexParam() {
        super();
        setDomain(UnitInterval.INSTANCE); // must set Input as well
    }

    public SimplexParam(final double[] values) {
        super(values, UnitInterval.INSTANCE);
    }

    public SimplexParam(double[] values, Double lower, Double upper) {
        super(values, UnitInterval.INSTANCE, lower, upper);
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();
    }

    // enforce the correct domain
    @Override
    public UnitInterval getDomain() {
        return UnitInterval.INSTANCE;
    }

    @Override
    public void setDomain(UnitInterval domain) {
        if (! domain.equals(UnitInterval.INSTANCE))
            throw new IllegalArgumentException();
    }
}