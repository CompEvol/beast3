# Guide for migrating BEAST v2.7 packages to BEAST 3

## 0. Setting up a BEAST 3 project

* Install **JDK 25** from [Azul Zulu](https://www.azul.com/downloads/?package=jdk#zulu) or any JDK 25+ distribution. A bundled JavaFX JDK is no longer needed — JavaFX is resolved as a Maven dependency.
* Install **Maven 3.9+** from [maven.apache.org](https://maven.apache.org/) or via your package manager.

Clone and install beast3modular to your local Maven repository:

```bash
git clone git@github.com:alexeid/beast3modular.git
cd beast3modular
mvn install -DskipTests
```

This installs all BEAST 3 modules (including beagle and colt) into `~/.m2/repository/` so your package can depend on them.

A complete working example is available in the [beast-package-skeleton](https://github.com/CompEvol/beast-package-skeleton) repository — a ready-to-copy Maven project with a custom `ScalarDistribution`, operator, tests, and BEAST XML using the strongly-typed spec API.

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
| — | `beast.base.spec.inference.distribution.ChiSquare` | New; `df` (`RealScalar<PositiveReal>`) |
| — | `beast.base.spec.inference.distribution.InverseGamma` | New; `alpha`, `beta` (`RealScalar<PositiveReal>`) |
| — | `beast.base.spec.inference.distribution.Laplace` | New; `mu` (`RealScalar<Real>`); `scale` (`RealScalar<PositiveReal>`) |
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
