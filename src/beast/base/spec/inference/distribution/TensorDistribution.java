package beast.base.spec.inference.distribution;


import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.*;
import beast.base.spec.type.Scalar;
import beast.base.spec.type.Tensor;
import beast.base.spec.type.Vector;
import beast.base.util.Randomizer;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Strong typed {@link Distribution} for {@link Tensor}.
 * @param <S> the shape for sampled value, which could be {@link Vector},
 *           but for {@link Scalar}, use {@link ScalarDistribution}.
// * @param <D> the domain for sampled value, which extends either {@link Real} or {@link Int}.
 * @param <T> the Java primitive type for sampled value, either Double or Integer.
 */
@Description("The BEAST Distribution over a tensor.")
public abstract class TensorDistribution<S extends Tensor<?,T>, T>
        extends Distribution {

    protected static final double EPS = 1e-12;

    // TODO how to change RNG ?
    // MT is reproducible scientific RNG
    protected static UniformRandomProvider rng = RandomSource.MT.create(Randomizer.getSeed());

    // Note this is the same tensor used for the sampled values defined in the class types.
    final public Input<S> paramInput = new Input<>("param",
            "point at which the density is calculated", Validate.OPTIONAL, Tensor.class);

    protected S param;

    @Override
    public void initAndValidate() {
        param = paramInput.get(); // optional
        if (param != null && !param.isValid())
            throw new IllegalArgumentException("Tensor param is not valid ! " + param);
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

    public int dimension() {
        return param != null ? param.size() : 0; //iidparam.size();
    }

    /**
     * Override {@link Distribution#calculateLogP()}.
     * Parameter value is wrapped by tensor S.
     * @return the normalized probability (density) for this distribution.
     */
    @Override
    public double calculateLogP() {
        throw new UnsupportedOperationException("Please override this method in every child class !");
    }

    
    /**
     * @return lower bound of the parameter this tensor distribution applies to
     * or is sampled from
     */
    public abstract T getLowerBoundOfParameter();

    /**
     * @return upper bound of the parameter this tensor distribution applies to
     * or is sampled from
     */
    public abstract T getUpperBoundOfParameter();


    //*** abstract methods ***//

    /**
     * Used by {@link IID} when computing the log probability/density from the base distribution.
     * @param value T in Java type
     * @return  the normalized probability (density) for this distribution.
     */
    protected abstract double calcLogP(T... value);

    /**
     * It is used to sample one data point from this distribution.
     * @return  Use <code>List.of</code> to an immutable list in Java type T containing sample point,
     *          if S is scalar then only 1 element in the list.
     *
     */
    protected abstract List<T> sample();

    //*** Override Distribution methods ***//

    @Override
    public void sample(State state, Random random) {

        if (sampledFlag)
            return;

        sampledFlag = true;

        // Cause conditional parameters to be sampled
        sampleConditions(state, random);
//TODO modified from old code, but not sure how it works without set state
        try {
            param = paramInput.get();
            // param is optional
            if (param != null) {

                switch (param) {
                    case Scalar scalar -> {
                        // sample distribution parameters
                        T newX = sample().getFirst();
                        while (! scalar.isValid(newX)) {
                            newX = sample().getFirst();
                        }
                        switch (scalar) {
                            case RealScalarParam rs -> rs.set((Double) newX);
                            case IntScalarParam is -> is.set((Integer) newX);
                            case BoolScalarParam bs -> bs.set((Boolean) newX);
                            default -> {
                                throw new IllegalStateException("sample is not implemented yet for scalar that is not a RealScalarParam or IntScalarParam");
                            }
                        }
                    }
                    case Vector vector -> {
                        List<T> newListX = null;
                        boolean valid = false; // start loop
                        while (! valid) {
                            newListX = sample();
                            if (vector.size() != newListX.size())
                                throw new IllegalStateException("sample is not implemented yet for vector that is not a Vector");

                            for (int i = 0; i < vector.size(); i++) {
                                valid = vector.isValid(newListX.get(i));
                                if ( ! valid)
                                    break;
                            }
                        } // end while

                        switch (vector) {
                            case RealVectorParam rv -> {
                                for (int i = 0; i < rv.size(); i++)
                                    rv.set(i, (Double) newListX.get(i));
                            }
                            case IntVectorParam iv -> {
                                for (int i = 0; i < iv.size(); i++)
                                    iv.set(i, (Integer) newListX.get(i));
                            }
                            case BoolVectorParam bv -> {
                                for (int i = 0; i < bv.size(); i++)
                                    bv.set(i, (Boolean) newListX.get(i));
                            }
                            default -> {
                                throw new IllegalStateException("sample is not implemented yet for scalar that is not a RealScalarParam or IntScalarParam");
                            }
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected tensor type");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to sample!");
        }
    }

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

}

