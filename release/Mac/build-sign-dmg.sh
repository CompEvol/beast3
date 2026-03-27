#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# build-sign-dmg.sh — Build and sign a macOS DMG containing BEAST 3 .app bundles
#
# Uses jpackage (JDK 25+) to create one full BEAST.app with a bundled JRE,
# then creates lightweight wrapper .app bundles for BEAUti, TreeAnnotator,
# LogCombiner, AppLauncher, and DensiTree that share BEAST.app's JRE and JARs.
# All .app bundles are code-signed with a Developer ID certificate: BEAST.app
# first (it owns the shared JRE and JARs), then the wrapper apps.
# jpackage is invoked WITHOUT a signing flag — all signing is done manually in
# Step 3b using an inside-out strategy: JRE binaries → native launcher → outer
# bundle seal, ensuring no inner file is signed after the bundle that contains it.
#
# JAR contents: fxtemplates are bundled inside beast-fx-*.jar as beast.fx/fxtemplates/
# and version.xml is bundled inside beast-base-*.jar — both are accessible at
# runtime via the module path and require no separate copy into staging/.
# version.xml must NOT be copied to Contents/ root — Apple's codesign resource
# rules reject plain files directly in Contents/.
#
# Also includes bin/ (command-line launchers) and examples/ (sample XML files).
#
# Usage:
#   cd release/Mac && ./build-sign-dmg.sh
#
# ─────────────────────────────────────────────────────────────────────────────
# HOW TO SAFELY MOVE AND DMG YOUR APP
# ─────────────────────────────────────────────────────────────────────────────
# To ensure the code-signature seal stays 100% intact from the moment you sign
# the app until it reaches the user, follow this workflow exactly:
#
#   1. Stage first:   Copy all files (app bundles, bin/, examples/) into the
#                     DMG_STAGING folder before signing anything. jpackage
#                     builds directly into $OUTPUT (= $DMG_STAGING/$APP_FOLDER)
#                     so no post-build copy of BEAST.app is needed.
#
#   2. Sign in place: Run the signing steps on the .app bundles while they are
#                     already inside DMG_STAGING. Never move or copy a signed
#                     app — every file system operation after signing is a
#                     potential seal-breaker.
#
#   3. DMG immediately: Run hdiutil immediately after signing. The less time
#                       the signed apps sit on disk, the less opportunity there
#                       is for macOS background services (Spotlight, Time
#                       Machine, Finder) to touch them.
#
#   4. Avoid Finder:  Do NOT open or browse DMG_STAGING in Finder between
#                     signing and creating the DMG. Finder writes hidden
#                     .DS_Store files on contact, which breaks the bundle seal.
#
# ─────────────────────────────────────────────────────────────────────────────
# THREE CODESIGN TRAPS TO AVOID
# ─────────────────────────────────────────────────────────────────────────────
#
# TRAP 1 — The .DS_Store Trap
#   If you  cp -r  the app into a new folder and then open that folder in
#   Finder, macOS instantly writes a hidden .DS_Store file inside BEAST.app
#   to remember how icons were arranged.
#   The Seal: Broken — codesign sees a file that was not present at signing.
#   The Fix:  Never open DMG_STAGING (or any signed .app parent folder) in
#             Finder after signing. Create the DMG immediately and from the
#             command line only.
#
# TRAP 2 — The Symlink Trap (JRE-specific)
#   BEAST.app bundles a JRE. JRE distributions use symbolic links extensively.
#   A plain  cp -r  can "flatten" symlinks — replacing a 1 KB alias with the
#   full 50 MB real file it points to.
#   The Seal: Broken — the file structure is now different from what the
#             signature expects.
#   The Fix:  Use  cp -a  or  cp -Rp  so that symlinks are preserved as
#             symlinks rather than dereferenced and copied as regular files.
#
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# ── Code-signing configuration ────────────────────────────────────────────────
CODESIGN_IDENTITY="Developer ID Application: Walter Xie (27V5YMX65C)"
ENTITLEMENTS="$SCRIPT_DIR/entitlements.plist"

if [ ! -f "$ENTITLEMENTS" ]; then
    echo "Generating entitlements.plist..."
    cat > "$ENTITLEMENTS" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
    "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.cs.allow-jit</key><true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key><true/>
    <key>com.apple.security.cs.allow-dyld-environment-variables</key><true/>
</dict>
</plist>
PLIST
fi

# ── Parse version from version.xml ───────────────────────────────────────────
VERSION=$(perl -e 'while($s=<>) {if ($s=~/version=/) {$s =~ /version="([^"]*)"/; print $1;exit(0);}}' < "$REPO_ROOT/version.xml")
if [ -z "$VERSION" ]; then
    echo "ERROR: Could not parse version from version.xml"
    exit 1
fi
echo "==> BEAST version: ${VERSION}"

# ── Verify jpackage is available ─────────────────────────────────────────────
if ! command -v jpackage &>/dev/null; then
    echo "ERROR: jpackage not found. JDK 25+ is required."
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
# Key layout decision: OUTPUT is set to $DMG_STAGING/$APP_FOLDER so that jpackage
# builds BEAST.app directly into its final DMG location. All signing is then done
# in place — no post-build copy of a signed .app is ever needed, eliminating the
# main source of seal corruption (see TRAP 1 and TRAP 2 in the header).
APP_FOLDER="BEAST ${VERSION}"
DMG_STAGING="$SCRIPT_DIR/dmg-staging"
OUTPUT="$DMG_STAGING/$APP_FOLDER"

rm -rf "$STAGING" "$DMG_STAGING" "$OUTPUT"
mkdir -p "$STAGING" "$DMG_STAGING" "$OUTPUT"

# beast-fx JAR (FX UI classes)
cp "$FX_JAR" "$STAGING/"

# All runtime dependencies (beast-base, beast-pkgmgmt, JavaFX, etc.)
cp "$REPO_ROOT/beast-fx/target/lib"/*.jar "$STAGING/"

# Remove empty JavaFX/jdk classifier JARs — these are Maven dependency-resolution
# artifacts with no code (automatic modules). Only the platform-specific JARs
# (e.g. javafx-graphics-25.0.2-mac-aarch64.jar) contain real module descriptors.
find "$STAGING" \( -name "javafx-*.jar" -o -name "jdk-*.jar" \) \
    ! -name "*-mac-*" ! -name "*-linux-*" ! -name "*-win-*" -delete

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
    
# jpackage --main-jar/--main-class writes app.classpath + app.mainclass to BEAST.cfg,
# which puts all JARs on the classpath. Patch to module-path mode so that module
# descriptors (provides/requires) are visible at runtime and external BEAST packages
# loaded via ModuleLayers resolve correctly.
# Note: jpackage's --module/--module-path flags cannot be used here because automatic
# modules (e.g. antlr4-runtime.jar) are incompatible with jlink's ALL-MODULE-PATH
# resolution — so we use --main-jar then patch the cfg as a workaround.
# This patch must happen after jpackage creates the app but before codesign seals it.
BEAST_CFG="$OUTPUT/BEAST.app/Contents/app/BEAST.cfg"
sed -i '' '/^app\.classpath/d' "$BEAST_CFG"
sed -i '' 's|^app\.mainclass=.*|app.mainmodule=beast.pkgmgmt/beast.pkgmgmt.launcher.BeastLauncher|' "$BEAST_CFG"
# The native launcher resolves $APPDIR to the app/ directory at runtime.
# Use = syntax because each java-options line is passed as a single JVM argument.
sed -i '' '/^\[JavaOptions\]/a\
java-options=--module-path=\$APPDIR\
java-options=--add-modules=ALL-MODULE-PATH
' "$BEAST_CFG"

# jpackage deliberately strips bin/java from the bundled runtime (it uses its own
# native launcher instead). Restore it so that wrapper .app bundles and bin/ scripts
# can locate and invoke java directly from the bundled JRE rather than relying on
# whatever java happens to be on the user's PATH.
RUNTIME_HOME="$OUTPUT/BEAST.app/Contents/runtime/Contents/Home"
BUILD_JAVA_HOME="$(/usr/libexec/java_home)"
mkdir -p "$RUNTIME_HOME/bin"
cp "$BUILD_JAVA_HOME/bin/java" "$RUNTIME_HOME/bin/java"
chmod 755 "$RUNTIME_HOME/bin/java"
echo "    Copied java binary into runtime."

echo "    BEAST.app created."



# ── Step 3a: Create lightweight wrapper .app bundles ─────────────────────────
echo ""
echo "==> Step 3a: Creating wrapper .app bundles..."

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

# ── DensiTree — standalone JAR app (not a BEAST module) ──────────────────────
DENSITREE_JAR="$REPO_ROOT/release/common/tools/DensiTree.jar"
DENSITREE_ICNS="$REPO_ROOT/release/common/tools/DensiTree.icns"
if [ -f "$DENSITREE_JAR" ]; then
    echo "    Creating DensiTree.app (standalone JAR wrapper)..."
    DT_APP="$OUTPUT/DensiTree.app"
    DT_CONTENTS="$DT_APP/Contents"
    mkdir -p "$DT_CONTENTS/MacOS" "$DT_CONTENTS/Resources" "$DT_CONTENTS/Java"

    cp "$DENSITREE_JAR" "$DT_CONTENTS/Java/"

    cat > "$DT_CONTENTS/MacOS/DensiTree" <<'LAUNCHER'
#!/bin/sh
CONTENTS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BEAST_APP="$(cd "$CONTENTS_DIR/../../BEAST.app/Contents" && pwd)"
exec "$BEAST_APP/runtime/Contents/Home/bin/java" \
    -Xmx4g -Duser.language=en -Dfile.encoding=UTF-8 \
    -jar "$CONTENTS_DIR/Java/DensiTree.jar" "$@"
LAUNCHER
    chmod 755 "$DT_CONTENTS/MacOS/DensiTree"

    cat > "$DT_CONTENTS/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>DensiTree</string>
    <key>CFBundleDisplayName</key>
    <string>DensiTree</string>
    <key>CFBundleIdentifier</key>
    <string>org.beast3.DensiTree</string>
    <key>CFBundleVersion</key>
    <string>$VERSION</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundleExecutable</key>
    <string>DensiTree</string>
    <key>CFBundleIconFile</key>
    <string>DensiTree</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
PLIST

    printf 'APPL????' > "$DT_CONTENTS/PkgInfo"
    cp "$DENSITREE_ICNS" "$DT_CONTENTS/Resources/"
else
    echo "    WARNING: DensiTree.jar not found at $DENSITREE_JAR — skipping DensiTree.app"
fi

APP_COUNT=$(find "$OUTPUT" -maxdepth 1 -name "*.app" -type d | wc -l | tr -d ' ')
echo "    Created ${APP_COUNT} application bundles (1 full + $(( APP_COUNT - 1 )) wrappers)."



# Patch the bundled JRE's CFBundleIdentifier to be unique. Apple's codesign requires
# every nested bundle to have a distinct identifier; the JRE shipped by jpackage uses
# a generic identifier that can collide with the outer BEAST.app bundle, causing
# "bundle format unrecognized" errors during deep signing. Must be done before
# Step 3b so the runtime Info.plist is stable when codesign seals the runtime bundle.
plutil -replace CFBundleIdentifier -string "beast.pkgmgmt.launcher.runtime" \
   "$OUTPUT/BEAST.app/Contents/runtime/Contents/Info.plist"


# ── Step 3b: Code-sign all .app bundles ──────────────────────────────────────
# BEAST.app must be signed first because the wrapper apps reference its runtime.
echo ""
echo "==> Step 3b: Code-signing all .app bundles..."

app="$OUTPUT/BEAST.app"
# Step 1 — Sign all JRE binaries (deepest first).
# Targets every .dylib, .so, and executable (+x) inside the bundled runtime,
# including the shared bin/java binary that wrapper apps and bin/ scripts invoke.
# Must be done before sealing the runtime bundle or the outer BEAST.app bundle.
#
# IMPORTANT: --entitlements must be passed here. bin/java is copied from the
# build JDK and retains a stale code signature tied to the source JDK bundle.
# Without re-signing it with entitlements.plist under Hardened Runtime, running
# bin/java directly produces "Trace/BPT trap: 5" (SIGTRAP — kernel kills the
# process because the signature is invalid in its new bundle context).
echo "Signing JRE binaries ..."
find "$app/Contents/runtime" -type f \( -name "*.dylib" -or -name "*.so" -or -perm +111 \) \
    -exec codesign --force --options runtime --timestamp \
           --entitlements "$ENTITLEMENTS" \
           --sign "$CODESIGN_IDENTITY" {} +
         
# Step 2 — Sign the BEAST native launcher binary.
# Only the BEAST binary in MacOS/ is signed explicitly; other files in MacOS/
# (if any) are sealed by the outer bundle signature in step 3. Signing the
# whole MacOS/ directory was tried but caused duplicate-signing warnings.
echo "Signing MacOS binaries ..."
codesign --force --options runtime --timestamp \
           --entitlements "$ENTITLEMENTS" \
           --sign "$CODESIGN_IDENTITY" "$app/Contents/MacOS/BEAST"

# Step 3 — Seal the outer BEAST.app bundle.
# This computes the resource seal over all Contents/ files and embeds our
# Developer ID signature. The JVM entitlements (allow-jit, allow-unsigned-
# executable-memory, allow-dyld-environment-variables) are required for the
# HotSpot JIT compiler and dynamic class loading to work under Hardened Runtime.
codesign --force --sign "$CODESIGN_IDENTITY" \
         --options runtime --timestamp \
         --entitlements "$ENTITLEMENTS" "$app"

# Step 4 — Verify the complete bundle seal.
# --deep checks all nested bundles (runtime, helpers). The three spot-checks
# below target the known trouble-makers: the native launcher (most likely to
# have a stale or malformed Mach-O signature), libjli.dylib (the JVM launch
# library inside the nested runtime bundle), and bin/java (the shared binary
# used by all wrapper apps — invalid signature here causes "Trace/BPT trap: 5").
echo "Verifying ..."
codesign --verify --deep --verbose=2 "$app"
codesign -vvv --strict "$app/Contents/MacOS/BEAST"
codesign -vvv --strict "$app/Contents/runtime/Contents/MacOS/libjli.dylib"
codesign -vvv --strict "$app/Contents/runtime/Contents/Home/bin/java"

echo "    BEAST.app signed."

# ── Step 3b (cont.): Sign wrapper .app bundles ───────────────────────────────
# Wrapper apps are shell-script bundles with no nested runtime or dylibs.
# Sign inner files first, then seal the outer bundle.
echo ""
echo "==> Step 3b (cont.): Code-signing wrapper .app bundles..."

sign_wrapper_app() {
    local app="$1"
    local app_name
    app_name=$(basename "$app")
    echo "    Signing ${app_name}..."

    # 1. Attempt to sign individual non-Mach-O files (icons, plists, JARs).
    #    codesign silently ignores non-Mach-O files in resource-seal terms —
    #    the outer bundle seal (step 2) already covers all Contents/ files.
    #    This loop is kept for reference in case a wrapper app ever gains a
    #    real binary resource that needs an individual signature first.
    find "$app/Contents" -type f \
       ! -path "$app/Contents/MacOS/*" | \
       sort | while read -r file; do
           codesign --force --sign "$CODESIGN_IDENTITY" \
                    --options runtime --timestamp "$file" 2>/dev/null || true
       done
    
    # 2. Seal the outer bundle — covers the MacOS/ shell-script launcher and
    #    all Contents/ files in a single code signature.
    #    Note: wrapper apps are pure shell-script bundles with no JVM or JIT;
    #    they do not technically require the JVM entitlements. However, the same
    #    entitlements file used for BEAST.app is passed here for consistency.
    #    Notarization accepts this because the entitlements are permissive, not
    #    restricted — Apple only rejects *missing* required entitlements, not
    #    extra ones on non-JVM bundles.
    codesign --force --sign "$CODESIGN_IDENTITY" \
            --options runtime --timestamp \
            --entitlements "$ENTITLEMENTS" "$app"

    # 3. Verify the wrapper bundle seal.
    codesign --verify --deep --verbose=2 "$app"
    app_base_name="${app_name%.app}"
    codesign -vvv --strict "$app/Contents/MacOS/${app_base_name}"
    echo "    ${app_name} signed."
}

for app in "$OUTPUT"/BEAUti.app \
           "$OUTPUT"/TreeAnnotator.app \
           "$OUTPUT"/LogCombiner.app \
           "$OUTPUT"/AppLauncher.app \
           "$OUTPUT"/DensiTree.app; do
    [ -d "$app" ] && sign_wrapper_app "$app"
done

echo "    All wrapper .app bundles signed."


# ── Step 4: Create DMG ──────────────────────────────────────────────────────
echo ""
echo "==> Step 4: Creating DMG..."

DMG_TITLE="BEAST v${VERSION}"
DMG_NAME="BEAST_with_JRE.v${VERSION}.dmg"

# ── bin/ — command-line launcher scripts (from release/Linux/jrebin) ──────────
# These shell scripts live alongside the .app bundles in the versioned folder,
# not inside any .app, so they are not covered by any bundle seal. They are
# intentionally added after signing so they cannot inadvertently affect the
# resource seal of any already-signed bundle.
JREBIN_DIR="$REPO_ROOT/release/Linux/jrebin"
if [ -d "$JREBIN_DIR" ]; then
    echo "    Copying bin/ scripts..."
    cp -r "$JREBIN_DIR" "$OUTPUT/bin"
    chmod 755 "$OUTPUT/bin"/*
else
    echo "    WARNING: $JREBIN_DIR not found — skipping bin/"
fi

# ── examples/ — example BEAST XML files ──────────────────────────────────────
EXAMPLES_DIR="$REPO_ROOT/beast-base/src/test/resources/examples"
if [ -d "$EXAMPLES_DIR" ]; then
    echo "    Copying examples/..."
    mkdir -p "$OUTPUT/examples"
    # Copy top-level XML files
    find "$EXAMPLES_DIR" -maxdepth 1 -name "*.xml" -exec cp {} "$OUTPUT/examples/" \;
    # Copy nexus subdirectory
    if [ -d "$EXAMPLES_DIR/nexus" ]; then
        cp -r "$EXAMPLES_DIR/nexus" "$OUTPUT/examples/nexus"
    fi
else
    echo "    WARNING: $EXAMPLES_DIR not found — skipping examples/"
fi

# ── README and LICENSE ─────────────────────────────────────────────────────
COMMON_DIR="$REPO_ROOT/release/common"
if [ -f "$COMMON_DIR/README.txt" ]; then
    cp "$COMMON_DIR/README.txt" "$OUTPUT/"
fi
if [ -f "$COMMON_DIR/LICENSE.txt" ]; then
    cp "$COMMON_DIR/LICENSE.txt" "$OUTPUT/"
fi


# $OUTPUT (= $DMG_STAGING/$APP_FOLDER) already exists from Step 2, but
# .background/ does not — create it now to hold the installer background image.
mkdir -p "$DMG_STAGING/$APP_FOLDER" "$DMG_STAGING/.background"

# Symlink lets users drag the versioned folder directly to /Applications.
ln -s /Applications "$DMG_STAGING/Applications"

# Background image shown in the DMG Finder window (positioned via AppleScript below).
cp "$SCRIPT_DIR/install.png" "$DMG_STAGING/.background/install.png"

# Create writable DMG (UDRW) — required for the AppleScript Finder layout step.
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

# Give Finder time to finish writing icon positions and .DS_Store before
# locking down the volume permissions.
sleep 2
chmod -Rf go-w "/Volumes/${DMG_TITLE}"

# Flush all pending filesystem writes to the mounted volume before detaching,
# preventing data loss or corruption in the converted DMG.
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
# staging/ (JAR inputs to jpackage) is always deleted — it is fully reproducible
# from a Maven build and there is no benefit to keeping it.
rm -rf "$STAGING"
# dmg-staging/ (the signed .app bundles + DMG layout) is intentionally kept for
# post-build inspection and debugging. Verify the DMG first, then delete manually.
#rm -rf "$DMG_STAGING"

echo ""
echo "==> Done!"
echo "    DMG: $SCRIPT_DIR/$DMG_NAME"
echo "    App bundles: $OUTPUT/"
echo ""
ls -lh "$SCRIPT_DIR/$DMG_NAME"


# Sign the DMG file itself. This is distinct from signing the .app bundles inside
# it: the DMG signature covers the disk image file as a whole, allowing Gatekeeper
# and the notarization service to verify the container was not tampered with in
# transit. The apps inside carry their own independent bundle signatures.
codesign --force --sign "$CODESIGN_IDENTITY" \
         --options runtime --timestamp "$SCRIPT_DIR/$DMG_NAME"

# Verify the DMG-level signature. Note: --deep here checks the DMG container
# signature only; the .app bundle seals inside were already verified in Step 3b.
codesign --verify --verbose=4 --deep --strict "$SCRIPT_DIR/$DMG_NAME"

echo "$SCRIPT_DIR/$DMG_NAME: signed."
echo ""
