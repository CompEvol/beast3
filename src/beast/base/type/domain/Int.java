package beast.base.type.domain;

/**
 * Integer type.
 *
 * Represents a whole number (positive, negative, or zero).
 * Note: This interface is named Integer to match PhyloSpec conventions,
 * but uses the fully qualified java.lang.Integer when needed to avoid conflicts.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public interface Int extends Domain<Integer> {

    @Override
    default public boolean isValid(Integer value) {
        return value != null;
    }
}