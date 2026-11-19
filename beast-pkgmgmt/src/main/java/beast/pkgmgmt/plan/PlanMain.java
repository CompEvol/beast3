package beast.pkgmgmt.plan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import beast.pkgmgmt.MavenPackageResolver;
import beast.pkgmgmt.plan.InstalledPackage.Location;

/**
 * Prints the load plan for an install, without loading anything: a prototype of
 * {@code packagemanager -plan}. Reads only; bundled zips are unpacked into a scratch folder.
 *
 * <pre>
 * PlanMain --user DIR [--override DIR]... [--app CONTENTS_APP_DIR --jre JAVA_HOME [--unpack DIR] | --boot MODULE_PATH]
 *          [--headless] [--modules] [--xml ANALYSIS] [--load [--run MAIN_CLASS [ARGS...]]]
 * </pre>
 * {@code --xml}: as {@code beast -strictversions ANALYSIS}: the package versions in the
 * analysis's {@code required} attribute. {@code --load}: also load the plan in this JVM (so the runtime must describe this JVM: use it
 * without {@code --app}/{@code --boot}), register the layer with BEASTClassLoader, and with
 * {@code --run} start {@code MAIN_CLASS} with the remaining arguments.
 * {@code --boot}: developer mode, the module path {@code mvn exec} uses (the core on the boot
 * layer). Without {@code --app} or {@code --boot}, the runtime is a release launcher 2.8.0 with
 * JavaFX and nothing else on the boot layer.
 */
public final class PlanMain {

    public static void main(String[] args) throws Exception {
        Path user = null, app = null, jre = null, unpack = null;
        List<Path> boot = null;
        List<Path> overrides = new ArrayList<>();
        boolean headless = false, modules = false, load = false;
        String run = null;
        Selection selection = Selection.ALL;
        String[] runArgs = new String[0];
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--user" -> user = Path.of(args[++i]);
                case "--override" -> overrides.add(Path.of(args[++i]));
                case "--app" -> app = Path.of(args[++i]);
                case "--jre" -> jre = Path.of(args[++i]);
                case "--unpack" -> unpack = Path.of(args[++i]);
                case "--boot" -> boot = java.util.Arrays.stream(args[++i].split(java.io.File.pathSeparator))
                        .filter(e -> !e.isEmpty()).map(Path::of).toList();
                case "--headless" -> headless = true;
                case "--modules" -> modules = true;
                case "--load" -> load = true;
                case "--xml" -> selection = Selection.fromAnalysis(Path.of(args[++i]));
                case "--run" -> {
                    run = args[++i];
                    runArgs = java.util.Arrays.copyOfRange(args, i + 1, args.length);
                    i = args.length;
                }
                default -> throw new IllegalArgumentException("unknown argument " + args[i]);
            }
        }

        Runtime runtime = load ? Inventory.readThisRuntime()
                : boot != null ? Inventory.readDeveloperRuntime(boot)
                : app != null ? Inventory.readRuntime(app, jre)
                : new Runtime(Version.of("2.8.0"), Version.of("25"), List.of(
                        new Runtime.BootModule("beast.pkgmgmt", java.util.Set.of("beast.pkgmgmt"))));
        if (headless) {
            runtime = new Runtime(runtime.launcher(), null, runtime.bootModules());
        }

        Inventory inventory = new Inventory();
        if (boot != null) {
            inventory.addCoreOnBootLayer(runtime, Version.of("2.8.0"));
        }
        for (Path o : overrides) {
            inventory.addFolder(o, Location.OVERRIDE);
        }
        if (user != null) {
            inventory.addFolder(user, Location.USER);
            inventory.addArchive(user);
            Path repo = user.resolve("maven-repo");
            if (Files.isRegularFile(user.resolve("maven-packages.xml"))) {
                MavenPackageResolver resolver = new MavenPackageResolver(repo);
                inventory.addMavenPackages(user, coordinate -> {
                    try {
                        return resolver.resolve(coordinate);
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                });
            }
        }
        if (app != null) {
            inventory.addBundledZips(app.resolve("packages"),
                    unpack != null ? unpack : Files.createTempDirectory("bundled-packages"));
        }

        System.out.println("Runtime: launcher " + runtime.launcher() + ", JavaFX "
                + (runtime.hasJavaFX() ? runtime.javafx() : "none (headless)") + ", "
                + runtime.bootModules().size() + " boot modules");
        System.out.println("Installed copies read: " + inventory.packages().size());
        for (String p : inventory.problems()) {
            System.out.println("  problem: " + p);
        }
        for (String w : inventory.warnings()) {
            System.out.println("  package metadata: " + w);
        }
        System.out.println();
        if (selection.strict()) {
            System.out.println("-strictversions: " + selection.pinned());
        }
        LoadPlan plan = Planner.plan(inventory.packages(), runtime, selection);
        System.out.print(plan.describe());
        if (modules) {
            System.out.println();
            for (LoadPlan.PlannedModule m : plan.modules()) {
                System.out.printf("  %-40s %-16s from %-12s %s%n", m.name(), m.version(), m.providedBy(),
                        m.jar() == null ? "" : m.jar().getFileName());
            }
        }
        if (load) {
            LoadedPackages loaded = Loader.load(plan);
            loaded.registerWithBEASTClassLoader();
            System.out.println();
            System.out.println("Loaded " + loaded.layer().modules().size() + " modules into one layer");
            if (run != null) {
                System.out.println("Running " + run + " " + String.join(" ", runArgs));
                loaded.loadClass(run).getMethod("main", String[].class).invoke(null, (Object) runArgs);
            }
        }
    }
}
