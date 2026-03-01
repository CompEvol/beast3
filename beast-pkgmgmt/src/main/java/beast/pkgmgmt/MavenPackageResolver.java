package beast.pkgmgmt;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Resolves Maven coordinates (groupId:artifactId:version) to local JAR paths
 * using the Apache Maven Resolver library.  Downloaded artifacts are cached in
 * a local repository directory (typically {@code ~/.beast/2.8/maven-repo/}).
 * <p>
 * JARs whose JPMS module name already appears in the boot {@link ModuleLayer}
 * (i.e. beast-base, beast-pkgmgmt, commons-math, etc.) are automatically
 * excluded from the result so that duplicate-module errors are avoided when
 * creating plugin module layers.
 */
public class MavenPackageResolver {

    private static final RemoteRepository MAVEN_CENTRAL =
            new RemoteRepository.Builder("central", "default",
                    "https://repo.maven.apache.org/maven2/").build();

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
     * (the artifact + its transitive compile/runtime dependencies), excluding
     * JARs whose module name is already present in the boot module layer or
     * any previously loaded plugin layer.
     */
    public List<Path> resolve(String groupId, String artifactId, String version)
            throws Exception {

        RepositorySystem system = new RepositorySystemSupplier().getRepositorySystem();
        ProgressListener progress = new ProgressListener();

        try (CloseableSession session = createSession(system, progress)) {
            Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version);
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(artifact, "compile"));
            for (RemoteRepository repo : repositories) {
                collectRequest.addRepository(repo);
            }

            DependencyRequest depRequest = new DependencyRequest(collectRequest,
                    DependencyFilterUtils.classpathFilter("compile", "runtime"));

            DependencyResult result = system.resolveDependencies(session, depRequest);

            // Collect module names already loaded in boot + plugin layers
            Set<String> loadedModuleNames = ModuleLayer.boot().modules().stream()
                    .map(Module::getName)
                    .collect(Collectors.toSet());
            for (ModuleLayer layer : BEASTClassLoader.getPluginLayers()) {
                layer.modules().stream()
                        .map(Module::getName)
                        .forEach(loadedModuleNames::add);
            }

            List<Path> jars = new ArrayList<>();
            for (ArtifactResult ar : result.getArtifactResults()) {
                Path jarPath = ar.getArtifact().getPath();
                if (jarPath == null) continue;

                // Check if this JAR's module is already loaded
                String moduleName = getModuleName(jarPath);
                if (moduleName != null && loadedModuleNames.contains(moduleName)) {
                    continue;
                }
                jars.add(jarPath);
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
        return system.createSessionBuilder()
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

    /**
     * Attempt to determine the JPMS module name for a JAR file by examining
     * its module descriptor or Automatic-Module-Name manifest entry.
     * Returns {@code null} if the name cannot be determined.
     */
    private static String getModuleName(Path jarPath) {
        try {
            java.lang.module.ModuleFinder finder =
                    java.lang.module.ModuleFinder.of(jarPath);
            return finder.findAll().stream()
                    .map(ref -> ref.descriptor().name())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
