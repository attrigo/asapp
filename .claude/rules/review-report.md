---
paths:
  - "docs/reviews/*.md"
---

# Review Report Format

The findings format for the review reports under `docs/reviews/` — written by `asapp-review-task` and `asapp-review-version`, ticked off as findings are resolved (by the developer or `asapp-resolve-review-issues`). Each skill supplies its own title and anchor line above these findings.

Findings group into **Must-fix**, **Should-fix**, **Nice-to-have** sections, in that order. Open with a counts line (`2 must-fix · 22 should-fix · 8 nice-to-have`); skip an empty section. Each section is a **summary table** then one **detail block** per finding.

Summary table — the scannable index:

| ID | Title | <axis> | Effort | Impact |
|----|-------|--------|--------|--------|

`<axis>` is the skill's grouping column:
- **task review** — **Kind** (issue / improvement).
- **version review** — **Theme**; within each severity section, order findings by theme so a theme's findings sit together.

Detail block:

```markdown
- [ ] **S1 — <title>**
    - **Location:** <file:line or `Class#method`; a nested list when it spans several sites — both sides, for a seam>
    - **Description:** <what is wrong — one plain sentence>
    - **Why it matters:** <the concrete consequence — one line>
    - **Evidence:** <offending line(s) — optional, only when it aids confirmation>
    - **Recommended action:** <what to do — one plain sentence>
    - **Resolver notes:** <gotchas / constraints / ordering — optional; never restate Description or Why>
```

- Severity is the section, tracked by the ID prefix (`M#` / `S#` / `N#`), not a column.
- **Location / Description / Why it matters / Recommended action** are always present; **Evidence** and **Resolver notes** only when they earn their place.
- Write every checkbox **unchecked** — the developer ticks them as findings are resolved.
