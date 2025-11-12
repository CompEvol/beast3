package beast.base.spec.inference.distribution;

import beast.base.core.Input;
import beast.base.spec.domain.Domain;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of ({@link Scalar}) is independent and identically distributed.
 * @param <V>  params
 * @param <S>  the sample type S of the distribution, see {@link TensorDistribution}.
 * @param <D>  domain {@link Domain}
 * @param <T>  Java type
 */
public class IID<V extends Vector<D, T>,
        S extends Scalar<D, T>,
        D extends Domain<T>,
        T> extends TensorDistribution<V, D, T> {

    // param in IID is vector, but distr is univariate

    // the param in distr is null
    final public Input<TensorDistribution<S, D, T>> distInput
            = new Input<>("distr",
            "the base distribution for iid, e.g. normal, beta, gamma.",
            Input.Validate.REQUIRED);

    protected TensorDistribution<S, D, T> dist;

    public IID() {}

    public IID(V param, TensorDistribution<S, D, T> dist) {

        try {
            initByName("param", param, "distr", dist);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        dist = distInput.get();
        // param
        super.initAndValidate();
        if (param == null || dimension() <= 1)
            throw new IllegalArgumentException("IID requires param, but it is null ! ");

    }

    // when param is vector, dist is univariate, then apply dist to each dim.
    @Override
    protected double calcLogP(List<T> value) {
        // value dim == param dim
        if (value.size() != dimension())
            throw new IllegalArgumentException("Value dim does not match param size ! ");
        double logP = 0.0;
        for (T t : value) {
            logP += dist.calcLogP(List.of(t));
        }
        return logP;
    }

    @Override
    protected List<T> sample() {
        List<T> newListX = new ArrayList<>(dimension());
        for (int i = 0; i < dimension(); i++) {
            // Scalar
            T newX = dist.sample().getFirst();
            while (! param.isValid(newX)) {
                newX = dist.sample().getFirst();
            }
            newListX.add(newX);
        }
        return newListX;
    }

}
