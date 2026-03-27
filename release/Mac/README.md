# Mac Release

Last update 2026-03-27

## Prerequisites

- **JDK 25+** with `jpackage` on your PATH
- **Maven** 3.6+
- macOS (for `hdiutil`, `codesign`, `osascript`)
- **Developer ID Application** certificate in your Keychain

## Building the DMG

```bash
cd release/Mac
./build-sign-dmg.sh
```

Produces:
- `dmg-staging/BEAST <version>/` — signed `.app` bundles (kept for inspection)
- `BEAST_with_JRE.v<version>.dmg` — signed, distributable DMG

The version number is read from `version.xml` at the repository root.
The Maven artifact version is detected from `beast-fx/target/beast-fx-*.jar`
and may include a `-SNAPSHOT` suffix independently of the release version.

## Notarisation

After building the signed DMG, notarise and staple it before distribution:

```bash
xcrun notarytool submit BEAST_with_JRE.v<version>.dmg \
    --apple-id <username> --password <app-specific-password> \
    --team-id <TEAM_ID> --wait

xcrun stapler staple BEAST_with_JRE.v<version>.dmg
```

Upload the stapled DMG to GitHub.

Reference: https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution

---

## Complete Logic and Flow

### Overall Goal

Produce a signed, distributable `BEAST_with_JRE.v<version>.dmg` that passes
Gatekeeper on any Mac without a separate Java install. All signing happens
in-place to keep codesign seals intact.

### Directory Layout

```
release/Mac/
├── staging/                  ← flat JAR inputs to jpackage (deleted after build)
├── dmg-staging/              ← final DMG layout (kept for inspection)
│   ├── BEAST <version>/      ← OUTPUT: all apps, bin/, examples/, README
│   │   ├── BEAST.app         ← full app with bundled JRE
│   │   ├── BEAUti.app        ← shell-script wrapper
│   │   ├── TreeAnnotator.app ← shell-script wrapper
│   │   ├── LogCombiner.app   ← shell-script wrapper
│   │   ├── AppLauncher.app   ← shell-script wrapper
│   │   ├── DensiTree.app     ← standalone JAR wrapper (if present)
│   │   ├── bin/              ← command-line launcher scripts
│   │   ├── examples/         ← sample XML files
│   │   ├── README.txt
│   │   └── LICENSE.txt
│   ├── .background/          ← install.png for DMG Finder window
│   └── Applications          ← symlink → /Applications
└── BEAST_with_JRE.v<version>.dmg   ← final output
```

### Pre-flight

- Sets `CODESIGN_IDENTITY` to the Developer ID Application certificate.
- Auto-generates `entitlements.plist` (3 JVM entitlements: `allow-jit`,
  `allow-unsigned-executable-memory`, `allow-dyld-environment-variables`)
  if the file does not already exist.
- Parses `VERSION` from `version.xml` via Perl.
- Checks `jpackage` is on PATH.

### Step 1 — Maven Build

Runs `mvn clean package -DskipTests` against the repository root `pom.xml`.
Detects the Maven artifact version from the built `beast-fx-*.jar` filename
and derives `MAIN_JAR = beast-pkgmgmt-<mvn_version>.jar`.

### Step 2 — Stage JARs

**Key layout decision:** `OUTPUT` is set to `dmg-staging/BEAST <version>/` so
that jpackage builds BEAST.app directly into its final DMG location. All
signing is done in-place — no post-build copy of a signed app ever happens,
eliminating the main source of codesign seal corruption.

- Copies `beast-fx-*.jar` and all `lib/*.jar` dependencies into `staging/`.
- Prunes cross-platform JavaFX/JDK classifier JARs, keeping only `-mac-*`
  (the others are empty Maven resolution artifacts with no real module content).
- Verifies `MAIN_JAR` is present before proceeding.

### Step 3 — Create BEAST.app

`jpackage --type app-image` builds a native macOS `.app` with a bundled JRE
into `OUTPUT/`. **No signing flag is passed to jpackage** — all signing is
done manually in Step 3b for full inside-out control.

**BEAST.cfg patch** (3 `sed` calls immediately after jpackage):
jpackage's `--main-jar/--main-class` writes classpath-mode config. The patch
switches to module-path mode so that module descriptors (`provides`/`requires`)
are visible at runtime and external BEAST packages loaded as `ModuleLayer`s
resolve correctly. `--module/--module-path` cannot be used at jpackage time
because automatic modules (e.g. `antlr4-runtime.jar`) are incompatible with
jlink's `ALL-MODULE-PATH` resolution.

**Restore `bin/java`:** jpackage strips the `java` binary from the bundled
runtime (it uses its own native launcher). The binary is copied from the build
JDK so that wrapper `.app` bundles and `bin/` scripts can call java from the
bundled JRE rather than whatever java is on the user's PATH.

### Step 3a — Create Wrapper .app Bundles

`build_wrapper_app()` creates four shell-script bundles — BEAUti,
TreeAnnotator, LogCombiner, AppLauncher — each containing:
- A shell-script launcher in `MacOS/` that resolves `BEAST.app/Contents`
  relative to its own location (`../../BEAST.app/Contents`) and invokes the
  bundled `bin/java` with `--module-path $BEAST_APP/app -m module/class`.
  BEAUti gets the extra argument `-capture`.
- `Info.plist` (bundle ID `org.beast3.<name>`, version, icon reference).
- `PkgInfo` (`APPL????`).
- `.icns` icon in `Resources/`.

**DensiTree** is handled separately: it is a standalone JAR (not a BEAST
module), so it gets `Java/DensiTree.jar` and its launcher calls `java -jar`
instead of `-m module/class`. It borrows BEAST.app's bundled JRE via the same
relative path.

**JRE CFBundleIdentifier patch:** `plutil` sets the runtime bundle's identifier
to `beast.pkgmgmt.launcher.runtime` before signing. Apple's `codesign --deep`
rejects nested bundles with duplicate identifiers, which jpackage's generic JRE
identifier can trigger.

### Step 3b — Code-Sign All Bundles

Signing follows a strict **inside-out order** — deepest files first, then each
enclosing bundle. Signing an inner file after its outer bundle is sealed
immediately invalidates the outer seal.

**BEAST.app (4 steps):**

1. **Sign JRE binaries** — `find Contents/runtime -type f` matching `.dylib`,
   `.so`, or executable (`+111`). Signs the entire bundled JRE in one pass.
2. **Sign native launcher** — `Contents/MacOS/BEAST` only. Signing all of
   `MacOS/` was tried but caused duplicate-signing warnings.
3. **Seal outer bundle** — `codesign --force` with `--entitlements` on
   `BEAST.app`. Computes the resource seal over all `Contents/` files and
   embeds the Developer ID signature.
4. **Verify** — `--verify --deep` plus two strict spot-checks on `BEAST` and
   `libjli.dylib` (the two known trouble-makers for JVM bundle signing).

**Wrapper apps** (`sign_wrapper_app()`, called for each of the 5 wrappers):

1. Attempt to sign individual non-`MacOS/` files (icons, plists, JARs).
   `codesign` silently ignores non-Mach-O files; this loop is kept in case a
   wrapper ever gains a real binary resource.
2. Seal the outer bundle with the same entitlements file. Wrapper apps are pure
   shell scripts and do not require JVM entitlements, but passing them is
   accepted by notarization (Apple only rejects missing required entitlements).
3. Verify with `--verify --deep`.

### Step 4 — Assemble and Create DMG

Non-bundle content is added **after** signing, so it cannot affect any bundle
seal (it lives alongside `.app` bundles, not inside them):

- `bin/` — copied from `release/Linux/jrebin`, made executable.
- `examples/` — top-level XML files and `nexus/` subdirectory from
  `beast-base/src/test/resources/examples`.
- `README.txt` and `LICENSE.txt` from `release/common/`.

**DMG creation (two-phase):**

1. `hdiutil create -format UDRW` — creates a 2 GB writable disk image. A
   read-write image is required because AppleScript needs to write Finder
   window layout metadata to the mounted volume.
2. `hdiutil attach` — mounts the UDRW image and captures the device node.
3. **AppleScript** configures the Finder window: icon view, background image
   (`install.png`), icon size 72 px, versioned folder at (150, 150),
   Applications symlink at (400, 150). Failure is non-fatal (cosmetic only).
4. `sleep 2` — lets Finder finish writing `.DS_Store` before the volume is
   locked.
5. `chmod -Rf go-w` — removes write permissions from the mounted volume.
6. `sync; sync` — flushes all pending filesystem writes before detaching.
7. `hdiutil detach` — unmounts (falls back to `--force` if needed).
8. `hdiutil convert -format UDZO -imagekey zlib-level=9` — compresses the
   UDRW into the final read-only DMG. The temp `pack.temp.dmg` is deleted.


### DMG Signing

`codesign` signs the `.dmg` file itself — a container-level signature separate
from the bundle seals inside. This allows Gatekeeper and the notarisation
service to verify the disk image was not tampered with in transit. The `.app`
bundle seals are independent and were already verified in Step 3b.
Verified with `--verify --verbose=4 --deep --strict`.

### Key Design Decisions

| Decision | Reason |
|---|---|
| `OUTPUT` inside `dmg-staging/` | jpackage builds in-place; no post-build copy of signed apps needed |
| No `--mac-app-image-sign-identity` in jpackage | All signing done manually for full inside-out order control |
| `--main-jar` + cfg patch instead of `--module` | Automatic modules (antlr4) incompatible with jlink `ALL-MODULE-PATH` |
| Restore `bin/java` manually | jpackage strips it; wrappers and `bin/` scripts need it |
| Unique JRE `CFBundleIdentifier` | Apple rejects `--deep` signing with duplicate nested bundle IDs |
| `bin/` and `examples/` added after signing | Outside any `.app` bundle; cannot affect bundle seals |
| UDRW → UDZO two-phase DMG | AppleScript Finder layout requires a writable mount |
