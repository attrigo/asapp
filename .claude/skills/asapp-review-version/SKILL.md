---
name: asapp-review-version
description: >
  Use when a version's work is complete and a thorough, holistic review of everything that shipped
  in it — across all services and cross-cutting themes — is wanted before the release is cut.
  Triggers: /asapp-review-version, review the version before release, pre-release review, release
  readiness review, review everything in this version, audit the release.
  Do NOT use to review a single task's branch (use asapp-review-task), to fix or commit anything (it
  only reviews and reports — nothing is logged to TODO.md), or to perform the release itself (use
  asapp-release).
---

# Review Version

The pre-release readiness gate: review everything that shipped in a version, sliced by cross-cutting theme, and produce a readiness report with a go/no-go verdict. Runs before `asapp-release`.

**Core principle:** the question this gate answers — is each theme coherent and complete across every service it touches, and do the themes hold at their seams?

Unlike `asapp-review-task` (one branch → findings logged to `TODO.md`), this anchors on the release-tag range, slices by theme, classifies findings must/should/nice, and writes a readiness report to `docs/reviews/`.

## Usage

- `/asapp-review-version <ver>` — review that version (e.g. `0.4.0`).
- `/asapp-review-version` — default to the in-progress version (top `## <ver> · <theme>` in `TODO.md`); state your read and proceed.

## Process

### 0. Set up progress tracking

**Before any other step**, create these six tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when done:

1. Resolve version, anchor, and cluster themes (Steps 1–2)
2. Review each theme, consolidate (Step 3)
3. Run the cross-cutting seam pass (Step 4)
4. Present the readiness report (Step 5)
5. Write the report (Step 6)
6. Wrap up (Step 7)

Keep task 4 `in_progress` across the wait for user input; move on only once the report is out.

### 1. Resolve version and anchor

Do this up front; review nothing yet.

- **Version** — resolve the input (or the top `TODO.md` section) to one version; heading `## <ver> · <theme>`.
- **Previous tag** — `git describe --tags --abbrev=0` (in-progress) or the tag before `v<ver>` (already released). First-ever release: use the root commit (`git rev-list --max-parents=0 HEAD`) as `v<prev>`.
- **Scope** — the changed files in the range, excluding design specs; also pull the commit log:
  ```bash
  git diff v<prev>...HEAD --name-only -- . ':(exclude)docs/superpowers/**'   # ...v<ver> if already released
  git log --oneline --no-merges v<prev>..HEAD
  ```
  The diff + commit log is the **anchor** and what you theme from. Only `docs/superpowers/**` is out of scope — shipped `.claude/**` is reviewable.

### 2. Cluster the diff into themes

Cluster the version's work into a small set of coherent themes — target **4–8**; merge slivers or split an oversized theme rather than force the count.

- **Theme from the commits + diff** — group by scope and shared paths. Every changed file lands in exactly one theme; never one-theme-per-file, never a catch-all "Misc."
- **Cross-check `TODO.md` for coverage only** — shipped work it doesn't list, or a listed task with no matching change, is a should-fix finding. `TODO.md` is not a theming input.
- **Show the theme list** (one line each, rough file counts) before fanning out; proceed unless the user objects.

### 3. Review each theme, then consolidate

Dispatch **one reviewer per theme**, scoped to that theme's files (not the whole diff), as its **dominant-concern specialist** (see Delegation). It gathers the theme's context once and applies every relevant lens in that pass. Keep **≤5 running at once**.

**Lenses (one pass):**
- Line-level quality — always.
- Layering, coherence, and completeness across services — for production-code themes; skip for docs / tooling / `.claude` themes.
- The theme's specialty — security, tests, API, docs, CI, etc.

**Escalate a second specialist** only when a theme is high-risk (auth/JWT/token/filter-chain → `security-auditor`), architecturally significant across services (→ `architect-reviewer`), or the first pass flags something deeper. Default is one gather per theme.

**Depth** — read the full changed files; follow outward only into code the diff reaches (callers, collaborators, covering tests, dependent config). Not a whole-repo audit.

**Mechanical themes** (renames, moves, mass find-replace) — a verify-only pass: check for stragglers, consistent application, orphaned imports. Their file counts are churn, not effort — say so, and don't let their size shortchange substantive themes.

Tell each reviewer to:
- Judge the code on its own merits — ignore specs / plans; no drift findings.
- Assess completeness — is the theme applied consistently across every service it should touch?
- Classify each finding must-fix / should-fix / nice-to-have, with a short title, effort (S/M/L), impact (High/Med/Low).
- Capture each finding's resolution context — the fields in `.claude/rules/review-report.md`.

Then **consolidate**: dedupe overlaps, merge into one list. IDs are assigned at the end of Step 4.

### 4. Cross-cutting pass — the seams between themes

Step 3 is blind to issues whose cause is in one theme and symptom in another (a field renamed in `auth`, still read the old way in `tasks`; a contract changed on one side only; a pattern applied unevenly). This pass catches those.

**Run only when ≥2 themes touch production code.** Otherwise skip it, and note the skip + reason in the report.

Dispatch **one `code-reviewer`** with the consolidated theme findings + repo access. Tell it to:
- Hunt only the **seams between themes** — never re-review inside a theme (Step 3 did).
- Confirm before reporting — reason over the findings, then spot-read only the lines needed to confirm. Report confirmed seams, not hunches.
- Classify like every finding; set **Theme** to the seam (e.g. `auth × tasks`); record Location as a list covering both sides.

**Then assign IDs** across the merged set.

### 5. Assemble the readiness report

Present the **verdict**, **per-theme summary**, and each section's **summary table** in chat; the full report (detail blocks included) is written in Step 6.

- **Verdict** — **Ready** / **Ready-with-caveats** / **Not-ready**, one-line rationale. Not-ready if any must-fix; Ready-with-caveats if only should/nice; Ready if nothing is worth acting on. This is a **code review only** — it does not assert the build or tests are green (that's `asapp-release`).
- **Per-theme summary** — one row per theme: `| Theme | Coherent & complete? | Notable gaps |`.
- **Findings** — see `.claude/rules/review-report.md` (summary column **Theme**).

### 6. Write the report

Write to **`docs/reviews/v<ver>-readiness-report.md`** (create `docs/reviews/` if absent; overwrite any existing report for the version). Lead with:

- title — `# Release Readiness Report — v<ver> · <theme>`
- anchor line — range, commit / file / theme counts (e.g. `` **Anchor:** `v0.3.0...HEAD` · 62 commits · 587 files · 8 themes ``)
- cross-cutting note — whether the seam pass ran, or was skipped and why
- the code-review-only disclaimer

then the verdict, per-theme summary, and findings.

### 7. Wrap-up

Restate the verdict and the report path. Remind the user: no code changed, nothing committed, nothing logged to `TODO.md` — the only write is the report. Findings are theirs to route (fix now or defer), ticking the report's checkboxes. The release is the separate `asapp-release` step.

## Delegation

Dispatch each theme's reviewer as its dominant-concern specialist:

| Theme's dominant concern | Dispatch as |
|--------------------------|-------------|
| General production code | `code-reviewer` |
| Auth / JWT / token / filter-chain | `security-auditor` |
| README / api-guide / Javadoc / OpenAPI | `documentation-engineer` |
| Test suites | `test-automator` |
| CI / git-hook / docker-compose / pipeline / observability | `devops-engineer` |
| Endpoint / DTO / status-code | `api-designer` |
| `.claude/**` agents / skills / rules | `claude-docs-maintainer` |

| Support | Use |
|---------|-----|
| Locate / understand touched code | `Explore` |
| Cross-cutting seam pass (Step 4) | `code-reviewer` |
| Cross-service architecture escalation (Step 3) | `architect-reviewer` |
| Framing the review | `superpowers:requesting-code-review` |
| A finding needs deeper diagnosis | `superpowers:systematic-debugging` |

## Guardrails

- **Review and report only** — never change code, commit, push, tag, or merge. The release is the user's separate `asapp-release` step.
- **The only write is the report** at `docs/reviews/v<ver>-readiness-report.md` — log nothing to `TODO.md`.
- **Exclude `docs/superpowers/**`** — never flag a spec as outdated or drifted.
- **Delegate all reviewing to subagents** — keep the main context clean.
