package beast.pkgmgmt.plan;

import static beast.pkgmgmt.plan.Fixture.app;
import static beast.pkgmgmt.plan.Fixture.base;
import static beast.pkgmgmt.plan.Fixture.developer;
import static beast.pkgmgmt.plan.Fixture.headless;
import static beast.pkgmgmt.plan.Fixture.pkg;
import static beast.pkgmgmt.plan.Fixture.pkgs;
import static beast.pkgmgmt.plan.Fixture.release;
import static beast.pkgmgmt.plan.Fixture.requires;
import static beast.pkgmgmt.plan.InstalledPackage.Location.BOOT;
import static beast.pkgmgmt.plan.InstalledPackage.Location.BUNDLED;
import static beast.pkgmgmt.plan.InstalledPackage.Location.MAVEN;
import static beast.pkgmgmt.plan.InstalledPackage.Location.OVERRIDE;
import static beast.pkgmgmt.plan.InstalledPackage.Location.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import beast.pkgmgmt.plan.LoadPlan.Excluded;

/**
 * The cases the planner must get right (design doc, "Testing"), each on a made-up install: no
 * jars, no network, no module layers.
 */
class PlannerTest {

    /** the bundled core of a 2.8.0 installer */
    private static List<InstalledPackage> core() {
        return List.of(base("2.8.0", BUNDLED).needs(Planner.LAUNCHER, "2.8.0").build(),
                app("2.8.0", BUNDLED).needs(Planner.LAUNCHER, "2.8.0").build());
    }

    private static List<InstalledPackage> with(InstalledPackage... packages) {
        List<InstalledPackage> all = new ArrayList<>(core());
        all.addAll(List.of(packages));
        return all;
    }

    private static String reason(LoadPlan plan, String name) {
        return plan.exclusion(name).map(Excluded::reason).orElse(null);
    }

    private static void assertLoaded(LoadPlan plan, String name, String version) {
        assertTrue(plan.loadedPackage(name).isPresent(), name + " not loaded:\n" + plan.describe());
        assertEquals(Version.of(version), plan.loadedPackage(name).get().version(), plan.describe());
    }

    @Test
    void bundledCoreAloneLoads() {
        LoadPlan plan = Planner.plan(core(), release());
        assertLoaded(plan, "BEAST.base", "2.8.0");
        assertLoaded(plan, "BEAST.app", "2.8.0");
        assertEquals(List.of("beast.base", "beast.fx"), plan.modules().stream().map(LoadPlan.PlannedModule::name).toList());
        assertTrue(plan.excluded().isEmpty());
    }

    /** #298: ccd 1.2.2 was built on 2.8.0-rc1, and its POM brings that core along */
    @Test
    void mavenPackageNeverBringsItsOwnCore() {
        InstalledPackage ccd = pkg("ccd", "1.2.2", MAVEN).needs("BEAST.base", "2.8.0-rc1")
                .own("ccd", pkgs("ccd.model"), requires("beast.base"))
                .lib("beast.base", "2.8.0-rc1", pkgs("beast.base.core", "beast.base.inference"), requires())
                .lib("beast.fx", "2.8.0-rc1", pkgs("beastfx.app"), requires("beast.base"))
                .lib("javafx.base", "21.0.1", pkgs("javafx.beans"), requires())
                .lib("commons.text", "1.12.0", pkgs("org.apache.commons.text"), requires())
                .build();
        LoadPlan plan = Planner.plan(with(ccd), release());
        assertLoaded(plan, "ccd", "1.2.2");
        assertEquals("BEAST.base", plan.module("beast.base").get().providedBy());
        assertEquals(Version.of("2.8.0"), plan.module("beast.base").get().version());
        assertEquals("BEAST.app", plan.module("beast.fx").get().providedBy());
        assertFalse(plan.module("javafx.base").isPresent(), "JavaFX comes from the JRE");
        assertTrue(plan.module("commons.text").isPresent());
        assertTrue(plan.modules().stream().noneMatch(m -> m.version().equals(Version.of("2.8.0-rc1"))), plan.describe());
        assertEquals(List.of("ccd 1.2.2: ignores 3 jars of BEAST, JavaFX or the JRE among its dependencies, "
                + "e.g. beast.base 2.8.0-rc1; BEAST's own copies are used"), plan.warnings());
    }

    /** seen on the rc3 install: JavaFX 25 on Maven Central brings jdk.jsobject, which the JRE provides */
    @Test
    void jreModulesNeverComeFromPackages() {
        InstalledPackage p = pkg("FxUser", "1.0", MAVEN).own("fxuser", pkgs("fxuser"), requires("javafx.web"))
                .lib("jdk.jsobject", "25.0.2", pkgs("netscape.javascript"), requires())
                .lib("jdk.jsobjectEmpty", "25.0.2", pkgs(), requires())
                .lib("javafx.webEmpty", "25.0.2", pkgs(), requires()).build();
        LoadPlan plan = Planner.plan(with(p), release());
        assertLoaded(plan, "FxUser", "1.0");
        assertTrue(plan.modules().stream().noneMatch(m -> m.name().startsWith("jdk.") || m.name().startsWith("javafx.")),
                plan.describe());
    }

    @Test
    void packageNeedingNewerCoreIsExcluded() {
        InstalledPackage future = pkg("Future", "1.0.0", USER).needs("BEAST.base", "2.9.0")
                .own("future", pkgs("future"), requires("beast.base")).build();
        LoadPlan plan = Planner.plan(with(future), release());
        assertEquals("needs BEAST.base 2.9.0 or a later 2.9.x; installed: 2.8.0", reason(plan, "Future"));
    }

    /** a package built for BEAST 2 does not run on BEAST 3 */
    @Test
    void coreDependencyStopsAtTheMajorVersion() {
        InstalledPackage old = pkg("Old", "1.0.0", USER).needs("BEAST.base", "2.7.0")
                .own("old", pkgs("old"), requires("beast.base")).build();
        Runtime beast3 = new Runtime(Version.of("3.0.0"), release().javafx(), release().bootModules());
        LoadPlan plan = Planner.plan(List.of(base("3.0.0", BUNDLED).build(), old), beast3);
        assertEquals("needs BEAST.base 2.7.0 or a later 2.7.x; installed: 3.0.0", reason(plan, "Old"));
    }

    /** minor releases of BEAST have broken packages: a package built for 2.8.x is not loaded on 2.9 */
    @Test
    void coreDependencyStopsAtTheMinorVersion() {
        InstalledPackage p = pkg("Built28", "1.0", USER).needs("BEAST.base", "2.8.0-rc3")
                .own("built28", pkgs("built28"), requires("beast.base")).build();
        List<InstalledPackage> on281 = new ArrayList<>(List.of(base("2.8.1", BUNDLED).build(), p));
        assertLoaded(Planner.plan(on281, release()), "Built28", "1.0");
        List<InstalledPackage> on290 = new ArrayList<>(List.of(base("2.9.0", BUNDLED).build(), p));
        assertEquals("needs BEAST.base 2.8.0-rc3 or a later 2.8.x; installed: 2.9.0", reason(Planner.plan(on290, release()), "Built28"));
    }

    /** the installed SA ZIP is used; a Maven package's POM version of SA is only a minimum */
    @Test
    void installedPackageBeatsALibraryCopyOfIt() {
        InstalledPackage sa = pkg("SA", "2.3.0", USER).needs("BEAST.base", "2.8.0")
                .own("sampled.ancestors", pkgs("sa.evolution"), requires("beast.base")).build();
        InstalledPackage uses = pkg("UsesSA", "1.0.0", MAVEN).needs("SA", "2.2.0")
                .own("uses.sa", pkgs("uses.sa"), requires("sampled.ancestors"))
                .lib("sampled.ancestors", "2.2.0", pkgs("sa.evolution"), requires("beast.base")).build();
        LoadPlan plan = Planner.plan(with(sa, uses), release());
        assertLoaded(plan, "UsesSA", "1.0.0");
        assertEquals("SA", plan.module("sampled.ancestors").get().providedBy());
        assertEquals(Version.of("2.3.0"), plan.module("sampled.ancestors").get().version());
    }

    @Test
    void oneLibraryVersionTheHighest() {
        InstalledPackage a = pkg("A", "1.0", USER).own("a", pkgs("a"), requires("commons.foo"))
                .lib("commons.foo", "1.2", pkgs("org.foo"), requires()).build();
        InstalledPackage b = pkg("B", "1.0", USER).own("b", pkgs("b"), requires("commons.foo"))
                .lib("commons.foo", "1.10", pkgs("org.foo"), requires()).build();
        LoadPlan plan = Planner.plan(with(a, b), release());
        assertEquals(Version.of("1.10"), plan.module("commons.foo").get().version());
        assertEquals("B", plan.module("commons.foo").get().providedBy());
        assertEquals(1, plan.modules().stream().filter(m -> m.name().equals("commons.foo")).count());
    }

    @Test
    void samePackageInTwoModulesExcludesTheLaterPackage() {
        InstalledPackage alpha = pkg("Alpha", "1.0", USER).own("alpha", pkgs("shared.util"), requires()).build();
        InstalledPackage beta = pkg("Beta", "1.0", USER).own("beta", pkgs("shared.util", "beta"), requires()).build();
        LoadPlan plan = Planner.plan(with(alpha, beta), release());
        assertLoaded(plan, "Alpha", "1.0");
        assertEquals("Java package shared.util is in both alpha (Alpha) and beta (Beta)", reason(plan, "Beta"));
    }

    @Test
    void packageMayNotShipTheCoreAsItsOwnModule() {
        InstalledPackage fork = pkg("Fork", "1.0", USER).own("beast.base", pkgs("beast.base.core"), requires()).build();
        LoadPlan plan = Planner.plan(with(fork), release());
        assertEquals("contains beast.base, which only BEAST.base and BEAST.app provide", reason(plan, "Fork"));
    }

    /** MM: a core module and an FX module; on a cluster the core module still loads */
    @Test
    void headlessLeavesOutFxModulesOnly() {
        InstalledPackage mm = pkg("MM", "1.3.1", USER).needs("BEAST.base", "2.8.0").needs("BEAST.app", "2.8.0")
                .own("beast.morph.models", pkgs("morph.models"), requires("beast.base"))
                .own("beast.morph.models.fx", pkgs("morph.models.fx"), requires("beast.morph.models", "beast.fx")).build();
        LoadPlan plan = Planner.plan(with(mm), headless());
        assertLoaded(plan, "MM", "1.3.1");
        assertTrue(plan.module("beast.morph.models").isPresent());
        assertFalse(plan.module("beast.morph.models.fx").isPresent());
        assertTrue(plan.droppedModules().stream().anyMatch(d -> d.module().equals("beast.morph.models.fx")
                && d.reason().equals("headless")), plan.describe());
        assertEquals("headless", reason(plan, "BEAST.app"));
    }

    @Test
    void folderOrderDoesNotMatter() {
        List<InstalledPackage> all = with(
                pkg("ORC", "1.3.1", USER).needs("BEAST.base", "2.8.0").own("orc", pkgs("orc"), requires("beast.base")).build(),
                pkg("SA", "2.3.0", USER).needs("BEAST.base", "2.8.0").own("sa", pkgs("sa"), requires("beast.base")).build(),
                pkg("starbeast3", "1.3.1", USER).needs("ORC", "1.3.0").needs("SA", "2.3.0")
                        .own("starbeast3", pkgs("sb3"), requires("orc", "sa")).build(),
                pkg("SA", "2.2.0", BUNDLED).needs("BEAST.base", "2.8.0").own("sa", pkgs("sa"), requires("beast.base")).build());
        String expected = Planner.plan(all, release()).describe();
        Random random = new Random(1);
        for (int i = 0; i < 20; i++) {
            List<InstalledPackage> shuffled = new ArrayList<>(all);
            Collections.shuffle(shuffled, random);
            assertEquals(expected, Planner.plan(shuffled, release()).describe());
        }
    }

    @Test
    void missingDependencyExcludesEverythingThatNeedsIt() {
        InstalledPackage a = pkg("A", "1.0", USER).needs("Missing", "1.0").own("a", pkgs("a"), requires()).build();
        InstalledPackage c = pkg("C", "1.0", USER).needs("A", "1.0").own("c", pkgs("c"), requires("a")).build();
        LoadPlan plan = Planner.plan(with(a, c), release());
        assertEquals("needs Missing, which is not installed", reason(plan, "A"));
        assertEquals("needs A, which is excluded", reason(plan, "C"));
    }

    /** BEAST_PACKAGE_PATH wins, even with an older version */
    @Test
    void packagePathOverridesNewerCopies() {
        InstalledPackage dev = pkg("SA", "2.2.0", OVERRIDE).own("sa", pkgs("sa"), requires()).build();
        InstalledPackage user = pkg("SA", "2.3.0", USER).own("sa", pkgs("sa"), requires()).build();
        LoadPlan plan = Planner.plan(with(user, dev), release());
        assertLoaded(plan, "SA", "2.2.0");
        assertEquals("using 2.2.0 (override)", plan.notUsed().get(0).reason());
    }

    @Test
    void developerModeCoreIsOnTheBootLayer() {
        List<InstalledPackage> all = List.of(base("2.8.0", BOOT).build(), app("2.8.0", BOOT).build(),
                pkg("SA", "2.3.0", USER).needs("BEAST.base", "2.8.0").own("sa", pkgs("sa"), requires("beast.base")).build());
        LoadPlan plan = Planner.plan(all, developer());
        assertLoaded(plan, "BEAST.base", "2.8.0");
        assertLoaded(plan, "SA", "2.3.0");
        assertEquals(List.of("sa"), plan.modules().stream().map(LoadPlan.PlannedModule::name).toList(),
                "the core is not loaded again");
    }

    // ---- core upgrades (R15) ----

    @Test
    void upgradedAppOnBundledBase() {
        List<InstalledPackage> all = new ArrayList<>(core());
        all.add(app("2.8.1", USER).needs(Planner.LAUNCHER, "2.8.0").build());
        LoadPlan plan = Planner.plan(all, release());
        assertLoaded(plan, "BEAST.app", "2.8.1");
        assertLoaded(plan, "BEAST.base", "2.8.0");
        assertEquals(Version.of("2.8.1"), plan.module("beast.fx").get().version());
    }

    @Test
    void coreUpgradeNeedingANewerInstallerFallsBackToTheBundledCopy() {
        List<InstalledPackage> all = new ArrayList<>(core());
        all.add(base("2.8.2", USER).needs(Planner.LAUNCHER, "2.8.1").build());
        LoadPlan plan = Planner.plan(all, release());
        assertLoaded(plan, "BEAST.base", "2.8.0");
        Excluded skipped = plan.notUsed().stream().filter(e -> e.copy().version().equals(Version.of("2.8.2"))).findFirst().get();
        assertEquals("needs a newer BEAST installer (launcher 2.8.1 or later; this one has 2.8.0)", skipped.reason());
    }

    /** decision 7: BEAST 3.0 is a new installer, never a core upgrade on a 2.8 installer */
    @Test
    void coreUpgradeToANewMajorVersionIsNotUsed() {
        List<InstalledPackage> all = new ArrayList<>(core());
        all.add(base("3.0.0", USER).build());
        LoadPlan plan = Planner.plan(all, release());
        assertLoaded(plan, "BEAST.base", "2.8.0");
        Excluded skipped = plan.notUsed().stream().filter(e -> e.copy().version().equals(Version.of("3.0.0"))).findFirst().get();
        assertEquals("BEAST 3.x needs a new BEAST installer (this one is 2.8.0)", skipped.reason());
    }

    /** an upgraded copy that does not work does not hold up the packages that work with another copy */
    @Test
    void dependentsUseTheCopyThatWorks() {
        InstalledPackage sa24 = pkg("SA", "2.4.0", USER).needs("BEAST.base", "2.9.0").own("sa", pkgs("sa"), requires("beast.base")).build();
        InstalledPackage sa23 = pkg("SA", "2.3.0", BUNDLED).needs("BEAST.base", "2.8.0").own("sa", pkgs("sa"), requires("beast.base")).build();
        // sorts before SA, and accepts only SA 2.3.x, so it fails as long as SA 2.4.0 is the copy tried
        InstalledPackage early = pkg("Aardvark", "1.0", USER).needsUpTo("SA", "2.3.0", "2.3.99")
                .own("aardvark", pkgs("aardvark"), requires("sa")).build();
        LoadPlan plan = Planner.plan(with(sa24, sa23, early), release());
        assertLoaded(plan, "SA", "2.3.0");
        assertLoaded(plan, "Aardvark", "1.0");
        assertNull(reason(plan, "Aardvark"));
    }

    @Test
    void pomNamingAFutureCoreExcludesThePackage() {
        InstalledPackage next = pkg("Next", "0.1", MAVEN).needs("BEAST.base", "2.9.0-SNAPSHOT")
                .own("next", pkgs("next"), requires("beast.base"))
                .lib("beast.base", "2.9.0-SNAPSHOT", pkgs("beast.base.core"), requires()).build();
        LoadPlan plan = Planner.plan(with(next), release());
        assertEquals("needs BEAST.base 2.9.0-SNAPSHOT or a later 2.9.x; installed: 2.8.0", reason(plan, "Next"));
        assertTrue(plan.modules().stream().noneMatch(m -> m.version().toString().contains("SNAPSHOT")));
    }

    @Test
    void newerJavaFXOnlyWarns() {
        InstalledPackage p = pkg("Shiny", "1.0", USER).needs(Planner.JAVAFX, "26")
                .own("shiny", pkgs("shiny"), requires("javafx.controls")).build();
        LoadPlan plan = Planner.plan(with(p), release());
        assertLoaded(plan, "Shiny", "1.0");
        assertEquals(List.of("Shiny 1.0 asks for JavaFX 26 or later; this BEAST has JavaFX 25.0.3"), plan.warnings());
    }

    @Test
    void moduleNeedingAMissingModuleIsLeftOut() {
        InstalledPackage p = pkg("Half", "1.0", USER)
                .own("half.core", pkgs("half"), requires("beast.base"))
                .own("half.extra", pkgs("half.extra"), requires("not.there")).build();
        LoadPlan plan = Planner.plan(with(p), release());
        assertLoaded(plan, "Half", "1.0");
        assertEquals("needs module not.there", plan.droppedModules().get(0).reason());
    }
}
