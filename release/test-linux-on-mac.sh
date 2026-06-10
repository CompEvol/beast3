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
# Mac-specific notes:
#   JDK_DIR: Zulu (and most OpenJDK vendors) dropped the standalone JRE package
#   format starting with Java 25. The default points to the local Zulu 25 JDK.
#   This is a macOS JDK — it produces a structurally correct bundle but
#   jdk/bin/java inside will be a macOS binary. For a real distributable Linux
#   bundle set JDK_DIR to a Linux aarch64 JDK 25+ (e.g. from azul.com).
#
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$SCRIPT_DIR"

# ── Mac-specific defaults ─────────────────────────────────────────────────────
JDK_DIR="${JDK_DIR:-/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home}"

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
VERSION="$VERSION"      \
OS_ARCH="Linux.aarch64" \
JDK_DIR="$JDK_DIR"      \
REPO_ROOT="$REPO_ROOT"  \
bash Linux/assemble-bundle.sh
