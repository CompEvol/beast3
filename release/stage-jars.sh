#!/usr/bin/env bash
# stage-jars.sh — Collect Maven build output into staging/ for platform bundling.
#
# Run from the repository root after `mvn clean package -DskipTests`:
#
#   bash release/stage-jars.sh [staging-dir]
#
# staging-dir defaults to "staging" (relative to cwd).
#
# Outputs:
#   <staging-dir>/*.jar   — beast-fx + all runtime dependencies, with cross-platform
#                           empty JavaFX/JDK stub JARs removed.  Platform classifier
#                           JARs (e.g. javafx-controls-25.0.2-linux-aarch64.jar) are
#                           kept; the platform bundle jobs filter down to one arch each.
#   <staging-dir>/.main-jar — one-line file containing the launcher JAR filename
#                             (beast-pkgmgmt-<mvn-version>.jar) so downstream jobs
#                             don't have to re-detect it.
set -euo pipefail

STAGING="${1:-staging}"
mkdir -p "$STAGING"

# ── Copy JARs from Maven build ────────────────────────────────────────────────
cp beast-fx/target/beast-fx-*.jar "$STAGING/"
cp beast-fx/target/lib/*.jar       "$STAGING/"

# ── Remove empty JavaFX/JDK root JARs ────────────────────────────────────────
# Maven downloads JavaFX JARs for all platforms because the javafx-* POMs list
# every platform classifier as a dependency.  The un-suffixed root JARs (e.g.
# javafx-controls-25.0.2.jar) are empty Maven resolution stubs — only the
# classifier JARs (e.g. -linux-aarch64.jar, -mac-aarch64.jar, -win.jar) contain
# real module code.  Remove the stubs; each platform bundle job then prunes the
# classifier JARs it doesn't need.
find "$STAGING" \( -name "javafx-*.jar" -o -name "jdk-*.jar" \) \
    ! -name "*-mac-*" ! -name "*-linux*" ! -name "*-win-*" -delete

# ── Write launcher JAR name ───────────────────────────────────────────────────
FX_JAR=$(find beast-fx/target -maxdepth 1 -name "beast-fx-*.jar" \
    ! -name "*-sources*" ! -name "*-javadoc*" ! -name "*-tests*" | head -1)
if [ -z "$FX_JAR" ]; then
    echo "ERROR: beast-fx JAR not found in beast-fx/target/ — run mvn package first." >&2
    exit 1
fi
MVN_VER=$(basename "$FX_JAR" | sed 's/^beast-fx-//;s/\.jar$//')
echo "beast-pkgmgmt-${MVN_VER}.jar" > "$STAGING/.main-jar"

JAR_COUNT=$(find "$STAGING" -name "*.jar" | wc -l | tr -d ' ')
echo "Staged ${JAR_COUNT} JARs into ${STAGING}/  (main jar: beast-pkgmgmt-${MVN_VER}.jar)"
