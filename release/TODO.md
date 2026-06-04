# Release TODO

## Testing Linux and Windows Bundles from macOS

No code changes are needed. Two options are available.

---

### Option 1 — `workflow_dispatch` (recommended)

Go to **GitHub → Actions → Release Bundles → Run workflow**, enter a plain
version such as `2.8.0`, and click Run.

- All three jobs run on the real CI runners (ubuntu-latest, ubuntu-24.04-arm,
  windows-latest).
- Bundles are uploaded as **Actions artifacts**, downloadable from the run page
  for 90 days.
- **No GitHub Release is created** — the release-upload steps are guarded by
  `if: startsWith(github.ref, 'refs/tags/v')`, which is false for
  `workflow_dispatch`.

The version input must be a plain `x.y.z` number — no `v` prefix, no
`-SNAPSHOT` suffix. `v2.8.0` would be passed verbatim to `mvn versions:set`,
which Maven rejects.

---

### Option 2 — Push a test tag

Use an obviously-test all-numeric version so Windows jpackage accepts it:

```bash
git tag v99.0.0
git push origin v99.0.0
```

This fires the full pipeline **including** GitHub Release creation. Clean up
afterwards with:

```bash
gh release delete v99.0.0 --cleanup-tag --yes
```

`--cleanup-tag` deletes the remote git tag at the same time. Without it the
tag remains and would interfere with future runs.

Do **not** use suffixes such as `v2.8.0-rc1` — the `-rc1` part is passed
to `jpackage --app-version` and Windows jpackage requires a purely numeric
version (`x.y.z`), so the Windows job would fail.

---

### Comparison

| | `workflow_dispatch` | Test tag |
|---|---|---|
| GitHub Release created | No | Yes — must clean up |
| Full pipeline tested | Build + bundle + artifact upload | Build + bundle + artifact upload + release upload |
| Cleanup needed | None | `gh release delete v99.0.0 --cleanup-tag --yes` |
| Recommended for | Day-to-day testing | Verifying the release upload step |
