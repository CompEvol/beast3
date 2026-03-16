# Releasing BEAST 3 to Maven Central

Pushing a `v*` tag triggers the `release-central.yml` workflow, which
derives the Maven version from the tag, builds, tests, GPG-signs, and
auto-publishes to Maven Central.

## Pre-tag checklist

1. **Version strings** — update to match the release (e.g. `2.8.0`):

   | File | Field |
   |------|-------|
   | `version.xml` | `version="..."` |
   | `beast-base/src/main/java/beast/base/core/BEASTVersion2.java` | `VERSION` |
   | `beast-pkgmgmt/src/main/java/beast/pkgmgmt/BEASTVersion.java` | `VERSION` |

   The POM versions (`2.8.0-SNAPSHOT`) are set automatically by the
   workflow — do **not** change them manually.

2. **README.md** — update the dependency `<version>` examples.

3. **release/common/VERSION HISTORY.txt** — add release notes.

4. **Commit** the above changes to master.

## Tag and release

```bash
git tag v2.8.0-beta1
git push origin v2.8.0-beta1
```

Monitor the workflow run at:
https://github.com/CompEvol/beast3/actions/workflows/release-central.yml

## After release

Verify the artifacts appear on Maven Central:
https://central.sonatype.com/namespace/io.github.compevol

## Secrets required (GitHub Actions)

| Secret | Description |
|--------|-------------|
| `GPG_PRIVATE_KEY` | `gpg --armor --export-secret-keys <KEY_ID>` |
| `GPG_PASSPHRASE` | Passphrase for the GPG key |
| `CENTRAL_USERNAME` | Sonatype Central Portal token username |
| `CENTRAL_TOKEN` | Sonatype Central Portal token password |

Generate Sonatype tokens at https://central.sonatype.com/account
