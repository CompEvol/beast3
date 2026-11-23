package beast.pkgmgmt;

import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.transport.http.HttpTransporterException;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.transfer.MetadataTransferException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resolves Maven coordinates (groupId:artifactId:version) to local JAR paths
 * using the Apache Maven Resolver library.  Downloaded artifacts are cached in
 * a local repository directory (typically {@code ~/.beast/2.8/maven-repo/}).
 * <p>
 * BEAST's own core and JavaFX are excluded from resolution: they always come
 * from the running BEAST (#298). Which of the resolved jars are loaded is
 * decided by {@link beast.pkgmgmt.plan.Planner}.
 */
public class MavenPackageResolver {

    private static final RemoteRepository MAVEN_CENTRAL =
            new RemoteRepository.Builder("central", "default",
                    "https://repo.maven.apache.org/maven2/").build();

    /** what BEAST provides itself, so never resolved from Maven (#298) */
    private static final List<Exclusion> PROVIDED_BY_BEAST = List.of(
            new Exclusion("io.github.compevol", "beast-base", "*", "*"),
            new Exclusion("io.github.compevol", "beast-fx", "*", "*"),
            new Exclusion("io.github.compevol", "beast-pkgmgmt", "*", "*"),
            new Exclusion("org.openjfx", "*", "*", "*"));

    private final Path localRepoDir;
    private final List<RemoteRepository> repositories;

    /**
     * @param localRepoDir path to the local Maven repository cache,
     *                     e.g. {@code ~/.beast/2.8/maven-repo/}
     */
    public MavenPackageResolver(Path localRepoDir) {
        this(localRepoDir, List.of());
    }

    /**
     * @param localRepoDir       path to the local Maven repository cache
     * @param extraRepositories  additional Maven repositories to search
     *                           (Maven Central is always included)
     */
    public MavenPackageResolver(Path localRepoDir, List<RemoteRepository> extraRepositories) {
        this(localRepoDir, extraRepositories, true);
    }

    /**
     * @param includeMavenCentral false to search only {@code repositories}, so tests can
     *                            resolve from a local {@code file://} repository without
     *                            the network (#318)
     */
    MavenPackageResolver(Path localRepoDir, List<RemoteRepository> repositories,
                         boolean includeMavenCentral) {
        this.localRepoDir = localRepoDir;
        this.repositories = new ArrayList<>();
        if (includeMavenCentral) {
            this.repositories.add(MAVEN_CENTRAL);
        }
        this.repositories.addAll(repositories);
    }

    /**
     * Resolve a Maven coordinate and return paths to all required JARs
     * (the artifact + its transitive compile/runtime dependencies), without
     * BEAST's own core and JavaFX.
     */
    public List<Path> resolve(String groupId, String artifactId, String version)
            throws Exception {

        RepositorySystem system = new RepositorySystemSupplier().getRepositorySystem();
        ProgressListener progress = new ProgressListener();

        try (CloseableSession session = createSession(system, progress)) {
            Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version);
            CollectRequest collectRequest = new CollectRequest();
            // BEAST's own core and JavaFX always come from the running BEAST, never from Maven
            // (#298): excluded here, which also skips everything they depend on. The core
            // versions a package works with are read from its POM and checked by the planner.
            collectRequest.setRoot(new Dependency(artifact, "compile", false, PROVIDED_BY_BEAST));
            for (RemoteRepository repo : repositories) {
                collectRequest.addRepository(repo);
            }

            DependencyRequest depRequest = new DependencyRequest(collectRequest,
                    DependencyFilterUtils.classpathFilter("compile", "runtime"));

            DependencyResult result;
            try {
                result = system.resolveDependencies(session, depRequest);
            } catch (DependencyResolutionException e) {
                String problem = unreachable(e);
                if (problem != null) {
                    throw new UnreachableException("Could not download " + groupId + ":" + artifactId
                            + ":" + version + ": " + problem, e);
                }
                throw e;
            }
            List<Path> jars = new ArrayList<>();
            for (ArtifactResult ar : result.getArtifactResults()) {
                Path jarPath = ar.getArtifact().getPath();
                if (jarPath != null) {
                    jars.add(jarPath);
                }
            }
            return jars;
        }
    }

    /**
     * Resolve from a compact coordinate string "groupId:artifactId:version".
     */
    public List<Path> resolve(String coordinate) throws Exception {
        String[] parts = coordinate.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Expected format groupId:artifactId:version, got: " + coordinate);
        }
        return resolve(parts[0], parts[1], parts[2]);
    }

    private CloseableSession createSession(RepositorySystem system, ProgressListener listener) {
        // Maven's own session defaults, among them conflict resolution: one version of each
        // groupId:artifactId (nearest wins), as Maven builds the package (guava 14 and 33 both
        // reached lphybeast-mm without it)
        return new SessionBuilderSupplier(system).get()
                .withLocalRepositoryBaseDirectories(localRepoDir)
                .setSystemProperties(System.getProperties())
                .setTransferListener(listener)
                // Prune test, provided, and optional dependencies during
                // collection so we don't walk their entire POM tree.
                // Without this, an artifact like gson pulls 1800+ POMs
                // via test deps (truth → guava → gwt → ...).
                .setDependencySelector(new AndDependencySelector(
                        new ScopeDependencySelector("test", "provided"),
                        new OptionalDependencySelector(),
                        new ExclusionDependencySelector()))
                // Only resolve from our explicitly configured repositories,
                // not from repositories declared in artifact POMs/parent POMs
                // (e.g. Sonatype snapshots).  This prevents hangs caused by
                // unreachable or misbehaving third-party repositories.
                .setIgnoreArtifactDescriptorRepositories(true)
                // Use depth-first collector to avoid thread-pool deadlocks
                .setConfigProperty("aether.dependencyCollector.impl", "df")
                // Timeout for each request (milliseconds); a stalled download then fails, and
                // is reported as unreachable, after 30 s, not the resolver's default 30 min
                .setConfigProperty(ConfigurationProperties.REQUEST_TIMEOUT, 30_000)
                .build();
    }

    /**
     * Thrown when a Maven repository cannot be reached or refuses requests, so a package
     * could not be downloaded (#318). Its message says what happened and what to do.
     */
    public static class UnreachableException extends IOException {
        UnreachableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * What went wrong if {@code e} was caused by the network or by a repository refusing
     * requests, else null (e.g. the package does not exist). The repository named is the
     * one of the nearest transfer exception enclosing the problem, so with several
     * repositories the message names the one that actually failed.
     */
    static String unreachable(Throwable e) {
        record Node(Throwable t, String repo) {}
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Node> todo = new ArrayDeque<>();
        todo.push(new Node(e, "the Maven repository"));
        Node problem = null;
        boolean centralAnswered = false;
        while (!todo.isEmpty()) {
            Node n = todo.pop();
            Throwable t = n.t();
            if (!seen.add(t)) continue;
            String repo = n.repo();
            if (t instanceof ArtifactTransferException a && a.getRepository() instanceof RemoteRepository r) {
                repo = r.getUrl();
            } else if (t instanceof MetadataTransferException m && m.getRepository() instanceof RemoteRepository r) {
                repo = r.getUrl();
            }
            if (t instanceof ArtifactNotFoundException && MAVEN_CENTRAL.getUrl().equals(repo)) {
                centralAnswered = true;
            }
            if (problem == null && describe(t, false) != null) {
                problem = new Node(t, repo);
            }
            if (t.getCause() != null) todo.push(new Node(t.getCause(), repo));
            for (Throwable s : t.getSuppressed()) todo.push(new Node(s, repo));
        }
        if (problem == null) {
            return null;
        }
        // Central answered, so the internet works: the problem is a repository the user added
        boolean addedByUser = centralAnswered && !MAVEN_CENTRAL.getUrl().equals(problem.repo());
        return describe(problem.t(), addedByUser).replace("{repo}", problem.repo());
    }

    /** said of a repository the user added, when Central answered but that repository did not */
    private static final String ADDED_BY_USER = "This Maven repository was added by you with "
            + "packagemanager -addMavenRepository. If its address is wrong, remove it with: "
            + "packagemanager -delMavenRepository {repo}";

    private static String describe(Throwable t, boolean addedByUser) {
        if (t instanceof HttpTransporterException h) {
            return switch (h.getStatusCode()) {
                case 429 -> "{repo} is refusing requests for now (HTTP 429 Too Many Requests). "
                        + "Please try again in a few minutes.";
                case 503 -> "{repo} is temporarily unavailable (HTTP 503). "
                        + "Please try again in a few minutes.";
                default -> null;
            };
        }
        if (t instanceof SocketTimeoutException) {
            return "timed out connecting to {repo}. "
                    + "Please check your internet connection and proxy settings, and try again.";
        }
        if (t instanceof UnknownHostException) {
            return addedByUser
                    ? "it is not on Maven Central, and {repo} does not exist. " + ADDED_BY_USER
                    : "cannot find the host of {repo} (" + t.getMessage() + "). "
                            + "Please check your internet connection and proxy settings.";
        }
        if (t instanceof SocketException) {
            return addedByUser
                    ? "it is not on Maven Central, and {repo} cannot be reached (" + t.getMessage() + "). "
                            + ADDED_BY_USER
                    : "cannot connect to {repo} (" + t.getMessage() + "). "
                            + "Please check your internet connection and proxy settings.";
        }
        return null;
    }

    /** Prints download progress to stderr and tracks transfer count. */
    private static class ProgressListener extends AbstractTransferListener {
        final AtomicInteger transferCount = new AtomicInteger();

        @Override
        public void transferStarted(TransferEvent event) {
            int count = transferCount.incrementAndGet();
            TransferResource r = event.getResource();
            String name = r.getResourceName();
            // shorten "org/foo/bar/baz-1.0.jar" to "baz-1.0.jar"
            int slash = name.lastIndexOf('/');
            if (slash >= 0) name = name.substring(slash + 1);
            long size = r.getContentLength();
            if (size > 0) {
                System.err.printf("  [%d] Downloading %s (%d KB)%n", count, name, size / 1024);
            } else {
                System.err.printf("  [%d] Downloading %s%n", count, name);
            }
        }

        @Override
        public void transferFailed(TransferEvent event) {
            TransferResource r = event.getResource();
            String name = r.getResourceName();
            int slash = name.lastIndexOf('/');
            if (slash >= 0) name = name.substring(slash + 1);
            System.err.printf("  Failed: %s (%s)%n", name, event.getException().getMessage());
        }
    }

    /**
     * Create a {@link RemoteRepository} from a URL string.
     */
    public static RemoteRepository remoteRepository(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
    }
}
