package beast.pkgmgmt.plan;

import static beast.pkgmgmt.plan.Fixture.app;
import static beast.pkgmgmt.plan.Fixture.base;
import static beast.pkgmgmt.plan.Fixture.pkg;
import static beast.pkgmgmt.plan.Fixture.pkgs;
import static beast.pkgmgmt.plan.Fixture.release;
import static beast.pkgmgmt.plan.Fixture.requires;
import static beast.pkgmgmt.plan.InstalledPackage.Location.BUNDLED;
import static beast.pkgmgmt.plan.InstalledPackage.Location.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import beast.pkgmgmt.plan.LoadPlan.PlannedModule;

/**
 * Packages needing incompatible versions of a library: each version is a private copy in its
 * own layer, with the packages that need it and those that build on them.
 */
class LibraryConflictTest {

    private static List<InstalledPackage> install(InstalledPackage... packages) {
        List<InstalledPackage> all = new ArrayList<>(List.of(base("2.8.0", BUNDLED).build(), app("2.8.0", BUNDLED).build()));
        all.addAll(List.of(packages));
        return all;
    }

    /** a package with its own module, needing commons.foo at a version */
    private static InstalledPackage uses(String name, String fooVersion) {
        String module = name.toLowerCase();
        return pkg(name, "1.0", USER).own(module, pkgs(module), requires("commons.foo"))
                .lib("commons.foo", fooVersion, pkgs("org.foo"), requires()).build();
    }

    private static InstalledPackage buildsOn(String name, String... others) {
        Fixture.Pkg p = pkg(name, "1.0", USER);
        for (String o : others) {
            p.needs(o, "1.0");
        }
        String[] mods = java.util.Arrays.stream(others).map(String::toLowerCase).toArray(String[]::new);
        return p.own(name.toLowerCase(), pkgs(name.toLowerCase()), requires(mods)).build();
    }

    private static String layerOf(LoadPlan plan, String module) {
        return plan.modules().stream().filter(m -> m.name().equals(module)).map(PlannedModule::layer).findFirst().orElseThrow();
    }

    @Test
    void incompatibleVersionsGetPrivateCopiesAndNoneIsShared() {
        LoadPlan plan = Planner.plan(install(uses("A", "1.4"), uses("B", "2.1"), uses("C", "2.0")), release());
        assertEquals(Set.of("A", "B"), plan.privateLayers(), plan.describe());
        assertEquals(Version.of("1.4"), plan.module("commons.foo", "A").get().version());
        assertEquals(Version.of("2.1"), plan.module("commons.foo", "B").get().version(), "B and C share the higher 2.x");
        assertFalse(plan.module("commons.foo", Planner.SHARED).isPresent(), plan.describe());
        assertEquals("B", layerOf(plan, "c"));
    }

    @Test
    void compatibleVersionsStayShared() {
        LoadPlan plan = Planner.plan(install(uses("A", "1.4"), uses("B", "1.9")), release());
        assertTrue(plan.privateLayers().isEmpty());
        assertEquals(Version.of("1.9"), plan.module("commons.foo", Planner.SHARED).get().version());
    }

    /** D builds on A, so it goes with A's copy; E builds on nothing conflicting and stays shared */
    @Test
    void dependentsFollowIntoThePrivateLayer() {
        LoadPlan plan = Planner.plan(install(uses("A", "1.4"), uses("B", "2.1"), buildsOn("D", "A"),
                buildsOn("E", "D"), pkg("F", "1.0", USER).own("f", pkgs("f"), requires()).build()), release());
        assertEquals("A", layerOf(plan, "d"));
        assertEquals("A", layerOf(plan, "e"), "dependents of dependents too");
        assertEquals(Planner.SHARED, layerOf(plan, "f"));
    }

    /** Q builds on A (foo 1.x) and on B (foo 2.x): it cannot see both copies */
    @Test
    void packageNeedingTwoCopiesIsExcluded() {
        LoadPlan plan = Planner.plan(install(uses("A", "1.4"), uses("B", "2.1"), buildsOn("Q", "A", "B")), release());
        assertEquals("needs packages that use incompatible versions of commons.foo (1.4, 2.1)",
                plan.exclusion("Q").get().reason());
        assertEquals(Set.of("A", "B"), plan.privateLayers());
    }

    /** R needs foo 2.x itself but builds on A, which uses foo 1.x */
    @Test
    void packageNeedingAnotherVersionThanItsDependencyIsExcluded() {
        InstalledPackage r = pkg("R", "1.0", USER).needs("A", "1.0").own("r", pkgs("r"), requires("a", "commons.foo"))
                .lib("commons.foo", "2.0", pkgs("org.foo"), requires()).build();
        LoadPlan plan = Planner.plan(install(uses("A", "1.4"), r), release());
        assertTrue(plan.exclusion("R").isPresent(), plan.describe());
        assertTrue(plan.exclusion("R").get().reason().contains("commons.foo"), plan.exclusion("R").get().reason());
        assertTrue(plan.loadedPackage("A").isPresent());
    }
}
