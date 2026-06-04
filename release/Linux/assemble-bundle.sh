#!/usr/bin/env bash
# assemble-bundle.sh — Assemble the Linux BEAST bundle from a jpackage app-image.
#
# Run from the repository root after jpackage has produced dist/BEAST/:
#
#   bash release/Linux/assemble-bundle.sh <VERSION> [dist-dir]
#
# Arguments:
#   VERSION   Release version string, e.g. 2.8.0  (required)
#   dist-dir  Directory that contains the jpackage BEAST/ app-image  (default: dist)
#
# Linux jpackage app-image layout (assumed input):
#   <dist-dir>/BEAST/
#     bin/BEAST              ← native ELF launcher
#     lib/app/               ← JARs + patched BEAST.cfg (module-path mode)
#     lib/runtime/bin/java   ← bundled JRE
#
# Output:
#   <dist-dir>/BEAST_with_JRE.v<VERSION>/
#     BEAST/       ← the jpackage app-image (copied in)
#     bin/         ← shell-script wrappers for each tool
#     examples/    ← sample XML files from beast-base
#     tools/       ← DensiTree.jar (if present in release/common/tools/)
#     README.txt
#     LICENSE.txt
set -euo pipefail

VERSION="${1:?Usage: $0 <VERSION> [dist-dir]}"
DIST="${2:-dist}"

BUNDLE="$DIST/BEAST_with_JRE.v${VERSION}"
mkdir -p "$BUNDLE/bin" "$BUNDLE/examples"

# ── jpackage app-image ────────────────────────────────────────────────────────
cp -r "$DIST/BEAST" "$BUNDLE/"

# ── Wrapper shell scripts ─────────────────────────────────────────────────────
# Linux jpackage layout:
#   BEAST/lib/runtime/bin/java  ← bundled JRE
#   BEAST/lib/app               ← module-path (JARs + BEAST.cfg)
#
# The quoted heredoc (HEADER) writes the fixed boilerplate without any variable
# interpolation; the exec line is appended via printf so $module_main/$extra
# are substituted by this script's shell.

write_script() {
    local name="$1" module_main="$2" extra="${3:-}"
    local out="$BUNDLE/bin/$name"
    cat > "$out" << 'HEADER'
#!/bin/sh
# Resolve the true path of this script, following symlinks
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  expr "$link" : '/.*' > /dev/null && PRG="$link" || PRG="$(dirname "$PRG")/$link"
done
BUNDLE_HOME="$(cd "$(dirname "$PRG")/.." && pwd)"
JAVA="$BUNDLE_HOME/BEAST/lib/runtime/bin/java"
APP="$BUNDLE_HOME/BEAST/lib/app"
# Extend LD_LIBRARY_PATH so the optional BEAGLE GPU library can be found
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

# ── DensiTree (optional standalone JAR) ──────────────────────────────────────
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

# ── Examples ──────────────────────────────────────────────────────────────────
EXAMPLES="beast-base/src/test/resources/examples"
if [ -d "$EXAMPLES" ]; then
    find "$EXAMPLES" -maxdepth 1 -name "*.xml" -exec cp {} "$BUNDLE/examples/" \;
    [ -d "$EXAMPLES/nexus" ] && cp -r "$EXAMPLES/nexus" "$BUNDLE/examples/"
fi

# ── Docs ──────────────────────────────────────────────────────────────────────
[ -f release/common/README.txt  ] && cp release/common/README.txt  "$BUNDLE/"
[ -f release/common/LICENSE.txt ] && cp release/common/LICENSE.txt "$BUNDLE/"

echo "Assembled $BUNDLE"
