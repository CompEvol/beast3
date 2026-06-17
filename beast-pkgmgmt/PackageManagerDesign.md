# BEAST PackageManager — Design Report

**Date:** 2026-06-17  
**Scope:** `beast.pkgmgmt.PackageManager`, `PackageManagerCLI`, `PackageInstaller`,
`MavenPackageResolver`, `BEASTClassLoader`, `BeastLauncher`  
**Purpose:** Document the design of the package management system — two deployment environments,
JPMS architecture, load mechanisms, service registration, class discovery, and design conformance.

---

## 1. Architecture Change: `URLClassLoader` → JPMS `ModuleLayer`

Understanding this change is the prerequisite for everything else in the design.

### 1.1 The Old System (BEAST 2 — `URLClassLoader`)

In BEAST 2.6–2.7, `BEASTClassLoader` was a `URLClassLoader` subclass. Package loading was flat:
each installed package's JARs were appended to one shared URL list via `addURL()`, and every
class from every package was visible to every other package through that single classloader.

```
BEAST 2 model:
  URLClassLoader  (one flat list)
    ├── beast.base.jar
    ├── beast.app.jar
    ├── ...
    └── MyPackage.jar      ← addURL() appended here at load time
```

Five methods — `addURL()` (two overloads), `addParent()`, `addJar()` (two overloads) — drove
this model. All five are now empty no-ops retained only for binary compatibility with external
packages that still call them. They are annotated `@Deprecated(forRemoval = true)`.

Consequences of the flat model:

- **No dependency enforcement.** A missing dependency only surfaced as a `ClassNotFoundException`
  deep inside application logic at the call site.
- **No isolation.** Any code could access any class from any package.
- **Ambiguous class identity.** The same class loaded from two JARs produced two distinct
  `Class<?>` objects, causing `ClassCastException` at runtime.
- **Silent shadowing.** The first JAR on the URL list won; later JARs with the same class were
  silently ignored.

### 1.2 The New System (BEAST 3 — JPMS `ModuleLayer`)

BEAST 3 uses the **Java Platform Module System (JPMS)**. Each JAR that ships a `module-info.class`
is a *module* explicitly declaring:

- **`requires`** — which other modules it depends on
- **`exports`** — which of its packages other modules may read
- **`provides … with …`** — which service implementations it registers

Modules are grouped into **`ModuleLayer`** objects forming a parent-child tree.
The layout differs between developer mode and the released bundle:

**Developer mode** (`mvn exec:exec` — all core modules compiled onto the module path):
```
boot ModuleLayer  (JVM-managed, immutable)
  ├── java.base, java.xml, ...
  ├── beast.pkgmgmt
  ├── beast.base          ← on boot layer in dev mode only
  └── beast.fx            ← on boot layer in dev mode only

plugin ModuleLayer A   ← createAndRegisterModuleLayer()
  └── beast.snapp        (skipped — beast.base already in boot layer)
```

No plugin layers are created for core modules in dev mode — the boot-layer filter in
`createAndRegisterModuleLayer()` (step 2) detects they are already present and returns immediately.

**User/release mode** (distributed bundle — only the launcher is on the boot layer):
```
boot ModuleLayer  (JVM-managed, immutable)
  ├── java.base, java.xml, ...
  └── beast.pkgmgmt       ← only the launcher; core modules are NOT here

plugin ModuleLayer (BEAST.base)   ← seeded from bundle packages/, loaded from ~/.beast/2.8/
  └── beast.base

plugin ModuleLayer (BEAST.app)    ← seeded from bundle packages/, loaded from ~/.beast/2.8/
  └── beast.fx

plugin ModuleLayer A   ← user-installed package
  └── beast.snapp        requires beast.base  (sees boot + BEAST.base layer)

plugin ModuleLayer B
  └── beast.gammaspike   requires beast.base, beast.snapp  (sees boot + layers above)
```

Each external package gets its own child layer. A child sees all its parents; siblings cannot
see each other directly. Keeping `beast.base` and `beast.fx` out of the boot layer means they
can be upgraded via the package manager (patch releases) without requiring a new launcher build.

### 1.3 `BEASTClassLoader.forName()` — Three-Tier Lookup

The public API of `BEASTClassLoader.forName()` is unchanged from BEAST 2. Internally it now
performs a three-tier fallback that hides the multi-layer structure from all callers:

```
Tier 1: explicit class→loader map
         populated from version.xml <service> entries and module-info.java `provides`
         putIfAbsent — first registered wins (Maven packages take precedence over ZIP)

Tier 2: walk every registered plugin ModuleLayer
         iterates pluginLayers in registration order

Tier 3: context / system classloader
         covers the boot layer (beast.base, beast.fx, etc.)
```

Code written against BEAST 2's `BEASTClassLoader.forName()` works without modification.

### 1.4 Class Identity Guard — `resolveLoaderFor()`

A subtle problem arises when the same class is reachable from both the module path and the
classpath: the JVM produces two distinct `Class<?>` objects for the same name, causing
`ClassCastException` when code compiled against one copy is handed an instance of the other.
`resolveLoaderFor()` (line 551) prevents this by always returning the authoritative
module-layer classloader when the class's package is owned by a named boot-layer module:

```java
for (Module m : ModuleLayer.boot().modules()) {
    if (desc.packages().contains(pkg)) {
        return m.getClassLoader();
    }
}
```

### 1.5 What Changed vs. What Stayed the Same

| Dimension | BEAST 2 (`URLClassLoader`) | BEAST 3 (`ModuleLayer`) |
|---|---|---|
| **Structure** | One flat classloader, all JARs appended | Hierarchy of layers, one per package |
| **Dependency enforcement** | None — missing classes fail at call site | `requires` checked at load time via `resolveAndBind` |
| **Duplicate module handling** | Silent shadowing; first on URL list wins | Module name uniqueness enforced; second copy skipped |
| **Package-level isolation** | None — any code accesses any class | Modules must `exports`/`requires` explicitly |
| **Inter-package visibility** | Everything sees everything | Child sees parents; siblings need a shared ancestor |
| **Service discovery** | `version.xml` only | `module-info.java` `provides` (primary) + `version.xml` (supplement) |
| **Caller-facing API** | `BEASTClassLoader.forName()` | Same `BEASTClassLoader.forName()` — unchanged |
| **New mechanisms required** | None | Retry loop; `requires static` distinction; class-identity guard |

---

## 2. Two Environments: Developer vs. User

Two environments govern how BEAST packages are loaded at runtime:

| Env | Launch method | Package source |
|---|---|---|
| **Developer (dev)** | `mvn -pl beast-fx exec:exec ...` | Compiled modules in the Maven build (boot layer — no disk copy needed) |
| **User (release)** | `.app` or `bin/` script | i. Traditional lib path (`~/.beast/2.8/`) **or** ii. Maven release jars installed locally |

---

## 3. Implemented Code Logic

### 3.1 Environment Detection

There is **no explicit boolean flag** that switches between dev and user mode. The distinction is
inferred structurally:

- **Dev mode**: JPMS modules `beast.base`, `beast.fx`, etc. are already in the **boot layer**
  because `mvn exec:exec` places the Maven-compiled modules directly on the module path.
  `addInstalledPackages()` (`PackageManager.java:200`) detects that no `version.xml` exists on
  disk for `BEAST.base` / `BEAST.app` and falls through to the synthetic fallback at lines 270–299,
  marking both packages as installed at the current runtime version.
- **User mode**: `BeastLauncher.getPath()` (`BeastLauncher.java:437`) bootstraps the core
  packages into `~/.beast/2.8/` before any scanning, so `version.xml` is present on disk and
  the real file is parsed instead of the synthetic fallback.

### 3.2 Developer Mode — Detailed Flow

```
mvn exec:exec
  └─ boot layer already contains: beast.base, beast.fx, beast.pkgmgmt
  └─ loadExternalJarsEvenIfAlreadyLoaded()      PackageManager:644
       ├─ processDeleteList()                   (no-op: nothing to delete)
       ├─ addInstalledPackages()                synthetic BEAST.base + BEAST.app
       ├─ processInstallList()                  (no-op: no deferred installs)
       ├─ log boot-layer BEAST modules          :666–681
       ├─ loadMavenPackages()                   reads maven-packages.xml (may be empty)
       ├─ for each getBeastDirectories():
       │    loadPackage(dir)                    :831
       │      createAndRegisterModuleLayer()    :892
       │        → modules already in boot layer → skipped (candidates == empty)
       └─ checkInstalledDependencies()
```

Key behaviour: `createAndRegisterModuleLayer()` at line 907–916 collects all module names already
present in the boot layer and plugin layers, then skips any candidate module whose name matches.
Because all core modules are in the boot layer in dev mode, effectively **nothing is loaded from
disk** — consistent with the design intent.

`BEASTClassLoader.initServices()` (`BEASTClassLoader:230`) scans `java.class.path` for
`version.xml` files and also calls `initServicesFromClassLoaderResources()` (`:254`) which uses
`ClassLoader.getResources("version.xml")` to discover `version.xml` files embedded in JARs on
the module path. This is how services are registered in dev mode without disk-based packages.

### 3.3 User Mode — Option i: Traditional Path (`~/.beast/2.8/`)

```
BEAST.app or bin/beast script
  └─ BeastLauncher.main()                        BeastLauncher:36
       └─ getPath(useStrictVersions, beastFile)   :437
            ├─ seedBundledPackage("BEAST.base")   :207
            │    finds <app>/packages/BEAST.base.package*.zip
            │    compares bundled vs installed version
            │    extracts zip → ~/.beast/2.8/BEAST.base/  (if bundled is newer)
            ├─ seedBundledPackage("BEAST.app")    same logic
            ├─ PackageManager.initialise()         :624
            │    ├─ processDeleteList()
            │    ├─ addInstalledPackages()         reads version.xml from disk
            │    └─ processInstallList()
            └─ determinePackagePath()              :564
                 for each getBeastDirectories():
                   addJarsToPath(dir, classes)   collects lib/*.jar paths
       └─ BeastLauncher.run(classpath, "beastfx.app.beast.BeastMain", args)
            ├─ PackageManager.loadExternalJars()   loads each dir as a ModuleLayer
            ├─ BEASTClassLoader.initServices()
            └─ BEASTClassLoader.forName(main).getMethod("main").invoke(...)
```

`getBeastDirectories()` (`PackageManager:489`) returns directories in this priority order:
1. `BEAST_PACKAGE_PATH` env/system property (CI or custom override)
2. `~/.beast/2.8/` (Linux), `~/Library/Application Support/BEAST/2.8/` (macOS),
   `%USERPROFILE%\BEAST\2.8\` (Windows)
3. System-wide directory (admin-managed cluster installs)
4. BEAST install directory (parent of launcher JAR)

After building this list, subdirectories of each entry that contain a `version.xml` are
**prepended** — they come before their parent container in the final list, so individual package
directories are visited first and take precedence.

Legacy `installBEASTPackage()` (`BeastLauncher:55`) is the fallback when no bundled zip is found
(e.g. running from an older distribution without package zips). It copies three files —
`<name>.jar`, `<name>.version.xml` → `version.xml`, `<name>.src.jar` — from the app bundle's
`lib/packages/` directory into `~/.beast/2.8/<name>/`.

### 3.4 User Mode — Option ii: Maven Release Jars

This is the **new feature**. Config lives in `~/.beast/2.8/maven-packages.xml`:

```xml
<packages>
    <package groupId="io.github.compevol" artifactId="beast-morph-models" version="1.3.0"/>
</packages>
```

**Install** (CLI):
```
packagemanager -maven io.github.compevol:beast-morph-models:1.3.0
```
→ `PackageManager.installMavenPackage()` (`PackageManager:1141`):
1. Creates `MavenPackageResolver` with local cache at `~/.beast/2.8/maven-repo/`.
2. Resolves coordinate via Eclipse Aether → downloads to local cache.
3. Appends coordinate to `maven-packages.xml`.
4. Loads into current runtime via `createAndRegisterModuleLayer()`.

**Startup loading** — `loadMavenPackages()` (`PackageManager:1109`):
1. Reads all entries from `maven-packages.xml`.
2. Resolves each coordinate (cache hit if already downloaded).
3. Parses `version.xml` from inside the artifact JAR (root or `META-INF/beast/version.xml`).
4. Loads into a new JPMS `ModuleLayer` via `createAndRegisterModuleLayer()`.

Maven packages are loaded **before** legacy ZIP packages in `loadExternalJarsEvenIfAlreadyLoaded()`
(`:691` then `:697`). This ensures Maven takes precedence: if both a Maven JAR and a legacy ZIP
provide the same JPMS module name, `createAndRegisterModuleLayer()` skips the ZIP because the
module is already present in a plugin layer. `BEASTClassLoader.registerPluginLayer()` uses
`putIfAbsent` so the Maven class-loader wins.

**Repository management** (CLI):
```
packagemanager -addMavenRepository https://beast2.org/maven/
packagemanager -listMavenRepositories
```
Extra repos are stored in `beauti.properties` under the key `maven.repositories` as a
comma-separated list. Maven Central is always included and does not need to be listed.

**Uninstall**:
```
packagemanager -delMaven io.github.compevol:beast-morph-models
```
Removes the coordinate from `maven-packages.xml`. The cached JARs in `maven-repo/` are **not**
deleted (they act as a download cache and are inert without a config entry).

---

## 4. `createAndRegisterModuleLayer()` — Core Load Mechanism

Both ZIP and Maven packages ultimately call this method (`PackageManager:892`). It performs five
steps that were impossible with the old `URLClassLoader` model:

**Step 1 — Discover modules** (`ModuleFinder.of(jarPaths)`)  
Reads `module-info.class` from each candidate JAR to get module names and descriptors.

**Step 2 — Exclude already-loaded modules** (line 907–916)

```java
Set<String> availableModules = parentLayers.stream()
    .flatMap(l -> l.modules().stream())
    .map(Module::getName)
    .collect(...);

Set<String> candidates = finder.findAll().stream()
    .filter(ref -> !availableModules.contains(ref.descriptor().name()))
    .collect(...);
```

Any module whose name already appears in the boot layer or a prior plugin layer is excluded before
loading begins. If `candidates` is empty, the method returns immediately — all modules are already
present. In developer mode this is the normal path.

**Step 3 — Filter unsatisfiable modules** (line 929–940)

```java
boolean satisfied = ref.descriptor().requires().stream()
    .filter(r -> !r.modifiers().contains(Modifier.STATIC))
    .allMatch(r -> availableModules.contains(r.name())
              || candidates.contains(r.name())
              || r.name().equals("java.base"));
```

Modules whose non-`static` `requires` cannot be satisfied are placed in a `skipped` set rather
than failing the entire load. This enables headless operation: an FX module that `requires beast.fx`
is silently skipped on a cluster where JavaFX is absent, leaving the core module unaffected:

```
Skipping modules with unsatisfied dependencies in SNAPP: [beast.snapp.fx]
```

**Step 4 — Resolve and create layer** (line 972–975)

```java
Configuration config = Configuration.resolveAndBind(
    filteredFinder, parentConfigs, ModuleFinder.of(), resolvable);
ModuleLayer layer = ModuleLayer.defineModulesWithOneLoader(
    config, parentLayers, ClassLoader.getSystemClassLoader()).layer();
```

`resolveAndBind` verifies the entire module graph is satisfiable before any class is loaded.
`defineModulesWithOneLoader` creates a single `ClassLoader` for all modules in this layer.

**Step 5 — Register with `BEASTClassLoader`**

```java
BEASTClassLoader.registerPluginLayer(layer, services);
```

Adds the layer to `pluginLayers`, merges `version.xml` services into the registry, and calls
`mergeAllProviders(layer)` to read `provides` declarations from `module-info.java`.

### 4.1 The Retry Loop (`PackageManager:708–747`)

Because `requires` must be satisfied at resolution time, load order matters. On the first pass,
package A may be skipped because its dependency B hasn't been loaded yet. After B loads, the
next pass satisfies A's `requires` and it loads successfully. The loop repeats until a full pass
produces no progress. This resolves inter-package JPMS dependencies regardless of filesystem
ordering.

### 4.2 Package Loading Precedence

When the same JPMS module name appears in more than one source, the **first one loaded wins**:

1. **Boot layer** — in dev mode, `beast.pkgmgmt`, `beast.base`, and `beast.fx` are all present;
   any installed copy is skipped. In user/release mode only `beast.pkgmgmt` is on the boot layer;
   `beast.base` and `beast.fx` arrive as plugin layers seeded from the bundle (see §8.2-B).
2. **Maven packages** — loaded first among external packages (`loadMavenPackages()` runs before
   the ZIP scan loop).
3. **ZIP packages** — scanned from `getBeastDirectories()` order; any module already loaded by
   Maven or an earlier plugin layer is skipped.

`class2loaderMap.putIfAbsent()` enforces the same rule in the service registry.

---

## 5. Package Installation Design

### 5.1 CLI Flags (ZIP packages — `PackageManagerCLI` line 264)

| Flag | Effect |
|---|---|
| *(none)* | Install into the **user** package directory (default) |
| `-useAppDir` | Install into the **system** package directory |
| `-dir DIR` | Override both; install directly into `DIR` |
| `-version X.Y.Z` | Archive mode — version kept as a subdirectory |

When `-dir DIR` is given, the CLI also sets the `beast.user.package.dir` and `BEAST_PACKAGE_PATH`
system properties to `DIR` so the new directory is visible to `getBeastDirectories()` on
subsequent loads.

### 5.2 Resolved Install Paths (`PackageInstaller.getPackageDir()`)

| Mode | Linux / Unix | macOS | Windows |
|---|---|---|---|
| **User** (default) | `~/.beast/2.8/` | `~/Library/Application Support/BEAST/2.8/` | `%USERPROFILE%\BEAST\2.8\` |
| **System** (`-useAppDir`) | `/usr/local/share/beast/2.8/` | `/Library/Application Support/BEAST/2.8/` | `\Program Files\BEAST\2.8\` |
| **Custom** (`-dir DIR`) | `DIR/` | `DIR/` | `DIR/` |

Final path: `base_root [/archive] / packageName [/ version]`

Examples for `packagemanager -add SNAPP`:

| Command | Final install path |
|---|---|
| `-add SNAPP` | `~/.beast/2.8/SNAPP/` |
| `-useAppDir -add SNAPP` | `/usr/local/share/beast/2.8/SNAPP/` (Linux) |
| `-dir /opt/beast -add SNAPP` | `/opt/beast/SNAPP/` |
| `-add SNAPP -version 1.3.2` | `~/.beast/2.8/archive/SNAPP/1.3.2/` |

### 5.3 Installation Steps

**1. Dependency resolution** — `populatePackagesToInstall()` → `DependencyResolver`  
Expands the requested package into its full transitive dependency set. `BEAST.base` and `BEAST.app`
are always present as installed, so packages declaring dependencies on them are resolved without
a network lookup.

**2. Pre-install check** — `prepareForInstall()` (`PackageInstaller:44`)  
Without `-version`: any currently-installed version that differs from the requested one is
uninstalled first via `uninstallPackage()`. With `-version`: both versions co-exist under
`archive/`. If a `toDeleteList` file exists (Windows JAR-locking deferral from a previous
session), the install is written to `toInstallList` and deferred to the next restart.

**3. Download and extract** — `installPackages()` (`PackageInstaller:77`)  
Fetches the package ZIP from the URL recorded in the repository XML (HTTP HEAD check first to
detect 404/redirect before downloading). Creates the target directory, streams the ZIP to
`<dirName>/<PackageName>.zip`, and extracts in-place via `doUnzip()` (Zip Slip protected —
entries outside the target directory throw `IOException`). The `.zip` file is **not deleted**
after extraction; it remains alongside `lib/`, `templates/`, `version.xml`.

**4. Namespace clash check** — `hasNamespaceClash()`  
After extraction, `version.xml` is parsed and service classes are checked against
`BEASTClassLoader.usesExistingNamespaces()`. If a class name collision is detected, the extracted
directory is deleted entirely and a `RuntimeException` is thrown — the install is rolled back.

**5. Post-install cleanup**  
`Utils6.saveBeautiProperty("package.path", null)` clears any cached package path in
`beauti.properties` so the next BEAST startup re-discovers packages fresh.

---

## 6. Service Registration Design

Two parallel mechanisms register services, used by `BEASTClassLoader.forName(className, service)`
and `PackageManager.listServices()`:

| Mechanism | Source | When applied |
|---|---|---|
| **JPMS `provides`** (`module-info.java`) | Boot layer + each plugin layer | At layer registration via `mergeAllProviders()` (primary) |
| **`version.xml` `<service>`** | Disk file or embedded in JAR | At `initServices()` / `registerPluginLayer()` (supplement) |

### 6.1 `version.xml` Format

```xml
<service type="beast.base.evolution.datatype.DataType">
    <provider classname="beast.base.evolution.datatype.Nucleotide"/>
    <provider classname="beast.base.evolution.datatype.Aminoacid"/>
</service>
<packageapp class="beast.app.beauti.Beauti"/>
```

`parseServices()` (`PackageManager:1151`) converts these into a `Map<String, Set<String>>` keyed
by the service interface name. `<packageapp>` elements are stored under the synthetic key
`"has.main.method"`. The map is passed to `BEASTClassLoader.registerPluginLayer()` and is
queryable at runtime via `listServices(String serviceType)` (`PackageManager:1193`).

Service declarations are read at two points: during **installation** (namespace clash check) and
during **loading** (registering providers with the classloader).

### 6.2 Merge Rules

`BEASTClassLoader.ensureServicesLoaded()` (`:155`) runs both mechanisms on first access for each
service type. JPMS declarations take precedence because `class2loaderMap.putIfAbsent()` is used —
the first class-loader to register a provider wins. Since Maven packages are loaded before ZIP
packages, Maven takes precedence when both formats provide the same service class. The two
mechanisms' provider sets are unioned — duplicates are harmlessly deduplicated via `Set.add()`.

---

## 7. Class Discovery Design — `find()`

Once packages are loaded, `find(Class<?> cls, String pkgname)` (`PackageManager:1534`) scans the
JVM classpath to locate all concrete implementations of a given class or interface within a
package prefix. This is how BEAST discovers all installed implementations of extension points such
as `TreeOperator`, `DistributionLikelihood`, or `DataType` across all loaded packages.

**Step 1 — `loadAllClasses()`** (line 1289)  
Walks `java.class.path` entries — directories, JARs, and individual `.class` files — and
accumulates fully-qualified class names in the `all_classes` list. This list is cached for the
lifetime of the process.

**Step 2 — Filter and load**  
Iterates `all_classes`, filters by package prefix, loads each class via `BEASTClassLoader.forName()`,
and accepts it if it is non-abstract and either implements the target interface (`hasInterface()`)
or is a subclass of the target class (`isSubclass()`).

**Step 3 — Sort and deduplicate**  
Results are sorted with `BEAST.base` first, `BEAST.app` second, then alphabetically by class name,
and deduplicated. The same ordering is applied to BEAST package names via `comparePackageNames()`.

---

## 8. Design Conformance 

| Original Design Goal | Implementation Status |
|---|---|
| Dev mode: no disk copy needed, modules from Maven build | **Implemented** — boot-layer filter in `createAndRegisterModuleLayer()` skips already-loaded modules; `addInstalledPackages()` falls back to synthetic version when no `version.xml` on disk |
| User mode option i: traditional `~/.beast/2.8/` path | **Implemented** — `BeastLauncher.getPath()` bootstraps core packages; `getBeastDirectories()` returns user dir first |
| User mode option ii: install Maven release jars locally | **Implemented** — `installMavenPackage()`, `loadMavenPackages()`, `maven-packages.xml` config |
| Maven packages take precedence over ZIP packages | **Implemented** — Maven loaded first; `putIfAbsent` in class-loader map |



---

## 9. Key Files, Data Structures & Config

### 9.1 Key Files

| File | Role |
|---|---|
| `PackageManager.java` | Central facade: directory scanning, package metadata, JAR loading, Maven package management, class discovery |
| `PackageManagerCLI.java` | CLI entry point: `-list`, `-add`, `-del`, `-maven`, `-delMaven`, `-listMaven`, `-addMavenRepository`, etc. |
| `PackageInstaller.java` | ZIP download, extraction (Zip Slip protected), namespace clash check, delete/install deferred lists |
| `MavenPackageResolver.java` | Eclipse Aether wrapper: resolves Maven coordinates → local JAR paths, downloads to `~/.beast/2.8/maven-repo/` |
| `BEASTClassLoader.java` | Class loading (JPMS ModuleLayers + fallback), service registry (version.xml + JPMS provides), plugin layer management |
| `BeastLauncher.java` | Bootstrap: seeds core packages into user dir, builds classpath, invokes `PackageManager.loadExternalJars()`, reflectively launches main class |
| `Utils6.java` | OS detection, beauti.properties I/O, splash screen logging, Java version check |
| `DependencyResolver.java` | Transitive dependency resolution and installed-dependency validation |
| `PackageRepository.java` | Remote repository URL management, CBAN package XML download |

### 9.2 Key Data Structures

| Field / Class | Purpose |
|---|---|
| `packages` (`TreeMap<String, Package>`) | In-memory database mapping package name to its `Package` object (installed version, dependencies). Populated by `addInstalledPackages()` at startup and during `-add`. `BEAST.base` and `BEAST.app` are always present and always marked installed. |
| `externalJarsLoaded` (`boolean`) | Guard flag; `loadExternalJars()` is a no-op if already `true`. Set to `true` after `checkInstalledDependencies()` completes. |
| `all_classes` (`List<String>`) | Cached flat list of all class names visible on the classpath; used by `find()`. |
| `BEASTClassLoader` | Custom classloader aggregating JPMS plugin layers and the service registry; bridge between `PackageManager` and the JVM. |
| `toDeleteList` | File listing paths that could not be deleted (Windows JAR locking); processed on next startup by `processDeleteList()`. |
| `toInstallList` | File listing package coordinates to install on next startup; written by `prepareForInstall()` when deletion is pending. |

### 9.3 Config Files Used at Runtime

| File | Location | Purpose |
|---|---|---|
| `version.xml` | `~/.beast/2.8/<PackageName>/version.xml` | Package name, version, dependencies, services |
| `maven-packages.xml` | `~/.beast/2.8/maven-packages.xml` | List of Maven coordinates to load at startup |
| `beauti.properties` | `~/.beast/2.8/beauti.properties` | Package update policy, extra repo URLs, `maven.repositories`, cached `package.path` |
| `toDeleteList` | `~/.beast/2.8/toDeleteList` | Files to delete on next startup (Windows JAR-locking workaround) |
| `toInstallList` | `~/.beast/2.8/toInstallList` | Packages to install on next startup (deferred when toDeleteList exists) |
| `maven-repo/` | `~/.beast/2.8/maven-repo/` | Local Maven repository cache (Eclipse Aether layout) |


