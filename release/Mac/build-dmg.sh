#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# build-dmg.sh — Build a macOS DMG containing BEAST 3 .app bundles
#
# Uses jpackage (JDK 16+) to create native application images with a bundled
# JRE, then packages them into a single DMG installer.
#
# Applications included:
#   BEAST, BEAUti, TreeAnnotator, LogCombiner, AppLauncher
#
# Usage:
#   cd release/Mac && ./build-dmg.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# ── Parse version from version.xml ───────────────────────────────────────────
VERSION=$(perl -e 'while($s=<>) {if ($s=~/version=/) {$s =~ /version="([^"]*)"/; print $1;exit(0);}}' < "$REPO_ROOT/version.xml")
if [ -z "$VERSION" ]; then
    echo "ERROR: Could not parse version from version.xml"
    exit 1
fi
echo "==> BEAST version: ${VERSION}"

# ── Verify jpackage is available ─────────────────────────────────────────────
if ! command -v jpackage &>/dev/null; then
    echo "ERROR: jpackage not found. JDK 16+ is required."
    echo "       Install a recent JDK and ensure jpackage is on your PATH."
    exit 1
fi
echo "==> jpackage: $(jpackage --version)"

# ── Step 1: Maven build ─────────────────────────────────────────────────────
echo ""
echo "==> Step 1: Building project with Maven..."
mvn -f "$REPO_ROOT/pom.xml" clean package -DskipTests -q
echo "    Build complete."

# ── Detect Maven artifact version (may include -SNAPSHOT) ────────────────────
FX_JAR=$(find "$REPO_ROOT/beast-fx/target" -maxdepth 1 -name "beast-fx-*.jar" \
    ! -name "*-sources*" ! -name "*-javadoc*" ! -name "*-tests*" | head -1)
if [ -z "$FX_JAR" ]; then
    echo "ERROR: beast-fx JAR not found in beast-fx/target/"
    exit 1
fi
MVN_VERSION=$(basename "$FX_JAR" | sed 's/^beast-fx-//;s/\.jar$//')
echo "    Maven artifact version: ${MVN_VERSION}"

MAIN_JAR="beast-pkgmgmt-${MVN_VERSION}.jar"

# ── Step 2: Stage files ─────────────────────────────────────────────────────
echo ""
echo "==> Step 2: Staging files..."
STAGING="$SCRIPT_DIR/staging"
OUTPUT="$SCRIPT_DIR/output"
rm -rf "$STAGING" "$OUTPUT"
mkdir -p "$STAGING" "$OUTPUT"

# beast-fx JAR (FX UI classes)
cp "$FX_JAR" "$STAGING/"

# All runtime dependencies (beast-base, beast-pkgmgmt, JavaFX, etc.)
cp "$REPO_ROOT/beast-fx/target/lib"/*.jar "$STAGING/"

# version.xml (read at runtime for version info)
cp "$REPO_ROOT/version.xml" "$STAGING/"

# Verify main JAR is present
if [ ! -f "$STAGING/$MAIN_JAR" ]; then
    echo "ERROR: Main JAR not found in staging: $MAIN_JAR"
    exit 1
fi

JAR_COUNT=$(find "$STAGING" -name "*.jar" | wc -l | tr -d ' ')
echo "    Staged ${JAR_COUNT} JARs into staging/"

# ── Step 3: Create app images with jpackage ──────────────────────────────────
echo ""
echo "==> Step 3: Creating application bundles with jpackage..."

ICON_DIR="$SCRIPT_DIR/../common/icons"
JAVA_OPTS="-Xss256m -Xmx8g -Duser.language=en -Dfile.encoding=UTF-8"

build_app() {
    local app_name="$1"
    local main_class="$2"
    local icon_file="$3"

    echo "    Creating ${app_name}.app..."
    jpackage --type app-image \
        --name "$app_name" \
        --app-version "$VERSION" \
        --icon "$ICON_DIR/$icon_file" \
        --input "$STAGING" \
        --main-jar "$MAIN_JAR" \
        --main-class "$main_class" \
        --java-options "$JAVA_OPTS" \
        --dest "$OUTPUT"
}

build_app "BEAST"          "beast.pkgmgmt.launcher.BeastLauncher"          "beast.icns"
build_app "BEAUti"         "beast.pkgmgmt.launcher.BeautiLauncher"         "beauti.icns"
build_app "TreeAnnotator"  "beast.pkgmgmt.launcher.TreeAnnotatorLauncher"  "utility.icns"
build_app "LogCombiner"    "beast.pkgmgmt.launcher.LogCombinerLauncher"    "utility.icns"
build_app "AppLauncher"    "beast.pkgmgmt.launcher.AppLauncherLauncher"    "utility.icns"

APP_COUNT=$(find "$OUTPUT" -maxdepth 1 -name "*.app" -type d | wc -l | tr -d ' ')
echo "    Created ${APP_COUNT} application bundles."

# ── Step 4: Create DMG ──────────────────────────────────────────────────────
echo ""
echo "==> Step 4: Creating DMG..."

DMG_TITLE="BEAST v${VERSION}"
DMG_NAME="BEAST_with_JRE.v${VERSION}.dmg"
DMG_STAGING="$SCRIPT_DIR/dmg-staging"

rm -rf "$DMG_STAGING"
mkdir -p "$DMG_STAGING/.background"

# Copy all .app bundles
cp -r "$OUTPUT"/*.app "$DMG_STAGING/"

# Applications symlink for drag-to-install
ln -s /Applications "$DMG_STAGING/Applications"

# Background image
cp "$SCRIPT_DIR/install.png" "$DMG_STAGING/.background/install.png"

# Create writable DMG
hdiutil create -srcfolder "$DMG_STAGING" \
    -volname "$DMG_TITLE" \
    -fs HFS+ \
    -fsargs "-c c=64,a=16,e=16" \
    -format UDRW \
    -size 1g \
    "$SCRIPT_DIR/pack.temp.dmg"

DEVICE=$(hdiutil attach -readwrite -noverify -noautoopen "$SCRIPT_DIR/pack.temp.dmg" | \
    egrep '^/dev/' | sed 1q | awk '{print $1}')
echo "    Attached device: ${DEVICE}"

# Configure DMG window layout with AppleScript
osascript <<APPLESCRIPT
tell application "Finder"
    tell disk "${DMG_TITLE}"
        open
        set current view of container window to icon view
        set toolbar visible of container window to false
        set statusbar visible of container window to false
        set the bounds of container window to {100, 100, 900, 520}
        set theViewOptions to the icon view options of container window
        set arrangement of theViewOptions to not arranged
        set background picture of theViewOptions to file ".background:install.png"
        set icon size of theViewOptions to 72
        -- Position apps in a row
        set position of item "BEAST.app" of container window to {80, 200}
        set position of item "BEAUti.app" of container window to {220, 200}
        set position of item "TreeAnnotator.app" of container window to {360, 200}
        set position of item "LogCombiner.app" of container window to {500, 200}
        set position of item "AppLauncher.app" of container window to {640, 200}
        set position of item "Applications" of container window to {720, 80}
        close
        open
        update without registering applications
        delay 3
    end tell
end tell
APPLESCRIPT

chmod -Rf go-w "/Volumes/${DMG_TITLE}"
sync
sync
hdiutil detach "$DEVICE"
echo "    Detached device: ${DEVICE}"

# Convert to compressed DMG
rm -f "$SCRIPT_DIR/$DMG_NAME"
hdiutil convert "$SCRIPT_DIR/pack.temp.dmg" \
    -format UDZO \
    -imagekey zlib-level=9 \
    -o "$SCRIPT_DIR/$DMG_NAME"

rm -f "$SCRIPT_DIR/pack.temp.dmg"

# ── Cleanup staging ─────────────────────────────────────────────────────────
rm -rf "$STAGING" "$DMG_STAGING"

echo ""
echo "==> Done!"
echo "    DMG: $SCRIPT_DIR/$DMG_NAME"
echo "    App bundles: $OUTPUT/"
echo ""
ls -lh "$SCRIPT_DIR/$DMG_NAME"
