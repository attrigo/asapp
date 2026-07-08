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

The pre-release readiness gate. Review **everything that shipped in a version**, sliced **by cross-cutting theme**, then present a **readiness report** with a go / no-go verdict. Runs *before* `asapp-release`.

**Core principle:** review and report **only** (change no code, commit nothing, log nothing to `TODO.md`, run no release step). The distinctive question this answers: **is each theme coherent and fully done across every service it touches — and do the themes hold together at their seams?**

> **This is not `asapp-review-task`.** You will be tempted to imitate it — don't inherit its contract; the differences are load-bearing. See *How this differs* below.

## Usage

- `/asapp-review-version <ver>` — e.g. `0.4.0`. Review that version.
- `/asapp-review-version` (no argument) — default to the current in-progress version (the top `## <ver> · <theme>` section of `TODO.md`, e.g. `## 0.4.0 · Establish SDD & AI Agents`); state your read in one line and proceed.

## Process

### Step 0: Set up progress tracking

**Before any other step**, create these six tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when done:

1. Resolve version, anchor, and theme map (Steps 1–2)
2. Review each theme, consolidate (Step 3)
3. Run the cross-cutting pass (Step 4)
4. Present the readiness report (Step 5)
5. Write the report (Step 6)
6. Wrap up (Step 7)

Keep Task 4 `in_progress` across the wait for any user input; move on only once the report is out.

### Step 1: Resolve version and anchor

Do all of this up front. Review nothing yet.

1. **Resolve the version** — turn the input (or the top `TODO.md` section) into a single version; its heading is `## <ver> · <theme>`, e.g. `## 0.4.0 · Establish SDD & AI Agents`.
2. **Find the previous tag** — `git describe --tags --abbrev=0` (in-progress version) or the tag immediately preceding `v<ver>` (already-released version). No prior tag (first-ever release): use the repository's root commit (`git rev-list --max-parents=0 HEAD`) as `v<prev>` instead.
3. **Set the review scope** — the changed files in the range, **excluding design specs**:

   ```bash
   git diff v<prev>...HEAD --name-only -- . ':(exclude)docs/superpowers/**'   # in-progress
   git diff v<prev>...v<ver> --name-only -- . ':(exclude)docs/superpowers/**' # already released
   ```

   Also pull `git log --oneline --no-merges v<prev>..HEAD` — the commit log, with the changed-file list, is what you **theme from** in Step 2. This diff is the **anchor** for the review. (`...` and `..` coincide here because the tag is an ancestor of HEAD — the normal case; use `...HEAD` for the diff either way.)

   Only the design specs under `docs/superpowers/**` are out of scope. Everything else the diff touches is reviewable — including the `.claude/**` skills, agents, and rules shipped by the version.

### Step 2: Cluster the diff into themes

This step is what makes a version review worth doing. Cluster the version's work into a small set of coherent themes — **target 4–8**. If natural clustering yields far fewer or more, don't force the target: merge slivers into a neighboring theme, or split an oversized theme along a clear internal boundary.

**Theme from the commits and diff — one source, and a complete one.** The commit log and changed-file list are the source of truth for what shipped; `TODO.md` is **not** a theming input.

- **Cluster the commits + diff into themes** — group by scope and shared file paths (e.g. all error-handler commits → one theme). Every changed file lands in exactly one theme — never one theme per file, never a catch-all "Misc."
- **Cross-check against `TODO.md`, for coverage only** — shipped work it doesn't list, or a task with no matching change, is a **should-fix** readiness finding; carry it into Step 5.
- **Show the proposed theme list** (one line each, with rough file counts) before fanning out. Proceed unless the user objects. Don't over-ask.

### Step 3: Review each theme with one agent, then consolidate

Dispatch **one review subagent per theme**, scoped to that theme's files (not the whole version diff). It gathers the theme's context **once** and applies every relevant lens in that single pass. Keep **no more than 5 running at once**; start the next theme as a slot frees, until every theme has been reviewed.

**Dispatch each theme's agent as its dominant-concern specialist** — general production code → `code-reviewer`; auth → `security-auditor`; docs → `documentation-engineer`; tests → `test-automator`; and so on (see *Delegation & tools* below).

**Lenses that the one agent applies in its single pass:**

- **Line-level quality** — always.
- **Layering, coherence, and completeness across services** — for every theme that touches production code; skip for pure docs, tooling, or `.claude/**` themes (no layering to assess).
- **The theme's specialty** — security, tests, API/DTO, docs, CI/observability, etc., matching the theme's concern.

**Escalate a second, dedicated specialist for a theme only when** it is high-risk (auth / JWT / token / filter-chain → `security-auditor`) or architecturally significant across services (→ `architect-reviewer`), or when the single pass surfaces something needing deeper scrutiny than it could give. Escalation is the exception — the default is one gather per theme.

**Review depth — diff-anchored:**

- Read the **full changed files**, not just the diff hunks.
- Follow outward only into code the diff reaches — callers, collaborators, covering tests, dependent config — enough to judge correctness, coherence, and completeness. Not a whole-repo audit.

**Mechanical / sweeping themes** (renames, moves, mass find-replace): treat as a **verify-only pass** — check for stragglers (leftover old names), consistent application, and side effects like orphaned imports; do not read every touched file. Their file counts are **churn, not effort** — say so when you present them, and never let such a theme's size justify shortchanging the substantive themes.

Tell each theme's reviewer to:

- **Judge the code on its own merits** — ignore specs/plans; no "spec is outdated" or drift findings.
- **Assess theme completeness explicitly** — is this theme applied *consistently across every service it should touch*? Any half-done, inconsistent, or regressed-after-its-own-review application?
- **Classify each finding** as **must-fix** (must fix before release), **should-fix** (fix before release if feasible), or **nice-to-have** (safe to defer). Give it a short **title**, and suggest an **effort** (S/M/L) and **impact** (High/Med/Low).
- **Capture the resolution context each finding needs** — the **Location** (file:line or `Class#method`; a list when it spans several sites), **why it matters** (the concrete consequence), and, when they help, an **Evidence** snippet and **Resolver notes** (gotchas / constraints for whoever fixes it). The reviewer has this in hand now; recording it spares the resolver from rediscovering it.

Then **consolidate**: dedupe overlaps across themes and merge into one list. (IDs are assigned at the end of Step 4, once the cross-cutting pass has folded its findings in.)

### Step 4: Cross-cutting pass — the seams between themes

Step 3 reviews *inside* each theme; it cannot see an issue whose cause sits in one theme and whose symptom sits in another (a field renamed in the `auth` theme but still read the old way in `tasks`), a contract changed on one side only, a pattern applied in some services but not others. This pass catches exactly those.

**Run it only when ≥2 themes touch production code.** Skip it for a single-theme version, or one that is entirely docs / tooling / `.claude` (no cross-service seams to find) — and note the skip, with its reason, in the report so it reads as a decision, not an omission. When skipped, still assign IDs (below) and move on.

Dispatch **one `code-reviewer`** subagent, handed the **consolidated theme findings** (each theme and its file group) plus repo access. Tell it to:

- **Hunt only the seams *between* themes** — contradictions, half-applied cross-service changes, contract/field mismatches, a pattern applied in one theme but not another. Do **not** re-review inside a single theme; Step 3 already did.
- **Confirm before reporting** — reason over the theme findings to spot a suspected seam, then **spot-read only the specific files/lines** needed to confirm it. Report confirmed seams, not hunches.
- **Classify like every other finding** — the same **must-fix / should-fix / nice-to-have** taxonomy, a short **title**, an **effort** (S/M/L) and **impact** (High/Med/Low). Set the **Theme** to the seam it spans, e.g. `auth × tasks`.
- **Record the seam's Location as a list** — both sides of it (e.g. the field's definition and each site still reading it the old way) — plus **why it matters** and, when useful, **Evidence** / **Resolver notes**, like every other finding.

**Then assign IDs.** Fold any returned seam findings into the consolidated list, then assign every finding across the merged set an `ID` (`M#`/`S#`/`N#`). Step 5 renders them inline in the severity sections — the `Theme` column marks each as a seam, and because they share the taxonomy the verdict counts them like any other finding.

### Step 5: Assemble the readiness report

Build the report in the structure below.

Present the **verdict**, the **per-theme summary**, and each section's **summary table** in chat; the full report — detail blocks included — is written to the file in Step 6.

1. **Verdict** — one of **Ready** / **Ready-with-caveats** / **Not-ready**, with a one-line rationale. *Not-ready* if any **must-fix** exists; *Ready-with-caveats* if only should-fix / nice-to-have remain; *Ready* if nothing is worth acting on. The verdict reflects this **code review only** — it does not assert the build or test suite is green (that is `asapp-release`'s job).
2. **Per-theme summary** — one row per theme:

   | Theme | Coherent & complete? | Notable gaps |
   |-------|----------------------|--------------|

3. **Findings** — grouped into three severity sections, in order: **Must-fix**, **Should-fix**, **Nice-to-have**. Open with a counts line, e.g. `2 must-fix · 22 should-fix · 8 nice-to-have`; skip an empty section. Each section has two parts:

   - a **summary table** — the scannable index:

     | ID | Title | Theme | Effort | Impact |
     |----|-------|-------|--------|--------|

   - a **detail block per finding** — a checkbox item carrying the prose the table omits:

     ```markdown
     - [ ] **S1 — <title>**
         - **Location:** <file:line or `Class#method`; a nested list when the finding spans several sites>
         - **Description:** <what is wrong — one short, plain sentence>
         - **Why it matters:** <the concrete consequence / failure scenario — one line>
         - **Evidence:** <offending line(s) or a short snippet — optional, only when it aids confirmation>
         - **Recommended action:** <what to do — one short, plain sentence>
         - **Resolver notes:** <optional free-form guidance for whoever fixes it — gotchas, constraints, ordering; omit when there's nothing extra>
     ```

   **Effort** S/M/L · **Impact** High/Med/Low · **Title** names the finding in a few words. Severity is the section, not a column; ID prefixes track it (`M#` / `S#` / `N#`). **Location / Description / Why it matters / Recommended action** are always present; **Evidence** and **Resolver notes** only when they help, never restating Description or Why. Write every checkbox **unchecked** — it is the developer's marker to tick as findings are resolved before release.

### Step 6: Write the report

Write the assembled report to **`docs/reviews/v<ver>-readiness-report.md`** (e.g. `docs/reviews/v0.4.0-readiness-report.md`); create the `docs/reviews/` directory if absent, and overwrite any existing report for the same version.

Lead the file with:

- a title — `# Release Readiness Report — v<ver> · <theme>`
- an anchor line — the range, commit count, file count, and theme count (e.g. `` **Anchor:** `v0.3.0...HEAD` · 62 commits · 587 files · 8 themes ``)
- a one-line cross-cutting note — whether the seam pass ran, or was skipped and why (e.g. `Cross-cutting pass: skipped — single production theme`)
- the code-review-only disclaimer

then the verdict, per-theme summary, and findings sections from Step 5.

### Step 7: Wrap-up

- Restate the verdict in one line, and give the path to the written report.
- Remind the user: no code was changed, nothing committed, nothing logged to `TODO.md` — the only write is the report file. Must-fixes / should-fixes are theirs to route (fix now, or defer to a later version), ticking the report's checkboxes as they go. The release itself is the separate manual `asapp-release` step — do not run any of its mechanics here.

## How this differs from `asapp-review-task`

| Aspect | `asapp-review-task` | `asapp-review-version` (this skill) |
|--------|---------------------|-------------------------------------|
| Anchor | `main...HEAD` (one branch) | `v<prev>...HEAD` release-tag range |
| Granularity | one task | whole version, **sliced by theme** |
| Finding taxonomy | issue / missing / out-of-scope | **must-fix / should-fix / nice-to-have** |
| Output | findings **logged into `TODO.md`** | **readiness report written to `docs/reviews/`** (verdict + tables + checkboxed findings); nothing logged to `TODO.md` |
| Timing | before a task closes | before `asapp-release` |

Everything else — parallel fan-out to `.claude/agents/`, reading full changed files, judging code on its merits, excluding `docs/superpowers/**`, keeping main context clean — is shared.

## Delegation & tools — quick reference

**Dispatch each theme's single reviewer as its dominant-concern specialist:**

| Theme's dominant concern | Dispatch as |
|--------------------------|-------------|
| General production code | `code-reviewer` |
| Auth / JWT / token / filter-chain | `security-auditor` |
| README / api-guide / Javadoc / OpenAPI | `documentation-engineer` |
| Test suites | `test-automator` |
| CI / git-hook / docker-compose / pipeline / observability | `devops-engineer` |
| Endpoint / DTO / status-code | `api-designer` |
| `.claude/**` agents / skills / rules authoring | `claude-docs-maintainer` |

| Support | Use |
|---------|-----|
| Locate / understand touched code | `Explore` |
| Cross-cutting pass over the seams between themes | `code-reviewer` (single pass — Step 4) |
| Framing the review pass | `superpowers:requesting-code-review` |
| A finding needs deeper diagnosis | `superpowers:systematic-debugging` |

## Hard rules

- **Review and report only** — never change code, commit, push, tag, or run any release mechanic.
- **Log nothing to `TODO.md`.** The only write is the report file at `docs/reviews/v<ver>-readiness-report.md` (Step 6).
- **Ignore specs/plans** — exclude `docs/superpowers/**`; never flag a spec as outdated or drifted.
- **Anchor on the release-tag range**, not `main...HEAD`.
- **Slice theme-first** — reviewer lenses layer *onto* themes, they don't replace them.
- **Run the cross-cutting pass when ≥2 themes touch production code** — one `code-reviewer` over the *seams between themes*, confirming by spot-read; never a re-review inside a theme. Skip it, and note why in the report, when fewer than two production themes exist.
- **Keep the main context clean** — delegate all reviewing to subagents.
- **Do not release, tag, or merge** — that is the user's separate `asapp-release` step.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Reviewing `main...HEAD` like a task | Anchor on `v<prev>...HEAD` — the whole version. |
| Slicing by reviewer lens, theme as an afterthought | Themes come first; lenses attach to each theme afterward. |
| Theming only from `TODO.md`, missing un-listed changes | The diff is the source of truth; every changed file lands in a theme, and TODO/diff mismatches get flagged. |
| Using the `issue / missing / out-of-scope` taxonomy | Use **must-fix / should-fix / nice-to-have** — this is a readiness gate. |
| Logging findings into `TODO.md` | Log nothing there; write the report to `docs/reviews/v<ver>-readiness-report.md`. |
| Cramming all findings into one wide table | Per section: a compact summary table + a checkboxed detail block for the prose. |
| Omitting Location so the resolver must rediscover the site | Record file:line / `Class#method` (a list if multi-site, both sides for a seam) — context the reviewer already has. |
| Padding every finding with Evidence and Resolver notes | Both are optional — include only when they aid confirmation or carry a real gotcha. |
| Handing a reviewer all commits at once | Scope each reviewer to its theme's files. |
| Spawning a separate agent per lens per theme | One agent per theme gathers context once and applies all lenses; escalate a specialist only when high-risk or flagged. |
| Running every theme at once | Keep no more than 5 theme reviewers running at once. |
| Full multi-lens review of a mass rename | Mechanical themes get a verify-only pass; their counts are churn, not effort. |
| Excluding `.claude/**` as if it were a spec | Only `docs/superpowers/**` is out of scope; shipped `.claude/**` work is reviewable. |
| Folding release mechanics into the verdict | Version bump, tags, spec archival, push are `asapp-release`'s job. |
| Skipping the completeness check | Ask per theme: applied consistently across *every* service it touches? |
| Skipping the cross-cutting pass on a multi-theme version | When ≥2 themes touch production code, run it — the per-theme pass is blind to issues in the seams between themes. |
| The cross-cutting agent re-reviewing inside a theme | It hunts only the seams *between* themes, confirming by spot-read; Step 3 already covered inside each theme. |
| Starting Step 1 before the six tracking tasks | Do Step 0 first. |
