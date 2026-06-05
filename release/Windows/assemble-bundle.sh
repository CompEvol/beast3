#!/usr/bin/env bash
# assemble-bundle.sh — Assemble the Windows BEAST bundle from a jpackage app-image.
#
# Run from the repository root (Git Bash or WSL) after jpackage has produced dist/BEAST/:
#
#   bash release/Windows/assemble-bundle.sh <VERSION> [dist-dir]
#
# Arguments:
#   VERSION   Release version string, e.g. 2.8.0  (required)
#   dist-dir  Directory that contains the jpackage BEAST/ app-image  (default: dist)
#
# Windows jpackage app-image layout (assumed input):
#   <dist-dir>/BEAST/
#     BEAST.exe              ← native Windows launcher (GUI, opens file-chooser dialog)
#     app/                   ← JARs + patched BEAST.cfg (module-path mode)
#     runtime/bin/java.exe   ← bundled JRE
#
# Output:
#   <dist-dir>/BEAST.v<VERSION>/
#     BEAST/       ← the jpackage app-image (copied in)
#     bat/         ← .bat wrappers for each tool (CRLF line endings)
#     examples/    ← sample XML files from beast-base
#     tools/       ← DensiTree.jar (if present in release/common/tools/)
#     README.txt
#     LICENSE.txt
set -euo pipefail

VERSION="${1:?Usage: $0 <VERSION> [dist-dir]}"
DIST="${2:-dist}"

BUNDLE="$DIST/BEAST.v${VERSION}"
mkdir -p "$BUNDLE/bat" "$BUNDLE/examples"

# ── jpackage app-image ────────────────────────────────────────────────────────
cp -r "$DIST/BEAST" "$BUNDLE/"

# ── Batch wrappers ────────────────────────────────────────────────────────────
# Windows jpackage layout:
#   BEAST\runtime\bin\java.exe  ← bundled JRE
#   BEAST\app                   ← module-path (JARs + BEAST.cfg)
#
# printf is used throughout so each line gets an explicit \r\n (CRLF), which
# ensures the .bat files work correctly in all Windows environments.

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

# ── DensiTree (optional standalone JAR) ──────────────────────────────────────
DENSITREE_JAR="release/common/tools/DensiTree.jar"
if [ -f "$DENSITREE_JAR" ]; then
    mkdir -p "$BUNDLE/tools"
    cp "$DENSITREE_JAR" "$BUNDLE/tools/"
    printf '@echo off\r\n'                                                           > "$BUNDLE/bat/densitree.bat"
    printf 'set BUNDLE_HOME=%%~dp0..\r\n'                                          >> "$BUNDLE/bat/densitree.bat"
    printf '"%%BUNDLE_HOME%%\\BEAST\\runtime\\bin\\java.exe" -Xmx4g -Duser.language=en -Dfile.encoding=UTF-8 -jar "%%BUNDLE_HOME%%\\tools\\DensiTree.jar" %%*\r\n' >> "$BUNDLE/bat/densitree.bat"
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
