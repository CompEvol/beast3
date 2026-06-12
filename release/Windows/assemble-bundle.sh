#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# assemble-bundle.sh — Assemble, verify, and package a BEAST 3 Windows bundle.
#
# Uses jpackage --type app-image with --add-launcher to produce one native .exe
# per tool (beast.exe, beauti.exe, treeannotator.exe, …) placed directly at
# the bundle root so users can double-click any tool to launch it.
#
# Strategy:
#   1. jpackage (from PATH) creates a temp app-image named beast/ (--name beast).
#      --runtime-image bundles $JRE_DIR (Zulu JRE+FX) directly as runtime/,
#      bypassing jlink. --add-launcher adds one .exe per additional tool.
#   2. The app-image is renamed to BUNDLE/ within DEST so exe files, app/, and
#      runtime/ all land directly under BEAST.v<VERSION>/.
#   3. All .cfg files in app/ are patched to module-path mode (inline sed).
#
# --main-jar is used (not --module) to avoid jlink failing on automatic modules
# (e.g. antlr4-runtime has no module-info). Module resolution at runtime is handled
# by the cfg patch (--module-path=$APPDIR --add-modules=ALL-MODULE-PATH in [JavaOptions]).
#
# Required env vars:
#   VERSION       Release version, e.g. 2.8.0
#   JRE_DIR       Zulu JRE+FX 25 home — used as --runtime-image for jpackage.
#                 Does not need to contain jpackage; jpackage is resolved from PATH.
#
# Optional env vars:
#   REPO_ROOT     Repo root directory (default: auto-detected from script location)
#   DEST          Output parent directory (default: <release>/dist)
#   OS_ARCH       Bundle label used in zip name (default: Windows.x86_64)
#
# Output:
#   $DEST/BEAST.v<VERSION>/
#     beast.exe, beauti.exe, treeannotator.exe, …   ← double-clickable launchers
#     app/      ← JARs + patched .cfg files (module path at runtime)
#     runtime/  ← bundled JRE  (runtime/bin/java.exe)
#     bat/      ← .bat CLI launchers
#     examples/, version.xml, README.txt, LICENSE.txt
#   $DEST/BEAST.v<VERSION>.<OS_ARCH>.zip
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RELEASE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="${REPO_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"
VERSION="${VERSION:?ERROR: VERSION is required. Example: VERSION=2.8.0 bash assemble-bundle.sh}"
JRE_DIR="${JRE_DIR:?ERROR: JRE_DIR is required — set it to the Zulu JRE+FX 25 home for the target platform}"
DEST="${DEST:-$RELEASE_DIR/dist}"
OS_ARCH="${OS_ARCH:-Windows.x86_64}"

STAGING="$RELEASE_DIR/staging-${OS_ARCH}"
BUNDLE="$DEST/BEAST.v${VERSION}"
ZIP="$DEST/BEAST.v${VERSION}.${OS_ARCH}.zip"

echo "==> Assembling BEAST ${VERSION} for ${OS_ARCH}"
echo "    JRE_DIR:     ${JRE_DIR}"
echo "    Output:      ${BUNDLE}"

# ── Cleanup trap ──────────────────────────────────────────────────────────────
# LAUNCHERS_DIR is set in Step 2 and cleared after jpackage consumes it.
# The trap handles both normal and error exits.
LAUNCHERS_DIR=
trap 'rm -rf "$STAGING" ${LAUNCHERS_DIR:+"$LAUNCHERS_DIR"}' EXIT

# ── Validate inputs ───────────────────────────────────────────────────────────
if [ ! -d "$JRE_DIR" ]; then
    echo "ERROR: JRE_DIR path does not exist: $JRE_DIR" >&2
    exit 1
fi
if ! jpackage --version >/dev/null 2>&1; then
    echo "ERROR: jpackage not found on PATH — install JDK+FX before running this script" >&2
    exit 1
fi

# ── Step 1: Stage JARs ────────────────────────────────────────────────────────
echo ""
echo "==> Step 1: Staging JARs..."

FX_JAR=$(find "$REPO_ROOT/beast-fx/target" -maxdepth 1 -name "beast-fx-*.jar" \
    ! -name "*-sources*" ! -name "*-javadoc*" ! -name "*-tests*" | head -1)
[ -n "$FX_JAR" ] || { echo "ERROR: beast-fx JAR not found — run mvn package first." >&2; exit 1; }
MVN_VER=$(basename "$FX_JAR" | sed 's/^beast-fx-//;s/\.jar$//')
MAIN_JAR="beast-pkgmgmt-${MVN_VER}.jar"

rm -rf "$STAGING"
mkdir -p "$STAGING"
cp "$FX_JAR" "$STAGING/"
# JavaFX and jdk-jsobject JARs are excluded: the bundled Zulu JRE+FX already provides
# javafx.* and jdk.jsobject as platform modules in lib/modules. Platform modules always
# take precedence over module-path JARs, so staging them is redundant (~46 MB wasted).
find "$REPO_ROOT/beast-fx/target/lib" -name "*.jar" \
    ! -name "javafx-*" ! -name "jdk-jsobject-*" \
    -exec cp {} "$STAGING/" \;

if [ ! -f "$STAGING/$MAIN_JAR" ]; then
    echo "ERROR: main JAR not found in staging: $MAIN_JAR" >&2
    exit 1
fi

# DensiTree — include in staging so jpackage places it in app/ alongside BEAST JARs
DENSITREE_FOUND=false
DENSITREE_JAR="$RELEASE_DIR/common/tools/DensiTree.jar"
if [ -f "$DENSITREE_JAR" ]; then
    cp "$DENSITREE_JAR" "$STAGING/"
    DENSITREE_FOUND=true
    echo "    + DensiTree.jar added to staging"
else
    echo "    WARNING: DensiTree.jar not found — densitree.bat will not work"
fi

JAR_COUNT=$(find "$STAGING" -name "*.jar" | wc -l | tr -d ' ')
echo "    Staged ${JAR_COUNT} JARs (main: ${MAIN_JAR})"

# ── Step 2: Generate per-launcher properties files for --add-launcher ─────────
echo ""
echo "==> Step 2: Generating launcher properties files..."
LAUNCHERS_DIR=$(mktemp -d)

# Common JVM flags shared by every launcher. Each call to _launcher() writes
# the per-launcher lines first (main-class, optional arguments) then appends
# the shared java-options so they don't need to be repeated six times.
_launcher() {
    local file=$1; shift
    {
        printf '%s\n' "$@"
        printf 'java-options=-Xss256m -Xmx8g -Duser.language=en -Dfile.encoding=UTF-8\n'
    } > "$file"
}

_launcher "$LAUNCHERS_DIR/beauti.properties" \
    "main-class=beast.pkgmgmt.launcher.BeautiLauncher" \
    "arguments=-capture"
_launcher "$LAUNCHERS_DIR/treeannotator.properties" \
    "main-class=beast.pkgmgmt.launcher.TreeAnnotatorLauncher"
_launcher "$LAUNCHERS_DIR/logcombiner.properties" \
    "main-class=beast.pkgmgmt.launcher.LogCombinerLauncher"
_launcher "$LAUNCHERS_DIR/loganalyser.properties" \
    "main-class=beast.pkgmgmt.launcher.AppLauncherLauncher" \
    "arguments=beastfx.app.tools.LogAnalyser"
_launcher "$LAUNCHERS_DIR/applauncher.properties" \
    "main-class=beast.pkgmgmt.launcher.AppLauncherLauncher"
_launcher "$LAUNCHERS_DIR/packagemanager.properties" \
    "main-class=beast.pkgmgmt.PackageManager"

# ── Step 3: jpackage app-image ────────────────────────────────────────────────
# --name beast → primary exe is beast.exe; --add-launcher adds one exe per tool.
# --runtime-image copies $JRE_DIR (Zulu JRE+FX) into runtime/ as-is (no jlink).
# jpackage writes to DEST/beast/ (named after --name); Step 4 renames it to
# BUNDLE/ so all output stays within DEST and no cross-filesystem move is needed.
echo ""
echo "==> Step 3: Running jpackage..."
mkdir -p "$DEST"
rm -rf "$BUNDLE" "$DEST/beast"

jpackage --type app-image \
    --name            "beast" \
    --app-version     "$VERSION" \
    --input           "$STAGING" \
    --main-jar        "$MAIN_JAR" \
    --main-class      "beast.pkgmgmt.launcher.BeastLauncher" \
    --java-options    "-Xss256m" \
    --java-options    "-Xmx8g" \
    --java-options    "-Duser.language=en" \
    --java-options    "-Dfile.encoding=UTF-8" \
    --runtime-image   "$JRE_DIR" \
    --icon            "$RELEASE_DIR/common/icons/beast.ico" \
    --dest            "$DEST" \
    --add-launcher    "beauti=$LAUNCHERS_DIR/beauti.properties" \
    --add-launcher    "treeannotator=$LAUNCHERS_DIR/treeannotator.properties" \
    --add-launcher    "logcombiner=$LAUNCHERS_DIR/logcombiner.properties" \
    --add-launcher    "loganalyser=$LAUNCHERS_DIR/loganalyser.properties" \
    --add-launcher    "applauncher=$LAUNCHERS_DIR/applauncher.properties" \
    --add-launcher    "packagemanager=$LAUNCHERS_DIR/packagemanager.properties"

rm -rf "$LAUNCHERS_DIR"; LAUNCHERS_DIR=  # consumed; clear so EXIT trap skips it

# ── Step 4: Rename app-image to BUNDLE/ ───────────────────────────────────────
# jpackage created DEST/beast/ — rename it to BUNDLE/ (BEAST.v<VERSION>/) so
# exe files, app/, and runtime/ all live at the versioned bundle root.
echo ""
echo "==> Step 4: Renaming app-image to bundle root..."
mv "$DEST/beast" "$BUNDLE"
echo "    Exe files at bundle root: $(find "$BUNDLE" -maxdepth 1 -name '*.exe' | wc -l | tr -d ' ')"

# ── Step 5: Patch all .cfg files to module-path mode ─────────────────────────
# jpackage emits classpath-mode cfg when --main-jar is used. Three transforms:
#   1. Delete app.classpath= (all JARs go on module-path, not classpath)
#   2. app.mainclass=<cls> → app.mainmodule=beast.pkgmgmt/<cls> (module-path launch)
#   3. Inject --module-path=$APPDIR and --add-modules=ALL-MODULE-PATH into [JavaOptions]
#      so module descriptors (provides/requires) resolve at runtime.
# $APPDIR is jpackage's own placeholder; the native launcher resolves it at runtime.
echo ""
echo "==> Step 5: Patching .cfg files to module-path mode..."
for cfg in "$BUNDLE/app/"*.cfg; do
    sed -i \
        -e '/^app\.classpath/d' \
        -e 's|^app\.mainclass=\(.*\)|app.mainmodule=beast.pkgmgmt/\1|' \
        -e '/^\[JavaOptions\]/a java-options=--module-path=$APPDIR\njava-options=--add-modules=ALL-MODULE-PATH' \
        "$cfg"
done

# ── Step 6: Copy bat/ CLI launchers ──────────────────────────────────────────
echo ""
echo "==> Step 6: Copying bat/ launchers from Windows/bat/..."
WINBIN_DIR="$SCRIPT_DIR/bat"
[ -d "$WINBIN_DIR" ] || { echo "ERROR: bat/ not found at $WINBIN_DIR" >&2; exit 1; }
mkdir -p "$BUNDLE/bat"
cp "$WINBIN_DIR/"*.bat "$BUNDLE/bat/"

# ── Step 7: Copy examples ─────────────────────────────────────────────────────
echo ""
echo "==> Step 7: Copying examples..."
EXAMPLES_DIR="$REPO_ROOT/beast-base/src/test/resources/beast.base/examples"
if [ -d "$EXAMPLES_DIR" ]; then
    mkdir -p "$BUNDLE/examples/spec"
    find "$EXAMPLES_DIR" -maxdepth 1 -name "*.xml" -exec cp {} "$BUNDLE/examples/" \;
    [ -d "$EXAMPLES_DIR/spec" ] && \
        find "$EXAMPLES_DIR/spec" -maxdepth 1 -name "*.xml" -exec cp {} "$BUNDLE/examples/spec/" \;
    [ -d "$EXAMPLES_DIR/nexus" ] && cp -r "$EXAMPLES_DIR/nexus" "$BUNDLE/examples/"
    EXAMPLE_COUNT=$(find "$BUNDLE/examples" -name "*.xml" | wc -l | tr -d ' ')
    echo "    Copied ${EXAMPLE_COUNT} example XML files (incl. spec/)"
else
    echo "    WARNING: examples not found at $EXAMPLES_DIR"
fi

# ── Step 8: Copy version.xml and docs ────────────────────────────────────────
echo ""
echo "==> Step 8: Copying version.xml and docs..."
cp "$REPO_ROOT/version.xml" "$BUNDLE/"
[ -f "$RELEASE_DIR/common/README.txt"  ] && cp "$RELEASE_DIR/common/README.txt"  "$BUNDLE/"
[ -f "$RELEASE_DIR/common/LICENSE.txt" ] && cp "$RELEASE_DIR/common/LICENSE.txt" "$BUNDLE/"

# ── Cleanup staging ───────────────────────────────────────────────────────────
rm -rf "$STAGING"

# ── Step 9: Verify bundle ─────────────────────────────────────────────────────
echo ""
echo "==> Step 9: Verifying bundle..."
PASS=0; FAIL=0
_ok()   { echo "  [ok]  $*"; PASS=$((PASS + 1)); }
_fail() { echo "  [FAIL] $*"; FAIL=$((FAIL + 1)); }

for f in beast.exe beauti.exe treeannotator.exe logcombiner.exe \
          loganalyser.exe applauncher.exe packagemanager.exe \
          runtime/bin/java.exe app version.xml examples/spec; do
    [ -e "$BUNDLE/$f" ] && _ok "$f present" || _fail "$f MISSING"
done

for f in applauncher.bat beast.bat beauti.bat densitree.bat \
          loganalyser.bat logcombiner.bat packagemanager.bat treeannotator.bat; do
    [ -e "$BUNDLE/bat/$f" ] && _ok "bat/$f present" || _fail "bat/$f MISSING"
done

CFG_COUNT=$(find "$BUNDLE/app" -name "*.cfg" | wc -l | tr -d ' ')
[ "$CFG_COUNT" -ge 7 ] \
    && _ok "app/ has ${CFG_COUNT} .cfg files" \
    || _fail "app/ has too few .cfg files: ${CFG_COUNT}"

grep -q 'app.mainmodule' "$BUNDLE/app/beast.cfg" \
    && _ok "app/beast.cfg patched to module-path mode" \
    || _fail "app/beast.cfg not patched — still classpath mode"

SPEC_COUNT=$(find "$BUNDLE/examples/spec" -name "*.xml" 2>/dev/null | wc -l | tr -d ' ')
[ "$SPEC_COUNT" -gt 0 ] \
    && _ok "examples/spec/ has ${SPEC_COUNT} XML files" \
    || _fail "examples/spec/ is empty — no XML files copied"

if $DENSITREE_FOUND; then
    [ -f "$BUNDLE/app/DensiTree.jar" ] \
        && _ok "app/DensiTree.jar present" \
        || _fail "app/DensiTree.jar MISSING"
fi

if command -v file >/dev/null 2>&1; then
    _FILE_OUT=$(file "$BUNDLE/runtime/bin/java.exe" 2>/dev/null || true)
    echo "$_FILE_OUT" | grep -qE "PE32\+.*x86-64" \
        && _ok "runtime/bin/java.exe is Windows x86-64" \
        || _fail "runtime/bin/java.exe arch mismatch — expected PE32+ x86-64, got: $_FILE_OUT"
else
    _ok "runtime/bin/java.exe arch check skipped (file command not available)"
fi

_FX_DLL=$(find "$BUNDLE/runtime/bin" -name "javafx*.dll" 2>/dev/null | head -1)
[ -n "$_FX_DLL" ] \
    && _ok "JavaFX native libs present in runtime/bin/ (.dll)" \
    || _fail "no JavaFX native libs (javafx*.dll) found in runtime/bin/"

if [ "$FAIL" -gt 0 ]; then
    echo "==> FAILED: ${FAIL} check(s) failed, ${PASS} passed — aborting."
    exit 1
fi
echo "==> All ${PASS} checks passed."

# ── Step 10: Create zip ───────────────────────────────────────────────────────
echo ""
echo "==> Step 10: Creating zip..."
(cd "$DEST"
 if command -v 7z >/dev/null 2>&1; then
     7z a -tzip "$(basename "$ZIP")" "BEAST.v${VERSION}" > /dev/null
 else
     powershell -Command \
       "Compress-Archive -Path 'BEAST.v${VERSION}' -DestinationPath '$(basename "$ZIP")' -Force"
 fi)
SIZE=$(du -sh "$ZIP" | cut -f1)

echo ""
echo "==> Done."
echo "    Bundle: $BUNDLE"
echo "    Archive: $ZIP ($SIZE)"
