# Linux Release

## Prerequisites

- **JDK 25+** with `mvn` on PATH (for the Maven build on macOS)
- **`cp -a`** support (standard on macOS and Linux — preserves JDK symlinks)
- A **pre-built Linux JDK** for the target arch, set via `JDK_DIR`
  - Zulu (and most OpenJDK vendors) dropped the standalone JRE package format
    starting with Java 25 — use the full JDK Home instead
  - Default in `test-linux-on-mac.sh`: `/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home`
    (the local macOS Zulu 25 install — produces correct structure but `jdk/bin/java`
    will be a macOS binary; for a distributable release set `JDK_DIR` to a
    Linux aarch64 JDK 25+ downloaded from azul.com)
  - `assemble-bundle.sh` has no default — `JDK_DIR` must be set explicitly when
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
JDK_DIR=/path/to/linux-aarch64-jdk25 \
bash Linux/assemble-bundle.sh
```

## Bundle layout

```
BEAST.v<version>/
  bin/              shell launchers (beast, beauti, treeannotator,
                    logcombiner, applauncher, packagemanager,
                    loganalyser, densitree)
  examples/         sample XML files + nexus/
    spec/           beast3 spec XMLs
  jdk/              bundled JDK (target platform)
  lib/              all JARs on the module path (incl. DensiTree.jar)
  version.xml
  README.txt
  LICENSE.txt
```

`bin/` scripts resolve `BUNDLE_HOME` via symlink-safe path resolution and invoke:
```sh
$BUNDLE_HOME/jdk/bin/java --module-path $BUNDLE_HOME/lib --add-modules ALL-MODULE-PATH \
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

1. **Stage JARs** — validates that OS-classified JARs for `javafx-base`,
   `javafx-controls`, and `javafx-graphics` are present in `beast-fx/target/lib/`
   (Maven downloads them automatically when running on the target OS; fails fast
   if missing). Then copies all JARs — both stubs and classified — into
   `staging-<OS_ARCH>/`.
2. **Create bundle dirs** — `bin/`, `examples/`, `jdk/`, `lib/`
3. **Copy JARs → lib/** — flat directory, becomes the module path at runtime.
4. **Copy JDK** — `cp -a` from `JDK_DIR` to preserve all symlinks inside the
   JDK tree (a plain `cp -r` dereferences symlinks, corrupting the runtime).
5. **Copy bin/ scripts** — static launcher scripts are copied from
   `release/Linux/linuxbin/` into `bin/` and made executable. Each script
   resolves `BUNDLE_HOME` symlink-safely, sets `JAVA_HOME=$BUNDLE_HOME/jdk`,
   and invokes `jdk/bin/java` with `--module-path lib/`.
6. **Copy examples** — XML files and `nexus/` from
   `beast-base/src/test/resources/beast.base/examples/`;
   `spec/*.xml` files go into `examples/spec/`.
7. **Copy version.xml and docs** — `version.xml`, `README.txt`, `LICENSE.txt`.
8. **Verify bundle** — checks required files, JAR count, no JavaFX stub JARs remain,
   bin/ executability, and `jdk/bin/java` references.
   Fails immediately if any check fails.
9. **Create tgz** — packages `BEAST.v<VERSION>/` into
   `dist/BEAST.v<VERSION>.<OS_ARCH>.tgz`.

Staging is deleted after assembly. The `release/dist/BEAST.v<version>/` bundle is kept
for inspection; delete it manually once the tgz is verified.
