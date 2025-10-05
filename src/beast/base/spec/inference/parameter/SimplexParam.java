package beast.base.spec.inference.parameter;

import beast.base.core.Description;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.Simplex;


@Description("A scalar real-valued parameter with domain constraints")
public class SimplexParam extends RealVectorParam<UnitInterval> implements Simplex, VectorParam<UnitInterval, Double> {

    public SimplexParam() {
        super();
//TODO        domainTypeInput.setRule(Input.Validate.FORBIDDEN);
        super.setDomain(UnitInterval.INSTANCE); // must set Input as well
    }

    public SimplexParam(final double[] values) {
        super(values, UnitInterval.INSTANCE);
    }

    public SimplexParam(double[] values, Double lower, Double upper) {
        super(values, UnitInterval.INSTANCE, lower, upper);
    }

    @Override
    public void initAndValidate() {
        super.setDomain(UnitInterval.INSTANCE); // fix domain to UnitInterval
        super.initAndValidate();
    }

    // enforce the correct domain
    @Override
    public UnitInterval getDomain() {
        return UnitInterval.INSTANCE;
    }

    // enforce the correct domain
    @Override
    public void setDomain(UnitInterval domain) {
        if (! domain.equals(UnitInterval.INSTANCE))
            throw new IllegalArgumentException();
    }
}