package beast.pkgmgmt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beast.pkgmgmt.plan.Dependency;
import beast.pkgmgmt.plan.InstalledPackage;
import beast.pkgmgmt.plan.InstalledPackage.Location;
import beast.pkgmgmt.plan.LoadPlan;
import beast.pkgmgmt.plan.Planner;
import beast.pkgmgmt.plan.Runtime;
import beast.pkgmgmt.plan.Version;

/**
 * Chooses the versions to install: the packages asked for and what they need. The same
 * {@link Planner} that decides what loads decides this, with the versions a repository offers
 * as one more kind of copy, so installing and loading follow the same rules. An installed copy
 * that meets what is needed is kept; nothing is downgraded unless that version is asked for.
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
     * Adds to {@code packagesToInstall} the versions of other packages that the packages in it
     * need and that are not installed in a version that fits.
     *
     * @param packageMap        installed and available packages
     * @param packagesToInstall the packages asked for, at the versions asked for
     * @throws DependencyResolutionException when a package asked for cannot be installed, with the reason
     */
    public void resolve(Map<String, Package> packageMap,
                        Map<Package, PackageVersion> packagesToInstall) throws DependencyResolutionException {
        Map<String, Version> asked = new LinkedHashMap<>();
        packagesToInstall.forEach((p, v) -> asked.put(p.getName(), new Version(v.getVersion())));

        List<InstalledPackage> copies = new ArrayList<>();
        for (Package p : packageMap.values()) {
            Version wanted = asked.get(p.getName());
            Version installed = p.isInstalled() ? new Version(p.getInstalledVersion().getVersion()) : null;
            if (installed != null && (wanted == null || wanted.equals(installed))) {
                copies.add(copy(p.getName(), installed, Location.USER, p.getInstalledVersionDependencies()));
            }
            for (PackageVersion offered : p.getAvailableVersions()) {
                Version v = new Version(offered.getVersion());
                boolean fits = wanted != null ? v.equals(wanted) && !v.equals(installed)
                        : installed == null || v.compareTo(installed) > 0;
                if (fits) {
                    copies.add(copy(p.getName(), v, Location.AVAILABLE, p.getDependencies(offered)));
                }
            }
        }

        // nothing is loaded here, so no jars or launcher are involved: only versions
        Version beast = new Version(PackageManager.beastVersion.getVersion());
        LoadPlan plan = Planner.plan(copies, new Runtime(beast, beast, List.of()));

        for (String name : asked.keySet()) {
            if (plan.loadedPackage(name).isEmpty()) {
                String why = plan.exclusion(name).map(LoadPlan.Excluded::reason).orElse("it is not available");
                throw new DependencyResolutionException("Cannot install " + name + " " + asked.get(name) + ": " + why);
            }
        }
        // what the packages asked for need, as planned; those not installed yet are installed
        Set<String> needed = new LinkedHashSet<>(asked.keySet());
        List<String> todo = new ArrayList<>(asked.keySet());
        while (!todo.isEmpty()) {
            String next = todo.remove(todo.size() - 1);
            plan.loadedPackage(next)
                    // a package installed from Maven gets its dependencies from Maven, so no ZIP
                    // copies are added for them (#316); the plan above still checks they fit
                    .filter(p -> !installsAsMaven(packageMap.get(next), p))
                    .ifPresent(p -> p.depends().forEach(d -> {
                if (needed.add(d.name())) {
                    todo.add(d.name());
                }
            }));
        }
        for (String name : needed) {
            InstalledPackage chosen = plan.loadedPackage(name).orElse(null);
            if (chosen != null && chosen.location() == Location.AVAILABLE && !asked.containsKey(name)) {
                Package pkg = packageMap.get(name);
                pkg.getAvailableVersions().stream().filter(v -> new Version(v.getVersion()).equals(chosen.version()))
                        .findFirst().ifPresent(v -> packagesToInstall.put(pkg, v));
            }
        }
    }

    /** true if {@code chosen} is a version still to be installed, and it is installed from Maven */
    private static boolean installsAsMaven(Package pkg, InstalledPackage chosen) {
        if (pkg == null || chosen.location() != Location.AVAILABLE) {
            return false;
        }
        return pkg.getAvailableVersions().stream()
                .filter(v -> new Version(v.getVersion()).equals(chosen.version()))
                .anyMatch(v -> PackageInstaller.installsAsMaven(pkg, v));
    }

    private static InstalledPackage copy(String name, Version version, Location location, Set<PackageDependency> deps) {
        List<Dependency> depends = new ArrayList<>();
        if (deps != null) {
            for (PackageDependency d : deps) {
                depends.add(new Dependency(d.dependencyName,
                        d.atLeast == null ? null : new Version(d.atLeast.getVersion()),
                        d.atMost == null ? null : new Version(d.atMost.getVersion())));
            }
        }
        return new InstalledPackage(name, version, location, depends, List.of());
    }
}
