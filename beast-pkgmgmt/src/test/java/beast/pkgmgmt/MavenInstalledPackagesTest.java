package beast.pkgmgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import beast.pkgmgmt.plan.Artifact;
import beast.pkgmgmt.plan.Dependency;
import beast.pkgmgmt.plan.InstalledPackage;
import beast.pkgmgmt.plan.InstalledPackage.Location;
import beast.pkgmgmt.plan.Version;

/**
 * #316: a package installed from Maven has no package folder, so it must be added to the
 * package database from the planner's inventory, and uninstalling it must remove its line from
 * maven-packages.xml.
 */
public class MavenInstalledPackagesTest {

    @TempDir
    Path userDir;

    private String savedUserDir;

    @BeforeEach
    void useTempUserDir() {
        savedUserDir = System.getProperty("beast.user.package.dir");
        System.setProperty("beast.user.package.dir", userDir.toString());
    }

    @AfterEach
    void restoreUserDir() {
        if (savedUserDir == null) {
            System.clearProperty("beast.user.package.dir");
        } else {
            System.setProperty("beast.user.package.dir", savedUserDir);
        }
    }

    private Path repo() {
        return userDir.resolve("maven-repo");
    }

    /** a copy of {@code name} installed from Maven as {@code g:a:v}, needing BEAST.base 2.8.0 */
    private InstalledPackage maven(String name, String g, String a, String v) {
        Path jar = repo().resolve(g.replace('.', '/')).resolve(a).resolve(v).resolve(a + "-" + v + ".jar");
        return new InstalledPackage(name, Version.of(v), Location.MAVEN,
                List.of(Dependency.atLeast("BEAST.base", "2.8.0")),
                List.of(new Artifact(a, Version.of(v), jar, Artifact.Kind.OWN, Set.of(a), Set.of())));
    }

    /** a ZIP copy already found by the folder scan */
    private static Package zip(Map<String, Package> packageMap, String name, String version) {
        Package pkg = new Package(name);
        pkg.setInstalled(new PackageVersion(version), new TreeSet<>());
        packageMap.put(name, pkg);
        return pkg;
    }

    @Test
    void mavenOnlyPackageIsInstalled() {
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        Package ccd = packageMap.get("CCD");
        assertTrue(ccd.isInstalled());
        assertEquals("1.2.2", ccd.getInstalledVersion().toString());
        assertEquals("io.github.compevol:ccd:1.2.2", ccd.getMavenCoordinate());
        assertEquals("1.2.2 (Maven)", ccd.getInstalledLabel());
        assertEquals("BEAST.base", ccd.getInstalledVersionDependencies().iterator().next().dependencyName);
    }

    @Test
    void zipCopyWinsATie() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.2");
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertNull(packageMap.get("CCD").getMavenCoordinate());
        assertEquals("1.2.2", packageMap.get("CCD").getInstalledLabel());
    }

    @Test
    void newerMavenCopyReplacesOlderZipCopy() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.1");
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertEquals("1.2.2 (Maven)", packageMap.get("CCD").getInstalledLabel());
    }

    @Test
    void olderMavenCopyDoesNotReplaceNewerZipCopy() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.3");
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertEquals("1.2.3", packageMap.get("CCD").getInstalledLabel());
    }

    @Test
    void otherLocationsAreLeftToTheFolderScan() {
        Map<String, Package> packageMap = new HashMap<>();
        InstalledPackage user = new InstalledPackage("SA", Version.of("2.3.0"), Location.USER, List.of(), List.of());
        PackageManager.addMavenPackages(packageMap, List.of(user), repo());

        assertFalse(packageMap.containsKey("SA"));
    }

    @Test
    void noMavenPackagesWithoutMavenConfig() {
        // no maven-packages.xml in the user folder: nothing listed, and the resolver is not run
        assertTrue(PackageManager.getMavenPackages().isEmpty());
    }

    @Test
    void uninstallRemovesTheMavenLine() throws IOException {
        PackageManager.saveMavenPackageConfig(List.of(
                new PackageManager.MavenCoordinate("io.github.compevol", "morph-models", "1.3.1"),
                new PackageManager.MavenCoordinate("io.github.compevol", "ccd", "1.2.2")));
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        String from = PackageManager.uninstallPackage(packageMap.get("CCD"), false, null);

        assertTrue(from.endsWith("maven-packages.xml"));
        List<PackageManager.MavenCoordinate> left = PackageManager.loadMavenPackageConfig();
        assertEquals(1, left.size());
        assertEquals("morph-models", left.get(0).artifactId);
    }

    @Test
    void aMavenDependencyCannotBeUninstalledOnItsOwn() throws IOException {
        // NS is listed; MODEL_SELECTION came with it as a dependency
        PackageManager.saveMavenPackageConfig(List.of(
                new PackageManager.MavenCoordinate("io.github.beast2-dev", "nested-sampling", "1.3.1")));
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap,
                List.of(maven("MODEL_SELECTION", "io.github.beast2-dev", "model-selection", "1.7.0")), repo());

        assertThrows(IOException.class, () -> PackageManager.uninstallPackage(packageMap.get("MODEL_SELECTION"), false, null));
        assertEquals(1, PackageManager.loadMavenPackageConfig().size());
        assertFalse(Files.exists(userDir.resolve("MODEL_SELECTION")));
    }
}
