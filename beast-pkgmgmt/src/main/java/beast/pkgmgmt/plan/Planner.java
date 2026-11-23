package beast.pkgmgmt.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import beast.pkgmgmt.plan.Artifact.Kind;
import beast.pkgmgmt.plan.InstalledPackage.Location;
import beast.pkgmgmt.plan.LoadPlan.DroppedModule;
import beast.pkgmgmt.plan.LoadPlan.Excluded;
import beast.pkgmgmt.plan.LoadPlan.PlannedModule;

/**
 * Decides what to load from the installed packages, before anything is loaded. A plain
 * function of its inputs: no files, no network, no module layers, so the same inputs always give
 * the same plan, whatever order the packages were found in.
 * <p>
 * The rules, each exclusion carrying its reason:
 * <ol>
 * <li>Only BEAST.base and BEAST.app provide the core ({@code beast.base}, {@code beast.fx},
 *     {@code beast.pkgmgmt}) and nothing provides JavaFX: such jars among another package's
 *     libraries are ignored (#298). A dependency on a core package without a maximum is a minimum
 *     within the same minor version (2.8.x).</li>
 * <li>Of several installed copies of a package, the first that works is used: a copy on the
 *     boot layer, then one on {@code BEAST_PACKAGE_PATH}, then the newest. So an upgraded core
 *     package is used if it works, and the bundled copy otherwise. When a package needs another
 *     version of a dependency than the copy tried, and another copy fits, that copy is tried
 *     before the package is excluded. Copies available to download come last.</li>
 * <li>A copy that needs a newer launcher than the installer has is not used, nor is a core copy
 *     of another major version than the launcher's: that needs a new installer.</li>
 * <li>One copy of each module: a package's own module before any library copy of it. Library
 *     versions with the same major version are compatible and share the highest copy. When
 *     packages need incompatible (different major) versions, each version is a private copy in
 *     its own layer, with the packages that need it and everything that depends on them; a
 *     package that would need two such copies is excluded, naming the library.</li>
 * <li>Each Java package is in exactly one module.</li>
 * <li>Every non-static {@code requires} is met inside the plan. A module that cannot be met is
 *     left out ("headless" when JavaFX is missing); a package left with none of its own modules
 *     is excluded, and so is everything that needs it. Headless, a dependency on BEAST.app is
 *     not a failure: the package's FX modules are left out, the rest loads.</li>
 * <li>Versions are ordered one way everywhere ({@link Version}).</li>
 * </ol>
 */
public final class Planner {

    public static final String BASE = "BEAST.base", APP = "BEAST.app";
    /** the launcher, beast.pkgmgmt: a dependency on it says which installer a core copy needs */
    public static final String LAUNCHER = "BEAST.launcher";
    /** JavaFX of the JRE: a newer one asked for than the JRE has only warns */
    public static final String JAVAFX = "JavaFX";

    private static final Set<String> CORE_PACKAGES = Set.of(BASE, APP);
    private static final Set<String> CORE_MODULES = Set.of("beast.base", "beast.fx", "beast.pkgmgmt");

    /** core packages first, then by name, so that "the later package" is always the same one */
    private static final Comparator<String> PACKAGE_ORDER = Comparator
            .comparingInt((String n) -> n.equals(BASE) ? 0 : n.equals(APP) ? 1 : 2)
            .thenComparing(Comparator.naturalOrder());

    /** boot layer, then BEAST_PACKAGE_PATH, then newest version, then location */
    private static final Comparator<InstalledPackage> COPY_ORDER = Comparator
            .comparingInt((InstalledPackage p) -> switch (p.location()) {
                case BOOT -> 0;
                case OVERRIDE -> 1;
                case ARCHIVE -> 3;
                case AVAILABLE -> 4;
                default -> 2;
            })
            .thenComparing(InstalledPackage::version, Comparator.reverseOrder())
            .thenComparing(InstalledPackage::location);

    private final Runtime runtime;
    private final Selection selection;
    /** with -strictversions: pinned package versions that are not installed */
    private final List<String> missing = new ArrayList<>();
    private final Set<String> bootModules = new LinkedHashSet<>();
    /** the copies still in the running for each package, best first */
    private final Map<String, List<InstalledPackage>> candidates = new TreeMap<>(PACKAGE_ORDER);
    private final List<Excluded> excluded = new ArrayList<>();
    private final List<Excluded> notUsed = new ArrayList<>();
    private final Set<String> warnings = new LinkedHashSet<>();

    private record Rejection(InstalledPackage copy, String reason) {}

    /** a module in the plan, and the package whose copy of the jar is used */
    private record Planned(Artifact artifact, InstalledPackage from) {}

    /** the shared layer's name; a private layer is named after its first package */
    static final String SHARED = "";

    /** layer name to its modules (module name to the planned copy); the shared layer first */
    private record Modules(Map<String, Map<String, Planned>> layers, List<DroppedModule> dropped, Rejection rejection) {}

    /** packages that get their own copy of libraries: the packages that need it and their dependents */
    private record Group(Set<String> members, Map<String, Planned> libraries) {}

    private Planner(Collection<InstalledPackage> installed, Runtime runtime, Selection selection) {
        this.runtime = runtime;
        this.selection = selection;
        runtime.bootModules().forEach(m -> bootModules.add(m.name()));
        for (InstalledPackage p : installed) {
            candidates.computeIfAbsent(p.name(), k -> new ArrayList<>()).add(p);
        }
        candidates.values().forEach(copies -> copies.sort(COPY_ORDER));
        if (selection.strict()) {
            select();
        }
    }

    public static LoadPlan plan(Collection<InstalledPackage> installed, Runtime runtime) {
        return plan(installed, runtime, Selection.ALL);
    }

    public static LoadPlan plan(Collection<InstalledPackage> installed, Runtime runtime, Selection selection) {
        return new Planner(installed, runtime, selection).run();
    }

    /**
     * -strictversions: a pinned package keeps only the copy of that exact version, or is
     * missing; a pinned core version is tried first, the other core copies stay as fallback (an
     * analysis made with an older BEAST still runs, with a warning); packages that are neither
     * pinned nor needed by a pinned package are not loaded.
     */
    private void select() {
        for (Map.Entry<String, Version> pin : selection.pinned().entrySet()) {
            List<InstalledPackage> copies = candidates.get(pin.getKey());
            String wanted = pin.getKey() + " " + pin.getValue();
            if (CORE_PACKAGES.contains(pin.getKey())) {
                if (copies != null) {
                    copies.sort(Comparator.comparing((InstalledPackage p) -> !p.version().equals(pin.getValue()))
                            .thenComparing(COPY_ORDER));
                }
                continue;
            }
            if (copies == null) {
                missing.add(wanted);
                continue;
            }
            for (InstalledPackage p : List.copyOf(copies)) {
                if (!p.version().equals(pin.getValue())) {
                    copies.remove(p);
                    notUsed.add(new Excluded(p, "the analysis was made with " + wanted));
                }
            }
            if (copies.isEmpty()) {
                candidates.remove(pin.getKey());
                missing.add(wanted);
            }
        }
        // keep the core, the pinned packages, and what they need
        Set<String> keep = new LinkedHashSet<>(CORE_PACKAGES);
        List<String> todo = new ArrayList<>(selection.pinned().keySet());
        while (!todo.isEmpty()) {
            String name = todo.remove(todo.size() - 1);
            if (keep.add(name) || CORE_PACKAGES.contains(name)) {
                List<InstalledPackage> copies = candidates.get(name);
                if (copies != null) {
                    copies.forEach(c -> c.depends().forEach(d -> {
                        if (!keep.contains(d.name())) {
                            todo.add(d.name());
                        }
                    }));
                }
            }
        }
        for (String name : List.copyOf(candidates.keySet())) {
            if (!keep.contains(name)) {
                for (InstalledPackage p : candidates.remove(name)) {
                    notUsed.add(new Excluded(p, "not used by the analysis (-strictversions)"));
                }
            }
        }
    }

    /**
     * Try the best copy of every package; reject one copy that does not work, and try again.
     * Each round removes a copy, so this ends.
     */
    private LoadPlan run() {
        while (true) {
            warnings.clear();
            Map<String, InstalledPackage> chosen = new LinkedHashMap<>();
            candidates.forEach((name, copies) -> chosen.put(name, copies.get(0)));
            Rejection rejection = checkDependencies(chosen);
            if (rejection == null) {
                Modules modules = resolveModules(chosen);
                if (modules.rejection() == null) {
                    return build(chosen, modules);
                }
                rejection = modules.rejection();
            }
            reject(rejection);
        }
    }

    private void reject(Rejection r) {
        List<InstalledPackage> copies = candidates.get(r.copy().name());
        copies.remove(r.copy());
        if (copies.isEmpty()) {
            candidates.remove(r.copy().name());
            excluded.add(new Excluded(r.copy(), r.reason()));
        } else {
            notUsed.add(new Excluded(r.copy(), r.reason()));
        }
    }

    // ---- package dependencies ----

    /**
     * The first package whose dependencies are not met. A package that fails only because
     * something it needs fails too is rejected last, since that dependency may still fall back
     * to another copy that works.
     */
    private Rejection checkDependencies(Map<String, InstalledPackage> chosen) {
        Map<String, String> failing = new LinkedHashMap<>();
        for (InstalledPackage p : chosen.values()) {
            String why = dependencyFailure(p, chosen);
            if (why != null) {
                failing.put(p.name(), why);
            }
        }
        // a dependency whose copy does not fit a package that needs it, while another copy
        // would: try that copy first, before excluding the package
        for (InstalledPackage p : chosen.values()) {
            for (Dependency d : p.depends()) {
                InstalledPackage q = chosen.get(d.name());
                List<InstalledPackage> copies = candidates.get(d.name());
                boolean core = CORE_PACKAGES.contains(d.name());
                if (q != null && q.location() != Location.BOOT && !d.accepts(q.version(), core)
                        && copies.subList(1, copies.size()).stream().anyMatch(c -> d.accepts(c.version(), core))) {
                    // a copy that fails on its own is rejected for its own reason
                    String own = failing.get(q.name());
                    return new Rejection(q, own != null ? own : p.name() + " " + p.version() + " needs " + d.describe(core));
                }
            }
        }
        if (failing.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> f : failing.entrySet()) {
            InstalledPackage p = chosen.get(f.getKey());
            if (p.depends().stream().noneMatch(d -> failing.containsKey(d.name()))) {
                return new Rejection(p, f.getValue());
            }
        }
        Map.Entry<String, String> first = failing.entrySet().iterator().next();
        return new Rejection(chosen.get(first.getKey()), first.getValue());
    }

    private String dependencyFailure(InstalledPackage p, Map<String, InstalledPackage> chosen) {
        // a core upgrade stays within the installer's major version (decision 7); a launcher
        // without a version (developer mode, tests) is not checked
        long launcherMajor = runtime.launcher().major();
        if (CORE_PACKAGES.contains(p.name()) && p.location() != Location.BOOT && launcherMajor > 0
                && p.version().major() != launcherMajor) {
            return "BEAST " + p.version().major() + ".x needs a new BEAST installer (this one is "
                    + runtime.launcher() + ")";
        }
        for (Dependency d : p.depends()) {
            switch (d.name()) {
                case LAUNCHER -> {
                    if (!d.accepts(runtime.launcher(), false)) {
                        return "needs a newer BEAST installer (launcher " + d.describe(false).substring(LAUNCHER.length() + 1)
                                + "; this one has " + runtime.launcher() + ")";
                    }
                }
                case JAVAFX -> {
                    if (runtime.hasJavaFX() && !d.accepts(runtime.javafx(), false)) {
                        warnings.add(p.name() + " " + p.version() + " asks for " + d.describe(false)
                                + "; this BEAST has JavaFX " + runtime.javafx());
                    }
                }
                default -> {
                    InstalledPackage q = chosen.get(d.name());
                    boolean core = CORE_PACKAGES.contains(d.name());
                    if (q == null && d.name().equals(APP) && !runtime.hasJavaFX()) {
                        // headless: packages also need BEAST.app for their FX modules only, which
                        // are left out on their own (their requires are not met); the rest loads
                        continue;
                    }
                    if (q == null) {
                        String pinned = missing.stream().filter(m -> m.startsWith(d.name() + " ")).findFirst().orElse(null);
                        if (pinned != null) {
                            return "needs " + pinned + ", which the analysis was made with and is not installed";
                        }
                        boolean wasExcluded = excluded.stream().anyMatch(e -> e.copy().name().equals(d.name()));
                        return "needs " + d.name() + (wasExcluded ? ", which is excluded" : ", which is not installed");
                    }
                    if (!d.accepts(q.version(), core)) {
                        return "needs " + d.describe(core) + "; installed: " + q.version();
                    }
                }
            }
        }
        return null;
    }

    // ---- modules ----

    private Modules resolveModules(Map<String, InstalledPackage> chosen) {
        Map<String, Planned> shared = new LinkedHashMap<>();
        Map<String, String> ownerOf = new HashMap<>();

        // every package's own modules, in the shared layer for now
        for (InstalledPackage p : chosen.values()) {
            for (Artifact a : p.artifacts()) {
                if (a.kind() != Kind.OWN || bootModules.contains(a.module())) {
                    continue;
                }
                if (!CORE_PACKAGES.contains(p.name()) && isBeastOrJavaFX(a.module())) {
                    return rejected(p, "contains " + a.module() + ", which only BEAST.base and BEAST.app provide");
                }
                Planned other = shared.get(a.module());
                if (other != null) {
                    return rejected(p, "module " + a.module() + " is also in " + other.from().name());
                }
                shared.put(a.module(), new Planned(a, p));
                ownerOf.put(a.module(), p.name());
            }
        }

        // libraries: never the core, JavaFX or the JRE's own modules; a package's own module wins
        Map<String, List<Planned>> requests = new java.util.TreeMap<>();
        for (InstalledPackage p : chosen.values()) {
            List<String> ignored = new ArrayList<>();
            for (Artifact a : p.artifacts()) {
                if (a.kind() != Kind.LIBRARY) {
                    continue;
                }
                if (isBeastOrJavaFX(a.module())) {
                    ignored.add(a.module() + " " + a.version());
                } else if (!bootModules.contains(a.module()) && !ownerOf.containsKey(a.module())) {
                    requests.computeIfAbsent(a.module(), k -> new ArrayList<>()).add(new Planned(a, p));
                }
            }
            if (!ignored.isEmpty()) {
                warnings.add(p.name() + " " + p.version() + ": ignores " + ignored.size()
                        + " jars of BEAST, JavaFX or the JRE among its dependencies, e.g. " + ignored.get(0)
                        + "; BEAST's own copies are used");
            }
        }

        // compatible versions (same major) share the highest copy. Incompatible ones each become
        // a private copy, and none is shared: a copy without module-info is an automatic module,
        // which reads every module in its parent layers, and may not read one of its own name
        List<Group> privateCopies = new ArrayList<>();
        for (Map.Entry<String, List<Planned>> e : requests.entrySet()) {
            Map<Long, List<Planned>> byMajor = new java.util.TreeMap<>();
            e.getValue().forEach(pl -> byMajor.computeIfAbsent(pl.artifact().version().major(), k -> new ArrayList<>()).add(pl));
            if (byMajor.size() == 1) {
                shared.put(e.getKey(), highest(e.getValue()));
                continue;
            }
            byMajor.forEach((major, copies) -> {
                Map<String, Planned> lib = new LinkedHashMap<>();
                lib.put(e.getKey(), highest(copies));
                privateCopies.add(new Group(new java.util.TreeSet<>(packagesOf(copies)), lib));
            });
        }

        // a private copy goes to its packages and everything that depends on them; groups that
        // meet merge, unless they hold different copies of the same library
        Map<String, Set<String>> dependents = new HashMap<>();
        for (Planned m : shared.values()) {
            if (m.artifact().kind() == Kind.OWN) {
                for (String r : m.artifact().requires()) {
                    String owner = ownerOf.get(r);
                    if (owner != null && !owner.equals(m.from().name())) {
                        dependents.computeIfAbsent(owner, k -> new LinkedHashSet<>()).add(m.from().name());
                    }
                }
            }
        }
        List<Group> groups = new ArrayList<>();
        for (Group copy : privateCopies) {
            Set<String> members = new java.util.TreeSet<>(copy.members());
            List<String> todo = new ArrayList<>(members);
            while (!todo.isEmpty()) {
                for (String d : dependents.getOrDefault(todo.remove(todo.size() - 1), Set.of())) {
                    if (members.add(d)) {
                        todo.add(d);
                    }
                }
            }
            Group g = new Group(members, new LinkedHashMap<>(copy.libraries()));
            for (java.util.Iterator<Group> it = groups.iterator(); it.hasNext();) {
                Group o = it.next();
                if (java.util.Collections.disjoint(o.members(), g.members())) {
                    continue;
                }
                for (String lib : g.libraries().keySet()) {
                    if (o.libraries().containsKey(lib)) {
                        Set<String> both = new java.util.TreeSet<>(o.members());
                        both.retainAll(g.members());
                        return rejected(chosen.get(both.iterator().next()), "needs packages that use incompatible versions of "
                                + lib + " (" + o.libraries().get(lib).artifact().version() + ", "
                                + g.libraries().get(lib).artifact().version() + ")");
                    }
                }
                g.members().addAll(o.members());
                g.libraries().putAll(o.libraries());
                it.remove();
            }
            groups.add(g);
        }
        // a member that itself needs another major of a private library cannot have both
        for (Group g : groups) {
            for (Map.Entry<String, Planned> lib : g.libraries().entrySet()) {
                for (Planned req : requests.get(lib.getKey())) {
                    long major = req.artifact().version().major();
                    if (g.members().contains(req.from().name()) && major != lib.getValue().artifact().version().major()) {
                        return rejected(req.from(), "needs " + lib.getKey() + " " + major + ".x, but depends on packages that use "
                                + lib.getKey() + " " + lib.getValue().artifact().version());
                    }
                }
            }
        }

        // the layers: members' own modules move from the shared layer to their group's
        Map<String, Map<String, Planned>> layers = new LinkedHashMap<>();
        layers.put(SHARED, shared);
        groups.sort(Comparator.comparing(g -> g.members().iterator().next()));
        for (Group g : groups) {
            Map<String, Planned> mods = new LinkedHashMap<>(g.libraries());
            for (String member : g.members()) {
                for (Planned m : List.copyOf(shared.values())) {
                    if (m.artifact().kind() == Kind.OWN && m.from().name().equals(member)) {
                        shared.remove(m.artifact().module());
                        mods.put(m.artifact().module(), m);
                    }
                }
            }
            layers.put(g.members().iterator().next(), mods);
        }

        // leave out modules whose requires are not met, until none is left out
        List<DroppedModule> dropped = new ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, Map<String, Planned>> layer : layers.entrySet()) {
                for (Planned m : List.copyOf(layer.getValue().values())) {
                    for (String r : m.artifact().requires()) {
                        if (!isAvailable(r, layer.getValue(), shared)) {
                            layer.getValue().remove(m.artifact().module());
                            dropped.add(new DroppedModule(m.from().name(), m.artifact().module(), missingReason(r)));
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }

        // a package left with none of its own modules is excluded
        for (InstalledPackage p : chosen.values()) {
            List<Artifact> own = p.artifacts().stream()
                    .filter(a -> a.kind() == Kind.OWN && !bootModules.contains(a.module())).toList();
            if (!own.isEmpty() && own.stream().noneMatch(a -> layers.values().stream().anyMatch(l -> l.containsKey(a.module())))) {
                String why = dropped.stream().filter(d -> d.packageName().equals(p.name()))
                        .map(DroppedModule::reason).findFirst().orElse("none of its modules can be loaded");
                return rejected(p, why);
            }
        }

        // each Java package in exactly one module that a module can see: in the shared layer, and
        // in each private layer together with the shared modules it does not replace
        for (Map.Entry<String, Map<String, Planned>> layer : layers.entrySet()) {
            Map<String, String> moduleOfPackage = new HashMap<>();
            for (Runtime.BootModule b : runtime.bootModules()) {
                b.javaPackages().forEach(jp -> moduleOfPackage.putIfAbsent(jp, b.name()));
            }
            List<Planned> visible = new ArrayList<>();
            if (!layer.getKey().equals(SHARED)) {
                shared.values().stream().filter(m -> !layer.getValue().containsKey(m.artifact().module())).forEach(visible::add);
            }
            visible.addAll(layer.getValue().values());
            Map<String, Planned> byModule = new HashMap<>();
            visible.forEach(m -> byModule.put(m.artifact().module(), m));
            for (Planned m : visible) {
                for (String jp : m.artifact().javaPackages()) {
                    String prev = moduleOfPackage.putIfAbsent(jp, m.artifact().module());
                    if (prev != null && !prev.equals(m.artifact().module())) {
                        Planned other = byModule.get(prev);
                        String otherName = other == null ? "BEAST itself" : other.from().name();
                        return rejected(m.from(), "Java package " + jp + " is in both " + prev + " (" + otherName
                                + ") and " + m.artifact().module() + " (" + m.from().name() + ")");
                    }
                }
            }
        }
        return new Modules(layers, dropped, null);
    }

    private static Set<String> packagesOf(List<Planned> copies) {
        Set<String> names = new java.util.TreeSet<>();
        copies.forEach(c -> names.add(c.from().name()));
        return names;
    }

    private static Planned highest(List<Planned> copies) {
        return copies.stream().max(Comparator.comparing((Planned c) -> c.artifact().version())).get();
    }

    private static Modules rejected(InstalledPackage p, String reason) {
        return new Modules(null, null, new Rejection(p, reason));
    }

    /** whether a module in {@code layer} can read {@code module} */
    private boolean isAvailable(String module, Map<String, Planned> layer, Map<String, Planned> shared) {
        return layer.containsKey(module) || shared.containsKey(module) || bootModules.contains(module)
                || module.startsWith("java.") || module.startsWith("jdk.")
                || (module.startsWith("javafx.") && runtime.hasJavaFX());
    }

    private String missingReason(String module) {
        boolean fx = module.startsWith("javafx.") || module.equals("beast.fx");
        return fx && !runtime.hasJavaFX() ? "headless" : "needs module " + module;
    }

    /**
     * Modules only BEAST's core or the JRE provide: the core, JavaFX (also its "...Empty"
     * placeholder jars on Maven Central), and {@code java.*} / {@code jdk.*} (e.g. JavaFX's
     * {@code jdk.jsobject}).
     */
    private static boolean isBeastOrJavaFX(String module) {
        return CORE_MODULES.contains(module) || module.startsWith("javafx.")
                || module.startsWith("jdk.") || module.startsWith("java.");
    }

    // ---- the plan ----

    /**
     * Why a copy is not used when only another copy of the same package is preferred:
     * "using 1.2.2 (user)". {@code beast -version} leaves it out, since it lists that copy (#317).
     */
    public static String usingReason(InstalledPackage used) {
        return "using " + used.version() + " (" + used.location().name().toLowerCase() + ")";
    }

    private LoadPlan build(Map<String, InstalledPackage> chosen, Modules modules) {
        List<Excluded> unused = new ArrayList<>(notUsed);
        candidates.forEach((name, copies) -> {
            InstalledPackage used = copies.get(0);
            for (InstalledPackage other : copies.subList(1, copies.size())) {
                unused.add(new Excluded(other, usingReason(used)));
            }
        });
        List<PlannedModule> list = new ArrayList<>();
        modules.layers().forEach((layer, mods) -> mods.values().forEach(m -> list.add(new PlannedModule(
                m.artifact().module(), m.artifact().version(), m.artifact().jar(), m.from().name(), layer))));
        list.sort(Comparator.comparing(PlannedModule::layer).thenComparing(PlannedModule::name));
        for (String core : CORE_PACKAGES) {
            Version pinned = selection.pinned().get(core);
            InstalledPackage used = chosen.get(core);
            if (pinned != null && used != null && !used.version().equals(pinned)) {
                warnings.add("the analysis was made with " + core + " " + pinned + "; using " + used.version());
            }
        }
        return new LoadPlan(List.copyOf(chosen.values()), List.copyOf(list), List.copyOf(modules.dropped()),
                List.copyOf(excluded), List.copyOf(unused), List.copyOf(warnings), List.copyOf(missing));
    }
}
