# BEAST2 → BEAST3 migration report (batch 2)

Branch: `migrate-legacy-xmls-batch2` (from `master` @ `aa8512e`), 2026-11-06.

12 BEAST 2 examples from `legacy/` were converted. **All 12 pass** `-validate`, and every changed BEAST 2
file still runs in BEAST 2.8 (see Result).

Eight BEAST 2 files were edited to get proper priors (see "Proper priors"); `legacy/testSRD06.xml` also has
two unused state nodes commented out. The two BEAST 2 seqgen files moved to beast-fx (see "Where the files are").

## How it was done

1. Converted each file with `beast3-migration/skills/xml-migration/convert_b2_to_b3.py`.
   The first version of the converter got several things wrong; those were corrected by hand and
   then fixed in the converter (skill branch `fix/converter-issues-from-batch2`). The updated converter now produces
   these files itself: its output matches them, except that `RealParameter6`/`7` were removed from
   testSRD06 by hand (see "Changes made by the BEAST 2 → BEAST 3 conversion").
2. `testUCLNclock.json` was written by hand (JSON input was not supported then; see "Bugs discovered").
3. Gave the estimated parameters proper priors, in both versions (see "Proper priors").
4. Validated every file (see Result).

## Proper priors (BEAST 2 and BEAST 3)

This is a change to the **model**, made in both versions: the BEAST 2 files in `legacy/` and the
BEAST 3 files. It is separate from the changes made by the conversion (next section). The examples must
not use improper priors (review of #204), so every estimated parameter with no prior, a 1/x (`OneOnX`)
prior or `Uniform(0, ∞)` now has a proper prior. The choices follow
[#199](https://github.com/CompEvol/beast3/pull/199) and BEAUti's defaults
([#208](https://github.com/CompEvol/beast3/pull/208)):

- **rates and sizes:** LogNormal(M = 0, S = 8) in log space: median 1, 95% from 1.6×10⁻⁷ to 6.4×10⁶;
- **kappa:** LogNormal(M = 1, S = 1.25), BEAUti's kappa prior.

| File | Parameter | 2.7.8 original | Now (BEAST 2 and BEAST 3) |
|---|---|---|---|
| testRandomLocalClock.xml | hky.kappa | none (flat) | LogNormal(1, 1.25) |
| | popSize | none (flat) | LogNormal(0, 8) |
| | rates (one per branch) | none (flat) | LogNormal(0, 8) on each rate |
| testRelaxedClock.xml | hky.kappa | none (flat) | LogNormal(1, 1.25) |
| | popSize | none (flat) | LogNormal(0, 8) |
| | ucld.stdev | none (flat) | LogNormal(0, 8) |
| testUCLNclock.json | birthRate.t:dna | Uniform(0, ∞) | LogNormal(0, 8) |
| testTipDates.xml | hky.kappa | OneOnX (1/x) | LogNormal(1, 1.25) |
| | clockRate | none (flat) | LogNormal(0, 8) |
| testTipDates2.xml | hky.kappa | OneOnX (1/x) | LogNormal(1, 1.25) |
| testSRD06.xml | firstAndSecondSites.hky.kappa, thirdSites.hky.kappa | OneOnX (1/x) | LogNormal(1, 1.25) |
| testMultipleAlignments_randomTaxaOrder.xml | clockRate.c:gene1 (starts at 1e-8) | OneOnX (1/x) | LogNormal(0, 8) |
| | popSize.t:gene1 | OneOnX (1/x) | LogNormal(0, 8) |
| testSimulatedAlignment.xml (beast-fx) | hky.kappa | none (flat) | LogNormal(1, 1.25) |
| | popSize | none (flat) | LogNormal(0, 8) |

How they are written:

| | BEAST 2 (`legacy/`) | BEAST 3 |
|---|---|---|
| prior | `Prior x="@p"` with `LogNormalDistributionModel M S meanInRealSpace="false"` | `LogNormal param="@p" meanInRealSpace="false"` with `M`, `S` |
| on a vector (`rates`) | the same `Prior`: it applies to each value | wrapped in `IID` |
| marked by | a comment above each prior | a comment at the top of each file |

Not changed, because they are already proper or not sampled: the DirectSimulator files and testSeqGen
(no MCMC); the frequencies (flat on the simplex, which is bounded); `indicators` and `rateCategories`
(finitely many values); `ucld.mean` in testRelaxedClock (no operator moves it); and the priors that were
proper in the originals (for example `ucldStdev` Gamma(0.5396, 0.3819) and `gammaShape` Exponential(1)).

The new priors change the posteriors. The comparison with BEAST 2.7.9 is still to do
(see `TODO-migrate-legacy-xmls-batch2.md`).

## Changes made by the BEAST 2 → BEAST 3 conversion

What the BEAST 2 → BEAST 3 conversion itself changes. Each file also has a comment at the top
listing its own differences.

- **`offset` on a distribution.** In BEAST2 every `ParametricDistribution` has an `offset` input; BEAST3
  distributions do not, and an offset is applied by wrapping the distribution in `OffsetReal` (or `OffsetInt`
  for integer distributions) with an `offset` input. Here every offset is `0.0`, which is no shift, so no wrapper
  is used and `offset="0.0"` is dropped: the tip-date `Uniform` priors in testTipDates and testTipDates2.
  If an offset is required, wrap the distribution, e.g.
  `<distr spec="…OffsetReal" offset="…"><distribution spec="…Uniform" lower="…" upper="…"/></distr>`.
- **Parameter domains.** BEAST2 parameters have only optional `lower`/`upper` bounds; BEAST3 parameters
  have a `domain`. Each parameter's domain is the domain of the Java `Input` it feeds (the narrowest one if
  it feeds several):
  - `PositiveReal`: kappa, popSize, clock.rate, mutationRate, gamma shape, birthDiffRate, Exponential mean,
    LogNormal S (most had no `lower` in BEAST2);
  - `NonNegativeReal`: `x` in testDirectSimulator2 / testDirectSimulatorHierarchical (feeds only `Exponential.param`);
  - `Real`: LogNormal `M`; `UnitInterval`: proportionInvariant.

  Upper bounds are not kept: `rates` ≤ 100 (testRandomLocalClock), `ucld.stdev` ≤ 5 (testRelaxedClock).
- **Parameter size.** In testRandomLocalClock, BEAST2 gives `rates` and `indicators` one value each,
  and `RandomLocalClockModel` resizes them to one value per branch when the model starts. BEAST3's
  `RandomLocalClockModel` takes them as vectors (`RealVectorParam` / `BoolVectorParam`) and resizes
  them the same way.
- **`SubtreeSlide` → `kernel.BactrianSubtreeSlide`** (`gaussian` dropped). *Later switched to `kernel.BactrianSubtreeScale` with no size, in #214; the tuned slide sizes below are kept as a record.* Each slide starts at the size
  its auto-tuning converges to in a BEAST 3 run, as in #199, instead of the original's 1.0 (which does not
  tune down within a run on substitution-scale trees, #200). Tuned in BEAST 3 (seed 127, the files' chain
  lengths), starting each slide below its optimum, since tuning grows a size much faster than it shrinks it:

  | File | Start size | Acceptance (target 0.3) |
  |---|---|---|
  | testRandomLocalClock | 0.008 | 0.32 |
  | testRelaxedClock | 0.008 | 0.33 |
  | testTipDates | 0.73 | 0.29 |
  | testTipDates2 | 0.013 | 0.31 |
  | testSRD06 | 0.013 | 0.31 |
  | testMultipleAlignments_randomTaxaOrder (genes 1–3; trees in years) | 27000, 25000, 21000 | 0.31 |
  | testUCLNclock.json | 0.019 | 0.31 |
  | testSimulatedAlignment | 6.0E-5 (between 5E-5 at 0.36 and 8E-5 at 0.20) | – |

  These differ from BEAUti's new start size, 0.01 ([#201](https://github.com/CompEvol/beast3/pull/201)),
  and from the class default, 1.0: the examples keep their tuned sizes, as #199's examples on master do,
  so each slide works well from the first step. For testMultipleAlignments (trees in years) and
  testSimulatedAlignment, tuning could not get from 0.01 to the tuned size within a run (#200). Each
  file's top comment says this.
- **Unused state nodes** are removed: `RealParameter6` / `RealParameter7` in testSRD06 are in the state
  but nothing uses them. They are also commented out (with a note) in `legacy/testSRD06.xml`.
- **Prior wrapper.** A BEAST2 `Prior` wraps a distribution and names its parameter with `x`, as an
  attribute or as an `<x>` child element. BEAST3 has no `Prior` class: the distribution takes the
  parameter directly through `param` (testDirectSimulator, testDirectSimulator2, testDirectSimulatorHierarchical
  use the `<x>` child form).
- **Operator classes.** `ScaleOperator` on a parameter → BEAST3 `ScaleOperator`; on a tree →
  `ScaleTreeOperator`; the tree `Uniform` operator keeps its legacy class. This holds whether the
  target is an attribute or a child element (`<parameter idref=…/>`, `<tree idref=…/>`, as in
  testRelaxedClock and testSimulatedAlignment).
- **Tree scalers** lose `degreesOfFreedom` / `scaleAll` / `scaleAllIndependently`: in BEAST2 these only
  apply to parameters ("not beast.tree"), so they had no effect, and `ScaleTreeOperator` does not have
  them (testTipDates2, testSRD06).

Other changes, all automatic and as listed in `deprecated_classes.md`:
`UniformOperator` on integers → `IntUniformOperator`, `Gamma beta` → `theta`.

## Where the files are

Rule: a BEAST3 file sits at the same relative path as its BEAST2 source,
`examples/legacy/<path>` → `examples/<path>`. Files that need beast-fx classes go to
the same structure in beast-fx.

| Kind | BEAST2 source | BEAST3 file | In release bundle |
|---|---|---|---|
| 10 beast-base XMLs + 1 JSON | `beast-base/src/test/resources/beast.base/examples/legacy/` | `beast-base/src/test/resources/beast.base/examples/` | `examples/` |
| 2 SeqGen XMLs (need beast-fx) | `beast-fx/src/test/resources/beast.fx/examples/legacy/` (moved from beast-base) | `beast-fx/src/test/resources/beast.fx/examples/` | `examples/beastfx/` |

All 12 sources are directly in a `legacy/` folder. The `legacy/` folders hold BEAST 2 XML that is not converted
and must still run in BEAST 2.8.x; see `legacy/README.md`. The two seqgen BEAST 2 files need the BEAST 3
`SiteModel` (their namespace has `beast.base.spec.evolution.sitemodel`, as set in 5cbd293), so they run in
BEAST 2.8 but not in BEAST 2.7.x.

Same-named files in subfolders (not part of this batch):

| BEAST2 (legacy) | BEAST3 |
|---|---|
| `legacy/beast2vs1/testRandomLocalClock.xml` | `beast2vs1/testRandomLocalClock.xml` (already migrated earlier) |
| `legacy/beast2vs1/testTipDates.xml` | ❌ not migrated yet (`beast2vs1/testTipDates.xml` does not exist) |

## Java fixes

`beastfx.app.seqgen.SimulatedAlignment` and `SequenceSimulator`: input `branchRateModel` changed from legacy
`BranchRateModel.Base` to the `BranchRateModel` interface, which both the legacy and the BEAST3 clock models
implement. So both BEAST3 and legacy clocks are accepted. A BEAST 2 seqgen XML still does not run in
BEAST3: the `siteModel` input accepts only the BEAST3 `SiteModel`.

## Result

How validated: `bin/beast -validate <file>` (beast-fx files: `bin/beast-fx -validate`).
Pass = last output line contains `Done!`. All checks below are on the final files.

| # | File | Validate | Notes |
|---|---|---|---|
| 1 | testRandomLocalClock.xml | ✅ PASS | |
| 2 | testRelaxedClock.xml | ✅ PASS | |
| 3 | testUCLNclock.json | ✅ PASS | written by hand (see How it was done) |
| 4 | testTipDates.xml | ✅ PASS | |
| 5 | testTipDates2.xml | ✅ PASS | |
| 6 | testSRD06.xml | ✅ PASS | |
| 7 | testMultipleAlignments_randomTaxaOrder.xml | ✅ PASS | |
| 8 | testDirectSimulator.xml | ✅ PASS | |
| 9 | testDirectSimulator2.xml | ✅ PASS | |
| 10 | testDirectSimulatorHierarchical.xml | ✅ PASS | |
| 11 | beast.fx/examples/testSimulatedAlignment.xml | ✅ PASS (`beast-fx`) | after the `SimulatedAlignment` Java fix |
| 12 | beast.fx/examples/testSeqGen.xml | ✅ PASS (`beast-fx`) | after the `SequenceSimulator` Java fix; also ran it fully (simulates the alignment) |

The changed BEAST 2 files:

| Files | BEAST 2.8 (BEAST 3, `-validate`) | BEAST 2.7.7 (`-validate`) |
|---|---|---|
| 7 files in `beast.base/examples/legacy/` | ✅ all pass | ✅ all pass |
| `beast.fx/examples/legacy/testSeqGen.xml`, `testSimulatedAlignment.xml` | ✅ pass | – (they use the BEAST 3 `SiteModel`) |

Extra checks:

| Check | Result |
|---|---|
| beast-base `ExampleXmlParsingTest`, `ExampleJSONParsingTest` (parse **and run 1000 MCMC steps** of every beast-base example) | ✅ all pass |
| beast-base `ExampleXmlParsingTest.test_ThatLegacyXmlExamplesParse` (new): every XML/JSON directly in `beast.base/examples/legacy/` parses in BEAST 2.8 | ✅ all 41 pass |
| beast-fx `ExampleXmlParsingTest` (new): parse and run `beast.fx/examples`, and parse `beast.fx/examples/legacy` | ✅ all pass |
| Full beast-base suite (after merging master) | ✅ 569 tests, 0 failures, 3 skipped |
| `ExampleDeprecationTest` (from #199, now on master: no object of a `@Deprecated` class reachable from the run) | ✅ passes |
| Parameter domains match the domain of the Java `Input` each parameter feeds (script check of the 11 XMLs, 80 parameters; testUCLNclock.json checked by hand) | ✅ no mismatches |

## Release packaging

The two beast-fx examples ship as `examples/beastfx/` in every installer:

| File | Change |
|---|---|
| `beast-fx/src/assembly/package.xml` | BEAST.app package zip now includes `beast.fx/examples/*.xml` as `examples/beastfx/`. |
| `release/Linux/assemble-bundle.sh` | Extracts `examples/` from the BEAST.app zip as well as the BEAST.base zip; new check that `examples/beastfx/` is not empty. |
| `release/Windows/assemble-bundle.sh` | Same as Linux. |
| `release/Mac/build-sign-dmg.sh` | Also unzips `examples/*` from the BEAST.app zip. |
| `release/{Linux,Windows,Mac}/README.md` | Documented `examples/beastfx/`. |

Checked: `mvn package` builds a BEAST.app zip containing `examples/beastfx/testSeqGen.xml` and
`examples/beastfx/testSimulatedAlignment.xml`; the BEAST.base zip no longer contains them.
The release scripts pass `bash -n` but were **not** run end-to-end.

## Bugs discovered

### BEAST3 `JSONProducer` writes parameter domains that BEAST3 cannot read

**Where:** `beast-base/src/main/java/beast/base/parser/JSONProducer.java`, `inputToJSON()`.

**What happens:** writing a BEAST3 model as JSON, each parameter's `domain` (a shared singleton such as
`PositiveReal`) is written as an object the first time and as `"@null"` after that:

```
{id: "birthRate.t:dna", spec: "…RealScalarParam", value: 1.0,
     domain: {spec: "beast.base.spec.domain.PositiveReal" } },
{id: "kappa.s:dna", spec: "…RealScalarParam", value: 2.0, domain: "@null" },
```

Reading the file back fails: `NoSuchMethodException: beast.base.spec.domain.PositiveReal.newInstance()`
(`Error 1011 parsing the json input file`). So any BEAST3 model with spec parameters cannot be
round-tripped through JSON.

**Cause:** `XMLProducer` has a case for `Domain` values and writes the short name
(`domain='PositiveReal'`, `XMLProducer.java:1141`); `JSONProducer` has none, so it treats the domain as
a BEAST object.

**Fixed:** reported in [#205](https://github.com/CompEvol/beast3/issues/205), fixed in
[#206](https://github.com/CompEvol/beast3/pull/206) (merged): `JSONProducer` now writes `domain: "PositiveReal"`.

**Effect here:** `testUCLNclock.json` was written by hand, before the fix.
