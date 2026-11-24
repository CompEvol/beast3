package beast.pkgmgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * #316 part 2: the CBAN package list can say that a version is also on Maven
 * ({@code groupId}/{@code artifactId} on a package entry). Then:
 * <ul>
 * <li>Latest is still the highest version CBAN lists, and is shown as "1.2.3 (Maven)" when it
 * would be installed from Maven;</li>
 * <li>an upgrade keeps the format of the copy in use: a package in use from Maven is upgraded from
 * Maven when CBAN has the new version there, otherwise from the ZIP, as before.</li>
 * </ul>
 * Nothing here uses the network: Maven installs go through a stub, and the user package folder
 * is a temporary folder.
 */
public class CbanMavenPackagesTest {

    @TempDir
    Path userDir;

    private String savedUserDir;
    private PackageInstaller.MavenInstall savedMavenInstall;
    /** the coordinates the stubbed Maven install was asked for */
    private final List<String> mavenInstalls = new ArrayList<>();

    @BeforeEach
    void setUp() {
        savedUserDir = System.getProperty("beast.user.package.dir");
        System.setProperty("beast.user.package.dir", userDir.toString());
        savedMavenInstall = PackageInstaller.mavenInstall;
        PackageInstaller.mavenInstall = (g, a, v) -> mavenInstalls.add(g + ":" + a + ":" + v);
    }

    @AfterEach
    void tearDown() {
        PackageInstaller.mavenInstall = savedMavenInstall;
        PackageInstaller.useArchive(false);
        if (savedUserDir == null) {
            System.clearProperty("beast.user.package.dir");
        } else {
            System.setProperty("beast.user.package.dir", savedUserDir);
        }
    }

    private static final String CCD_GA = "io.github.compevol:ccd";

    private static URL zipUrl(String name, String version) {
        try {
            // .invalid never resolves: a test that tries to download the ZIP fails
            return URI.create("https://example.invalid/" + name + ".v" + version + ".zip").toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Set<PackageDependency> needs(String... nameAtLeast) {
        Set<PackageDependency> deps = new HashSet<>();
        for (int i = 0; i < nameAtLeast.length; i += 2) {
            deps.add(new PackageDependency(nameAtLeast[i], new PackageVersion(nameAtLeast[i + 1]), null));
        }
        return deps;
    }

    /** CCD with 1.2.2 and 1.2.3 in CBAN; {@code mavenVersions} also have the Maven coordinate */
    private static Package ccd(String... mavenVersions) {
        Package ccd = new Package("CCD");
        for (String v : List.of("1.2.2", "1.2.3")) {
            boolean onMaven = List.of(mavenVersions).contains(v);
            ccd.addAvailableVersion(new PackageVersion(v), zipUrl("CCD", v), needs("BEAST.base", "2.8.0"),
                    onMaven ? CCD_GA : null);
        }
        return ccd;
    }

    /** marks {@code pkg} installed at {@code version}, from Maven if {@code fromMaven} */
    private static Package installed(Package pkg, String version, boolean fromMaven) {
        pkg.setInstalled(new PackageVersion(version), needs("BEAST.base", "2.8.0"));
        if (fromMaven) {
            pkg.setMavenCoordinate(CCD_GA + ":" + version);
        }
        return pkg;
    }

    private static Map<String, Package> load(String xml) throws Exception {
        Map<String, Package> packageMap = new HashMap<>();
        new PackageRepository().loadURL(URI.create("https://example.invalid/packages.xml").toURL(),
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), packageMap);
        return packageMap;
    }

    // ---- reading the package list ----

    /** groupId/artifactId on an entry give that version's Maven coordinate; other versions have none */
    @Test
    void theListGivesTheMavenCoordinateOfAVersion() throws Exception {
        Package ccd = load("""
                <packages>
                  <package name="CCD" version="1.2.2" url="https://example.invalid/CCD.v1.2.2.zip"
                           projectURL="https://github.com/CompEvol/CCD"/>
                  <package name="CCD" version="1.2.3" url="https://example.invalid/CCD.v1.2.3.zip"
                           projectURL="https://github.com/CompEvol/CCD"
                           groupId="io.github.compevol" artifactId="ccd"/>
                </packages>""").get("CCD");

        assertEquals("io.github.compevol:ccd:1.2.3", ccd.getAvailableMavenCoordinate(new PackageVersion("1.2.3")));
        assertNull(ccd.getAvailableMavenCoordinate(new PackageVersion("1.2.2")));
        assertEquals(zipUrl("CCD", "1.2.3"), ccd.getVersionURL(new PackageVersion("1.2.3")));
    }

    // ---- the shapes a CBAN list can have: ZIP only, Maven only, both, and mixed across versions ----

    private static final String ZIP_ATTRS = "url=\"https://example.invalid/CCD.v1.2.3.zip\"";
    private static final String MAVEN_ATTRS = "groupId=\"io.github.compevol\" artifactId=\"ccd\"";

    /** a CBAN entry for CCD {@code version} with {@code attrs} (a url, a Maven coordinate, or both) */
    private static String entry(String version, String attrs) {
        return "<package name=\"CCD\" version=\"" + version + "\" projectURL=\"https://github.com/CompEvol/CCD\" "
                + attrs + "></package>";
    }

    /**
     * Loads {@code entries} as a CBAN list and checks, for CCD not installed, ZIP 1.2.2 in use and
     * Maven 1.2.2 in use, whether its latest version installs from Maven and how Latest is shown.
     * {@code expected} gives the answer for each of the three, in that order.
     */
    private static void assertShape(String entries, boolean... expected) throws Exception {
        String[] states = {"not installed", "ZIP 1.2.2 in use", "Maven 1.2.2 in use"};
        for (int i = 0; i < states.length; i++) {
            Package ccd = load("<packages>" + entries + "</packages>").get("CCD");
            if (i > 0) {
                installed(ccd, "1.2.2", i == 2);
            }
            PackageVersion latest = ccd.getLatestVersion();
            assertEquals(new PackageVersion("1.2.3"), latest, states[i]);
            assertEquals(expected[i], PackageInstaller.installsAsMaven(ccd, latest), states[i]);
            assertEquals(expected[i] ? "1.2.3 (Maven)" : "1.2.3", ccd.getLatestLabel(), states[i]);
        }
    }

    /** 1. only Maven attributes, no ZIP: the version installs from Maven whatever is installed */
    @Test
    void aMavenOnlyEntryInstallsFromMaven() throws Exception {
        assertShape(entry("1.2.3", MAVEN_ATTRS), true, true, true);
    }

    /** 2. only a ZIP, as today: the version installs from the ZIP whatever is installed */
    @Test
    void aZipOnlyEntryInstallsTheZip() throws Exception {
        assertShape(entry("1.2.3", ZIP_ATTRS), false, false, false);
    }

    /** 3a. ZIP and Maven on one entry: from Maven only when the copy in use is from Maven */
    @Test
    void anEntryWithBothKeepsTheFormatInUse() throws Exception {
        assertShape(entry("1.2.3", ZIP_ATTRS + " " + MAVEN_ATTRS), false, false, true);
    }

    /** 3b. 1.2.2 as a ZIP and 1.2.3 on Maven only: 1.2.3 installs from Maven, 1.2.2 from the ZIP */
    @Test
    void aListMixingZipAndMavenOnlyVersionsGivesEachItsFormat() throws Exception {
        String entries = entry("1.2.2", "url=\"https://example.invalid/CCD.v1.2.2.zip\"") + entry("1.2.3", MAVEN_ATTRS);
        assertShape(entries, true, true, true);

        Package ccd = load("<packages>" + entries + "</packages>").get("CCD");
        assertFalse(PackageInstaller.installsAsMaven(ccd, new PackageVersion("1.2.2")));
    }

    /** a version only on Maven is installed from its coordinate, also when not installed yet; no ZIP is downloaded */
    @Test
    void aMavenOnlyVersionIsInstalledFromMaven() throws Exception {
        Package ccd = load("<packages>" + entry("1.2.3", MAVEN_ATTRS) + "</packages>").get("CCD");

        Map<String, String> dirs = PackageManager.installPackages(
                new HashMap<>(Map.of(ccd, new PackageVersion("1.2.3"))), false, null);

        assertEquals(List.of("io.github.compevol:ccd:1.2.3"), mavenInstalls);
        assertTrue(dirs.get("CCD").endsWith(PackageManager.MAVEN_PACKAGES_FILE), dirs.get("CCD"));
    }

    /**
     * the same version as a ZIP in one package list (e.g. CBAN) and only on Maven in another (e.g. a
     * list the user added) is available as both, whichever list is read first
     */
    @Test
    void twoListsOfferingOneVersionGiveBothFormats() throws Exception {
        for (List<String> order : List.of(List.of(ZIP_ATTRS, MAVEN_ATTRS), List.of(MAVEN_ATTRS, ZIP_ATTRS))) {
            Map<String, Package> packageMap = new HashMap<>();
            for (String attrs : order) {
                new PackageRepository().loadURL(URI.create("https://example.invalid/packages.xml").toURL(),
                        new ByteArrayInputStream(("<packages>" + entry("1.2.3", attrs) + "</packages>")
                                .getBytes(StandardCharsets.UTF_8)), packageMap);
            }
            Package ccd = packageMap.get("CCD");
            PackageVersion v = new PackageVersion("1.2.3");
            assertEquals(zipUrl("CCD", "1.2.3"), ccd.getVersionURL(v), order.toString());
            assertEquals("io.github.compevol:ccd:1.2.3", ccd.getAvailableMavenCoordinate(v), order.toString());
        }
    }

    // ---- which format an install uses, and the Latest column ----

    /** Maven 1.2.2 in use, CBAN has 1.2.3 on Maven: the upgrade is from Maven, shown as "1.2.3 (Maven)" */
    @Test
    void aMavenCopyIsUpgradedFromMaven() {
        Package ccd = installed(ccd("1.2.3"), "1.2.2", true);

        assertTrue(ccd.installsAsMaven(new PackageVersion("1.2.3")));
        assertEquals("1.2.3 (Maven)", ccd.getLatestLabel());
        assertTrue(ccd.newVersionAvailable());
    }

    /** Maven 1.2.2 in use, CBAN has 1.2.3 as a ZIP only: the upgrade is the ZIP, shown as "1.2.3" */
    @Test
    void aMavenCopyIsUpgradedFromTheZipWhenCbanHasNoMavenCoordinate() {
        Package ccd = installed(ccd(), "1.2.2", true);

        assertFalse(ccd.installsAsMaven(new PackageVersion("1.2.3")));
        assertEquals("1.2.3", ccd.getLatestLabel());
    }

    /** a ZIP copy in use stays a ZIP, even when CBAN has the new version on Maven too */
    @Test
    void aZipCopyIsUpgradedFromTheZip() {
        Package ccd = installed(ccd("1.2.2", "1.2.3"), "1.2.2", false);

        assertFalse(ccd.installsAsMaven(new PackageVersion("1.2.3")));
        assertEquals("1.2.3", ccd.getLatestLabel());
    }

    /** a package not installed yet is installed as a ZIP when CBAN has one */
    @Test
    void aNewPackageIsInstalledFromTheZip() {
        Package ccd = ccd("1.2.2", "1.2.3");

        assertFalse(ccd.installsAsMaven(new PackageVersion("1.2.3")));
        assertEquals("1.2.3", ccd.getLatestLabel());
    }

    /** the case in #316: Maven 1.2.2 installed, CBAN's latest is 1.2.2: Latest is 1.2.2 and nothing is offered */
    @Test
    void theInstalledMavenVersionIsTheLatest() {
        Package ccd = new Package("CCD");
        ccd.addAvailableVersion(new PackageVersion("1.2.2"), zipUrl("CCD", "1.2.2"), needs("BEAST.base", "2.8.0"), CCD_GA);
        installed(ccd, "1.2.2", true);

        assertEquals("1.2.2 (Maven)", ccd.getLatestLabel());
        assertFalse(ccd.newVersionAvailable());
    }

    /** choosing a version into the archive folder is always a ZIP */
    @Test
    void theArchiveFolderStaysZipOnly() {
        Package ccd = installed(ccd("1.2.2", "1.2.3"), "1.2.3", true);
        Package mavenOnly = new Package("X");
        mavenOnly.addAvailableVersion(new PackageVersion("1.0.0"), null, needs("BEAST.base", "2.8.0"), "g:x");

        PackageInstaller.useArchive(true);
        assertFalse(PackageInstaller.installsAsMaven(ccd, new PackageVersion("1.2.2")));
        assertFalse(PackageInstaller.installsAsMaven(mavenOnly, new PackageVersion("1.0.0")));
        PackageInstaller.useArchive(false);
        assertTrue(PackageInstaller.installsAsMaven(ccd, new PackageVersion("1.2.2")));
        assertTrue(PackageInstaller.installsAsMaven(mavenOnly, new PackageVersion("1.0.0")));
    }

    /**
     * #322 review: with Maven 1.2.2 in use, choosing 1.2.1, which is only on Maven, into the
     * archive is refused with a clear message, instead of replacing the copy in use
     */
    @Test
    void anOlderMavenOnlyVersionIsRefusedInTheArchive() throws IOException {
        PackageManager.saveMavenPackageConfig(List.of(new PackageManager.MavenCoordinate("io.github.compevol", "ccd", "1.2.2")));
        Package ccd = new Package("CCD");
        ccd.addAvailableVersion(new PackageVersion("1.2.1"), null, needs("BEAST.base", "2.8.0"), CCD_GA);
        ccd.addAvailableVersion(new PackageVersion("1.2.2"), null, needs("BEAST.base", "2.8.0"), CCD_GA);
        installed(ccd, "1.2.2", true);
        Map<Package, PackageVersion> packagesToInstall = new HashMap<>(Map.of(ccd, new PackageVersion("1.2.1")));

        PackageInstaller.useArchive(true);
        PackageManager.prepareForInstall(packagesToInstall, false, null);
        IOException e = assertThrows(IOException.class,
                () -> PackageManager.installPackages(packagesToInstall, false, null));

        assertTrue(e.getMessage().contains("CCD 1.2.1 is only available from Maven and can't be installed in the archive. To install it from Maven instead"), e.getMessage());
        assertTrue(e.getMessage().contains("packagemanager -maven io.github.compevol:ccd:1.2.1"), e.getMessage());
        assertEquals(List.of(), mavenInstalls);
        assertEquals("1.2.2", PackageManager.loadMavenPackageConfig().get(0).version);
    }

    /** a version in no package list (e.g. a typo in -version) is refused as such, in the archive or not */
    @Test
    void aVersionInNoListIsRefusedAsSuch() {
        Package ccd = ccd("1.2.3");
        for (boolean archive : new boolean[] {true, false}) {
            PackageInstaller.useArchive(archive);
            IOException e = assertThrows(IOException.class, () -> PackageManager.installPackages(
                    new HashMap<>(Map.of(ccd, new PackageVersion("9.9"))), false, null));
            assertEquals("CCD 9.9 is not in any package list.", e.getMessage());
        }
        assertEquals(List.of(), mavenInstalls);
    }

    // ---- installing ----

    /**
     * upgrading a listed Maven package from Maven keeps its line until the new version is
     * installed: installing from Maven replaces it, or leaves it if the new version is refused
     */
    @Test
    void upgradingFromMavenKeepsTheMavenLineUntilInstalled() throws IOException {
        PackageManager.saveMavenPackageConfig(List.of(new PackageManager.MavenCoordinate("io.github.compevol", "ccd", "1.2.2")));
        Package ccd = installed(ccd("1.2.3"), "1.2.2", true);
        Map<Package, PackageVersion> packagesToInstall = new HashMap<>(Map.of(ccd, new PackageVersion("1.2.3")));

        PackageManager.prepareForInstall(packagesToInstall, false, null);

        assertEquals(new PackageVersion("1.2.3"), packagesToInstall.get(ccd));
        assertEquals(1, PackageManager.loadMavenPackageConfig().size());
    }

    /** the upgrade from Maven installs CBAN's coordinate and downloads no ZIP */
    @Test
    void upgradingFromMavenInstallsTheCoordinate() throws IOException {
        Package ccd = installed(ccd("1.2.3"), "1.2.2", true);

        Map<String, String> dirs = PackageManager.installPackages(
                new HashMap<>(Map.of(ccd, new PackageVersion("1.2.3"))), false, null);

        assertEquals(List.of("io.github.compevol:ccd:1.2.3"), mavenInstalls);
        assertTrue(dirs.get("CCD").endsWith(PackageManager.MAVEN_PACKAGES_FILE), dirs.get("CCD"));
    }

    /** if the Maven install refuses the version (e.g. the planner would not load it), the install fails with its reason */
    @Test
    void aRefusedMavenInstallFailsWithItsReason() {
        PackageInstaller.mavenInstall = (g, a, v) -> {
            throw new IllegalArgumentException("Not installing " + g + ":" + a + ":" + v + ": needs BEAST.base 9.0.0");
        };
        Package ccd = installed(ccd("1.2.3"), "1.2.2", true);

        IOException e = assertThrows(IOException.class, () -> PackageManager.installPackages(
                new HashMap<>(Map.of(ccd, new PackageVersion("1.2.3"))), false, null));
        assertTrue(e.getMessage().contains("needs BEAST.base 9.0.0"), e.getMessage());
    }

    // ---- dependencies ----

    /** packageMap with BEAST.base 2.8.0 installed, {@code ccd}, and NEWDEP 1.0.0, which CCD 1.2.3 now needs; 1.2.3 is on Maven if {@code onMaven} */
    private static Map<String, Package> withNewDependency(Package ccd, boolean onMaven) {
        Map<String, Package> packageMap = new HashMap<>();
        Package base = new Package("BEAST.base");
        base.setInstalled(new PackageVersion("2.8.0"), new HashSet<>());
        packageMap.put("BEAST.base", base);
        ccd.addAvailableVersion(new PackageVersion("1.2.3"), zipUrl("CCD", "1.2.3"),
                needs("BEAST.base", "2.8.0", "NEWDEP", "1.0.0"), onMaven ? CCD_GA : null);
        packageMap.put("CCD", ccd);
        Package newDep = new Package("NEWDEP");
        newDep.addAvailableVersion(new PackageVersion("1.0.0"), zipUrl("NEWDEP", "1.0.0"), needs("BEAST.base", "2.8.0"));
        packageMap.put("NEWDEP", newDep);
        return packageMap;
    }

    /** a package upgraded from Maven gets its new dependency from Maven: no ZIP copy of it is added */
    @Test
    void anUpgradeFromMavenAddsNoZipDependencies() throws Exception {
        Package ccd = installed(ccd("1.2.3"), "1.2.2", true);
        Map<String, Package> packageMap = withNewDependency(ccd, true);
        Map<Package, PackageVersion> packagesToInstall = new HashMap<>(Map.of(ccd, new PackageVersion("1.2.3")));

        PackageManager.populatePackagesToInstall(packageMap, packagesToInstall);

        assertEquals(Map.of(ccd, new PackageVersion("1.2.3")), packagesToInstall);
    }

    /** a ZIP upgrade still installs its new dependency as a ZIP, as before */
    @Test
    void aZipUpgradeStillAddsItsDependencies() throws Exception {
        Package ccd = installed(ccd(), "1.2.2", false);
        Map<String, Package> packageMap = withNewDependency(ccd, false);
        Map<Package, PackageVersion> packagesToInstall = new HashMap<>(Map.of(ccd, new PackageVersion("1.2.3")));

        PackageManager.populatePackagesToInstall(packageMap, packagesToInstall);

        assertEquals(new PackageVersion("1.0.0"), packagesToInstall.get(packageMap.get("NEWDEP")));
    }
}
