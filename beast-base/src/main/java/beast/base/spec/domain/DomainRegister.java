package beast.base.spec.domain;

import java.util.Map;

/**
 * TODO: it is not extendable yet.
 *  Solution: use version.xml to declare domains,
 *   and use the same mechanism as DataTypes to make Domain extendable in packages.
 */
public final class DomainRegister {

    // Prevent instantiation
    private DomainRegister() {}

    /**
     * Include all domain classes here
     */
    private static final Map<String, Object> DOMAINS = Map.of(
            // class name => Java instance
            Real.class.getSimpleName(), Real.INSTANCE,
            NonNegativeReal.class.getSimpleName(), NonNegativeReal.INSTANCE,
            PositiveReal.class.getSimpleName(), PositiveReal.INSTANCE,
            UnitInterval.class.getSimpleName(), UnitInterval.INSTANCE,
            Int.class.getSimpleName(), Int.INSTANCE,
            NonNegativeInt.class.getSimpleName(), NonNegativeInt.INSTANCE,
            PositiveInt.class.getSimpleName(), PositiveInt.INSTANCE,
            Bool.class.getSimpleName(), Bool.INSTANCE

            // TODO: more domain types ...
    );

    /**
     * Create the instance of a domain class given its name in string.
     * @param stringValue   the name of a domain class
     * @return  the instance of a {@link Domain} class
     * @param <D> {@link Domain}
     */
    @SuppressWarnings("unchecked")
    public static <D> D fromString(String stringValue) {
        Object value = DOMAINS.get(stringValue);
        if (value == null) {
            throw new IllegalArgumentException("The domain type " + stringValue + " is not supported !");
        }
        return (D) value;
    }

}
