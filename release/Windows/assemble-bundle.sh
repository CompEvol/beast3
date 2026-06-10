#!/usr/bin/env bash
# assemble-bundle.sh — Build the Windows BEAST bundle end-to-end.
#
# Run from the repository root (Git Bash) after `mvn clean package -DskipTests`:
#
#   bash release/Windows/assemble-bundle.sh <VERSION> [dist-dir]
#
# Arguments:
#   VERSION   Release version string, e.g. 2.8.0  (required)
#   dist-dir  Output directory for the bundle  (default: dist)
#
# Stages:
#   1. Stage JARs — copy beast-fx + dependencies into staging/ (JavaFX JARs excluded;
#      bundled JDK already provides them as platform modules).
#   2. jpackage   — produces dist/BEAST/ app-image with bundled JRE.
#                   Windows jpackage requires --app-version to be purely
#                   numeric (x.y.z); keep pom.xml at x.y.z-SNAPSHOT and let
#                   CI strip the suffix before calling this script.
#   3. Patch       — converts BEAST.cfg from classpath to module-path mode.
#   4. Assemble    — adds .bat wrappers, examples, and docs.
#
# Output:
#   <dist-dir>/BEAST.v<VERSION>/
#     BEAST/       ← jpackage app-image (BEAST.exe  app/  runtime/)
#     bat/         ← .bat wrappers for each tool (CRLF line endings)
#     examples/    ← sample XML files from beast-base
#     tools/       ← DensiTree.jar (if present)
#     README.txt / LICENSE.txt
set -euo pipefail

VERSION="${1:?Usage: $0 <VERSION> [dist-dir]}"
DIST="${2:-dist}"
STAGING="staging"

# ── Stage JARs ────────────────────────────────────────────────────────────────
rm -rf "$STAGING"
mkdir -p "$STAGING"
cp beast-fx/target/beast-fx-*.jar "$STAGING/"
# JavaFX and jdk-jsobject JARs are excluded: the bundled JDK already provides
# javafx.* and jdk.jsobject as platform modules in lib/modules. Platform modules always
# take precedence over module-path JARs, so staging them is redundant (~46 MB wasted).
find beast-fx/target/lib -name "*.jar" \
    ! -name "javafx-*" ! -name "jdk-jsobject-*" \
    -exec cp {} "$STAGING/" \;

# ── Detect launcher JAR ───────────────────────────────────────────────────────
FX_JAR=$(find beast-fx/target -maxdepth 1 -name "beast-fx-*.jar" \
    ! -name "*-sources*" ! -name "*-javadoc*" ! -name "*-tests*" | head -1)
[ -n "$FX_JAR" ] || { echo "ERROR: beast-fx JAR not found — run mvn package first." >&2; exit 1; }
MVN_VER=$(basename "$FX_JAR" | sed 's/^beast-fx-//;s/\.jar$//')
MAIN_JAR="beast-pkgmgmt-${MVN_VER}.jar"

JAR_COUNT=$(find "$STAGING" -name "*.jar" | wc -l | tr -d ' ')
echo "Staged ${JAR_COUNT} JARs  (platform: win  main jar: ${MAIN_JAR})"

# ── jpackage app-image ────────────────────────────────────────────────────────
# --main-jar avoids jlink trying to process automatic modules (e.g.
# antlr4-runtime.jar has no module-info and causes jlink to fail).
# --add-modules ALL-MODULE-PATH keeps all JDK modules in the bundled JRE so
# external BEAST packages loaded via ModuleLayer resolve correctly at runtime.
# BEAST.cfg is patched in the next step to switch to module-path mode.
rm -rf "$DIST/BEAST"
mkdir -p "$DIST"
jpackage --type app-image \
  --name            "BEAST" \
  --app-version     "$VERSION" \
  --input           "$STAGING" \
  --main-jar        "$MAIN_JAR" \
  --main-class      "beast.pkgmgmt.launcher.BeastLauncher" \
  --java-options    "-Xss256m" \
  --java-options    "-Xmx8g" \
  --java-options    "-Duser.language=en" \
  --java-options    "-Dfile.encoding=UTF-8" \
  --add-modules     ALL-MODULE-PATH \
  --icon            "release/common/icons/beast.ico" \
  --dest            "$DIST"

# ── Patch BEAST.cfg to module-path mode ──────────────────────────────────────
# jpackage emits classpath-mode BEAST.cfg; module-path mode makes module
# descriptors (provides/requires) visible so external packages loaded via
# ModuleLayer resolve correctly.  Windows layout: BEAST/app/BEAST.cfg
# (no lib/ prefix unlike Linux).
python3 release/patch-beast-cfg.py "$DIST/BEAST/app/BEAST.cfg"

# ── Assemble bundle ───────────────────────────────────────────────────────────
BUNDLE="$DIST/BEAST.v${VERSION}"
mkdir -p "$BUNDLE/bat" "$BUNDLE/examples"
cp -r "$DIST/BEAST" "$BUNDLE/"

# Batch wrappers.
# Windows jpackage layout:
#   BEAST\runtime\bin\java.exe  ← bundled JRE
#   BEAST\app                   ← module-path (JARs + BEAST.cfg)
#
# printf with explicit \r\n produces CRLF line endings so the .bat files
# work correctly in all Windows environments.
write_bat() {
    local name="$1" module_main="$2" extra="${3:-}"
    local out="$BUNDLE/bat/${name}.bat"
    printf '@echo off\r\n'                                                          > "$out"
    printf 'set BUNDLE_HOME=%%~dp0..\r\n'                                         >> "$out"
    printf '"%%BUNDLE_HOME%%\\BEAST\\runtime\\bin\\java.exe" ^\r\n'              >> "$out"
    printf '    --module-path "%%BUNDLE_HOME%%\\BEAST\\app" ^\r\n'               >> "$out"
    printf '    --add-modules ALL-MODULE-PATH ^\r\n'                              >> "$out"
    printf '    -Xss256m -Xmx8g -Duser.language=en -Dfile.encoding=UTF-8 ^\r\n' >> "$out"
    if [ -n "$extra" ]; then
        printf '    -m %s %s %%*\r\n' "$module_main" "$extra"                    >> "$out"
    else
        printf '    -m %s %%*\r\n'    "$module_main"                             >> "$out"
    fi
}

write_bat beast          beast.pkgmgmt/beast.pkgmgmt.launcher.BeastLauncher       "-window -working -options"
write_bat beauti         beast.pkgmgmt/beast.pkgmgmt.launcher.BeautiLauncher       "-capture"
write_bat treeannotator  beast.pkgmgmt/beast.pkgmgmt.launcher.TreeAnnotatorLauncher
write_bat logcombiner    beast.pkgmgmt/beast.pkgmgmt.launcher.LogCombinerLauncher
write_bat applauncher    beast.pkgmgmt/beast.pkgmgmt.launcher.AppLauncherLauncher
write_bat packagemanager beast.pkgmgmt/beast.pkgmgmt.PackageManager
write_bat loganalyser    beast.pkgmgmt/beast.pkgmgmt.launcher.AppLauncherLauncher  "beastfx.app.tools.LogAnalyser"

# DensiTree (optional standalone JAR).
DENSITREE_JAR="release/common/tools/DensiTree.jar"
if [ -f "$DENSITREE_JAR" ]; then
    mkdir -p "$BUNDLE/tools"
    cp "$DENSITREE_JAR" "$BUNDLE/tools/"
    printf '@echo off\r\n'                                                           > "$BUNDLE/bat/densitree.bat"
    printf 'set BUNDLE_HOME=%%~dp0..\r\n'                                          >> "$BUNDLE/bat/densitree.bat"
    printf '"%%BUNDLE_HOME%%\\BEAST\\runtime\\bin\\java.exe" -Xmx4g -Duser.language=en -Dfile.encoding=UTF-8 -jar "%%BUNDLE_HOME%%\\tools\\DensiTree.jar" %%*\r\n' >> "$BUNDLE/bat/densitree.bat"
    echo "    Added DensiTree.jar"
fi

EXAMPLES="beast-base/src/test/resources/examples"
if [ -d "$EXAMPLES" ]; then
    find "$EXAMPLES" -maxdepth 1 -name "*.xml" -exec cp {} "$BUNDLE/examples/" \;
    [ -d "$EXAMPLES/nexus" ] && cp -r "$EXAMPLES/nexus" "$BUNDLE/examples/"
fi

[ -f release/common/README.txt  ] && cp release/common/README.txt  "$BUNDLE/"
[ -f release/common/LICENSE.txt ] && cp release/common/LICENSE.txt "$BUNDLE/"

echo "Assembled $BUNDLE"
