package beast.pkgmgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import beast.pkgmgmt.plan.LoadPlan;
import beast.pkgmgmt.plan.Planner;
import beast.pkgmgmt.plan.Runtime;
import beast.pkgmgmt.plan.Version;

/**
 * #316: a package installed from Maven has no package folder, so the folder scan never found it,
 * and the Package Manager, packagemanager -list, beast -version and the start-up check treated it
 * as not installed. #317: which copy counts as installed, here and in beast -version, is the one
 * the planner loads. These tests cover:
 * <ul>
 * <li>which copy counts as installed when there are ZIP and Maven copies, and what beast -version
 * says about the other copy;</li>
 * <li>uninstalling a Maven copy: its line in maven-packages.xml is removed, unless it came in as a
 * dependency of another Maven package;</li>
 * <li>installing and upgrading when a Maven copy is installed.</li>
 * </ul>
 * The user package folder is a temporary folder, so the real one is never touched.
 */
public class MavenInstalledPackagesTest {

    /** a release: only the launcher on the boot layer, JavaFX in the JRE */
    private static final Runtime RELEASE = new Runtime(Version.of("2.8.0"), Version.of("25.0.3"),
            List.of(new Runtime.BootModule("beast.pkgmgmt", Set.of("beast.pkgmgmt"))));

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

    /** the planner's answer for the bundled BEAST.base 2.8.0 and {@code copies} */
    private static LoadPlan plan(InstalledPackage... copies) {
        List<InstalledPackage> installed = new ArrayList<>(List.of(core(Location.BUNDLED)));
        installed.addAll(List.of(copies));
        return Planner.plan(installed, RELEASE);
    }

    /** BEAST.base 2.8.0 installed at {@code location} */
    private static InstalledPackage core(Location location) {
        return new InstalledPackage(Planner.BASE, Version.of("2.8.0"), location,
                List.of(Dependency.atLeast(Planner.LAUNCHER, "2.8.0")),
                List.of(new Artifact("beast.base", Version.of("2.8.0"), Path.of(location.name(), "beast.base.jar"),
                        Artifact.Kind.OWN, Set.of("beast.base.core"), Set.of())));
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

    /** a ZIP copy of {@code name} in a package folder at {@code location}, needing BEAST.base 2.8.0 */
    private static InstalledPackage folder(String name, String v, Location location) {
        String module = name.toLowerCase();
        return new InstalledPackage(name, Version.of(v), location, List.of(Dependency.atLeast("BEAST.base", "2.8.0")),
                List.of(new Artifact(module, Version.of(v), Path.of(location.name(), name, module + ".jar"),
                        Artifact.Kind.OWN, Set.of(module), Set.of())));
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
        PackageManager.addMavenPackages(packageMap, plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        Package ccd = packageMap.get("CCD");
        assertTrue(ccd.isInstalled());
        assertEquals("1.2.2", ccd.getInstalledVersion().toString());
        assertEquals("io.github.compevol:ccd:1.2.2", ccd.getMavenCoordinate());
        assertEquals("1.2.2 (Maven)", ccd.getInstalledLabel());
        assertEquals("BEAST.base", ccd.getInstalledVersionDependencies().iterator().next().dependencyName);
    }

    /** ZIP in the user folder and Maven copies of the same version: the planner loads the ZIP copy */
    @Test
    void zipCopyWinsATie() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.2");
        PackageManager.addMavenPackages(packageMap, plan(folder("CCD", "1.2.2", Location.USER),
                maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertNull(packageMap.get("CCD").getMavenCoordinate());
        assertEquals("1.2.2", packageMap.get("CCD").getInstalledLabel());
    }

    /**
     * ZIP in the system folder and Maven copies of the same version: the planner loads the Maven
     * copy, so it is the one installed. The rule copied from the planner said "a ZIP copy wins a
     * tie", and disagreed with it here (#317).
     */
    @Test
    void mavenCopyWinsATieWithTheSystemFolder() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.2");
        PackageManager.addMavenPackages(packageMap, plan(folder("CCD", "1.2.2", Location.SYSTEM),
                maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertEquals("1.2.2 (Maven)", packageMap.get("CCD").getInstalledLabel());
    }

    /** a copy on BEAST_PACKAGE_PATH is used whatever its version, so a newer Maven copy is not the installed one */
    @Test
    void overrideCopyWinsOverANewerMavenCopy() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.1");
        PackageManager.addMavenPackages(packageMap, plan(folder("CCD", "1.2.1", Location.OVERRIDE),
                maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertEquals("1.2.1", packageMap.get("CCD").getInstalledLabel());
    }

    /** ZIP 1.2.1 and Maven 1.2.2: the newer Maven copy counts */
    @Test
    void newerMavenCopyReplacesOlderZipCopy() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.1");
        PackageManager.addMavenPackages(packageMap, plan(folder("CCD", "1.2.1", Location.USER),
                maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertEquals("1.2.2 (Maven)", packageMap.get("CCD").getInstalledLabel());
    }

    /** ZIP 1.2.3 and Maven 1.2.2: the newer ZIP copy still counts */
    @Test
    void olderMavenCopyDoesNotReplaceNewerZipCopy() {
        Map<String, Package> packageMap = new HashMap<>();
        zip(packageMap, "CCD", "1.2.3");
        PackageManager.addMavenPackages(packageMap, plan(folder("CCD", "1.2.3", Location.USER),
                maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        assertEquals("1.2.3", packageMap.get("CCD").getInstalledLabel());
    }

    /** a Maven copy that cannot be loaded is still installed, so that it can be removed */
    @Test
    void anExcludedMavenCopyIsStillInstalled() {
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2",
                Dependency.atLeast("BEAST.base", "9.0.0"))), repo());

        assertEquals("1.2.2 (Maven)", packageMap.get("CCD").getInstalledLabel());
    }

    /** only Maven copies are added here; ZIP packages in folders are found by the existing folder scan */
    @Test
    void otherLocationsAreLeftToTheFolderScan() {
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, plan(folder("SA", "2.3.0", Location.USER)), repo());

        assertFalse(packageMap.containsKey("SA"));
    }

    /** #317, as alexeid found it: ZIP and Maven CCD 1.2.2; beast -version says which one is used */
    @Test
    void versionLinesMarkTheCopyThatIsNotUsed() {
        List<String> lines = PackageManager.versionLines(plan(folder("CCD", "1.2.2", Location.USER),
                maven("CCD", "io.github.compevol", "ccd", "1.2.2")));

        assertEquals(List.of("BEAST.base v2.8.0", "CCD v1.2.2 (user)", "CCD v1.2.2 (Maven) - not used"), lines);
    }

    /** #317, alexeid's nit: with several ZIP copies, each line says which copy it is */
    @Test
    void versionLinesSayWhichZipCopyIsLoaded() {
        List<String> lines = PackageManager.versionLines(plan(folder("CCD", "1.2.2", Location.SYSTEM),
                folder("CCD", "1.2.2", Location.OVERRIDE)));

        assertEquals(List.of("BEAST.base v2.8.0", "CCD v1.2.2 (BEAST_PACKAGE_PATH)", "CCD v1.2.2 (system) - not used"),
                lines);
    }

    /** a package with one copy also says where it is; BEAST itself does not */
    @Test
    void versionLinesSayWhereASingleCopyIs() {
        assertEquals(List.of("BEAST.base v2.8.0", "CCD v1.2.2 (user)"),
                PackageManager.versionLines(plan(folder("CCD", "1.2.2", Location.USER))));
        assertEquals(List.of("BEAST.base v2.8.0", "CCD v1.2.2 (system)"),
                PackageManager.versionLines(plan(folder("CCD", "1.2.2", Location.SYSTEM))));
        assertEquals(List.of("BEAST.base v2.8.0", "CCD v1.2.2 (BEAST_PACKAGE_PATH)"),
                PackageManager.versionLines(plan(folder("CCD", "1.2.2", Location.OVERRIDE))));
        assertEquals(List.of("BEAST.base v2.8.0", "CCD v1.2.2 (Maven)"),
                PackageManager.versionLines(plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2"))));
    }

    /** a newer copy that needs a newer BEAST keeps its reason, which the line above does not explain */
    @Test
    void versionLinesKeepTheReasonANewerCopyIsNotUsed() {
        List<String> lines = PackageManager.versionLines(plan(folder("CCD", "1.2.2", Location.USER),
                maven("CCD", "io.github.compevol", "ccd", "1.3.0", Dependency.atLeast("BEAST.base", "9.0.0"))));

        assertEquals(3, lines.size(), lines.toString());
        assertEquals("CCD v1.2.2 (user)", lines.get(1), lines.toString());
        assertTrue(lines.get(2).startsWith("CCD v1.3.0 (Maven) - not used: ")
                && lines.get(2).contains("BEAST.base"), lines.toString());
    }

    /** a newer Maven copy is used, and the ZIP copy is the one marked */
    @Test
    void versionLinesMarkAnOlderZipCopy() {
        List<String> lines = PackageManager.versionLines(plan(folder("CCD", "1.2.1", Location.USER),
                maven("CCD", "io.github.compevol", "ccd", "1.2.2")));

        assertEquals(List.of("BEAST.base v2.8.0", "CCD v1.2.2 (Maven)", "CCD v1.2.1 (user) - not used"), lines);
    }

    /** a package that cannot be loaded is listed with the reason, not as if it worked */
    @Test
    void versionLinesSayWhatIsNotLoaded() {
        List<String> lines = PackageManager.versionLines(plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2",
                Dependency.atLeast("BEAST.base", "9.0.0"))));

        assertEquals(2, lines.size(), lines.toString());
        assertTrue(lines.get(1).startsWith("CCD v1.2.2 (Maven) - not loaded: "), lines.toString());
    }

    /** a second copy of the core, e.g. in the user folder and the installation, is not listed */
    @Test
    void versionLinesListTheCoreOnce() {
        List<String> lines = PackageManager.versionLines(plan(core(Location.USER)));

        assertEquals(List.of("BEAST.base v2.8.0"), lines);
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
        PackageManager.addMavenPackages(packageMap, plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

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
                plan(maven("MODEL_SELECTION", "io.github.beast2-dev", "model-selection", "1.7.0")), repo());
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
        PackageManager.addMavenPackages(packageMap, plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());
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
        PackageManager.addMavenPackages(packageMap, plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());
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
        PackageManager.addMavenPackages(packageMap, plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        PackageInstaller.useArchive(true);
        try {
            PackageManager.uninstallPackage(packageMap.get("CCD"), new PackageVersion("1.2.1"), false, null);
        } finally {
            PackageInstaller.useArchive(false);
        }

        assertFalse(Files.exists(archived));
        assertEquals(1, PackageManager.loadMavenPackageConfig().size());
    }

    /**
     * -del CCD -version 1.2.2 with Maven 1.2.2 in use and no archive copy removes the Maven line,
     * as BEAST 2 removes the copy in use when there is no archive copy of the version
     */
    @Test
    void deletingTheMavenVersionInUseRemovesTheMavenLine() throws IOException {
        PackageManager.saveMavenPackageConfig(List.of(
                new PackageManager.MavenCoordinate("io.github.compevol", "morph-models", "1.3.1"),
                new PackageManager.MavenCoordinate("io.github.compevol", "ccd", "1.2.2")));
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        String from;
        PackageInstaller.useArchive(true);
        try {
            from = PackageManager.uninstallPackage(packageMap.get("CCD"), new PackageVersion("1.2.2"), false, null);
        } finally {
            PackageInstaller.useArchive(false);
        }

        assertTrue(from.endsWith("maven-packages.xml"), from);
        List<PackageManager.MavenCoordinate> left = PackageManager.loadMavenPackageConfig();
        assertEquals(1, left.size());
        assertEquals("morph-models", left.get(0).artifactId);
    }

    /**
     * -del CCD -version 1.2.1 with Maven 1.2.2 in use and no archive copy of 1.2.1 removes nothing:
     * only version 1.2.1 may be removed, never the copy in use of another version
     */
    @Test
    void deletingAVersionNotInstalledKeepsTheMavenLine() throws IOException {
        PackageManager.saveMavenPackageConfig(List.of(
                new PackageManager.MavenCoordinate("io.github.compevol", "ccd", "1.2.2")));
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());

        String from;
        PackageInstaller.useArchive(true);
        try {
            from = PackageManager.uninstallPackage(packageMap.get("CCD"), new PackageVersion("1.2.1"), false, null);
        } finally {
            PackageInstaller.useArchive(false);
        }

        assertNull(from);
        assertEquals(1, PackageManager.loadMavenPackageConfig().size());
    }

    /** the existing check before an uninstall sees Maven dependencies: NS needs MODEL_SELECTION */
    @Test
    void aMavenPackageNeededByAnotherCannotBeRemoved() {
        Map<String, Package> packageMap = new HashMap<>();
        PackageManager.addMavenPackages(packageMap, plan(
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
        PackageManager.addMavenPackages(packageMap, plan(maven("CCD", "io.github.compevol", "ccd", "1.2.2")), repo());
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
                plan(maven("MODEL_SELECTION", "io.github.beast2-dev", "model-selection", "1.7.0")), repo());

        assertThrows(IOException.class, () -> PackageManager.uninstallPackage(packageMap.get("MODEL_SELECTION"), false, null));
        assertEquals(1, PackageManager.loadMavenPackageConfig().size());
        assertFalse(Files.exists(userDir.resolve("MODEL_SELECTION")));
    }
}
