package beast.pkgmgmt;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.http.HttpTransporterException;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MavenPackageResolver} that never touch the network (#318): they
 * resolve from a small Maven repository written to a temporary folder and read
 * through a {@code file://} URL, with Maven Central left out.
 * Resolution from Maven Central itself is in
 * {@code test.beast.pkgmgmt.MavenPackageResolverCentralTest} (tag "network").
 */
class MavenPackageResolverTest {

    @TempDir
    Path remoteDir;

    @TempDir
    Path localRepo;

    private MavenPackageResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        // test.pkg:main:1.0 needs test.pkg:dep:1.0. The other dependencies are not in the
        // repository, so resolution fails if the resolver tries to fetch any of them:
        // BEAST's own core and JavaFX (#298), a test-scope dependency, and an optional
        // dependency of dep (as in Maven, optional dependencies of dependencies are left out).
        writeArtifact("test.pkg", "main", "1.0", """
                <dependency><groupId>test.pkg</groupId><artifactId>dep</artifactId><version>1.0</version></dependency>
                <dependency><groupId>io.github.compevol</groupId><artifactId>beast-base</artifactId><version>2.8.0</version></dependency>
                <dependency><groupId>org.openjfx</groupId><artifactId>javafx-controls</artifactId><version>25</version></dependency>
                <dependency><groupId>test.pkg</groupId><artifactId>testonly</artifactId><version>1.0</version><scope>test</scope></dependency>
                """);
        writeArtifact("test.pkg", "dep", "1.0", """
                <dependency><groupId>test.pkg</groupId><artifactId>optional</artifactId><version>1.0</version><optional>true</optional></dependency>
                """);

        RemoteRepository repo = MavenPackageResolver.remoteRepository("test", remoteDir.toUri().toString());
        resolver = new MavenPackageResolver(localRepo, List.of(repo), false);
    }

    @Test
    void resolvesArtifactAndItsDependency() throws Exception {
        List<Path> jars = resolver.resolve("test.pkg", "main", "1.0");

        Set<String> names = jars.stream().map(p -> p.getFileName().toString()).collect(Collectors.toSet());
        assertEquals(Set.of("main-1.0.jar", "dep-1.0.jar"), names,
                "BEAST core, JavaFX, test and transitive optional dependencies must be left out");
        for (Path jar : jars) {
            assertTrue(Files.isRegularFile(jar), "JAR should exist: " + jar);
            assertTrue(jar.startsWith(localRepo), "JAR should be cached in the local repository: " + jar);
        }
    }

    @Test
    void resolvesFromCompactCoordinate() throws Exception {
        assertEquals(2, resolver.resolve("test.pkg:main:1.0").size());
    }

    @Test
    void resolveBadCoordinateThrows() {
        assertThrows(Exception.class,
                () -> resolver.resolve("com.nonexistent", "does-not-exist", "99.99.99"));
    }

    @Test
    void resolveInvalidFormatThrows() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("bad-format"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("only:two"));
    }

    @Test
    void unreachableRepositoryIsReported() {
        // nothing listens on port 1, so the connection is refused at once, without the network
        RemoteRepository closed = MavenPackageResolver.remoteRepository("closed", "http://127.0.0.1:1/");
        MavenPackageResolver offline = new MavenPackageResolver(localRepo, List.of(closed), false);

        MavenPackageResolver.UnreachableException e = assertThrows(MavenPackageResolver.UnreachableException.class,
                () -> offline.resolve("test.pkg", "main", "1.0"));
        assertTrue(e.getMessage().startsWith("Could not download test.pkg:main:1.0: cannot connect to http://127.0.0.1:1/"),
                e.getMessage());
        assertTrue(e.getMessage().contains("check your internet connection"), e.getMessage());
    }

    @Test
    void missingPackageIsNotReportedAsUnreachable() {
        Exception e = assertThrows(Exception.class,
                () -> resolver.resolve("com.nonexistent", "does-not-exist", "99.99.99"));
        assertFalse(e instanceof MavenPackageResolver.UnreachableException, e.getMessage());
    }

    @Test
    void rateLimitAndOutageAreExplained() {
        // the resolver wraps the HTTP status in several layers of exceptions
        Exception tooMany = new Exception("Failed to read artifact descriptor",
                new Exception("Could not transfer artifact", new HttpTransporterException(429)));
        String message = MavenPackageResolver.unreachable(tooMany);
        assertNotNull(message);
        assertTrue(message.contains("HTTP 429 Too Many Requests") && message.contains("try again in a few minutes"),
                message);

        Exception suppressed = new Exception("Failed to collect dependencies");
        suppressed.addSuppressed(new HttpTransporterException(503));
        message = MavenPackageResolver.unreachable(suppressed);
        assertNotNull(message);
        assertTrue(message.contains("temporarily unavailable (HTTP 503)"), message);

        assertNull(MavenPackageResolver.unreachable(new Exception("x", new HttpTransporterException(404))),
                "404 means the package is not there, not that the repository is unreachable");
        assertNotNull(MavenPackageResolver.unreachable(new Exception(new SocketTimeoutException("Read timed out"))));
        assertNotNull(MavenPackageResolver.unreachable(new Exception(new UnknownHostException("repo.maven.apache.org"))));
    }

    @Test
    void unreachableNamesTheRepositoryThatFailed() {
        // one repository answers 404, the other's host is unknown: the message must name the
        // second, whichever order the resolver reports them in
        Artifact art = new DefaultArtifact("g:a:1");
        RemoteRepository central = MavenPackageResolver.remoteRepository("central", "https://central.example/");
        RemoteRepository extra = MavenPackageResolver.remoteRepository("extra", "https://no-such-host.invalid/maven2/");
        Exception notFound = new ArtifactTransferException(art, central, "not found", new HttpTransporterException(404));
        Exception noHost = new ArtifactTransferException(art, extra, "no host",
                new UnknownHostException("no-such-host.invalid"));
        for (List<Exception> order : List.of(List.of(notFound, noHost), List.of(noHost, notFound))) {
            Exception top = new Exception("Failed to collect dependencies");
            order.forEach(top::addSuppressed);
            String message = MavenPackageResolver.unreachable(top);
            assertNotNull(message);
            assertTrue(message.startsWith("cannot find the host of https://no-such-host.invalid/maven2/"), message);
        }
    }

    @Test
    void unknownHostAmongTwoRepositoriesIsReported() {
        // the file:// repository does not have the package (not found); the other host
        // cannot be found (.invalid never resolves)
        RemoteRepository good = MavenPackageResolver.remoteRepository("test", remoteDir.toUri().toString());
        RemoteRepository bad = MavenPackageResolver.remoteRepository("extra", "http://no-such-host.invalid/maven2/");
        MavenPackageResolver two = new MavenPackageResolver(localRepo, List.of(good, bad), false);

        MavenPackageResolver.UnreachableException e = assertThrows(MavenPackageResolver.UnreachableException.class,
                () -> two.resolve("test.pkg", "missing", "1.0"));
        assertTrue(e.getMessage().contains("cannot find the host of http://no-such-host.invalid/maven2/"),
                e.getMessage());
    }

    /** writes {@code groupId:artifactId:version} (a POM and an empty jar) to the test repository */
    private void writeArtifact(String groupId, String artifactId, String version,
                               String dependencies) throws IOException {
        Path dir = remoteDir.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
        Files.createDirectories(dir);
        String base = artifactId + "-" + version;
        Files.writeString(dir.resolve(base + ".pom"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                  <dependencies>
                %s  </dependencies>
                </project>
                """.formatted(groupId, artifactId, version, dependencies));
        try (OutputStream out = Files.newOutputStream(dir.resolve(base + ".jar"));
             JarOutputStream jar = new JarOutputStream(out)) {
            // an empty jar is enough
        }
    }
}
