package beast.pkgmgmt.plan;

/**
 * A package's need for another package, as in {@code <depends on='SA' atleast='2.2.0'/>} or a
 * POM dependency. Besides real packages, {@link Planner#LAUNCHER} and {@link Planner#JAVAFX}
 * name what the installer provides.
 *
 * @param name    the package needed
 * @param atLeast lowest acceptable version, or null for any
 * @param atMost  highest acceptable version, or null; for BEAST.base and BEAST.app a missing
 *                maximum means "the same major version as atLeast"
 */
public record Dependency(String name, Version atLeast, Version atMost) {

    public static Dependency atLeast(String name, String version) {
        return new Dependency(name, Version.of(version), null);
    }

    /** whether {@code v} meets this dependency; sameMajor applies the implicit maximum */
    boolean accepts(Version v, boolean sameMajor) {
        if (atLeast != null && !v.isAtLeast(atLeast)) {
            return false;
        }
        if (atMost != null) {
            return atMost.isAtLeast(v);
        }
        return !(sameMajor && atLeast != null && v.major() != atLeast.major());
    }

    /** e.g. "BEAST.base 2.9.0 or later" */
    String describe(boolean sameMajor) {
        StringBuilder b = new StringBuilder(name);
        if (atLeast != null && atMost != null) {
            b.append(' ').append(atLeast).append(" to ").append(atMost);
        } else if (atLeast != null) {
            b.append(' ').append(atLeast).append(sameMajor ? " or a later " + atLeast.major() + ".x" : " or later");
        } else if (atMost != null) {
            b.append(' ').append(atMost).append(" or earlier");
        }
        return b.toString();
    }
}
