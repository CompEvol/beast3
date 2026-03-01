package test.beast.pkgmgmt;

import beast.pkgmgmt.MavenPackageResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MavenPackageResolver}.
 * Some tests require network access to Maven Central.
 */
class MavenPackageResolverTest {

    @TempDir
    Path tempRepo;

    @Test

    void resolveWellKnownArtifact() throws Exception {
        // javax.inject:1 is tiny (6KB), has no parent POM and no dependencies,
        // so resolution only needs a handful of HTTP requests.
        MavenPackageResolver resolver = new MavenPackageResolver(tempRepo);
        List<Path> jars = resolver.resolve("javax.inject", "javax.inject", "1");

        assertFalse(jars.isEmpty(), "Should resolve at least one JAR");

        // The main artifact JAR should be present
        boolean hasMainJar = jars.stream()
                .anyMatch(p -> p.getFileName().toString().startsWith("javax.inject-"));
        assertTrue(hasMainJar, "Should contain the javax.inject JAR");

        // All paths should exist and be JAR files
        for (Path jar : jars) {
            assertTrue(jar.toFile().exists(), "JAR should exist: " + jar);
            assertTrue(jar.toString().endsWith(".jar"), "Should be a .jar file: " + jar);
        }
    }

    @Test

    void resolveFilterBootLayerModules() throws Exception {
        MavenPackageResolver resolver = new MavenPackageResolver(tempRepo);
        List<Path> jars = resolver.resolve("javax.inject", "javax.inject", "1");

        for (Path jar : jars) {
            String name = jar.getFileName().toString();
            // None of the returned JARs should be core JDK modules
            assertFalse(name.startsWith("java."),
                    "Should not include boot-layer JARs: " + name);
        }
    }

    @Test

    void resolveBadCoordinateThrows() {
        MavenPackageResolver resolver = new MavenPackageResolver(tempRepo);
        assertThrows(Exception.class,
                () -> resolver.resolve("com.nonexistent", "does-not-exist", "99.99.99"));
    }

    @Test
    void resolveInvalidFormatThrows() {
        MavenPackageResolver resolver = new MavenPackageResolver(tempRepo);
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve("bad-format"));
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve("only:two"));
    }
}
