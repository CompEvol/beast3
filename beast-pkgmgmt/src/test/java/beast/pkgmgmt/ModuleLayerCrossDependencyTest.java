package beast.pkgmgmt;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
