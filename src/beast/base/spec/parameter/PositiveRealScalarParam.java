package beast.base.spec.parameter;

import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;

/**
 * TODO this class has to be created, only because domainTypeInput.setValue(PositiveReal.INSTANCE
 */
@Deprecated
public class PositiveRealScalarParam<D extends PositiveReal> extends NonNegativeRealScalarParam<D> {
    public PositiveRealScalarParam() {
        super();
        // Domain is baked in
        domainTypeInput.setValue(PositiveReal.INSTANCE, this);
        //TODO description ?
    }
}