# Release

## Platform coverage

| Platform | Built by |
|---|---|
| Linux x86, Linux aarch64 | `release-bundles.yml` (CI) |
| Windows | `release-bundles.yml` (CI) |
| Mac DMG | `release/Mac/build-sign-dmg.sh` (local, requires Apple signing) |

## Release steps

1. Push a `v*` tag → `ci-publish.yml` publishes all modules to Maven Central.
2. Trigger `Release Bundles` via GitHub Actions UI. No inputs — version is read from `pom.xml` (`-SNAPSHOT` stripped).
3. Download bundles from the workflow run page (Artifacts section, 90-day retention).
4. Test locally, then upload to the GitHub Release alongside the Mac DMG.

## Artifacts produced by CI

| Artifact | File |
|---|---|
| `Linux.x86` | `BEAST.v<ver>.Linux.x86.tgz` |
| `Linux.aarch64` | `BEAST.v<ver>.Linux.aarch64.tgz` |
| `Windows` | `BEAST.v<ver>.Windows.zip` |

## Version constraints

Version is read from `pom.xml` and must be plain `x.y.z`. Windows jpackage rejects non-numeric suffixes (e.g. `-rc1`). Keep `pom.xml` at `x.y.z-SNAPSHOT`; the CI strips `-SNAPSHOT` automatically.
