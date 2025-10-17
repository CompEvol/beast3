package beast.base.spec.inference.distribution;

import beast.base.spec.domain.Domain;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Vector;

public interface ParamDistInterface<D extends Domain<T>, T> {

    double calcLogP(Scalar<D, T> scalar);

    double calcLogP(Vector<D, T> vector);

    T[][] sample(int size);

}
