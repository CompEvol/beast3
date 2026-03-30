package beast.pkgmgmt;

import java.util.*;

/**
 * Resolves package dependencies for BEAST package installations.
 * Given a set of packages to install, determines all transitive dependencies
 * that must also be installed and verifies that a consistent set of versions exists.
 */
public class DependencyResolver {

    /**
     * Exception thrown when an operation fails due to package dependency issues.
     */
    public static class DependencyResolutionException extends Exception {
        private static final long serialVersionUID = 1L;

        public DependencyResolutionException(String message) {
            super(message);
        }
    }

    /**
     * Populate the given map with versions of packages to install which satisfy
     * dependencies of those already present.
     *
     * @param packageMap        database of installed and available packages
     * @param packagesToInstall map to populate with package versions requiring installation
     * @throws DependencyResolutionException when no consistent set of dependent packages can be found
     */
    public void resolve(Map<String, Package> packageMap,
                        Map<Package, PackageVersion> packagesToInstall) throws DependencyResolutionException {

        Map<Package, PackageVersion> copy = new HashMap<>(packagesToInstall);

        for (Map.Entry<Package, PackageVersion> entry : copy.entrySet()) {
            resolve(packageMap, packagesToInstall, entry.getKey(), entry.getValue(),
                    new LinkedHashSet<>());
        }
    }

    private void resolve(Map<String, Package> packageMap,
                         Map<Package, PackageVersion> packagesToInstall,
                         Package rootPackage, PackageVersion rootPackageVersion,
                         Set<String> resolving) throws DependencyResolutionException {

        if (!resolving.add(rootPackage.getName()))
            throw new DependencyResolutionException("Circular dependency detected: "
                    + String.join(" -> ", resolving) + " -> " + rootPackage.getName());

        if (!rootPackage.getAvailableVersions().contains(rootPackageVersion))
            throw new IllegalArgumentException("Package version " + rootPackageVersion + " is not available.");

        Set<PackageDependency> dependencies = rootPackage.getDependencies(rootPackageVersion);
        for (PackageDependency dependency : dependencies) {
            if (!packageMap.containsKey(dependency.dependencyName))
                throw new DependencyResolutionException("Package " + rootPackage
                        + " depends on unknown package " + dependency.dependencyName);

            Package depPkg = packageMap.get(dependency.dependencyName);
            PackageVersion intendedVersion = packagesToInstall.get(depPkg);
            if (intendedVersion == null) {
                if (depPkg.isInstalled() && dependency.isMetBy(depPkg.getInstalledVersion()))
                    continue;
            } else {
                if (dependency.isMetBy(intendedVersion))
                    continue;
                else
                    throw new DependencyResolutionException("Package " + rootPackage + " depends on a different " +
                            "version of package " + dependency.dependencyName + " to that required by another package.");
            }

            boolean foundCompatible = false;
            for (PackageVersion depPkgVersion : depPkg.getAvailableVersions()) {
                if (dependency.isMetBy(depPkgVersion)) {
                    if (depPkg.isInstalled() && depPkgVersion.compareTo(depPkg.getInstalledVersion()) < 0)
                        continue; // No downgrading of installed versions

                    packagesToInstall.put(depPkg, depPkgVersion);

                    try {
                        resolve(packageMap, packagesToInstall, depPkg, depPkgVersion,
                                new LinkedHashSet<>(resolving));
                        foundCompatible = true;
                        break;
                    } catch (DependencyResolutionException ignored) { }

                    packagesToInstall.remove(depPkg);
                }
            }
            if (!foundCompatible)
                throw new DependencyResolutionException("Package " + rootPackage + " requires " + dependency + ", " +
                        "but no installable version of that package was found.");
        }
    }

    /**
     * Checks that dependencies of all installed packages are met.
     * Prints warnings for any broken dependencies.
     *
     * @param packageMap database of installed and available packages
     */
    public void checkInstalled(Map<String, Package> packageMap) {
        Map<PackageDependency, Package> dependencies = new HashMap<>();

        for (Package pkg : packageMap.values()) {
            if (!pkg.isInstalled())
                continue;
            for (PackageDependency dep : pkg.getInstalledVersionDependencies())
                dependencies.put(dep, pkg);
        }

        for (PackageDependency dep : dependencies.keySet()) {
            Package depPackage = packageMap.get(dep.dependencyName);
            Package requiredBy = dependencies.get(dep);
            if (depPackage == null) {
                System.err.println("WARNING: Package " + requiredBy.getName()
                        + " requires another package (" + dep.dependencyName + ") which is not available.\n" +
                        "Either uninstall " + requiredBy.getName() + " or ask the package maintainer for " +
                        "information about this dependency.");
            } else if (!depPackage.isInstalled()) {
                System.err.println("WARNING: Package " + requiredBy.getName()
                        + " requires another package (" + dep.dependencyName + ") which is not installed.\n" +
                        "Either uninstall " + requiredBy.getName() + " or install the " + dep.dependencyName + " package.");
            } else if (!dep.isMetBy(depPackage.getInstalledVersion())) {
                System.err.println("WARNING: Package " + requiredBy.getName()
                        + " requires another package " + dep
                        + " but the installed " + dep.dependencyName + " has version " + depPackage.getInstalledVersion() + ".\n" +
                        "Either uninstall " + requiredBy.getName() + " or install the correct version of " + dep.dependencyName + ".");
            }
        }
    }
}
