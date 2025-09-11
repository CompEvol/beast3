package beast.base.spec.parameter;

import beast.base.spec.domain.PositiveReal;

public class PositiveRealScalarParam extends RealScalarParam<PositiveReal> {
    public PositiveRealScalarParam() {
        super();
        // Domain is baked in
        domainTypeInput.setValue(PositiveReal.INSTANCE, this);
        //TODO description ?
    }
}