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

Fetch a GitHub Release's notes, apply the editorial **Improvement rules**, and update the release after the user confirms.

## Usage

- `/asapp-improve-changelog` — improve the latest release.
- `/asapp-improve-changelog v0.2.0` — improve a specific release.

## Process

### 0. Set up progress tracking

**Before any other step**, create these five tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when it's done:

1. Check `gh` availability (Step 1)
2. Detect the version (Step 2)
3. Get the release notes (Step 3)
4. Apply the Improvement rules (Step 4)
5. Confirm and apply the update (Step 5)

If the command aborts (`gh` missing, or the user declines at the confirmation gate), leave the current task `in_progress` so the stopping point stays visible.

### 1. Check gh availability

```bash
gh --version
```

If the command fails or `gh` is not found, abort immediately with:

```
Error: This command requires the GitHub CLI (gh).
Install it from https://cli.github.com, authenticate with `gh auth login`, and re-run.
```

### 2. Detect version

If a version argument was provided, it must be `v`-prefixed (e.g. `v0.2.0`) — release tags follow the `v[0-9].*` pattern in `.github/cliff.toml`. If the user supplies one without the `v` (e.g. `0.2.0`), prepend it. Use the resulting tag directly.

Otherwise, fetch the latest release tag:

```bash
gh release list --limit 1 --json tagName -q '.[0].tagName'
```

If no releases exist (empty result), abort with:

```
Error: No releases found in this repository.
```

### 3. Get release notes

```bash
gh release view <tag> --json body -q '.body'
```

If the tag doesn't exist or the command fails, abort with:

```
Error: Release <tag> not found.
```

### 4. Apply improvements

Apply the **Improvement rules** (below) to the fetched content.

### 5. Confirm and apply

Display the improved changelog and summarize what changed, then ask:

```
Proceed to update the GitHub Release? [y/N]
```

Only continue on confirmation. Then write the improved content to `.github/changelog-draft.md` (a temporary working file) and update the release:

```bash
gh release edit <tag> --notes-file .github/changelog-draft.md
```

- On failure (network or auth error), abort and report it. Leave `.github/changelog-draft.md` in place so no work is lost.
- On success, delete the working file, then confirm success and print the release URL:

```bash
rm .github/changelog-draft.md
```

## Improvement rules

**Merge** — combine entries covering the same change or topic across commits into one clearer entry, even when the sources sit in different sections.
- Keep all commit links from the merged entries, listed together at the end: `([`abc1234`](url1), [`def5678`](url2))`.
- When merge sources span sections, file the combined entry under whichever source section is more impactful (New Features > Bug Fixes > Documentation > Upgrades > Others). This decides only where the merged entry lands — it does not reorder the sections.

**Remove** — drop entries with no user-facing value:
- Internal refactors with no behavioral change visible to users
- Typo, comment, or formatting fixes
- Entries that repeat what another entry already says
- Commits already implied by a merged entry

**Remove empty sections** — if every entry in a section is removed or merged away, drop its header too.

**Rewrite** — clarify terse or ambiguous messages in plain language; keep entries concise (one line preferred).

**Normalize tone** — imperative form (`Add`, `Fix`, `Update`), never past tense (`Added`, `Fixed`) or third person (`Adds`, `Fixes`).

**Normalize scope casing** — lowercase scopes (`**authentication:**`, not `**Authentication:**`).

**Reorder within a section** — most user-facing or impactful entries first.

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

The two `auth` entries merge into one (links combined), the merged entry moves ahead of the less-impactful health-check entry, and both are normalized to imperative tone.

## Guardrails

- **Confirm before writing** — never update the release before displaying the result and getting explicit confirmation (Step 5).
- **Never touch ⚠️ Breaking Changes** — no removing, merging, or reordering; they are never a merge source or target.
- **Preserve verbatim** — every commit link (including all links from merged entries), each section header and icon (they mirror `.github/cliff.toml`), and the overall markdown structure.
- **Never invent** information, change an entry's meaning, or reorder sections.
- **Never commit** `.github/changelog-draft.md` — it is a temporary working file.
