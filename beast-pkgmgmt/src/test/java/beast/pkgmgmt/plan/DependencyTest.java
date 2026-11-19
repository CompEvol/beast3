package beast.pkgmgmt.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;

/** Versions a package needs: version.xml bounds and POM versions and ranges, as Maven reads them. */
class DependencyTest {

    private static boolean ok(Dependency d, String v) {
        return d.accepts(Version.of(v), false);
    }

    /** #298's recommended range, and Walter's table of what is in it */
    @Test
    void recommendedRange() {
        Dependency d = Dependency.fromMaven("BEAST.base", "[2.8.0-rc3,2.9.0-alpha)");
        for (String in : List.of("2.8.0-rc3", "2.8.0-rc10", "2.8.0", "2.8.1")) {
            assertTrue(ok(d, in), in);
        }
        for (String out : List.of("2.8.0-rc1", "2.9.0-alpha", "2.9.0-alpha1", "2.9.0-SNAPSHOT", "2.9.0-rc1", "2.9.0")) {
            assertFalse(ok(d, out), out);
        }
        assertEquals("BEAST.base [2.8.0-rc3,2.9.0-alpha)", d.describe(true));
    }

    /** the same answers as Maven's own range check, on every range and version here */
    @Test
    void rangesAsMavenReadsThem() throws Exception {
        GenericVersionScheme maven = new GenericVersionScheme();
        List<String> ranges = List.of("[1.0,2.0)", "(1.0,2.0]", "[1.0,)", "(,2.0]", "(,2.0)", "[1.5]", "(1.0,1.5)");
        List<String> versions = List.of("0.9", "1.0", "1.0.1", "1.5", "1.9", "2.0-alpha", "2.0", "2.1");
        for (String r : ranges) {
            org.eclipse.aether.version.VersionConstraint c = maven.parseVersionConstraint(r);
            for (String v : versions) {
                assertEquals(c.containsVersion(maven.parseVersion(v)), ok(Dependency.fromMaven("X", r), v), r + " " + v);
            }
        }
    }

    /** a plain POM version is a minimum, as Maven treats it; for the core, within its minor version */
    @Test
    void plainVersionIsAMinimum() {
        Dependency d = Dependency.fromMaven("BEAST.base", "2.8.0-rc1");
        assertTrue(d.accepts(Version.of("2.8.0"), true));
        assertTrue(d.accepts(Version.of("2.8.5"), true));
        assertFalse(d.accepts(Version.of("2.9.0"), true), "core: same minor version only");
        assertTrue(d.accepts(Version.of("2.9.0"), false), "other packages: a minimum only");
        assertFalse(d.accepts(Version.of("2.7.9"), true));
    }

    @Test
    void unionsOfRangesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Dependency.fromMaven("X", "[1,2),[3,4)"));
    }
}
