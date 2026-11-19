package beast.pkgmgmt.plan;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import beast.pkgmgmt.plan.LoadPlan.PlannedModule;

/**
 * Carries out a {@link LoadPlan}: the shared modules are resolved in one {@link Configuration},
 * so the JVM checks them all at once, and defined in one {@link ModuleLayer} on the boot layer,
 * each module with its own class loader. Each private layer (packages with their own copy of a
 * library) is a child of the shared layer, so its copy is seen only by those packages. No retry
 * loop, no "skip if already loaded": the planner has already decided.
 * <p>
 * The plan must have been made for this JVM's boot layer ({@link Inventory#readThisRuntime()}).
 */
public final class Loader {

    private Loader() {
    }

    /** thrown when the JVM rejects a plan; names the packages whose modules are involved */
    public static final class LoadException extends Exception {
        private static final long serialVersionUID = 1L;

        LoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static LoadedPackages load(LoadPlan plan) throws LoadException {
        ModuleLayer boot = ModuleLayer.boot();
        try {
            ModuleLayer shared = define(plan, "", boot);
            Map<String, ModuleLayer> privateLayers = new LinkedHashMap<>();
            for (String name : plan.privateLayers()) {
                privateLayers.put(name, define(plan, name, shared));
            }
            return new LoadedPackages(plan, shared, privateLayers);
        } catch (RuntimeException e) {
            // FindException, ResolutionException, LayerInstantiationException
            throw new LoadException("The JVM rejected the load plan" + packagesNamedIn(e.getMessage(), plan)
                    + ": " + e.getMessage(), e);
        }
    }

    /**
     * One layer: the planned modules of {@code layerName}, resolved in one configuration on top
     * of {@code parent}. In a private layer its own copy of a library comes before the shared one.
     */
    private static ModuleLayer define(LoadPlan plan, String layerName, ModuleLayer parent) {
        List<PlannedModule> modules = plan.modules().stream()
                .filter(m -> m.layer().equals(layerName) && m.jar() != null).toList();
        Set<String> roots = modules.stream().map(PlannedModule::name).collect(Collectors.toCollection(LinkedHashSet::new));
        ModuleFinder finder = ModuleFinder.of(modules.stream().map(PlannedModule::jar).toArray(Path[]::new));
        Configuration cf = parent.configuration().resolveAndBind(finder, ModuleFinder.of(), roots);
        return ModuleLayer.defineModulesWithManyLoaders(cf, List.of(parent), ClassLoader.getSystemClassLoader()).layer();
    }

    /** " (packages A, B)" for the planned modules a JVM message names */
    private static String packagesNamedIn(String message, LoadPlan plan) {
        if (message == null) {
            return "";
        }
        Map<String, String> packageOf = plan.modules().stream()
                .collect(Collectors.toMap(PlannedModule::name, PlannedModule::providedBy, (a, b) -> a));
        Set<String> named = new LinkedHashSet<>();
        Matcher m = Pattern.compile("[A-Za-z_][\\w.]*").matcher(message);
        while (m.find()) {
            String p = packageOf.get(m.group());
            if (p != null) {
                named.add(p);
            }
        }
        return named.isEmpty() ? "" : " (packages " + String.join(", ", named) + ")";
    }
}
