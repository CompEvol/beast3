package beast.base.spec.domain;

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
public class Bool implements Domain<Boolean> {
    public static final Bool INSTANCE = new Bool();

    protected Bool() {}

    @Override
    public boolean isValid(Boolean value) {
        return value != null;
    }

    @Override
    public Class<Boolean> getTypeClass() {
        return Boolean.class;
    }
}