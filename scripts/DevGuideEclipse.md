# Setup BEAST 3 in Eclipse

This is the developer guide for setting up the BEAST 3 project in Eclipse IDE.

## Prerequisites

### JDK 25

Install JDK 25 from [Azul Zulu](https://www.azul.com/downloads/?package=jdk#zulu) or any JDK 25+ distribution. A bundled JavaFX JDK is no longer needed — JavaFX is resolved as a Maven dependency.

### Maven

Maven 3.9+ must be installed. Eclipse embeds Maven (m2e), but you can also install it separately.

### Eclipse IDE

Download or upgrade to the latest version of Eclipse IDE for Java Developers:

https://www.eclipse.org/downloads/

Ensure the **m2e** (Maven Integration for Eclipse) plugin is installed — it ships with the standard "Eclipse IDE for Java Developers" package.

## Import the project

1. **File → Import → Maven → Existing Maven Projects**.
2. Set **Root Directory** to the `beast3` repository root.
3. Eclipse will list the parent POM and the three modules (`beast-pkgmgmt`, `beast-base`, `beast-fx`). Make sure all are selected and click **Finish**.
4. m2e will import the project structure, resolve dependencies, and configure the build path automatically.

## Configure JDK

If Eclipse does not automatically pick up JDK 25:

1. Open **Window → Preferences → Java → Installed JREs** (or **Eclipse → Settings** on macOS).
2. **Add → Standard VM** and point to your JDK 25 installation.
3. Check it as the default JRE.
4. Under **Java → Compiler**, set **Compiler compliance level** to **25**.

To set the JDK per-project (if you have other projects on older JDKs):

1. Right-click the `beast3` parent project → **Properties → Java Build Path → Libraries**.
2. Select the **JRE System Library** entry and click **Edit**.
3. Choose **Alternate JRE** or **Execution environment** and select JDK 25.

## Run application in Eclipse

Create a [Run Configuration](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/tasks/tasks-java-local-configuration.htm):

1. **Run → Run Configurations → Java Application → New**
2. Set **Project** to `beast-fx`
3. Set **Main class** to `beastfx.app.beast.BeastMain` (or `beastfx.app.beauti.Beauti` for BEAUti)
4. On the **Arguments** tab, set **Program arguments** to your XML file path (for BeastMain)
5. Eclipse resolves the module path from Maven via m2e — no manual `--module-path` or `--add-modules` flags are needed.

## BEAGLE native library

If you have [BEAGLE](https://github.com/beagle-dev/beagle-lib) installed and
want to use it from Eclipse, add the following **VM arguments** to your run
configuration (**Run → Run Configurations → Arguments tab → VM arguments**):

```
--enable-native-access=javafx.graphics,beagle -Djava.library.path=/usr/local/lib
```

- `--enable-native-access=javafx.graphics,beagle`: Java 25 restricts JNI
  calls by default. This flag allows the `javafx.graphics` and `beagle`
  modules to load their native libraries without warnings. Without it you
  will see `WARNING: Use --enable-native-access=...` at startup, and in a
  future Java release the calls will be blocked entirely.
  **Important:** use a single comma-separated flag, not two separate
  `--enable-native-access` flags — the second would override the first.
- `-Djava.library.path=...`: points to the directory containing
  `libhmsbeagle-jni.dylib` (or `.so` on Linux). Common locations:
  - macOS (Homebrew on Apple Silicon): `/opt/homebrew/lib`
  - macOS (system): `/usr/local/lib`
  - Linux: `/usr/local/lib` or `/usr/lib`

The deployed BEAST.app sets both of these automatically in its launcher
scripts, which is why BEAGLE works out of the box in the user environment
but requires manual configuration in the IDE.

## Developing an external package alongside BEAST 3

To test your own BEAST package against BEAST 3 core in a single IDE session:

1. Import the `beast3` project as described above.
2. **File → Import → Maven → Existing Maven Projects** and select your package's root directory.
3. Add a `module-info.java` to your package with `provides` declarations for your service implementations:
   ```java
   open module my.beast.package {
       requires beast.pkgmgmt;
       requires beast.base;

       provides beast.base.core.BEASTInterface with
           my.beast.package.MyModel,
           my.beast.package.MyOperator;
   }
   ```
4. If your package declares a Maven dependency on `beast-base` (or `beast-fx`), m2e will resolve it from the workspace reactor automatically. Otherwise, add a project dependency manually: right-click your package project → **Properties → Java Build Path → Projects → Add** and select `beast-base`.
5. Run `BeastMain` — BEAST discovers your package's services automatically from the module descriptors in the boot layer. No `version.xml` or manual package installation needed during development.

## Multi-package workspace for lead developers

Lead developers of BEAST 3 who maintain the core and many packages simultaneously can use a workspace aggregator POM to import everything into Eclipse at once.

### Directory layout

Create a parent directory and clone all the repositories into it:

```
beast-workspace/
├── pom.xml              ← aggregator POM (local only, not committed to any repo)
├── beast3/              ← git clone of core
├── beast-morph-models/  ← git clone of a package
├── beast-phylodynamics/ ← git clone of another package
└── ...
```

### Aggregator POM

Create a minimal `pom.xml` in the `beast-workspace/` directory that lists all repositories as modules:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>local</groupId>
    <artifactId>beast-workspace</artifactId>
    <version>0</version>
    <packaging>pom</packaging>

    <modules>
        <module>beast3</module>
        <module>beast-morph-models</module>
        <module>beast-phylodynamics</module>
        <!-- add more packages as needed -->
    </modules>
</project>
```

### Import

1. **File → Import → Maven → Existing Maven Projects**.
2. Set **Root Directory** to `beast-workspace/`.
3. Eclipse will discover the aggregator, all BEAST 3 core modules, and all package modules. Select all and click **Finish**.

m2e's workspace resolution means that when a package declares a dependency on `beast-base` or `beast-fx`, Eclipse resolves it from the workspace source rather than from a JAR. Source navigation, refactoring, and debugging all work across project boundaries automatically.

### Building from the command line

From the `beast-workspace/` directory, Maven builds everything in dependency order:

```bash
mvn compile
mvn test -DskipTests   # or mvn test to run all tests across all projects
```

### Adding or removing packages

Edit `beast-workspace/pom.xml` to add or remove `<module>` entries, then **Maven → Update Project** (Alt+F5) in Eclipse. Each package must be a Maven project with a `pom.xml` that declares its BEAST 3 dependencies.

## One plain project for the core and packages, without m2e

This setup works like the Ant setup of BEAST 2.7. The core and several packages sit in **one plain Java project**, everything runs on the class path, and Eclipse never calls Maven. Maven is used once, by a script, to fetch the third-party JARs.

An Eclipse project can hold only one `module-info.java`, so this setup leaves them all out. BEAST then finds each package's services in its `version.xml`, as in 2.7.

### Checkouts

Clone the core and the packages bundled with the release into the same folder:

```
git/
├── beast3/              https://github.com/CompEvol/beast3
├── ORC/                 https://github.com/jordandouglas/ORC
├── sampled-ancestors/   https://github.com/CompEvol/sampled-ancestors
├── bModelTest/          https://github.com/BEAST2-Dev/bModelTest
├── CCD/                 https://github.com/CompEvol/CCD
└── starbeast3/          https://github.com/rbouckaert/starbeast3
```

These five are the packages in `release/bundled-packages.tsv`. Together they cover a package that needs only `beast-base` (ORC, SA), one that also needs `beast-fx` (bModelTest, CCD), and one that needs other packages (StarBeast3 needs ORC and SA). If a checkout is missing, the script prints the `git clone` command for it.

### Generate the project

```bash
cd beast3
scripts/eclipse/make-single-project.sh
```

This creates `git/beast-eclipse/`. Use `--project-dir DIR` to put it somewhere else. The script:

1. runs `mvn install -DskipTests` in `beast3` (skip this with `--skip-install`);
2. asks Maven for the runtime JARs of each module and package, keeps the highest version of each, and copies them to `lib/`, with JavaFX in `lib/javafx/`;
3. writes `.project` and `.classpath`. For each checkout, `src/main/java` (without `module-info.java`) and `src/main/resources` are linked source folders, so you edit the files in the checkouts themselves. Each checkout's root folder is also on the class path, as a class folder, for its `version.xml` and `fxtemplates/`;
4. writes launch configurations for BEAUti, BEAST, TreeAnnotator, LogCombiner and AppLauncher.

Re-run the script after a pull that changes a dependency, and after adding or removing a package. It only rewrites files in the project folder; your code is in the checkouts.

To check the result without opening Eclipse, add `--check`. The script then compiles everything with ECJ, the Eclipse compiler, and runs each package's example in BEAST for 20 seconds, with no window:

```
==> PASS  compiled 895 source files with ECJ on Java 25
==> PASS  beast-base  testHKY.xml
==> PASS  ORC  caterpillars.xml
...
```

### Import and run

1. **File → Import → General → Existing Projects into Workspace**, and choose `git/beast-eclipse`. Don't use the Maven import.
2. The project uses the **JavaSE-25** execution environment. Make sure a JDK 25 is listed under **Preferences → Java → Installed JREs** (see [Configure JDK](#configure-jdk)). Run the script with that same JDK as `java`: whether the JDK includes the `jdk.jsobject` module decides what goes on the class path.
3. **Run → Run Configurations → Java Application** lists **BEAUti**, **BEAST** (asks for an XML file), **TreeAnnotator**, **LogCombiner** and **AppLauncher**.

The launch configurations do three things to make this work:

- They put `lib/javafx` on the module path with `--add-modules`. JavaFX refuses to start an application whose main class is on the class path only ("JavaFX runtime components are missing").
- They set `-Dbeast.user.package.dir` to `beast-eclipse/user-packages`, an empty folder. Packages installed in your normal BEAST folder would otherwise load next to the ones built here and clash.
- They turn off BEAUti's check for package updates (`package.update.status=DO_NOT_CHECK` in that folder). The check would otherwise try to install the packages you are editing.

For BEAGLE, add `-Djava.library.path=...` to the VM arguments (see [BEAGLE native library](#beagle-native-library)).

### Add another package

Clone it next to the others and pass it to the script:

```bash
scripts/eclipse/make-single-project.sh --package ../MyPackage
```

Use `--package` once for each extra package. Each one needs a `pom.xml`, `src/main/java` and a `version.xml` at its root that lists every service in its `module-info.java`. On the module path BEAST reads `module-info.java`, so a class missing from `version.xml` goes unnoticed in a release but fails here. Its BEAUti templates go in `fxtemplates/` at the root, or in `src/main/resources/<module>/fxtemplates/`.

Packages your package depends on but that aren't in the project come from Maven as JARs, like any other dependency.

### Limitations

- AppLauncher lists only the apps of installed packages, not of the packages in the project. To run a package app, run its main class directly (**Run As → Java Application**), with the VM arguments of the BEAUti launch configuration if it uses JavaFX.
- Tests (`src/test`) are not in the project. Run them with Maven, or use the per-module import above.
- StarBeast3 1.3.1 lacks `OperatorScheduleRecalculator` in its `version.xml`, so its examples fail here until [rbouckaert/starbeast3#21](https://github.com/rbouckaert/starbeast3/pull/21) is merged.
- Use JDK 25 for the project, not 26. JDK 26 removed `Thread.stop()`, and `beastfx.app.beast.Controller` still calls it, so `beast-fx` doesn't compile in Eclipse on JDK 26.
- `--check` uses ECJ on its own, which misses some of the checks Eclipse makes when it builds a project. Building in Eclipse remains the real test.

## Running tests

Right-click a test class or the `src/test/java` folder and select **Run As → JUnit Test**. Eclipse picks up the JUnit 5 and JUnit 4 dependencies from Maven automatically.

Several operator and BEAUti tests run long MCMC chains and are tagged `@Tag("slow")`. From the command line these are excluded by default (`mvn test`). To include them use `mvn test -Pslow-tests`. In Eclipse all tests run regardless of the Maven profile unless you configure the run configuration to pass `-Pslow-tests`.
