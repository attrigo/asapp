---
name: asapp-review-version
description: >
  Use when a version's work is complete and you want a thorough, holistic review of everything that
  shipped in it — across all services and cross-cutting themes — before you cut the release.
  Triggers: /asapp-review-version, review the version before release, pre-release review, release
  readiness review, review everything in this version, audit the release.
  Do NOT use to review a single task's branch (use asapp-review-task), to fix or commit anything (it
  only reviews and reports — nothing is logged to TODO.md), or to perform the release itself (use
  asapp-release).
---

# Review Version

The pre-release readiness gate. Review **everything that shipped in a version**, sliced **by
cross-cutting theme**, then present a **readiness report** with a go / no-go verdict. Runs *before*
`asapp-release`.

**Core principle:** review and report **only** — change no code, commit nothing, log nothing to
`TODO.md`, run no release step. The distinctive question this answers, that a per-task review cannot:
**is each theme coherent and fully done across every service it touches?**

> **This is not `asapp-review-task`.** You will be tempted to imitate it — don't inherit its contract.
> Three differences are load-bearing: (1) the anchor is a **release-tag range**, not one branch;
> (2) slicing is **theme-first**, not reviewer-lens-first; (3) the output is a **readiness report you
> present** using **blocker / should-fix / nice-to-have**, never the `issue / missing / out-of-scope`
> buckets logged into `TODO.md`. See *How this differs* below.

## Usage

- `/asapp-review-version <ver>` — e.g. `0.4.0`. Review that version.
- `/asapp-review-version` (no argument) — default to the current in-progress version (the top
  `## <ver> · <theme>` section of `TODO.md`, e.g. `## 0.4.0 · Establish SDD & AI Agents`); state your
  read in one line and proceed.

## Process

### Step 0: Set up progress tracking

**Before any other step**, create these four tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when done:

1. Resolve version, anchor, and theme map (Steps 1–2)
2. Fan out per theme and consolidate (Step 3)
3. Present the readiness report (Step 4)
4. Offer to persist, then wrap up (Steps 5–6)

Keep Task 3 `in_progress` across the wait for any user input; move on only once the report is out.

### Step 1: Resolve version and anchor

Do all of this up front. Review nothing yet.

1. **Resolve the version** — turn the input (or the top `TODO.md` section) into a single version; its
   heading is `## <ver> · <theme>`, e.g. `## 0.4.0 · Establish SDD & AI Agents`.
2. **Find the previous tag** — `git describe --tags --abbrev=0` (in-progress version) or the tag
   immediately preceding `v<ver>` (already-released version).
3. **Set the review scope** — the changed files in the range, **excluding design specs**:

   ```bash
   git diff v<prev>...HEAD --name-only -- . ':(exclude)docs/superpowers/**'   # in-progress
   git diff v<prev>...v<ver> --name-only -- . ':(exclude)docs/superpowers/**' # already released
   ```

   Also skim `git log --oneline --no-merges v<prev>..HEAD` for the narrative. This diff is the
   **anchor** for the review. (`...` and `..` coincide here because the tag is an ancestor of HEAD —
   the normal case; use `...HEAD` for the diff either way.)

   Only the design specs under `docs/superpowers/**` are out of scope. Everything else the diff touches
   is reviewable — including the `.claude/**` skills, agents, and rules shipped by the version.

### Step 2: Cluster the diff into themes

This step is what makes a version review worth doing. Cluster the version's work into a small set of
coherent review slices — **target 4–8**.

- **Seed the themes from the version's `TODO.md` section** — its bold-prefix groups and buckets
  (e.g. *Error handler*, *HTTP clients*, *Security / JWT*, *Tests*, *Docs & Javadoc*, *Renames*) are
  the raw material. Merge tiny/related groups; split one only if it is genuinely two concerns.
- **Map each theme to its changed files** from the Step 1 diff. Standalone tasks with no bold prefix
  are grouped by file affinity or collected into one "Misc" theme — never one slice per file.
- **Show the proposed theme list** (one line each, with rough file counts) before fanning out.
  Proceed unless the user objects. Don't over-ask.

### Step 3: Fan out per theme, then consolidate

For **each theme slice**, dispatch the review subagents **in parallel**, scoped to that slice's files
(not the whole version diff). Pick reviewers per theme:

- **Every theme that touches production code:** `code-reviewer` (line-level quality) **and**
  `architect-reviewer` (layering, coherence, completeness across services). For **pure docs, tooling,
  or `.claude/**` themes**, lead with the domain specialist and skip `architect-reviewer` — there is no
  layering to assess.
- **The most relevant specialist(s) for the theme**, from `.claude/agents/`:
  `security-auditor` (auth / JWT / token / filter-chain / crypto / secrets / new endpoints),
  `documentation-engineer` (README / api-guide / Javadoc / OpenAPI sync),
  `test-automator` (test-suite changes), `devops-engineer` (CI / git hooks / docker-compose /
  Maven pipeline / observability config), `api-designer` (endpoint / DTO / status-code changes),
  `claude-docs-maintainer` (`.claude/**` agents / skills / rules authoring).

**Review depth — diff-anchored:**

- Read the **full changed files**, not just the diff hunks.
- Follow outward only into code the diff reaches — callers, collaborators, covering tests, dependent
  config — enough to judge correctness, coherence, and completeness. Not a whole-repo audit.

**Mechanical / sweeping themes** (renames, moves, mass find-replace): treat as a **verify-only pass** —
check for stragglers (leftover old names), consistent application, and side effects like orphaned
imports; do not read every touched file. Their file counts are **churn, not effort** — say so when you
present them, and never let such a theme's size justify shortchanging the substantive themes.

Tell every reviewer to:

- **Judge the code on its own merits** — ignore specs/plans; no "spec is outdated" or drift findings.
- **Assess theme completeness explicitly** — is this theme applied *consistently across every service
  it should touch*? Any half-done, inconsistent, or regressed-after-its-own-review application?
- **Classify each finding** as **blocker** (must fix before release), **should-fix** (fix before
  release if feasible), or **nice-to-have** (safe to defer). Suggest a one-line rationale.

Then **consolidate**: dedupe overlaps across themes, merge into one list, assign each an `ID`.

### Step 4: Present the readiness report

Present in chat (see Step 5 for persisting):

1. **Verdict** — one of **Ready** / **Ready-with-caveats** / **Not-ready**, with a one-line rationale.
   *Not-ready* if any blocker exists; *Ready-with-caveats* if only should-fix / nice-to-have remain;
   *Ready* if nothing is worth acting on. The verdict reflects this **code review only** — it does not
   assert the build or test suite is green (that is `asapp-release`'s job).
2. **Per-theme summary** — one row per theme:

   | Theme | Coherent & complete? | Notable gaps |
   |-------|----------------------|--------------|

3. **Findings** — sorted by severity, blockers first:

   | ID | Theme | Severity | Description | Recommended action |
   |----|-------|----------|-------------|--------------------|

   **Severity** Blocker / Should-fix / Nice-to-have. **Description** and **Recommended action** are
   each one short, plain sentence — what is wrong and what to do.

### Step 5: Offer to persist the report

The report is **not** auto-written and **not** logged to `TODO.md`. After presenting it, **offer** to
save it to a file (e.g. under the scratchpad or alongside release housekeeping). Write it only if the
user asks.

### Step 6: Wrap-up

- Restate the verdict in one line.
- Remind the user: nothing was changed, committed, or logged. Blockers / should-fixes are theirs to
  route (fix now, or defer to a later version). The release itself is the separate manual `asapp-release`
  step — do not run any of its mechanics here.

## How this differs from `asapp-review-task`

| Aspect | `asapp-review-task` | `asapp-review-version` (this skill) |
|--------|---------------------|-------------------------------------|
| Anchor | `main...HEAD` (one branch) | `v<prev>...HEAD` release-tag range |
| Granularity | one task | whole version, **sliced by theme** |
| Finding taxonomy | issue / missing / out-of-scope | **blocker / should-fix / nice-to-have** |
| Output | findings **logged into `TODO.md`** | **readiness report you present** (verdict + tables); nothing logged |
| Timing | before a task closes | before `asapp-release` |

Everything else — parallel fan-out to `.claude/agents/`, reading full changed files, judging code on
its merits, excluding `docs/superpowers/**`, keeping main context clean — is shared.

## Delegation & tools — quick reference

| Situation | Use |
|-----------|-----|
| Line-level quality (every theme) | `code-reviewer` |
| Layering, coherence, cross-service completeness (every theme) | `architect-reviewer` |
| Auth / JWT / token / filter-chain themes | `security-auditor` |
| README / api-guide / Javadoc / OpenAPI themes | `documentation-engineer` |
| Test-suite themes | `test-automator` |
| CI / git-hook / docker-compose / pipeline / observability themes | `devops-engineer` |
| Endpoint / DTO / status-code themes | `api-designer` |
| `.claude/**` agents / skills / rules authoring | `claude-docs-maintainer` |
| Locate / understand touched code | `Explore` |
| Framing the review pass | `superpowers:requesting-code-review` |
| A finding needs deeper diagnosis | `superpowers:systematic-debugging` |

Pick the **most specific** agent; `general-purpose` is a last resort.

## Hard rules

- **Review and report only** — never change code, commit, push, tag, or run any release mechanic.
- **Log nothing to `TODO.md`.** The only write is the optional report file in Step 5, after the user asks.
- **Ignore specs/plans** — exclude `docs/superpowers/**`; never flag a spec as outdated or drifted.
- **Anchor on the release-tag range**, not `main...HEAD`.
- **Slice theme-first** — reviewer lenses layer *onto* themes, they don't replace them.
- **Keep the main context clean** — delegate all reviewing to subagents.
- **Do not release, tag, or merge** — that is the user's separate `asapp-release` step.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Reviewing `main...HEAD` like a task | Anchor on `v<prev>...HEAD` — the whole version. |
| Slicing by reviewer lens, theme as an afterthought | Themes are the primary slices; lenses attach to each theme. |
| Using the `issue / missing / out-of-scope` taxonomy | Use **blocker / should-fix / nice-to-have** — this is a readiness gate. |
| Logging findings into `TODO.md` | Present a report; log nothing. Offer to persist to a file only. |
| Handing a reviewer all commits at once | Scope each reviewer to its theme's files. |
| Full code+architect review of a mass rename | Mechanical themes get a verify-only pass; their counts are churn, not effort. |
| Excluding `.claude/**` as if it were a spec | Only `docs/superpowers/**` is out of scope; shipped `.claude/**` work is reviewable. |
| Folding release mechanics into the verdict | Version bump, tags, spec archival, push are `asapp-release`'s job. |
| Skipping the completeness check | Ask per theme: applied consistently across *every* service it touches? |
| Starting Step 1 before the four tracking tasks | Do Step 0 first. |
