# Guide for migrating BEAST v2.7 packages to BEAST 3

## 0. Setting up a BEAST 3 project

* Clone the repository:
  ```
  git clone git@github.com:alexeid/beast3modular.git
  ```
* Install **JDK 25** from [Azul Zulu](https://www.azul.com/downloads/?package=jdk#zulu) or any JDK 25+ distribution. A bundled JavaFX JDK is no longer needed — JavaFX is resolved as a Maven dependency.
* Install **Maven 3.9+** from [maven.apache.org](https://maven.apache.org/) or via your package manager.
* One-time setup — install local JARs that are not in Maven Central:
  ```bash
  mvn install:install-file -Dfile=lib/beagle.jar -DgroupId=beagle -DartifactId=beagle -Dversion=4.0.1 -Dpackaging=jar
  mvn install:install-file -Dfile=lib/colt.jar -DgroupId=colt -DartifactId=colt -Dversion=1.2.0 -Dpackaging=jar
  ```
* Build:
  ```bash
  mvn compile
  ```

## 1. Make sure your package compiles

If your external package uses Ant, include the following JARs on the classpath:
  * beagle.jar
  * colt.jar
  * antlr-runtime-4.13.2.jar
  * commons-math3-3.6.1.jar + other commons JARs (commons-numbers, commons-rng, commons-statistics)

If your package uses Maven, add BEAST 3 modules as dependencies in your `pom.xml`.

Some API changes to keep in mind:
* StateNode is not a Function any more — plan is to use Typed version to produce values, but making your class implement Function works as intermediate step
* StateNode does not have a `scale` method any more — implement the `Scalable` interface if you want your StateNode to scale
* `beast.base.inference.Evaluator` removed — impacts MCMC derived classes
* `beast.base.evolution.alignment.AscertainedAlignment` removed — use standard Alignment instead

## JPMS modules

BEAST 3 core is split into two JPMS modules: `beast.pkgmgmt` and `beast.base`. Both are declared as `open module` in their `module-info.java`.

**For packages under active development** (recommended):

Add a `module-info.java` to your package with `provides` declarations for your service implementations.  This is the primary service discovery mechanism — BEAST scans module descriptors in the boot layer and plugin layers.  When you open both BEAST 3 and your package in IntelliJ, the IDE places both on the module path, and BEAST discovers your services automatically with no extra configuration.

Example `module-info.java` for an external package:

```java
open module my.beast.package {
    requires beast.pkgmgmt;
    requires beast.base;

    provides beast.base.core.BEASTInterface with
        my.beast.package.MyModel,
        my.beast.package.MyOperator;
}
```

**For deployed packages** (installed via Package Manager):

Deployed package JARs are loaded at runtime into a child `ModuleLayer` per package.  Both `module-info.java` `provides` declarations and `version.xml` service entries are discovered.  If your package JAR has no `module-info.java`, it is treated as an automatic module and services are registered from `version.xml` as before.

## 2. Migrate package to use strongly typed classes

Your IDE should show where you are using deprecated classes (e.g. shown as line through in Eclipse). Identify where this happens by navigating your code.

Wherever classes are deprecated, there will be an alternative in the `beast.base.spec` hierarchy.
Links can be found in deprecated classes, for example for RealParameter there is a comment stating

``` * @deprecated use {@link RealScalarParam} or {@link RealVectorParam} ```

For your package
* consider using a `<yourpackage>.spec` package if you want to be backward compatible
* option mostly for packages that have other packages depending on them
* for `stand alone` packages, just replace the Parameter and Function Inputs with typed versions


## 3a. Update example XMLs

* Replace all `XXXParameters` with appropriate types `XXXParams`
* Replace all `spec` attributes with the appropriate classes (mostly insert '.spec')
* Replace operator sets for Trees with new operator sets

Old set:
```
<operator id="$(m)BICEPSEpochTop.t:$(n)" spec="beast.base.evolution.operator.EpochFlexOperator" tree="@Tree.t:$(n)" weight="2.0" scaleFactor="0.1"/>
<operator id="$(m)BICEPSEpochAll.t:$(n)" spec="beast.base.evolution.operator.EpochFlexOperator" tree="@Tree.t:$(n)" weight="2.0" scaleFactor="0.1" fromOldestTipOnly="false"/>
<operator id="$(m)BICEPSTreeFlex.t:$(n)" spec="beast.base.evolution.operator.TreeStretchOperator" scaleFactor="0.01" tree="@Tree.t:$(n)" weight="2.0"/>
<operator id='$(m)TreeRootScaler.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianScaleOperator' scaleFactor="0.1" weight="3" tree="@Tree.t:$(n)" rootOnly='true'/>
<operator id='$(m)UniformOperator.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianNodeOperator' weight="30" tree="@Tree.t:$(n)"/>
<operator id='$(m)SubtreeSlide.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianSubtreeSlide' weight="15" size="1.0" tree="@Tree.t:$(n)"/>
<operator id='$(m)Narrow.t:$(n)' spec='Exchange' isNarrow='true' weight="15" tree="@Tree.t:$(n)"/>
<operator id='$(m)Wide.t:$(n)' spec='Exchange' isNarrow='false' weight="3" tree="@Tree.t:$(n)"/>
<operator id='$(m)WilsonBalding.t:$(n)' spec='WilsonBalding' weight="3" tree="@Tree.t:$(n)"/>
```

New set
```
<operator id="$(m)TreeScaler.t:$(n)" spec="beast.base.spec.evolution.operator.UpDownOperator" up="@Tree.t:$(n)" weight="4.0" scaleFactor="0.1"/>
<operator id='$(m)TreeRootScaler.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianScaleOperator' scaleFactor="0.1" weight="3" tree="@Tree.t:$(n)" rootOnly='true'/>
<operator id='$(m)UniformOperator.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianNodeOperator' weight="30" tree="@Tree.t:$(n)"/>
<operator id='$(m)SubtreeSlide.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianSubtreeSlide' weight="15" size="1.0" tree="@Tree.t:$(n)"/>
<operator id='$(m)Narrow.t:$(n)' spec='Exchange' isNarrow='true' weight="15" tree="@Tree.t:$(n)"/>
<operator id='$(m)Wide.t:$(n)' spec='Exchange' isNarrow='false' weight="3" tree="@Tree.t:$(n)"/>
<operator id='$(m)WilsonBalding.t:$(n)' spec='WilsonBalding' weight="3" tree="@Tree.t:$(n)"/>
```

## 3b. Update BEAUti templates & input editors

* Replace all `XXXParameters` with appropriate types `XXXParams`
* Replace all `spec` attributes with the appropriate classes (mostly insert '.spec')
* Replace operator sets for Trees with new operator sets (see above)
* Update `InputEditor` implementations to deal with typed inputs
* Some BEAUti InputEditors have changed signatures
