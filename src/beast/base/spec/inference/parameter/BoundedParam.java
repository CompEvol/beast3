package beast.base.spec.inference.parameter;

import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.spec.Bounded;
import beast.base.spec.type.Tensor;
import org.w3c.dom.Node;

/**
 * This defines a lower and upper limit, with the option
 * to specify whether each bound is inclusive or exclusive.
 * It also provides a validation method to check whether a given value
 * lies within the defined range.
 *
 * @param <T> the numeric or comparable type of the value (e.g. {@link Double}, {@link Integer})
 */
public interface BoundedParam<T extends Comparable<T>> extends Bounded<T> {

    /**
     * set the lower bound of the range.
     */
    void setLower(T value);

    /**
     * set the upper bound of the range.
     */
    void setUpper(T value);

    /**
     * set the lower and upper bound of the range.
     */
    default void setBounds(T lower, T upper) {
        setLower(lower);
        setUpper(upper);
    }

    default void initBounds(Input<T> lowerInput, Input<T> upperInput,
                            T domainLower, T domainUpper) {
        // if input is given, then use it, otherwise use the default
        T lower = (lowerInput.get() != null) ? lowerInput.get() : getLower();
        T upper = (upperInput.get() != null) ? upperInput.get() : getUpper();

        // adjust bounds to the Domain range
        adjustBounds(lower, upper, domainLower, domainUpper);
    }

    // adjust bounds to the Domain range
    default void adjustBounds(T lower, T upper, T domainLower, T domainUpper) {
        setBounds(BoundedParam.max(lower, domainLower),
                BoundedParam.min(upper, domainUpper) );
    }

    static <T extends Comparable<? super T>> T max(T a, T b) {
        return (a.compareTo(b) >= 0) ? a : b;
    }

    static <T extends Comparable<? super T>> T min(T a, T b) {
        return (a.compareTo(b) <= 0) ? a : b;
    }

    // for resume

    /**
     * Used internally by {@link beast.base.inference.StateNode#fromXML(Node)} to restore
     * a state that was previously serialized via {@link StateNode#toXML()}.
     * <p>
     * The {@link StateNode#toXML()} method, in turn, writes out the state
     * using {@link StateNode#toString()}.
     *
     * @param lower     lower bound in string
     * @param upper     upper bound in string
     * @param shape     null for Scalar, one integer for Vector, two integers for Matrix, e.g. [3,4].
     *                  It is created by {@link beast.base.spec.type.TypeUtils#shapeToString(Tensor)}.
     * @param valuesStr the value(s) in string
     */
    void fromXML(final String lower, final String upper, final String shape, final String... valuesStr);

}
