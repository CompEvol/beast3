package beast.base.spec.inference.distribution;

import beast.base.core.Input;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Vector;

import java.util.ArrayList;
import java.util.List;

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
    protected D domain;

    @Override
    public void initAndValidate() {
        dist = distInput.get();
//        domain = dist.
        // param
        super.initAndValidate();
        if (param == null && param.size() <= 1)
            throw new IllegalArgumentException("IID requires param, but it is null ! ");

    }

    @Override
    protected double calcLogP(T... value) {
        if (value.length != dimension())
            throw new IllegalArgumentException("Given value dimension must equal to " +
                    " param dimension in IID ! " + value.length + " != " + dimension());

        logP = 0;
        for (T v : value) {
            // use base dist, assuming each dim independent
            logP += dist.calcLogP(v);
        }
        return logP;
    }

    @Override
    protected List<V> sample() {
        List<T> oneSample = new ArrayList<>();
        for (int i = 0; i < dimension(); i++) {
            // scalar
            List<S> valueI = dist.sample();
            S s = valueI.getFirst();
            oneSample.add(s.get());
        }
        // convert Scalar to Vector

        D d = param.getDomain();
        if (d instanceof Real real) {
            double[] values = oneSample.stream()
                    .mapToDouble(v -> ((Number) v).doubleValue())
                    .toArray();
            return List.of( (V) new RealVectorParam<>(values, real) );
        } else if (d instanceof Int inte) {
            int[] values = oneSample.stream()
                    .mapToInt(v -> ((Number) v).intValue())
                    .toArray();
            return List.of( (V) new IntVectorParam<>(values, inte) );
        }
        //TODO more ...
        throw new IllegalStateException("IID sampling is not implemented yet for " + param.getClass());
    }

//    <D extends Real, Double>  private Vector<D, Double> convert(List<Double> oneSample, D domain) {
//        double[] values = oneSample.stream()
//                .mapToDouble(v -> ((Number) v).doubleValue())
//                .toArray();
//        return new RealVectorParam<>(values, domain);
//    }


    <D extends Real, T> RealVectorParam<D> convert(List<T> oneSample, D domain) {
        double[] values = oneSample.stream()
                .mapToDouble(v -> ((Number) v).doubleValue())
                .toArray();
        return new RealVectorParam<>(values, domain);
    }

}
