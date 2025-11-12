package beast.base.spec.inference.distribution;

import beast.base.core.Input;
import beast.base.inference.State;
import beast.base.spec.domain.Domain;
import beast.base.spec.inference.parameter.BoolScalarParam;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.Scalar;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A collection of ({@link Scalar}) is independent and identically distributed.
 * @param <S>  the sample type S of the distribution, see {@link TensorDistribution}.
 * @param <D>  domain {@link Domain}
 * @param <T>  Java type
 */
public class IID2<S extends Scalar<D, T>,
        D extends Domain<T>,
        T> extends TensorDistribution<S, D, T> {

    // param in IID is vector, but distr is univariate
    final public Input<List<S>> iidparamInput = new Input<>("iidparam",
            "multiple point at which the density is calculated using IID",
            Input.Validate.REQUIRED, List.class);

    // the param in distr is null
    final public Input<TensorDistribution<S, D, T>> distInput
            = new Input<>("distr",
            "the base distribution for iid, e.g. normal, beta, gamma.",
            Input.Validate.REQUIRED);

    protected TensorDistribution<S, D, T> dist;
    protected List<S> iidparam;


    public IID2() {
        paramInput.setRule(Input.Validate.FORBIDDEN);
    }

    public IID2(List<S> iidparam, TensorDistribution<S, D, T> dist) {

        try {
            initByName("iidparam", iidparam, "distr", dist);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        dist = distInput.get();
        // param
        iidparam = iidparamInput.get();
        if (iidparam == null || dimension() <= 1)
            throw new IllegalArgumentException("IID requires param, but it is null ! ");

        for (S s : iidparam)
            if (s != null && !s.isValid())
                throw new IllegalArgumentException("Param in IID is not valid ! " + s);

    }

    @Override
    public int dimension() {
        return iidparam != null ? iidparam.size() : 0;
    }

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

    @Override
    public void sample(State state, Random random) {

        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled
        sampleConditions(state, random);

        if (this.iidparam == null)
            throw new IllegalArgumentException("IID requires param, but it is null ! ");

        try {
            // sample at each dim();
            for (int i = 0; i < dimension(); i++) {
                // scalar
                S x = this.iidparam.get(i);
                // sample distribution parameters
                T newX = dist.sample().getFirst();
                while (! x.isValid(newX)) {
                    newX = dist.sample().getFirst();
                }
                switch (x) {
                    case RealScalarParam rs -> rs.set((Double) newX);
                    case IntScalarParam is -> is.set((Integer) newX);
                    case BoolScalarParam bs -> bs.set((Boolean) newX);
                    default -> {
                        throw new IllegalStateException("sample is not implemented yet for scalar that is not a RealScalarParam or IntScalarParam");
                    }
                }

                //TODO what about S is vector or matrix?
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to sample!");
        }
    }

}
