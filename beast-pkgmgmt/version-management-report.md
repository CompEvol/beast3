# How BEAST picks package versions: Maven packages vs ZIP packages

## The problem found in #298

Installing a package from Maven can break BEAST itself.

- **What happens:** `io.github.compevol:ccd:1.2.2` was built against BEAST 2.8.0-rc1. When it
  is installed, BEAST's own core (`beast-base`, `beast-fx`, `beast-pkgmgmt`) 2.8.0-rc1 is also
  downloaded from Maven Central. At the next start, BEAST runs on that old core instead of the
  one it shipped with, and `beast -validate examples/testHKY.xml` fails with
  `ClassNotFoundException: BactrianSubtreeScale`.
- **Why:** Maven packages are loaded before ZIP packages, and the first one loaded wins. BEAST's
  core is now a ZIP package (BEAST.base, BEAST.app), so the old core from the Maven package is
  loaded first and replaces it.
- **Why it was not seen before:** until #297 was fixed, no Maven package could be installed in
  the release at all. Every Maven package depends on `beast-base`, so after #297 any Maven
  install can do this.

The root cause is wider than #298. BEAST decides versions in several places with different
rules. The rest of this report describes those rules and proposes a single one.

## Summary

BEAST makes version decisions at three stages, and each stage uses its own rule:

| Stage | Maven packages | ZIP packages |
|---|---|---|
| 1. Install: which versions to download | Maven Resolver (Aether), one package at a time | Hand-written `DependencyResolver` |
| 2. Comparing two versions | Maven's `GenericVersionScheme` | Hand-written `PackageVersion.compareTo` |
| 3. Start-up: which jar is loaded for a module | Hand-written: the first one loaded wins | The same hand-written rule |

Only stage 1 for Maven packages reuses the Maven library, and even there one of Maven's defaults is missing (see 1a).

## 1. Install

### a. Maven packages: `MavenPackageResolver.resolve()`

The Maven library builds the dependency graph and downloads the jars. Two things are ours:

- **The session is built without Maven's defaults.** `createSession()` starts from the bare
  `system.createSessionBuilder()`. Testing with the bundled resolver 2.0.16:

  ```
  bare builder:           transformer=null                        manager=null
  SessionBuilderSupplier: transformer=ChainedDependencyGraphTransformer  manager=TransitiveDependencyManager
  ```

  So Maven's conflict resolution ("nearest wins") and `<dependencyManagement>` are switched off.
  If two dependencies of a package need different versions of the same library, the resolver
  returns both versions. `ModuleFinder` then uses whichever comes first in the list.
- **The jars are filtered afterwards.** Jars whose module is already loaded are dropped. The
  check is by module name only, not by version. This is how #298 happens: BEAST.base is not
  loaded yet when Maven packages are resolved, so the `beast-base` named in the package's POM
  (2.8.0-rc1) gets through.

Each Maven package is also resolved **on its own**. Nothing decides between two Maven packages
that need different versions of the same library.

### b. ZIP packages: `DependencyResolver.resolve()`

This is entirely hand-written, from BEAST 2. It reads the `<depends on= atleast= atmost=>` ranges
from `version.xml`, takes the first compatible version that is available, never downgrades an
installed package, and gives an error if two packages need different versions. It only knows
about ZIP packages. Maven packages are not in its package map, which `addInstalledPackages()`
builds by scanning folders that contain a `version.xml`. So:

- the `<depends>` in a Maven package's embedded `version.xml` is never checked;
- a ZIP package that depends on a package installed from Maven reports it as missing.

## 2. Comparing two versions

`PackageVersion.compareTo` splits on `.` and compares the suffixes as plain strings. Comparing
it with Maven's `GenericVersionScheme` on the same pairs:

| A vs B | BEAST | Maven |
|---|---|---|
| 2.8.0-rc1 vs 2.8.0 | A < B | A < B |
| 2.9 vs 2.10 | A < B | A < B |
| 1.0 vs 1.0.0 | equal | equal |
| **1.0-rc9 vs 1.0-rc10** | **A > B** (wrong) | A < B |
| **1.0-RC1 vs 1.0-rc1** | **A < B** | equal |
| **1.0-SNAPSHOT vs 1.0-rc1** | **A < B** | A > B |

So the same package can be "newer" for ZIP packages and "older" for Maven packages.

## 3. Start-up: which jar is used for a module

`PackageManager.loadExternalJarsEvenIfAlreadyLoaded()` loads every Maven package first, then
every ZIP package. `createAndRegisterModuleLayer()` skips any module that is already loaded.
**No versions are compared at start-up at all.** Whatever is loaded first wins:

- a Maven package always beats a ZIP package, even an older one;
- among ZIP packages, the order of `getBeastDirectories()` decides;
- BEAST's own core gets no protection, because it is a ZIP package now (#298).

## Proposal: one set of rules, in one place

### The rules

There are two kinds of dependency, and each gets one rule.

**A. BEAST packages** (the core BEAST.base and BEAST.app, and packages such as SA)

> The installed copy is used. The version a package was built against is a **minimum**.

- For the core, the installed copy is the one that came with BEAST. A package can never
  replace it.
- For other BEAST packages, the installed copy is the ZIP or Maven package that the user
  installed.
- The minimum comes from the same place for both kinds of package: `<depends atleast=>` in
  `version.xml`, or the dependency version in the POM.
- The core also has a maximum: the same major version. A package built for 2.x does not run on
  3.x.
- **JavaFX counts as part of the core.** The installed BEAST provides it, and a package's
  JavaFX version is a minimum too. Because the JavaFX API rarely changes, an unmet JavaFX
  minimum only gives a warning; the package is still loaded.

**Assumption:** the installed BEAST provides all seven JavaFX modules (`base`, `controls`,
`fxml`, `graphics`, `media`, `swing`, `web`). Today it does in both setups. The release JRE
(Zulu with FX) has all seven at 25.0.3. In Maven dev mode, `beast-fx` brings in all seven
(`javafx-web` pulls in `media`). If a later BEAST drops a module, or ships without JavaFX, a
package that needs it cannot get it from Maven, because JavaFX is excluded. The package is
then not loaded, with a message, instead of crashing.

**B. Plain libraries** (commons-math, antlr, ...)

> The highest version wins, compared the Maven way.

**When the minimum is not met:**

| When | Core | Other BEAST package |
|---|---|---|
| Install | Refuse: "ccd 1.2.2 needs BEAST 2.8.1 or later; please update BEAST" | Install a version that meets it, as `DependencyResolver` already does for ZIP packages |
| Start-up | Do not load the package; print the same message | Do not load the package; say which package to update |

For JavaFX, an unmet minimum only prints a warning at install and at start-up.

Not loading the package changes current behaviour, which loads it and prints a warning. It is
safer: a package that starts and then crashes with `NoSuchMethodError` in the middle of a run
is worse than one that does not start.

### Where the rules live

One class, e.g. `PackageSet`, sees every installed package before anything is loaded:

1. **Collect.** Read every installed package, ZIP (`version.xml`) and Maven
   (`maven-packages.xml`), into one list of `name, version, minimums, jars`.
2. **Read the minimums from Maven packages.** For each Maven package, read its POM
   (`readArtifactDescriptor`, which reads the POM only and does not download jars). Note the
   versions of the BEAST packages it depends on, including `beast-base`, and its JavaFX
   version. This step is how gap 1 is covered: the core and JavaFX versions are recorded before
   they are excluded.
3. **Resolve the plain libraries.** Run one `CollectRequest` with every Maven package as a
   root, using a session from `SessionBuilderSupplier`, so Maven's own conflict resolution
   applies across all of them. Exclude BEAST's core and `org.openjfx:*`, so they are never
   downloaded. This also means BEAST never has to pick the right platform-specific JavaFX jars
   (mac-aarch64, linux, win) from Maven Central.
4. **Choose.** Apply rule A to BEAST packages and rule B to libraries. If a downloaded jar is a
   BEAST package that is already installed (it has a `version.xml`) and the installed copy meets
   the minimum, the installed copy is used. This is the answer to gap 2: the installed SA ZIP is
   used, and the POM's SA version is only a minimum.
5. **Load.** Build the module layers from the chosen jars, in dependency order. Load order no
   longer decides anything.

`PackageVersion.compareTo` calls Maven's `GenericVersionScheme`, so every comparison above
orders versions the same way. The cost is that the resolver jars stop being optional. The #297
fix already puts them in every launcher.

**What goes away:** the "skip if already loaded" filter in `MavenPackageResolver`, the "Maven
first, then ZIP" ordering, the skip/retry loop, and the hand-written version parsing.

### Which part fixes which problem

| Problem | Fixed by |
|---|---|
| **#298**: old core from Maven replaces the installed core | Excluding the core (step 3) and rule A |
| Gap 1: package built against a newer core runs on an older one | Core minimum read from the POM (step 2) and refusing to load |
| Package built against a newer JavaFX than BEAST ships | JavaFX minimum read from the POM (step 2), warning only |
| Gap 2: POM version of SA vs the installed SA ZIP | Rule A: installed copy, POM version is a minimum (step 4) |
| No conflict resolution inside a Maven package | `SessionBuilderSupplier` (step 3) |
| No conflict resolution between Maven packages | One `CollectRequest` for all of them (step 3) |
| Maven packages' `<depends>` never checked | One list for ZIP and Maven packages (step 1) |
| ZIP and Maven order versions differently | `GenericVersionScheme` everywhere |

Only the first two rows are needed for #298. The other rows are separate bugs that the same
design also fixes.

### Order of work

1. **For 2.8, to close #298** (small changes, all in `MavenPackageResolver`):
   - exclude the core and `org.openjfx:*`;
   - read the core version from the package's POM, and refuse to install or load a package
     whose core minimum is newer than the installed BEAST, or whose major version is different;
   - warn when the package's JavaFX version is newer than the one BEAST ships.
2. **Next, small bug fixes:** `SessionBuilderSupplier`, and `GenericVersionScheme` in
   `PackageVersion`.
3. **Later:** `PackageSet`, which brings in one list for both kinds of package, rule A for
   BEAST packages other than the core (gap 2), and loading without depending on order.

## Code referred to

- `beast-pkgmgmt/src/main/java/beast/pkgmgmt/MavenPackageResolver.java`: `resolve()`, `createSession()`
- `beast-pkgmgmt/src/main/java/beast/pkgmgmt/DependencyResolver.java`: `resolve()`, `checkInstalled()`
- `beast-pkgmgmt/src/main/java/beast/pkgmgmt/PackageVersion.java`: `compareVersionStrings()`
- `beast-pkgmgmt/src/main/java/beast/pkgmgmt/PackageManager.java`: `addInstalledPackages()`,
  `loadExternalJarsEvenIfAlreadyLoaded()`, `createAndRegisterModuleLayer()`, `loadMavenPackages()`
