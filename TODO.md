# TODO — Package Developer Readiness

## 1. ~~Javadoc: evolution-tier spec classes~~ DONE
Added class-level javadoc to ~90 evolution-tier spec files: substitution models,
likelihood, tree operators, branch rate models, speciation, coalescent, distributions,
site model, tree types, loggers, and utility classes.

## 2. Package Developer Guide
Write a guide covering:
- How to write a custom Operator (lifecycle, proposal, Hastings ratio)
- How to write a custom Distribution (ScalarDistribution / TensorDistribution)
- Type system patterns (RealScalar vs RealVector, domain propagation, casting utilities)
- Service registration (@ServiceProvider vs module-info.java provides vs version.xml)
- Testing patterns for custom components

## 3. ~~Fix pre-existing javadoc errors~~ DONE
`mvn javadoc:javadoc -pl beast-base` now runs with 0 errors (was 100+).

## 4. Publish javadoc site
Set up GitHub Pages (or similar) to host generated javadoc, and link from README.

## 5. CHANGELOG
Summarize BEAST 2 → 3 differences beyond what the migration guide covers.

## 6. ~~Verify beast-package-skeleton~~ DONE
Fixed dead README links, removed unnecessary surefire `--add-reads` flags, corrected XML
version attribute. Skeleton compiles, all 4 tests pass, no warnings.

## 7. Version / release strategy
Currently `2.8.0-SNAPSHOT`. Decide whether to cut a release or tag before the call so
package developers have a stable version to build against.

## 8. Fix disabled beast-fx tests
Several beast-fx tests are disabled pending deeper fixes:
- **Port StarBeast.xml template** to BEAST 3 (enables BeautiStarBeastTest, BeautiCLITest.testStarBeastBatchMode, LinkUnlinkTest.starBeastLinkTreesAndDeleteTest)
- **Port Relaxed Clock subtemplates** (Exponential, Log Normal) to ClockModels.xml (enables SimpleClockModelTest, CloneTest.simpleClockModelCloneTest)
- **Fix BEAUti partition deletion** to clean up tree priors and clock priors for deleted partitions (enables 6 LinkUnlinkTest methods: linkTreesAndDeleteTest2a/2b/3, linkSiteModelsAndDeleteTest/2, linkClocksSitesAndDeleteTest)
- **Fix SimplexParam cloning** across partitions — freqParameter dimension gets multiplied when site model is cloned (enables CloneTest.simpleSiteModelCloneTest)
- **Update assertParameterCountInPriorIs** in BeautiBase to handle new spec distributions (Gamma, Beta, etc.) which are not `instanceof beast.base.inference.distribution.Prior`
- **Investigate testBSP.xml intermittent failure** — passes in isolation but fails in full suite due to Randomizer state pollution from BEAUti JavaFX threads

## 9. Maven Central package distribution ([#40](https://github.com/CompEvol/beast3/issues/40))
Enable external BEAST packages to be distributed as plain Maven Central JARs instead of
(or in addition to) ZIP archives. Roadmap:
- ~~**Step 1: Load fxtemplates from JAR resources**~~ DONE — `BeautiDoc.processTemplate()` now
  scans JPMS module resources in addition to filesystem directories
- ~~**Step 2: Maven-based package resolution**~~ DONE — `MavenPackageResolver` resolves Maven
  coordinates to local JAR paths via Apache Maven Resolver; `PackageManager` loads, installs,
  and uninstalls Maven packages alongside ZIP packages; config persisted in `maven-packages.xml`
- ~~**Step 3: Package manager UI**~~ DONE — "Install from Maven" button added to BEAUti
  package manager dialog
- ~~**Step 4: Migrate existing packages**~~ DONE — beast-package-skeleton updated with Maven
  Central metadata, release profile, version.xml JAR embedding, and publishing docs; morph-models
  updated with io.github.compevol groupId, release profile, and version.xml JAR embedding

## 10. Custom domain extension limitation
The current domain system (Real, PositiveReal, etc.) is a closed set of enum-like classes.
Document the recommended approach for packages that need custom domains (e.g., correlation
matrices, probability simplices with specific constraints).
