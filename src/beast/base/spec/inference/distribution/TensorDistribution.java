package beast.base.spec.inference.distribution;


import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.BoolScalarParam;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;
import beast.base.spec.type.Vector;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Strong typed {@link Distribution} for {@link Tensor}.
 * @param <S> the shape for sampled value, which could be {@link Scalar} or {@link Vector}
 * @param <D> the domain for sampled value, which extends either {@link Real} or {@link Int}.
 * @param <T> the Java primitive type for sampled value, either Double or Integer.
 */
@Description("The BEAST Distribution over a tensor.")
public abstract class TensorDistribution<S extends Tensor<D,T>, D extends Domain<T>, T>
        extends Distribution {

    protected static final double EPS = 1e-12;

    // TODO how to change RNG ?
    // MT is reproducible scientific RNG
    protected static UniformRandomProvider rng = RandomSource.MT.create();

    // Note this is the same tensor used for the sampled values defined in the class types.
    final public Input<S> paramInput = new Input<>("param",
            "point at which the density is calculated", Validate.OPTIONAL, Tensor.class);
//    final public Input<List<S>> iidparamInput = new Input<>("iidparam",
//            "multiple point at which the density is calculated using IID", Validate.XOR, List.class);

    protected S param;
//    protected List<S> iidparam;

    @Override
    public void initAndValidate() {
        param = paramInput.get();
//        iidparam = iidparamInput.get();
//        if (param == null && iidparam == null || param != null && iidparam != null)
//            throw new IllegalArgumentException("Only one of param and iidparam input can be specified !");
        if (param != null && !param.isValid())
            throw new IllegalArgumentException("Tensor param is not valid ! " + param);
//        if (iidparam != null)
//            for (S s : iidparam)
//                if (s != null && !s.isValid())
//                    throw new IllegalArgumentException("Param in IID is not valid ! " + s);
    }

    //*** abstract methods ***//

    /**
     * Override {@link beast.base.inference.Distribution#calculateLogP()}.
     * Parameter value is wrapped by tensor S.
     * @return the normalized probability (density) for this distribution.
     */
    @Override
    public double calculateLogP() {
        logP = 0;
        if (param != null) {
            param = paramInput.get();
            if (param instanceof Scalar scalar) {
                // single value
                T val = (T) scalar.get();
                logP += calcLogP(List.of(val));

            } else if (param instanceof Vector vector) {
//                @SuppressWarnings("unchecked")
//                T[] vals = (T[]) Array.newInstance(vector.get(0).getClass(), vector.size());
                List<T> vals = vector.getElements();
                logP += calcLogP(vals);
            } else
                throw new IllegalStateException("Unexpected tensor type");
//        } else if (iidparam != null) {
//            // if IID
//            for (S s : iidparam) {
//                List<T> values = getTensorValue(s);
//                for (T value : values)
//                    logP += calcLogP(value);
//            }
        }
        return logP;
    }

    /**
     * Implement this case by case to compute the log-density or log-probability.
     * @param value T in Java type
     * @return  the normalized probability (density) for this distribution.
     */
    protected abstract double calcLogP(List<T> value);
    //TODO difficult to handle T... value, because varargs are actually T[] at runtime.
    // easy to get ClassCastException

    /**
     * It is used to sample one data point from this distribution.
     * @return  Use <code>List.of</code> to an immutable list in Java type T containing sample point,
     *          if S is scalar then only 1 element in the list.
     *
     */
    protected abstract List<T> sample();

    public int dimension() {
        return param != null ? param.size() : 0; //iidparam.size();
    }

    // unwrap values
    @Deprecated
//    private List<T> getTensorValue(Tensor<D,T> tensor) {
//        if (tensor instanceof Scalar<D,T> scalar)
//            return List.of(scalar.get());
//        else if (tensor instanceof Vector<D,T> vector)
//            return vector.getElements();
//        else
//            throw new IllegalStateException("Unexpected tensor type");
//    }

    //*** Override Distribution methods ***//

    @Override
    public void sample(State state, Random random) {

        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled
        sampleConditions(state, random);

        try {
            // sample distribution parameters
            List<T> newListX = sample();

            if (param != null) {
                param = paramInput.get();
                setNewValue(param, newListX.getFirst());
//            } else if (iidparam != null) {
//                iidparam = iidparamInput.get();
//                setNewValue(iidparam, newListX);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to sample!");
        }
    }

    private void setNewValue(S param, T newx) {
        switch (param) {
            case Scalar scalar -> {
                if (param instanceof Bounded b) {
                    while (!b.withinBounds((Comparable) newx)) {
                        newx = sample().getFirst();
                    }
                }
                switch (scalar) {
                    case RealScalarParam rs -> rs.set((Double) newx);
                    case IntScalarParam is -> is.set((Integer) newx);
                    case BoolScalarParam bs -> bs.set((Boolean) newx);
                    default -> {
                        throw new IllegalStateException("sample is not implemented yet for scalar that is not a RealScalarParam or IntScalarParam");
                    }
                }
            }
            case Vector vector -> {

                //TODO

                throw new UnsupportedOperationException("sample is not implemented yet for vector parameters");
            }
            default -> throw new IllegalStateException("Unexpected tensor type");
        }
    }

//    private void setNewValue(List<S> iidparam, List<S> newListX) {
//        assert iidparam.size() == newListX.size();
//        for (int i = 0; i < iidparam.size(); i++) {
//            setNewValue(iidparam.get(i), newListX.get(i));
//        }
//    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<>();
        // TODO check
        conditions.add(getID());
        return conditions;
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();
        // TODO id for IID ?
        String id = param != null ? ((BEASTInterface) param).getID() : null;
        arguments.add(id);
        return arguments;
    }

    /**
     * Floating point comparison. Make it public for other packages to use
     * @param a
     * @param b
     * @return   if the two double numbers are equal.
     */
    public static boolean isNotEqual(double a, double b) {
        return Math.abs(a - b) > EPS;
    }

}

