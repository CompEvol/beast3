#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# test-linux-on-mac.sh — Build a BEAST 3 Linux release bundle on macOS.
#
# Usage:
#   cd release
#   bash test-linux-on-mac.sh [--skip-build]
#
# What it does:
#   1. Parses VERSION from version.xml
#   2. Runs Maven build (skippable with --skip-build)
#   3. Calls Linux/assemble-bundle.sh (assembles, verifies, creates tgz)
#
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$SCRIPT_DIR"

# ── Configurable: path to the extracted Zulu JRE+FX 25 for the target Linux arch.
# Download the jre+fx package from azul.com and extract it here (or anywhere),
# then update this path. The directory name matches the Azul download convention.
# Override at runtime: JRE_DIR=/other/path bash test-linux-on-mac.sh
ZULU_JRE_FX_DIR="${HOME}/WorkSpace/beast3/release/zulu25.34.17-ca-fx-jre25.0.3-macosx_aarch64"

# ── Mac-specific defaults ─────────────────────────────────────────────────────
JRE_DIR="${JRE_DIR:-${ZULU_JRE_FX_DIR}}"

SKIP_BUILD=false
for arg in "$@"; do
    case "$arg" in
        --skip-build) SKIP_BUILD=true ;;
        *) echo "ERROR: unknown argument: $arg" >&2; exit 1 ;;
    esac
done

# ── Step 1: Resolve version from version.xml ──────────────────────────────────
echo "=== Step 1: Resolve version ==="
VERSION=$(perl -e 'while($s=<>) {if ($s=~/version=/) {$s =~ /version="([^"]*)"/; print $1;exit(0);}}' \
    < "$REPO_ROOT/version.xml")
[ -n "$VERSION" ] || { echo "ERROR: could not parse version from version.xml" >&2; exit 1; }
echo "    VERSION=$VERSION"

# ── Step 2: Maven build ───────────────────────────────────────────────────────
if $SKIP_BUILD; then
    echo "=== Step 2: Maven build (skipped via --skip-build) ==="
else
    echo "=== Step 2: Maven build ==="
    mvn -f "$REPO_ROOT/pom.xml" clean package -DskipTests -q
    echo "    [ok] mvn clean package"
fi

# ── Step 3: Assemble Linux aarch64 bundle ─────────────────────────────────────
# assemble-bundle.sh handles assembly, verification, and tgz creation.
echo ""
echo "=== Step 3: Assemble Linux aarch64 ==="
VERSION="$VERSION"         \
OS_ARCH="Linux.aarch64"    \
JRE_DIR="$JRE_DIR"         \
REPO_ROOT="$REPO_ROOT"     \
DEST="$SCRIPT_DIR/dist"    \
bash Linux/assemble-bundle.sh
