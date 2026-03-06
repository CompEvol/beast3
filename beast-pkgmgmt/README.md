# beast-pkgmgmt

Package management module for BEAST 3. Handles discovery, installation, and
runtime loading of external BEAST packages.

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

Packages can be distributed and installed in two ways:

1. **ZIP packages** — the traditional format. A ZIP contains `version.xml`,
   JARs in `lib/`, optional `fxtemplates/`, and `examples/`. Installed to
   `~/.beast/2.8/<PackageName>/` (or the directory specified by
   `BEAST_PACKAGE_PATH`).

2. **Maven packages** — resolved from Maven Central (or custom repositories)
   by coordinate (`groupId:artifactId:version`). JARs are cached in
   `~/.beast/2.8/maven-repo/` and tracked in `maven-packages.xml`.

Both formats end up going through the same `ModuleLayer` creation path.

## Startup sequence

When BEAST starts, `PackageManager.loadExternalJars()` runs:

1. **Process pending installs/deletes** from previous BEAUti sessions
2. **Scan package directories** — for each installed ZIP package, find its
   JARs, parse `version.xml` for service declarations, and call
   `createAndRegisterModuleLayer()`
3. **Load Maven packages** — read `maven-packages.xml`, resolve each
   coordinate via `MavenPackageResolver`, and create a `ModuleLayer` per
   package

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
