# Release

## Platform coverage

| Platform | Built by |
|---|---|
| Linux x86, Linux aarch64 | `release-bundles.yml` (CI) |
| Windows | `release-bundles.yml` (CI) |
| Mac DMG | `release/Mac/build-sign-dmg.sh` (local, requires Apple signing) — see [Mac/README.md](Mac/README.md) |

## Release steps

1. Push a `v*` tag → `ci-publish.yml` publishes all modules to Maven Central.
2. Trigger `Release Bundles` via GitHub Actions UI. No inputs — version is read from `pom.xml` (`-SNAPSHOT` stripped).
3. Download bundles from the workflow run page (Artifacts section, 90-day retention).
4. Test locally, then upload to the GitHub Release alongside the Mac DMG.

## Bundled packages

From 2.8.0 the installers carry five packages next to `BEAST.base` and `BEAST.app`: **bModelTest, ORC, SA, StarBeast3 and CCD** ([#248](https://github.com/CompEvol/beast3/issues/248)). Their zips go in the bundle's `packages/` folder (`Contents/app/packages/` on the Mac, `lib/packages/` on Linux and Windows). On first launch the launcher seeds them into the user's package folder ([#276](https://github.com/CompEvol/beast3/pull/276)).

A package zip is compiled against one build of `beast-base` and `beast-fx`. It has to work with the core it ships with, whatever its `version.xml` says it needs. So the packages are built against the release candidate, and the order of a release is:

1. **Publish the core.** Tag the release candidate (e.g. `v2.8.0-rc3`); `ci-publish.yml` publishes `beast-pkgmgmt`, `beast-base` and `beast-fx` to Maven Central.
2. **Build and release each bundled package against it.** In the package's `pom.xml`, set `beast.version` to the candidate (e.g. `2.8.0-rc3`), build, and attach the package zip to a GitHub release of the package. The package's maintainer normally does this. If they cannot in time, build it from a pinned commit of the package's repository.
3. **Pin the zips.** `release/bundled-packages.tsv` lists, for each package, the version, the URL of the released zip and its SHA-256. The bundle scripts take exactly these files and check each checksum.
4. **Check the pinned zips against the candidate**, with the package check in [beast3-test](https://github.com/CompEvol/beast3-test) (`packages/`): build, examples, BEAUti scenarios and TreeAnnotator with CCD. This tests what ships, not the packages' default branches.
5. **Build the installers.** `release/Mac/build-sign-dmg.sh`, `release/Linux/assemble-bundle.sh` and `release/Windows/assemble-bundle.sh` put the pinned zips into `packages/` next to the two core zips.

**From the last release candidate to the final release**, the API of `beast-base` and `beast-fx` that packages use does not change (see "Changes to `beast-base`" in [scripts/DevNotes.md](../scripts/DevNotes.md)). The packages built against the last candidate then ship in the final release unchanged; only the core is rebuilt. If that API has to change after all, the order above is repeated for the final release.

Status for 2.8.0 (12 Nov): the launcher seeds the bundled packages (#276). Still to do: `bundled-packages.tsv`, and the three bundle scripts, which copy only the two core zips.

## Artifacts produced by CI

| Artifact | File |
|---|---|
| `Linux.x86_64` | `BEAST.v<ver>.Linux.x86_64.tgz` |
| `Linux.aarch64` | `BEAST.v<ver>.Linux.aarch64.tgz` |
| `Windows.x86_64` | `BEAST.v<ver>.Windows.x86_64.zip` |

## Version constraints

Keep `pom.xml` at `x.y.z-SNAPSHOT`; never commit a release version to the poms. Each release build sets the release version itself with `mvn versions:set` before building, because the version names the jars and package zips and is compiled into the module descriptors (e.g. `beast.pkgmgmt@2.8.0`):

| Build | Version from | How |
|---|---|---|
| Linux, Windows (CI) | `pom.xml`, `-SNAPSHOT` stripped | `versions:set` in `release-bundles.yml`, on a throw-away checkout |
| Mac DMG (local) | `version="..."` in `version.xml` | `versions:set` in Step 1 of `build-sign-dmg.sh`; `versions:revert` on exit puts the poms back |

The two sources must agree, so all three bundle scripts check them before doing anything else: `build-sign-dmg.sh` compares `version.xml` with `pom.xml` (without `-SNAPSHOT`), and `Linux/assemble-bundle.sh` and `Windows/assemble-bundle.sh` compare the `VERSION` that CI passes with `version.xml`, which the bundle ships. A mismatch stops the build.

All three bundle scripts also stop with an error if any jar or package zip in the bundle still has `SNAPSHOT` in its name. The 2.8.0-rc3 DMG shipped `beast.pkgmgmt@2.8.0-SNAPSHOT`, because the Mac script did not set the version yet (#297).

## Maven resolver on the classpath

`packagemanager -maven`, BEAUti's "Install from Maven" and the loading of installed Maven packages at every start all need the Maven resolver (`maven-resolver-supplier-mvn4` and its dependencies, 40 jars, about 6.8 MB). Every bundle ships them in a `resolver/` folder next to the boot-layer jars (#297):

| Platform | Resolver jars |
|---|---|
| macOS | `BEAST.app/Contents/app/resolver/` |
| Windows | `BEAST.v<ver>/app/resolver/` |
| Linux | `BEAST.v<ver>/lib/resolver/` |

They come from `beast-pkgmgmt/target/resolver/`, which the `copy-resolver` execution in `beast-pkgmgmt/pom.xml` writes during `mvn package`. It copies every runtime dependency of beast-pkgmgmt, which today is only the resolver and `slf4j-nop`, both `<optional>`. A new runtime dependency of beast-pkgmgmt would land in `resolver/` too, so exclude it there or stage it separately.

**They go on the classpath, not the module path.** Maven's own jars split two packages between them (`org.apache.maven.artifact.repository.metadata` in `maven-artifact` and `maven-repository-metadata`; `org.apache.maven.model.merge` in `maven-model` and `maven-model-builder`). On the module path they become automatic modules, and the JVM refuses to start with a `ResolutionException`. On the classpath they all go into the unnamed module, where split packages are allowed. The `resolver/` subfolder is not scanned by `--module-path`, which only looks at the jars directly in the folder.

Every launcher (bin scripts, `.bat` files, jpackage `.cfg` files and the Mac wrapper apps) therefore has:

```
-cp "<app>/resolver/*" --add-reads beast.pkgmgmt=ALL-UNNAMED --add-modules ALL-MODULE-PATH,java.compiler,...
```

- `--add-reads` lets `beast.pkgmgmt` (on the module path) read the resolver classes in the unnamed module.
- `java.compiler` is needed because a `-m` launch does not resolve `javax.tools` by default, and the mvn4 supplier uses it.

All launchers need this, not only `packagemanager`: every launcher loads the installed Maven packages at start (`PackageManager.loadMavenPackages()`), and without the resolver it only prints `Warning: Maven resolver not available...` and skips them.

The resolver is part of the launcher, not of the core packages, so it is not in the BEAST.base or BEAST.app package zips (`src/assembly/package.xml`).

## Why JavaFX JARs are excluded from the release bundle

### The bundled JRE+FX already provides JavaFX as platform modules

BEAST bundles a Zulu JRE+FX 25 in every release. Running `java --list-modules` on that JRE
confirms JavaFX is baked directly into `lib/modules` as platform modules:

```
javafx.base@25.0.2       javafx.fxml@25.0.2      javafx.swing@25.0.2
javafx.controls@25.0.2   javafx.graphics@25.0.2  javafx.web@25.0.2
javafx.media@25.0.2      jdk.jsobject@25.0.2
```

These are available to every `java` invocation automatically, without any `--module-path`
argument pointing at JAR files.

To verify which OS and architecture the bundled JRE targets, use the `file` command on
its `java` binary:

```bash
file dist/BEAST.v2.8.0/jre/bin/java   # Linux
file dist/BEAST.v2.8.0/runtime/bin/java.exe   # Windows
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
present inside the bundled Zulu JRE+FX, making them redundant in the release bundle.

### Platform modules always shadow module-path JARs

The Java module system enforces a strict precedence rule:

> Platform modules (from `lib/modules`) always take precedence over identically-named
> modules on `--module-path`.

The launch commands use `--module-path $APP --add-modules ALL-MODULE-PATH`. Even if
JavaFX JARs were present in `lib/` or `app/`, the JVM would silently ignore them — it
uses the built-in `javafx.graphics@25.0.2` etc. from `lib/modules` instead.

This applies to all three platforms:

| Platform | How JRE is bundled | JavaFX JARs in lib/ |
|---|---|---|
| Mac | `jpackage --runtime-image` copies Zulu JRE+FX into `runtime/`; JavaFX in `runtime/lib/modules` | Redundant |
| Linux | Zulu JRE+FX copied with `cp -a` into `jre/`; JavaFX in `jre/lib/modules` | Redundant |
| Windows | `jpackage --runtime-image` copies Zulu JRE+FX into `runtime/`; JavaFX in `runtime/lib/modules` | Redundant |

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
