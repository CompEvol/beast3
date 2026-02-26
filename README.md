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
- **JPMS modules** — the codebase is split into `beast.pkgmgmt` and `beast.base` Java modules with explicit `module-info.java` descriptors.
- **Java 25** — requires JDK 25 or later.
- **Strongly typed inputs** — new `beast.base.spec` hierarchy replaces loosely-typed parameters with compile-time-checked typed inputs.
- **External packages** — discovered via `module-info.java` `provides` declarations (primary) or `version.xml` service entries (for legacy/non-modular JARs). Deployed packages are loaded into a JPMS `ModuleLayer` per package.

Project Structure
-----------------

```
beast3modular/            (parent POM)
├── beast-pkgmgmt/        (package manager module)
├── beast-base/           (core BEAST module)
└── lib/                  (beagle.jar, colt.jar)
```

Building
--------

### Prerequisites

- **Java 25** — install from [Azul Zulu](https://www.azul.com/downloads/?package=jdk#zulu) or any JDK 25+ distribution.
- **Maven 3.9+** — install from [maven.apache.org](https://maven.apache.org/) or via your package manager.

### One-time setup: install local JARs

Two dependencies (`beagle.jar` and `colt.jar`) are not in Maven Central. Install them to your local repository:

```bash
mvn install:install-file -Dfile=lib/beagle.jar -DgroupId=beast -DartifactId=beagle -Dversion=1.0 -Dpackaging=jar
mvn install:install-file -Dfile=lib/colt.jar -DgroupId=beast -DartifactId=colt -Dversion=1.0 -Dpackaging=jar
```

### Compile

```bash
mvn compile
```

### Test

```bash
mvn test
```

Running
-------

### From the command line

Build the project, then use the `exec-maven-plugin` to launch BEAST applications with the correct module path:

```bash
mvn package -DskipTests

# Run BEAST on an XML file
mvn -pl beast-base exec:exec -Dbeast.args="example.xml"

# Run BEAUti
mvn -pl beast-base exec:exec -Dbeast.main=beastfx.app.beauti.Beauti

# Run other tools (LogCombiner, TreeAnnotator, etc.)
mvn -pl beast-base exec:exec -Dbeast.main=beastfx.app.tools.LogCombiner
mvn -pl beast-base exec:exec -Dbeast.main=beastfx.app.tools.TreeAnnotator
```

The `-Dbeast.main=` property selects the main class (defaults to `beastfx.app.beast.BeastMain`). The `-Dbeast.args=` property passes arguments to the application.

### From IntelliJ

See `scripts/DevGuideIntelliJ.md`. IntelliJ resolves the full module path from Maven automatically.

Using BEAST 3 as a library
--------------------------

Projects like LPhyBEAST can use BEAST 3 as a Maven dependency to build BEAST models in memory and write them to XML.

### 1. Install BEAST 3 to your local Maven repository

In the `beast3modular` directory:

```bash
mvn install -DskipTests
```

### 2. Add the dependency to your project's `pom.xml`

```xml
<dependency>
    <groupId>beast</groupId>
    <artifactId>beast-base</artifactId>
    <version>2.8.0-SNAPSHOT</version>
</dependency>
```

Maven resolves all transitive dependencies (JavaFX, Commons Math, ANTLR, etc.) automatically. The consuming project also needs `beagle.jar` and `colt.jar` installed in its local repository (see One-time setup above).

### 3. JPMS module declaration (if your project uses modules)

```java
open module my.project {
    requires beast.base;
    // requires beast.pkgmgmt;  // only if you use package management APIs
}
```

If your project does not use JPMS modules, the BEAST classes are accessible from the unnamed module without any extra configuration.

Development
-----------

See `scripts/DevGuideIntelliJ.md` for IntelliJ IDEA setup instructions, including how to develop an external BEAST package alongside BEAST 3 core in a single IDE session.

For guidance on migrating external packages, see:
- `scripts/migrate.md` — migrating from BEAST v2.6 to v2.7
- `scripts/migration-guide.md` — migrating from BEAST v2.7 to v3

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
