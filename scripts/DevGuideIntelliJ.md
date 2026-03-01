# Setup BEAST 3 in IntelliJ

This is the developer guide for setting up the BEAST 3 project in IntelliJ IDEA.

## Prerequisites

### JDK 25

Install JDK 25 from [Azul Zulu](https://www.azul.com/downloads/?package=jdk#zulu) or any JDK 25+ distribution. A bundled JavaFX JDK is no longer needed — JavaFX is resolved as a Maven dependency.

### Maven

Maven 3.9+ must be installed. IntelliJ bundles Maven, but you can also install it separately.

### IntelliJ IDEA

Download or upgrade to the latest version of IntelliJ:

https://www.jetbrains.com/idea/download/

## Open the project

1. **File → Open** and select the `beast3` repository root directory.
2. IntelliJ will detect the Maven `pom.xml` and automatically import the project structure, modules, and dependencies.
3. If prompted, select **Trust Project** and **Open as Project**.

That's it — no manual library setup, module configuration, or package prefixes are needed. Maven handles all dependency resolution.

## Configure JDK

If IntelliJ does not automatically pick up JDK 25:

1. Open **File → Project Structure → Project**.
2. Set **SDK** to your JDK 25 installation.
3. Set **Language level** to **25**.

## One-time setup: install local JARs

Two dependencies (`beagle.jar` and `colt.jar`) are not in Maven Central. They ship as modular JARs (containing `module-info.class`); the corresponding `module-info.java` sources live in `lib/beagle/` and `lib/colt/`. If you haven't already, install them from the terminal:

```bash
mvn install:install-file -Dfile=lib/beagle.jar -DgroupId=io.github.compevol -DartifactId=beagle -Dversion=1.0 -Dpackaging=jar
mvn install:install-file -Dfile=lib/colt.jar -DgroupId=io.github.compevol -DartifactId=colt -Dversion=1.0 -Dpackaging=jar
```

Then reload Maven in IntelliJ (right-click the root `pom.xml` → **Maven → Reload project**).

## Run application in IntelliJ

Create a [Run Configuration](https://www.jetbrains.com/help/idea/creating-and-running-your-first-java-application.html#create_jar_run_config):

1. **Run → Edit Configurations → Add New → Application**
2. Set **Module** to `beast-fx`
3. Set **Main class** to `beastfx.app.beast.BeastMain` (or `beastfx.app.beauti.Beauti` for BEAUti)
4. Set **Program arguments** to your XML file path (for BeastMain)
5. IntelliJ automatically configures the module path from Maven — no manual `--module-path` or `--add-modules` flags are needed.

## Developing an external package alongside BEAST 3

To test your own BEAST package against BEAST 3 core in a single IDE session:

1. Open the `beast3` project as described above.
2. **File → New → Module from Existing Sources** and select your package's root directory (or its `pom.xml`).
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
4. Set your package module to depend on `beast-base` (IntelliJ will resolve this from the Maven reactor if your package is also a Maven module, or you can add a module dependency manually).
5. Run `BeastMain` — BEAST discovers your package's services automatically from the module descriptors in the boot layer.  No `version.xml` or manual package installation needed during development.

## Multi-package workspace for lead developers

Lead developers of BEAST 3 who maintain the core and many packages simultaneously can use a workspace aggregator POM to import everything into IntelliJ at once.

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

1. **File → Open** and select the `beast-workspace/` directory.
2. IntelliJ will detect the aggregator POM and import all BEAST 3 core modules and package modules.
3. If prompted, select **Trust Project** and **Open as Project**.

IntelliJ's Maven reactor resolution means that when a package declares a dependency on `beast-base` or `beast-fx`, IntelliJ resolves it from the workspace source rather than from a JAR. Source navigation, refactoring, and debugging all work across project boundaries automatically.

### Building from the command line

From the `beast-workspace/` directory, Maven builds everything in dependency order:

```bash
mvn compile
mvn test -DskipTests   # or mvn test to run all tests across all projects
```

### Adding or removing packages

Edit `beast-workspace/pom.xml` to add or remove `<module>` entries, then right-click the root `pom.xml` → **Maven → Reload project**. Each package must be a Maven project with a `pom.xml` that declares its BEAST 3 dependencies.

## Running tests

Right-click a test class or the `src/test/java` directory and select **Run Tests**. IntelliJ picks up the JUnit 5 and JUnit 4 dependencies from Maven automatically.

Several operator and BEAUti tests run long MCMC chains and are tagged `@Tag("slow")`. From the command line these are excluded by default (`mvn test`). To include them use `mvn test -Pslow-tests`. In IntelliJ all tests run regardless of the Maven profile unless you configure the run configuration to pass `-Pslow-tests`.
