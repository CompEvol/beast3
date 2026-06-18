package beast.pkgmgmt;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that PackageManager.createAndRegisterModuleLayer() can resolve a
 * package whose module dependency lives in another plugin layer (not the boot
 * layer). This is the cross-layer behaviour required before beast.base/beast.fx
 * can be moved off the boot module path. Under the previous boot-only-parent
 * code the dependent package would fail to resolve.
 *
 * Two minimal modular jars are generated on the fly: mptestplugin requires
 * mptestbase. mptestbase is loaded into its own layer first, then mptestplugin
 * must resolve against that layer.
 */
public class ModuleLayerCrossDependencyTest {

    @Test
    public void dependentPackageResolvesAgainstAnotherPluginLayer() throws Exception {
        ToolProvider javac = ToolProvider.findFirst("javac").orElse(null);
        Assumptions.assumeTrue(javac != null, "no javac tool available - skipping");

        Path work = Files.createTempDirectory("mp-layer-test");

        Path baseJar = buildModuleJar(javac, work, "mptestbase", null,
                "module mptestbase { exports mptestbase; }",
                "mptestbase", "Base",
                "package mptestbase; public class Base { public static String hello() { return \"base\"; } }");

        Path pluginJar = buildModuleJar(javac, work, "mptestplugin", baseJar,
                "module mptestplugin { requires mptestbase; exports mptestplugin; }",
                "mptestplugin", "Plugin",
                "package mptestplugin; public class Plugin { public static String go() { return mptestbase.Base.hello() + \"+plugin\"; } }");

        // Load the dependency into its own plugin layer first.
        boolean baseLoaded = PackageManager.createAndRegisterModuleLayer(
                List.of(baseJar), null, "mptestbase", "1.0", "mptestbase");
        assertTrue(baseLoaded, "dependency module mptestbase should load");

        // Now load the dependent: its 'requires mptestbase' can only be satisfied
        // by the previously-registered plugin layer, not the boot layer.
        boolean pluginLoaded = PackageManager.createAndRegisterModuleLayer(
                List.of(pluginJar), null, "mptestplugin", "1.0", "mptestplugin");
        assertTrue(pluginLoaded, "dependent module mptestplugin should resolve against the mptestbase layer");

        // And it must actually link at runtime: Plugin.go() calls Base.hello().
        Class<?> pluginClass = BEASTClassLoader.forName("mptestplugin.Plugin");
        String result = (String) pluginClass.getMethod("go").invoke(null);
        assertEquals("base+plugin", result, "cross-layer call should execute");
    }

    /**
     * Regression test for the BEAUti "ClassNotFoundException: beastfx.app.inputeditor.*"
     * failure. A package's service providers are listed in its version.xml. If a
     * version.xml scan registers a provider (via addServices → resolveLoaderFor)
     * before the owning package's plugin ModuleLayer is registered, the provider
     * is mapped to the fallback system loader, which cannot load it. registerPluginLayer()
     * uses putIfAbsent and so cannot replace that stale mapping. forName() must still
     * resolve the class by falling through to the plugin layers.
     */
    @Test
    public void forNameHealsStaleFallbackLoaderMapping() throws Exception {
        ToolProvider javac = ToolProvider.findFirst("javac").orElse(null);
        Assumptions.assumeTrue(javac != null, "no javac tool available - skipping");

        Path work = Files.createTempDirectory("stale-loader-test");

        // A modular jar exporting a provider class, mimicking beast.fx's input editors.
        Path editorJar = buildModuleJar(javac, work, "fxstaletest", null,
                "module fxstaletest { exports fxstaletest; }",
                "fxstaletest", "Editor",
                "package fxstaletest; public class Editor { }");

        // 1. Simulate the version.xml scan that runs before the plugin layer exists:
        //    the provider is mapped to the fallback system loader (cannot load it).
        Map<String, Set<String>> services = Map.of(
                "fxstaletest.InputEditor", Set.of("fxstaletest.Editor"));
        BEASTClassLoader.classLoader.addServices("FxStaleTest", services);

        // 2. Register the package's real plugin layer. putIfAbsent leaves the
        //    stale fallback mapping from step 1 in place.
        boolean loaded = PackageManager.createAndRegisterModuleLayer(
                List.of(editorJar), null, "FxStaleTest", "1.0", "FxStaleTest");
        assertTrue(loaded, "provider module should load into a plugin layer");

        // 3. Despite the stale mapping, forName must resolve the class via the
        //    plugin layer (previously threw ClassNotFoundException here).
        Class<?> editor = BEASTClassLoader.forName("fxstaletest.Editor");
        assertNotNull(editor);
        assertEquals("fxstaletest.Editor", editor.getName());
    }

    /** Compile a single-package module and pack it into a jar; return the jar path. */
    private static Path buildModuleJar(ToolProvider javac, Path work, String moduleName,
                                       Path modulePathJar, String moduleInfo,
                                       String pkg, String className, String classSource) throws IOException {
        Path src = work.resolve(moduleName + "-src");
        Files.createDirectories(src.resolve(pkg));
        Files.writeString(src.resolve("module-info.java"), moduleInfo);
        Files.writeString(src.resolve(pkg).resolve(className + ".java"), classSource);

        Path classes = work.resolve(moduleName + "-classes");
        Files.createDirectories(classes);

        List<String> args = new ArrayList<>(List.of("-d", classes.toString()));
        if (modulePathJar != null) {
            args.add("--module-path");
            args.add(modulePathJar.toString());
        }
        try (Stream<Path> walk = Files.walk(src)) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> args.add(p.toString()));
        }
        int rc = javac.run(System.out, System.err, args.toArray(new String[0]));
        assertEquals(0, rc, "compilation of " + moduleName + " should succeed");

        Path jar = work.resolve(moduleName + ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar));
             Stream<Path> walk = Files.walk(classes)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                try {
                    jos.putNextEntry(new JarEntry(classes.relativize(p).toString().replace('\\', '/')));
                    Files.copy(p, jos);
                    jos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        return jar;
    }
}
