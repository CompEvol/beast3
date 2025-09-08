package beast.base.spec.domain;

/**
 * Domain defines constraints and validation.
 *
 * @param <T> is the Java type this domain works with.
 */
public interface Domain<T> {

    /**
     * Check if a value is valid for this primitive type.
     */
    boolean isValid(T value);

    Class<T> getTypeClass();

}

