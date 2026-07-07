---
name: asapp-improve-changelog
description: >
  Fetches a GitHub Release's notes, applies editorial improvements (merge duplicate entries, drop
  noise, tighten phrasing, normalize tone), and updates the release after confirmation, optionally
  targeting a specific version.
  Use when the user wants to improve, clean up, polish, or tidy a GitHub Release's changelog or release notes.
  Triggers: /asapp-improve-changelog, improve changelog, clean up release notes, polish the changelog, tidy release notes.
  Do NOT use to create or publish a release (use asapp-release), or to edit a CHANGELOG file in the
  repo — this edits an existing GitHub Release's notes only.
---

# Improve Changelog

Fetches the release notes of a GitHub Release, applies AI editorial improvements (merge duplicates, remove noise, improve phrasing), and updates the release after confirmation.

## Usage

- `/asapp-improve-changelog` — improves the latest release
- `/asapp-improve-changelog v0.2.0` — improves a specific release

## Steps

### Step 0: Set up progress tracking

**Before any other step**, create these five tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when it's done:

1. Check gh availability (Step 1)
2. Detect the version (Step 2)
3. Get the release notes (Step 3)
4. Apply the improvement rules (Step 4)
5. Confirm and apply the update (Step 5)

If the command aborts (gh missing, or the user declines at the confirmation gate), leave the current task `in_progress` so the stopping point stays visible.

### Step 1: Check gh availability

```bash
gh --version
```

If the command fails or `gh` is not found, abort immediately with:

```
Error: This command requires the GitHub CLI (gh).
Install it from https://cli.github.com, authenticate with `gh auth login`, and re-run.
```

### Step 2: Detect version

If a version argument was provided, it must be `v`-prefixed (e.g. `v0.2.0`) — release tags follow the `v[0-9].*` pattern in `.github/cliff.toml`. If the user supplies one without the `v` (e.g. `0.2.0`), prepend it before use. Use the resulting tag directly.

Otherwise, fetch the latest release tag:

```bash
gh release list --limit 1 --json tagName -q '.[0].tagName'
```

If no releases exist (empty result), abort with:

```
Error: No releases found in this repository.
```

### Step 3: Get release notes

```bash
gh release view <tag> --json body -q '.body'
```

If the tag doesn't exist or the command fails, abort with:

```
Error: Release <tag> not found.
```

### Step 4: Apply improvements

Apply the [Improvement Rules](#improvement-rules) to the fetched content.

### Step 5: Confirm and apply

Display the improved changelog and summarize what changed. Ask for confirmation:

```
Proceed to update the GitHub Release? [y/N]
```

Only continue if the user confirms.

Write the improved content to `.github/changelog-draft.md` — a temporary working file; never commit it — then run:

```bash
gh release edit <tag> --notes-file .github/changelog-draft.md
```

If this command fails (network or auth error), abort and report the failure. Leave `.github/changelog-draft.md` in place — do not delete it — so no work is lost.

After a successful update, delete the working file:

```bash
rm .github/changelog-draft.md
```

Confirm success and print the release URL.

---

## Improvement Rules

**Merge** — combine entries that cover the same change or topic across multiple commits into a single clearer entry, even when the source entries sit in different sections.
- Preserve all commit links from the merged entries, listed together at the end: `([`abc1234`](url1), [`def5678`](url2))`.
- When merge sources span sections, file the combined entry under whichever original section is more impactful (Breaking Changes > New Features > Bug Fixes > Documentation > Upgrades > Others); this decides only where the one merged entry lands and does not reorder the sections themselves.
- Breaking Changes entries are never a merge source or target — leave them untouched.

**Remove** — drop entries with no user-facing value:
- Internal refactors with no behavioral change visible to users
- Typo, comment, or formatting fixes
- Repetitive entries that say the same thing as another entry
- Commits already implied by a merged entry

**Remove empty sections** — if all entries in a section are removed or merged away, drop the section header too.

**Rewrite** — improve clarity of terse or ambiguous messages. Use plain language. Keep entries concise (one line preferred).

**Normalize tone** — all entries must use imperative form (`Add`, `Fix`, `Update`), not past tense (`Added`, `Fixed`) or third person (`Adds`, `Fixes`).

**Normalize scope casing** — scopes must be lowercase (e.g. `**authentication:**` not `**Authentication:**`).

**Reorder entries within a section** — within each section, place the most user-facing or impactful entries first.

**Example** (merge + reorder):

Before:
```
## ✨ New Features
- Adds a health check endpoint for the tasks service ([`aaa1111`](url1))
- **auth:** Adds a JWT refresh endpoint ([`bbb2222`](url2))
- **auth:** Adds refresh token rotation on top of the new refresh endpoint ([`ccc3333`](url3))
```

After:
```
## ✨ New Features
- **auth:** Add JWT refresh endpoint with token rotation ([`bbb2222`](url2), [`ccc3333`](url3))
- Add a health check endpoint for the tasks service ([`aaa1111`](url1))
```

The two `auth` entries merge into one (their commit links combine), the merged entry moves ahead of the less-impactful health-check entry, and both are normalized to imperative tone.

**Preserve always**:
- Section headers and their icons, exactly as they appear in the source notes (these mirror the commit group config in `.github/cliff.toml`)
- All commit links — keep every link, including all links from merged entries
- All entries under ⚠️ Breaking Changes — never remove, merge, or reorder these
- The overall markdown structure of the release notes

**Do not**:
- Invent information not present in the original
- Change the meaning of any entry
- Reorder sections

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Starting Step 1 before creating the five tracking tasks | Do Step 0 first — create the tasks, then begin. |
| Updating the release before showing the improved version | Display the result and wait for confirmation at Step 5. |
| Removing, merging, or reordering Breaking Changes entries | Preserve every ⚠️ entry untouched. |
| Dropping commit links when merging entries | Keep every link; list the merged links together at the end. |
| Inventing content not in the original notes | Only rewrite what is already there. |
| Leaving `.github/changelog-draft.md` behind | Delete the working file after a successful update. |
