package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.inference.Distribution;
import beast.base.spec.type.Scalar;

/**
 * Strong typed {@link Distribution} for {@link Scalar}.
 * @param <S> the shape for sampled value, which could be {@link Scalar}
 * @param <T> the Java primitive type for sampled value, either Double or Integer.
 */
@Description("The BEAST Distribution over a tensor.")
public abstract class ScalarDistribution<S extends Scalar<?,T>, T>
        extends TensorDistribution<S,T> {

    //*** abstract methods ***//

    /**
     * Used by {@link IID} when computing the log probability/density from the base distribution.
     * @param value T in Java type
     * @return  the normalized probability (density) for this distribution.
     */
    protected abstract double calcLogP(T value);

    @Override
    protected double calcLogP(T... value) {
        throw new IllegalArgumentException("Illegal operation !");
    }

}

