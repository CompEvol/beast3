# beast-pkgmgmt

Package management module for BEAST 3. Handles discovery, installation, and runtime loading of
external BEAST packages.

For architecture, JPMS internals, and design decisions see
[PackageManagerDesign.md](PackageManagerDesign.md).

---

## Installing Packages

Packages can be installed from two sources: the **CBAN repository** (ZIP format) or
**Maven Central** (Maven format). Both end up under `~/.beast/2.8/` and are loaded the same
way at startup. Maven is the recommended format going forward.

### ZIP packages from CBAN

```sh
# Install to user directory (default)
packagemanager -add SNAPP

# Install to system directory (shared across users)
packagemanager -useAppDir -add SNAPP

# Install to a custom directory
packagemanager -dir /opt/beast -add SNAPP

# Install a specific version (both versions co-exist under archive/)
packagemanager -add SNAPP -version 1.3.2

# Uninstall
packagemanager -del SNAPP

# List available packages
packagemanager -list

# List installed packages
packagemanager -list -installed
```

### Install paths

| Mode | Linux / Unix | macOS | Windows |
|------|--------------|-------|---------|
| **User** (default) | `~/.beast/2.8/` | `~/Library/Application Support/BEAST/2.8/` | `%USERPROFILE%\BEAST\2.8\` |
| **System** (`-useAppDir`) | `/usr/local/share/beast/2.8/` | `/Library/Application Support/BEAST/2.8/` | `\Program Files\BEAST\2.8\` |
| **Custom** (`-dir DIR`) | `DIR/` | `DIR/` | `DIR/` |

### Maven packages

```sh
# Install by coordinate
packagemanager -maven io.github.compevol:beast-morph-models:1.3.0

# Uninstall (cached JARs in maven-repo/ are left in place)
packagemanager -delMaven io.github.compevol:beast-morph-models

# List installed Maven packages
packagemanager -listMaven

# Add a custom Maven repository
packagemanager -addMavenRepository https://beast2.org/maven/

# List configured Maven repositories
packagemanager -listMavenRepositories
```

Maven Central is always included. Custom repositories are stored in `beauti.properties` under
the key `maven.repositories` as a comma-separated list.

---

## Headless Mode

When BEAST runs without JavaFX (e.g. on a cluster), FX modules are automatically skipped during
package loading. A package with both a core and an FX module loads the core and skips the FX
module with a message:

```
Skipping modules with unsatisfied dependencies in .../MM: [beast.morph.models.fx]
```

No special configuration or flags are needed.

---

## Writing a Package

External packages follow the conventions in
[beast-package-skeleton](https://github.com/CompEvol/beast-package-skeleton):

- **Two-module layout** for packages with GUI components: a core module (no JavaFX dependency)
  and an FX module (BEAUti editors and templates). Headless installs load only the core module.
- **Single-module layout** for headless-only packages.
- `version.xml` at project root with `<depends on='BEAST.base' atleast='2.8.0'/>`.
- `module-info.java` with `provides beast.base.core.BEASTInterface with ...`.
- JARs in `lib/` within the ZIP; fxtemplates in `fxtemplates/`.

### Resource directory namespacing

Resources under `src/main/resources/` or `src/test/resources/` must be placed under a directory
named after the JPMS module (using dots):

```
src/main/resources/my.beast.example/fxtemplates/MyTemplate.xml
src/test/resources/my.beast.example/examples/mypackage.xml
```

This prevents JPMS **split package** errors when multiple modules share the boot layer (as in
Eclipse with `--patch-module`). The pattern is consistent across the project: `beast-base` uses
`beast.base/examples/`, `beast-fx` uses `beast.fx/fxtemplates/`.

In assembly descriptors (ZIP distribution), map the namespaced source path back to a flat output
path, since installed ZIPs are not JPMS modules:

```xml
<fileSet>
    <directory>${project.basedir}/src/test/resources/my.beast.example/examples</directory>
    <outputDirectory>/examples</outputDirectory>
</fileSet>
```

---

## Testing Your Package

Maven Surefire runs tests on the classpath, not the JPMS module path, so the primary service
discovery mechanism (module descriptors) does not work during tests. Service discovery falls back
to `version.xml` scanning.

### Required POM configuration

```xml
<dependencies>
    <!-- beast3 core (compile scope) -->
    <dependency>
        <groupId>io.github.compevol</groupId>
        <artifactId>beast-base</artifactId>
        <version>${beast.version}</version>
    </dependency>

    <!-- Test utilities: BEASTTestCase, TestOperator, etc.
         Uses 'optional' scope so it lands on the module path
         at compile time (required for JPMS visibility). -->
    <dependency>
        <groupId>io.github.compevol</groupId>
        <artifactId>beast-test-utils</artifactId>
        <version>${beast.version}</version>
        <optional>true</optional>
    </dependency>
    <!-- Add 'requires static beast.test.utils' to your module-info.java -->
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <argLine>
                    --add-reads YOUR.MODULE=ALL-UNNAMED
                    --add-reads YOUR.MODULE=beast.test.utils
                    --add-reads beast.base=ALL-UNNAMED
                    --add-reads beast.pkgmgmt=ALL-UNNAMED
                </argLine>
                <systemPropertyVariables>
                    <!-- Point at your own version.xml AND the beast-base JAR
                         so that beast.base services (DataType, etc.) are
                         discovered via version.xml scanning. -->
                    <BEAST_PACKAGE_PATH>
                        ${project.build.outputDirectory}:${settings.localRepository}/io/github/compevol/beast-base/${beast.version}/beast-base-${beast.version}.jar
                    </BEAST_PACKAGE_PATH>
                </systemPropertyVariables>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Why this is needed

1. **`BEAST_PACKAGE_PATH`** — Surefire runs on the classpath, so the two-parent-directory walk
   in `initServices()` does not find beast-base's `version.xml` (it is inside a JAR in `~/.m2/`).
   Pointing `BEAST_PACKAGE_PATH` at the JAR triggers the JAR-internal `version.xml` scan.

2. **`--add-reads`** — Even though Surefire uses the classpath, JPMS module declarations are
   still partially enforced. These flags allow test code to access the unnamed module (where JUnit
   and beast-test-utils live at runtime).

3. **`beast-test-utils`** — Provides `BEASTTestCase` and `TestOperator` in a proper JPMS module
   (`beast.test.utils`). This avoids split-package errors: if your tests were in package
   `test.beast`, they would clash with the `test.beast` package exported by `beast.test.utils`.
   Use subpackages like `test.beast.evolution.likelihood` (fine) or `test.yourpackage` (safest).

### Split package pitfall

Do not put test classes directly in the `test.beast` package. The `beast.test.utils` module
exports `test.beast`, so any other module with classes in `test.beast` triggers a JPMS
split-package error. Subpackages like `test.beast.core` or `test.beast.evolution.tree` are fine
because JPMS treats each dotted package name as independent.

---

## Embedding the Package Manager

BEAST 3's package manager can be embedded in standalone applications (e.g. LPhyBEAST) with their
own isolated package cache, independent of the default `~/.beast/` directory.

### Application-specific package directories

`Utils6.getPackageUserDir(String application)` returns a platform-specific directory:

| Platform | `getPackageUserDir("LPhyBEAST")` |
|----------|----------------------------------|
| macOS    | `~/Library/Application Support/LPhyBEAST/<majorVersion>/` |
| Linux    | `~/.lphybeast/<majorVersion>/` |
| Windows  | `%USERPROFILE%\LPhyBEAST\<majorVersion>\` |

Override with the system property `<prefix>.user.package.dir` (lowercase application name).

### Bootstrap sequence

```java
// 1. Redirect the package directory before any PackageManager call
String pkgDir = Utils6.getPackageUserDir("LPhyBEAST");
System.setProperty("beast.user.package.dir", pkgDir);

// 2. Load installed packages (Maven + ZIP) into ModuleLayers
PackageManager.loadExternalJars();

// 3. Discover services from version.xml files on classpath / BEAST_PACKAGE_PATH
BEASTClassLoader.initServices();
```

After this, `BEASTClassLoader.forName()` and `BEASTClassLoader.loadService()` will find classes
from both the boot layer and any installed packages.

### Key API methods

| Method | Description |
|--------|-------------|
| `PackageManager.loadExternalJars()` | Load all installed packages into child `ModuleLayer`s. Call once at startup. |
| `PackageManager.installMavenPackage(groupId, artifactId, version)` | Install a Maven package; downloads JARs and adds to `maven-packages.xml`. |
| `PackageManager.uninstallMavenPackage(groupId, artifactId)` | Remove from `maven-packages.xml`; cached JARs left in place. |
| `PackageManager.addInstalledPackages(Map<String, Package>)` | Populate map with all currently installed packages (Maven and ZIP). |
| `PackageManager.addAvailablePackages(Map<String, Package>)` | Populate map with packages available from remote CBAN. |
| `BEASTClassLoader.initServices()` | Scan classpath and `BEAST_PACKAGE_PATH` for `version.xml` and register services. |

### Running BEAST headlessly

`beast.base.minimal.BeastMCMC` provides a headless MCMC runner with no JavaFX dependency:

```java
BeastMCMC beast = new BeastMCMC();
beast.parseArgs(new String[]{"-seed", "777", "-threads", "2", "output.xml"});
beast.run();
```

`parseArgs` accepts: `-seed <long|random>`, `-threads <int>`, `-resume`, `-overwrite`,
`-prefix <name>`, `-D <key=value,...>`, and a positional XML/JSON file path.

---

## Reference

### Package distribution formats

| Format | Storage | Index |
|--------|---------|-------|
| **ZIP** (legacy) | `~/.beast/2.8/<PackageName>/` — contains `version.xml`, `lib/*.jar`, `fxtemplates/`, `examples/` | Any subdirectory with `version.xml` is treated as a package |
| **Maven** (recommended) | `~/.beast/2.8/maven-repo/` (local cache) | `~/.beast/2.8/maven-packages.xml` lists which coordinates are installed |

### Directory search order

`PackageManager.getBeastDirectories()` searches these locations in order:

1. **`BEAST_PACKAGE_PATH`** — environment variable or `-D` system property. Useful for CI or
   custom layouts.
2. **User package directory** — `~/.beast/2.8/` (platform-specific, see install paths above).
3. ~~**System package directory**~~ *(deprecated)* — e.g. `/opt/beast/` on Linux.
4. ~~**BEAST installation directory**~~ *(deprecated)* — redundant with JPMS boot layer.

Within each directory, subdirectories containing a `version.xml` are prepended to the list and
visited first, so individual package directories take precedence over their parent containers.

### Design principles

1. **One module layer per package.** Each external package is loaded into its own JPMS
   `ModuleLayer`, giving namespace isolation while still letting every package see the core
   BEAST API.

2. **Same code path for development and deployment.** A developer running BEAST and their
   package together in an IDE sees exactly the same service discovery as an end user who
   installed the package from a ZIP or Maven coordinate. The `module-info.java` `provides`
   declarations are the single source of truth in both cases.

3. **Graceful degradation.** When a dependency is unavailable (e.g. JavaFX on a headless
   cluster), unsatisfied modules are silently skipped rather than failing the whole application.

4. **First-found wins, no silent duplicates.** When the same JPMS module name appears in more
   than one location (boot layer, installed package), the first one encountered is loaded and
   all subsequent copies are skipped. This prevents "reads more than one module" errors and
   ensures deterministic behaviour.

5. **Dual distribution.** ZIP archives (CBAN) and Maven Central JARs both converge on the same
   `ModuleLayer` creation path — no runtime difference between them.

### Key classes

| Class | Role |
|-------|------|
| `PackageManager` | Orchestrator: package discovery, JPMS `ModuleLayer` creation, Maven integration |
| `BEASTClassLoader` | Facade for class loading and service registry across all layers |
| `MavenPackageResolver` | Resolves Maven coordinates to local JAR paths via Apache Maven Resolver |
| `DependencyResolver` | Resolves transitive package dependencies and checks installed dependency health |
| `PackageRepository` | Fetches available-package lists from CBAN repository URLs |
| `PackageInstaller` | Installs and uninstalls ZIP packages |
| `PackageManagerCLI` | CLI entry point (`main()`) |
