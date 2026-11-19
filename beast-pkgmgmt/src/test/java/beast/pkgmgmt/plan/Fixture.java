package beast.pkgmgmt.plan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import beast.pkgmgmt.plan.Artifact.Kind;
import beast.pkgmgmt.plan.InstalledPackage.Location;
import beast.pkgmgmt.plan.Runtime.BootModule;

/** Builders for made-up installs: packages, their jars, and the runtime. */
final class Fixture {

    private Fixture() {
    }

    static Set<String> pkgs(String... javaPackages) {
        return Set.of(javaPackages);
    }

    static Set<String> requires(String... modules) {
        return Set.of(modules);
    }

    static Pkg pkg(String name, String version, Location location) {
        return new Pkg(name, version, location);
    }

    /** BEAST.base: the module beast.base */
    static Pkg base(String version, Location location) {
        return pkg(Planner.BASE, version, location).own("beast.base", pkgs("beast.base.core", "beast.base.inference"), requires());
    }

    /** BEAST.app: the module beast.fx, which needs beast.base and JavaFX */
    static Pkg app(String version, Location location) {
        return pkg(Planner.APP, version, location).needs(Planner.BASE, "2.8.0")
                .own("beast.fx", pkgs("beastfx.app"), requires("beast.base", "javafx.controls"));
    }

    /** a release: only the launcher on the boot layer, JavaFX in the JRE */
    static Runtime release() {
        return new Runtime(Version.of("2.8.0"), Version.of("25.0.3"),
                List.of(new BootModule("beast.pkgmgmt", pkgs("beast.pkgmgmt"))));
    }

    /** a cluster: no JavaFX */
    static Runtime headless() {
        return new Runtime(Version.of("2.8.0"), null,
                List.of(new BootModule("beast.pkgmgmt", pkgs("beast.pkgmgmt"))));
    }

    /** developer mode: the core on the boot layer */
    static Runtime developer() {
        return new Runtime(Version.of("2.8.0"), Version.of("25.0.3"), List.of(
                new BootModule("beast.pkgmgmt", pkgs("beast.pkgmgmt")),
                new BootModule("beast.base", pkgs("beast.base.core", "beast.base.inference")),
                new BootModule("beast.fx", pkgs("beastfx.app"))));
    }

    static final class Pkg {
        private final String name;
        private final Version version;
        private final Location location;
        private final List<Dependency> depends = new ArrayList<>();
        private final List<Artifact> artifacts = new ArrayList<>();

        Pkg(String name, String version, Location location) {
            this.name = name;
            this.version = Version.of(version);
            this.location = location;
        }

        Pkg needs(String pkg, String atLeast) {
            depends.add(Dependency.atLeast(pkg, atLeast));
            return this;
        }

        Pkg needsUpTo(String pkg, String atLeast, String atMost) {
            depends.add(new Dependency(pkg, Version.of(atLeast), Version.of(atMost)));
            return this;
        }

        /** a module of the package itself */
        Pkg own(String module, Set<String> javaPackages, Set<String> requires) {
            artifacts.add(new Artifact(module, version, Path.of(name, module + ".jar"), Kind.OWN, javaPackages, requires));
            return this;
        }

        /** a library jar the package brings */
        Pkg lib(String module, String libVersion, Set<String> javaPackages, Set<String> requires) {
            artifacts.add(new Artifact(module, Version.of(libVersion), Path.of(name, "lib", module + "-" + libVersion + ".jar"),
                    Kind.LIBRARY, javaPackages, requires));
            return this;
        }

        InstalledPackage build() {
            return new InstalledPackage(name, version, location, List.copyOf(depends), List.copyOf(artifacts));
        }
    }
}
