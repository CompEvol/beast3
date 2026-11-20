package beast.pkgmgmt.plan;

import java.nio.file.Path;
import java.util.Set;

/**
 * One jar of an installed package: a module the package itself provides ({@link Kind#OWN}), or
 * a library it brings ({@link Kind#LIBRARY}): a jar in a ZIP package's {@code lib/} that is not
 * one of its own modules, or a dependency resolved from a Maven package's POM.
 *
 * @param module       the module name (from {@code module-info}, or the automatic name)
 * @param version      the jar's version (Maven version, or the package's for its own modules)
 * @param jar          the file, or null in tests and for modules already on the boot layer
 * @param kind         own module or library
 * @param javaPackages the Java packages the jar contains
 * @param requires     the module's non-static {@code requires}, without {@code java.*} and {@code jdk.*}
 */
public record Artifact(String module, Version version, Path jar, Kind kind,
                       Set<String> javaPackages, Set<String> requires) {

    public enum Kind { OWN, LIBRARY }
}
