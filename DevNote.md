# BEAST 3 Developer Notes

Notes for developers working on the BEAST 3 code base: conventions and lessons learnt
that the code alone does not make obvious. Add new topics as new sections.

## Contents

1. [Cached fields vs. reading `Input.get()`](#cached-fields-vs-reading-inputget)

---

## Cached fields vs. reading `Input.get()`

BEAST classes often copy their inputs into fields, usually in `initAndValidate()`. This section
explains when to use the cached field and when to read the input, based on the
non-deprecated classes in `beast-base` (mostly `beast.base.spec`).

> **The rule in one line**
> The `Input` is the source of truth. A cached field is a fast copy for code that runs on
> every MCMC step. Any method that can be called outside that loop, or after the model
> changed, reads the input or calls `refresh()` first.

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
`refresh()` re-reads the inputs and rebuilds the object only if a value changed. Every
method that needs the object calls `refresh()` first.

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

16 of the 19 classes that define `getApacheDistribution()` call `refresh()` this way.
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

### Mistakes fixed for issue #230

These were found while fixing the BEAUti prior refresh bugs (PR #233). In each case, BEAUti
changed the model after `initAndValidate()` and the parameter's bounds ignored the change.

| Class | Problem | Fix |
|---|---|---|
| `TensorDistribution.getArguments()` | read the cached `param`, which is `null` when the param is attached after `initAndValidate()` | read `paramInput.get()` (D) |
| `IID.refresh()` | called `dist.refresh()` without reloading `dist` from `distInput` | reload `dist = distInput.get()` first (B) |
| `IID.get*BoundOfParameter()` | used the cached `dist` without refreshing | call `refresh()` first (B) |
| `OffsetReal` / `OffsetInt` `get*BoundOfParameter()` | called `refresh()` only `if (dist == null)` | always call `refresh()` (B) |
| `TruncatedReal` / `TruncatedInt` `get*BoundOfParameter()` | used `getInnerDistribution()`, which calls `refresh()` only `if (dist == null)` | always call `refresh()` (B) |
| `TruncatedInt.getUpperBoundOfParameter()` | returned `Math.max(lower, inner upper)`, ignoring the truncation upper | `Math.min(upper, inner upper)` |
| `TruncatedReal.getUpperBoundOfParameter()` | checked the inner *lower* bound for `null` | check the inner upper bound |

Tests: `beast-base/src/test/java/beast/base/spec/inference/distribution/BoundsAfterInitTest.java`.

#### Known places that still differ (not changed yet)

- `TruncatedReal` / `TruncatedInt` `getInnerDistribution()` still use the `if (dist == null) refresh()`
  guard. Their `calculateLogP()` and `calcLogP()` call `refresh()` themselves, so this only matters
  when `getInnerDistribution()` is called from elsewhere after an input was replaced.
- `OffsetReal` / `OffsetInt` `getApacheDistribution()` also use that guard, and their `calculateLogP()`
  and `calcLogP()` do **not** call `refresh()`. They use the cached `dist` and `offset`, so a replaced
  inner distribution or offset is ignored there until something else calls `refresh()`.
