package beast.pkgmgmt;

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
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
        this.localRepoDir = localRepoDir;
        this.repositories = new ArrayList<>();
        this.repositories.add(MAVEN_CENTRAL);
        this.repositories.addAll(extraRepositories);
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

            DependencyResult result = system.resolveDependencies(session, depRequest);
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
                // Set request timeout (milliseconds)
                .setConfigProperty("aether.connector.requestTimeout", 30_000)
                .build();
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
