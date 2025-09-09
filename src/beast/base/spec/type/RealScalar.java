package beast.base.spec.type;

import beast.base.spec.domain.Real;

public interface RealScalar<P extends Real> extends Scalar<P, Double> {

    Double get();

}