# BEAST 2 examples (`legacy/`)

The XML and JSON files here are **BEAST 2** examples (from BEAST 2.7.8). They are **not converted** to
BEAST 3, and they are **not released**.

They must still **run in BEAST 2.8.x** (BEAST 3), which keeps the old BEAST 2 classes for them. Many of
those classes are marked `@Deprecated`; that is expected here, and they must keep working with these files.
The changelog announces that these legacy classes are removed in **3.0.0**; until then, these files are
the check that they still work.

## What they are for

- **Backward compatibility:** BEAST 2.8 must load BEAST 2 XML.
  `ExampleXmlParsingTest.test_ThatLegacyXmlExamplesParse` parses every file directly in this folder.
- **Source of the BEAST 3 examples:** each file here that has been migrated has its BEAST 3 conversion at
  the same relative path one level up: `examples/legacy/<path>` → `examples/<path>`.
- **Comparison:** a BEAST 3 example and its BEAST 2 source should give the same posterior (compared
  against BEAST 2.7.9).

## Rules

- Do not convert these files to BEAST 3 classes. Keep them BEAST 2 XML.
- A model change made to a BEAST 3 example (for example a proper prior instead of an improper one) is
  made here as well, so that the two versions stay comparable.
- A file that needs beast-fx classes (for example `beastfx.app.seqgen`) is in
  `beast-fx/examples/legacy/`, checked by beast-fx's `ExampleXmlParsingTest`.

## Subfolders

Only the files directly in this folder are parsed by the test. Some subfolders do not load in BEAST 2.8:

| Folder | Why not |
|---|---|
| `benchmark/1/` | BEAST 1 XML (BEAUti v1.5) |
| `benchmark/II/` | early BEAST 2 XML that uses `TreeHeightLogger`, removed in BEAST 3 (use `TreeStatLogger`) |
| `starbeastinit/` | the short name `Prior` is not in their namespace |
| `parameterised/` | `RSV2.xml` needs `-D` / `-DF` arguments; `RSV2.json` is its data file |
