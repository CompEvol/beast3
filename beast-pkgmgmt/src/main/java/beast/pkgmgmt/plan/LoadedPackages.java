package beast.pkgmgmt.plan;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import beast.pkgmgmt.BEASTClassLoader;

/**
 * What a {@link Loader} loaded: the shared layer and the private layers, and lookups over them
 * and the boot layer.
 * <p>
 * Each Java package of BEAST code is in exactly one module (the planner sees to that), so a class
 * is found from its package name directly, never by trying layers in turn. Services are the
 * {@code provides} of loaded modules only, so a package that did not load offers nothing.
 */
public final class LoadedPackages {

    private final LoadPlan plan;
    private final ModuleLayer layer;
    private final Map<String, ModuleLayer> privateLayers;
    private final Map<String, Module> moduleOfPackage = new HashMap<>();

    LoadedPackages(LoadPlan plan, ModuleLayer layer, Map<String, ModuleLayer> privateLayers) {
        this.plan = plan;
        this.layer = layer;
        this.privateLayers = privateLayers;
        // a library's Java package can be in the shared layer and in private layers: by name, a
        // class is the shared copy. BEAST's own classes are in one module only.
        for (ModuleLayer l : layers()) {
            for (Module m : l.modules()) {
                m.getPackages().forEach(p -> moduleOfPackage.putIfAbsent(p, m));
            }
        }
    }

    /** the boot layer, the shared layer, then the private layers */
    public List<ModuleLayer> layers() {
        List<ModuleLayer> all = new ArrayList<>(List.of(ModuleLayer.boot(), layer));
        all.addAll(privateLayers.values());
        return all;
    }

    public Map<String, ModuleLayer> privateLayers() {
        return privateLayers;
    }

    public LoadPlan plan() {
        return plan;
    }

    /** the shared layer */
    public ModuleLayer layer() {
        return layer;
    }

    /**
     * The class, from the module that has its package: for a BEAST class the one module, for
     * a library class with private copies the shared copy.
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        int dot = name.lastIndexOf('.');
        Module m = dot < 0 ? null : moduleOfPackage.get(name.substring(0, dot));
        if (m == null) {
            throw new ClassNotFoundException(name + ": no loaded module has its package");
        }
        Class<?> c = Class.forName(m, name);
        if (c == null) {
            throw new ClassNotFoundException(name + ": not in module " + m.getName());
        }
        return c;
    }

    /**
     * The providers of a service: {@code provides} in the module-info of loaded modules, and, for
     * old packages, {@code <service>} in the version.xml of loaded packages (decision 5: still
     * honoured; the inventory warns about them).
     */
    public Set<String> providers(String service) {
        Set<String> result = new TreeSet<>();
        plan.loaded().forEach(p -> result.addAll(p.services().getOrDefault(service, Set.of())));
        for (ModuleLayer l : layers()) {
            for (Module m : l.modules()) {
                ModuleDescriptor d = m.getDescriptor();
                if (d != null) {
                    d.provides().stream().filter(p -> p.service().equals(service))
                            .forEach(p -> result.addAll(p.providers()));
                }
            }
        }
        return result;
    }

    /**
     * Makes the loaded modules visible to today's {@link BEASTClassLoader}, so that existing
     * BEAST code ({@code BEASTClassLoader.forName}, services) works on top of this loader: the
     * bridge for moving the package manager over one step at a time.
     */
    public void registerWithBEASTClassLoader() {
        // version.xml services of the loaded packages, so old packages keep working (decision 5)
        Map<String, Set<String>> services = new HashMap<>();
        plan.loaded().forEach(p -> p.services().forEach((s, providers) ->
                services.computeIfAbsent(s, k -> new TreeSet<>()).addAll(providers)));
        BEASTClassLoader.registerPluginLayer(layer, services);
        privateLayers.values().forEach(l -> BEASTClassLoader.registerPluginLayer(l, null));
    }
}
