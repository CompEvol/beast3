# Release

## Platform coverage

| Platform | Built by |
|---|---|
| Linux x86, Linux aarch64 | `release-bundles.yml` (CI) |
| Windows | `release-bundles.yml` (CI) |
| Mac DMG | `release/Mac/build-sign-dmg.sh` (local, requires Apple signing) |

## Release steps

1. Push a `v*` tag → `ci-publish.yml` publishes all modules to Maven Central.
2. Trigger `Release Bundles` via GitHub Actions UI. No inputs — version is read from `pom.xml` (`-SNAPSHOT` stripped).
3. Download bundles from the workflow run page (Artifacts section, 90-day retention).
4. Test locally, then upload to the GitHub Release alongside the Mac DMG.

## Artifacts produced by CI

| Artifact | File |
|---|---|
| `Linux.x86_64` | `BEAST.v<ver>.Linux.x86_64.tgz` |
| `Linux.aarch64` | `BEAST.v<ver>.Linux.aarch64.tgz` |
| `Windows` | `BEAST.v<ver>.Windows.zip` (not yet implemented) |

## Version constraints

Version is read from `pom.xml` and must be plain `x.y.z`. Windows jpackage rejects non-numeric suffixes (e.g. `-rc1`). Keep `pom.xml` at `x.y.z-SNAPSHOT`; the CI strips `-SNAPSHOT` automatically.

## Why JavaFX JARs are excluded from the release bundle

### The bundled JDK already provides JavaFX as platform modules

BEAST bundles a Zulu-FX JDK in every release. Running `java --list-modules` on that JDK
confirms JavaFX is baked directly into `lib/modules` as platform modules:

```
javafx.base@25.0.2       javafx.fxml@25.0.2      javafx.swing@25.0.2
javafx.controls@25.0.2   javafx.graphics@25.0.2  javafx.web@25.0.2
javafx.media@25.0.2      jdk.jsobject@25.0.2
```

These are available to every `java` invocation automatically, without any `--module-path`
argument pointing at JAR files.

To verify which OS and architecture the bundled JDK targets, use the `file` command on
its `java` binary:

```bash
file dist/BEAST.v2.8.0/jdk/bin/java
```

Quick reference for all platforms:

| Output | OS | Architecture |
|---|---|---|
| `Mach-O 64-bit executable arm64` | macOS | Apple Silicon (aarch64) |
| `Mach-O 64-bit executable x86_64` | macOS | Intel (x86_64) |
| `ELF 64-bit LSB executable, ARM aarch64` | Linux | aarch64 |
| `ELF 64-bit LSB executable, x86-64` | Linux | x86_64 |
| `PE32+ executable … x86-64` | Windows | x86_64 |

The native lib extension also confirms the OS: `.dylib` = macOS, `.so` = Linux, `.dll` = Windows.

### What the Maven-produced JavaFX JARs actually are

Maven publishes JavaFX as two artifacts per module:

| Kind | Example | Size | Content |
|---|---|---|---|
| Stub (no classifier) | `javafx-base-25.0.2.jar` | ~302 bytes | `MANIFEST.MF` only — no bytecode, no native libs |
| Classified (platform) | `javafx-base-25.0.2-mac-aarch64.jar` | 732 KB–36 MB | Bytecode + native `.dylib`/`.so`/`.dll` |

The stub is a Maven dependency-resolution trigger. Its POM contains a self-referencing
dependency with `<classifier>${javafx.platform}</classifier>`; the parent POM activates
OS-specific profiles that set `${javafx.platform}` (e.g. `mac-aarch64`, `linux-aarch64`,
`win`) based on the build host, so Maven downloads only the matching classified JAR —
one platform, one build host.

The classified JAR carries the real bytecode and native libraries — but those are already
present inside the bundled Zulu-FX JDK, making them redundant in the release bundle.

### Platform modules always shadow module-path JARs

The Java module system enforces a strict precedence rule:

> Platform modules (from `lib/modules`) always take precedence over identically-named
> modules on `--module-path`.

The launch commands use `--module-path $APP --add-modules ALL-MODULE-PATH`. Even if
JavaFX JARs were present in `lib/` or `app/`, the JVM would silently ignore them — it
uses the built-in `javafx.graphics@25.0.2` etc. from `lib/modules` instead.

This applies to all three platforms:

| Platform | How JDK is bundled | JavaFX JARs in lib/ |
|---|---|---|
| Mac | `jpackage` calls `jlink` with `ALL-MODULE-PATH` from Zulu-FX jmods → baked into `runtime/lib/modules` | Redundant |
| Linux | Full Zulu-FX JDK copied with `cp -a` → ships with `lib/modules` containing JavaFX | Redundant |
| Windows | `jpackage` same as Mac | Redundant |

### Not needed for compilation either

Maven compilation resolves JavaFX from the local Maven repository (`.m2/`), not from
the release `lib/`. The `beast-fx/pom.xml` declares JavaFX as a Maven dependency —
`mvn package` downloads the correct platform-classified JAR from Maven Central at build
time. The release scripts never feed `lib/` back to `javac`.

### Size savings

| JAR (mac-aarch64 classified) | Size |
|---|---|
| `javafx-web-25.0.2-mac-aarch64.jar` | 36 MB |
| `javafx-graphics-25.0.2-mac-aarch64.jar` | 5.3 MB |
| `javafx-controls-25.0.2-mac-aarch64.jar` | 2.5 MB |
| `javafx-media-25.0.2-mac-aarch64.jar` | 1.6 MB |
| `javafx-base-25.0.2-mac-aarch64.jar` | 732 KB |
| `javafx-fxml-25.0.2-mac-aarch64.jar` | 124 KB |
| `javafx-swing-25.0.2-mac-aarch64.jar` | 86 KB |
| 7× stub JARs + `jdk-jsobject-*` | ~4 KB |
| **Total saved per platform** | **~46 MB** |

The release scripts exclude these with:

```bash
find "$REPO_ROOT/beast-fx/target/lib" -name "*.jar" \
    ! -name "javafx-*" ! -name "jdk-jsobject-*" \
    -exec cp {} "$STAGING/" \;
```
