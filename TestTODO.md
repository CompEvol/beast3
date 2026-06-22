# beast-base Test Migration TODO

Old tests live under `src/test/java/test/beast/` (package `test.beast.*`).  
New/spec tests live under `src/test/java/beast/base/spec/` (package `beast.base.spec.*`).

The goal is to retire every old test whose primary subject class is `@Deprecated`,
once a spec equivalent exists and provides equivalent coverage.

---

## Summary Table

### Module Overview

`beast-base` is a special case: it has both an old test tree (`test.beast.*`) and a new spec tree (`beast.base.spec.*`). Spec tests are all new. Old tests that do not target deprecated classes are not considered "old" — they are current and require no action. `beast-fx` and `beast-pkgmgmt` have no `spec` package and none of their tests target deprecated classes, so all their tests are current.

| Module | Total files | Spec (new) | No migration required | Old: deprecated — covered ✅+⚠️ | Old: deprecated — ❌ missing | Old: deprecated — 💤 disabled |
|---|---|---|---|---|---|---|
| beast-base | 147 | 70 | 35 | 35 | 4 | 3 |
| beast-fx | 16 | — | 16 | — | — | — |
| beast-pkgmgmt | 9 | — | 9 | — | — | — |
| **Total** | **172** | **70** | **60** | **35** | **4** | **3** |

- **Spec (new):** `beast.base.spec.*` test files — all freshly written, no migration needed.
- **No migration required:** tests in old package that need no action — either the subject class is not deprecated, or the test already uses the spec API directly (already migrated).
- **Covered ✅+⚠️:** old tests on deprecated classes where a spec replacement already exists (directly ✅, or via new-API coverage ⚠️). The old test is a candidate for deletion once the spec test is verified in CI.
- **❌ missing:** old tests on deprecated classes with no spec equivalent yet — a new spec test must be written.
- **💤 disabled:** old tests whose `@Test` methods are all commented out; decide whether to migrate or delete.

---

### beast-base Old Test Migration Detail

The 42 old tests in `test.beast.*` that target deprecated production classes, split by priority.

#### ❌ Missing spec replacement — 4 tests, action required

Write a new spec test for each of these before deleting the old test.

| Old test class | Deprecated class under test | Note |
|---|---|---|
| `test.beast.beast2vs1.StarBEASTTest` | `test.beast.beast2vs1.TestFramework` (@Deprecated) | Create `beast.base.spec.beast2vs1.StarBEASTTest` |
| `test.beast.beast2vs1.TaxonOrderTest` | `test.beast.beast2vs1.TestFramework` (@Deprecated) | Create `beast.base.spec.beast2vs1.TaxonOrderTest` |
| `test.beast.evolution.alignment.FilteredAlignmentTest` | `FilteredAlignment` (@Deprecated) | Create `beast.base.spec.evolution.alignment.FilteredAlignmentTest` |
| `test.beast.util.ClusterTreeTest` | `ClusterTree` (@Deprecated) | Create `beast.base.spec.evolution.tree.ClusterTreeTest` once new starting-tree API exists |

#### ⚠️ Covered by new-API tests — 7 tests, verify then delete

The old test targets a deprecated class (e.g. `BactrianXxx`); the spec replacement tests the new-API class instead. Verify the spec test exercises the same statistical properties, then delete the old test.

| Old test class | Deprecated class under test | Spec test covering the new API |
|---|---|---|
| `test.beast.core.parameter.ParameterTest` | `RealParameter` (@Deprecated) | `beast.base.spec.inference.parameter.Real/IntScalarParamTest` et al. |
| `test.beast.evolution.operator.BactrianDeltaExchangeOperatorTest` | `BactrianDeltaExchangeOperator` (@Deprecated forRemoval) | `beast.base.spec.evolution.operator.DeltaExchangeOperatorTest` |
| `test.beast.evolution.operator.BactrianIntervalOperatorTest` | `BactrianIntervalOperator` (@Deprecated) | `beast.base.spec.evolution.operator.IntervalOperatorTest` |
| `test.beast.evolution.operator.BactrianRandomWalkOperatorTest` | `BactrianRandomWalkOperator` (@Deprecated) | `beast.base.spec.evolution.operator.RealRandomWalkOperatorTest` |
| `test.beast.evolution.operator.BactrianScaleOperatorTest` | `BactrianScaleOperator` (@Deprecated) | `beast.base.spec.evolution.operator.ScaleOperatorTest` |
| `test.beast.evolution.operator.BactrianUpDownOperatorTest` | `BactrianUpDownOperator` (@Deprecated) | `beast.base.spec.evolution.operator.UpDownOperatorTest` |
| `test.beast.evolution.operator.NoPriorVsUniformTest` | `BactrianRandomWalkOperator`, `BactrianScaleOperator`, `RealRandomWalkOperator` (all @Deprecated) | `beast.base.spec.evolution.operator.RandomVsBactrianTest` |

#### ✅ Direct spec replacement exists — 28 tests, safe to delete

Spec replacement has the same class name and tests the same logic. Delete the old test once the spec is confirmed passing in CI.

| Old test class | Deprecated class under test | Spec replacement |
|---|---|---|
| `test.beast.beast2vs1.ClockModelTest` | `test.beast.beast2vs1.TestFramework` (@Deprecated) | `beast.base.spec.beast2vs1.ClockModelTest` |
| `test.beast.beast2vs1.SubstitutionModelTest` | `test.beast.beast2vs1.TestFramework` (@Deprecated) | `beast.base.spec.beast2vs1.SubstitutionModelTest` |
| `test.beast.beast2vs1.TipTimeTest` | `test.beast.beast2vs1.TestFramework` (@Deprecated) | `beast.base.spec.beast2vs1.TipTimeTest` |
| `test.beast.beast2vs1.TreePriorTest` | `test.beast.beast2vs1.TestFramework` (@Deprecated) | `beast.base.spec.beast2vs1.TreePriorTest` |
| `test.beast.beast2vs1.TreeTest` | `test.beast.beast2vs1.TestFramework` (@Deprecated) | `beast.base.spec.beast2vs1.TreeTest` |
| `test.beast.evolution.inference.DirichletTest` | `Dirichlet` (@Deprecated) | `beast.base.spec.inference.distribution.DirichletTest` |
| `test.beast.evolution.likelihood.BeagleTreeLikelihoodTest` | `BeagleTreeLikelihood` (@Deprecated) | `beast.base.spec.evolution.likelihood.BeagleTreeLikelihoodTest` |
| `test.beast.evolution.likelihood.TreeLikelihoodTest` | `TreeLikelihood` (@Deprecated) | `beast.base.spec.evolution.likelihood.TreeLikelihoodTest` |
| `test.beast.evolution.operator.DeltaExchangeOperatorTest` | `DeltaExchangeOperator` (@Deprecated) | `beast.base.spec.evolution.operator.DeltaExchangeOperatorTest` |
| `test.beast.evolution.operator.IntRandomWalkOperatorTest` | `IntRandomWalkOperator` (@Deprecated) | `beast.base.spec.evolution.operator.IntRandomWalkOperatorTest` |
| `test.beast.evolution.operator.ScaleOperatorTest` | `ScaleOperator` (@Deprecated) | `beast.base.spec.evolution.operator.ScaleOperatorTest` |
| `test.beast.evolution.operator.UniformIntegerOperatorTest` | `IntUniformOperator` (@Deprecated) | `beast.base.spec.evolution.operator.IntUniformOperatorTest` |
| `test.beast.evolution.speciation.BirthDeathGernhard08ModelTest` | `BirthDeathGernhard08Model` (@Deprecated) | `beast.base.spec.evolution.speciation.BirthDeathGernhard08ModelTest` |
| `test.beast.evolution.speciation.YuleModelTest` | `YuleModel` (@Deprecated) | `beast.base.spec.evolution.speciation.YuleModelTest` |
| `test.beast.evolution.substmodel.GeneralSubstitutionModelTest` | `GeneralSubstitutionModel` (@Deprecated) | `beast.base.spec.evolution.substmodel.GeneralSubstitutionModelTest` |
| `test.beast.evolution.substmodel.GTRTest` | `GTR` (@Deprecated) | `beast.base.spec.evolution.substmodel.GTRTest` |
| `test.beast.evolution.substmodel.HKYTest` | `HKY` (@Deprecated) | `beast.base.spec.evolution.substmodel.HKYTest` |
| `test.beast.evolution.tree.coalescent.BayesianSkylineTest` | `BayesianSkyline` (@Deprecated) | `beast.base.spec.evolution.tree.coalescent.BayesianSkylineTest` |
| `test.beast.evolution.tree.coalescent.CoalescentTest` | `ConstantPopulation` (@Deprecated) | `beast.base.spec.evolution.tree.coalescent.CoalescentTest` |
| `test.beast.evolution.tree.RandomTreeTest` | `RandomTree` (@Deprecated) | `beast.base.spec.evolution.tree.RandomTreeTest` |
| `test.beast.math.distributions.GammaTest` | `Gamma` (@Deprecated) | `beast.base.spec.inference.distribution.GammaTest` |
| `test.beast.math.distributions.InvGammaTest` | `InverseGamma` (@Deprecated) | `beast.base.spec.inference.distribution.InverseGammaTest` |
| `test.beast.math.distributions.LaplaceDistributionTest` | `LaplaceDistribution` (@Deprecated) | `beast.base.spec.inference.distribution.LaplaceTest` |
| `test.beast.math.distributions.LogNormalDistributionModelTest` | `LogNormalDistributionModel` (@Deprecated) | `beast.base.spec.inference.distribution.LogNormalTest` |
| `test.beast.math.distributions.MeanOfParametricDistributionTest` | `Gamma`, `InverseGamma` etc. (@Deprecated) | `beast.base.spec.inference.distribution.MeanOfParametricDistributionTest` |
| `test.beast.math.distributions.MRCAPriorTest` | `MRCAPrior` (@Deprecated) | `beast.base.spec.inference.distribution.MRCAPriorTest` |
| `test.beast.math.distributions.NormalDistributionTest` | `Normal` (@Deprecated) | `beast.base.spec.inference.distribution.NormalTest` |
| `test.beast.statistic.RPNCalculatorTest` | `RPNcalculator` (@Deprecated) | `beast.base.spec.statistic.RPNCalculatorTest` |

#### 💤 Disabled — 3 tests, decide fate

All `@Test` methods are commented out. Migrate to spec or delete.

| Old test class | Deprecated class under test | Note |
|---|---|---|
| `test.beast.beast2vs1.tutorials.DivergenceDatingTest` | `test.beast.beast2vs1.TestFramework` (@Deprecated) | All @Tests commented out; migrate or delete |
| `test.beast.beast2vs1.tutorials.RateTutorialTest` | `test.beast.beast2vs1.TestFramework` (@Deprecated) | All @Tests commented out; migrate or delete |
| `test.beast.core.util.SumTest` | `Sum` (@Deprecated) | Entire file commented out; migrate or delete |

---

## Spec Test Quality Issues

29 `// TODO` comments found across 9 `beast.base.spec.*` test files. These are distinct from the old→spec migration above: they concern the quality and completeness of the spec tests themselves.

| Category | Items | Files | Severity | Status |
|---|---|---|---|---|
| A — Missing or broken assertions | 3 | 2 | High | Open |
| B — Incomplete or known-failing tests | 5 | 2 | High | Open |
| C — Fragile or unclear | 3 | 3 | Medium | Open |
| D — JUnit 4 / auto-generated stubs | 18 | 3 | Low | ✅ Fixed |

### A — Missing or broken assertions

| File | Line | Issue |
|---|---|---|
| `spec/evolution/branchratemodel/UCRelaxedClockModelTest.java` | 64 | `testDistr()` assertion on `getDistribution()` return type is commented out (`// TODO how this is passed ?`); the method makes no assertion about the distribution |
| `spec/evolution/branchratemodel/UCRelaxedClockModelTest.java` | 95 | `// TODO more ?` — only 2 test methods; coverage of `UCRelaxedClockModel` is incomplete |
| `spec/evolution/operator/UpDownOperatorTest.java` | 45 | `RPNcalculator` constructed and passed through `AsRealScalar` but `// TODO why not working?` — unclear whether the calculator feeds into the MCMC prior as intended; statistical assertions may not reflect the operator under test |

### B — Incomplete or known-failing tests

| File | Line | Issue |
|---|---|---|
| `spec/inference/distribution/GammaTest.java` | 69 | `testPdf()` marked `//TODO not working yet` but still runs as `@Test`; outcome is unreliable |
| `spec/inference/distribution/GammaTest.java` | 88 | `final int index = 1; //TODO Randomizer.nextInt(4)` — only ShapeRate mode exercised; ShapeScale (0), ShapeMean (2), OneParameter (3) never tested |
| `spec/beast2vs1/TreePriorTest.java` | 60 | `// TODO testEBSP() in beast 2.7 v beast 1 also fails, add a test to compare 2.8 to 2.7` — `testEBSP()` runs against BEAST 1 expectations it cannot meet; a BEAST 2.7 vs 2.8 regression test is needed instead |
| `spec/beast2vs1/TreePriorTest.java` | 154 | `skyline.groupSize.3` expectation retained even though diff > 2×delta (`// TODO diff bigger than 2*delta`); BSP test is borderline |
| `spec/beast2vs1/TreePriorTest.java` | 173 | `groupSizes2` expectation commented out because diff > 2×delta; BSP test has a known coverage gap |

### C — Fragile or unclear tests

| File | Line | Issue |
|---|---|---|
| `spec/evolution/tree/RandomTreeTest.java` | 48 | Seed 53 causes `testCoalescentTimes()` to fail (`// TODO this seed makes test fail`); workaround seed 666 is used — indicates flaky statistics or a real bug triggered by that seed |
| `spec/beast2vs1/TipTimeTest.java` | 29 | Bare `//TODO` with no description before `testStrictClockTipTime()`; intent unknown |
| `spec/evolution/operator/IntUniformOperatorTest.java` | 72 | Bare `//TODO` before `parameter.setLower(0)` in `testIntegerVectorBound()`; possibly noting bounds should be set via `IntUniform` distribution rather than directly on the parameter |

### D — JUnit 4 / auto-generated stubs ✅ Fixed

Migrated `ScalarTest`, `TensorUtilsTest`, and `TypeUtilsTest` from JUnit 4 (`org.junit.Test`, `org.junit.Assert`) to JUnit 5 (`org.junit.jupiter.api.Test`, `org.junit.jupiter.api.Assertions`). All 18 auto-generated stubs replaced with minimal faithful implementations; `@Test(expected=…)` converted to `assertThrows(…)`.

| File | Items fixed |
|---|---|
| `spec/type/ScalarTest.java` | 1 stub: `TestIntScalar.get(int... idx)` now returns `value` |
| `spec/type/TensorUtilsTest.java` | 13 stubs: `TestRealVector.getDomain/isValid/getElements` implemented; anonymous stubs cleaned; 2× `@Test(expected=…)` → `assertThrows` |
| `spec/type/TypeUtilsTest.java` | 4 stubs: `TestTensor.rank()` returns `shape.length`; others cleaned |

---

## Status legend

| Symbol | Meaning |
|---|---|
| ✅ | Spec test exists with the same name and tests the same logic via the new API |
| ⚠️ | Spec test exists but under a different name (old class → new spec class, e.g. `BactrianRandomWalkOperator` → `RealRandomWalkOperator`) |
| ❌ | No spec test yet — needs to be written |
| 💤 | All `@Test` methods in the old file are commented out; lowest priority |

---

## Details by old test class

### `test.beast.beast2vs1` package

All classes in this package extend the deprecated `test.beast.beast2vs1.TestFramework`, which uses
`System.getProperty("user.dir")` to locate XMLs. The replacement base class is
`beast.base.spec.beast2vs1.TestFramework`, which uses `getResource()` (classpath-safe).

---

#### `test.beast.beast2vs1.ClockModelTest` ✅
- **Tested class:** XML-driven MCMC runs via deprecated `TestFramework`; exercises `StrictClockModel`, `RandomLocalClockModel`, `UCRelaxedClockModel`
- **XML files:** `testStrictClock.xml`, `testStrictClock2.xml`, `testRandomLocalClock.xml`, `testUCRelaxedClockLogNormal.xml`
- **Spec replacement:** `beast.base.spec.beast2vs1.ClockModelTest` — same XML files, same expectations, uses `readTestXML()` via `getResource()`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.beast2vs1.SubstitutionModelTest` ✅
- **Tested class:** XML-driven MCMC runs; exercises `HKY`, `SiteModel` (both deprecated)
- **XML files:** `testHKY.xml`, `testSiteModelAlpha.xml`, `testMultiSubstModel.xml`, `testSRD06CP12_3.xml`
- **Spec replacement:** `beast.base.spec.beast2vs1.SubstitutionModelTest` — same XML files, `testSRD06CP12_3` commented out in both
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.beast2vs1.TipTimeTest` ✅
- **Tested class:** Tip-dated coalescent MCMC via deprecated `TestFramework`
- **XML files:** `testCoalescentTipDates.xml`, `testCoalescentTipDates1.xml`, `testStrictClockTipTime.xml`, `testCoalescentTipDatesSampling.xml`, `testStrictClockTipDatesSampling.xml`
- **Spec replacement:** `beast.base.spec.beast2vs1.TipTimeTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.beast2vs1.TreePriorTest` ✅
- **Tested class:** Tree prior MCMC (coalescent, Yule, BD models, all deprecated) via deprecated `TestFramework`
- **XML files:** `testCoalescentConstant.xml`, `testYuleModel.xml`, `testBirthDeath.xml` etc.
- **Spec replacement:** `beast.base.spec.beast2vs1.TreePriorTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.beast2vs1.TreeTest` ✅
- **Tested class:** Calibrated tree prior MCMC via deprecated `TestFramework`
- **XML files:** `testCalibration.xml`, `testCalibrationMono.xml`
- **Spec replacement:** `beast.base.spec.beast2vs1.TreeTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.beast2vs1.StarBEASTTest` ❌
- **Tested class:** StarBEAST multi-species coalescent via deprecated `TestFramework`
- **XML files:** `testStarBEASTConstant.xml`, `testStarBEASTLinear.xml`, `testStarBEASTNone.xml`
- **Spec replacement:** none
- **Action:** Create `beast.base.spec.beast2vs1.StarBEASTTest` extending the new `TestFramework`

---

#### `test.beast.beast2vs1.TaxonOrderTest` ❌
- **Tested class:** Taxon ordering in StarBEAST2 via deprecated `TestFramework`
- **XML files:** `testStarBeast2.xml`
- **Spec replacement:** none
- **Action:** Create `beast.base.spec.beast2vs1.TaxonOrderTest` extending the new `TestFramework`

---

#### `test.beast.beast2vs1.tutorials.DivergenceDatingTest` 💤
- **Tested class:** Divergence dating tutorial via deprecated `TestFramework`
- **Note:** All `@Test` methods are commented out
- **Spec replacement:** none
- **Action:** Decide whether to re-enable and migrate, or delete

---

#### `test.beast.beast2vs1.tutorials.RateTutorialTest` 💤
- **Tested class:** Molecular clock rate tutorial via deprecated `TestFramework`
- **Note:** All `@Test` methods are commented out
- **Spec replacement:** none
- **Action:** Decide whether to re-enable and migrate, or delete

---

### `test.beast.core` package

---

#### `test.beast.core.parameter.ParameterTest` ⚠️
- **Tested class:** `RealParameter` (@Deprecated)
- **Tests:** scalar get/set, array bounds, copy, restore
- **Spec replacements:** `beast.base.spec.inference.parameter.RealScalarParamTest`, `RealVectorParamTest`, `IntScalarParamTest`, `VectorElementTest` — collectively cover the same operations via the new typed parameter API
- **Action:** Verify coverage gap between old and spec tests; delete old test if no unique scenario remains

---

#### `test.beast.core.util.SumTest` 💤
- **Tested class:** `Sum` (@Deprecated)
- **Note:** Entire file is commented out (not compiled or run)
- **Spec replacement:** none
- **Action:** Delete the file, or un-comment and migrate to a spec test for the new spec `FunctionOfTensor` equivalent if needed

---

### `test.beast.evolution.alignment` package

---

#### `test.beast.evolution.alignment.FilteredAlignmentTest` ❌
- **Tested class:** `FilteredAlignment` (@Deprecated) — 7 active `@Test` methods covering mask filters, site filtering, and pattern handling
- **Spec replacement:** none
- **Action:** Create `beast.base.spec.evolution.alignment.FilteredAlignmentTest` (or equivalent spec for the new alignment API)

---

### `test.beast.evolution.inference` package

---

#### `test.beast.evolution.inference.DirichletTest` ✅
- **Tested class:** `Dirichlet` (@Deprecated)
- **Spec replacement:** `beast.base.spec.inference.distribution.DirichletTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

### `test.beast.evolution.likelihood` package

---

#### `test.beast.evolution.likelihood.BeagleTreeLikelihoodTest` ✅
- **Tested class:** `BeagleTreeLikelihood` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.likelihood.BeagleTreeLikelihoodTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.evolution.likelihood.TreeLikelihoodTest` ✅
- **Tested class:** `TreeLikelihood` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.likelihood.TreeLikelihoodTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

### `test.beast.evolution.operator` package

---

#### `test.beast.evolution.operator.BactrianDeltaExchangeOperatorTest` ⚠️
- **Tested class:** `BactrianDeltaExchangeOperator` (@Deprecated, **forRemoval**)
- **Extends:** `BactrianRandomWalkOperatorTest` (also deprecated)
- **Spec coverage:** `beast.base.spec.evolution.operator.DeltaExchangeOperatorTest` tests the spec `DeltaExchangeOperator` replacement
- **Action:** Delete old test; verify `DeltaExchangeOperatorTest` covers the same statistical properties

---

#### `test.beast.evolution.operator.BactrianIntervalOperatorTest` ⚠️
- **Tested class:** `BactrianIntervalOperator` (@Deprecated)
- **Spec coverage:** `beast.base.spec.evolution.operator.IntervalOperatorTest` tests the new spec `ScalarIntervalOperator`/`VectorIntervalOperator`
- **Action:** Delete old test once spec `IntervalOperatorTest` provides equivalent coverage

---

#### `test.beast.evolution.operator.BactrianRandomWalkOperatorTest` ⚠️
- **Tested class:** `BactrianRandomWalkOperator` (@Deprecated)
- **Spec coverage:** `beast.base.spec.evolution.operator.RealRandomWalkOperatorTest` tests the new spec `RealRandomWalkOperator`
- **Action:** Delete old test; ensure `RealRandomWalkOperatorTest` covers all distribution modes

---

#### `test.beast.evolution.operator.BactrianScaleOperatorTest` ⚠️
- **Tested class:** `BactrianScaleOperator` (@Deprecated)
- **Spec coverage:** `beast.base.spec.evolution.operator.ScaleOperatorTest` tests the new spec `ScaleOperator`
- **Action:** Delete old test once spec `ScaleOperatorTest` covers Bactrian kernel mode

---

#### `test.beast.evolution.operator.BactrianUpDownOperatorTest` ⚠️
- **Tested class:** `BactrianUpDownOperator` (@Deprecated)
- **Spec coverage:** `beast.base.spec.evolution.operator.UpDownOperatorTest` tests the new spec `UpDownOperator`
- **Action:** Delete old test once spec `UpDownOperatorTest` provides equivalent coverage

---

#### `test.beast.evolution.operator.DeltaExchangeOperatorTest` ✅
- **Tested class:** `DeltaExchangeOperator` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.operator.DeltaExchangeOperatorTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.evolution.operator.IntRandomWalkOperatorTest` ✅
- **Tested class:** `IntRandomWalkOperator` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.operator.IntRandomWalkOperatorTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.evolution.operator.NoPriorVsUniformTest` ⚠️
- **Tested classes:** `BactrianRandomWalkOperator`, `BactrianScaleOperator`, `RealRandomWalkOperator` (all @Deprecated)
- **Extends:** `BactrianRandomWalkOperatorTest` (deprecated)
- **Spec coverage:** `beast.base.spec.evolution.operator.RandomVsBactrianTest` compares `KernelDistribution` modes (the underlying mechanism shared by the new spec operators)
- **Action:** Delete old test; verify `RandomVsBactrianTest` covers no-prior vs uniform prior scenarios

---

#### `test.beast.evolution.operator.ScaleOperatorTest` ✅
- **Tested class:** `ScaleOperator` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.operator.ScaleOperatorTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.evolution.operator.UniformIntegerOperatorTest` ✅
- **Tested class:** `IntUniformOperator` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.operator.IntUniformOperatorTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

### `test.beast.evolution.speciation` package

---

#### `test.beast.evolution.speciation.BirthDeathGernhard08ModelTest` ✅
- **Tested class:** `BirthDeathGernhard08Model` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.speciation.BirthDeathGernhard08ModelTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.evolution.speciation.YuleModelTest` ✅
- **Tested class:** `YuleModel` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.speciation.YuleModelTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

### `test.beast.evolution.substmodel` package

---

#### `test.beast.evolution.substmodel.GeneralSubstitutionModelTest` ✅
- **Tested class:** `GeneralSubstitutionModel` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.substmodel.GeneralSubstitutionModelTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.evolution.substmodel.GTRTest` ✅
- **Tested class:** `GTR` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.substmodel.GTRTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.evolution.substmodel.HKYTest` ✅
- **Tested class:** `HKY` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.substmodel.HKYTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

### `test.beast.evolution.tree.coalescent` package

---

#### `test.beast.evolution.tree.coalescent.BayesianSkylineTest` ✅
- **Tested class:** `BayesianSkyline` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.tree.coalescent.BayesianSkylineTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.evolution.tree.coalescent.CoalescentTest` ✅
- **Tested class:** `ConstantPopulation` (@Deprecated); also uses `Coalescent` (not deprecated)
- **Spec replacement:** `beast.base.spec.evolution.tree.coalescent.CoalescentTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.evolution.tree.RandomTreeTest` ✅
- **Tested class:** `RandomTree` (@Deprecated)
- **Spec replacement:** `beast.base.spec.evolution.tree.RandomTreeTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

### `test.beast.math.distributions` package

---

#### `test.beast.math.distributions.GammaTest` ✅
- **Tested class:** `Gamma` (@Deprecated)
- **Spec replacement:** `beast.base.spec.inference.distribution.GammaTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.math.distributions.InvGammaTest` ✅
- **Tested class:** `InverseGamma` (@Deprecated)
- **Spec replacement:** `beast.base.spec.inference.distribution.InverseGammaTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.math.distributions.LaplaceDistributionTest` ✅
- **Tested class:** `LaplaceDistribution` (@Deprecated)
- **Spec replacement:** `beast.base.spec.inference.distribution.LaplaceTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.math.distributions.LogNormalDistributionModelTest` ✅
- **Tested class:** `LogNormalDistributionModel` (@Deprecated)
- **Spec replacement:** `beast.base.spec.inference.distribution.LogNormalTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.math.distributions.MeanOfParametricDistributionTest` ✅
- **Tested class:** `MeanOfParametricDistribution` (not deprecated itself), but exercises `Gamma`, `InverseGamma`, `LogNormalDistributionModel` etc. as inputs (all @Deprecated)
- **Spec replacement:** `beast.base.spec.inference.distribution.MeanOfParametricDistributionTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.math.distributions.MRCAPriorTest` ✅
- **Tested class:** `MRCAPrior` (@Deprecated)
- **Spec replacement:** `beast.base.spec.inference.distribution.MRCAPriorTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

#### `test.beast.math.distributions.NormalDistributionTest` ✅
- **Tested class:** `Normal` (@Deprecated)
- **Spec replacement:** `beast.base.spec.inference.distribution.NormalTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

### `test.beast.statistic` package

---

#### `test.beast.statistic.RPNCalculatorTest` ✅
- **Tested class:** `RPNcalculator` (@Deprecated)
- **Spec replacement:** `beast.base.spec.statistic.RPNCalculatorTest`
- **Action:** Delete old test once spec is confirmed passing in CI

---

### `test.beast.util` package

---

#### `test.beast.util.ClusterTreeTest` ❌
- **Tested class:** `ClusterTree` (@Deprecated) — tests NJ and UPGMA starting-tree construction
- **Spec replacement:** none
- **Action:** Create `beast.base.spec.evolution.tree.ClusterTreeTest` (or equivalent) once the spec provides a starting-tree API

---

## Old tests requiring no action

These old tests need no migration. They fall into two sub-categories.

### Already migrated to spec API

These tests live in the old `test.beast.*` package tree but already import and exercise the new spec API directly. Migration is complete.

| Old test class | Spec subject | Note |
|---|---|---|
| `test.beast.evolution.substmodel.BinaryCovarionModelTest` | `beast.base.spec.evolution.substmodel.BinaryCovarion` | Already uses spec API directly |

### Not deprecated — no migration needed

These tests target classes that are not deprecated; they are current and require no action.

| Old test class | Subject | Note |
|---|---|---|
| `test.beast.BEASTTestCase` | Base helper class | Not a test; still needed by other tests |
| `test.beast.core.BEASTInterfaceTest` | `BEASTInterface` | Class not deprecated (only one method is) |
| `test.beast.core.InputForAnnotatedConstructorTest` | `Input` | Not deprecated |
| `test.beast.core.InputTest` | `Input` | Not deprecated |
| `test.beast.core.LoggerTest` | `Logger` | Not deprecated |
| `test.beast.core.OperatorScheduleTest` | `OperatorSchedule` | Not deprecated |
| `test.beast.core.StateNodeInitialiserTest` | `StateNode` | Not deprecated |
| `test.beast.evolution.alignment.UncertainAlignmentTest` | `Alignment` | Class not deprecated |
| `test.beast.evolution.datatype.DataTypeDeEncodeTest` | `DataType` | Not deprecated |
| `test.beast.evolution.datatype.IntegerDataTest` | `IntegerData` | Not deprecated |
| `test.beast.evolution.operator.CompoundParameterHelperTest` | `CompoundParameterHelper` | Not deprecated |
| `test.beast.evolution.operator.ExchangeOperatorTest` | `Exchange` | Not deprecated |
| `test.beast.evolution.operator.KernelDistiburionTest` | `KernelDistribution` | Not deprecated; used by new spec operators |
| `test.beast.evolution.substmodel.ColtEigenSystemBenchmark` | `ColtEigenSystem` | Not deprecated |
| `test.beast.evolution.substmodel.ColtEigenSystemTest` | `ColtEigenSystem` | Not deprecated |
| `test.beast.evolution.tree.NodeTest` | `Node` | Class not deprecated (only some methods) |
| `test.beast.evolution.tree.TraitSetTest` | `TraitSet` | Class not deprecated |
| `test.beast.evolution.tree.TreeTest` | `Tree` | Not deprecated |
| `test.beast.evolution.tree.TreeUtilsTest` | `TreeUtils` | Not deprecated |
| `test.beast.integration.DependencyTest` | integration | Not deprecated |
| `test.beast.integration.DocumentationTest` | integration | Not deprecated |
| `test.beast.integration.InputTypeTest` | integration | Not deprecated |
| `test.beast.integration.XMLElementNameTest` | integration | Not deprecated |
| `test.beast.integration.XMLProducerTest` | integration | Not deprecated |
| `test.beast.util.NexusParserTest` | `NexusParser` | Not deprecated |
| `test.beast.util.RandomizerTest` | `Randomizer` | Not deprecated |
| `test.beast.util.TreeParserTest` | `TreeParser` | Not deprecated |
| `test.beast.util.XMLParserTest` | `XMLParser` | Not deprecated |
| `test.beast.util.XMLTest` | XML utilities | Not deprecated |
