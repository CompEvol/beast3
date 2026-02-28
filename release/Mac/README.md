# Mac Release

Last update 2026-03-01

## Prerequisites

- **JDK 16+** with `jpackage` on your PATH (JDK 21+ recommended)
- **Maven** 3.6+
- macOS (for `hdiutil` and code signing)

## Building the DMG

The `build-dmg.sh` script builds a macOS DMG containing `.app` bundles for all
BEAST 3 applications, each with a bundled JRE (no separate Java install needed
by end users).

```bash
cd release/Mac
./build-dmg.sh
```

This will:
1. Build the project with Maven (`mvn clean package -DskipTests`)
2. Stage all JARs into a flat directory
3. Create `.app` bundles via `jpackage` for: **BEAST**, **BEAUti**,
   **TreeAnnotator**, **LogCombiner**, **AppLauncher**
4. Package all `.app` bundles into a single DMG with an Applications symlink

Output:
- `output/` — individual `.app` bundles
- `BEAST_with_JRE.v<version>.dmg` — the distributable DMG

The version number is read from `version.xml` at the repository root.

## Notarisation

After building the DMG, notarise and staple it before distribution:

```bash
xcrun notarytool submit BEAST_with_JRE.v3.?.?.dmg \
    --apple-id <username> --password <passwd> --team-id <TEAM_ID> --wait

xcrun stapler staple BEAST_with_JRE.v3.?.?.dmg
```

Upload the stapled DMG to GitHub.

## Workflow

Refer to https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution
