package beast.base.spec.domain;

/**
 * Domain defines constraints and validation.
 *
 * @param <T> is the Java type this domain works with.
 */
public interface Domain<T> {

    /**
     * Check if a value is valid for this domain type.
     * <p>
     * To customize how value ranges are validated in {@link #isValid(T)},
     * override the four bound-related methods:
     * {@link beast.base.spec.Bounded#getLower()}, {@link beast.base.spec.Bounded#getUpper()},
     * {@link beast.base.spec.Bounded#lowerInclusive()},
     * and {@link beast.base.spec.Bounded#upperInclusive()}.
     * </p>
     */
    boolean isValid(T value);

    Class<T> getTypeClass();

}

