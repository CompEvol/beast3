# BEAST 3 Changelog (vs BEAST 2)

This is a high-level summary of what changed between BEAST 2 and BEAST 3 and why.
For class-by-class migration mappings and step-by-step porting instructions, see
[`scripts/migration-guide.md`](scripts/migration-guide.md).

---

## Build & Tooling

- **Ant replaced by Maven** — multi-module POM (`beast-pkgmgmt`, `beast-base`, `beast-fx`). Dependencies are declared in `pom.xml` and resolved automatically; no manual JAR management.
- **Java 25 required** — target release is Java 25 (was Java 8 / 11 in BEAST 2). A bundled JavaFX JDK is no longer needed.
- **JPMS modules** — each subproject has a `module-info.java`. The core inference engine (`beast.base`) has no JavaFX dependency and can run headlessly; the GUI (`beast.fx`) is a separate module. All three modules are declared as `open module` for reflection-based XML unmarshalling.
- **Project structure** — `beast3/` root contains `beast-pkgmgmt/`, `beast-base/`, `beast-fx/`, and `lib/` (local modular JARs for `beagle` and `colt`).

## Strongly Typed Spec System

- **New `beast.base.spec` hierarchy** — replaces loosely-typed `Input<RealParameter>` / `Input<Function>` with compile-time-checked typed inputs (e.g., `Input<RealScalar<PositiveReal>>`).
- **Scalar / Vector / Tensor parameter types** — `RealScalarParam`, `RealVectorParam`, `IntScalarParam`, `IntVectorParam`, `BoolScalarParam`, `BoolVectorParam`, `SimplexParam` replace the old `RealParameter`, `IntegerParameter`, `BooleanParameter`.
- **Domain types replace explicit bounds** — `PositiveReal`, `UnitInterval`, `Real`, `PositiveInt`, `NonNegativeInt`, etc. replace `lower="0"` / `upper="1"` XML attributes. Domains propagate through the model graph at initialization time.
- **"Distribution IS the prior" pattern** — in BEAST 2, a prior is `Prior(x=parameter, distr=distribution)`. In BEAST 3, each distribution has a `param` input directly (inherited from `TensorDistribution`); no separate `Prior` wrapper needed.
- **Spec classes coexist with legacy** — deprecated legacy classes remain, linking to their spec replacements. Packages can migrate incrementally.

## New Distributions & Operators

Genuinely new components not present in BEAST 2:

**Distributions:**
- `Cauchy` — location/scale parameterization
- `Laplace` — mu/scale parameterization
- `InverseGamma` — alpha/beta parameterization
- `ChiSquare` — degrees-of-freedom parameterization
- `Bernoulli` — discrete boolean distribution with probability `p`
- `IntUniform` — discrete uniform over integer range
- `GammaMean` — mean parameterization of Gamma (alpha + mean, alternative to alpha + scale/rate)
- `IID` — applies a `ScalarDistribution` independently to each element of a vector parameter
- `OffsetReal` — wraps a distribution with an additive offset
- `TruncatedReal` — wraps a distribution with lower/upper truncation bounds

**Operators:**
- `IntervalScaleOperator` — scales intervals between consecutive node heights (preserves topology, changes relative branch lengths)
- `ScaleTreeOperator` — split from the old `ScaleOperator`; dedicated tree-scaling operator
- `UpDownOperator` (spec version) — `up`/`down` take `List<Scalable>`; uses Bactrian kernel by default

**Default tree operator set simplified** — BICEPS `EpochFlexOperator` / `TreeStretchOperator` replaced by spec `UpDownOperator` as the tree scaler. The rest of the set (BactrianNodeOperator, BactrianSubtreeSlide, Exchange, WilsonBalding) is unchanged.

## API Removals & Breaking Changes

- **`StateNode` is no longer a `Function`** — classes that relied on `StateNode` implementing `Function` must either use the typed spec parameter or explicitly implement `Function`.
- **`StateNode.scale()` removed** — implement the `Scalable` interface instead if your `StateNode` needs to support scaling.
- **`Evaluator` removed** — `beast.base.inference.Evaluator` is gone; impacts MCMC-derived classes that used it for custom acceptance logic.
- **`AscertainedAlignment` removed** — use the standard `Alignment` class instead.
- **Operator input types narrowed** — spec operators accept typed parameters (e.g., `RealVectorParam` instead of `RealParameter`), `Scalable` instead of raw `StateNode`.
- **`DeltaExchangeOperator`** — adds typed inputs (`rvparameter`, `ivparameter`, `rsparameter`, `isparameter`) alongside the legacy untyped input.

## Package System

- **`module-info.java` service discovery** (primary) — packages declare `provides beast.base.core.BEASTInterface with ...` in their module descriptor. BEAST scans module descriptors in the boot layer and plugin layers at startup.
- **`version.xml` service discovery** (fallback) — still supported for non-modular JARs. Deployed JARs without `module-info` are treated as automatic modules.
- **Plugin `ModuleLayer` isolation** — each deployed package is loaded into its own JPMS `ModuleLayer`, preventing classpath conflicts between packages.
- **Maven Central distribution** — packages can be published as plain Maven Central JARs. Users install via BEAUti's "Install from Maven" button or `packagemanager -maven groupId:artifactId:version`. Resolution uses Apache Maven Resolver; JARs are cached in `~/.beast/2.8/maven-repo/`.
- **Custom Maven repositories** — `packagemanager -addMavenRepository <url>` for organization-hosted repositories.
- **`maven-packages.xml`** — new config file (alongside `beauti.cfg`) tracks installed Maven packages.

## Dependency Updates

| Dependency | BEAST 2 | BEAST 3 |
|---|---|---|
| Java | 8 / 11 | 25 |
| Build system | Ant | Maven 3.9+ |
| Math library | commons-math3 3.6.1 | commons-math4-legacy 4.0-beta1, commons-numbers 1.2, commons-rng 1.6, commons-statistics 1.2 |
| ANTLR | 4.x (bundled) | antlr4-runtime 4.13.2 (Maven dependency) |
| JavaFX | Bundled with JDK | 25.0.2 (Maven dependency) |
| JUnit | JUnit 4 | JUnit 5 (Jupiter 5.8.2), JUnit 4 retained for compatibility |
| GUI testing | — | TestFX 4.0.18 + openjfx-monocle 21.0.2 (headless) |
| Maven Resolver | — | maven-resolver-supplier-mvn4 2.0.16 (for package manager) |

## BEAUti & Templates

- **New default tree operator set** — spec `UpDownOperator` replaces BICEPS epoch operators as the tree scaler in BEAUti-generated XML.
- **fxtemplates namespacing** — `beast-fx` templates moved to `beast.fx/fxtemplates/` (module-unique resource path) to avoid JPMS split-package conflicts. External packages use their own namespace (e.g., `beast.morph.models.fx/fxtemplates/`).
- **Module resource scanning** — `BeautiDoc.processTemplate()` scans JPMS module resources in addition to filesystem directories, enabling templates to be discovered from JARs on the module path.
- **"Install from Maven" UI** — new button in the BEAUti package manager dialog for installing packages by Maven coordinates.

## Testing

- **JUnit 5** — primary test framework (JUnit Jupiter 5.8.2). JUnit 4 tests still compile via the vintage engine.
- **TestFX headless testing** — BEAUti GUI tests run headlessly via Monocle Glass platform (`openjfx-monocle`). No display server needed.
- **Slow-test profile** — operator and BEAUti tests that run long MCMC chains (1M–11M iterations) are tagged `@Tag("slow")` and excluded from the default build. Run with `mvn test -Pslow-tests`.
