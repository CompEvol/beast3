# BEAST2 → BEAST3 migration report (batch 2)

Branch: `migrate-legacy-xmls-batch2` (from `master` @ `aa8512e`), 2026-11-06.

12 legacy files in `examples/legacy/` were converted.
**All 12 pass** `-validate`. The 11 beast-base files also pass the beast-base example tests and the deprecation check.

One BEAST2 file was edited: `legacy/testSRD06.xml` (two unused state nodes commented out, see below).

## How it was done

1. Converted each file with `beast3-migration/skills/xml-migration/convert_b2_to_b3.py`.
   The first version of the converter got several things wrong; those were corrected by hand and
   then fixed in the converter (skill branch `fix/converter-issues-from-batch2`). The updated converter now produces
   these files itself: its output matches them, except that `RealParameter6`/`7` were removed from
   testSRD06 by hand (see Differences).
2. `testUCLNclock.json` was written by hand. The updated converter also handles JSON, but writes the
   result as XML because of a BEAST3 `JSONProducer` bug (see "Bugs discovered").
3. Validated every file (see Result).

## Differences from the originals

Same method as [PR #199](https://github.com/CompEvol/beast3/pull/199). Each file also has a comment
at the top listing its own differences.

- **Improper `OneOnX` priors** become `LogUniform` on **[1e-8, 1e8]**. On that range the density is
  proportional to 1/x. The `posterior`/`prior` columns are lower by the constant log(ln 1e16) = 3.6 per prior.

  | File | Parameter(s) | Offset |
  |---|---|---|
  | testTipDates.xml | hky.kappa | 3.6 |
  | testTipDates2.xml | hky.kappa | 3.6 |
  | testSRD06.xml | firstAndSecondSites.hky.kappa, thirdSites.hky.kappa | 7.2 |
  | testMultipleAlignments_randomTaxaOrder.xml | clockRate.c:gene1 (starts at 1e-8, the lower bound), popSize.t:gene1 | 7.2 |

- **`offset` on a distribution.** In BEAST2 every `ParametricDistribution` has an `offset` input; BEAST3
  distributions do not, and an offset is applied by wrapping the distribution in `OffsetReal` (or `OffsetInt`
  for integer distributions) with an `offset` input. Here every offset is `0.0`, which is no shift, so no wrapper
  is used and `offset="0.0"` is dropped: the tip-date `Uniform` priors in testTipDates and testTipDates2.
  If an offset is required, wrap the distribution, e.g.
  `<distr spec="…OffsetReal" offset="…"><distribution spec="…Uniform" lower="…" upper="…"/></distr>`.
- **`Uniform(0, Infinity)` priors** have log density 0, so they are left out (BEAST3 `Uniform` also needs a finite range): birthRate in testUCLNclock.json.
- **Parameter domains.** BEAST2 parameters have only optional `lower`/`upper` bounds; BEAST3 parameters
  have a `domain`. Each parameter's domain is the domain of the Java `Input` it feeds (the narrowest one if
  it feeds several):
  - `PositiveReal`: kappa, popSize, clock.rate, mutationRate, gamma shape, birthDiffRate, Exponential mean,
    LogNormal S, LogUniform bounds (most had no `lower` in BEAST2);
  - `NonNegativeReal`: `x` in testDirectSimulator2 / testDirectSimulatorHierarchical (feeds only `Exponential.param`);
  - `Real`: LogNormal `M`; `UnitInterval`: proportionInvariant.

  Upper bounds are not kept: `rates` ≤ 100 (testRandomLocalClock), `ucld.stdev` ≤ 5 (testRelaxedClock).
- **Parameter size.** In testRandomLocalClock, BEAST2 gives `rates` and `indicators` one value each,
  and `RandomLocalClockModel` resizes them to one value per branch when the model starts. BEAST3's
  `RandomLocalClockModel` takes them as vectors (`RealVectorParam` / `BoolVectorParam`) and resizes
  them the same way.
- **`SubtreeSlide` → `kernel.BactrianSubtreeSlide`** (`gaussian` dropped). The `size` is still the
  original's value (1.0, or the default). PR #199 instead sets it to the value auto-tuning converges to;
  that needs a run per file and was not done.
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

**Not validated against BEAST 2:** PR #199 also runs each file in BEAST 3 and BEAST 2.7.9 and compares
them (`beast3-test/scripts/compare.py`). That was not done here; these files are validated
(parse + 1000 steps), not checked for matching posteriors.

## Where the files are

Rule: a BEAST3 file sits at the same relative path as its BEAST2 source,
`examples/legacy/<path>` → `examples/<path>`. Files that need beast-fx classes go to
the same structure in beast-fx.

| Kind | BEAST2 source | BEAST3 file | In release bundle |
|---|---|---|---|
| 10 beast-base XMLs + 1 JSON | `beast-base/src/test/resources/beast.base/examples/legacy/` | `beast-base/src/test/resources/beast.base/examples/` | `examples/` |
| 2 SeqGen XMLs (need beast-fx) | `beast-base/src/test/resources/beast.base/examples/legacy/` | `beast-fx/src/test/resources/beast.fx/examples/` | `examples/beastfx/` |

All 12 sources are directly in `legacy/`, so nothing is in a subfolder.

Same-named files in subfolders (not part of this batch):

| BEAST2 (legacy) | BEAST3 |
|---|---|
| `legacy/beast2vs1/testRandomLocalClock.xml` | `beast2vs1/testRandomLocalClock.xml` (already migrated earlier) |
| `legacy/beast2vs1/testTipDates.xml` | ❌ not migrated yet (`beast2vs1/testTipDates.xml` does not exist) |

## Java fixes

`beastfx.app.seqgen.SimulatedAlignment` and `SequenceSimulator`: input `branchRateModel` changed from legacy
`BranchRateModel.Base` to `beast.base.spec.evolution.branchratemodel.Base`, so BEAST3 clock models are accepted.

## Result

How validated: `bin/beast -validate <file>` (beast-fx files: `bin/beast-fx -validate`).
Pass = last output line contains `Done!`.

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

Extra checks, on the final files:

| Check | Result |
|---|---|
| `ExampleXmlParsingTest`, `ExampleJSONParsingTest` (parse **and run 1000 MCMC steps** of every beast-base example) | ✅ all pass |
| `ExampleDeprecationTest` from PR #199 (no object of a `@Deprecated` class reachable from the run), run temporarily | ✅ passes |
| Parameter domains match the domain of the Java `Input` each parameter feeds (script check, all 12 files) | ✅ no mismatches |

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

**Fix (not done):** add the same `Domain` case to `JSONProducer.inputToJSON()`, writing
`domain: "PositiveReal"`.

**Effect here:** `testUCLNclock.json` was written by hand. The migration skill converts JSON through
XML, and until this is fixed it writes BEAST3 XML instead of JSON.
