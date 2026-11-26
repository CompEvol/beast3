package beast.base.spec.type;

import java.util.ArrayList;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.spec.Bounded;
import beast.base.spec.domain.Domain;
import beast.base.spec.inference.distribution.TensorDistribution;

/**
 * Tensor type - ordered value or values.
 *
 * @param <D> the type of {@link Domain}.
 * @param <T> the primitive type in Java.
 */
public interface Tensor<D extends Domain<T>, T> {

    /**
     * Calling <code>get()</code> when idx is an empty array <code>idx.length == 0</code>.
     * Calling <code>get(0)</code> when idx is a single-element array,
     * where <code>idx.length == 1; idx[0] == 0</code>.
     * Calling <code>get(1,2)</code> for multidimensional indexing.
     * <p>
     * Get the primitive value with the type T.
     * Primitive numeric types (e.g. double) are typically 10×–50× faster than
     * boxed types (e.g. Double) in computation and 3×–4× smaller in memory.
     * Use boxed value T only when required for API or nullability.
     *
     * @param idx  index/indices, depending on the dimension
     * @return  the primitive value with the type T in Java.
     */
    T get(int... idx);

    /**
     * Get the domain type D.
     *
     * @return the domain type, e.g. Real, Int, ...
     */
    D getDomain();

    /**
     * The order or degree of a tensor.
     * For example, Scalar → rank = 0 (no indices),
     * Vector → rank = 1 (needs 1 index),
     * Matrix → rank = 2 (needs 2 indices).
     *
     * @return  the number of indices needed to uniquely identify one of its elements.
     */
    int rank();

    /**
     * The size(s) along each dimension.
     *
     * @return empty for Scalar, one integer for Vector, two integers for Matrix, e.g. [3,4].
     */
    int[] shape();

    default int size(){
        int s=1;
        for(int d:shape())
            s*=d;
        return s;
    }

    /**
     * Validate that this instance satisfies the type constraints.
     *
     * @return true if this instance is valid according to its type constraints, false otherwise
     */
    boolean isValid(T value);


    default boolean isValid(){ return isValid(get()); }

    /**
     * Says why the value is not valid, for an error message. Call it only after
     * {@link #isValid()} is false: it is not used on the MCMC path. The default names
     * the first element that is not valid, with the bounds if the tensor has them.
     * A type that adds a rule to {@link #isValid()} should override this to explain
     * that rule (issue #253).
     *
     * @return the reason, or {@code null} if no element is found that is not valid
     */
    default String whyNotValid() {
        for (int i = 0; i < size(); i++) {
            T value = get(i);
            if (!isValid(value)) {
                String element = size() > 1 ? "Element " + (i + 1) + " (" + value + ")" : "It";
                return element + " " + whyValueNotValid(value) + ".";
            }
        }
        return null;
    }

    /**
     * The message for a value this parameter rejects (#332): it names the parameter, and the
     * element of a vector, and says whether the value is outside the domain or outside the
     * bounds that priors set, e.g. "gammaShape = 5.1 is outside the allowed range [0.1,5.0],
     * set by GammaShapePrior" or "gammaShape = -1.0 is not in the domain PositiveReal".
     *
     * @param value the rejected value
     * @param index the element of a vector (0-based), or -1 for a scalar
     */
    default String invalidValueMessage(T value, int index) {
        String name = this instanceof BEASTInterface b && b.getID() != null ? b.getID() : "the parameter";
        if (index >= 0 && size() > 1) {
            name += " element " + (index + 1);
        }
        return name + " = " + value + " " + whyValueNotValid(value);
    }

    /**
     * Why a value is not valid: "is not in the domain X", or "is outside the allowed range
     * [a,b]", naming the priors that narrow the domain's range, if any.
     */
    private String whyValueNotValid(T value) {
        D domain = getDomain();
        if (value == null || !domain.isValid(value) || !(this instanceof Bounded<?> bounded)) {
            return "is not in the domain " + domain.getClass().getSimpleName();
        }
        String priors = boundingPriors();
        return "is outside the allowed range " + bounded.boundsToString()
                + (priors.isEmpty() ? "" : ", set by " + priors);
    }

    /** the IDs of the distributions on this parameter whose support is narrower than the domain */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String boundingPriors() {
        if (!(this instanceof BEASTInterface b) || b.getID() == null || !(getDomain() instanceof Bounded domainBounds)) {
            return "";
        }
        List<String> ids = new ArrayList<>();
        for (BEASTInterface o : b.getOutputs()) {
            if (o instanceof TensorDistribution<?, ?> d && !d.isAwaitingInit() && d.getArguments().contains(b.getID())) {
                try {
                    Comparable lower = (Comparable) d.getLowerBoundOfParameter();
                    Comparable upper = (Comparable) d.getUpperBoundOfParameter();
                    boolean narrower = (lower != null && lower.compareTo(domainBounds.getLower()) > 0)
                            || (upper != null && upper.compareTo(domainBounds.getUpper()) < 0);
                    if (narrower) {
                        ids.add(d.getID() != null ? d.getID() : d.getClass().getSimpleName());
                    }
                } catch (RuntimeException e) {
                    // a distribution that can't give its bounds is left out of the message
                }
            }
        }
        return String.join(", ", ids);
    }
}