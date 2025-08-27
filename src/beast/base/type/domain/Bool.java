package beast.base.type.domain;

/**
 * Boolean type.
 *
 * Represents a true/false value.
 * Used for binary switches, flags, and binary character data.
 *
 * Note: This interface is named Boolean to match PhyloSpec conventions,
 * but uses the fully qualified java.lang.Boolean when needed to avoid conflicts.
 *
 * @author PhyloSpec Contributors
 * @since 1.0
 */
public interface Bool extends Domain<Boolean> {

	@Override
    default public boolean isValid(Boolean value) {
        return value != null;
    }
}