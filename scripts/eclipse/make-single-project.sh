#!/usr/bin/env bash
#
# Generate one plain Eclipse Java project that holds the BEAST 3 core and the
# packages bundled with the release, all on the class path (issue #239).
#
# Maven is used once, here, to fetch the third-party JARs. After that Eclipse
# builds and runs everything itself: no m2e, no module-info.java.
#
# Usage: scripts/eclipse/make-single-project.sh [options]
#
#   --project-dir DIR   where to create the Eclipse project
#                       (default: beast-eclipse next to the beast3 checkout)
#   --package DIR       add one more package checkout (repeatable)
#   --no-bundled        leave out the bundled packages
#   --skip-install      don't run 'mvn install' in beast3 first
#   --check             after generating, compile everything with the Eclipse
#                       compiler (ECJ) and run each package's example in BEAST
#   -h, --help          show this help
#
# Re-run the script after pulling changes that add a dependency or a package;
# your own edits live in the checkouts, so nothing in the project is lost.

set -euo pipefail

BEAST3_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PARENT_DIR="$(dirname "$BEAST3_DIR")"
PROJECT_DIR="$PARENT_DIR/beast-eclipse"
PROJECT_NAME="BEAST"
EXTRA_PACKAGES=()
USE_BUNDLED=1
DO_INSTALL=1
DO_CHECK=0

# The packages bundled with the release (#248). Keep in step with
# release/bundled-packages.tsv; the script warns when the two differ.
#   name in version.xml | checkout folder | repository | example XML (or -)
BUNDLED="
ORC|ORC|https://github.com/jordandouglas/ORC.git|examples/caterpillars.xml
SA|sampled-ancestors|https://github.com/CompEvol/sampled-ancestors.git|examples/bears.xml
bModelTest|bModelTest|https://github.com/BEAST2-Dev/bModelTest.git|examples/bModelTest.xml
CCD|CCD|https://github.com/CompEvol/CCD.git|-
starbeast3|starbeast3|https://github.com/rbouckaert/starbeast3.git|src/main/resources/starbeast3/examples/gophers.xml
"

# The JavaFX modules BEAUti and the other apps need.
FX_MODULES="javafx.controls,javafx.fxml,javafx.web,javafx.swing,javafx.media"

usage() { sed -n '3,22p' "$0" | sed 's/^# \{0,1\}//'; }

while [ $# -gt 0 ]; do
    case "$1" in
        --project-dir) PROJECT_DIR="$2"; shift 2 ;;
        --package)     EXTRA_PACKAGES+=("$(cd "$2" && pwd)"); shift 2 ;;
        --no-bundled)  USE_BUNDLED=0; shift ;;
        --skip-install) DO_INSTALL=0; shift ;;
        --check)       DO_CHECK=1; shift ;;
        -h|--help)     usage; exit 0 ;;
        *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
    esac
done

say()  { printf '==> %s\n' "$*"; }
warn() { printf 'WARNING: %s\n' "$*" >&2; }
die()  { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

# Maven without colours or progress output, so its output can be parsed.
mvnq() { mvn -B -q -Dstyle.color=never "$@"; }

# Escape a string for an XML attribute value.
xml_escape() { printf '%s' "$1" | sed -e 's/&/\&amp;/g' -e 's/"/\&quot;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g'; }

# ---------------------------------------------------------------- prerequisites

command -v java >/dev/null || die "java not found; install JDK 25 or later"
command -v mvn  >/dev/null || die "mvn not found; Maven is needed once, to fetch the JARs"
JAVA_MAJOR=$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.specification.version = //p')
[ "${JAVA_MAJOR:-0}" -ge 25 ] 2>/dev/null || die "JDK 25 or later needed (found ${JAVA_MAJOR:-none})"

# ---------------------------------------------------------------- the modules
#
# Each module is: label|root folder|folder with pom.xml|example
# The root folder goes on the class path as a class folder: that is where the
# module's version.xml (its services) and, for some, fxtemplates/ live.

MODULES=()
MODULES+=("beast-pkgmgmt|$BEAST3_DIR|$BEAST3_DIR/beast-pkgmgmt|-")
MODULES+=("beast-base|$BEAST3_DIR|$BEAST3_DIR/beast-base|beast-base/examples/testHKY.xml")
MODULES+=("beast-fx|$BEAST3_DIR/beast-fx|$BEAST3_DIR/beast-fx|-")

missing=0
if [ $USE_BUNDLED = 1 ]; then
    while IFS='|' read -r name dir repo example; do
        [ -n "$name" ] || continue
        if [ ! -f "$PARENT_DIR/$dir/pom.xml" ]; then
            echo "Missing checkout of $name. Clone it with:" >&2
            echo "    git clone $repo \"$PARENT_DIR/$dir\"" >&2
            missing=1
            continue
        fi
        MODULES+=("$name|$PARENT_DIR/$dir|$PARENT_DIR/$dir|$example")
    done <<< "$BUNDLED"
    [ $missing = 0 ] || die "clone the packages above (next to beast3), or pass --no-bundled"

    # warn if the list above no longer matches the release's bundled list
    tsv="$BEAST3_DIR/release/bundled-packages.tsv"
    if [ -f "$tsv" ]; then
        want=$(grep -v '^#' "$tsv" | cut -f1 | grep -v '^$' | sort | tr '\n' ' ')
        have=$(printf '%s\n' "$BUNDLED" | cut -d'|' -f1 | grep -v '^$' | sort | tr '\n' ' ')
        [ "$want" = "$have" ] || warn "bundled packages in release/bundled-packages.tsv ($want) differ from this script ($have)"
    fi
fi
for p in ${EXTRA_PACKAGES[@]+"${EXTRA_PACKAGES[@]}"}; do
    [ -f "$p/pom.xml" ] && [ -d "$p/src/main/java" ] || die "$p is not a package checkout (needs pom.xml and src/main/java)"
    MODULES+=("$(basename "$p")|$p|$p|-")
done

# ---------------------------------------------------------------- Maven, once

if [ $DO_INSTALL = 1 ]; then
    say "Installing the BEAST 3 core into the local Maven repository (mvn install -DskipTests)"
    mvnq -f "$BEAST3_DIR/pom.xml" install -DskipTests
fi
CORE_VERSION=$(mvnq -f "$BEAST3_DIR/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout)
say "BEAST 3 core version $CORE_VERSION"

# The artifacts built from source in this project: excluded from the JAR list.
EXCLUDE="beast-pkgmgmt,beast-base,beast-fx"
for m in "${MODULES[@]}"; do
    IFS='|' read -r label root pomdir example <<< "$m"
    case "$label" in beast-*) continue ;; esac
    a=$(mvnq -f "$pomdir/pom.xml" help:evaluate -Dexpression=project.artifactId -DforceStdout)
    EXCLUDE="$EXCLUDE,$a"
done

mkdir -p "$PROJECT_DIR"
PROJECT_DIR="$(cd "$PROJECT_DIR" && pwd)"
WORK="$PROJECT_DIR/.deps"
rm -rf "$WORK"; mkdir -p "$WORK"

say "Listing third-party JARs"
for m in "${MODULES[@]}"; do
    IFS='|' read -r label root pomdir example <<< "$m"
    # beast.version: packages pin a BEAST release; build them against this core
    mvnq -f "$pomdir/pom.xml" dependency:list -DincludeScope=runtime \
        -Dbeast.version="$CORE_VERSION" -DexcludeArtifactIds="$EXCLUDE" \
        -DoutputAbsoluteArtifactFilename=true -DoutputFile="$WORK/$label.txt" \
        || die "Maven could not resolve the dependencies of $label"
done

# One JAR per groupId:artifactId:classifier, the highest version wins.
# Lines look like  g:a:jar[:classifier]:version:scope:/path/x.jar -- module m
cat "$WORK"/*.txt | sed -e 's/ -- .*//' -e 's/ (optional)$//' -e 's/^ *//' | grep -E '^[^ :]+:[^ :]+:jar:' |
awk -F: '{ if (NF == 7) { key=$1":"$2":"$4; v=$5; p=$7 } else { key=$1":"$2":"; v=$4; p=$6 }
           print key "\t" v "\t" p "\t" $1 }' |
sort -t$'\t' -k1,1 -k2,2V | awk -F'\t' '{ last[$1]=$0 } END { for (k in last) print last[k] }' |
sort > "$WORK/jars.tsv"

rm -rf "$PROJECT_DIR/lib"; mkdir -p "$PROJECT_DIR/lib/javafx"
while IFS=$'\t' read -r key version path group; do
    [ -f "$path" ] || die "missing $path"
    # JavaFX (and the jdk.jsobject it needs) must be on the module path
    case "$group" in
        org.openjfx) cp "$path" "$PROJECT_DIR/lib/javafx/" ;;
        *) cp "$path" "$PROJECT_DIR/lib/" ;;
    esac
done < "$WORK/jars.tsv"
say "$(ls "$PROJECT_DIR/lib" | grep -c '\.jar$') JARs in lib/, $(ls "$PROJECT_DIR/lib/javafx" | wc -l | tr -d ' ') in lib/javafx/"

# ---------------------------------------------------------------- .project

say "Writing the Eclipse project in $PROJECT_DIR"
LINKS=""
CLASSPATH_SRC=""
CLASSPATH_ROOTS=""
seen_roots=" "
link() { # name, absolute folder
    LINKS="$LINKS
		<link>
			<name>$1</name>
			<type>2</type>
			<location>$(xml_escape "$2")</location>
		</link>"
}
for m in "${MODULES[@]}"; do
    IFS='|' read -r label root pomdir example <<< "$m"
    link "$label-java" "$pomdir/src/main/java"
    CLASSPATH_SRC="$CLASSPATH_SRC
	<classpathentry kind=\"src\" path=\"$label-java\" excluding=\"module-info.java\"/>"
    if [ -d "$pomdir/src/main/resources" ]; then
        link "$label-resources" "$pomdir/src/main/resources"
        CLASSPATH_SRC="$CLASSPATH_SRC
	<classpathentry kind=\"src\" path=\"$label-resources\"/>"
    fi
    case "$seen_roots" in *" $root "*) continue ;; esac
    seen_roots="$seen_roots$root "
    rootname="$label-root"
    [ "$root" = "$BEAST3_DIR" ] && rootname="beast3-root"
    link "$rootname" "$root"
    CLASSPATH_ROOTS="$CLASSPATH_ROOTS
	<classpathentry kind=\"lib\" path=\"$rootname\"/>"
done

cat > "$PROJECT_DIR/.project" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!-- Generated by beast3/scripts/eclipse/make-single-project.sh; re-run it rather than editing. -->
<projectDescription>
	<name>$PROJECT_NAME</name>
	<comment>BEAST 3 core and packages in one plain Java project (beast3 issue #239)</comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
	<linkedResources>$LINKS
	</linkedResources>
</projectDescription>
EOF

# ---------------------------------------------------------------- .classpath

# JDK 25 still has the jdk.jsobject module (JDK 26 dropped it). Eclipse refuses
# netscape.javascript when it is in both the JDK and a class-path JAR, so the
# jdk-jsobject JAR goes on the class path only when the JDK lacks the module.
# The launchers keep it on the module path, where the JDK's copy wins.
JDK_HAS_JSOBJECT=0
java --list-modules 2>/dev/null | grep -q '^jdk.jsobject@' && JDK_HAS_JSOBJECT=1
CLASSPATH_LIBS=""
for j in "$PROJECT_DIR"/lib/*.jar "$PROJECT_DIR"/lib/javafx/*.jar; do
    case "$(basename "$j")" in jdk-jsobject-*) [ $JDK_HAS_JSOBJECT = 1 ] && continue ;; esac
    CLASSPATH_LIBS="$CLASSPATH_LIBS
	<classpathentry kind=\"lib\" path=\"${j#$PROJECT_DIR/}\"/>"
done

cat > "$PROJECT_DIR/.classpath" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!-- Generated by beast3/scripts/eclipse/make-single-project.sh; re-run it rather than editing.
     Sources: src/main/java and src/main/resources of each module, module-info.java excluded.
     *-root class folders: each checkout's root, for its version.xml and fxtemplates/.
     lib/: third-party JARs. lib/javafx/: JavaFX, also put on the module path by the launchers. -->
<classpath>$CLASSPATH_SRC$CLASSPATH_ROOTS$CLASSPATH_LIBS
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-25"/>
	<classpathentry kind="output" path="bin"/>
</classpath>
EOF

mkdir -p "$PROJECT_DIR/.settings"
cat > "$PROJECT_DIR/.settings/org.eclipse.jdt.core.prefs" <<EOF
eclipse.preferences.version=1
org.eclipse.jdt.core.compiler.codegen.targetPlatform=25
org.eclipse.jdt.core.compiler.compliance=25
org.eclipse.jdt.core.compiler.source=25
org.eclipse.jdt.core.compiler.release=enabled
org.eclipse.jdt.core.compiler.problem.deprecation=ignore
org.eclipse.jdt.core.compiler.problem.forbiddenReference=ignore
EOF
cat > "$PROJECT_DIR/.settings/org.eclipse.core.resources.prefs" <<EOF
eclipse.preferences.version=1
encoding/<project>=UTF-8
EOF

# ---------------------------------------------------------------- launchers
#
# A user package folder inside the project keeps installed packages (which
# would clash with the ones built here) out of the way, and switches off the
# package update check, which would otherwise try to install them again.

mkdir -p "$PROJECT_DIR/user-packages"
printf 'package.update.status=DO_NOT_CHECK\n' > "$PROJECT_DIR/user-packages/beauti.properties"

VMARGS="--module-path &quot;\${project_loc:$PROJECT_NAME}/lib/javafx&quot; --add-modules $FX_MODULES --enable-native-access=ALL-UNNAMED,javafx.graphics,javafx.web,javafx.media -Dbeast.user.package.dir=&quot;\${project_loc:$PROJECT_NAME}/user-packages&quot;"

launcher() { # file name, main class, program arguments
    cat > "$PROJECT_DIR/$1.launch" <<EOF
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.jdt.launching.localJavaApplication">
    <listAttribute key="org.eclipse.debug.ui.favoriteGroups">
        <listEntry value="org.eclipse.debug.ui.launchGroup.run"/>
        <listEntry value="org.eclipse.debug.ui.launchGroup.debug"/>
    </listAttribute>
    <booleanAttribute key="org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD" value="false"/>
    <stringAttribute key="org.eclipse.jdt.launching.MAIN_TYPE" value="$2"/>
    <stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="$PROJECT_NAME"/>
    <stringAttribute key="org.eclipse.jdt.launching.PROGRAM_ARGUMENTS" value="$3"/>
    <stringAttribute key="org.eclipse.jdt.launching.VM_ARGUMENTS" value="$VMARGS"/>
    <stringAttribute key="org.eclipse.jdt.launching.WORKING_DIRECTORY" value="\${project_loc:$PROJECT_NAME}"/>
</launchConfiguration>
EOF
}
launcher "BEAUti"        "beastfx.app.beauti.Beauti" ""
launcher "BEAST"         "beastfx.app.beast.BeastMain" "-overwrite \${file_prompt}"
launcher "TreeAnnotator" "beastfx.app.treeannotator.TreeAnnotator" ""
launcher "LogCombiner"   "beastfx.app.tools.LogCombiner" ""
launcher "AppLauncher"   "beastfx.app.tools.AppLauncher" ""

rm -rf "$WORK"
say "Done. In Eclipse: File > Import > General > Existing Projects into Workspace, and choose $PROJECT_DIR"
say "Then run BEAUti, BEAST, ... from Run > Run Configurations (Java Application)."

# ---------------------------------------------------------------- --check

[ $DO_CHECK = 1 ] || exit 0

say "Check: compiling with the Eclipse compiler (ECJ), as Eclipse would"
CHECK="$PROJECT_DIR/.check"
rm -rf "$CHECK"; mkdir -p "$CHECK/bin"
mvnq dependency:copy -Dartifact=org.eclipse.jdt:ecj:3.44.0 -DoutputDirectory="$CHECK" >/dev/null
ECJ=$(ls "$CHECK"/ecj-*.jar)

CP_LIBS=$(ls "$PROJECT_DIR"/lib/*.jar "$PROJECT_DIR"/lib/javafx/*.jar | { if [ $JDK_HAS_JSOBJECT = 1 ]; then grep -v '/jdk-jsobject-'; else cat; fi; } | tr '\n' ':')
CP_ROOTS=""
: > "$CHECK/sources.txt"
seen_roots=" "
for m in "${MODULES[@]}"; do
    IFS='|' read -r label root pomdir example <<< "$m"
    find "$pomdir/src/main/java" -name '*.java' ! -name module-info.java >> "$CHECK/sources.txt"
    # Eclipse copies the non-Java files of source folders to the output folder
    for s in "$pomdir/src/main/java" "$pomdir/src/main/resources"; do
        [ -d "$s" ] && (cd "$s" && find . -type f ! -name '*.java' -exec sh -c 'mkdir -p "$0/$(dirname "$1")" && cp "$1" "$0/$1"' "$CHECK/bin" {} \;)
    done
    case "$seen_roots" in *" $root "*) ;; *) seen_roots="$seen_roots$root "; CP_ROOTS="$CP_ROOTS$root:" ;; esac
done
# ECJ compiles against the JDK it runs on, as Eclipse compiles against the
# project's JRE. -proceedOnError: like Eclipse, still write the other classes,
# so the examples below are tried even when something fails to compile.
failed=0
if java -jar "$ECJ" --release 25 -encoding UTF-8 -proc:none -nowarn -proceedOnError \
        -d "$CHECK/bin" -cp "$CP_LIBS" @"$CHECK/sources.txt" > "$CHECK/ecj.log" 2>&1; then
    say "PASS  compiled $(wc -l < "$CHECK/sources.txt" | tr -d ' ') source files with ECJ on Java $JAVA_MAJOR"
else
    echo "FAIL  ECJ on Java $JAVA_MAJOR (log: $CHECK/ecj.log)"
    grep -A3 'ERROR in' "$CHECK/ecj.log" | sed 's/^/        /' | head -40
    failed=1
fi

# The class path an Eclipse launch uses: output folder, then the .classpath entries in order.
RUN_CP="$CHECK/bin:$CP_ROOTS$CP_LIBS"
RUN="java --module-path $PROJECT_DIR/lib/javafx --add-modules $FX_MODULES -Dbeast.user.package.dir=$PROJECT_DIR/user-packages -cp $RUN_CP"

# Headless BeastMain (no window). Run for this many seconds, then stop; pass means it reached the sampling log.
SECONDS_PER_RUN=${SECONDS_PER_RUN:-20}
for m in "${MODULES[@]}"; do
    IFS='|' read -r label root pomdir example <<< "$m"
    [ "$example" != "-" ] || continue
    xml="$root/$example"
    [ "$label" = beast-base ] && xml="$BEAST3_DIR/$example"
    out="$CHECK/run-$label"; mkdir -p "$out"
    { (cd "$out" && perl -e "alarm $SECONDS_PER_RUN; exec @ARGV" $RUN beast.base.minimal.BeastMain -overwrite -seed 1 "$xml" > log.txt 2>&1); } 2>/dev/null || true
    if grep -qE '^ +[0-9]+[[:space:]]+-?[0-9]' "$out/log.txt" && ! grep -qE 'Exception|Error [0-9]+ parsing' "$out/log.txt"; then
        say "PASS  $label  $(basename "$xml")"
    else
        echo "FAIL  $label  $(basename "$xml")  (log: $out/log.txt)"
        grep -m3 -E 'Exception|Error|Could not' "$out/log.txt" | sed 's/^/        /'
        failed=1
    fi
done
[ $failed = 0 ] || die "check failed (see FAIL lines above)"
say "Check passed"
