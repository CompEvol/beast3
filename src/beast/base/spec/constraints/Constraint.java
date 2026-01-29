package beast.base.spec.constraints;

/**
 * Interface for constraints on BEASTObject inputs.
 * Constraints validate properties of input values beyond simple type checking.
 * 
 * @param <T> The type of BEASTObject this constraint applies to
 */
public abstract class Constraint<T extends Constrainable> {
    
    /**
     * @return The type of constrainable this constraint can validate
     */
    abstract public Class<? extends T> getApplicableType();
    
    /**
     * Check if this constraint can be applied to the given value.
     * By default, checks if value is an instance of getApplicableType().
     * 
     * @param value The value to check
     * @return true if this constraint can validate the value
     */
    public boolean isApplicable(Object value) {
        return getApplicableType().isInstance(value);
    }

    /**
     * Validate the given value against this constraint.
     * 
     * @param arg The value to validate
     * @return true if the value satisfies the constraint
     */
    abstract public boolean check(Constrainable arg);
    
    /**
     * Get a human-readable description of what this constraint requires.
     * Used in error messages when validation fails.
     */
    public String getDescription() {
        return getClass().getSimpleName();
    }

    /**
     * Placeholder constraint used when specification of the actual constraint is deferred to subclasses.
     */
    static public class Placeholder extends Constraint<Constrainable> {

        @Override
        public Class<Constrainable> getApplicableType() {
            throw new UnsupportedOperationException("Unimplemented method 'getApplicableType' for Constraint.Placeholder");
        }

        @Override
        public boolean check(Constrainable arg) {
            throw new UnsupportedOperationException("Unimplemented method 'check' for Constraint.Placeholder");
        }
    }

    /**
     * Constraint that always passes validation.
     */
    static public class Unconstrained extends Constraint<Constrainable> {

        @Override
        public Class<Constrainable> getApplicableType() {
            return Constrainable.class;
        }

        @Override
        public boolean check(Constrainable arg) {
            return true;
        }
    }
}
