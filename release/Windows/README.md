# Windows Release

## Prerequisites

- **JDK 25+** with `mvn` and `jpackage` on PATH (for the Maven build; jpackage is a JDK tool)
- **Git Bash** (or any bash) — the assemble script runs under bash on Windows
- A **Zulu JRE+FX 25** for Windows x86_64, set via `JRE_DIR`
  - Use the `jre+fx` package from azul.com (includes JavaFX as platform modules)
  - Passed as `--runtime-image` to jpackage; `JRE_DIR` does **not** need to contain `jpackage`
  - In CI, a second `setup-java` with `java-package: jre+fx` sets `$JAVA_HOME` to this JRE after
    the Maven build (first `setup-java` uses `jdk+fx` so Maven and jpackage are on PATH)

## Building via CI

Trigger the `Release Bundles` workflow from GitHub Actions UI.  
Produces: `BEAST.v<version>.Windows.x86_64.zip` (artifact `Windows.x86_64`)

## Building locally (Git Bash)

```bash
cd <repo-root>
# build with the release version, as CI does; VERSION must match version.xml
mvn versions:set -DnewVersion=2.8.0 -DgenerateBackupPoms=true -q
mvn clean package -DskipTests

VERSION=2.8.0 \
JRE_DIR="$JAVA_HOME" \
DEST="release/dist" \
bash release/Windows/assemble-bundle.sh

# put the poms back to -SNAPSHOT
mvn versions:revert -q
```

Without `versions:set`, the JARs and package ZIPs keep `-SNAPSHOT` in their names, and the
script stops at its SNAPSHOT check.

Produces: `release/dist/BEAST.v<version>.Windows.x86_64.zip`

## Bundle layout

```
BEAST.v<version>/
  beast.exe, beauti.exe, …   double-clickable native launchers (jpackage)
  bat/              .bat launchers (beast.bat, beauti.bat, treeannotator.bat,
                    logcombiner.bat, applauncher.bat, packagemanager.bat,
                    loganalyser.bat, densitree.bat)
  app/              boot-layer JARs (beast-pkgmgmt + deps, DensiTree.jar)
                    + patched .cfg files (module-path mode)
  app/packages/     beast-base and beast-fx package ZIPs — seeded into
                    %USERPROFILE%\BEAST\2.8\ on first run, loaded as plugin layers
  app/resolver/     Maven resolver JARs, on the classpath (#297)
  examples/         beast3 XML files + nexus/ + fasta/ + beastfx/
  runtime/          bundled Zulu JRE+FX 25 (full, via --runtime-image; Windows x86_64)
  version.xml
  README.txt
  LICENSE.txt
```

`bat/` scripts invoke:
```bat
%BUNDLE_HOME%\runtime\bin\java.exe --module-path "%BUNDLE_HOME%\app" ^
    -cp "%BUNDLE_HOME%\app\resolver\*" --add-reads beast.pkgmgmt=ALL-UNNAMED ^
    --add-modules ALL-MODULE-PATH,java.compiler ...
```

The Maven resolver is on the classpath because Maven's own JARs split packages,
which stops the JVM at start on the module path; see "Maven resolver on the
classpath" in [../README.md](../README.md).

## How assemble-bundle.sh works

0. **Check the version** — stops if `VERSION` differs from the version in
   `version.xml`, so the bundle and the Mac DMG are built with the same version.
1. **Stage JARs** — copies beast-fx + dependencies into `staging-Windows.x86_64/`.
   All `javafx-*` and `jdk-jsobject-*` JARs excluded (already in the bundled JRE+FX
   as platform modules).
2. **jpackage app-image** — uses `--runtime-image $JRE_DIR` to embed Zulu JRE+FX 25
   directly as `runtime/`, creates per-tool `.exe` launchers, and places all staged
   JARs into `app/`.
3. **Remove core modules from app/** — `beast-base-*.jar` and `beast-fx-*.jar` are
   deleted from `app/` so they are not on the boot module path.
4. **Bundle core package ZIPs** — `BEAST.base.package.v*.zip` and
   `BEAST.app.package.v*.zip` are placed in `app/packages/`. On first launch,
   `BeastLauncher.seedBundledPackage()` extracts them into `%USERPROFILE%\BEAST\2.8\`
   and loads them as plugin `ModuleLayer`s, allowing patch releases without a new zip.
   **Copy the Maven resolver → app/resolver/** — the JARs from
   `beast-pkgmgmt/target/resolver/` (Step 4c of the script).
5. **Patch .cfg files** — switches all launchers from classpath to module-path mode,
   adds `java.compiler` and `--add-reads=beast.pkgmgmt=ALL-UNNAMED`, and inserts one
   `app.classpath=$APPDIR\resolver\<jar>` line per resolver JAR after `[Application]`
   in every `.cfg`, so the `.exe` launchers get the resolver too.
6. **Copy bat/ scripts** — static launchers from `release/Windows/bat/` into `bat/`.
   Each script resolves `BUNDLE_HOME` via `%~dp0..` and invokes `runtime\bin\java.exe`
   with `--module-path app\`.
7. **Copy examples** — beast3 XML files, `nexus/`, and `fasta/` from
   `beast-base/src/test/resources/beast.base/examples/` into `examples/`.
   `beast2vs1/` and `legacy/` subdirectories are excluded from the release.
   `beast-fx/src/test/resources/beast.fx/examples/` (XMLs needing beast-fx classes) go to `examples/beastfx/`.
8. **Copy version.xml and docs** — `version.xml`, `README.txt`, `LICENSE.txt`.
9. **Verify bundle** — checks required exe/bat files, `runtime/bin/java.exe`,
   `.cfg` patch (module-path mode), beast-base/beast-fx absent from `app/`,
   package ZIPs present in `app/packages/`, arch (PE32+/x86-64), and JavaFX DLLs;
   also that `app/resolver/` has all the resolver JARs, no `maven-*.jar` is directly in
   `app/`, every `.cfg` has `--add-reads` and one classpath line per resolver JAR,
   `bat/packagemanager.bat` has `--add-reads`, and no file has `SNAPSHOT` in its name.
10. **Create zip** — packages `BEAST.v<VERSION>/` into `BEAST.v<VERSION>.Windows.x86_64.zip`
    using 7-Zip (`7z a -tzip`), with PowerShell `Compress-Archive` as a fallback.

## Finding the CI-generated release file

1. Open the repository on GitHub and click the **Actions** tab.
2. In the left sidebar under **Workflows**, click **Release Bundles**.
3. Click the workflow run you want.
4. Scroll to the **Artifacts** section and click **Windows.x86_64** to download.

GitHub wraps the file in an outer zip on download, so you receive
`Windows.x86_64.zip` containing `BEAST.v<version>.Windows.x86_64.zip`.
Extract the inner zip before distributing it.
