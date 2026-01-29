package beast.base.spec.constraints;

import beast.base.evolution.tree.Tree;

/**
 * Constraint for Tree inputs. Nested classes provide concrete implementations
 * for ultrametricity and sampled ancestor constraints.
 */
abstract public class TreeConstraint extends Constraint<Tree> {

    @Override
    public Class<? extends Tree> getApplicableType() {
        return Tree.class;
    }

    /* Concrete types of tree constraints */

    static public class RequireSampledAncestors extends TreeConstraint {

        @Override
        public boolean check(Constrainable arg) {
            if (arg instanceof Tree tree)
                return tree.allowSampledAncestors();
            else
                return false;
        }
    }

    static public class ForbidSampledAncestors extends TreeConstraint {

        @Override
        public boolean check(Constrainable arg) {
            if (arg instanceof Tree tree)
                return !tree.allowSampledAncestors();
            else
                return false;
        }
    }

    static public class RequireUltrametric extends TreeConstraint {

        @Override
        public boolean check(Constrainable arg) {
            if (arg instanceof Tree tree)
                return tree.isUltrametric();
            else
                return false;
        }
    }

    static public class ForbidUltrametric extends TreeConstraint {

        @Override
        public boolean check(Constrainable arg) {
            if (arg instanceof Tree tree)
                return !tree.isUltrametric();
            else
                return false;
        }
    }

}
