# BEAST package manager: design

**Updated:** 2026-11-19, for 2.8.0 (the planner: [#298](https://github.com/CompEvol/beast3/issues/298))
**Scope:** `beast.pkgmgmt` and `beast.pkgmgmt.plan`

How BEAST finds installed packages, decides what to load, loads it, and installs packages. For
package authors, `README.md` is the shorter guide.

---

## 1. Why module layers

In BEAST 2, `BEASTClassLoader` was one `URLClassLoader` with every package's jars appended:
no dependency checks, any class visible to any package, and the first jar on the list silently
won. BEAST 3 uses the Java module system (JPMS). Each jar with a `module-info.class` is a module
that declares what it `requires`, `exports` and `provides`. A jar without one is an *automatic*
module: it reads every other module and exports all its packages.

Modules are loaded in `ModuleLayer`s on top of the boot layer. The JVM checks a layer's
configuration as a whole before any class is loaded: every non-static `requires` must be met,
no module may read two modules that contain the same Java package, and no module may read
two modules of the same name. Different versions of a module can only coexist in
different layers.

| Mode | Boot layer | Packages |
|---|---|---|
| Release (installed app) | `beast.pkgmgmt` (the launcher) and its libraries | Everything else, including BEAST.base (`beast.base`) and BEAST.app (`beast.fx`), seeded from the app into the user package directory on first start |
| Developer (`mvn exec`, IDE) | `beast.pkgmgmt`, `beast.base`, `beast.fx` and their libraries | Installed packages only |

## 2. Overview

```
installed packages ──► Inventory ──► Planner ──► LoadPlan ──► Loader ──► module layers
  (ZIP folders,            ▲          ▲  │                                   │
   archive, Maven)    Runtime ────────┘  └─► packagemanager -plan            ▼
                      Selection ──────┘                          BEASTClassLoader (bridge)
```

Every decision about what to load is made in one place, the planner, before anything is
loaded, and every package or copy that is left out comes with its reason. The planner is a
plain function: no files, network or module layers, so it is tested on made-up installs.

| Part | Class | Job |
|---|---|---|
| Inventory | `plan.Inventory` | Reads what is installed into `InstalledPackage` records |
| Planner | `plan.Planner` | Decides; result is a `LoadPlan` |
| Loader | `plan.Loader`, `plan.LoadedPackages` | Loads the plan; class and service lookup |
| Versions | `plan.Version`, `plan.Dependency` | Maven's version order and ranges, without the resolver |
| Selection | `plan.Selection` | Everything, or the versions an analysis needs (`-strictversions`) |
| Entry points | `PackageManager` | `loadExternalJars()`, `installMavenPackage()`, `describePlan()` |

## 3. Inventory

One `InstalledPackage` per installed copy, whatever its format: name, version, location,
dependencies, artifacts (jars: the package's own modules and the libraries it brings, with
their module names, Java packages and `requires`), and `version.xml` services. The same
package can be installed more than once (bundled and upgraded, ZIP and Maven, in two places);
the planner picks one.

| Location | Read from | Precedence |
|---|---|---|
| `BOOT` | The boot layer (developer mode): BEAST.base and BEAST.app | Always used |
| `OVERRIDE` | `BEAST_PACKAGE_PATH` | Used whatever its version |
| `USER`, `MAVEN`, `SYSTEM` | User package directory and its `maven-packages.xml`; system and installation directories | Newest version first |
| `ARCHIVE` | `archive/<name>/<version>/` in the user directory | Only if no other copy is used, or pinned by `-strictversions` |
| `AVAILABLE` | Versions a repository offers (only when planning an install) | After all installed copies |

**Where each fact comes from** (one job per source; disagreements are warnings, shown by
`packagemanager -plan`, and never stop a package from loading):

| | ZIP package | Maven package |
|---|---|---|
| Name | `version.xml` | `version.xml` in the main jar (else the artifactId) |
| Version | `version.xml` | the Maven coordinate |
| Dependencies on BEAST and BEAST packages | `<depends>` in `version.xml` | the POM (`beast-base` / `beast-fx` → BEAST.base / BEAST.app; a dependency whose jar has a `version.xml` → that BEAST package) |
| Libraries | jars in `lib/` that are not BEAST modules | the POM, resolved by Maven |
| Services | `provides` in `module-info` (counts); `<service>` in `version.xml` still honoured | the same |
| Apps | `<packageapp>` in `version.xml` | the same |

In a ZIP package's `lib/`, a jar is the package's own module if it is a named module called
`beast.*` or one that requires a `beast.*` module; other jars are libraries. POM properties and
parent POMs (in the local repository) are followed.

## 4. Planner

`Planner.plan(installed, runtime, selection)`. `Runtime` is what only a new installer changes:
the launcher's version, JavaFX's (null when headless) and the boot layer's modules. The rules,
in the order they act:

1. **Selection.** With `-strictversions`, a pinned package keeps only the copy of that exact
   version (else it is *missing*, and the run stops with the install command); a pinned core
   version is preferred but not required (an analysis made with an older BEAST still runs, with
   a warning); packages neither pinned nor needed by a pinned package are not loaded.
2. **Copies.** Of several copies of a package, the first that works is used (precedence above).
   A copy that fails is rejected with its reason, and the next is tried. When a package needs a
   different version of a dependency than the copy tried, and another copy of the dependency
   fits, that copy is tried before the package is excluded.
3. **Dependencies.** A dependency is a minimum, a maximum or a range. On BEAST.base or
   BEAST.app, a minimum without a maximum accepts the same minor version only (2.8.x). Headless,
   a dependency on BEAST.app does not fail: the package's FX modules are left out by rule 6.
4. **Core and launcher.** BEAST's own modules (`beast.base`, `beast.fx`, `beast.pkgmgmt`),
   JavaFX and the JRE's (`java.*`, `jdk.*`) are never taken from a package; such jars among a
   package's libraries are ignored, and a package that ships one as its own module is excluded.
   A core copy that needs a newer launcher, or is of another major version than the launcher,
   is not used (that needs a new installer).
5. **Modules.** A package's own module comes before any library copy of it. Library versions
   with the same major version are compatible and share the highest copy. Incompatible
   versions (different majors) each become a private copy, and none is shared: a private copy
   goes in a child layer with the packages that need it and everything that depends on them
   (a copy without `module-info` is an automatic module, which reads all modules of its parent
   layers and may not read one of its own name). A package that would need two private copies
   of a library is excluded, naming it.
6. **Requires.** A module whose non-static `requires` cannot be met within the plan is left
   out ("headless" when JavaFX is missing); a package left with none of its own modules is
   excluded, and so is everything that needs it.
7. **Java packages.** Each Java package is in exactly one module that a module can see (per
   layer); a clash excludes the later package (core first, then by name) and names both.

Each round rejects one copy and tries again, so planning ends; the result does not depend on
the order in which packages were found. A `LoadPlan` lists the loaded copies, the modules with
their jars and layers, the modules left out, the excluded packages, the copies not used, the
missing pinned versions and warnings, each with a reason.

## 5. Loader

`Loader.load(plan)`: the shared modules are resolved in one `Configuration` on top of the boot
layer and defined in one layer with a class loader per module; each private group gets a child
layer of the shared layer. No retry loop, no "skip a module already loaded".

`LoadedPackages` finds a class from its Java package (BEAST's classes are in one module each; a
library class with private copies resolves to the shared copy) and lists services from the
`provides` of loaded modules and the `version.xml` services of loaded packages. For 2.8.0 the
layers are registered with `BEASTClassLoader` (`registerWithBEASTClassLoader()`), which BEAST's
code uses; moving `BEASTClassLoader`'s lookup onto the new loader is
[#314](https://github.com/CompEvol/beast3/issues/314).

If the JVM rejects a plan (something the planner did not foresee), BEAST reports it and starts
with its core alone rather than not at all.

## 6. Start-up

`BeastLauncher` (BEAST, BEAUti, TreeAnnotator, LogCombiner, AppLauncher) seeds the bundled
packages into the user directory (`seedBundledPackage`: a newer user copy is never downgraded;
a removed bundled package stays removed), then calls `PackageManager.loadExternalJars()`, which
reads the inventory, plans, loads and registers. It prints the packages loaded and those left
out, with the reason. Later calls are no-ops, so BEAST's own calls need no change.
`beast -strictversions` loads with a `Selection` read from the analysis's `required` attribute.

## 7. Installing

- **ZIP packages** (`packagemanager -add`, BEAUti): `DependencyResolver.resolve()` plans with the
  installed packages and the versions the repositories offer (`AVAILABLE`). An installed copy
  that fits is kept; nothing is downgraded unless that version is asked for; only what the
  packages asked for need is installed. Download, unpacking and Windows deferrals
  (`toDeleteList`, `toInstallList`) are `PackageInstaller`'s.
- **Maven packages** (`packagemanager -maven`, BEAUti): `MavenPackageResolver` downloads the
  package and its libraries, excluding BEAST's core and `org.openjfx:*` (#298). The planner checks
  that the package would load; if not, it is refused with the reason, otherwise recorded in
  `maven-packages.xml`. Uninstalling removes it from there; cached jars stay in `maven-repo/`.
- Installs take effect at the next start: nothing is loaded into the running BEAST.

## 8. Versions

`plan.Version` orders versions as Maven's `GenericVersionScheme` does (tested on every pair of 49
versions), without needing the resolver: split at `.`, `-`, `_` and digit/letter changes;
numbers as numbers (`rc9 < rc10`); `alpha < beta < milestone < rc = cr < snapshot < (release) =
ga = final < sp`; other words after a release; trailing zeros do not count. `PackageVersion`
uses the same order. `plan.Dependency.fromMaven` reads `[a,b)`, `(a,b]`, `[a,)`, `(,b]` and `[a]`.

## 9. Not done yet

- **Lockfile.** Start-up still resolves each Maven package's jars with the Maven resolver (from
  the local repository), so the release must ship the resolver jars (#308). Saving the plan
  would avoid both.
- **Consumers.** BEAUti, AppLauncher and BeastMain still look for templates, examples and apps in
  `PackageManager.getBeastDirectories()` (with special cases for Maven packages) rather than
  asking the inventory.
- **`BEASTClassLoader`** and `PackageManager.find()`: [#314](https://github.com/CompEvol/beast3/issues/314).
