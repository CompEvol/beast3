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
