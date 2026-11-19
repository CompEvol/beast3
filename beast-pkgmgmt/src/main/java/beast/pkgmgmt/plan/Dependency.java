package beast.pkgmgmt.plan;

/**
 * A package's need for another package: {@code <depends on='SA' atleast='2.2.0'/>} in a ZIP
 * package's version.xml, or a dependency in a Maven package's POM, as a plain version or a Maven
 * range such as {@code [2.8.0-rc3,2.9.0-alpha)}. Besides real packages, {@link Planner#LAUNCHER}
 * and {@link Planner#JAVAFX} name what the installer provides.
 *
 * @param name         the package needed
 * @param atLeast      lower bound, or null for none
 * @param atMost       upper bound, or null; for BEAST.base and BEAST.app a missing upper bound
 *                     means "the same minor version as atLeast": a package built for 2.8.x is not
 *                     loaded on 2.9, since minor releases of BEAST have broken packages
 * @param lowerOpen    whether atLeast itself is excluded, as in {@code (2.8.0,...}
 * @param upperOpen    whether atMost itself is excluded, as in {@code ...,2.9.0-alpha)}
 */
public record Dependency(String name, Version atLeast, Version atMost, boolean lowerOpen, boolean upperOpen) {

    /** inclusive bounds, as version.xml's atleast and atmost */
    public Dependency(String name, Version atLeast, Version atMost) {
        this(name, atLeast, atMost, false, false);
    }

    public static Dependency atLeast(String name, String version) {
        return new Dependency(name, Version.of(version), null);
    }

    /**
     * From a POM: a plain version is a minimum, as Maven treats it (a soft requirement);
     * {@code [a,b)}, {@code (a,b]}, {@code [a,)}, {@code (,b]} and {@code [a]} are Maven ranges.
     *
     * @throws IllegalArgumentException for a union of ranges ({@code [1,2),[3,4)}) or bad syntax
     */
    public static Dependency fromMaven(String name, String spec) {
        String s = spec.trim();
        if (!s.startsWith("[") && !s.startsWith("(")) {
            return new Dependency(name, Version.of(s), null);
        }
        if (!s.endsWith("]") && !s.endsWith(")")) {
            throw new IllegalArgumentException("not a Maven version range: " + spec);
        }
        String inner = s.substring(1, s.length() - 1);
        if (inner.contains("[") || inner.contains("(") || inner.contains("]") || inner.contains(")")) {
            throw new IllegalArgumentException("a union of version ranges is not supported: " + spec);
        }
        boolean lowerOpen = s.startsWith("("), upperOpen = s.endsWith(")");
        int comma = inner.indexOf(',');
        if (comma < 0) {
            Version exact = Version.of(inner);
            return new Dependency(name, exact, exact, false, false);
        }
        String lo = inner.substring(0, comma).trim(), hi = inner.substring(comma + 1).trim();
        return new Dependency(name, lo.isEmpty() ? null : Version.of(lo), hi.isEmpty() ? null : Version.of(hi),
                lowerOpen && !lo.isEmpty(), upperOpen && !hi.isEmpty());
    }

    /** whether {@code v} meets this dependency; sameMinor applies the implicit maximum */
    boolean accepts(Version v, boolean sameMinor) {
        if (atLeast != null) {
            int c = v.compareTo(atLeast);
            if (c < 0 || (c == 0 && lowerOpen)) {
                return false;
            }
        }
        if (atMost != null) {
            int c = v.compareTo(atMost);
            return c < 0 || (c == 0 && !upperOpen);
        }
        return !(sameMinor && atLeast != null && (v.major() != atLeast.major() || v.minor() != atLeast.minor()));
    }

    /** e.g. "BEAST.base 2.9.0 or a later 2.9.x", "BEAST.base [2.8.0-rc3,2.9.0-alpha)" */
    String describe(boolean sameMinor) {
        StringBuilder b = new StringBuilder(name);
        if (lowerOpen || upperOpen || (atLeast == null && atMost != null)) {
            b.append(' ').append(lowerOpen || atLeast == null ? '(' : '[')
                    .append(atLeast == null ? "" : atLeast).append(',').append(atMost == null ? "" : atMost)
                    .append(upperOpen || atMost == null ? ')' : ']');
        } else if (atLeast != null && atMost != null) {
            b.append(' ').append(atLeast.equals(atMost) ? atLeast.toString() : atLeast + " to " + atMost);
        } else if (atLeast != null) {
            b.append(' ').append(atLeast)
                    .append(sameMinor ? " or a later " + atLeast.major() + "." + atLeast.minor() + ".x" : " or later");
        }
        return b.toString();
    }
}
