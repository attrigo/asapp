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

If the command fails or `gh` is not found, abort immediately with:

```
Error: This command requires the GitHub CLI (gh).
Install it from https://cli.github.com, authenticate with `gh auth login`, and re-run.
```

### Step 2: Detect version

If a version argument was provided (e.g. `v0.2.0`), use it directly.

Otherwise, fetch the latest release tag:

```bash
gh release list --limit 1 --json tagName -q '.[0].tagName'
```

### Step 3: Get release notes

```bash
gh release view <tag> --json body -q '.body'
```

### Step 4: Apply improvements

Apply the [Improvement Rules](#improvement-rules) to the fetched content.

### Step 5: Confirm and apply

Display the improved changelog and summarize what changed. Ask for confirmation:

```
Proceed to update the GitHub Release? [y/N]
```

Only continue if the user confirms.

Write the improved content to `.github/changelog-draft.md`, then run:

```bash
gh release edit <tag> --notes-file .github/changelog-draft.md
```

After a successful update, delete the working file:

```bash
rm .github/changelog-draft.md
```

Confirm success and print the release URL.

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
