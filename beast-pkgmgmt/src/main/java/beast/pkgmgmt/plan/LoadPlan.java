package beast.pkgmgmt.plan;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * What to load, and why the rest is not loaded: the planner's answer for one set of installed
 * packages and one runtime. The loader carries it out; {@code packagemanager -plan} prints it.
 *
 * @param loaded         the copy of each package that is loaded
 * @param modules        the modules to load, one copy each, from the loaded packages' jars;
 *                       modules on the boot layer are not listed
 * @param droppedModules modules of loaded packages that are left out, e.g. an FX module headless
 * @param excluded       packages not loaded at all, one entry each, with the reason
 * @param notUsed        other installed copies of loaded packages, with the reason they are not used
 * @param warnings       things that do not stop loading, e.g. a package asking for a newer JavaFX
 * @param missing        with -strictversions: package versions the analysis needs that are not
 *                       installed, e.g. "SA 2.1.1"; the analysis cannot run as it was made
 */
public record LoadPlan(List<InstalledPackage> loaded, List<PlannedModule> modules,
                       List<DroppedModule> droppedModules, List<Excluded> excluded,
                       List<Excluded> notUsed, List<String> warnings, List<String> missing) {

    /**
     * a module to load: its jar, the package whose copy it is, and its layer: "" for the shared
     * layer, or the name of a private layer, which holds a private copy of a library and the
     * packages that use it
     */
    public record PlannedModule(String name, Version version, Path jar, String providedBy, String layer) {}

    public record DroppedModule(String packageName, String module, String reason) {}

    public record Excluded(InstalledPackage copy, String reason) {}

    public Optional<InstalledPackage> loadedPackage(String name) {
        return loaded.stream().filter(p -> p.name().equals(name)).findFirst();
    }

    /** the module in the shared layer, or else the first private copy */
    public Optional<PlannedModule> module(String name) {
        return modules.stream().filter(m -> m.name().equals(name)).findFirst();
    }

    public Optional<PlannedModule> module(String name, String layer) {
        return modules.stream().filter(m -> m.name().equals(name) && m.layer().equals(layer)).findFirst();
    }

    /** the private layers, by name */
    public java.util.Set<String> privateLayers() {
        java.util.Set<String> names = new java.util.TreeSet<>();
        modules.stream().map(PlannedModule::layer).filter(l -> !l.isEmpty()).forEach(names::add);
        return names;
    }

    public Optional<Excluded> exclusion(String name) {
        return excluded.stream().filter(e -> e.copy().name().equals(name)).findFirst();
    }

    /** one line per package, as {@code packagemanager -plan} would print it */
    public String describe() {
        StringBuilder b = new StringBuilder();
        for (InstalledPackage p : loaded) {
            b.append(String.format("%-14s %-12s %-9s loaded%n", p.name(), p.version(), p.location().name().toLowerCase()));
        }
        for (String layer : privateLayers()) {
            b.append("private layer ").append(layer).append(": ");
            b.append(String.join(", ", modules.stream().filter(m -> m.layer().equals(layer))
                    .map(m -> m.name() + " " + m.version()).toList()));
            b.append(System.lineSeparator());
        }
        for (DroppedModule d : droppedModules) {
            b.append(String.format("%-14s %-12s %-9s module %s left out: %s%n", d.packageName(), "", "", d.module(), d.reason()));
        }
        for (Excluded e : excluded) {
            b.append(String.format("%-14s %-12s %-9s EXCLUDED: %s%n", e.copy().name(), e.copy().version(),
                    e.copy().location().name().toLowerCase(), e.reason()));
        }
        for (Excluded e : notUsed) {
            b.append(String.format("%-14s %-12s %-9s not used: %s%n", e.copy().name(), e.copy().version(),
                    e.copy().location().name().toLowerCase(), e.reason()));
        }
        for (String m : missing) {
            b.append("MISSING: the analysis needs ").append(m).append(", which is not installed (packagemanager -add ")
                    .append(m.replace(" ", " -version ")).append(')').append(System.lineSeparator());
        }
        for (String w : warnings) {
            b.append("warning: ").append(w).append(System.lineSeparator());
        }
        return b.toString();
    }
}
