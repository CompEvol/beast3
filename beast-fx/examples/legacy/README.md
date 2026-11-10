# beast-fx legacy XMLs: deprecated SiteModel in the sequence simulators

This folder holds two **BEAST 2** example XMLs that use the beast-fx sequence simulators:

| File | Uses |
|---|---|
| `testSeqGen.xml` | `beastfx.app.seqgen.SequenceSimulator` |
| `testSimulatedAlignment.xml` | `beastfx.app.seqgen.SimulatedAlignment` |

They are kept as BEAST 2 XML, so they are not converted to BEAST 3 and not released. They are parsed by
`testLegacyExamplesParse` in beast-fx's `ExampleXmlParsingTest`. The general rules for legacy examples
are in `beast-base/examples/legacy/README.md`.

## Status

| Version | Result |
|---|---|
| BEAST 2.7.9 | ✅ Both run. 4 replicates each completed. |
| BEAST 3, master | ✅ Both parse (`testLegacyExamplesParse` passes) after the fix below. |
| BEAST 3, 2.8.0-rc2 app (built 5 Nov) | ❌ Both fail while the XML is loaded. The app predates the fix. |

## The problem

### The error

```
Input 102: type mismatch for input siteModel
Provide type class beast.base.evolution.sitemodel.SiteModel, but expected class beast.base.spec.evolution.sitemodel.SiteModel
```

### Why

BEAST 3 has two `SiteModel` classes:

- **old (BEAST 2, deprecated):** `beast.base.evolution.sitemodel.SiteModel`, which these XMLs use
- **new (BEAST 3):** `beast.base.spec.evolution.sitemodel.SiteModel`

They are siblings: both extend `SiteModelInterface.Base`, but neither one extends the other.

Before the fix, the simulators' `siteModel` input accepted only the **new** class:

| File | Line | Code |
|---|---|---|
| `beast-fx/src/main/java/beastfx/app/seqgen/SequenceSimulator.java` | 26 | `import beast.base.spec.evolution.sitemodel.SiteModel;` |
| | 39 | `Input<SiteModel> m_pSiteModelInput` |
| `beast-fx/src/main/java/beastfx/app/seqgen/SimulatedAlignment.java` | 17 | the same import |
| | 30 | `Input<SiteModel> m_pSiteModelInput` |

When the XML gave the old class, `Input.java` (beast-base) rejected it. The check is
`theClass.isAssignableFrom(value.getClass())` at line 514, and line 523 throws the error above.

## The fix

Both simulators now declare the input with the shared parent type, so either `SiteModel` is accepted:

```java
import beast.base.evolution.sitemodel.SiteModelInterface;

final public Input<SiteModelInterface.Base> m_pSiteModelInput = new Input<>("siteModel", ...);
protected SiteModelInterface.Base m_siteModel;
```

- The same change is in `SequenceSimulator.java` and `SimulatedAlignment.java`.
- `m_siteModel` already had this type: `SiteModel.Base` resolved to the inherited
  `SiteModelInterface.Base`. Only the input got wider.
- The cast at `SequenceSimulator.java:304`, which reads the site model of a `TreeLikelihood`, uses
  the same type now. Before, it would have thrown a `ClassCastException` for an old site model.
- Commit 18b6fbf made the same kind of fix for the `branchRateModel` input.

### What was checked

- `testLegacyExamplesParse`, and the whole beast-fx `ExampleXmlParsingTest` (3 tests, including running
  the BEAST 3 examples), pass.
- All modules compile (`mvn test-compile`).
- Nothing else in beast3 reads `m_pSiteModelInput` or `m_siteModel` of these classes. Code that passes a
  BEAST 3 `SiteModel` still works, for example model-selection's `GenerateXMLs`, which uses `initByName`.
- An XML `<siteModel>` with no `spec` is not affected. The parser does not build the input's declared
  type, so the abstract type is never instantiated.

A package outside beast3 would break only if it does `SiteModel sm = simulator.m_pSiteModelInput.get();`
with the BEAST 3 `SiteModel` type. That now needs a cast. None was found in the local package repos.

## Notes on the XMLs

- **Full class name:** the site models are written as `spec='beast.base.evolution.sitemodel.SiteModel'`.
  The namespace includes `beast.base.spec.evolution.sitemodel`, so a plain `spec='SiteModel'` would
  pick the new BEAST 3 class, which BEAST 2.7.9 does not have.
- **Older 2.8.0-rc2 builds:** the app built on 5 Nov fails earlier in `testSeqGen`, on
  `branchRateModel`. Commit 18b6fbf fixed that on master.
- **Output file:** `testSimulatedAlignment.xml` writes `simulated_alignment.xml` to the current
  directory. Do not commit that file if it appears here: the test would try to parse it and fail.
