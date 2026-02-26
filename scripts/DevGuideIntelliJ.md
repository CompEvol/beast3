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

1. **File → Open** and select the `beast3modular` repository root directory.
2. IntelliJ will detect the Maven `pom.xml` and automatically import the project structure, modules, and dependencies.
3. If prompted, select **Trust Project** and **Open as Project**.

That's it — no manual library setup, module configuration, or package prefixes are needed. Maven handles all dependency resolution.

## Configure JDK

If IntelliJ does not automatically pick up JDK 25:

1. Open **File → Project Structure → Project**.
2. Set **SDK** to your JDK 25 installation.
3. Set **Language level** to **25**.

## One-time setup: install local JARs

Two dependencies (`beagle.jar` and `colt.jar`) are not in Maven Central. If you haven't already, install them from the terminal:

```bash
mvn install:install-file -Dfile=lib/beagle.jar -DgroupId=beagle -DartifactId=beagle -Dversion=4.0.1 -Dpackaging=jar
mvn install:install-file -Dfile=lib/colt.jar -DgroupId=colt -DartifactId=colt -Dversion=1.2.0 -Dpackaging=jar
```

Then reload Maven in IntelliJ (right-click the root `pom.xml` → **Maven → Reload project**).

## Run application in IntelliJ

Create a [Run Configuration](https://www.jetbrains.com/help/idea/creating-and-running-your-first-java-application.html#create_jar_run_config):

1. **Run → Edit Configurations → Add New → Application**
2. Set **Module** to `beast-base`
3. Set **Main class** to `beastfx.app.beast.BeastMain` (or `beastfx.app.beauti.Beauti` for BEAUti)
4. Set **Program arguments** to your XML file path (for BeastMain)
5. IntelliJ automatically configures the module path from Maven — no manual `--module-path` or `--add-modules` flags are needed.

## Developing an external package alongside BEAST 3

To test your own BEAST package against BEAST 3 core in a single IDE session:

1. Open the `beast3modular` project as described above.
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

## Running tests

Right-click a test class or the `src/test/java` directory and select **Run Tests**. IntelliJ picks up the JUnit 5 and JUnit 4 dependencies from Maven automatically.
