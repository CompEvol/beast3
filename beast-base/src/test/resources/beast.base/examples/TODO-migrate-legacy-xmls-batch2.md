# TODO — PR #204 (migrate-legacy-xmls-batch2)

Open items from alexeid's review of #204 (2026-11-06), and follow-ups.

Done on the branch: request 1 (proper priors, BEAST3 and `legacy/`), request 2 (tuned slide sizes),
request 4 (`Input<BranchRateModel>` in `SequenceSimulator` / `SimulatedAlignment`). Also: the two legacy
seqgen files moved to `beast.fx/examples/legacy/`, `legacy/README.md`, and legacy parsing tests in both
`ExampleXmlParsingTest`s.

## From the review

- [ ] **3. Posterior comparison against BEAST 2.7.9** (steps 2–4), now that the priors changed.
  - Files: the 8 MCMC examples — testRandomLocalClock, testRelaxedClock, testUCLNclock.json,
    testTipDates, testTipDates2, testSRD06, testMultipleAlignments_randomTaxaOrder,
    testSimulatedAlignment (beast-fx). The DirectSimulator files and testSeqGen run no MCMC.
  - BEAST 3: `examples/<file>`; BEAST 2: `examples/legacy/<file>`. Two replicates each, at the files'
    chain lengths, compared with `beast3-test/scripts/compare.py` (as in #199).
  - BEAST 2.7.9: the installed 2.7.7 launcher already loads the BEAST.base v2.7.9 package
    (`/Applications/BEAST 2.7.7/bin/beast`).
  - `beast.fx/examples/legacy/testSimulatedAlignment.xml` runs in BEAST 2.8 only (it uses the BEAST 3
    `SiteModel`, as set in 5cbd293). For the 2.7.9 run, use a temporary copy whose namespace has
    `beast.base.evolution.sitemodel` instead of `beast.base.spec.evolution.sitemodel`.
- [ ] Ask alexeid: request 4 lets legacy clocks into `SequenceSimulator` / `SimulatedAlignment`, but
      their `siteModel` input still takes only the BEAST3 `SiteModel`, so BEAST 2 seqgen XML still
      does not run in BEAST3. Widen `siteModel` too?
- [ ] **5. Run the changed release scripts end to end** before the next RC.
  - Linux / Windows: run `release-bundles.yml` (`workflow_dispatch`) on this branch; check that
    `examples/beastfx/` is in the bundles.
  - macOS: `release/Mac/build-sign-dmg.sh` (needs signing), or the local unsigned build.

## Follow-ups

- [ ] Ask alexeid whether the `legacy/` changes are enough for "both versions", or whether the same
      changes should also go to the CompEvol/beast2 examples (as #1225 did for batch 1).
- [ ] Update the PR description (the report is updated): proper priors in both versions; tuned slide
      sizes; `Input<BranchRateModel>`; legacy seqgen files moved to `beast.fx/examples/legacy/`; new
      legacy parsing tests; `legacy/README.md`; the `JSONProducer` bug is fixed (#205 / #206).
- [ ] `legacy/` subfolders that do not load in BEAST 2.8 (listed in `legacy/README.md`): benchmark/1
      (BEAST 1), benchmark/II (`TreeHeightLogger`), starbeastinit (namespace), parameterised (-D args).
      Fix, or leave out of the legacy parsing test as now.
- [ ] Reply to the review once 3 and 5 are done.
- [ ] Extend `ExampleDeprecationTest` (on master since #199) to `beast.fx/examples` (testSeqGen,
      testSimulatedAlignment).
- [x] Merge order: #203, #207 and #199 are merged; master is merged into this branch.
- [ ] Migration skill (beast3-migration, branch `fix/converter-issues-from-batch2`): switch to the
      proper-prior method (LogNormal(0, 8); LogNormal(1, 1.25) for kappa) and drop the JSON
      workaround now that #206 is merged.
- [ ] Optional, if review is slow: split #204 — keep the beast-fx packaging, the Java fix,
      testSeqGen and the DirectSimulator files; move the 8 MCMC examples to a follow-up PR.
