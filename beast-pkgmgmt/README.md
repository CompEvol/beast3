# beast-pkgmgmt

Package management module for BEAST 3. Handles discovery, installation, and
runtime loading of external BEAST packages.

## Design principles

1. **One module layer per package.** Each external package is loaded into its
   own JPMS `ModuleLayer`, a child of the boot layer. This gives namespace
   isolation — two packages can bundle different versions of the same library
   without conflict — while still letting every package see the core BEAST API.

2. **Same code path for development and deployment.** A developer running BEAST
   and their package together in an IDE (both on the module path) sees exactly
   the same service discovery as an end user who installed the package from a
   ZIP or Maven coordinate. The `module-info.java` `provides` declarations are
   the single source of truth in both cases.

3. **Graceful degradation.** When a dependency is unavailable (e.g. JavaFX on a
   headless cluster), the unsatisfied modules are silently skipped rather than
   failing the whole application. A package with both a core module and an FX
   module will load the core module and skip the FX module automatically.

4. **First-found wins, no silent duplicates.** When the same JPMS module name
   appears in more than one location (boot layer, installed package, archive),
   the first one encountered is loaded and all subsequent copies are skipped.
   This prevents "reads more than one module" errors and ensures deterministic
   behaviour.

5. **Dual distribution.** Packages can be distributed as traditional ZIP
   archives (submitted to CBAN) or as standard Maven Central JARs. Both
   formats converge on the same `ModuleLayer` creation path, so there is no
   runtime difference between them.

## Design overview

BEAST 3 uses the Java Platform Module System (JPMS). The core application ships
as three modules — `beast.pkgmgmt`, `beast.base`, and `beast.fx` — loaded in
the boot `ModuleLayer`. External packages are loaded into **plugin
`ModuleLayer`s** at startup, one layer per package, so that each package's
classes are visible to the BEAST runtime without modifying the boot layer.

### Key classes

| Class | Role |
|-------|------|
| `PackageManager` | Package installation, dependency checking, and `ModuleLayer` creation |
| `BEASTClassLoader` | Facade for class loading and service registry across all layers |
| `MavenPackageResolver` | Resolves Maven coordinates to local JAR paths via Apache Maven Resolver |

### Package distribution formats

Packages can be distributed and installed in two ways. Both live under
`~/.beast/2.8/` and both go through the same `ModuleLayer` creation path,
but they use different storage models:

1. **ZIP packages** (legacy) — the traditional format. A ZIP contains
   `version.xml`, JARs in `lib/`, optional `fxtemplates/`, and `examples/`.
   Installed to `~/.beast/2.8/<PackageName>/`. The directory structure *is*
   the index: any subdirectory containing a `version.xml` is treated as a
   package. No external index file is needed.

2. **Maven packages** (recommended) — resolved by coordinate
   (`groupId:artifactId:version`). Three things are involved:

   | What | Where | Purpose |
   |------|-------|---------|
   | **Install list** | `~/.beast/2.8/maven-packages.xml` | Which coordinates the user installed |
   | **Local cache** | `~/.beast/2.8/maven-repo/` | Downloaded JARs (package + transitive deps) |
   | **Remote repos** | Maven Central + `beauti.properties` | Where to fetch JARs from |

   At startup, each coordinate in `maven-packages.xml` is resolved via
   `MavenPackageResolver`: the resolver checks the local cache first, and
   downloads from the remote repositories if needed. The local cache is a
   flat Maven layout containing both package JARs and their transitive
   dependencies — you cannot tell which JARs constitute "a package" by
   looking at the directory, which is why the explicit `maven-packages.xml`
   index is needed.

   Uninstalling a Maven package removes it from `maven-packages.xml`; the
   cached JARs are left in place. Custom repositories are stored as a
   comma-separated list in `beauti.properties` (key `maven.repositories`)
   and are searched alongside Maven Central.

## Development vs user environment

The package manager is designed so that development and deployment use the same
discovery mechanism. The difference is only in *where* modules come from.

### Development (IDE)

When you open both BEAST 3 and your external package as modules in IntelliJ (or
any JPMS-aware IDE), the IDE places all modules on a single module path. BEAST
discovers your package's services via the `provides` declarations in your
`module-info.java` — no ZIP, no `version.xml` parsing, and no runtime
`ModuleLayer` creation needed. Your classes are in the boot layer alongside
BEAST's own modules.

This means you can set breakpoints, hot-reload, and debug your package as if it
were part of BEAST itself.

### User installation (runtime)

End users install packages via BEAUti or the command line. Installed packages
live outside the boot layer and are loaded at startup into child
`ModuleLayer`s. The package manager scans a series of directories to find them.

### Directory search order (ZIP packages)

`PackageManager.getBeastDirectories()` builds the list of candidate
directories for ZIP package discovery. Only the first two are needed for
BEAST 3; the rest are deprecated legacy paths retained temporarily for
backward compatibility:

1. **`BEAST_PACKAGE_PATH`** — environment variable or `-D` system property.
   Colon-separated list of directories. Useful for CI or custom layouts.
2. **User package directory** — `~/.beast/2.8/` (platform-specific).
3. ~~**System package directory**~~ *(deprecated)* — e.g. `/opt/beast/` on
   Linux. No known users.
4. ~~**BEAST installation directory**~~ *(deprecated)* — located by finding
   the JAR that contains `PackageManager` and navigating up two levels.
   Redundant with JPMS boot layer.
5. ~~**Classpath-derived directories**~~ *(deprecated)* — non-JAR entries on
   `java.class.path`. Was needed pre-JPMS for IDE runs; now redundant
   because IDE-launched runs place all modules in the boot layer via the
   module path.
6. ~~**Archive directory**~~ *(deprecated)* — `~/.beast/2.8/archive/`.
   Implicit rollback adds complexity; explicit reinstall of a specific
   version is preferred.

Within each directory, subdirectories containing a `version.xml` are treated
as packages.

A warning is logged at startup when a package is loaded from a deprecated
path (steps 3–6), advising the user to reinstall to `~/.beast/2.8/` or as a
Maven package. These paths will be removed in a future release.

### Package loading precedence

When the same JPMS module name appears in more than one location, the first
one loaded wins and all subsequent copies are skipped. The full precedence
order is:

1. **Boot layer** — Core BEAST modules (`beast.pkgmgmt`, `beast.base`,
   `beast.fx`) and their transitive dependencies (commons-math, colt, etc.)
   are already loaded before any package scanning begins. Any installed
   package that bundles the same module is silently skipped.

2. **Maven packages** — Loaded first among external packages.
   `maven-packages.xml` is read and each coordinate is resolved via
   `MavenPackageResolver`. Maven is the recommended distribution format
   going forward, so Maven packages take precedence over legacy ZIP
   packages.

3. **ZIP packages** — Scanned from the directory search order listed above
   (`BEAST_PACKAGE_PATH` → user dir → system dir → install dir → classpath →
   archive). Any module already loaded by a Maven package (or the boot
   layer) is skipped. Within ZIP packages, earlier directories in the
   search order take precedence over later ones.

This ordering means that if the same package is installed both as a Maven
coordinate and a ZIP, the Maven version takes precedence — which is the
desired behaviour during the transition from ZIP to Maven distribution.

In the development environment, all modules are in the boot layer (tier 1),
so installed ZIP and Maven copies are automatically skipped — a developer
will never have their IDE version conflict with an old installed version.

## Startup sequence

When BEAST starts, `PackageManager.loadExternalJars()` runs:

1. **Process pending installs/deletes** from previous BEAUti sessions
2. **Load Maven packages** — read `maven-packages.xml`, resolve each
   coordinate via `MavenPackageResolver`, and create a `ModuleLayer` per
   package. Module names are recorded in the "already loaded" set.
3. **Scan package directories** — for each installed ZIP package, find its
   JARs, parse `version.xml` for service declarations, and call
   `createAndRegisterModuleLayer()`. Modules already loaded in step 2 are
   skipped, so Maven packages take precedence over legacy ZIP packages.

## ModuleLayer creation

`PackageManager.createAndRegisterModuleLayer()` is the core method. Given a
list of JAR paths (from a ZIP package or Maven resolution):

1. **Discover modules** — use `ModuleFinder` to read `module-info.class` from
   each JAR
2. **Exclude already-loaded modules** — skip any module whose name already
   appears in the boot layer or a previously registered plugin layer (avoids
   "reads more than one module" errors)
3. **Filter unsatisfiable modules** — check each candidate module's `requires`
   against available modules. If a required module is not present (e.g.
   `beast.fx` or `javafx.controls` when running headless), that module is
   skipped rather than failing the entire package. This allows core modules
   to load even when the FX module cannot.
4. **Resolve and create layer** — call `Configuration.resolveAndBind()` on the
   filtered set, then `ModuleLayer.defineModulesWithOneLoader()` to create the
   layer
5. **Register with BEASTClassLoader** — the new layer's class-loaders and
   service providers become available for class lookup and service discovery

### Headless mode

When BEAST runs without JavaFX (e.g. on a cluster, or via the `beast-base`
module only), external package FX modules are automatically skipped at step 3.
For example, a package with both `beast-morph-models` and
`beast-morph-models-fx` JARs will load the core module successfully and skip
the FX module with a message:

```
Skipping modules with unsatisfied dependencies in .../MM: [beast.morph.models.fx]
```

No special configuration or flags are needed.

## Service discovery

BEAST uses a dual service discovery mechanism:

1. **Primary: `module-info.java` `provides` declarations** — the recommended
   approach for BEAST 3 packages. When a `ModuleLayer` is created, all
   `provides` declarations from its modules are merged into the
   `BEASTClassLoader` service registry. IDEs like IntelliJ resolve these
   automatically from the module path.

2. **Supplementary: `version.xml`** — each package includes a `version.xml`
   listing `<service>` entries with `<provider>` elements. This is parsed at
   startup and used alongside module descriptors. It is required for backward
   compatibility mappings (`<map>` elements) and for declaring dependencies
   (`<depends>`).

Both mechanisms feed into the same registry in `BEASTClassLoader`. When code
calls `BEASTClassLoader.loadService(BEASTInterface.class)`, all providers
from both sources are returned.

### How services are merged

When a package has both `module-info.java` `provides` declarations and
`version.xml` `<service>` entries, both are registered. The service sets are
unioned — duplicates are harmlessly deduplicated via `Set.add()`.

For the class-loader map (used by `BEASTClassLoader.forName()`), the
first-registered loader wins (`putIfAbsent`). Since Maven packages are
loaded before ZIP packages, Maven takes precedence when both formats provide
the same service class. Within a single package, `version.xml` services are
registered before `module-info.java` services, but because they share the
same `ModuleLayer` class-loader this has no practical effect.

## Class loading

`BEASTClassLoader.forName(className)` searches in order:

1. **Explicit class→loader map** — populated from `version.xml` services and
   `module-info.java` `provides` declarations. First-registered wins, so
   Maven packages take precedence over ZIP packages for the same class.
2. **Plugin `ModuleLayer`s** — iterates each registered layer's modules
3. **Context / system class-loader** — covers the boot layer

This means classes from external packages are found without any classpath
manipulation — the plugin `ModuleLayer` mechanism handles isolation and
visibility.

## Package structure conventions

External packages follow the conventions in
[beast-package-skeleton](https://github.com/CompEvol/beast-package-skeleton):

- **Two-module layout** for packages with GUI components: a core module
  (no JavaFX dependency) and an FX module (BEAUti editors, templates)
- **Single-module layout** for headless-only packages
- `version.xml` at project root with `<depends on='BEAST.base' atleast='2.8.0'/>`
- `module-info.java` with `provides beast.base.core.BEASTInterface with ...`
- JARs in `lib/` within the ZIP, fxtemplates in `fxtemplates/`

### Resource directory namespacing

Resources that live under `src/main/resources/` or `src/test/resources/` must
be placed under a directory named after the JPMS module (using dots, not
slashes). For example:

```
src/main/resources/my.beast.example/fxtemplates/MyTemplate.xml
src/test/resources/my.beast.example/examples/mypackage.xml
```

This prevents JPMS **split package** errors when multiple modules are loaded
in the same boot layer (as happens in Eclipse when developing a package
alongside beast3). Eclipse patches `target/test-classes/` into each module
via `--patch-module`, so any top-level directory that appears in two modules
(e.g. `examples/` or `fxtemplates/`) is treated as a split package and
rejected.

The `<module.name>/` prefix ensures each module owns its resource
directories. The pattern is consistent across the project: beast-base uses
`beast.base/examples/`, beast-fx uses `beast.fx/fxtemplates/`.

In assembly descriptors (for ZIP distribution), map the namespaced source
path back to a flat output path, since installed ZIPs are not JPMS modules:

```xml
<fileSet>
    <directory>${project.basedir}/src/test/resources/my.beast.example/examples</directory>
    <outputDirectory>/examples</outputDirectory>
</fileSet>
```

## Maven package resolution

`MavenPackageResolver` uses Apache Maven Resolver (declared as `requires
static` — optional at runtime) to:

1. Resolve a coordinate's full transitive dependency tree
2. Download artifacts to a local cache (`~/.beast/2.8/maven-repo/`)
3. Filter out JARs whose JPMS module is already loaded in the boot or
   plugin layers (prevents duplicate module errors for beast-base,
   commons-math, etc.)
4. Return the filtered list of JAR paths for `ModuleLayer` creation

Custom Maven repositories can be added via:
```
packagemanager -addMavenRepository https://example.com/maven/
```
