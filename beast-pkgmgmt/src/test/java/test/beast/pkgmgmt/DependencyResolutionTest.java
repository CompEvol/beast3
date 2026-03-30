package test.beast.pkgmgmt;

import beast.pkgmgmt.Package;
import beast.pkgmgmt.PackageDependency;
import beast.pkgmgmt.DependencyResolver.DependencyResolutionException;
import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.PackageVersion;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PackageManager#populatePackagesToInstall}.
 * Constructs Package/PackageDependency objects directly — no network access.
 */
class DependencyResolutionTest {

    private static final URL DUMMY_URL;

    static {
        try {
            DUMMY_URL = new URL("https://example.com/pkg.zip");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Helper: create a package with one available version and given dependencies. */
    private Package makePackage(String name, String version, PackageDependency... deps) {
        Package pkg = new Package(name);
        pkg.setProjectURL(DUMMY_URL);
        Set<PackageDependency> depSet = new HashSet<>(Arrays.asList(deps));
        pkg.addAvailableVersion(new PackageVersion(version), DUMMY_URL, depSet);
        return pkg;
    }

    /** Helper: create a package with multiple available versions. */
    private Package makePackage(String name, String[][] versionsAndDeps) {
        Package pkg = new Package(name);
        pkg.setProjectURL(DUMMY_URL);
        for (String[] entry : versionsAndDeps) {
            String version = entry[0];
            Set<PackageDependency> deps = new HashSet<>();
            for (int i = 1; i < entry.length; i += 3) {
                // each dep: name, atLeast, atMost (empty string = null)
                String depName = entry[i];
                PackageVersion atLeast = entry[i + 1].isEmpty() ? null : new PackageVersion(entry[i + 1]);
                PackageVersion atMost = entry[i + 2].isEmpty() ? null : new PackageVersion(entry[i + 2]);
                deps.add(new PackageDependency(depName, atLeast, atMost));
            }
            pkg.addAvailableVersion(new PackageVersion(version), DUMMY_URL, deps);
        }
        return pkg;
    }

    /** Mark a package as installed at the given version (with no installed deps). */
    private void markInstalled(Package pkg, String version) {
        pkg.setInstalled(new PackageVersion(version), new HashSet<>());
    }

    @Test
    void simpleDependency() throws DependencyResolutionException {
        // A 1.0 depends on B >= 1.0
        Package pkgB = makePackage("B", "1.0");
        Package pkgA = makePackage("A", "1.0",
                new PackageDependency("B", new PackageVersion("1.0"), null));

        Map<String, Package> packageMap = new TreeMap<>();
        packageMap.put("A", pkgA);
        packageMap.put("B", pkgB);

        Map<Package, PackageVersion> toInstall = new HashMap<>();
        toInstall.put(pkgA, new PackageVersion("1.0"));

        PackageManager.populatePackagesToInstall(packageMap, toInstall);

        // B should be pulled in
        assertTrue(toInstall.containsKey(pkgB));
        assertEquals("1.0", toInstall.get(pkgB).getVersionString());
    }

    @Test
    void transitiveDependency() throws DependencyResolutionException {
        // A -> B -> C
        Package pkgC = makePackage("C", "1.0");
        Package pkgB = makePackage("B", "1.0",
                new PackageDependency("C", new PackageVersion("1.0"), null));
        Package pkgA = makePackage("A", "1.0",
                new PackageDependency("B", new PackageVersion("1.0"), null));

        Map<String, Package> packageMap = new TreeMap<>();
        packageMap.put("A", pkgA);
        packageMap.put("B", pkgB);
        packageMap.put("C", pkgC);

        Map<Package, PackageVersion> toInstall = new HashMap<>();
        toInstall.put(pkgA, new PackageVersion("1.0"));

        PackageManager.populatePackagesToInstall(packageMap, toInstall);

        assertTrue(toInstall.containsKey(pkgB));
        assertTrue(toInstall.containsKey(pkgC));
    }

    @Test
    void installedDependencySatisfied() throws DependencyResolutionException {
        // A depends on B >= 1.0. B 1.0 is already installed. Should not re-add B.
        Package pkgB = makePackage("B", "1.0");
        markInstalled(pkgB, "1.0");

        Package pkgA = makePackage("A", "1.0",
                new PackageDependency("B", new PackageVersion("1.0"), null));

        Map<String, Package> packageMap = new TreeMap<>();
        packageMap.put("A", pkgA);
        packageMap.put("B", pkgB);

        Map<Package, PackageVersion> toInstall = new HashMap<>();
        toInstall.put(pkgA, new PackageVersion("1.0"));

        PackageManager.populatePackagesToInstall(packageMap, toInstall);

        // B should NOT be added since it's already installed and satisfies the constraint
        assertFalse(toInstall.containsKey(pkgB));
    }

    @Test
    void conflictingVersionRequirements() {
        // A needs B >= 2.0, C needs B <= 1.0 — impossible
        Package pkgB = makePackage("B", new String[][]{
                {"1.0"},
                {"2.0"}
        });
        Package pkgA = makePackage("A", "1.0",
                new PackageDependency("B", new PackageVersion("2.0"), null));
        Package pkgC = makePackage("C", "1.0",
                new PackageDependency("B", null, new PackageVersion("1.0")));

        Map<String, Package> packageMap = new TreeMap<>();
        packageMap.put("A", pkgA);
        packageMap.put("B", pkgB);
        packageMap.put("C", pkgC);

        Map<Package, PackageVersion> toInstall = new HashMap<>();
        toInstall.put(pkgA, new PackageVersion("1.0"));
        toInstall.put(pkgC, new PackageVersion("1.0"));

        assertThrows(DependencyResolutionException.class,
                () -> PackageManager.populatePackagesToInstall(packageMap, toInstall));
    }

    @Test
    void unknownDependencyThrows() {
        // A depends on "NonExistent"
        Package pkgA = makePackage("A", "1.0",
                new PackageDependency("NonExistent", new PackageVersion("1.0"), null));

        Map<String, Package> packageMap = new TreeMap<>();
        packageMap.put("A", pkgA);

        Map<Package, PackageVersion> toInstall = new HashMap<>();
        toInstall.put(pkgA, new PackageVersion("1.0"));

        DependencyResolutionException ex = assertThrows(DependencyResolutionException.class,
                () -> PackageManager.populatePackagesToInstall(packageMap, toInstall));
        assertTrue(ex.getMessage().contains("NonExistent"));
    }

    @Test
    void mutualDependencyResolvesWhenVersionsSatisfied() throws DependencyResolutionException {
        // A -> B -> A: not a problem when A is already scheduled and satisfies B's constraint
        Package pkgA = makePackage("A", "1.0",
                new PackageDependency("B", new PackageVersion("1.0"), null));
        Package pkgB = makePackage("B", "1.0",
                new PackageDependency("A", new PackageVersion("1.0"), null));

        Map<String, Package> packageMap = new TreeMap<>();
        packageMap.put("A", pkgA);
        packageMap.put("B", pkgB);

        Map<Package, PackageVersion> toInstall = new HashMap<>();
        toInstall.put(pkgA, new PackageVersion("1.0"));

        // Should resolve: A is already in toInstall, so B's dep on A is satisfied
        PackageManager.populatePackagesToInstall(packageMap, toInstall);
        assertTrue(toInstall.containsKey(pkgB));
    }

    @Test
    void noDowngradeOfInstalledPackage() {
        // A needs B >= 1.0. B 2.0 is installed, B 1.0 is available.
        // Resolution should not downgrade B to 1.0.
        Package pkgB = makePackage("B", new String[][]{
                {"1.0"},
                {"2.0"}
        });
        markInstalled(pkgB, "2.0");

        // A requires B in range [1.0, 1.5] — this excludes installed 2.0
        // and would require downgrade to 1.0 — should fail
        Package pkgA = makePackage("A", "1.0",
                new PackageDependency("B", new PackageVersion("1.0"), new PackageVersion("1.5")));

        Map<String, Package> packageMap = new TreeMap<>();
        packageMap.put("A", pkgA);
        packageMap.put("B", pkgB);

        Map<Package, PackageVersion> toInstall = new HashMap<>();
        toInstall.put(pkgA, new PackageVersion("1.0"));

        assertThrows(DependencyResolutionException.class,
                () -> PackageManager.populatePackagesToInstall(packageMap, toInstall));
    }
}
