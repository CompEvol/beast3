package beast.base.spec.inference.distribution;

import beast.base.core.Input;
import beast.base.inference.State;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Domain;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A collection of ({@link Scalar}) is independent and identically distributed.
 * @param <V>  samples
 * @param <S>  the sample type S of the distribution, see {@link TensorDistribution}.
 * @param <D>  domain {@link Domain}
 * @param <T>  Java type
 */
public class ScalarIID<V extends Vector<D, T>,
        S extends Scalar<D, T>,
        D extends Domain<T>,
        T> extends TensorDistribution<V, D, T> {

    // param is vector, but distr is univariate

    final public Input<TensorDistribution<S, D, T>> distInput
            = new Input<>("distr",
            "the base distribution for iid, e.g. normal, beta, gamma.",
            Input.Validate.REQUIRED);

    protected TensorDistribution<S, D, T> dist;

    @Override
    public void initAndValidate() {
        dist = distInput.get();
//        domain = dist.
        // param
        super.initAndValidate();
        if (param == null || param.size() <= 1)
            throw new IllegalArgumentException("IID requires param, but it is null ! ");

    }

    @Override
    protected double calcLogP(List<T> value) {
        // value dim == param dim
        if (value.size() != param.size())
            throw new IllegalArgumentException("Value dim does not match param size ! ");
        double logP = 0.0;
        for (T t : value) {
            logP += dist.calcLogP(List.of(t));
        }
        return logP;
    }

    @Override
    protected List<T> sample() {
        throw new UnsupportedOperationException("Directly use sample(State state, Random random)");
    }

    @Override
    public void sample(State state, Random random) {

        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled
        sampleConditions(state, random);

        if (this.param == null)
            throw new IllegalArgumentException("IID requires param, but it is null ! ");
        List<T> newListX = new ArrayList<>(dimension());
        try {
            // sample at each dim();
            for (int i = 0; i < dimension(); i++) {
                // scalar
                T newx = dist.sample().getFirst();
                if (param instanceof Bounded b) {
                    while (!b.withinBounds((Comparable) newx)) {
                        newx = dist.sample().getFirst();
                    }
                }
                newListX.add(newx);
            }

            if (this.param.size() != newListX.size())
                throw new IllegalArgumentException("Samples size needs to match param dimension " + dimension());

            switch (this.param) {
                case RealVectorParam rv -> {
                    for (int i = 0; i < dimension(); i++) {
                        rv.set(i, (Double) newListX.get(i));
                    }
                }
                case IntVectorParam iv -> {
                    for (int i = 0; i < dimension(); i++) {
                        iv.set(i, (Integer) newListX.get(i));
                    }
                }
                case BoolVectorParam bv -> {
                    for (int i = 0; i < dimension(); i++) {
                        bv.set(i, (Boolean) newListX.get(i));
                    }
                }
                default ->
                    throw new IllegalStateException("sample is not implemented yet for scalar that is not a RealScalarParam or IntScalarParam");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to sample!");
        }
    }

}
