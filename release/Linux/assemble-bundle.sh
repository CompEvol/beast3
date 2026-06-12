#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# assemble-bundle.sh — Assemble, verify, and package a BEAST 3 release bundle.
#
# No jpackage is used. JARs go into lib/ and are invoked via module-path.
# A pre-built Zulu JRE+FX 25 is copied with cp -a (preserves symlinks).
# Use the jre+fx package from azul.com — it is smaller than the full JDK and
# already contains JavaFX as platform modules in lib/modules.
# The bundle folder is named jre/.
#
# Required env vars:
#   VERSION       Release version, e.g. 2.8.0
#   JRE_DIR       Zulu JRE+FX 25 home for the target platform
#
# Optional env vars:
#   REPO_ROOT     Repo root directory (default: auto-detected from script location)
#   DEST          Output parent directory (default: <release>/dist)
#   OS_ARCH       Bundle label used in tgz name, e.g. Linux.aarch64
#                 (default: auto-detected from uname)
#
# Output:
#   $DEST/BEAST.v<VERSION>/          assembled bundle
#   $DEST/BEAST.v<VERSION>.<OS_ARCH>.tgz   distributable archive
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RELEASE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="${REPO_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"
VERSION="${VERSION:?ERROR: VERSION is required. Example: VERSION=2.8.0 bash assemble-bundle.sh}"
JRE_DIR="${JRE_DIR:?ERROR: JRE_DIR is required — set it to the Zulu JRE+FX 25 home for the target platform}"
DEST="${DEST:-$RELEASE_DIR/dist}"

# ── Auto-detection ────────────────────────────────────────────────────────────
_HOST="$(uname -s)-$(uname -m)"
if [ -z "${OS_ARCH:-}" ]; then
    case "$_HOST" in
        Linux-x86_64)   OS_ARCH="Linux.x86_64"  ;;
        Linux-aarch64)  OS_ARCH="Linux.aarch64" ;;
        *) echo "ERROR: cannot auto-detect OS_ARCH for $_HOST — set OS_ARCH explicitly." >&2; exit 1 ;;
    esac
fi

STAGING="$RELEASE_DIR/staging-${OS_ARCH}"
BUNDLE="$DEST/BEAST.v${VERSION}"
TGZ="$DEST/BEAST.v${VERSION}.${OS_ARCH}.tgz"

trap 'rm -rf "$STAGING"' EXIT

echo "==> Assembling BEAST ${VERSION} for ${OS_ARCH}"
echo "    JRE_DIR:     ${JRE_DIR}"
echo "    Output:      ${BUNDLE}"

# ── Validate inputs ───────────────────────────────────────────────────────────
if [ ! -d "$JRE_DIR" ]; then
    echo "ERROR: JRE_DIR path does not exist: $JRE_DIR" >&2
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
JAR_COUNT=$(find "$STAGING" -name "*.jar" | wc -l | tr -d ' ')
echo "    Staged ${JAR_COUNT} JARs (main: ${MAIN_JAR})"

# ── Step 2: Create bundle directory structure ─────────────────────────────────
echo ""
echo "==> Step 2: Creating bundle structure..."
rm -rf "$BUNDLE"
mkdir -p "$BUNDLE/bin" "$BUNDLE/examples" "$BUNDLE/lib"

# ── Step 3: Copy JARs into lib/ ───────────────────────────────────────────────
cp "$STAGING/"*.jar "$BUNDLE/lib/"
echo "    Copied ${JAR_COUNT} JARs to lib/"

# ── Step 4: Copy JRE+FX ──────────────────────────────────────────────────────
# cp -a preserves symlinks. A plain cp -r dereferences JRE internal symlinks,
# corrupting the runtime structure (replaces 1 KB aliases with full files).
echo ""
echo "==> Step 4: Copying Zulu JRE+FX 25..."
cp -a "$JRE_DIR/." "$BUNDLE/jre/"
chmod u+x "$BUNDLE/jre/bin/java"
rm -f "$BUNDLE/jre/lib/src.zip"
echo "    JRE+FX copied ($(du -sh "$BUNDLE/jre" | cut -f1))"

# ── Step 5: Copy bin/ launcher scripts from linuxbin/ ────────────────────────
# Static scripts in linuxbin/ use $BUNDLE_HOME/jre/bin/java and $BUNDLE_HOME/lib.
echo ""
echo "==> Step 5: Copying bin/ launchers from linuxbin/..."
LINUXBIN_DIR="$SCRIPT_DIR/linuxbin"
[ -d "$LINUXBIN_DIR" ] || { echo "ERROR: linuxbin/ not found at $LINUXBIN_DIR" >&2; exit 1; }
cp "$LINUXBIN_DIR/"* "$BUNDLE/bin/"
chmod u+x "$BUNDLE/bin/"*

# DensiTree — copy standalone JAR into lib/ (densitree script is already in linuxbin/)
DENSITREE_JAR="$RELEASE_DIR/common/tools/DensiTree.jar"
if [ -f "$DENSITREE_JAR" ]; then
    cp "$DENSITREE_JAR" "$BUNDLE/lib/"
    echo "    + densitree JAR copied to lib/"
else
    echo "    WARNING: DensiTree.jar not found — densitree script will not work"
fi

# ── Step 6: Copy examples ─────────────────────────────────────────────────────
echo ""
echo "==> Step 6: Copying examples..."
EXAMPLES_DIR="$REPO_ROOT/beast-base/src/test/resources/beast.base/examples"
if [ -d "$EXAMPLES_DIR" ]; then
    find "$EXAMPLES_DIR" -maxdepth 1 -name "*.xml" -exec cp {} "$BUNDLE/examples/" \;
    mkdir -p "$BUNDLE/examples/spec"
    [ -d "$EXAMPLES_DIR/spec" ] && \
        find "$EXAMPLES_DIR/spec" -maxdepth 1 -name "*.xml" -exec cp {} "$BUNDLE/examples/spec/" \;
    [ -d "$EXAMPLES_DIR/nexus" ] && cp -r "$EXAMPLES_DIR/nexus" "$BUNDLE/examples/"
    EXAMPLE_COUNT=$(find "$BUNDLE/examples" -name "*.xml" | wc -l | tr -d ' ')
    echo "    Copied ${EXAMPLE_COUNT} example XML files (incl. spec/)"
else
    echo "    WARNING: examples not found at $EXAMPLES_DIR"
fi

# ── Step 7: Copy version.xml and docs ────────────────────────────────────────
echo ""
echo "==> Step 7: Copying version.xml and docs..."
cp "$REPO_ROOT/version.xml" "$BUNDLE/"
[ -f "$RELEASE_DIR/common/README.txt"  ] && cp "$RELEASE_DIR/common/README.txt"  "$BUNDLE/"
[ -f "$RELEASE_DIR/common/LICENSE.txt" ] && cp "$RELEASE_DIR/common/LICENSE.txt" "$BUNDLE/"

# ── Cleanup staging ───────────────────────────────────────────────────────────
rm -rf "$STAGING"

# ── Step 8: Verify bundle ─────────────────────────────────────────────────────
echo ""
echo "==> Step 8: Verifying bundle..."
PASS=0; FAIL=0
_ok()   { echo "  [ok]  $*"; PASS=$((PASS + 1)); }
_fail() { echo "  [FAIL] $*"; FAIL=$((FAIL + 1)); }

for f in bin/beast bin/beauti bin/treeannotator bin/logcombiner \
          bin/applauncher bin/packagemanager bin/loganalyser \
          jre/bin/java lib version.xml examples/spec; do
    [ -e "$BUNDLE/$f" ] && _ok "$f present" || _fail "$f MISSING"
done

VJAR_COUNT=$(find "$BUNDLE/lib" -name "*.jar" | wc -l | tr -d ' ')
[ "$VJAR_COUNT" -gt 5 ] \
    && _ok "lib/ has ${VJAR_COUNT} JARs" \
    || _fail "lib/ has too few JARs: ${VJAR_COUNT}"

for s in beast beauti treeannotator logcombiner applauncher packagemanager loganalyser; do
    [ -x "$BUNDLE/bin/$s" ] && _ok "bin/$s executable" || _fail "bin/$s not executable"
done

grep -q 'BUNDLE_HOME/jre' "$BUNDLE/bin/beast" \
    && _ok "bin/beast references bundled jre" \
    || _fail "bin/beast does not reference bundled jre"

case "$OS_ARCH" in
    Linux.x86_64)  _ARCH_PAT="ELF.*x86-64"  ;;
    Linux.aarch64) _ARCH_PAT="ELF.*aarch64"  ;;
    *)             _ARCH_PAT="ELF"           ;;
esac
_FILE_OUT=$(file "$BUNDLE/jre/bin/java" 2>/dev/null || true)
echo "$_FILE_OUT" | grep -qE "$_ARCH_PAT" \
    && _ok "jre/bin/java matches $OS_ARCH" \
    || _fail "jre/bin/java arch mismatch — expected $_ARCH_PAT, got: $_FILE_OUT"

_FX_SO=$(find "$BUNDLE/jre/lib" -name "libjavafx*.so" 2>/dev/null | head -1)
[ -n "$_FX_SO" ] \
    && _ok "JavaFX native libs present in jre/lib/ (.so)" \
    || _fail "no JavaFX native libs (libjavafx*.so) found in jre/lib/"
find "$BUNDLE/jre/lib" -name "libjavafx*.dylib" 2>/dev/null | grep -q . \
    && _fail "JavaFX .dylib files found in jre/lib/ — wrong platform bundled"

SPEC_COUNT=$(find "$BUNDLE/examples/spec" -name "*.xml" 2>/dev/null | wc -l | tr -d ' ')
[ "$SPEC_COUNT" -gt 0 ] \
    && _ok "examples/spec/ has ${SPEC_COUNT} XML files" \
    || _fail "examples/spec/ is empty — no XML files copied"

if [ "$FAIL" -gt 0 ]; then
    echo "==> FAILED: ${FAIL} check(s) failed, ${PASS} passed — aborting."
    exit 1
fi
echo "==> All ${PASS} checks passed."

# ── Step 9: Create tgz ────────────────────────────────────────────────────────
echo ""
echo "==> Step 9: Creating tgz..."
(cd "$DEST" && tar -czf "$(basename "$TGZ")" "BEAST.v${VERSION}")
SIZE=$(du -sh "$TGZ" | cut -f1)

echo ""
echo "==> Done."
echo "    Bundle: $BUNDLE"
echo "    Archive: $TGZ ($SIZE)"
