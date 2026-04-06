# Guide for migrating BEAST v2.7 packages to BEAST 3

## 0. Setting up a BEAST 3 project

* Install **JDK 25** from [Azul Zulu](https://www.azul.com/downloads/?package=jdk#zulu) or any JDK 25+ distribution. A bundled JavaFX JDK is no longer needed — JavaFX is resolved as a Maven dependency.
* Install **Maven 3.9+** from [maven.apache.org](https://maven.apache.org/) or via your package manager.

Clone and install BEAST 3 to your local Maven repository:

```bash
git clone git@github.com:CompEvol/beast3.git
cd beast3
mvn install -DskipTests
```

This installs all BEAST 3 modules into `~/.m2/repository/` so your package can depend on them. BEAGLE is resolved automatically from Maven Central.

A complete working example is available in the [beast-package-skeleton](https://github.com/CompEvol/beast-package-skeleton) repository — a ready-to-copy Maven project with a custom `ScalarDistribution`, operator, tests, and BEAST XML using the strongly-typed spec API.

## 1. Make sure your package compiles

If your external package uses Ant, include the following JARs on the classpath:
  * beagle.jar
  * antlr-runtime-4.13.2.jar
  * commons-math4-legacy + other commons JARs (commons-numbers, commons-rng, commons-statistics)

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

## Package layout: single module vs core + fx

If your package includes BEAUti input editors or other GUI components, you
need to decide how to organise the GUI code relative to the core logic.

### Option A: single module with optional GUI (recommended for most packages)

Keep everything in one Maven artifact and one JPMS module. Declare the GUI
dependencies as `static` so the module loads without JavaFX (headless/cluster
runs):

```java
open module my.beast.package {
    requires beast.pkgmgmt;
    requires beast.base;
    requires static beast.fx;         // optional at runtime
    requires static javafx.controls;  // optional at runtime

    exports my.beast.package;
    exports my.beast.package.app.beauti;

    provides beast.base.core.BEASTInterface with
        my.beast.package.MyModel,
        my.beast.package.MyOperator,
        my.beast.package.app.beauti.MyInputEditor;
}
```

**Convention:** place GUI classes in a `*.app.beauti` subpackage.

When running headless, the module loads normally. BEAUti provider classes are
registered by name but never instantiated, so the missing GUI dependencies
cause no errors. When running with BEAUti, everything works as expected.

### Option B: two modules (core + fx)

Split into a parent POM with two submodules. The core module has no JavaFX
dependency; the fx module declares `requires beast.fx;` and
`requires javafx.controls;` as regular dependencies. This is the pattern
used by beast3 itself (`beast-base` + `beast-fx`) and by
[morph-models](https://github.com/CompEvol/morph-models).

Use this when your package has substantial GUI code (multiple custom input
editors, complex BEAUti panels) that warrants its own module. The trade-off
is doubling the number of IDE projects per package.

## Publishing to Maven Central

BEAST 3 packages can be distributed as plain Maven Central JARs in addition to
(or instead of) ZIP archives submitted to CBAN. This lets users install your
package with a single Maven coordinate (e.g. `io.github.compevol:beast-morph-models:1.3.0`)
via BEAUti's **Install from Maven** button or the command line.

Requirements for Maven Central distribution:

1. Your `pom.xml` must include Maven Central metadata (`<url>`, `<licenses>`,
   `<developers>`, `<scm>`) and a `release` profile with source, javadoc, GPG
   signing, and the `central-publishing-maven-plugin`
2. Your JAR must embed `version.xml` at the root (via `maven-resources-plugin`)
   so that `PackageManager.parseServicesFromJar()` can discover the package's
   service providers
3. Your `groupId` must be a verified namespace on
   [central.sonatype.com](https://central.sonatype.com/)

The [beast-package-skeleton](https://github.com/CompEvol/beast-package-skeleton)
template includes all of this pre-configured. See its README for full setup
and deployment instructions.

## 2. Migrate package to use strongly typed classes

Your IDE should show where you are using deprecated classes (e.g. shown as line through in Eclipse). Identify where this happens by navigating your code.

Wherever classes are deprecated, there will be an alternative in the `beast.base.spec` hierarchy.
Links can be found in deprecated classes, for example for RealParameter there is a comment stating

``` * @deprecated use {@link RealScalarParam} or {@link RealVectorParam} ```

**Important:** The strongly-typed spec parameters (`RealScalarParam`, `IntVectorParam`, etc.) are only for model parameters that are (or could be) estimated during MCMC sampling. Configuration inputs that are not subject to MCMC — such as dimension counts, category counts, boolean flags, or initializer values — should remain as plain `Input<Integer>`, `Input<Double>`, etc. For example, a `stateNumber` input that configures the size of a rate matrix is not an MCMC parameter and should stay as `Input<Integer>`.

For your package
* consider using a `<yourpackage>.spec` package if you want to be backward compatible
* option mostly for packages that have other packages depending on them
* for `stand alone` packages, just replace the Parameter and Function Inputs with typed versions

### Class mapping: Legacy → Spec

#### Parameters

| Legacy Class | Spec Class | Key Changes |
|---|---|---|
| `RealParameter` (scalar) | `beast.base.spec.inference.parameter.RealScalarParam` | `lower`/`upper` replaced by `domain` (e.g. `PositiveReal.INSTANCE`, `UnitInterval.INSTANCE`); no `dimension` |
| `RealParameter` (vector) | `beast.base.spec.inference.parameter.RealVectorParam` | `lower`/`upper` replaced by `domain`; `dimension` retained |
| `IntegerParameter` (scalar) | `beast.base.spec.inference.parameter.IntScalarParam` | `lower`/`upper` replaced by `domain` (e.g. `PositiveInt.INSTANCE`, `NonNegativeInt.INSTANCE`) |
| `IntegerParameter` (vector) | `beast.base.spec.inference.parameter.IntVectorParam` | `lower`/`upper` replaced by `domain`; `dimension` retained |
| `BooleanParameter` (scalar) | `beast.base.spec.inference.parameter.BoolScalarParam` | Domain fixed to `Bool.INSTANCE` |
| `BooleanParameter` (vector) | `beast.base.spec.inference.parameter.BoolVectorParam` | Domain fixed to `Bool.INSTANCE` |
| `RealParameter` (simplex) | `beast.base.spec.inference.parameter.SimplexParam` | Extends `RealVectorParam<UnitInterval>` |

#### Domain types (replace explicit `lower`/`upper` bounds)

| Legacy Pattern | Spec Domain |
|---|---|
| `RealParameter lower="0"` | `PositiveReal.INSTANCE` |
| `RealParameter lower="0" upper="1"` | `UnitInterval.INSTANCE` |
| `RealParameter` (unbounded) | `Real.INSTANCE` |
| `IntegerParameter lower="1"` | `PositiveInt.INSTANCE` |
| `IntegerParameter lower="0"` | `NonNegativeInt.INSTANCE` |
| `IntegerParameter` (unbounded) | `Int.INSTANCE` |

#### Distributions / Priors

**Key architectural change:** In legacy BEAST, a prior is `Prior(x=parameter, distr=distribution)`. In the spec system, **the distribution IS the prior** — each distribution has a `param` input directly (inherited from `TensorDistribution`). No separate `Prior` wrapper is needed.

| Legacy Pattern | Spec Class | Key Changes |
|---|---|---|
| `Prior` + `Normal` | `beast.base.spec.inference.distribution.Normal` | `param` replaces `Prior.x`; `mean` → `RealScalar<Real>`; `sigma` → `RealScalar<PositiveReal>` |
| `Prior` + `LogNormalDistributionModel` | `beast.base.spec.inference.distribution.LogNormal` | `param`; `M` → `RealScalar<Real>`; `S` → `RealScalar<PositiveReal>` |
| `Prior` + `Gamma` | `beast.base.spec.inference.distribution.Gamma` | `param`; `alpha` → `RealScalar<PositiveReal>`; `theta` (scale) XOR `lambda` (rate) |
| — | `beast.base.spec.inference.distribution.GammaMean` | New mean parameterization: `alpha` + `mean` |
| `Prior` + `Exponential` | `beast.base.spec.inference.distribution.Exponential` | `param`; `mean` → `RealScalar<PositiveReal>` |
| `Prior` + `Beta` | `beast.base.spec.inference.distribution.Beta` | `param` (must be `RealScalar<UnitInterval>`); `alpha`, `beta` → `RealScalar<PositiveReal>` |
| `Prior` + `Uniform` | `beast.base.spec.inference.distribution.Uniform` | `param`; `lower`, `upper` → `RealScalar<Real>` |
| `Prior` + `Dirichlet` | `beast.base.spec.inference.distribution.Dirichlet` | `param` (must be `Simplex`); `alpha` → `RealVector<PositiveReal>` |
| `Prior` + `Poisson` | `beast.base.spec.inference.distribution.Poisson` | `param` → `IntScalar<NonNegativeInt>`; `lambda` → `RealScalar<NonNegativeReal>` |
| — | `beast.base.spec.inference.distribution.Cauchy` | New; `location` (`RealScalar<Real>`); `scale` (`RealScalar<PositiveReal>`) |
| `Prior` + `ChiSquare` | `beast.base.spec.inference.distribution.ChiSquare` | `df` (`RealScalar<PositiveReal>`) |
| `Prior` + `InverseGamma` | `beast.base.spec.inference.distribution.InverseGamma` | `alpha`, `beta` (`RealScalar<PositiveReal>`) |
| `Prior` + `LaplaceDistribution` | `beast.base.spec.inference.distribution.Laplace` | `mu` (`RealScalar<Real>`); `scale` (`RealScalar<PositiveReal>`) |
| — | `beast.base.spec.inference.distribution.Bernoulli` | New; `param` (`BoolScalar`); `p` (`RealScalar<UnitInterval>`) |
| — | `beast.base.spec.inference.distribution.IntUniform` | New discrete uniform; `lower`, `upper` (`IntScalar<Int>`) |
| `Prior` applying dist to vector elements | `beast.base.spec.inference.distribution.IID` | Applies a `ScalarDistribution` to each element of a `Vector` independently |
| Offset-prior pattern | `beast.base.spec.inference.distribution.OffsetReal` | `distribution` + `offset` (`RealScalar<Real>`) |
| — | `beast.base.spec.inference.distribution.TruncatedReal` | `distribution` + `lower`/`upper` (`RealScalar<Real>`) |

#### Substitution Models

| Legacy Class | Spec Class | Key Changes |
|---|---|---|
| `SubstitutionModel.Base` | `beast.base.spec.evolution.substitutionmodel.Base` | `frequencies` input uses spec `Frequencies` |
| `GeneralSubstitutionModel` | `beast.base.spec.evolution.substitutionmodel.BasicGeneralSubstitutionModel` | — |
| `Frequencies` | `beast.base.spec.evolution.substitutionmodel.Frequencies` | `frequencies` now takes `Simplex` (was `RealParameter`) |
| `HKY` | `beast.base.spec.evolution.substitutionmodel.HKY` | `kappa` → `RealScalar<PositiveReal>` |
| `GTR` | `beast.base.spec.evolution.substitutionmodel.GTR` | `rateAC`, `rateAG`, etc. → `RealScalar<PositiveReal>` |
| `TN93` | `beast.base.spec.evolution.substitutionmodel.TN93` | `kappa1`, `kappa2` → `RealScalar<PositiveReal>` |

#### Site Model

| Legacy Class | Spec Class | Key Changes |
|---|---|---|
| `SiteModel` | `beast.base.spec.evolution.sitemodel.SiteModel` | `mutationRate` → `RealScalar<PositiveReal>`; `shape` → `RealScalar<PositiveReal>`; `proportionInvariant` → `RealScalar<UnitInterval>` |

#### Branch Rate Models

| Legacy Class | Spec Class | Key Changes |
|---|---|---|
| `StrictClockModel` | `beast.base.spec.evolution.branchratemodel.StrictClockModel` | `clock.rate` → `RealScalar<PositiveReal>` |
| `UCRelaxedClockModel` | `beast.base.spec.evolution.branchratemodel.UCRelaxedClockModel` | `distr` → `ScalarDistribution`; `rateCategories` → `IntVector<NonNegativeInt>`; `rateQuantiles` → `RealVector<UnitInterval>` |
| `RandomLocalClockModel` | `beast.base.spec.evolution.branchratemodel.RandomLocalClockModel` | `indicators` → `BoolVector`; `rates` → `RealVector<PositiveReal>` |

#### Likelihood

| Legacy Class | Spec Class | Key Changes |
|---|---|---|
| `GenericTreeLikelihood` | `beast.base.spec.evolution.likelihood.GenericTreeLikelihood` | `branchRateModel` accepts spec `Base` |
| `TreeLikelihood` | `beast.base.spec.evolution.likelihood.TreeLikelihood` | Extends spec `GenericTreeLikelihood` |

#### Speciation Models

| Legacy Class | Spec Class | Key Changes |
|---|---|---|
| `YuleModel` | `beast.base.spec.evolution.speciation.YuleModel` | `birthDiffRate` → `RealScalar<PositiveReal>` |

#### Operators (Parameter)

| Legacy Class | Spec Class | Key Changes |
|---|---|---|
| `ScaleOperator` (parameter) | `beast.base.spec.inference.operator.ScaleOperator` | `parameter` now takes `Scalable` interface |
| `RealRandomWalkOperator` | `beast.base.spec.inference.operator.RealRandomWalkOperator` | `parameter` → `RealVectorParam` XOR `scalar` → `RealScalarParam` |
| `DeltaExchangeOperator` | `beast.base.spec.inference.operator.DeltaExchangeOperator` | Adds typed inputs: `rvparameter`, `ivparameter`, `rsparameter`, `isparameter` |
| `BitFlipOperator` | `beast.base.spec.inference.operator.BitFlipOperator` | `parameter` → `BoolVectorParam` |
| `SwapOperator` | `beast.base.spec.inference.operator.SwapOperator` | `parameter` → `RealVectorParam` XOR `intparameter` → `IntVectorParam` |
| `IntRandomWalkOperator` | `beast.base.spec.inference.operator.IntRandomWalkOperator` | `parameter` → `IntVectorParam` |
| `IntUniformOperator` | `beast.base.spec.inference.operator.uniform.IntUniformOperator` | `parameter` → `Tensor<? extends Int, Integer>` |

#### Operators (Tree)

| Legacy Class | Spec Class | Key Changes |
|---|---|---|
| `ScaleOperator` (tree mode) | `beast.base.spec.evolution.operator.ScaleTreeOperator` | Split from parameter ScaleOperator; `tree` required |
| `UpDownOperator` | `beast.base.spec.evolution.operator.UpDownOperator` | `up`/`down` take `List<Scalable>`; uses Bactrian kernel |
| — | `beast.base.spec.evolution.operator.IntervalScaleOperator` | New; scales intervals between node heights |

#### Loggers

| Legacy Class | Spec Class | Key Changes |
|---|---|---|
| `TreeWithMetaDataLogger` | `beast.base.spec.evolution.TreeWithMetaDataLogger` | — |
| `ESS` | `beast.base.spec.inference.util.ESS` | — |


## 3a. Update example XMLs

### Remove `<map>` blocks

The `Prior` wrapper class is no longer used. Remove all `<map>` entries:

```xml
<!-- Remove all of these -->
<map name="Uniform">beast.base.inference.distribution.Uniform</map>
<map name="prior">beast.base.inference.distribution.Prior</map>
<!-- etc. -->
```

### Update namespace

Add `beast.base.spec.*` packages to the namespace (listed first for priority over deprecated classes):

```xml
namespace="beast.base.spec.inference.parameter:beast.base.spec.inference.operator:
beast.base.spec.inference.distribution:beast.base.spec.evolution.tree.coalescent:
beast.base.spec.evolution.sitemodel:beast.base.spec.evolution.likelihood:
beast.base.spec.evolution.operator:beast.base.spec.evolution.substitutionmodel:
beast.base.spec.evolution.branchratemodel:beast.base.spec.domain:
beast.base.evolution.alignment:beast.base.evolution.tree:
beast.pkgmgmt:beast.base.core:beast.base.inference:
beast.base.inference.operator:beast.base.evolution.operator"
```

Also update `version="2.8"`.

### Replace parameters

Replace `lower`/`upper` attributes with `domain`:

```xml
<!-- Old -->
<parameter spec="parameter.RealParameter" value="1.0" lower="0.0" estimate="true"/>
<!-- New scalar -->
<parameter spec="RealScalarParam" domain="PositiveReal" value="1.0" estimate="true"/>
<!-- New vector -->
<parameter spec="RealVectorParam" domain="NonNegativeReal" value="1.0" estimate="true"/>
<!-- Boolean -->
<stateNode spec="BoolVectorParam" dimension="20">true</stateNode>
```

Replace `Function$Constant` with `RealScalarParam`:

```xml
<!-- Old -->
<mean spec="Function$Constant" value="1.0"/>
<!-- New -->
<mean spec="RealScalarParam" domain="PositiveReal" value="1.0" estimate="false"/>
```

### Replace priors with direct spec distributions

In the spec framework, the distribution IS the prior. The input name changes from `x` to `param`:

```xml
<!-- Old: Prior wrapper -->
<prior id="NePrior" name="distribution" x="@Ne">
    <Exponential name="distr">
        <mean spec="Function$Constant" value="1.0"/>
    </Exponential>
</prior>

<!-- New: direct spec distribution (scalar param) -->
<distribution id="clockPrior" spec="beast.base.spec.inference.distribution.Exponential"
              param="@clockRate">
    <mean spec="RealScalarParam" domain="PositiveReal" value="1.0" estimate="false"/>
</distribution>
```

### IID for vector priors

Spec distributions like `Exponential`, `Normal`, `LogNormal` are `ScalarDistribution` objects and only accept scalar params. For vector parameters, wrap with `IID`:

```xml
<!-- Vector param: wrap with IID -->
<distribution id="NePrior" spec="beast.base.spec.inference.distribution.IID"
              param="@Ne">
    <distr spec="beast.base.spec.inference.distribution.Exponential">
        <mean spec="RealScalarParam" domain="PositiveReal" value="1.0" estimate="false"/>
    </distr>
</distribution>
```

Without the `IID` wrapper, you will get: `ClassCastException: RealVectorParam cannot be cast to RealScalar`.

### Casting utilities for complex patterns

When a boolean indicator sum needs a Poisson prior, use `IntSum` and `AsIntScalar` (see `beast.base.spec.README.md` for details):

```xml
<distribution spec="beast.base.spec.inference.distribution.Poisson">
    <lambda spec="RealScalarParam" domain="PositiveReal" value="0.693" estimate="false"/>
    <param spec="beast.base.spec.inference.util.AsIntScalar" domain="NonNegativeInt">
        <arg spec="beast.base.spec.evolution.IntSum" arg="@indicators"/>
    </param>
</distribution>
```

### Replace `spec` attributes with spec class paths

Most beast-base classes have spec equivalents. Replace short names or old paths with spec paths:

| Old | New |
|-----|-----|
| `ConstantPopulation` | `beast.base.spec.evolution.tree.coalescent.ConstantPopulation` |
| `RandomTree` | `beast.base.spec.evolution.tree.coalescent.RandomTree` |
| `SiteModel` | `beast.base.spec.evolution.sitemodel.SiteModel` |
| `ThreadedTreeLikelihood` | `beast.base.spec.evolution.likelihood.ThreadedTreeLikelihood` |
| `JukesCantor` | `beast.base.spec.evolution.substitutionmodel.JukesCantor` |
| `HKY` | `beast.base.spec.evolution.substitutionmodel.HKY` |
| `beast.base.evolution.branchratemodel.StrictClockModel` | `beast.base.spec.evolution.branchratemodel.StrictClockModel` |
| `ScaleOperator` (parameter) | `beast.base.spec.inference.operator.ScaleOperator` |
| `operator.BitFlipOperator` | `beast.base.spec.inference.operator.BitFlipOperator` |
| `AdaptableOperatorSampler` | `beast.base.spec.evolution.operator.AdaptableOperatorSampler` |
| `kernel.AdaptableVarianceMultivariateNormalOperator` | `beast.base.spec.evolution.operator.AdaptableVarianceMultivariateNormalOperator` |
| `operator.kernel.BactrianUpDownOperator` | `beast.base.spec.evolution.operator.UpDownOperator` |
| `operator.kernel.Transform$LogTransform` | `beast.base.spec.inference.operator.Transform$LogTransform` |
| `operator.kernel.Transform$NoTransform` | `beast.base.spec.inference.operator.Transform$NoTransform` |
| `beast.base.evolution.Sum` | `beast.base.spec.evolution.Sum` (or `IntSum` for booleans) |

Classes that do NOT have spec equivalents (keep as-is): `MCMC`, `State`, `CompoundDistribution`, `Logger`, `OperatorSchedule`, `Exchange`, `WilsonBalding`, `EpochFlexOperator`, `TreeStretchOperator`, `kernel.BactrianScaleOperator` (tree mode with `rootOnly`), `kernel.BactrianNodeOperator`, `kernel.BactrianSubtreeSlide`.

### Function vs Tensor incompatibility

Spec types (`RealScalarParam`, `RealVectorParam`, etc.) implement `Tensor`, NOT `Function`. Old beast-base classes that expect `Function` inputs will NOT accept spec types. You must use spec versions of beast-base classes throughout.

In AVMN transforms, `Transform$NoTransform` takes `Tensor` for its `f` input, but `Tree` is not a `Tensor`. Remove tree references from `NoTransform` entries:

```xml
<!-- Old -->
<transformations spec="operator.kernel.Transform$NoTransform">
    <f idref="Tree.t:$(n)"/>
</transformations>
<!-- New: self-closing (tree passed via AdaptableOperatorSampler.tree input) -->
<transformations spec="beast.base.spec.inference.operator.Transform$NoTransform"/>
```

### Replace tree operator sets

Old set:
```xml
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

New set:
```xml
<operator id="$(m)TreeScaler.t:$(n)" spec="beast.base.spec.evolution.operator.UpDownOperator" up="@Tree.t:$(n)" weight="4.0" scaleFactor="0.1"/>
<operator id='$(m)TreeRootScaler.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianScaleOperator' scaleFactor="0.1" weight="3" tree="@Tree.t:$(n)" rootOnly='true'/>
<operator id='$(m)UniformOperator.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianNodeOperator' weight="30" tree="@Tree.t:$(n)"/>
<operator id='$(m)SubtreeSlide.t:$(n)' spec='beast.base.evolution.operator.kernel.BactrianSubtreeSlide' weight="15" size="1.0" tree="@Tree.t:$(n)"/>
<operator id='$(m)Narrow.t:$(n)' spec='Exchange' isNarrow='true' weight="15" tree="@Tree.t:$(n)"/>
<operator id='$(m)Wide.t:$(n)' spec='Exchange' isNarrow='false' weight="3" tree="@Tree.t:$(n)"/>
<operator id='$(m)WilsonBalding.t:$(n)' spec='WilsonBalding' weight="3" tree="@Tree.t:$(n)"/>
```

## 3b. Update BEAUti templates & input editors

### FXTemplate parameter declarations

Replace deprecated parameter types with spec types using `domain`:

```xml
<!-- Old -->
<param id="Ne.t:$(n)" spec="beast.base.inference.parameter.RealParameter"
       value="1.0" lower="0.0" estimate="true"/>
<!-- New vector -->
<param id="Ne.t:$(n)" spec="RealVectorParam"
       domain="NonNegativeReal" value="1.0" estimate="true"/>
<!-- New scalar (e.g. clock rate) -->
<param id="clock.t:$(n)" spec="RealScalarParam"
       domain="PositiveReal" value="1.0" estimate="true"/>
<!-- Boolean indicators -->
<param id="indicators.t:$(n)" spec="BoolVectorParam"
       value="true" estimate="true"/>
```

### InputEditor no-arg constructors

JPMS `provides` declarations require service implementations to have a public no-arg constructor. `InputEditor.Base` now provides one, but your subclasses must also have one if they define a `(BeautiDoc doc)` constructor (which suppresses the default):

```java
public class MyInputEditor extends InputEditor.Base {
    public MyInputEditor() { super(); }           // required for JPMS
    public MyInputEditor(BeautiDoc doc) { super(doc); }
    // ...
}
```

### Computed quantities as spec types

If your package has utility classes that compute derived quantities (e.g. differences between vector elements, mean of a vector), these previously implemented `Function`. In beast3, spec distributions expect `Tensor` types, not `Function`.

Make these classes implement the appropriate spec type interface:

| Output | Implement | Required methods |
|--------|-----------|-----------------|
| Vector (N values) | `RealVector<Real>` | `getDomain()`, `size()`, `get(int i)`, `getElements()` |
| Scalar (1 value) | `RealScalar<Real>` | `getDomain()`, `get()` |

Example:
```java
public class Difference extends CalculationNode implements RealVector<Real> {
    final public Input<RealVector<? extends Real>> argInput = new Input<>("arg", "...");

    @Override public Real getDomain() { return Real.INSTANCE; }
    @Override public int size() { return values.length; }
    @Override public double get(int i) { return values[i]; }
    @Override public List<Double> getElements() {
        return Arrays.stream(values).boxed().collect(Collectors.toList());
    }
}
```

Then in XML, vector computed quantities use `IID` wrapping, scalar ones use distributions directly:
```xml
<!-- Vector -> IID+Normal -->
<distribution spec="beast.base.spec.inference.distribution.IID">
    <param spec="mypackage.Difference" arg="@myVector"/>
    <distr spec="beast.base.spec.inference.distribution.Normal">...</distr>
</distribution>
<!-- Scalar -> Normal directly -->
<distribution spec="beast.base.spec.inference.distribution.Normal">
    <param spec="mypackage.First" arg="@myVector"/>
    ...
</distribution>
```

### Other code migration notes

* `RealScalarParam` does not have `isDirty(int)`. Use `somethingIsDirty()` instead.
* `size()` replaces `getDimension()` on spec vector types.
* `get(i)` / `set(i, v)` replace `getValue(i)` / `setValue(i, v)`.
* `get()` replaces `getValue()` / `getArrayValue()` on scalar types.
* `BoolVectorParam` does not implement `Function`. Use `get(i)` (returns `boolean`) instead of `getArrayValue(i)`.
* Replace operator sets for Trees with new operator sets (see Section 3a).
* Update `InputEditor` implementations to deal with typed inputs.
* When `StateNode` is needed as a generic type (e.g. in BEAUti code that handles both old and new parameters), cast to `StateNode` rather than `RealParameter`. The `isEstimatedInput` field is on `StateNode`.
