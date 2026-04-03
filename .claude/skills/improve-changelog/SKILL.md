---
name: improve-changelog
description: >
  Improves the release notes of a GitHub Release using AI editorial judgment.
  Use when the user wants to polish, clean up, refine, or improve a changelog or release notes.
  Triggers: /improve-changelog, improve changelog, improve release notes, polish release,
  clean up changelog, refine release notes, edit release notes, fix changelog.
  Do NOT use for generating changelogs from scratch, modifying git history, or editing CHANGELOG.md files.
---

# Improve Changelog

Fetches the release notes of a GitHub Release, applies AI editorial improvements (merge duplicates, remove noise, improve phrasing), and updates the release after confirmation.

Supports two modes depending on whether the `gh` CLI is available.

## Usage

- `/improve-changelog` — improves the latest release
- `/improve-changelog v0.2.0` — improves a specific release

## Quick Reference

| Situation | Mode |
|---|---|
| `gh` CLI available | Automatic: fetch, improve, update via `gh` |
| `gh` CLI not available | Manual: user pastes into draft file, skill improves, user copies back |

## Core Workflow

### Step 1: Check `gh` Availability

```bash
gh --version
```

If the command succeeds → proceed with **Automatic mode**.
If it fails or is not found → proceed with **Manual mode**.

---

### Automatic Mode

#### Step 2A: Detect Version

If a version argument was provided (e.g. `v0.2.0`), use it directly.

Otherwise, fetch the latest release tag:

```bash
gh release list --limit 1 --json tagName -q '.[0].tagName'
```

#### Step 3A: Fetch Release Notes

```bash
gh release view <tag> --json body -q '.body'
```

#### Step 4A: Improve and Confirm

Apply editorial improvements (see [Improvement Rules](#improvement-rules)).

Display the improved changelog and summarize changes. Ask for confirmation:

```
Proceed to update the GitHub Release? [y/N]
```

#### Step 5A: Update the GitHub Release

Only if the user confirms:

```bash
gh release edit <tag> --notes-file /tmp/improved-changelog.md
```

Confirm success and print the release URL.

---

### Manual Mode

#### Step 2M: Detect Version

If a version argument was provided (e.g. `v0.2.0`), use it directly.

Otherwise, ask the user which version they are working on.

#### Step 3M: Instruct User to Create Draft File

Tell the user:

```
gh CLI is not available. To proceed manually:

1. Open the GitHub Release page for <tag>
2. Copy the release notes
3. Paste them into: .github/changelog-draft.md
4. Come back and confirm when ready
```

Wait for the user to confirm the file is ready before continuing.

#### Step 4M: Read and Improve Draft File

Read `.github/changelog-draft.md` and apply editorial improvements (see [Improvement Rules](#improvement-rules)).

Write the improved content back to `.github/changelog-draft.md`.

Display the improved changelog and summarize changes.

#### Step 5M: Instruct User to Apply Changes

```
Done. Once the content looks good, you can follow these steps to publish it:

1. Open the GitHub Release page for <tag>
2. Click Edit
3. Replace the release notes with the contents of .github/changelog-draft.md
4. Save

You can delete .github/changelog-draft.md afterwards — it is listed in .gitignore and will not be committed.
```

---

## Improvement Rules

Apply these rules in both modes:

**Merge** — combine entries that cover the same change or topic across multiple commits into a single clearer entry, regardless of section. Preserve all commit links from the merged entries, listed together at the end: `([`abc1234`](url1), [`def5678`](url2))`.

**Remove** — drop entries with no user-facing value:
- Internal refactors with no behavioral change visible to users
- Typo, comment, or formatting fixes
- Repetitive entries that say the same thing as another entry
- Commits already implied by a merged entry

**Remove empty sections** — if all entries in a section are removed or merged away, drop the section header too.

**Rewrite** — improve clarity of terse or ambiguous messages. Use plain language. Keep entries concise (one line preferred).

**Normalize tone** — all entries must use imperative form (`Add`, `Fix`, `Update`), not past tense (`Added`, `Fixed`) or third person (`Adds`, `Fixes`).

**Normalize scope casing** — scopes must be lowercase (e.g. `**authentication:**` not `**Authentication:**`).

**Reorder within sections** — within each section, place the most user-facing or impactful entries first.

**Preserve always**:
- Section headers and their icons (⚠️ ✨ 🐛 📖 ⬆️ 🔨)
- All commit links — keep every link, including all links from merged entries
- All entries under ⚠️ Breaking Changes — never remove, merge, or reorder these
- The overall markdown structure of the release notes

**Do not**:
- Invent information not present in the original
- Change the meaning of any entry
- Reorder sections

## Safety

- **Never update without confirmation** — always show the improved version first
- **Never remove Breaking Changes entries** — these are critical for consumers
- **Never invent content** — only rewrite what is already there
- **Preserve all commit links** — traceability is non-negotiable
- **Never commit `.github/changelog-draft.md`** — it is a temporary working file
