package beast.pkgmgmt.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.plan.InstalledPackage.Location;

/**
 * Inventory, planner and loader together, on real jars compiled here: package folders as a
 * ZIP package leaves them (version.xml + lib/), read, planned and loaded into a module layer
 * in this JVM.
 * <ul>
 * <li>commons-foo 1.0, 1.1 and 2.0: a plain jar (an automatic module), {@code org.foo.Foo.version()}</li>
 * <li>beast.delta: compiled against commons.foo 2.0</li>
 * <li>beast.alpha: needs commons.foo; provides a {@code Supplier} service</li>
 * <li>beast.beta: needs beast.alpha</li>
 * <li>beast.gamma: same Java package as beast.alpha</li>
 * <li>beast.view: needs beast.missingfx, which is not installed</li>
 * </ul>
 */
class LoaderTest {

    @TempDir
    static Path built;

    @TempDir
    Path installed;

    private static Path foo1, foo11, foo2, alpha, beta, gamma, view, delta, scripted;

    @BeforeAll
    static void compile() throws IOException {
        foo1 = plainJar("commons-foo-1.0.jar", Map.of("org/foo/Foo.java",
                "package org.foo; public class Foo { public static String version() { return \"1.0\"; } }"));
        foo11 = plainJar("commons-foo-1.1.jar", Map.of("org/foo/Foo.java",
                "package org.foo; public class Foo { public static String version() { return \"1.1\"; } }"));
        foo2 = plainJar("commons-foo-2.0.jar", Map.of("org/foo/Foo.java",
                "package org.foo; public class Foo { public static String version() { return \"2.0\"; } }"));
        alpha = moduleJar("beast-alpha.jar", List.of(foo1), Map.of(
                "module-info.java", "module beast.alpha { requires commons.foo; exports alpha;"
                        + " provides java.util.function.Supplier with alpha.AlphaSupplier; }",
                "alpha/Alpha.java", "package alpha; public class Alpha {"
                        + " public static String hello() { return \"alpha on foo \" + org.foo.Foo.version(); } }",
                "alpha/AlphaSupplier.java", "package alpha; public class AlphaSupplier implements java.util.function.Supplier<String> {"
                        + " public String get() { return Alpha.hello(); } }"));
        beta = moduleJar("beast-beta.jar", List.of(alpha, foo1), Map.of(
                "module-info.java", "module beast.beta { requires beast.alpha; exports beta; }",
                "beta/Beta.java", "package beta; public class Beta {"
                        + " public static String hello() { return \"beta, \" + alpha.Alpha.hello(); } }"));
        delta = moduleJar("beast-delta.jar", List.of(foo2), Map.of(
                "module-info.java", "module beast.delta { requires commons.foo; exports delta; }",
                "delta/Delta.java", "package delta; public class Delta {"
                        + " public static String hello() { return \"delta on foo \" + org.foo.Foo.version(); } }"));
        gamma = moduleJar("beast-gamma.jar", List.of(), Map.of(
                "module-info.java", "module beast.gamma { exports alpha; }",
                "alpha/Other.java", "package alpha; public class Other { }"));
        // compiled against a module that is then never installed
        Path missing = moduleJar("beast-missingfx.jar", List.of(), Map.of(
                "module-info.java", "module beast.missingfx { exports missingfx; }",
                "missingfx/Missing.java", "package missingfx; public class Missing { }"));
        // needs a JDK module that nothing on the boot layer needs, as BEASTLabs' Nashorn needs jdk.dynalink
        scripted = moduleJar("beast-scripted.jar", List.of(), Map.of(
                "module-info.java", "module beast.scripted { requires jdk.dynalink; exports scripted; }",
                "scripted/Scripted.java", "package scripted; public class Scripted {"
                        + " public static String hello() { return \"scripted with \" + jdk.dynalink.StandardOperation.GET; } }"));
        view = moduleJar("beast-view.jar", List.of(missing), Map.of(
                "module-info.java", "module beast.view { requires beast.missingfx; }",
                "view/View.java", "package view; public class View { }"));
    }

    // ---- making packages ----

    private Path packageFolder(String name, String version, String depends, Path... jars) throws IOException {
        Path dir = Files.createDirectories(installed.resolve(name).resolve("lib")).getParent();
        Files.writeString(dir.resolve("version.xml"), "<package name='" + name + "' version='" + version + "'>"
                + depends + "</package>");
        for (Path jar : jars) {
            Files.copy(jar, dir.resolve("lib").resolve(jar.getFileName()));
        }
        return dir;
    }

    private LoadedPackages planAndLoad() throws Exception {
        Inventory inventory = new Inventory().addFolder(installed, Location.USER);
        assertTrue(inventory.problems().isEmpty(), inventory.problems().toString());
        LoadPlan plan = Planner.plan(inventory.packages(), Inventory.readThisRuntime());
        return Loader.load(plan);
    }

    private static String hello(LoadedPackages loaded, String className) throws Exception {
        return (String) loaded.loadClass(className).getMethod("hello").invoke(null);
    }

    // ---- tests ----

    @Test
    void loadsPackagesAndTheirLibraries() throws Exception {
        packageFolder("Alpha", "1.0", "", alpha, foo1);
        packageFolder("Beta", "1.0", "<depends on='Alpha' atleast='1.0'/>", beta);
        LoadedPackages loaded = planAndLoad();
        assertEquals("beta, alpha on foo 1.0", hello(loaded, "beta.Beta"));
        assertEquals("beast.beta", loaded.loadClass("beta.Beta").getModule().getName());
        assertEquals(loaded.layer(), loaded.loadClass("org.foo.Foo").getModule().getLayer());
    }

    @Test
    void servicesComeFromLoadedModules() throws Exception {
        packageFolder("Alpha", "1.0", "", alpha, foo1);
        LoadedPackages loaded = planAndLoad();
        assertTrue(loaded.providers("java.util.function.Supplier").contains("alpha.AlphaSupplier"));
    }

    /** decision 5: a provider declared only in version.xml still works, and the inventory warns */
    @Test
    void versionXmlServicesStillWork() throws Exception {
        packageFolder("Alpha", "1.0", "<service type='test.Service'><provider classname='alpha.Alpha'/></service>", alpha, foo1);
        Inventory inventory = new Inventory().addFolder(installed, Location.USER);
        assertTrue(inventory.warnings().stream().anyMatch(w -> w.contains("declared only in version.xml")),
                inventory.warnings().toString());
        LoadedPackages loaded = Loader.load(Planner.plan(inventory.packages(), Inventory.readThisRuntime()));
        assertTrue(loaded.providers("test.Service").contains("alpha.Alpha"));
        loaded.registerWithBEASTClassLoader();
        assertEquals(loaded.loadClass("alpha.Alpha"), BEASTClassLoader.forName("alpha.Alpha", "test.Service"));
    }

    /** compatible versions (same major): one shared copy, the highest, and Alpha runs on it */
    @Test
    void compatibleLibraryVersionsShareTheHighest() throws Exception {
        packageFolder("Alpha", "1.0", "", alpha, foo1);
        packageFolder("Epsilon", "1.0", "", foo11);
        LoadedPackages loaded = planAndLoad();
        assertEquals("alpha on foo 1.1", hello(loaded, "alpha.Alpha"));
        assertTrue(loaded.privateLayers().isEmpty());
    }

    /** incompatible versions (1.x and 2.x): each package runs on its own copy, in the same JVM */
    @Test
    void incompatibleLibraryVersionsEachGetTheirOwn() throws Exception {
        packageFolder("Alpha", "1.0", "", alpha, foo1);
        packageFolder("Delta", "1.0", "", delta, foo2);
        LoadedPackages loaded = planAndLoad();
        assertEquals("alpha on foo 1.0", hello(loaded, "alpha.Alpha"));
        assertEquals("delta on foo 2.0", hello(loaded, "delta.Delta"));
        assertEquals(java.util.Set.of("Alpha", "Delta"), loaded.privateLayers().keySet());
    }

    /** Beta builds on Alpha, so it joins Alpha's private layer and sees Alpha's copy */
    @Test
    void dependentJoinsThePrivateLayer() throws Exception {
        packageFolder("Alpha", "1.0", "", alpha, foo1);
        packageFolder("Beta", "1.0", "<depends on='Alpha' atleast='1.0'/>", beta);
        packageFolder("Delta", "1.0", "", delta, foo2);
        LoadedPackages loaded = planAndLoad();
        assertEquals("beta, alpha on foo 1.0", hello(loaded, "beta.Beta"));
        assertEquals("delta on foo 2.0", hello(loaded, "delta.Delta"));
        assertEquals(loaded.privateLayers().get("Alpha"), loaded.loadClass("beta.Beta").getModule().getLayer());
    }

    @Test
    void excludedPackageOffersNothing() throws Exception {
        packageFolder("Alpha", "1.0", "<depends on='Missing' atleast='1.0'/>", alpha, foo1);
        LoadedPackages loaded = planAndLoad();
        assertEquals("needs Missing, which is not installed", loaded.plan().exclusion("Alpha").get().reason());
        assertThrows(ClassNotFoundException.class, () -> loaded.loadClass("alpha.Alpha"));
        assertTrue(loaded.providers("java.util.function.Supplier").isEmpty());
    }

    /** gamma has a class in alpha's Java package: the planner excludes it, and the layer loads */
    @Test
    void javaPackageClashNeverReachesTheJVM() throws Exception {
        packageFolder("Alpha", "1.0", "", alpha, foo1);
        packageFolder("Gamma", "1.0", "", gamma);
        LoadedPackages loaded = planAndLoad();
        assertTrue(loaded.plan().exclusion("Gamma").isPresent(), loaded.plan().describe());
        assertEquals("alpha on foo 1.0", hello(loaded, "alpha.Alpha"));
    }

    @Test
    void moduleWithUnmetRequiresIsLeftOutTheRestLoads() throws Exception {
        packageFolder("Alpha", "1.0", "", alpha, view, foo1);
        LoadedPackages loaded = planAndLoad();
        assertEquals("alpha on foo 1.0", hello(loaded, "alpha.Alpha"));
        assertThrows(ClassNotFoundException.class, () -> loaded.loadClass("view.View"));
        assertEquals("needs module beast.missingfx", loaded.plan().droppedModules().get(0).reason());
    }

    /**
     * A package module that needs a JDK module the boot layer does not have, as BEASTLabs'
     * Nashorn needs jdk.dynalink in the release: the module comes from the runtime image. It
     * used to make the JVM reject the whole plan, so BEAST loaded none of its packages.
     * <p>
     * Run in a JVM of its own whose main class is in a named module, as BEAST's launchers do:
     * in this test JVM the main class is on the class path, so every JDK module with an API,
     * jdk.dynalink among them, is already on the boot layer.
     */
    @Test
    void jdkModuleNotOnTheBootLayerIsFoundInTheRuntime() throws Exception {
        packageFolder("Scripted", "1.0", "", scripted);
        Path pkgmgmt = Path.of("target", "classes").toAbsolutePath();
        Path probe = moduleJar("probe.jar", List.of(pkgmgmt), Map.of(
                "module-info.java", "module probe { requires beast.pkgmgmt; }",
                "probe/Main.java", "package probe; public class Main { public static void main(String[] a) throws Exception {"
                        + " System.out.println(\"boot has jdk.dynalink: \" + ModuleLayer.boot().findModule(\"jdk.dynalink\").isPresent());"
                        + " beast.pkgmgmt.PackageManager.loadExternalJars();"
                        + " System.out.println(beast.pkgmgmt.BEASTClassLoader.forName(\"scripted.Scripted\").getMethod(\"hello\").invoke(null)); } }"));
        Path javaExe = Path.of(System.getProperty("java.home"), "bin", "java");
        Process p = new ProcessBuilder(javaExe.toString(), "-Dbeast.user.package.dir=" + installed,
                "--module-path", pkgmgmt + java.io.File.pathSeparator + probe, "-m", "probe/probe.Main")
                .redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        assertEquals(0, p.waitFor(), out);
        assertTrue(out.contains("boot has jdk.dynalink: false"), out);
        assertTrue(out.contains("scripted with GET"), out);
    }

    /** a plan the planner would never make (two modules with Java package alpha): the JVM's error names both packages */
    @Test
    void jvmRejectionNamesThePackages() {
        LoadPlan bad = new LoadPlan(List.of(), List.of(
                new LoadPlan.PlannedModule("beast.alpha", Version.of("1.0"), alpha, "Alpha", ""),
                new LoadPlan.PlannedModule("commons.foo", Version.of("1.0"), foo1, "Alpha", ""),
                new LoadPlan.PlannedModule("beast.gamma", Version.of("1.0"), gamma, "Gamma", "")),
                List.of(), List.of(), List.of(), List.of(), List.of());
        Loader.LoadException e = assertThrows(Loader.LoadException.class, () -> Loader.load(bad));
        assertTrue(e.getMessage().contains("Alpha") && e.getMessage().contains("Gamma"), e.getMessage());
    }

    /** existing BEAST code finds the loaded classes through BEASTClassLoader */
    @Test
    void bridgeToBEASTClassLoader() throws Exception {
        packageFolder("Alpha", "1.0", "", alpha, foo1);
        packageFolder("Beta", "1.0", "<depends on='Alpha' atleast='1.0'/>", beta);
        LoadedPackages loaded = planAndLoad();
        loaded.registerWithBEASTClassLoader();
        Class<?> c = BEASTClassLoader.forName("beta.Beta");
        assertEquals(loaded.loadClass("beta.Beta"), c);
    }

    // ---- compiling test jars ----

    private static Path plainJar(String name, Map<String, String> sources) throws IOException {
        return jar(name, compile(name, sources, List.of(), false));
    }

    private static Path moduleJar(String name, List<Path> modulePath, Map<String, String> sources) throws IOException {
        return jar(name, compile(name, sources, modulePath, true));
    }

    private static Path compile(String name, Map<String, String> sources, List<Path> modulePath, boolean modular)
            throws IOException {
        Path src = Files.createDirectories(built.resolve("src").resolve(name));
        Path classes = Files.createDirectories(built.resolve("classes").resolve(name));
        Path javac = Path.of(System.getProperty("java.home"), "bin", "javac");
        List<String> args = new ArrayList<>(List.of(javac.toString(), "-d", classes.toString(), "--release", "25"));
        if (modular && !modulePath.isEmpty()) {
            args.add("--module-path");
            args.add(String.join(java.io.File.pathSeparator, modulePath.stream().map(Path::toString).toList()));
        }
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Path f = src.resolve(e.getKey());
            Files.createDirectories(f.getParent());
            Files.writeString(f, e.getValue());
            args.add(f.toString());
        }
        Process p = new ProcessBuilder(args).redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes());
        try {
            if (p.waitFor() != 0) {
                throw new IOException("javac failed for " + name + ":\n" + output);
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        return classes;
    }

    private static Path jar(String name, Path classes) throws IOException {
        Path jar = built.resolve("jars").resolve(name);
        Files.createDirectories(jar.getParent());
        try (OutputStream out = Files.newOutputStream(jar); JarOutputStream j = new JarOutputStream(out);
             Stream<Path> files = Files.walk(classes)) {
            for (Path f : files.filter(Files::isRegularFile).toList()) {
                j.putNextEntry(new JarEntry(classes.relativize(f).toString().replace('\\', '/')));
                Files.copy(f, j);
                j.closeEntry();
            }
        }
        return jar;
    }
}
