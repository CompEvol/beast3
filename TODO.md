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
