#!/usr/bin/env bash
# assemble-bundle.sh — Build the Linux BEAST bundle end-to-end.
#
# Run from the repository root after `mvn clean package -DskipTests`:
#
#   bash release/Linux/assemble-bundle.sh <VERSION> [dist-dir]
#
# Arguments:
#   VERSION   Release version string, e.g. 2.8.0  (required)
#   dist-dir  Output directory for the bundle  (default: dist)
#
# Stages:
#   1. Stage JARs — copy beast-fx + dependencies into staging/, then delete
#      all JavaFX/JDK JARs except those matching the host arch (linux or
#      linux-aarch64).  No JAR renaming; classifier suffixes are preserved.
#   2. jpackage   — produces dist/BEAST/ app-image with bundled JRE.
#   3. Patch       — converts BEAST.cfg from classpath to module-path mode.
#   4. Assemble    — adds shell-script wrappers, examples, and docs.
#
# Output:
#   <dist-dir>/BEAST.v<VERSION>/
#     BEAST/       ← jpackage app-image (bin/BEAST  lib/app/  lib/runtime/)
#     bin/         ← shell-script wrappers for each tool
#     examples/    ← sample XML files from beast-base
#     tools/       ← DensiTree.jar (if present)
#     README.txt / LICENSE.txt
set -euo pipefail

VERSION="${1:?Usage: $0 <VERSION> [dist-dir]}"
DIST="${2:-dist}"
STAGING="staging"

# ── Stage JARs ────────────────────────────────────────────────────────────────
# Maven downloads JavaFX JARs for every platform because the openjfx POMs list
# all classifier JARs as dependencies.  Keep only the JARs for the host arch;
# delete all others (foreign classifiers and empty stubs).  No JAR renaming.
rm -rf "$STAGING"
mkdir -p "$STAGING"
cp beast-fx/target/beast-fx-*.jar "$STAGING/"
cp beast-fx/target/lib/*.jar       "$STAGING/"

case "$(uname -s)-$(uname -m)" in
    Linux-x86_64)   FX_PLATFORM="linux"         ;;
    Linux-aarch64)  FX_PLATFORM="linux-aarch64" ;;
    Darwin-arm64)   FX_PLATFORM="mac-aarch64"   ;;  # local testing on M-series Mac
    Darwin-x86_64)  FX_PLATFORM="mac"           ;;  # local testing on Intel Mac
    *) echo "ERROR: unsupported OS/arch: $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

find "$STAGING" \( -name "javafx-*.jar" -o -name "jdk-*.jar" \) \
    ! -name "*-${FX_PLATFORM}.jar" -delete

# ── Detect launcher JAR ───────────────────────────────────────────────────────
# .main-jar is not written to a file here; the name is resolved locally and
# passed directly to jpackage below.
FX_JAR=$(find beast-fx/target -maxdepth 1 -name "beast-fx-*.jar" \
    ! -name "*-sources*" ! -name "*-javadoc*" ! -name "*-tests*" | head -1)
[ -n "$FX_JAR" ] || { echo "ERROR: beast-fx JAR not found — run mvn package first." >&2; exit 1; }
MVN_VER=$(basename "$FX_JAR" | sed 's/^beast-fx-//;s/\.jar$//')
MAIN_JAR="beast-pkgmgmt-${MVN_VER}.jar"

JAR_COUNT=$(find "$STAGING" -name "*.jar" | wc -l | tr -d ' ')
echo "Staged ${JAR_COUNT} JARs  (platform: ${FX_PLATFORM}  main jar: ${MAIN_JAR})"

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
  --icon            "release/common/icons/beast.png" \
  --dest            "$DIST"

# ── Patch BEAST.cfg to module-path mode ──────────────────────────────────────
# jpackage emits classpath-mode BEAST.cfg; module-path mode makes module
# descriptors (provides/requires) visible so external packages loaded via
# ModuleLayer resolve correctly.  Linux layout: BEAST/lib/app/BEAST.cfg
python3 release/patch-beast-cfg.py "$DIST/BEAST/lib/app/BEAST.cfg"

# ── Assemble bundle ───────────────────────────────────────────────────────────
BUNDLE="$DIST/BEAST.v${VERSION}"
mkdir -p "$BUNDLE/bin" "$BUNDLE/examples"
cp -r "$DIST/BEAST" "$BUNDLE/"

# Wrapper shell scripts.
# Linux jpackage layout:
#   BEAST/lib/runtime/bin/java  ← bundled JRE
#   BEAST/lib/app               ← module-path (JARs + BEAST.cfg)
#
# The quoted heredoc (HEADER) writes fixed boilerplate without variable
# interpolation; the exec line is appended via printf so $module_main/$extra
# are expanded by this script's shell, not the generated launcher.
write_script() {
    local name="$1" module_main="$2" extra="${3:-}"
    local out="$BUNDLE/bin/$name"
    cat > "$out" << 'HEADER'
#!/bin/sh
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  expr "$link" : '/.*' > /dev/null && PRG="$link" || PRG="$(dirname "$PRG")/$link"
done
BUNDLE_HOME="$(cd "$(dirname "$PRG")/.." && pwd)"
JAVA="$BUNDLE_HOME/BEAST/lib/runtime/bin/java"
APP="$BUNDLE_HOME/BEAST/lib/app"
if [ -n "$BEAGLE_LIB" ]; then
  export LD_LIBRARY_PATH="${LD_LIBRARY_PATH:+$LD_LIBRARY_PATH:}/usr/local/lib:$BEAGLE_LIB"
else
  export LD_LIBRARY_PATH="${LD_LIBRARY_PATH:+$LD_LIBRARY_PATH:}/usr/local/lib"
fi
HEADER
    printf 'exec "$JAVA" --module-path "$APP" --add-modules ALL-MODULE-PATH \\\n' >> "$out"
    printf '    -Xss256m -Xmx8g -Duser.language=en -Dfile.encoding=UTF-8 \\\n'   >> "$out"
    if [ -n "$extra" ]; then
        printf '    -m %s %s "$@"\n' "$module_main" "$extra"                      >> "$out"
    else
        printf '    -m %s "$@"\n'    "$module_main"                               >> "$out"
    fi
    chmod 755 "$out"
}

write_script beast          beast.pkgmgmt/beast.pkgmgmt.launcher.BeastLauncher
write_script beauti         beast.pkgmgmt/beast.pkgmgmt.launcher.BeautiLauncher         "-capture"
write_script treeannotator  beast.pkgmgmt/beast.pkgmgmt.launcher.TreeAnnotatorLauncher
write_script logcombiner    beast.pkgmgmt/beast.pkgmgmt.launcher.LogCombinerLauncher
write_script applauncher    beast.pkgmgmt/beast.pkgmgmt.launcher.AppLauncherLauncher
write_script packagemanager beast.pkgmgmt/beast.pkgmgmt.PackageManager
write_script loganalyser    beast.pkgmgmt/beast.pkgmgmt.launcher.AppLauncherLauncher    "beastfx.app.tools.LogAnalyser"

# DensiTree (optional standalone JAR).
DENSITREE_JAR="release/common/tools/DensiTree.jar"
if [ -f "$DENSITREE_JAR" ]; then
    mkdir -p "$BUNDLE/tools"
    cp "$DENSITREE_JAR" "$BUNDLE/tools/"
    cat > "$BUNDLE/bin/densitree" << 'DENSISCRIPT'
#!/bin/sh
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG"); link=$(expr "$ls" : '.*-> \(.*\)$')
  expr "$link" : '/.*' > /dev/null && PRG="$link" || PRG="$(dirname "$PRG")/$link"
done
BUNDLE_HOME="$(cd "$(dirname "$PRG")/.." && pwd)"
JAVA="$BUNDLE_HOME/BEAST/lib/runtime/bin/java"
exec "$JAVA" -Xmx4g -Duser.language=en -Dfile.encoding=UTF-8 \
    -jar "$BUNDLE_HOME/tools/DensiTree.jar" "$@"
DENSISCRIPT
    chmod 755 "$BUNDLE/bin/densitree"
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
