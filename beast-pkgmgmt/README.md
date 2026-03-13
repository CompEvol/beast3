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

2. **Maven packages** (recommended) — resolved from Maven Central (or custom
   repositories) by coordinate (`groupId:artifactId:version`). JARs are
   cached in `~/.beast/2.8/maven-repo/`, which is a flat Maven cache
   containing both package JARs and their transitive dependencies. Because
   the cache structure is opaque (you cannot tell which JARs constitute "a
   package" by looking at the directory), an explicit index file
   `maven-packages.xml` records which coordinates the user installed.
   Uninstalling a Maven package removes it from the index; the cached JARs
   are left in place as a cache.

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

### Directory search order

`PackageManager.getBeastDirectories()` currently builds the list of candidate
directories in this order:

1. **`BEAST_PACKAGE_PATH`** — environment variable or `-D` system property.
   Colon-separated list of directories. Useful for CI or custom layouts.
2. **User package directory** — `~/.beast/2.8/` (platform-specific).
3. **System package directory** — e.g. `/opt/beast/` on Linux.
4. **BEAST installation directory** — located by finding the JAR that contains
   `PackageManager` and navigating up two levels.
5. **Classpath-derived directories** — non-JAR entries on `java.class.path`,
   excluding IDE build paths like `out/production/`. This is how IDE-launched
   runs pick up development packages that aren't in the standard install
   locations.

Within each directory, subdirectories containing a `version.xml` are treated as
packages.

6. **Archive directory** — `~/.beast/2.8/archive/`. Old package versions are
   stored here. Only the latest archived version of each package is considered,
   and only if that package has not already been found in one of the earlier
   directories.

### Proposed simplification

In the development environment, all modules (BEAST core + external packages)
are on the IDE module path in the boot layer. The package manager has nothing
to do — Maven dependencies come from `~/.m2/` via the IDE, and service
discovery works through `module-info.java` `provides` declarations.

In the user environment, only `beast.pkgmgmt`, `beast.base`, and `beast.fx`
are in the boot layer. The package manager must find and load everything else.
The current six-location search order is inherited from BEAST 2 and is more
complex than BEAST 3 needs. The classpath scanning (step 5) was needed before
JPMS but is now redundant. The system directory (step 3) has no known users.
The archive directory (step 6) adds complexity for a rarely-used rollback
feature.

A simplified search for the user environment would need only two locations:

1. **`~/.beast/2.8/`** — Maven packages (resolved to `maven-repo/` subfolder)
   and any future package format
2. **One configurable directory** — for old-style `version.xml` ZIP packages,
   defaulting to `~/.beast/2.8/` but overridable via `BEAST_PACKAGE_PATH`

This collapses the six locations to two, eliminates the reconciliation logic,
and makes the system easier to reason about. Package rollback could be handled
explicitly (reinstall a specific version) rather than implicitly through
archive scanning.

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

## Class loading

`BEASTClassLoader.forName(className)` searches in order:

1. **Explicit class→loader map** — populated from `version.xml` services and
   `module-info.java` `provides` declarations
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
