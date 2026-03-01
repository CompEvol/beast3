BEAST 3
=======

BEAST is a cross-platform program for Bayesian inference using MCMC of
molecular sequences. It is entirely oriented towards rooted,
time-measured phylogenies inferred using strict or relaxed molecular
clock models. It can be used as a method of reconstructing phylogenies
but is also a framework for testing evolutionary hypotheses without
conditioning on a single tree topology. BEAST uses MCMC to average
over tree space, so that each tree is weighted proportional to its
posterior probability. We include a simple to use user-interface
program for setting up standard analyses and a suite of programs for
analysing the results.

What's New in BEAST 3
---------------------

BEAST 3 is a major update from BEAST 2. Key changes:

- **Maven build system** — replaces the previous Ant build. Dependencies are declared in `pom.xml` and resolved automatically.
- **JPMS modules** — the codebase is split into `beast.pkgmgmt`, `beast.base`, and `beast.fx` Java modules with explicit `module-info.java` descriptors. The core inference engine (`beast.base`) has no JavaFX dependency and can be used headlessly; the GUI (`beast.fx`) is a separate module.
- **Java 25** — requires JDK 25 or later.
- **Strongly typed inputs** — new `beast.base.spec` hierarchy replaces loosely-typed parameters with compile-time-checked typed inputs.
- **External packages** — discovered via `module-info.java` `provides` declarations (primary) or `version.xml` service entries (for legacy/non-modular JARs). Deployed packages are loaded into a JPMS `ModuleLayer` per package.

Project Structure
-----------------

```
beast3/            (parent POM)
├── beast-pkgmgmt/        (package manager module)
├── beast-base/           (core BEAST module — no JavaFX dependency)
├── beast-fx/             (JavaFX GUI module — BEAUti, BEAST app, tools)
└── lib/                  (local JARs + module-info sources)
    ├── beagle.jar        (modular JAR — module beagle)
    ├── beagle/           (module-info.java source)
    ├── colt.jar          (modular JAR — module colt)
    └── colt/             (module-info.java source)
```

Building
--------

### Prerequisites

- **Java 25** — install from [Azul Zulu](https://www.azul.com/downloads/?package=jdk#zulu) or any JDK 25+ distribution.
- **Maven 3.9+** — install from [maven.apache.org](https://maven.apache.org/) or via your package manager.

### One-time setup: install local JARs

Two dependencies (`beagle.jar` and `colt.jar`) are not in Maven Central. They ship as modular JARs (containing `module-info.class`); the corresponding `module-info.java` sources live in `lib/beagle/` and `lib/colt/`. Install them to your local repository:

```bash
mvn install:install-file -Dfile=lib/beagle.jar -DgroupId=io.github.compevol -DartifactId=beagle -Dversion=1.0 -Dpackaging=jar
mvn install:install-file -Dfile=lib/colt.jar -DgroupId=io.github.compevol -DartifactId=colt -Dversion=1.0 -Dpackaging=jar
```

### Compile

```bash
mvn compile
```

### Test

```bash
mvn test                    # fast tests only (skips @Tag("slow"))
mvn test -Pslow-tests       # all tests including slow
mvn test -Dgroups=slow      # only slow tests
```

Several operator and BEAUti tests run MCMC chains of 1M–11M iterations and are tagged `@Tag("slow")`. They are excluded from the default build via the `surefire.excludedGroups` property. Activate the `slow-tests` profile to include them.

BEAUti GUI tests use [TestFX](https://github.com/TestFX/TestFX) and run headlessly via the Monocle Glass platform (`openjfx-monocle`). The surefire plugin is configured with the required system properties (`testfx.headless`, `glass.platform=Monocle`, etc.) and sets `workingDirectory` to `target/classes` so that BEAUti can discover its `fxtemplates/` at runtime. No display server is needed to run the tests.

Running
-------

### From the command line

Build the project, then use the `exec-maven-plugin` to launch BEAST applications with the correct module path:

```bash
mvn install -DskipTests

# Run BEAST on an XML file
mvn -pl beast-fx exec:exec -Dbeast.args="example.xml"

# Run BEAUti
mvn -pl beast-fx exec:exec -Dbeast.main=beastfx.app.beauti.Beauti

# Run other tools (LogCombiner, TreeAnnotator, etc.)
mvn -pl beast-fx exec:exec -Dbeast.main=beastfx.app.tools.LogCombiner
mvn -pl beast-fx exec:exec -Dbeast.main=beastfx.app.tools.TreeAnnotator
```

The `-Dbeast.main=` property selects the main class (defaults to `beastfx.app.beast.BeastMain`). The `-Dbeast.args=` property passes arguments to the application.

### From IntelliJ

See `scripts/DevGuideIntelliJ.md`. IntelliJ resolves the full module path from Maven automatically.

Using BEAST 3 as a Maven dependency
-------------------------------------

BEAST 3 artifacts are published to [Maven Central](https://central.sonatype.com/). Projects like LPhyBEAST can depend on BEAST 3 without cloning and building locally.

Add BEAST dependencies to your project's `pom.xml`. For **headless / library** usage (no JavaFX dependency):

```xml
<dependency>
    <groupId>io.github.compevol</groupId>
    <artifactId>beast-base</artifactId>
    <version>2.8.0</version>
</dependency>
```

For **GUI** usage (includes JavaFX, BEAUti, and all GUI tools):

```xml
<dependency>
    <groupId>io.github.compevol</groupId>
    <artifactId>beast-fx</artifactId>
    <version>2.8.0</version>
</dependency>
```

No repository configuration or authentication is needed — Maven Central is the default repository.

### JPMS module declaration (if your project uses modules)

```java
open module my.project {
    requires beast.base;
    // requires beast.fx;       // only if you need the GUI components
    // requires beast.pkgmgmt;  // only if you use package management APIs
}
```

If your project does not use JPMS modules, the BEAST classes are accessible from the unnamed module without any extra configuration.

### Alternative: local install

If you prefer to build from source instead of using GitHub Packages, you can install BEAST 3 to your local Maven repository:

```bash
cd /path/to/beast3
mvn install:install-file -Dfile=lib/beagle.jar -DgroupId=io.github.compevol -DartifactId=beagle -Dversion=1.0 -Dpackaging=jar
mvn install:install-file -Dfile=lib/colt.jar -DgroupId=io.github.compevol -DartifactId=colt -Dversion=1.0 -Dpackaging=jar
mvn install -DskipTests
```

In this case, no `<repositories>` block or `settings.xml` configuration is needed.

Installing packages from Maven Central
---------------------------------------

External BEAST packages that are published to Maven Central can be installed
directly — no ZIP download or CBAN entry required.

### From BEAUti

Open the Package Manager (`File > Manage Packages`), click **Install from Maven**,
and enter the Maven coordinates:

```
groupId:artifactId:version
```

For example: `io.github.compevol:beast-morph-models:1.3.0`

### From the command line

```bash
packagemanager -maven io.github.compevol:beast-morph-models:1.3.0
```

Maven packages are resolved via [Apache Maven Resolver](https://maven.apache.org/resolver/),
downloaded to a local cache (`~/.beast/2.8/maven-repo/`), and loaded into a JPMS
`ModuleLayer` at runtime — just like ZIP-installed packages. The package's `version.xml`
(embedded at the JAR root) is used for service discovery.

Installed Maven packages are tracked in `maven-packages.xml` alongside the
standard `beauti.cfg` package list.

### Custom Maven repositories

By default, packages are resolved from Maven Central. To add an additional
Maven repository (e.g. one hosted by your organisation):

```bash
packagemanager -addMavenRepository https://beast2.org/maven/
packagemanager -listMavenRepositories
packagemanager -delMavenRepository https://beast2.org/maven/
```

### For package developers

To publish your package to Maven Central (or another Maven repository), see the
[beast-package-skeleton](https://github.com/CompEvol/beast-package-skeleton) template
which includes a `release` profile and full publishing instructions.

Development
-----------

See `scripts/DevGuideIntelliJ.md` for IntelliJ IDEA setup instructions, including how to develop an external BEAST package alongside BEAST 3 core in a single IDE session.

For guidance on migrating external packages, see `scripts/migration-guide.md` (migrating from BEAST v2.7 to v3).

Development rules and philosophy are discussed at
[beast2.org/package-development-guide/core-development-rules](https://www.beast2.org/package-development-guide/core-development-rules/).

License
-------

BEAST is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as published
by the Free Software Foundation; either version 2.1 of the License, or
(at your option) any later version. A copy of the license is contained
in the file COPYING, located in the root directory of this repository.

This software is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License contained in the file COPYING for more
details.
