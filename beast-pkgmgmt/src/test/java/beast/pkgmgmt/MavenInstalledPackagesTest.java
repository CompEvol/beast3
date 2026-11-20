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
 * #316: a package installed from Maven has no package folder, so the folder scan never found it,
 * and the Package Manager, packagemanager -list, beast -version and the start-up check treated it
 * as not installed. These tests cover:
 * <ul>
 * <li>which copy counts as installed when there are ZIP and Maven copies (as in the planner: the
 * newest, and a ZIP copy wins a tie);</li>
 * <li>uninstalling a Maven copy: its line in maven-packages.xml is removed, unless it came in as a
 * dependency of another Maven package;</li>
 * <li>installing and upgrading when a Maven copy is installed.</li>
 * </ul>
 * The user package folder is a temporary folder, so the real one is never touched.
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
        return maven(name, g, a, v, Dependency.atLeast("BEAST.base", "2.8.0"));
    }

    /** a copy of {@code name} installed from Maven as {@code g:a:v}, with the dependencies its POM gives */
    private InstalledPackage maven(String name, String g, String a, String v, Dependency... depends) {
        Path jar = repo().resolve(g.replace('.', '/')).resolve(a).resolve(v).resolve(a + "-" + v + ".jar");
        return new InstalledPackage(name, Version.of(v), Location.MAVEN, List.of(depends),
                List.of(new Artifact(a, Version.of(v), jar, Artifact.Kind.OWN, Set.of(a), Set.of())));
    }

    /** a ZIP copy already found by the folder scan */
    private static Package zip(Map<String, Package> packageMap, String name, String version) {
        Package pkg = new Package(name);
        pkg.setInstalled(new PackageVersion(version), new TreeSet<>());
        packageMap.put(name, pkg);
        return pkg;
    }

    /** the case in #316: CCD installed from Maven only is installed, shown as "1.2.2 (Maven)", with its POM dependencies */
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

    /** ZIP and Maven copies of the same version: the ZIP copy counts, as in the planner */
    @Test
    void zipCopyWinsATie() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.2");
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertNull(packageMap.get("CCD").getMavenCoordinate());
        assertEquals("1.2.2", packageMap.get("CCD").getInstalledLabel());
    }

    /** ZIP 1.2.1 and Maven 1.2.2: the newer Maven copy counts */
    @Test
    void newerMavenCopyReplacesOlderZipCopy() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.1");
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertEquals("1.2.2 (Maven)", packageMap.get("CCD").getInstalledLabel());
    }

    /** ZIP 1.2.3 and Maven 1.2.2: the newer ZIP copy still counts */
    @Test
    void olderMavenCopyDoesNotReplaceNewerZipCopy() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.3");
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertEquals("1.2.3", packageMap.get("CCD").getInstalledLabel());
    }

    /** only Maven copies are added here; ZIP packages in folders are found by the existing folder scan */
    @Test
    void otherLocationsAreLeftToTheFolderScan() {
        Map<String, Package> packageMap = new HashMap<>();
        InstalledPackage user = new InstalledPackage("SA", Version.of("2.3.0"), Location.USER, List.of(), List.of());
        PackageManager.addMavenPackages(packageMap, List.of(user), repo());

        assertFalse(packageMap.containsKey("SA"));
    }

    /** no maven-packages.xml in the user folder: no Maven packages, and the Maven resolver is not run */
    @Test
    void noMavenPackagesWithoutMavenConfig() {
        assertTrue(PackageManager.getMavenPackages().isEmpty());
    }

    /**
     * uninstalling CCD installed from Maven (GUI Uninstall, packagemanager -del CCD) removes only
     * its line from maven-packages.xml; there is no folder to delete
     */
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

    /**
     * Upgrading a package that came in as a Maven dependency (MODEL_SELECTION via NS) to a newer
     * ZIP version must not fail: the Maven copy stays for NS, and the newer ZIP copy is installed
     * next to it, which the planner then loads (newest wins).
     */
    @Test
    void upgradingAMavenDependencyKeepsItAndInstallsTheZip() throws IOException {
        PackageManager.saveMavenPackageConfig(List.of(
                new PackageManager.MavenCoordinate("io.github.beast2-dev", "nested-sampling", "1.3.1")));
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap,
                List.of(maven("MODEL_SELECTION", "io.github.beast2-dev", "model-selection", "1.7.0")), repo());
        Package ms = packageMap.get("MODEL_SELECTION");
        Map<Package, PackageVersion> packagesToInstall = new HashMap<>(Map.of(ms, new PackageVersion("1.7.1")));

        PackageManager.prepareForInstall(packagesToInstall, false, null);

        assertEquals(new PackageVersion("1.7.1"), packagesToInstall.get(ms));
        assertEquals(1, PackageManager.loadMavenPackageConfig().size());
    }

    /** upgrading a listed Maven package to a newer ZIP version replaces it: its line is removed */
    @Test
    void upgradingAListedMavenPackageRemovesItsLine() throws IOException {
        PackageManager.saveMavenPackageConfig(List.of(
                new PackageManager.MavenCoordinate("io.github.compevol", "ccd", "1.2.2")));
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());
        Package ccd = packageMap.get("CCD");
        Map<Package, PackageVersion> packagesToInstall = new HashMap<>(Map.of(ccd, new PackageVersion("1.2.3")));

        PackageManager.prepareForInstall(packagesToInstall, false, null);

        assertEquals(new PackageVersion("1.2.3"), packagesToInstall.get(ccd));
        assertTrue(PackageManager.loadMavenPackageConfig().isEmpty());
    }

    /** asking for the version already installed from Maven installs nothing */
    @Test
    void theInstalledMavenVersionIsNotInstalledAgain() throws IOException {
        PackageManager.saveMavenPackageConfig(List.of(
                new PackageManager.MavenCoordinate("io.github.compevol", "ccd", "1.2.2")));
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());
        Map<Package, PackageVersion> packagesToInstall = new HashMap<>(Map.of(packageMap.get("CCD"), new PackageVersion("1.2.2")));

        PackageManager.prepareForInstall(packagesToInstall, false, null);

        assertTrue(packagesToInstall.isEmpty());
        assertEquals(1, PackageManager.loadMavenPackageConfig().size());
    }

    /** -del CCD -version 1.2.1 removes that archive copy, not the Maven copy in use */
    @Test
    void deletingAnotherVersionKeepsTheMavenLine() throws IOException {
        PackageManager.saveMavenPackageConfig(List.of(
                new PackageManager.MavenCoordinate("io.github.compevol", "ccd", "1.2.2")));
        Path archived = Files.createDirectories(userDir.resolve("archive/CCD/1.2.1"));
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        PackageInstaller.useArchive(true);
        try {
            PackageManager.uninstallPackage(packageMap.get("CCD"), new PackageVersion("1.2.1"), false, null);
        } finally {
            PackageInstaller.useArchive(false);
        }

        assertFalse(Files.exists(archived));
        assertEquals(1, PackageManager.loadMavenPackageConfig().size());
    }

    /** the existing check before an uninstall sees Maven dependencies: NS needs MODEL_SELECTION */
    @Test
    void aMavenPackageNeededByAnotherCannotBeRemoved() {
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, List.of(
                maven("NS", "io.github.beast2-dev", "nested-sampling", "1.3.1",
                        Dependency.atLeast("BEAST.base", "2.8.0"), Dependency.atLeast("MODEL_SELECTION", "1.7.0")),
                maven("MODEL_SELECTION", "io.github.beast2-dev", "model-selection", "1.7.0")), repo());

        assertEquals(List.of("NS"), PackageManager.getInstalledDependencyNames(packageMap.get("MODEL_SELECTION"), packageMap));
    }

    /** installing a ZIP package that needs CCD uses the CCD installed from Maven, and does not install it again */
    @Test
    void aZipNeedingCcdUsesTheMavenCcd() throws Exception {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "BEAST.base", "2.8.0");
        PackageManager.addMavenPackages(packageMap, List.of(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());
        packageMap.get("CCD").addAvailableVersion(new PackageVersion("1.2.2"),
                java.net.URI.create("https://example.org/CCD.zip").toURL(), new TreeSet<>());
        Package x = new Package("X");
        Set<PackageDependency> needsCcd = new TreeSet<>(java.util.Comparator.comparing(d -> d.dependencyName));
        needsCcd.add(new PackageDependency("CCD", new PackageVersion("1.2.0"), null));
        x.addAvailableVersion(new PackageVersion("1.0.0"), java.net.URI.create("https://example.org/X.zip").toURL(), needsCcd);
        packageMap.put("X", x);
        Map<Package, PackageVersion> packagesToInstall = new HashMap<>(Map.of(x, new PackageVersion("1.0.0")));

        PackageManager.populatePackagesToInstall(packageMap, packagesToInstall);

        assertEquals(Map.of(x, new PackageVersion("1.0.0")), packagesToInstall);
    }

    /**
     * MODEL_SELECTION came in as a Maven dependency of NS and is not in maven-packages.xml:
     * uninstalling it on its own is refused, and nothing is changed
     */
    @Test
    void aMavenDependencyCannotBeUninstalledOnItsOwn() throws IOException {
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
