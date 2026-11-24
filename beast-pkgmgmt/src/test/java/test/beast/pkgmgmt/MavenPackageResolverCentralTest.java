package test.beast.pkgmgmt;

import beast.pkgmgmt.MavenPackageResolver;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resolves a real artifact from Maven Central (#318). It needs the network, so it is
 * tagged "network" and left out of the default build; run it with
 * {@code mvn test -Pslow-tests} or {@code -Dsurefire.excludedGroups=}.
 * When Central refuses (429 Too Many Requests, 503) or cannot be reached, the test
 * is skipped, not failed: that says nothing about BEAST.
 * The same resolver logic is tested offline in {@code beast.pkgmgmt.MavenPackageResolverTest}.
 */
@Tag("network")
class MavenPackageResolverCentralTest {

    @TempDir
    Path tempRepo;

    @Test
    void resolveWellKnownArtifact() throws Exception {
        // javax.inject:1 is tiny (6KB), has no parent POM and no dependencies,
        // so resolution only needs a handful of HTTP requests.
        List<Path> jars = resolveOrSkip("javax.inject", "javax.inject", "1");

        assertTrue(jars.stream().anyMatch(p -> p.getFileName().toString().startsWith("javax.inject-")),
                "Should contain the javax.inject JAR");
        for (Path jar : jars) {
            assertTrue(jar.toFile().exists(), "JAR should exist: " + jar);
            assertTrue(jar.toString().endsWith(".jar"), "Should be a .jar file: " + jar);
            assertFalse(jar.getFileName().toString().startsWith("java."),
                    "Should not include boot-layer JARs: " + jar);
        }
    }

    private List<Path> resolveOrSkip(String groupId, String artifactId, String version) throws Exception {
        try {
            return new MavenPackageResolver(tempRepo).resolve(groupId, artifactId, version);
        } catch (MavenPackageResolver.UnreachableException e) {
            // Central refused (429, 503) or could not be reached: says nothing about BEAST
            Assumptions.assumeTrue(false, e.getMessage());
            throw e;
        }
    }
}
