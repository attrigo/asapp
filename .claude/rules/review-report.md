---
paths:
  - "docs/reviews/*.md"
---

The findings format for the review reports under `docs/reviews/` — written by `asapp-review-task` and `asapp-review-version` (each supplying its own title and anchor line above these findings), and ticked off as findings are settled (by the developer or `asapp-resolve-review-issues`).

## Sections

- Findings group into **Must-fix**, **Should-fix**, **Nice-to-have** sections, in that order.
- Open with a counts line (`2 must-fix · 22 should-fix · 8 nice-to-have`); skip an empty section.
- Each section is a **summary table** then one **detail block** per finding.
- Add no other prose — at most one `Also checked, clean:` line after the counts, naming in ≤20 words what was verified and found clean.

## Summary Table

The scannable index:

| ID | Title | <axis> | Effort | Impact |
|----|-------|--------|--------|--------|

`<axis>` is the skill's grouping column:
- **`asapp-review-task`** — **Kind** (issue / improvement).
- **`asapp-review-version`** — **Theme**.
- For a version review, order findings within each severity section by theme, so a theme's findings sit together.

## Detail Block

```markdown
- [ ] **S1 — <title>**
    - **Where:** <file:line or `Class#method`; the offending line quoted after it, when it helps>
    - **What:** <the defect, so its consequence>
    - **Fix:** <what to do>
    - **Watch:** <one gotcha the fix must respect — optional>
    - **Defer:** <why this is a separate concern, not part of the work under review — optional>
```

Every bullet is **one sentence, one topic**, within its cap:

| Field | Cap | Its job — and only this |
|-------|-----|-------------------------|
| Title | 10 words | names the defect |
| Where | the sites, bare | `file:line` / `Class#method`; a list when it spans sites (both sides, for a seam); a verbatim quoted line — never command output |
| What | 30 words | the defect and the concrete consequence it causes |
| Fix | 20 words | what to do |
| Watch | 20 words | one gotcha, constraint, or ordering the fix must respect |
| Defer | 20 words | why the finding is a separate concern rather than part of the work under review |

- **A second topic is a second finding** — never a second clause. Cut it when it is rationale rather than a defect.
- **State the defect, not your reading of it** — no meta-commentary on the code's own rationale, no hedging, no citation padding. Stack terms belong where they *are* the subject.
- **Where / What / Fix** are always present; **Watch** and **Defer** only when they earn their place.
- **Defer** is a suggestion, not a routing instruction — its presence says the reviewer judges the finding out of scope here; the developer settles it at fix time.
- Severity is the section, tracked by the ID prefix (`M#` / `S#` / `N#`), not a column.
- Write every checkbox **unchecked**.

## Outcome Bullet

A settled finding is ticked and gains exactly one outcome bullet, appended last — one sentence of ≤15 words naming the outcome only, never the rationale, the files touched, or a before/after:

```markdown
    - **Applied:** <what changed>
    - **Deferred:** <why not now>
    - **Ignored:** <why not at all>
```
