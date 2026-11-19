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
packagemanager -maven io.github.compevol:morph-models:1.3.1

# Uninstall (cached JARs in maven-repo/ are left in place)
packagemanager -delMaven io.github.compevol:morph-models

# List installed Maven packages
packagemanager -listMaven

# Add a custom Maven repository
packagemanager -addMavenRepository https://beast2.org/maven/

# List configured Maven repositories
packagemanager -listMavenRepositories

# Show what BEAST loads from what is installed, and why the rest is not loaded
packagemanager -plan
```

An install downloads the package (never BEAST's own core or JavaFX, which always come from the
installed BEAST), checks that BEAST would load it, and records it. It is loaded when BEAST next
starts. If BEAST would not load it, for example because it needs another BEAST version, the
install is refused with the reason.

Maven Central is always included. Custom repositories are stored in `beauti.properties` under
the key `maven.repositories` as a comma-separated list.

---

## Headless Mode

When BEAST runs without JavaFX (e.g. on a cluster), FX modules are automatically skipped during
package loading. A package with both a core and an FX module loads the core and skips the FX
module with a message:

```
Not loading module beast.morph.models.fx of MM: headless
```

BEAST.app itself is left out the same way. A package that declares a dependency on BEAST.app
still loads its core module.

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

### version.xml in a Maven package

A package installed from Maven has no package directory, so its `version.xml` goes inside
its main JAR (the one named `<artifactId>-<version>.jar`), at either of:

- `version.xml` at the JAR root (as in [CCD](https://github.com/CompEvol/CCD)), or
- `META-INF/beast/version.xml`, if the root is taken.

The root is read first. BEAST reads the package name, the services (`<service>`) and the apps
(`<packageapp>`) from it, so `applauncher -list` shows the apps of a Maven package too. Only
the main JAR is read; a `version.xml` in one of its dependencies is not. A Maven package's
dependencies come from its POM, not from `version.xml` (next section).

### Declaring the BEAST versions a package works with

Each kind of package states its dependencies in one place:

| Package | Dependencies on BEAST and other BEAST packages | Libraries |
|---|---|---|
| ZIP | `version.xml`: `<depends on='BEAST.base' atleast='2.8.0'/>` (`atmost` optional) | jars in `lib/` |
| Maven | its POM: the `beast-base` / `beast-fx` dependency, and other BEAST packages | its POM |

For a Maven package, give the core as a version range. For a package built for BEAST 2.8:

```xml
<dependency>
    <groupId>io.github.compevol</groupId>
    <artifactId>beast-base</artifactId>
    <version>[2.8.0-rc3,2.9.0-alpha)</version>
</dependency>
```

This means any 2.8.x from 2.8.0-rc3 on, and nothing from 2.9, including 2.9 pre-releases. The
upper bound must be `2.9.0-alpha`: `[2.8.0-rc3,2.9)` would let in `2.9.0-SNAPSHOT` and
`2.9.0-rc1`, which Maven orders before `2.9`. A package that depends on `beast-fx` gives it the
same range.

A plain version (e.g. `2.8.0-rc1`) or an `atleast` without `atmost` is a minimum within the
same minor version: a package built for 2.8.x is not loaded on 2.9, since minor releases of
BEAST have broken packages. BEAST never downloads its own core for a package; it checks the
installed core against the range. Where `version.xml` and the POM disagree, the POM counts for a
Maven package; `packagemanager -plan` warns about it, and about services declared only in
`version.xml` (`provides` in `module-info.java` is what counts).

To put the project's `version.xml` at the JAR root, copy it into the classes directory
before packaging, as CCD does:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>embed-version-xml-in-jar</id>
            <phase>generate-resources</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                <resources>
                    <resource>
                        <directory>${project.basedir}</directory>
                        <includes>
                            <include>version.xml</include>
                        </includes>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

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

### Where packages are found

| Location | Used |
|---|---|
| Boot layer (developer mode: `mvn exec`, an IDE) | Always: BEAST's own core when run from source |
| `BEAST_PACKAGE_PATH` (environment variable or `-D` property) | Whatever its version, e.g. a package under development |
| User package directory (`~/.beast/2.8/`, platform-specific, see above), and Maven packages | Newest version first |
| System package directory, BEAST installation directory | Newest version first |
| `archive/<name>/<version>/` in the user directory (`-add NAME -version V`) | Only when no other copy is, or when `beast -strictversions` asks for that version |

### How BEAST decides what to load

All decisions are made in one place, the planner (`beast.pkgmgmt.plan.Planner`), before
anything is loaded. Its result says which jar provides each module and why every package or
copy that is left out is left out (`packagemanager -plan`). The rules:

1. **BEAST's own core and JavaFX come only from BEAST.** `beast.base`, `beast.fx`,
   `beast.pkgmgmt`, JavaFX and the JRE's own modules are never taken from a package's
   dependencies. BEAST.base and BEAST.app can be upgraded as packages, within the major version
   of the installer; a core upgrade that needs a newer installer is not used.
2. **Of several copies of a package, the first that works is used** (table above). When a
   package needs a different version of a dependency than the copy tried, another copy that
   fits is tried before the package is left out.
3. **Libraries:** versions with the same major version share one copy, the highest. Packages
   that need incompatible (different major) versions each get their own copy, in a module layer
   with the packages that use it and those that build on them. A package that would need two
   such copies is left out, naming the library.
4. **Each Java package is in exactly one module**, so a class name always means one class. A
   package that would put a Java package in a second module is left out, naming both.
5. **Headless** (no JavaFX), modules that need JavaFX or `beast.fx` are left out; the rest of
   their package loads.
6. **One version order everywhere:** Maven's, for ZIP and Maven packages alike.
7. **`beast -strictversions`** loads the package versions an analysis was made with (its
   `required` attribute), from the archive, and no other packages; a missing version is
   reported with the command that installs it.

The loader then loads the plan in one go: the shared modules in one module layer on top of the
boot layer, and a child layer for each set of packages with their own copy of a library. If the
JVM still rejects the plan, BEAST starts without its packages rather than not at all.

### Key classes

| Class | Role |
|-------|------|
| `PackageManager` | Entry points: load the installed packages, install and uninstall, list |
| `plan.Inventory` | Reads what is installed (ZIP folders, archive, Maven packages) into one model |
| `plan.Planner` | Decides what to load, with a reason for everything left out |
| `plan.Loader`, `plan.LoadedPackages` | Loads a plan as module layers; class and service lookup |
| `plan.Version`, `plan.Dependency` | Maven's version order and ranges, without the resolver |
| `BEASTClassLoader` | Class and service lookup used by BEAST's code; the loaded layers are registered with it |
| `MavenPackageResolver` | Downloads Maven packages and their libraries (never BEAST's core or JavaFX) |
| `DependencyResolver` | Chooses the versions to install, with the planner |
| `PackageRepository` | Fetches available-package lists from CBAN repository URLs |
| `PackageInstaller` | Installs and uninstalls ZIP packages |
| `PackageManagerCLI` | CLI entry point (`main()`) |
