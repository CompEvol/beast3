package beast.base.spec.inference.distribution;

import beast.base.core.Input;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of ({@link Scalar}) is independent and identically distributed.
 * @param <V>  samples
 * @param <S>  the sample type S of the distribution, see {@link TensorDistribution}.
 * @param <D>  domain {@link Domain}
 * @param <T>  Java type
 */
public class IID<V extends Vector<D, T>,
        S extends Scalar<D, T>,
        D extends Domain<T>,
        T> extends TensorDistribution<V, D, T> {

    // param is vector, but distr is univariate

    final public Input<TensorDistribution<S, D, T>> distInput
            = new Input<>("distr",
            "the base distribution for iid, e.g. normal, beta, gamma.",
            Input.Validate.REQUIRED);

    protected TensorDistribution<S, D, T> dist;
    protected D domain;

    @Override
    public void initAndValidate() {
        dist = distInput.get();
//        domain = dist.
        // param
        super.initAndValidate();
        if (! (param instanceof V) )
            throw new IllegalArgumentException("Incorrect type of param " + param +
                    " for IID, vector is required !");
    }

    @Override
    public double calculateLogP() {
        logP = 0;
        param = paramInput.get();
        for (int i = 0; i < param.size(); i++) {
            // TODO can put the generic types
            if (dist instanceof RealTensorDistribution realDist) {
                if (param instanceof RealVector realVector) {
                    final double x = realVector.get(i);
                    logP += realDist.logDensity(x);
                } else
                    throw new IllegalArgumentException("Param type " + param.getClass() +
                            " doesn't match base distribution type " + realDist.getClass());
            } else if (dist instanceof IntTensorDistribution intDist) {
                if (param instanceof IntVector intVector) {
                    final int x = intVector.get(i);
                    logP += intDist.logProbability(x);
                } else
                    throw new IllegalArgumentException("Param type " + param.getClass() +
                            " doesn't match base distribution type " + intDist.getClass());
            } else
                throw new IllegalStateException("Unexpected base distribution type " + dist.getClass());
        }
        return logP;
    }

    @Override
    public List<V> sample(int size) {
        List<V> samples = new ArrayList<>(size);
        int dim = dimension();
        if (dist instanceof RealTensorDistribution realDist) {
            for (int i = 0; i < size; i++) {
                double[] baseSamples = new double[dim];
                for (int j = 0; j < dim; j++) {
                    S s = (S) realDist.sample();
                    if (s instanceof RealScalar realScalar) {
                        baseSamples[j] = realScalar.get();
                    } else
                        throw new IllegalArgumentException("Sampled value type " + s.getClass() +
                        " doesn't match base distribution type " + realDist.getClass());
//                    V v = (V) create(baseSamples, s.getDomain());
//                    samples.add(v);
                }
            }
        } else if (dist instanceof IntTensorDistribution intDist) {
            for (int i = 0; i < size; i++) {
                int[] baseSamples = new int[dim];
                for (int j = 0; j < dim; j++) {
                    S s = (S) intDist.sample();
                    if (s instanceof IntScalar intScalar) {
                        baseSamples[j] = intScalar.get();
                    } else
                        throw new IllegalArgumentException("Sampled value type " + s.getClass() +
                                " doesn't match base distribution type " + intDist.getClass());
                    D d = s.getDomain();
//                    V v = new IntVectorParam<>(baseSamples, d);
//                    samples.add(v);
                }
            }
        } else
            throw new IllegalStateException("Unexpected base distribution type " + dist.getClass());

        return samples;
    }

    public static <D extends Real> RealVectorParam<D> create(double[] values, D domain) {
        // You need to know which concrete V to create
        return new RealVectorParam<>(values, domain);
    }

    @Override
    public T getOffset() {
        return null;
    }

}
