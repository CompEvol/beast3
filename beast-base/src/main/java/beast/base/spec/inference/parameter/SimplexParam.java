package beast.base.spec.inference.parameter;

import java.util.stream.DoubleStream;

import beast.base.core.Description;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.Simplex;


@Description("A real-valued vector whose elements sum to 1")
public class SimplexParam extends RealVectorParam<UnitInterval> implements Simplex { // VectorParam<UnitInterval, Double> {

    public SimplexParam() {
        super();
        super.setDomain(UnitInterval.INSTANCE); // correct domain using setter
    }

    public SimplexParam(final double[] values) {
        // use Domain bounds
        valuesInput.setValue(DoubleStream.of(values).boxed().toList(), this);
        domainTypeInput.setValue(UnitInterval.INSTANCE, this);
        
        // always validate
        initAndValidate();
    }

//    public SimplexParam(double[] values, double lower, double upper) {
//        setInputsNoValidation(values, UnitInterval.INSTANCE, lower, upper);
//
//        // always validate
//        initAndValidate();
//    }

    @Override
    public void initAndValidate() {
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