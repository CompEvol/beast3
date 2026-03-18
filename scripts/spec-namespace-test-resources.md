# Specification: Namespace Test Resources to Avoid JPMS Split Package Errors

## Problem

When two BEAST modules are loaded in the same JPMS boot layer (as happens in
Eclipse when developing a package alongside beast3), any top-level directory
that appears in `target/test-classes/` of both modules creates a "split
package" error:

```
Error occurred during initialization of boot layer
java.lang.LayerInstantiationException: Package examples in both module beast.fx and module my.beast.example
```

Eclipse patches `target/test-classes/` into each module via `--patch-module`,
so directories like `examples/` and `fxtemplates/` are treated as Java
packages. If two modules both contain one of these directories, JPMS rejects
the configuration because the same package cannot belong to two modules.

Currently affected directories:

| Directory      | Appears in                                                         |
|----------------|--------------------------------------------------------------------|
| `examples/`    | beast-base, beast-fx (copied from beast-base), and any BEAST package that ships example XMLs |
| `fxtemplates/` | beast-fx (copied from main resources to test-classes by pom.xml)   |

## Convention

Use `<module.name>/<resource>/` as the top-level directory for test resources.
This matches the pattern already established by beast-fx's main resources
(`beast.fx/fxtemplates/`), and is consistent across all modules. Since each
module exclusively owns its JPMS module name, no two modules can collide.

## Changes

### beast-base

**1. Move test resources directory:**

```
src/test/resources/examples/   →   src/test/resources/beast.base/examples/
```

All contents (XMLs, subdirectories `nexus/`, `beast2vs1/`, `spec/`, `fasta/`,
`benchmark/`, `parameterised/`, `starbeastinit/`, `testDirichlet/`) move
unchanged.

**2. Update path references in test Java files:**

| File | Old path | New path |
|------|----------|----------|
| `test/beast/util/NexusParserTest.java:24` | `"/examples/nexus"` | `"/beast.base/examples/nexus"` |
| `test/beast/util/NexusParserTest.java:160` | `"/examples/nexus/Primates.nex"` | `"/beast.base/examples/nexus/Primates.nex"` |
| `test/beast/integration/XMLProducerTest.java:37` | `"/examples/nexus"` | `"/beast.base/examples/nexus"` |
| `test/beast/beast2vs1/TestFramework.java:28` | `"/examples/beast2vs1/"` | `"/beast.base/examples/beast2vs1/"` |
| `beast/base/spec/beast2vs1/TestFramework.java:35` | `"/examples/spec/beast2vs1/"` | `"/beast.base/examples/spec/beast2vs1/"` |

### beast-fx

**3. Update `pom.xml` resource copying:**

The `copy-examples-to-test-classes` execution currently copies
`examples/**` from beast-base into `target/test-classes/`. Update to copy
`beast.base/examples/**` instead (matching beast-base's renamed directory).

**4. Remove `copy-fxtemplates-legacy` execution from `pom.xml`:**

This execution copies `fxtemplates/` to `target/test-classes/fxtemplates/` —
a bare top-level directory that will collide with any package that has its own
`fxtemplates/`. Remove it. The `copy-fxtemplates-to-test-classes` execution
that copies to `target/test-classes/beast.fx/fxtemplates/` is already
module-namespaced and stays.

**5. Update `BeautiDoc.processTemplate()` to find module-namespaced templates:**

`processTemplate()` searches filesystem directories for templates using
`BeautiConfig.TEMPLATE_DIR` (`"fxtemplates"`). After removing the legacy
copy, it must also search `beast.fx/fxtemplates/`. Two changes:

- Main template search: add fallback `dirName/beast.fx/<fileName>` (note:
  `fileName` already includes `fxtemplates/` as prefix, so do not add
  `TEMPLATE_DIR` again).
- Sub-template scan: if `dirName/fxtemplates/` does not exist, try
  `dirName/beast.fx/fxtemplates/`.

**6. Update path references in test Java files:**

| File | Old path | New path |
|------|----------|----------|
| `test/beastfx/app/beauti/BeautiBase.java` | `"examples/nexus"` | `"beast.base/examples/nexus"` |
| `test/beastfx/integration/ExampleXmlParsingTest.java` (3 refs) | `"/examples"` | `"/beast.base/examples"` |
| `test/beastfx/integration/ExampleJSONParsingTest.java` (2 refs) | `"examples"` / `"/examples"` | `"beast.base/examples"` / `"/beast.base/examples"` |
| `test/beastfx/integration/ResumeTest.java` | `"/examples"` | `"/beast.base/examples"` |

The ~12 BEAUti test classes that extend `BeautiBase` inherit `NEXUS_DIR` and
need no changes themselves.

### beast-package-skeleton

**7. Move test resources directory:**

```
src/test/resources/examples/   →   src/test/resources/my.beast.example/examples/
```

**8. Update `src/assembly/beast-package.xml`:**

Update the `<directory>` from `src/test/resources/examples` to
`src/test/resources/my.beast.example/examples`. The `<outputDirectory>` stays
as `/examples` since the assembled ZIP is not a JPMS module.

**9. Update `README.md`:**

Document the namespacing convention and update the directory layout example.

## What does NOT change

- **`BeautiConfig.TEMPLATE_DIR`** (`"fxtemplates"`) — production constant for
  runtime template discovery. Stays as-is.
- **`BeautiDoc.java`** template loading — updated to also search
  `beast.fx/fxtemplates/` paths (see step 5).
- **`PackageHealthChecker.java`** — inspects installed package ZIPs on disk,
  not JPMS test-classes.
- **Production resources** — only test resources are affected.
- **`copy-fxtemplates-to-test-classes`** pom.xml execution — already copies
  to the module-namespaced path `beast.fx/fxtemplates/`.

## Verification

1. `mvn clean test` passes on all three modules (beast-pkgmgmt, beast-base,
   beast-fx).
2. In Eclipse with beast3 and beast-package-skeleton both in the workspace,
   launching BeastMain no longer produces a split package error.
3. The skeleton's `mypackage.xml` runs successfully with the skeleton's
   services discovered from the boot layer.
