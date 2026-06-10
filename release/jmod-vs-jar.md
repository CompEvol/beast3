# `.jmod` vs `.jar` — Differences and Benefits

## What is `.jmod`?

`.jmod` is a packaging format introduced in **Java 9** as part of the Java Platform Module System (JPMS).
It is a ZIP archive with a structured layout, designed for use with `jlink` and `jpackage` to assemble
custom, self-contained runtime images. It is **not** a drop-in replacement for `.jar` — both serve
different roles in the Java ecosystem.

---

## Internal Structure

### `.jar`
```
mylib.jar
├── com/example/MyClass.class
├── META-INF/MANIFEST.MF
└── resources/
```

### `.jmod`
```
javafx.graphics.jmod
├── classes/            ← compiled bytecode + module-info.class
│   └── module-info.class
├── lib/                ← native shared libraries
│   ├── libjavafx_font.dylib   (macOS)
│   ├── libjavafx_font.so      (Linux)
│   └── javafx_font.dll        (Windows)
├── bin/                ← native executables
├── conf/               ← configuration files
├── legal/              ← per-module licenses and notices
└── man/                ← man pages
```

---

## Side-by-Side Comparison

| Feature | `.jar` | `.jmod` |
|---|---|---|
| Bytecode / classes | Yes | Yes (`classes/`) |
| Native libraries | Manual hack (extract at runtime) | First-class (`lib/`) |
| Native executables | No | Yes (`bin/`) |
| Config files | Ad-hoc | Standardised (`conf/`) |
| Per-module legal notices | No | Yes (`legal/`) |
| Used on runtime classpath | Yes | **No** |
| Used on compile-time module-path | Yes | Yes |
| Used by `jlink` | No | **Yes** |
| Used by `jpackage` | No | **Yes** |
| Explicit dependency declaration | Optional (`MANIFEST.MF`) | Enforced (`module-info.class`) |
| Encapsulation enforcement | Convention only | JVM-enforced |
| Link-time optimisation | None | Full plugin pipeline |
| Cross-module integrity hashing | No | Yes (`--hash-modules`) |

---

## Benefits of `.jmod` Over `.jar`

### 1. Native Library Bundling

JARs have no standardised place for native code. The old workaround was extracting `.so`/`.dylib`/`.dll`
at runtime into temp directories — fragile and platform-specific.

`.jmod` carries native libs in `lib/` alongside bytecode. `jlink` extracts them correctly for the target
platform with no manual intervention.

```
javafx.graphics.jmod/lib/
├── libjavafx_font.dylib
└── libjavafx_iio.dylib
```

---

### 2. Custom Minimal Runtime Images with `jlink`

`jlink` reads `.jmod` files and assembles a **trimmed JRE** containing only the modules your app needs:

```bash
jlink \
  --module-path $JAVA_HOME/jmods \
  --add-modules javafx.controls,javafx.fxml,java.logging \
  --output my-runtime/
```

Typical result: **200 MB JDK → 40–60 MB custom runtime**. This is impossible with plain JARs on a
classpath because `jlink` has no module boundary information to determine what is safe to remove.

---

### 3. Explicit, Enforced Dependency Graph

Every `.jmod` contains a `module-info.class` compiled from a `module-info.java`:

```java
module javafx.controls {
    requires javafx.base;
    requires javafx.graphics;
    exports javafx.scene.control;
    // everything else is encapsulated
}
```

- Missing dependencies are a **hard error at link time**, not a `NoClassDefFoundError` at runtime.
- Circular dependencies are **forbidden** by the module system and caught at build time.
- Tooling can compute the full transitive closure without classpath scanning.

---

### 4. Strong Encapsulation

Packages not listed in `exports` are **inaccessible** to other modules — enforced by the JVM, not just
convention. With JARs, all public classes are visible to everything on the classpath. This is why
internal JDK APIs like `sun.misc.Unsafe` were so easily misused: `.jmod` makes that impossible without
an explicit `--add-opens` flag.

---

### 5. Link-Time Optimisations

Because `jlink` processes `.jmod` files with full module graph knowledge, it can apply optimisations
impossible with a dynamic classpath:

- Dead code elimination — remove unreferenced classes and methods
- Bytecode compression — pack `lib/modules` as a single optimised binary blob
- Class data sharing — pre-compute class metadata
- Custom plugin pipeline — `jlink` has a plugin API for additional link-time transforms

---

### 6. Self-Contained App Bundles with `jpackage`

`jpackage` calls `jlink` internally and reads `.jmod` files to produce a hermetic bundle:

```
BEAST.app/
└── Contents/
    └── runtime/
        ├── lib/modules             ← all bytecode, single binary blob
        ├── lib/libjavafx_font.dylib
        └── bin/java
```

The end user needs **no JRE installed**. The exact module set, native libs, and config are frozen at
package time. With JARs this required manual native lib handling and installer scripts.

---

### 7. Integrity Verification via Hashing

The `--hash-modules` step records the hash of all dependent modules inside the `.jmod`. At link time,
`jlink` verifies these hashes and rejects any tampered or mismatched modules. JARs have no equivalent
cross-JAR integrity mechanism.

---

## When to Use Each

| Use case | Use |
|---|---|
| Library dependency at compile/runtime | `.jar` |
| Publishing to Maven Central | `.jar` |
| Building a platform module (JDK-style) | `.jmod` |
| Bundling native libraries with bytecode | `.jmod` |
| Creating a minimal custom JRE (`jlink`) | `.jmod` |
| Packaging a self-contained app (`jpackage`) | `.jmod` |

`.jmod` is not a general replacement for `.jar`. JARs remain the standard for library distribution.
`.jmod` is specifically designed for **platform module packaging and runtime image construction**,
solving problems that JARs were never built to handle.
