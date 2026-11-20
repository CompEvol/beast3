package beast.pkgmgmt.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.junit.jupiter.api.Test;

/**
 * {@link Version} orders versions exactly as Maven's {@link GenericVersionScheme} does, without
 * needing the resolver at run time.
 */
class VersionTest {

    private static final List<String> VERSIONS = List.of(
            "0", "1", "1.0", "1.0.0", "1.0.0.0", "1-0", "1_0", "01.0",
            "1.0-alpha", "1.0-alpha1", "1.0a1", "1.0-a1", "1.0-beta", "1.0-b2", "1.0b2",
            "1.0-milestone", "1.0-m3", "1.0-cr1", "1.0-rc1", "1.0-RC1", "1.0-rc9", "1.0-rc10",
            "1.0-SNAPSHOT", "1.0-ga", "1.0-final", "1.0-release", "1.0-sp1", "1.0-foo", "1.0-bar",
            "1.0-1", "1.0.1", "1.0.1-SNAPSHOT", "1.1", "1.10", "1.9",
            "2.7.9", "2.8", "2.8.0-beta7", "2.8.0-rc1", "2.8.0-rc3", "2.8.0-SNAPSHOT", "2.8.0", "2.8.1",
            "2.9", "2.10", "10.0", "1.2.3.4.5", "20261119", "123456789012345678901234567890");

    @Test
    void ordersAsMavenDoes() throws InvalidVersionSpecificationException {
        GenericVersionScheme maven = new GenericVersionScheme();
        List<String> differences = new ArrayList<>();
        for (String a : VERSIONS) {
            for (String b : VERSIONS) {
                org.eclipse.aether.version.Version ma = maven.parseVersion(a), mb = maven.parseVersion(b);
                int expected = Integer.signum(ma.compareTo(mb));
                int actual = Integer.signum(Version.of(a).compareTo(Version.of(b)));
                if (expected != actual) {
                    differences.add(a + " vs " + b + ": Maven " + expected + ", Version " + actual);
                }
            }
        }
        assertTrue(differences.isEmpty(), differences.size() + " differences:\n" + String.join("\n", differences));
    }

    /** the pairs #309 found ordered wrongly by PackageVersion */
    @Test
    void pairsFromIssue309() {
        assertTrue(Version.of("1.0-rc9").compareTo(Version.of("1.0-rc10")) < 0);
        assertEquals(Version.of("1.0-RC1"), Version.of("1.0-rc1"));
        assertTrue(Version.of("1.0-SNAPSHOT").compareTo(Version.of("1.0-rc1")) > 0);
        assertTrue(Version.of("2.8.0-rc1").compareTo(Version.of("2.8.0")) < 0);
        assertEquals(Version.of("1.0"), Version.of("1.0.0"));
    }

    @Test
    void equalVersionsHaveEqualHashCodes() {
        assertEquals(Version.of("1").hashCode(), Version.of("1.0.0").hashCode());
        assertEquals(Version.of("1.0-ga").hashCode(), Version.of("1.0").hashCode());
        assertEquals(Version.of("1.0-RC1").hashCode(), Version.of("1.0-rc1").hashCode());
    }

    @Test
    void major() {
        assertEquals(2, Version.of("2.8.0-rc1").major());
        assertEquals(3, Version.of("3").major());
        assertEquals(0, Version.of("rc1").major());
        assertEquals(8, Version.of("2.8.0-rc1").minor());
        assertEquals(0, Version.of("3").minor());
    }
}
