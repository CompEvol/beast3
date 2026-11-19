package beast.pkgmgmt.plan;

import java.util.List;
import java.util.Set;

/**
 * What the package manager cannot change: it comes with the BEAST installer.
 *
 * @param launcher    the version of {@code beast.pkgmgmt}, on the boot layer
 * @param javafx      the JavaFX version of the JRE, or null when there is none (headless)
 * @param bootModules the modules on the boot layer besides {@code java.*} and {@code jdk.*}:
 *                    {@code beast.pkgmgmt} and its libraries, and in developer mode the core
 */
public record Runtime(Version launcher, Version javafx, List<BootModule> bootModules) {

    public record BootModule(String name, Set<String> javaPackages) {}

    public boolean hasJavaFX() {
        return javafx != null;
    }
}
