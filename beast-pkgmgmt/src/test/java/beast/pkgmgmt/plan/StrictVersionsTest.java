package beast.pkgmgmt.plan;

import static beast.pkgmgmt.plan.Fixture.app;
import static beast.pkgmgmt.plan.Fixture.base;
import static beast.pkgmgmt.plan.Fixture.pkg;
import static beast.pkgmgmt.plan.Fixture.pkgs;
import static beast.pkgmgmt.plan.Fixture.release;
import static beast.pkgmgmt.plan.Fixture.requires;
import static beast.pkgmgmt.plan.InstalledPackage.Location.ARCHIVE;
import static beast.pkgmgmt.plan.InstalledPackage.Location.BUNDLED;
import static beast.pkgmgmt.plan.InstalledPackage.Location.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import beast.pkgmgmt.plan.InstalledPackage.Location;

/**
 * {@code beast -strictversions}: an analysis runs with the package versions it was made with,
 * from archive copies ({@code packagemanager -add SA -version 2.1.1}), and nothing else.
 */
class StrictVersionsTest {

    @TempDir
    Path dir;

    private static InstalledPackage sa(String version, Location location) {
        return pkg("SA", version, location).needs("BEAST.base", "2.8.0").own("sa", pkgs("sa"), requires("beast.base")).build();
    }

    private static List<InstalledPackage> install(InstalledPackage... packages) {
        List<InstalledPackage> all = new ArrayList<>(List.of(base("2.8.0", BUNDLED).build(), app("2.8.0", BUNDLED).build()));
        all.addAll(List.of(packages));
        return all;
    }

    private static InstalledPackage orc() {
        return pkg("ORC", "1.3.1", USER).needs("BEAST.base", "2.8.0").own("orc", pkgs("orc"), requires("beast.base")).build();
    }

    private static boolean isLoaded(LoadPlan plan, String name, String version) {
        return plan.loadedPackage(name).map(p -> p.version().equals(Version.of(version))).orElse(false);
    }

    @Test
    void requiredAttributeAsBEAUtiWritesIt() {
        Selection s = Selection.fromRequired("BEAST.base v2.7.6:SA v2.1.1: BEAST.app v2.7.6");
        assertEquals(Map.of("BEAST.base", Version.of("2.7.6"), "SA", Version.of("2.1.1"), "BEAST.app", Version.of("2.7.6")),
                s.pinned());
        assertTrue(s.strict());
    }

    @Test
    void requiredAttributeReadFromTheAnalysis() throws Exception {
        Path xml = dir.resolve("analysis.xml");
        Files.writeString(xml, "<?xml version='1.0'?>\n<!-- made by BEAUti -->\n"
                + "<beast version='2.8' required='BEAST.base v2.8.0:SA v2.1.1' namespace='beast.base'>\n</beast>");
        assertEquals(Version.of("2.1.1"), Selection.fromAnalysis(xml).pinned().get("SA"));

        Path none = dir.resolve("none.xml");
        Files.writeString(none, "<beast version='2.8'></beast>");
        assertThrows(IllegalArgumentException.class, () -> Selection.fromAnalysis(none));
    }

    @Test
    void pinnedVersionComesFromTheArchive() {
        LoadPlan plan = Planner.plan(install(sa("2.3.0", USER), sa("2.1.1", ARCHIVE), orc()), release(),
                Selection.fromRequired("BEAST.base v2.8.0:SA v2.1.1"));
        assertTrue(isLoaded(plan, "SA", "2.1.1"), plan.describe());
        assertTrue(plan.notUsed().stream().anyMatch(e -> e.copy().version().equals(Version.of("2.3.0"))
                && e.reason().equals("the analysis was made with SA 2.1.1")), plan.describe());
        assertTrue(plan.notUsed().stream().anyMatch(e -> e.copy().name().equals("ORC")
                && e.reason().equals("not used by the analysis (-strictversions)")), plan.describe());
        assertTrue(plan.missing().isEmpty());
    }

    /** without -strictversions an archive copy is only a last resort, even when it is newer */
    @Test
    void archiveIsIgnoredNormally() {
        LoadPlan plan = Planner.plan(install(sa("2.4.0", ARCHIVE), sa("2.3.0", USER), orc()), release());
        assertTrue(isLoaded(plan, "SA", "2.3.0"), plan.describe());
        assertTrue(plan.loadedPackage("ORC").isPresent());
    }

    /** as today: a package only in the archive loads its newest archived version */
    @Test
    void packageOnlyInTheArchiveLoadsItsNewestVersion() {
        LoadPlan plan = Planner.plan(install(sa("2.1.1", ARCHIVE), sa("2.2.0", ARCHIVE)), release());
        assertTrue(isLoaded(plan, "SA", "2.2.0"), plan.describe());
    }

    /** a pinned version that is not installed is reported, never replaced by another version */
    @Test
    void pinnedVersionNotInstalledIsMissing() {
        LoadPlan plan = Planner.plan(install(sa("2.3.0", USER)), release(),
                Selection.fromRequired("SA v2.1.1:ORC v1.3.1"));
        assertEquals(List.of("SA 2.1.1", "ORC 1.3.1"), plan.missing());
        assertFalse(plan.loadedPackage("SA").isPresent(), plan.describe());
        assertTrue(plan.describe().contains("packagemanager -add SA -version 2.1.1"), plan.describe());
    }

    /** a package that needs a missing pinned version says which version */
    @Test
    void dependentOfAMissingPinnedVersionSaysWhich() {
        InstalledPackage sb3 = pkg("starbeast3", "1.3.1", USER).needs("SA", "2.0.0")
                .own("starbeast3", pkgs("sb3"), requires("sa")).build();
        LoadPlan plan = Planner.plan(install(sb3, sa("2.3.0", USER)), release(),
                Selection.fromRequired("starbeast3 v1.3.1:SA v2.1.1"));
        assertEquals("needs SA 2.1.1, which the analysis was made with and is not installed",
                plan.exclusion("starbeast3").get().reason());
    }

    /** a pinned package's dependencies load too, at whatever version works */
    @Test
    void dependenciesOfPinnedPackagesLoad() {
        InstalledPackage sb3 = pkg("starbeast3", "1.3.1", USER).needs("ORC", "1.3.0").needs("SA", "2.3.0")
                .own("starbeast3", pkgs("sb3"), requires("orc", "sa")).build();
        InstalledPackage other = pkg("Other", "1.0", USER).own("other", pkgs("other"), requires()).build();
        LoadPlan plan = Planner.plan(install(sb3, orc(), sa("2.3.0", USER), other), release(),
                Selection.fromRequired("starbeast3 v1.3.1"));
        assertTrue(isLoaded(plan, "starbeast3", "1.3.1"), plan.describe());
        assertTrue(isLoaded(plan, "ORC", "1.3.1"));
        assertTrue(isLoaded(plan, "SA", "2.3.0"));
        assertFalse(plan.loadedPackage("Other").isPresent());
    }

    /** an analysis made with an older BEAST still runs, on the current core, with a warning */
    @Test
    void olderCoreVersionWarns() {
        LoadPlan plan = Planner.plan(install(), release(), Selection.fromRequired("BEAST.base v2.7.6:BEAST.app v2.7.6"));
        assertTrue(isLoaded(plan, "BEAST.base", "2.8.0"));
        assertTrue(plan.warnings().contains("the analysis was made with BEAST.base 2.7.6; using 2.8.0"), plan.warnings().toString());
        assertTrue(plan.missing().isEmpty());
    }

    /** a pinned core version that is installed is used, even when a newer copy is */
    @Test
    void pinnedCoreVersionIsPreferred() {
        List<InstalledPackage> all = install();
        all.add(app("2.8.1", USER).build());
        LoadPlan plan = Planner.plan(all, release(), Selection.fromRequired("BEAST.app v2.8.0"));
        assertTrue(isLoaded(plan, "BEAST.app", "2.8.0"), plan.describe());
        assertTrue(plan.warnings().isEmpty(), plan.warnings().toString());
    }
}
