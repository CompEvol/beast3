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
import beast.base.spec.type.*;
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
public abstract class TensorDistribution<S extends Tensor<D,T>, D extends Domain<T>, T> extends Distribution {

    public static final double EPS = 1e-12;

    // TODO how to change RNG ?
    // MT is reproducible scientific RNG
    protected static UniformRandomProvider rng = RandomSource.MT.create();

    // Note this is the same tensor used for the sampled values defined in the class types.
    final public Input<S> paramInput = new Input<>("param",
            "point at which the density is calculated", Validate.REQUIRED, Tensor.class);

    protected S param;

    @Override
    public void initAndValidate() {
        param = paramInput.get();

        if (! param.isValid())
            throw new IllegalArgumentException("Tensor param is not valid ! " + param);

        calculateLogP();
    }

    //*** abstract methods ***//

    public abstract double logProb(final T x);

    public abstract List<S> sample(int size);

    public S sample() {
        return sample(1).getFirst();
    }

    /**
     * @return  offset of distribution.
     */
    public abstract T getOffset();


    //*** Override Distribution methods ***//

    @Override
    public double calculateLogP() {
        logP = 0;

        param = paramInput.get();
        switch (param) {
            case Scalar scalar -> {
                if (!scalar.isValid(scalar.get())) return Double.NEGATIVE_INFINITY;
                final T x = (T) scalar.get();
                logP += logProb(x);
            }
            case Vector vector -> {
                if (!vector.isValid())
                    return Double.NEGATIVE_INFINITY;
                for (int i = 0; i < vector.size(); i++) {
                    final T x = (T) vector.get(i);
                    logP += logProb(x);
                }
            }
            default -> throw new IllegalStateException("Unexpected tensor type");
        }
        return logP;
    }

    @Override
    public void sample(State state, Random random) {

        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled
        sampleConditions(state, random);

        // sample distribution parameters
        S newx;
        try {
            newx = sample();

            param = paramInput.get();
            switch (param) {
                case Scalar scalar -> {
                    if (scalar instanceof Bounded b) {
                        while (!b.withinBounds((Comparable) newx)) {
                            newx = sample();
                        }
                    }
                    if (scalar instanceof RealScalarParam rs)
                        rs.set( ((RealScalar) newx).get() );
                    else if (scalar instanceof IntScalarParam is)
                        is.set( ((IntScalar) newx).get() );
                    else if (scalar instanceof BoolScalarParam bs)
                        bs.set( ((BoolScalar) newx).get() );

                    throw new RuntimeException("sample is not implemented yet for scalar that is not a RealScalarParam or IntScalarParam");
                }
                case Vector vector -> {

                    //TODO

                    throw new UnsupportedOperationException("sample is not implemented yet for vector parameters");
                }
                default -> throw new IllegalStateException("Unexpected tensor type");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to sample!");
        }
    }

    @Override
    public List<String> getConditions() {
        List<String> conditions = new ArrayList<>();
        // dist.getID()
        conditions.add(getID());
        return conditions;
    }

    @Override
    public List<String> getArguments() {
        List<String> arguments = new ArrayList<>();
        // TODO safe cast ?
        String id = ((BEASTInterface) param).getID();
        arguments.add(id);
        return arguments;
    }

    // used by unit test
    public double calcLogP(Tensor<D, T> tensor) {
        paramInput.setValue(tensor, this);
        return calculateLogP();
    }

}

