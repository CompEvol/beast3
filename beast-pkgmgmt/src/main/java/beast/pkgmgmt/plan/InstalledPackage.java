package beast.pkgmgmt.plan;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One installed copy of a package, whatever its format. The same package can be installed
 * more than once (bundled and upgraded, ZIP and Maven, in two folders); the planner picks one.
 *
 * @param name      the package name, e.g. BEAST.base, SA, ccd
 * @param version   its version
 * @param location  where this copy is installed
 * @param depends   the packages it needs, including {@link Planner#LAUNCHER} for core packages
 * @param artifacts its jars: its own modules and the libraries it brings
 * @param services  service providers declared in its version.xml ({@code <service>}): still
 *                  honoured, but {@code provides} in module-info is what counts (decision 5)
 */
public record InstalledPackage(String name, Version version, Location location,
                               List<Dependency> depends, List<Artifact> artifacts,
                               Map<String, Set<String>> services) {

    public InstalledPackage(String name, Version version, Location location,
                            List<Dependency> depends, List<Artifact> artifacts) {
        this(name, version, location, depends, artifacts, Map.of());
    }

    /**
     * Where a copy is installed, in order of precedence when two copies of the same version
     * exist. {@code BOOT}: on the boot layer (developer mode, IDE), always used.
     * {@code OVERRIDE}: on {@code BEAST_PACKAGE_PATH}, used whatever its version.
     * {@code ARCHIVE}: {@code archive/<name>/<version>/}, installed with
     * {@code packagemanager -add NAME -version V} for {@code -strictversions}; used only when no
     * other copy is, or when an analysis pins that version. {@code AVAILABLE}: a version on
     * offer from a repository, when planning an install. The others are used newest first.
     */
    public enum Location { BOOT, OVERRIDE, USER, MAVEN, SYSTEM, BUNDLED, ARCHIVE, AVAILABLE }

    @Override
    public String toString() {
        return name + " " + version + " (" + location.name().toLowerCase() + ")";
    }
}
