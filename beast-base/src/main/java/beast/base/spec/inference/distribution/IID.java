package beast.base.spec.inference.distribution;

import beast.base.core.Input;
import beast.base.spec.domain.Domain;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A collection of ({@link Scalar}) is independent and identically distributed.
 * @param <V>  params
 * @param <S>  the sample type S of the distribution, see {@link TensorDistribution}.
// * @param <D>  domain {@link Domain}
 * @param <T>  Java type
 */
public class IID<V extends Vector<?, T>,
        S extends Scalar<?, T>,
        T> extends TensorDistribution<V, T> {

    // param in IID is vector, but distr is univariate

    // the param in distr is null
    final public Input<ScalarDistribution<?, T>> distInput
            = new Input<>("distr",
            "the base distribution for iid, e.g. normal, beta, gamma.",
            Input.Validate.REQUIRED);

    protected ScalarDistribution<?, T> dist;

    public IID() {}

    public IID(V param, ScalarDistribution<?, T> dist) {

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
        if (param == null)
            throw new IllegalArgumentException("IID requires param, but it is null ! ");
        // Note: the param dim can be changed after this init,
        // for example, RandomLocalClockModel will reset rates dimension given tree during model init.
    }

    // when param is vector, dist is univariate, then apply dist to each dim.
    @Override
    public double calculateLogP() {
        logP = this.calcLogP(param.getElements());
        return logP;
    }

    @Override
	public void refresh() {
        dist.refresh();
    }

    @Override
    protected double calcLogP(T... value) {
        return this.calcLogP(Arrays.asList(value));
    }

    private double calcLogP(List<T> values) {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC

        if (values == null)
            throw new IllegalArgumentException("IID requires param, but it is null ! ");
        if (values.size() != dimension())
            throw new IllegalArgumentException("Values dimension != parameter dimension !");
        double logP = 0.0;
        for (T t : values) {
            // refresh(); has been called in getApacheDistribution();
            logP += dist.calcLogP(t);
        }
        return logP;
    }

    @Override
	public List<T> sample() {
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

    @Override
    public T getLowerBoundOfParameter() {
        return dist.getLowerBoundOfParameter();
    }

    @Override
    public T getUpperBoundOfParameter() {
        return dist.getUpperBoundOfParameter();
    }

}
