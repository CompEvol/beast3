package beast.base.spec.constraints;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ConstraintSet<T extends Constrainable> extends Constraint<T> {
    
    Constraint<T>[] constraints;
    
    @SafeVarargs
    public ConstraintSet(Constraint<T>... constraints) {
        this.constraints = constraints;
    }

    @Override
    public boolean check(Constrainable arg) {
        for (Constraint<T> c : constraints)
            if (!c.check(arg))
                return false;
        return true;
    }

    /**
     * Infer the applicable type from inner constraints.
     * @return The type of constrainable this constraint can validate
     */
    @Override
    public Class<? extends T> getApplicableType() {
        if (constraints.length == 0) {
            return (Class<? extends T>) Constrainable.class;
        }
        return constraints[0].getApplicableType();
    }

    /**
     * Determines whether all constraints in the set are applicable to the type of the given value.
     */
    @Override
    public boolean isApplicable(Object value) {
        for (Constraint<?> c : constraints)
            if (!c.isApplicable(value))
                return false;
        return true;
    }
        
    @Override
    public String getDescription() {
        return "ConstraintSet[" + 
            Arrays.stream(constraints)
                .map(Constraint::getDescription)
                .collect(Collectors.joining(",")) + 
            "]";
    }
}
