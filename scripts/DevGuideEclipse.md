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

## One-time setup: install local JARs

Two dependencies (`beagle.jar` and `colt.jar`) are not in Maven Central. They ship as modular JARs (containing `module-info.class`); the corresponding `module-info.java` sources live in `lib/beagle/` and `lib/colt/`. If you haven't already, install them from the terminal:

```bash
mvn install:install-file -Dfile=lib/beagle.jar -DgroupId=io.github.compevol -DartifactId=beagle -Dversion=1.0 -Dpackaging=jar
mvn install:install-file -Dfile=lib/colt.jar -DgroupId=io.github.compevol -DartifactId=colt -Dversion=1.0 -Dpackaging=jar
```

Then update the Maven projects in Eclipse: select all beast3 modules, right-click → **Maven → Update Project** (or press **Alt+F5**).

## Run application in Eclipse

Create a [Run Configuration](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/tasks/tasks-java-local-configuration.htm):

1. **Run → Run Configurations → Java Application → New**
2. Set **Project** to `beast-fx`
3. Set **Main class** to `beastfx.app.beast.BeastMain` (or `beastfx.app.beauti.Beauti` for BEAUti)
4. On the **Arguments** tab, set **Program arguments** to your XML file path (for BeastMain)
5. Eclipse resolves the module path from Maven via m2e — no manual `--module-path` or `--add-modules` flags are needed.

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

## Running tests

Right-click a test class or the `src/test/java` folder and select **Run As → JUnit Test**. Eclipse picks up the JUnit 5 and JUnit 4 dependencies from Maven automatically.

Several operator and BEAUti tests run long MCMC chains and are tagged `@Tag("slow")`. From the command line these are excluded by default (`mvn test`). To include them use `mvn test -Pslow-tests`. In Eclipse all tests run regardless of the Maven profile unless you configure the run configuration to pass `-Pslow-tests`.
