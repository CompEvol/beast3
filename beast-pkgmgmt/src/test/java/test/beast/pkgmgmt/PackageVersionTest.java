package test.beast.pkgmgmt;

import beast.pkgmgmt.PackageVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PackageVersion} comparison, equals, and hashCode.
 */
class PackageVersionTest {

    // ---- compareTo ----

    @Test
    void equalVersions() {
        assertEquals(0, new PackageVersion("1.0.0").compareTo(new PackageVersion("1.0.0")));
    }

    @Test
    void greaterMinorVersion() {
        assertTrue(new PackageVersion("2.1").compareTo(new PackageVersion("2.0")) > 0);
    }

    @Test
    void lesserMajorVersion() {
        assertTrue(new PackageVersion("1.9").compareTo(new PackageVersion("2.0")) < 0);
    }

    @Test
    void patchVersionGreater() {
        assertTrue(new PackageVersion("2.0.1").compareTo(new PackageVersion("2.0")) > 0);
    }

    @Test
    void numericNotLexicographic() {
        // "1.10" > "1.9" numerically, but "<" lexicographically
        assertTrue(new PackageVersion("1.10").compareTo(new PackageVersion("1.9")) > 0);
    }

    @Test
    void trailingZerosEquivalent() {
        // "2.0" and "2" should be equivalent by compareTo
        assertEquals(0, new PackageVersion("2.0").compareTo(new PackageVersion("2")));
        assertEquals(0, new PackageVersion("1.0.0").compareTo(new PackageVersion("1")));
    }

    @Test
    void prereleaseIsLessThanRelease() {
        // "1.0-alpha" < "1.0"
        assertTrue(new PackageVersion("1.0-alpha").compareTo(new PackageVersion("1.0")) < 0);
    }

    @Test
    void prereleaseOrdering() {
        // "1.0-alpha" < "1.0-beta" (lexicographic on suffix)
        assertTrue(new PackageVersion("1.0-alpha").compareTo(new PackageVersion("1.0-beta")) < 0);
    }

    // ---- equals / hashCode consistency with compareTo ----

    @Test
    void equalsConsistentWithCompareToForIdenticalStrings() {
        PackageVersion a = new PackageVersion("1.2.3");
        PackageVersion b = new PackageVersion("1.2.3");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equalsConsistentWithCompareToForTrailingZeros() {
        PackageVersion a = new PackageVersion("2.0");
        PackageVersion b = new PackageVersion("2");
        assertEquals(0, a.compareTo(b), "compareTo should treat them as equal");
        assertEquals(a, b, "equals should be consistent with compareTo");
        assertEquals(a.hashCode(), b.hashCode(), "hashCode should be consistent with equals");
    }

    @Test
    void equalsConsistentWithCompareToMultipleTrailingZeros() {
        PackageVersion a = new PackageVersion("1.0.0");
        PackageVersion b = new PackageVersion("1");
        assertEquals(0, a.compareTo(b));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqualToDifferentVersion() {
        assertNotEquals(new PackageVersion("1.0"), new PackageVersion("2.0"));
    }

    @Test
    void notEqualToNull() {
        assertNotEquals(null, new PackageVersion("1.0"));
    }

    @Test
    void notEqualToDifferentType() {
        assertNotEquals("1.0", new PackageVersion("1.0"));
    }
}
