#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# build-dmg.sh — Build a macOS DMG containing BEAST 3 .app bundles
#
# Uses jpackage (JDK 16+) to create one full BEAST.app with a bundled JRE,
# then creates lightweight wrapper .app bundles for BEAUti, TreeAnnotator,
# LogCombiner, and AppLauncher that share BEAST.app's JRE and JARs.
#
# Also includes bin/ (command-line launchers) and examples/ (sample XML files).
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

# Remove empty JavaFX/jdk classifier JARs — these are Maven dependency-resolution
# artifacts with no code (automatic modules). Only the platform-specific JARs
# (e.g. javafx-graphics-25.0.2-mac-aarch64.jar) contain real module descriptors.
find "$STAGING" \( -name "javafx-*.jar" -o -name "jdk-*.jar" \) \
    ! -name "*-mac-*" ! -name "*-linux-*" ! -name "*-win-*" -delete

# version.xml (read at runtime for version info)
cp "$REPO_ROOT/version.xml" "$STAGING/"

# Verify main JAR is present
if [ ! -f "$STAGING/$MAIN_JAR" ]; then
    echo "ERROR: Main JAR not found in staging: $MAIN_JAR"
    exit 1
fi

JAR_COUNT=$(find "$STAGING" -name "*.jar" | wc -l | tr -d ' ')
echo "    Staged ${JAR_COUNT} JARs into staging/"

# ── Step 3: Create BEAST.app with jpackage ───────────────────────────────────
echo ""
echo "==> Step 3: Creating BEAST.app with jpackage..."

ICON_DIR="$SCRIPT_DIR/../common/icons"
JAVA_OPTS="-Xss256m -Xmx8g -Duser.language=en -Dfile.encoding=UTF-8"

FXTEMPLATES="$REPO_ROOT/beast-fx/src/main/resources/beast.fx/fxtemplates"

echo "    Creating BEAST.app..."
jpackage --type app-image \
    --name "BEAST" \
    --app-version "$VERSION" \
    --icon "$ICON_DIR/beast.icns" \
    --input "$STAGING" \
    --main-jar "$MAIN_JAR" \
    --main-class "beast.pkgmgmt.launcher.BeastLauncher" \
    --java-options "$JAVA_OPTS" \
    --arguments "-window" \
    --add-modules ALL-MODULE-PATH \
    --dest "$OUTPUT"

# Switch the native launcher from classpath to module-path mode so that
# module descriptors (provides/requires) are visible at runtime.
# jpackage's --main-jar/--main-class puts JARs on -cp, but external BEAST
# packages loaded as ModuleLayers need core modules to be on the module path.
BEAST_CFG="$OUTPUT/BEAST.app/Contents/app/BEAST.cfg"
sed -i '' '/^app\.classpath/d' "$BEAST_CFG"
sed -i '' 's|^app\.mainclass=.*|app.mainmodule=beast.pkgmgmt/beast.pkgmgmt.launcher.BeastLauncher|' "$BEAST_CFG"
# The native launcher resolves $APPDIR to the app/ directory at runtime.
# Use = syntax because each java-options line is passed as a single JVM argument.
sed -i '' '/^\[JavaOptions\]/a\
java-options=--module-path=\$APPDIR\
java-options=--add-modules=ALL-MODULE-PATH
' "$BEAST_CFG"

# BEAUti (and others) look for fxtemplates at Contents/app/../fxtemplates/
if [ -d "$FXTEMPLATES" ]; then
    cp -r "$FXTEMPLATES" "$OUTPUT/BEAST.app/Contents/fxtemplates"
fi

# version.xml must also be at Contents/version.xml (not just Contents/app/version.xml)
# because BEASTClassLoader.initServices scans the grandparent of each JAR.
cp "$REPO_ROOT/version.xml" "$OUTPUT/BEAST.app/Contents/version.xml"

# jpackage strips bin/java from the bundled runtime — restore it so that
# wrapper .app bundles and bin/ scripts can invoke java from the runtime.
RUNTIME_HOME="$OUTPUT/BEAST.app/Contents/runtime/Contents/Home"
BUILD_JAVA_HOME="$(/usr/libexec/java_home)"
mkdir -p "$RUNTIME_HOME/bin"
cp "$BUILD_JAVA_HOME/bin/java" "$RUNTIME_HOME/bin/java"
chmod 755 "$RUNTIME_HOME/bin/java"
echo "    Copied java binary into runtime."

echo "    BEAST.app created."

# ── Step 3b: Create lightweight wrapper .app bundles ─────────────────────────
echo ""
echo "==> Step 3b: Creating wrapper .app bundles..."

build_wrapper_app() {
    local app_name="$1"
    local main_class="$2"
    local icon_file="$3"
    local extra_args="${4:-}"

    local app_dir="$OUTPUT/$app_name.app"
    local contents="$app_dir/Contents"

    echo "    Creating ${app_name}.app (wrapper)..."

    mkdir -p "$contents/MacOS" "$contents/Resources"

    # ── Launcher shell script ──
    # Derive the module/class from the fully qualified main class
    # e.g. beast.pkgmgmt.launcher.BeautiLauncher → -m beast.pkgmgmt/beast.pkgmgmt.launcher.BeautiLauncher
    local module_name
    module_name=$(echo "$main_class" | sed 's/\.launcher\..*//')
    local launch_cmd
    if [ -n "$extra_args" ]; then
        launch_cmd="exec \"\$BEAST_APP/runtime/Contents/Home/bin/java\" \\
    --module-path \"\$BEAST_APP/app\" --add-modules ALL-MODULE-PATH \\
    -Xss256m -Xmx8g -Duser.language=en -Dfile.encoding=UTF-8 \\
    -m $module_name/$main_class $extra_args \"\$@\""
    else
        launch_cmd="exec \"\$BEAST_APP/runtime/Contents/Home/bin/java\" \\
    --module-path \"\$BEAST_APP/app\" --add-modules ALL-MODULE-PATH \\
    -Xss256m -Xmx8g -Duser.language=en -Dfile.encoding=UTF-8 \\
    -m $module_name/$main_class \"\$@\""
    fi

    cat > "$contents/MacOS/$app_name" <<LAUNCHER
#!/bin/sh
CONTENTS_DIR="\$(cd "\$(dirname "\$0")/.." && pwd)"
BEAST_APP="\$(cd "\$CONTENTS_DIR/../../BEAST.app/Contents" && pwd)"
$launch_cmd
LAUNCHER
    chmod 755 "$contents/MacOS/$app_name"

    # ── Info.plist ──
    local icon_basename
    icon_basename=$(basename "$icon_file" .icns)
    cat > "$contents/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>$app_name</string>
    <key>CFBundleDisplayName</key>
    <string>$app_name</string>
    <key>CFBundleIdentifier</key>
    <string>org.beast3.$app_name</string>
    <key>CFBundleVersion</key>
    <string>$VERSION</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundleExecutable</key>
    <string>$app_name</string>
    <key>CFBundleIconFile</key>
    <string>$icon_basename</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
PLIST

    # ── PkgInfo ──
    printf 'APPL????' > "$contents/PkgInfo"

    # ── Icon ──
    cp "$ICON_DIR/$icon_file" "$contents/Resources/"
}

build_wrapper_app "BEAUti"         "beast.pkgmgmt.launcher.BeautiLauncher"         "beauti.icns"   "-capture"
build_wrapper_app "TreeAnnotator"  "beast.pkgmgmt.launcher.TreeAnnotatorLauncher"  "utility.icns"
build_wrapper_app "LogCombiner"    "beast.pkgmgmt.launcher.LogCombinerLauncher"    "utility.icns"
build_wrapper_app "AppLauncher"    "beast.pkgmgmt.launcher.AppLauncherLauncher"    "utility.icns"

APP_COUNT=$(find "$OUTPUT" -maxdepth 1 -name "*.app" -type d | wc -l | tr -d ' ')
echo "    Created ${APP_COUNT} application bundles (1 full + $(( APP_COUNT - 1 )) wrappers)."

# ── Step 4: Create DMG ──────────────────────────────────────────────────────
echo ""
echo "==> Step 4: Creating DMG..."

DMG_TITLE="BEAST v${VERSION}"
DMG_NAME="BEAST_with_JRE.v${VERSION}.dmg"
DMG_STAGING="$SCRIPT_DIR/dmg-staging"
APP_FOLDER="BEAST ${VERSION}"

rm -rf "$DMG_STAGING"
mkdir -p "$DMG_STAGING/.background" "$DMG_STAGING/$APP_FOLDER"

# Copy all .app bundles into the versioned folder
cp -r "$OUTPUT"/*.app "$DMG_STAGING/$APP_FOLDER/"

# ── bin/ — command-line launcher scripts (from release/Linux/jrebin) ──────────
JREBIN_DIR="$REPO_ROOT/release/Linux/jrebin"
if [ -d "$JREBIN_DIR" ]; then
    echo "    Copying bin/ scripts..."
    cp -r "$JREBIN_DIR" "$DMG_STAGING/$APP_FOLDER/bin"
    chmod 755 "$DMG_STAGING/$APP_FOLDER/bin"/*
else
    echo "    WARNING: $JREBIN_DIR not found — skipping bin/"
fi

# ── examples/ — example BEAST XML files ──────────────────────────────────────
EXAMPLES_DIR="$REPO_ROOT/beast-base/src/test/resources/examples"
if [ -d "$EXAMPLES_DIR" ]; then
    echo "    Copying examples/..."
    mkdir -p "$DMG_STAGING/$APP_FOLDER/examples"
    # Copy top-level XML files
    find "$EXAMPLES_DIR" -maxdepth 1 -name "*.xml" -exec cp {} "$DMG_STAGING/$APP_FOLDER/examples/" \;
    # Copy nexus subdirectory
    if [ -d "$EXAMPLES_DIR/nexus" ]; then
        cp -r "$EXAMPLES_DIR/nexus" "$DMG_STAGING/$APP_FOLDER/examples/nexus"
    fi
else
    echo "    WARNING: $EXAMPLES_DIR not found — skipping examples/"
fi

# Applications symlink for drag-to-install (drag the folder here)
ln -s /Applications "$DMG_STAGING/Applications"

# Background image
cp "$SCRIPT_DIR/install.png" "$DMG_STAGING/.background/install.png"

# Create writable DMG
hdiutil create -srcfolder "$DMG_STAGING" \
    -volname "$DMG_TITLE" \
    -fs HFS+ \
    -fsargs "-c c=64,a=16,e=16" \
    -format UDRW \
    -size 2g \
    "$SCRIPT_DIR/pack.temp.dmg"

DEVICE=$(hdiutil attach -readwrite -noverify -noautoopen "$SCRIPT_DIR/pack.temp.dmg" | \
    egrep '^/dev/' | sed 1q | awk '{print $1}')
echo "    Attached device: ${DEVICE}"

# Configure DMG window layout with AppleScript
osascript <<APPLESCRIPT || echo "    Warning: AppleScript layout failed (cosmetic only)"
tell application "Finder"
    tell disk "${DMG_TITLE}"
        open
        delay 2
        set current view of container window to icon view
        set toolbar visible of container window to false
        set statusbar visible of container window to false
        set the bounds of container window to {100, 100, 700, 400}
        set theViewOptions to the icon view options of container window
        set arrangement of theViewOptions to not arranged
        set background picture of theViewOptions to file ".background:install.png"
        set icon size of theViewOptions to 72
        -- Position folder and Applications alias
        set position of item "${APP_FOLDER}" of container window to {150, 150}
        set position of item "Applications" of container window to {400, 150}
        close
        open
        update without registering applications
        delay 3
        close
    end tell
end tell
APPLESCRIPT

sleep 2
chmod -Rf go-w "/Volumes/${DMG_TITLE}"
sync
sync
hdiutil detach "$DEVICE" || hdiutil detach -force "$DEVICE"
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
