package beast.pkgmgmt;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.http.HttpTransporterException;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
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

    private static final RemoteRepository CENTRAL =
            MavenPackageResolver.remoteRepository("central", "https://repo.maven.apache.org/maven2/");

    /** the resolver's failure when Central does not have the package and {@code added} fails with {@code cause} */
    private static List<Exception> notOnCentralAnd(RemoteRepository added, Exception cause) {
        Artifact art = new DefaultArtifact("io.github.compevol:no-such-pkg:9.9.9");
        return List.of(new ArtifactNotFoundException(art, CENTRAL),
                new ArtifactTransferException(art, added, "transfer failed", cause));
    }

    /**
     * Central answered that it does not have the package, so the internet works: the message
     * names the repository the user added (not Central, #321 review), says it does not exist,
     * and how to remove it, whichever order the resolver reports them in
     */
    @Test
    void unreachableNamesTheRepositoryThatFailed() {
        RemoteRepository added = MavenPackageResolver.remoteRepository("extra-0", "https://no-such-host.invalid/maven2/");
        List<Exception> failures = notOnCentralAnd(added, new UnknownHostException("no-such-host.invalid"));
        for (List<Exception> order : List.of(failures, failures.reversed())) {
            Exception top = new Exception("Failed to collect dependencies");
            order.forEach(top::addSuppressed);
            assertEquals("it is not on Maven Central, and https://no-such-host.invalid/maven2/ does not exist. "
                    + "This Maven repository was added by you with packagemanager -addMavenRepository. "
                    + "If its address is wrong, remove it with: "
                    + "packagemanager -delMavenRepository https://no-such-host.invalid/maven2/",
                    MavenPackageResolver.unreachable(top));
        }
    }

    /** the same when the repository the user added refuses the connection */
    @Test
    void anAddedRepositoryThatRefusesIsNamedAsTheUsers() {
        RemoteRepository added = MavenPackageResolver.remoteRepository("extra-0", "https://no-such-host.invalid/maven2/");
        Exception top = new Exception("Failed to collect dependencies");
        notOnCentralAnd(added, new ConnectException("Connection refused")).forEach(top::addSuppressed);

        String message = MavenPackageResolver.unreachable(top);
        assertTrue(message.startsWith("it is not on Maven Central, and https://no-such-host.invalid/maven2/ "
                + "cannot be reached (Connection refused). This Maven repository was added by you"), message);
    }

    /** if Central cannot be reached either, the internet is the likely cause: the message says so */
    @Test
    void withoutAnAnswerFromCentralTheInternetIsSuspected() {
        Artifact art = new DefaultArtifact("io.github.compevol:ccd:1.2.2");
        Exception top = new Exception("Failed to collect dependencies");
        top.addSuppressed(new ArtifactTransferException(art, CENTRAL, "no host",
                new UnknownHostException("repo.maven.apache.org")));
        top.addSuppressed(new ArtifactTransferException(art,
                MavenPackageResolver.remoteRepository("extra-0", "https://no-such-host.invalid/maven2/"), "no host",
                new UnknownHostException("no-such-host.invalid")));

        String message = MavenPackageResolver.unreachable(top);
        assertTrue(message.contains("check your internet connection"), message);
        assertFalse(message.contains("added by you"), message);
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
