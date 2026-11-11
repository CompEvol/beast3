# BEAST 3 Developer Notes

Notes for developers working on the BEAST 3 code base: conventions and lessons learnt
that the code alone does not make obvious. Add new topics as new sections.

## Contents

1. [Changes to `beast-base`](#changes-to-beast-base)
2. [Cached fields vs. reading `Input.get()`](#cached-fields-vs-reading-inputget)

---

## Changes to `beast-base`

`beast-base` is the sampler and the models. Every analysis runs it and every package builds
on it, so a change there has to clear a higher bar than a change to `beast-fx` or to a package.

A pull request that changes anything under `beast-base/src/main` must answer three questions
in its description:

1. **Why can it not be done in `beast-fx` or in a package?** If the change is there to suit
   BEAUti, say so, and say why BEAUti cannot do the work itself.
2. **Does it touch code that runs on every MCMC step?** That means likelihoods, priors,
   operators, state nodes, `store()`/`restore()`, `requiresRecalculation()` and anything they
   call. If it does, give a timing before and after.
3. **Does it change XML or an API that packages extend?** That means input names, defaults,
   class names, and public or protected methods. If it does, say what happens to existing
   XML files and existing packages.

The pull request template asks these questions. `.github/CODEOWNERS` makes a core developer
a required reviewer for `beast-base/src/main`.

Changes to tests, examples and documentation under `beast-base` are not covered by this rule.

---

## Cached fields vs. reading `Input.get()`

BEAST classes often copy their inputs into fields, usually in `initAndValidate()`. This section
describes how the non-deprecated classes in `beast-base` (mostly `beast.base.spec`) keep those
copies valid today, so that new code fits in with them.

> **The rule in one line**
> The `Input` is the source of truth. A cached field is a fast copy for code that runs on
> every MCMC step. Any method that can be called outside that loop, or after the model
> changed, reads the input or calls `refresh()` first.

> **This convention is fragile.** It is correct only if every method remembers to refresh
> before it uses a cached field, and nothing enforces that. Several bugs came from a method
> that did not (PR #233). Issue #252 is about replacing it with a design that cannot go
> stale.

> **MCMC speed comes first.** Code in `beast-base` that runs on every MCMC step must not be
> made slower to suit BEAUti. Where BEAUti changes a model after `initAndValidate()`, it is
> BEAUti's job to bring the object up to date (for example by calling `initAndValidate()`
> again), not the job of the hot path to re-read its inputs. Outside the hot path, prefer
> reading `Input.get()` to caching: a method that reads its inputs cannot go stale. A change
> to a method that runs on every step needs a timing check.

The model *can* change after `initAndValidate()`. BEAUti, for example, creates a new prior
from a template (which initialises it) and only then attaches the parameter, and it swaps
the inner distribution of a wrapper without initialising the wrapper again.

---

### The four patterns

| | What is cached | Kept up to date by | Typical use |
|---|---|---|---|
| **A** | Settings that never change during a run | nothing (read once) | flags, options |
| **B** | Expensive derived objects | `refresh()`, called on use | Apache Commons distributions |
| **C** | Simple values used in the hot path | `requiresRecalculation()`, `store()`, `restore()` | a clock rate |
| **D** | nothing | (reads inputs every time) | questions about the model structure, such as `getArguments()` and bounds |

#### A. Settings read once in `initAndValidate()`

Use this for values that are fixed for the whole run. Values that can change are still read
from their inputs.

```java
// spec/evolution/speciation/YuleModel.java
public void initAndValidate() {
    conditionalOnRoot = conditionalOnRootInput.get();                // fixed: cache it
    conditionalOnOrigin = originHeightParameterInput.get() != null;
    ...
}
...
final double r = birthDiffRateParameterInput.get().get();            // can change: read the input
```

#### B. Expensive derived objects, rebuilt by `refresh()`

This is the main reason to cache: rebuilding an Apache distribution on every call is slow.
`refresh()` re-reads the inputs and rebuilds the object if a value changed. Every
method that needs the object calls `refresh()` first.

The existing distributions compare the old and new values with `isNotEqual`, which treats
values closer than `EPS = 1e-12` (absolute) as equal. A change smaller than that does not
rebuild the object.

```java
// spec/inference/distribution/Uniform.java
public void refresh() {
    double lower = (lowerInput.get() != null) ? lowerInput.get().get() : 0.0;
    double upper = (upperInput.get() != null) ? upperInput.get().get() : 1.0;
    if (isNotEqual(dist.getSupportLowerBound(), lower)
            || isNotEqual(dist.getSupportUpperBound(), upper)) {
        dist = UniformContinuousDistribution.of(lower, upper);       // rebuild only on change
    }
}

protected UniformContinuousDistribution getApacheDistribution() {
    refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
    return dist;
}
```

Most classes that define `getApacheDistribution()` call `refresh()` this way.
`Dirichlet.refresh()` does the same for its `gammas` array.

**Wrapper distributions** (`IID`, `OffsetReal`, `OffsetInt`, `TruncatedReal`, ...) cache the
distribution they wrap. Their `refresh()` must **reload it from the input** and then refresh it:

```java
// spec/inference/distribution/OffsetReal.java (same in TruncatedReal and IID)
public void refresh() {
    dist = distributionInput.get();   // reload: the inner distribution may have been replaced
    dist.refresh();
    offset = offsetInput.get();
}
```

#### C. Simple values kept in step with `store()` / `restore()`

Use this for a number read in the hot path, kept in step at the `CalculationNode` hooks.

```java
// spec/evolution/branchratemodel/StrictClockModel.java
public void initAndValidate() {
    muParameter = meanRateInput.get();
    if (muParameter != null) mu = muParameter.get();
}
public boolean requiresRecalculation() { mu = muParameter.get(); return true; }
protected void store()   { mu = muParameter.get(); super.store(); }
protected void restore() { mu = muParameter.get(); super.restore(); }
```

#### D. Questions about the model structure read the inputs

`getArguments()`, `getConditions()` and the parameter bounds are called by other objects at
any time, including from BEAUti while the model is being edited. They must never return
a stale cached value.

```java
// spec/evolution/speciation/YuleModel.java
public List<String> getArguments() {
    arguments.add(treeInput.get().getID());
    ...
}

// spec/evolution/likelihood/TreeLikelihood.java
public List<String> getArguments() {
    return Collections.singletonList(dataInput.get().getID());
}

// spec/inference/util/RPNcalculator.java
for (final Tensor p : parametersInput.get()) arguments.add(((BEASTInterface) p).getID());
```

Bound getters either read the input directly or go through `refresh()` (pattern B):

```java
// spec/inference/distribution/Uniform.java: read the input directly
public Double getLower() {
    return (lowerInput.get() != null) ? lowerInput.get().get() : 0.0;
}

// spec/inference/distribution/ScalarDistribution.java: default for Normal, Gamma, ...
public T getLowerBoundOfParameter() {
    Object dist = getApacheDistribution();   // calls refresh()
    ...
}

// spec/inference/distribution/IID.java: wrapper, refresh() then use the cache
public T getLowerBoundOfParameter() {
    refresh();
    return dist.getLowerBoundOfParameter();
}
```

---

### Checklist for a new class

- [ ] Fields set in `initAndValidate()` are used only in the hot path (`calculateLogP`,
      `calcLogP`, `sample`, `getRateForBranch`, ...), or hold settings that never change (A).
- [ ] Every expensive derived object has a `refresh()` that re-reads the inputs, and every
      method using the object calls `refresh()` first (B).
- [ ] A wrapper's `refresh()` reloads the wrapped object from its input (`dist = distInput.get()`),
      not only `dist.refresh()`.
- [ ] Simple hot-path values are kept in step in `requiresRecalculation()`, `store()` and `restore()` (C).
- [ ] `getArguments()`, `getConditions()` and `get*BoundOfParameter()` read inputs or call
      `refresh()`. They never rely on a field that only `initAndValidate()` sets (D).
- [ ] Do **not** guard `refresh()` with `if (dist == null)`. That runs it only once, so the
      cache goes stale when an input is replaced.

---

### Examples of what goes wrong

PR #233 fixed seven cases in the distributions where a cached field was used without being
brought up to date; `BoundsAfterInitTest` in `beast-base` shows each of them. The places
that still differ from the checklist above are listed in issue #252.
