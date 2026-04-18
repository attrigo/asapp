# Improve Changelog

Fetches the release notes of a GitHub Release, applies AI editorial improvements (merge duplicates, remove noise, improve phrasing), and updates the release after confirmation.

## Usage

- `/improve-changelog` — improves the latest release
- `/improve-changelog v0.2.0` — improves a specific release

## Steps

### Step 1: Check gh availability

```bash
gh --version
```

If the command succeeds → `gh` is available. If it fails or is not found → `gh` is not available.
This determines which path to take in steps 3 and 5.

### Step 2: Detect version

If a version argument was provided (e.g. `v0.2.0`), use it directly.

If `gh` is available and no argument was provided, fetch the latest release tag:

```bash
gh release list --limit 1 --json tagName -q '.[0].tagName'
```

If `gh` is not available and no argument was provided, ask the user which version they are working on.

### Step 3: Get release notes

**If `gh` is available:**

```bash
gh release view <tag> --json body -q '.body'
```

**If `gh` is not available:**

Tell the user:

```
gh CLI is not available. To proceed manually:

1. Open the GitHub Release page for <tag>
2. Copy the release notes
3. Paste them into: .github/changelog-draft.md
4. Confirm when ready
```

Wait for the user to confirm the file is ready, then read `.github/changelog-draft.md`.

### Step 4: Apply improvements

Apply the [Improvement Rules](#improvement-rules) to the fetched content.

### Step 5: Confirm and apply

Display the improved changelog and summarize what changed. Ask for confirmation:

```
Proceed to update the GitHub Release? [y/N]
```

Only continue if the user confirms.

**If `gh` is available:**

```bash
gh release edit <tag> --notes-file /tmp/improved-changelog.md
```

Confirm success and print the release URL.

**If `gh` is not available:**

Write the improved content back to `.github/changelog-draft.md`, then tell the user:

```
Done. To publish the changes:

1. Open the GitHub Release page for <tag>
2. Click Edit
3. Replace the release notes with the contents of .github/changelog-draft.md
4. Save

You can delete .github/changelog-draft.md afterwards — it is listed in .gitignore and will not be committed.
```

---

## Improvement Rules

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