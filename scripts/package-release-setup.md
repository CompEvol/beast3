# Setting up Maven Central releases for a BEAST 3 package

This guide walks third-party package developers through configuring a GitHub
Actions workflow that publishes their BEAST 3 package to Maven Central
when a `v*` tag is pushed.

It assumes your package is already structured per the gold standard
(single-module Maven project, JPMS `module-info.java`, `version.xml`,
assembly descriptor, `release.sh`, `ci-publish.yml`). If not, start from
[beast-package-skeleton](https://github.com/CompEvol/beast-package-skeleton)
or use [sampled-ancestors](https://github.com/CompEvol/sampled-ancestors)
or [morph-models](https://github.com/CompEvol/morph-models) as a template.

## First decision: which namespace?

Maven Central artifacts live under a verified namespace (the `groupId`).
Picking the right one upfront avoids redoing the Sonatype dance.

| Your situation | Recommended `groupId` | What you need |
|---|---|---|
| Repo lives in **CompEvol** org | `io.github.compevol` | Nothing — org-level secrets already configured. Skip to "Wire up the workflow". |
| Repo lives in **BEAST2-Dev** org | `io.github.beast2-dev` | Nothing — org-level secrets already configured. Skip to "Wire up the workflow". |
| Repo lives in **your own** GitHub org or personal account | `io.github.<your-username>` (or any namespace you control) | You'll set up Sonatype + GPG yourself (steps 1–4 below). |

**If your `pom.xml` says `io.github.compevol` but your repo is on your
personal account or another org**, you have two options:

1. **Transfer the repo to CompEvol** — simplest. The repo inherits org-level
   Sonatype/GPG secrets and a tag push publishes automatically. Contact
   Alexei Drummond to arrange.
2. **Change the `groupId`** to a namespace you control (e.g. `io.github.<you>`)
   and follow the full setup below. Note that anyone consuming your artifact
   will need to update their dependency coordinates.

## Step 1: Sonatype Central account

1. Register an account at [central.sonatype.com](https://central.sonatype.com/).
2. Verify your namespace:
   - For `io.github.<username>`, follow the GitHub-based verification flow:
     create a temporary public repo named after the verification code Sonatype
     gives you, then click "Verify" on the namespace page.
   - For a custom domain (e.g. `org.example`), follow the DNS TXT record flow.
3. Generate a publishing token at
   [central.sonatype.com/account](https://central.sonatype.com/account).
   Save the **username** and **password** strings — these become
   `CENTRAL_USERNAME` and `CENTRAL_TOKEN`.

## Step 2: GPG signing key

Maven Central requires every artifact to be GPG-signed.

```bash
# Generate a 4096-bit RSA key (accept defaults; use a strong passphrase)
gpg --full-generate-key

# List keys to find the long key ID
gpg --list-secret-keys --keyid-format=long
# Look for a line like: sec   rsa4096/ABCDEF1234567890

# Publish the public key to a key server (Maven Central looks it up here)
gpg --keyserver keys.openpgp.org --send-keys ABCDEF1234567890
gpg --keyserver keyserver.ubuntu.com --send-keys ABCDEF1234567890

# Export the private key in armored form for the GitHub secret
gpg --armor --export-secret-keys ABCDEF1234567890 > gpg-private-key.asc
```

Treat `gpg-private-key.asc` like a credential — paste it into the GitHub
secret (next step), then delete the file.

## Step 3: Add the four GitHub secrets

In your repo, go to **Settings → Secrets and variables → Actions → New
repository secret** and add:

| Secret name | Value |
|---|---|
| `CENTRAL_USERNAME` | Sonatype token username (from Step 1) |
| `CENTRAL_TOKEN` | Sonatype token password (from Step 1) |
| `GPG_PRIVATE_KEY` | Contents of `gpg-private-key.asc` (the full armored block, including BEGIN/END lines) |
| `GPG_PASSPHRASE` | Passphrase for your GPG key |

If your repo is in an org you control, you can set these as **organisation
secrets** instead — every repo in the org inherits them. This is what
CompEvol and BEAST2-Dev do.

## Step 4: Wire up the workflow

The gold-standard `.github/workflows/ci-publish.yml` (copy from
[sampled-ancestors](https://github.com/CompEvol/sampled-ancestors/blob/master/.github/workflows/ci-publish.yml))
runs on every push and PR (build + test), and additionally publishes to
Maven Central when a `v*` tag is pushed:

```yaml
on:
  push:
    branches: [ master ]
    tags: [ 'v*' ]
  pull_request:
    branches: [ master ]
```

Key behaviours:

- The release job uses `actions/setup-java` with `server-id: central` —
  this writes the `CENTRAL_USERNAME` / `CENTRAL_TOKEN` env vars into
  `~/.m2/settings.xml` automatically.
- `mvn versions:set -DnewVersion=$VERSION` strips the `v` from the tag
  (so tag `v1.3.0-beta1` publishes as version `1.3.0-beta1`). You do
  **not** need to bump the `<version>` in `pom.xml` before tagging —
  leave it as `X.Y.Z-SNAPSHOT` during development.
- `mvn deploy -Prelease -DskipTests` activates the `release` profile,
  which adds javadoc + sources JARs, GPG-signs everything, and uploads
  to Maven Central via the `central-publishing-maven-plugin`.

## Step 5: Pre-tag checklist

Before tagging a release:

1. **`version.xml`** — update `version="..."` to match the tag you're about
   to push (e.g. `1.3.0-beta1`). The CI workflow only updates the POM
   version from the tag, not `version.xml`.
2. **`README.md`** — update Maven coordinate examples if the version is
   referenced.
3. **Commit** these changes to `master`.
4. **Verify CI passes on master** (the build-and-test job should be green).

## Step 6: Tag and release

```bash
git tag v1.3.0-beta1
git push origin v1.3.0-beta1
```

Watch the workflow at:
`https://github.com/<org>/<repo>/actions/workflows/ci-publish.yml`

If the publish step succeeds, the artifact appears at:
`https://central.sonatype.com/artifact/<groupId>/<artifactId>/<version>`
within ~15 minutes (usually faster).

## Step 7: After the release

- **Verify on Maven Central**: search for your artifact at
  [central.sonatype.com](https://central.sonatype.com/).
- **CBAN entry** (optional but recommended for BEAUti discoverability):
  add or update your package entry in
  [CompEvol/CBAN/packages2.8.xml](https://github.com/CompEvol/CBAN/blob/master/packages2.8.xml).
  Use the `release.sh --release` script to build a ZIP and create a GitHub
  release; the script prints the CBAN XML snippet for you.

## Troubleshooting

**Publish step fails with "401 Unauthorized"**
The `CENTRAL_USERNAME` / `CENTRAL_TOKEN` secrets are missing or wrong, or
the token has expired. Regenerate at
[central.sonatype.com/account](https://central.sonatype.com/account) and
update the secrets.

**Publish step fails with "namespace not verified"**
Your `groupId` does not match a namespace you've verified on Sonatype.
Either change the `groupId` to a verified namespace, or verify the namespace
you're trying to use. If the `groupId` is `io.github.compevol` or
`io.github.beast2-dev`, the repo must be in the matching org (or have those
orgs' secrets) — only those orgs are verified for those namespaces.

**Publish step fails with GPG errors**
Check that `GPG_PRIVATE_KEY` includes the full armored block (`-----BEGIN
PGP PRIVATE KEY BLOCK-----` through `-----END PGP PRIVATE KEY BLOCK-----`)
and that `GPG_PASSPHRASE` is correct. Verify the public key is on
`keys.openpgp.org` (Sonatype looks it up there during validation).

**Publish step fails with "Component validation failed: missing javadoc/sources"**
The `release` profile in your `pom.xml` is missing the
`maven-javadoc-plugin` or `maven-source-plugin`. Compare with
[sampled-ancestors/pom.xml](https://github.com/CompEvol/sampled-ancestors/blob/master/pom.xml).

**Tag pushed but no workflow ran**
Tag must match the `v*` pattern (`v1.0.0`, `v1.0.0-beta1`, etc.). Check
that `ci-publish.yml` is on the commit the tag points to.

## Related documentation

- [BEAST 3 core release process](../RELEASING.md) — how to release
  beast3 itself (different procedure; manages POM versions automatically).
- [beast-package-skeleton](https://github.com/CompEvol/beast-package-skeleton)
  — minimal template repo with `ci-publish.yml` and `release.sh` already in place.
- [migration-guide.md](migration-guide.md) — for migrating an existing
  BEAST 2 package to BEAST 3.
