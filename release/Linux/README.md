# Linux Release

## Prerequisites

- **JDK 25+** with `mvn` on PATH (for the Maven build)
- **`cp -a`** support (standard on macOS and Linux — preserves JRE symlinks)
- A **Zulu JRE+FX 25** for the target Linux arch, set via `JRE_DIR`
  - Download the `jre+fx` package from azul.com (smaller than the full JDK;
    already contains JavaFX as platform modules)
  - Expected directory name: `zulu25.34.17-ca-fx-jre25.0.3-linux_<arch>`
  - Default in `test-linux-on-mac.sh`: set via `ZULU_JRE_FX_DIR` at the top of
    that script — must point to the extracted Linux JRE+FX (not the macOS install)
  - `assemble-bundle.sh` has no default — `JRE_DIR` must be set explicitly when
    calling it directly

## Building locally (macOS → Linux aarch64)

```bash
cd release
bash test-linux-on-mac.sh
```

Produces: `release/dist/BEAST.v<version>.Linux.aarch64.tgz`

Skip the Maven build if already built:

```bash
bash test-linux-on-mac.sh --skip-build
```

`JAVAFX_CLASSIFIER` defaults to `mac-aarch64` in this script so Mac's local
Maven output is used. On a real Linux CI runner `JAVAFX_CLASSIFIER` is
auto-detected from `uname` and no override is needed.

## Building on Linux CI (or directly)

```bash
cd release
VERSION=2.8.0 \
OS_ARCH=Linux.aarch64 \
JRE_DIR=/path/to/zulu25-jre-fx-linux-aarch64 \
bash Linux/assemble-bundle.sh
```

## Bundle layout

```
BEAST.v<version>/
  bin/              shell launchers (beast, beauti, treeannotator,
                    logcombiner, applauncher, packagemanager,
                    loganalyser, densitree)
  examples/         beast3 XML files + nexus/ + fasta/
  jre/              bundled Zulu JRE+FX 25 (target platform)
  lib/              boot-layer JARs (beast-pkgmgmt + deps, DensiTree.jar)
  lib/packages/     beast-base and beast-fx package ZIPs — seeded into
                    ~/.beast/2.8/ on first run, loaded as plugin layers
  version.xml
  README.txt
  LICENSE.txt
```

`bin/` scripts resolve `BUNDLE_HOME` via symlink-safe path resolution and invoke:
```sh
$BUNDLE_HOME/jre/bin/java --module-path $BUNDLE_HOME/lib --add-modules ALL-MODULE-PATH \
    -m beast.pkgmgmt/beast.pkgmgmt.launcher.BeastLauncher "$@"
```

`BEAGLE_LIB` is respected if set — it is appended to `LD_LIBRARY_PATH`.

## Finding the CI-generated release file

The `Release Bundles` workflow (`.github/workflows/release-bundles.yml`) uploads
each assembled tgz as a **GitHub Actions artifact** — a temporary file attached
to the specific workflow run, not to the repository itself.

### Navigating to the artifact

1. Open the repository on GitHub and click the **Actions** tab.
2. In the left sidebar under **Workflows**, click **Release Bundles**.
3. Click the workflow run you want (identified by date and the commit that
   triggered it).
4. Scroll to the **Artifacts** section at the bottom of the run summary page.
5. Click **Linux.x86_64** or **Linux.aarch64** to download.

GitHub wraps the file in a zip on download, so you receive
`Linux.x86_64.zip` containing `BEAST.v2.8.0.Linux.x86_64.tgz`.
Extract the inner tgz before distributing it.

### Retention — artifacts expire after 90 days

Artifacts are automatically and permanently deleted after 90 days.
To keep a release file indefinitely, attach it to a **GitHub Release** as a
Release Asset. Release Assets have no expiry and are publicly accessible at a
stable URL under the repository's **Releases** page.

Do this manually after downloading the artifact, or add a step to the workflow:

```bash
# create the release if it does not exist yet, then upload
gh release create "v2.8.0" --title "BEAST v2.8.0" --draft 2>/dev/null || true
gh release upload "v2.8.0" BEAST.v2.8.0.Linux.x86_64.tgz --clobber
```

Or as a workflow step (requires `contents: write` permission):

```yaml
- name: Attach tgz to GitHub Release
  run: |
    gh release create "v${VERSION}" --title "BEAST v${VERSION}" --draft 2>/dev/null || true
    gh release upload "v${VERSION}" "dist/BEAST.v${VERSION}.Linux.x86_64.tgz" --clobber
  env:
    GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## How assemble-bundle.sh works

`release/Linux/assemble-bundle.sh` (called by `test-linux-on-mac.sh`):

1. **Stage JARs** — copies beast-fx + dependencies into `staging-<OS_ARCH>/`.
   All `javafx-*` and `jdk-jsobject-*` JARs excluded (already in the bundled
   JRE+FX as platform modules).
2. **Create bundle dirs** — `bin/`, `examples/`, `jre/`, `lib/`
3. **Copy JARs → lib/** — boot-layer JARs (beast-pkgmgmt + deps, DensiTree.jar).
4. **Remove core modules from lib/** — `beast-base-*.jar` and `beast-fx-*.jar`
   are deleted from `lib/` so they are not on the boot module path.
5. **Bundle core package ZIPs** — `BEAST.base.package.v*.zip` and
   `BEAST.app.package.v*.zip` are placed in `lib/packages/`. On first launch,
   `BeastLauncher.seedBundledPackage()` extracts them into `~/.beast/2.8/` and
   loads them as plugin `ModuleLayer`s, allowing patch releases without a new tgz.
6. **Copy JRE+FX** — `cp -a` from `JRE_DIR` to preserve all symlinks inside the
   JRE tree (a plain `cp -r` dereferences symlinks, corrupting the runtime).
7. **Copy bin/ scripts** — static launcher scripts are copied from
   `release/Linux/linuxbin/` into `bin/` and made executable. Each script
   resolves `BUNDLE_HOME` symlink-safely, sets `JAVA_HOME=$BUNDLE_HOME/jre`,
   and invokes `jre/bin/java` with `--module-path lib/`.
8. **Copy examples** — beast3 XML files, `nexus/`, and `fasta/` from
   `beast-base/src/test/resources/beast.base/examples/` into `examples/`.
   `beast2vs1/` and `legacy/` subdirectories are excluded from the release.
9. **Copy version.xml and docs** — `version.xml`, `README.txt`, `LICENSE.txt`.
10. **Verify bundle** — checks required files, JAR count, beast-base/beast-fx
    absent from `lib/`, package ZIPs present in `lib/packages/`, bin/
    executability, and `jre/bin/java` arch and JavaFX `.so` libs.
    Fails immediately if any check fails.
11. **Create tgz** — packages `BEAST.v<VERSION>/` into
    `dist/BEAST.v<VERSION>.<OS_ARCH>.tgz`.

Staging is deleted after assembly. The `release/dist/BEAST.v<version>/` bundle is kept
for inspection; delete it manually once the tgz is verified.
