package beast.base.spec.parameter;

import beast.base.spec.domain.NonNegativeReal;

@Deprecated
public class NonNegativeRealScalarParam<D extends NonNegativeReal> extends RealScalarParam<D> {
    public NonNegativeRealScalarParam() {
        super();
        // Domain is baked in
        domainTypeInput.setValue(NonNegativeReal.INSTANCE, this);
    }
}