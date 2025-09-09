package beast.base.spec.domain;

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
public class Int implements Domain<Integer> {
    public static final Int INSTANCE = new Int();

    protected Int() {}

    @Override
    public boolean isValid(Integer value) {
        return value != null;
    }

    @Override
    public Class<Integer> getTypeClass() {
        return Integer.class;
    }

    @Override
    public Integer getLower() {
        return null;
    }

    @Override
    public Integer getUpper() {
        return null;
    }
}