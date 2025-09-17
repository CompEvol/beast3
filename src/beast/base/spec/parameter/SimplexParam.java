package beast.base.spec.parameter;

import beast.base.core.Description;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.Simplex;


@Description("A scalar real-valued parameter with domain constraints")
public class SimplexParam extends RealVectorParam<UnitInterval> implements Simplex {

    public SimplexParam() {
        super();
        domain = UnitInterval.INSTANCE;
        domainTypeInput.setValue(UnitInterval.INSTANCE, this);
    }

    public SimplexParam(final Double[] values) {
        super(values, UnitInterval.INSTANCE);
        domain = UnitInterval.INSTANCE;
//        domainTypeInput.setValue(UnitInterval.INSTANCE, this);
    }
    
    @Override
    public void initAndValidate() {
        super.initAndValidate();
    }

    // enforce the correct domain
    @Override
    public UnitInterval domainType() {
        return UnitInterval.INSTANCE;
    }
}