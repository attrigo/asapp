---
name: asapp-review-task
description: >
  Use when a TODO.md task's implementation and earlier fix pass are done and a fresh, thorough review
  of the current branch is wanted — surfacing bugs, gaps, and improvement ideas — before closing it.
  Triggers: /asapp-review-task, final review, review the branch before closing, review the current status of
  the app, find issues and improvements before I close this task, audit the changes.
  Do NOT use to fix or commit anything (it only reviews and logs — use asapp-resolve-review-issues to fix the
  logged findings), to refine or decompose a task (use asapp-refine-task), to review an external pull
  request (use the PR review tools), or to review a whole shipped version before release (use
  asapp-review-version).
---

# Review Task

The final review gate before a task closes. Delegate a thorough review of the current branch to subagents, present a **prioritized findings table with detail blocks**, then route the findings you select: **apply-now** findings to a report file, **deferred** findings to `TODO.md`.

**Core principle:** surface findings of two **kinds** — **issues** (something is wrong) and **improvements** (something could be better) — and give each a **disposition**: **apply now** (address before closing this task → report file) or **defer** (a separate concern for later → `TODO.md`). Kind and disposition are independent: either kind can be applied now or deferred. `asapp-resolve-review-issues` fixes the apply-now findings afterwards.

## Usage

- `/asapp-review-task <line-number>` — review the branch for the task at that line of `TODO.md`
- `/asapp-review-task <quoted or named task>` — locate the matching task, then review
- `/asapp-review-task` (no argument) — ask which task

## Process

### Step 0: Set up progress tracking

**Before any other step**, create these four tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when it's done:

1. Locate task and determine review scope (Step 1)
2. Run the review and consolidate (Step 2)
3. Present findings and user selection (Steps 3–4)
4. Route selected findings (Step 5)
5. Wrap up (Step 6)

Keep Task 3 `in_progress` across the wait for the user's selection; move to Task 4 only once they answer. Step 6 (wrap-up) completes Task 4.

### Step 1: Locate and determine review scope

Do all of this up front. Do not review anything yet.

1. **Resolve the task** — turn the input (line number, or quoted/named text) into a single `TODO.md` entry.
2. **Determine the review scope** — the files touched on the current branch, **excluding the design docs**: `git diff main...HEAD --name-only -- . ':(exclude)docs/superpowers/**'`. This diff is the **anchor** for the review (see *Review depth* in Step 2).
3. **Confirm only if in doubt** — if the task and review scope are unambiguous, state your read in one line and go straight to Step 2. **Stop and confirm only when the task match is genuinely ambiguous.** Don't over-ask.

### Step 2: Delegate the review — fan out, then consolidate

Dispatch review subagents **in parallel** over the branch diff (design docs excluded), each returning concise findings (not file dumps):

- **Always:** `code-reviewer` (line-level quality) **and** `architect-reviewer` (layering, design, structural concerns).
- **Conditionally:** `security-auditor` — **only when security-relevant files changed**: auth or security config, JWT/token handling, filter chains, crypto, secrets, or new endpoints.

**Review depth — diff-anchored:**

- Read the **full changed files**, not just the diff hunks.
- Follow outward only into code the diff reaches — callers, collaborators, covering tests, dependent config — enough to judge correctness and completeness. Not a whole-repo audit.
- This outward reach surfaces findings beyond the changed lines: an un-updated caller, an absent test, a config that should have changed.

Tell every reviewer to:

- **Judge the code on its own merits** — ignore the spec and plan; no "spec is outdated" or drift findings.
- **Classify each finding** by **kind** — an **issue** (something wrong) or an **improvement** (something better).
- **Suggest** a **severity** (must-fix / should-fix / nice-to-have), **effort**, **impact**, and **disposition** (apply now / defer) per finding.
- **Capture the resolution context each finding needs** — the **Location** (file:line or `Class#method`; a list when it spans several sites), **why it matters** (the concrete consequence), and, when they help, an **Evidence** snippet and **Resolver notes** (gotchas / constraints for whoever fixes it). This is context the reviewer already has in hand; recording it now spares the resolver from rediscovering it.

Then **consolidate** the returns: dedupe overlaps, merge into one list, assign each an `ID`.

### Step 3: Present the findings

A **summary table** sorted by severity (highest first) — the scannable index:

| ID | Title | Kind | Severity | Effort | Impact | Disposition |
|----|-------|------|----------|--------|--------|-------------|

then a **detail block** per finding, carrying the prose the table omits:

```markdown
**S1 — <title>**
- **Location:** <file:line or `Class#method`; a nested list when the finding spans several sites>
- **Description:** <what is wrong — one short, plain sentence>
- **Why it matters:** <the concrete consequence / failure scenario — one line>
- **Evidence:** <offending line(s) or a short snippet — optional, only when it aids confirmation>
- **Recommended action:** <what to do — one short, plain sentence>
- **Resolver notes:** <optional free-form guidance for whoever fixes it — gotchas, constraints, ordering; omit when there's nothing extra>
```

- **Kind** Issue / Improvement · **Severity** must-fix / should-fix / nice-to-have · **Effort** S/M/L · **Impact** High/Med/Low · **Disposition** Apply now / Defer.
- **Apply now** = address before closing this task (→ report file). **Defer** = a separate concern for later (→ `TODO.md`). Either kind can take either disposition.
- **Location**, **Description**, **Why it matters**, and **Recommended action** are always present, each terse (Evidence a few lines). **Evidence** and **Resolver notes** appear only when they earn their place; the notes never restate Description or Why. Write in terms a non-specialist could act on.

### Step 4: Ask which to act on, then wait

Ask the user which findings to act on (`AskUserQuestion` or a clear numbered list). The user may override any suggested **disposition** (apply now ↔ defer). **Wait for the selection. Do not write any file before this.**

### Step 5: Route the selected findings

Route **only the selected findings** — apply-now findings to a report file, deferred findings to `TODO.md`.

**Apply now → the report file.** Write all selected apply-now findings to `docs/reviews/<task-slug>-review.md` (`<task-slug>` = a short kebab-case slug from the task title, e.g. `docs/reviews/find-tasks-by-ids-review.md`); create `docs/reviews/` if absent, and overwrite an existing report for the same task. Use review-version's findings format:

- lead with a title (`# Task Review — <task title>`), an anchor line (`` `main...HEAD` · <N> files ``), and a one-line disclaimer (apply-now findings only; deferred findings routed to `TODO.md`; code review only, nothing committed);
- group findings into **Must-fix**, **Should-fix**, **Nice-to-have** sections, opened by a counts line (e.g. `1 must-fix · 3 should-fix · 1 nice-to-have`); skip an empty section;
- each section = a **summary table** (`| ID | Title | Kind | Effort | Impact |`) plus a **checkbox detail block** per finding:

  ```markdown
  - [ ] **S1 — <title>**
      - **Location:** <file:line or `Class#method`; a nested list when the finding spans several sites>
      - **Description:** <what is wrong — one short, plain sentence>
      - **Why it matters:** <the concrete consequence / failure scenario — one line>
      - **Evidence:** <offending line(s) or a short snippet — optional, only when it aids confirmation>
      - **Recommended action:** <what to do — one short, plain sentence>
      - **Resolver notes:** <optional free-form guidance for whoever fixes it — gotchas, constraints, ordering; omit when there's nothing extra>
  ```

  IDs prefix by severity (`M#` / `S#` / `N#`); write every checkbox **unchecked** — it is the developer's marker to tick as findings are resolved. **Location / Description / Why it matters / Recommended action** are always present; **Evidence** and **Resolver notes** only when they help.

**Defer → `TODO.md`.** A new top-level entry, formatted for its destination (see `.claude/rules/todo.md`):

- into a **version** → under the correct bucket (`Features` / `Bugfix` / `Technical` / `Docs & Tooling`) as `- [ ] (scope) <finding>`, the scope from the rule's vocabulary;
- into the **Backlog** → under the matching `#### <scope>` within `### Features` / `### Technical`, as a bare `* <finding>` (the Backlog shape — no checkbox, no inline tag).

If no home fits, **propose a new bucket/area and confirm before creating it**:

```markdown
### Technical
- [ ] (scope) <deferred finding, in a version>

#### <scope>
* <deferred finding, in the Backlog>
```

Word each `TODO.md` entry per the TODO Wording conventions (`.claude/rules/todo.md`): imperative, ~10 words, plain language, sentence case, no trailing period. A version entry carries a `(scope)`; a Backlog entry does not. Preserve file structure; touch only the relevant spots.

### Step 6: Wrap-up

- Summarize what was routed and where — apply-now findings → the report file (give its path); deferred → which `TODO.md` sections.
- Remind the user: nothing was committed; next is `asapp-resolve-review-issues` to fix the apply-now findings, then their manual close (merge, etc.).

## Delegation & tools — quick reference

| Situation | Use |
|-----------|-----|
| Line-level code quality | `code-reviewer` |
| Architecture, layering, design drift | `architect-reviewer` |
| Security-relevant changes | `security-auditor` |
| Locate / understand touched code | `Explore` |
| IDE problems & inspections | IntelliJ MCP (`get_file_problems`, `run_inspection_kts`) |

## Hard rules

- **Review and report only** — never change code, never commit, never push.
- The **only files you may write are the report at `docs/reviews/<task-slug>-review.md` and `TODO.md`**, and only after the user selects findings.
- **Confirm in Step 1 only when the task match is in doubt** — otherwise state your read and proceed.
- **Ignore the spec and plan** — exclude `docs/superpowers/` from review; never flag the spec as outdated or drifted (reconciled at close).
- **Keep the main context clean** — delegate the review to subagents.
- **Do not close the task or merge** — that is the user's manual step.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Starting Step 1 before creating the four tracking tasks | Do Step 0 first — create the tasks, then begin. |
| Writing files before the user selects | Present findings → wait for selection → then route. |
| Cramming description and action into the summary table | The table holds the short attributes; a detail block per finding carries the prose. |
| Omitting Location so the resolver must rediscover the site | Record file:line / `Class#method` (a list if multi-site) — context the reviewer already has. |
| Padding every finding with Evidence and Resolver notes | Both are optional — include only when they aid confirmation or carry a real gotcha. |
| Logging apply-now findings into `TODO.md` | Apply-now findings go to `docs/reviews/<task-slug>-review.md`; only deferred findings go to `TODO.md`. |
| Writing a deferred version finding as a bare bullet | Version entry = `- [ ] (scope) <finding>`; only the Backlog uses a bare `*` bullet. |
| Creating a new section silently | Propose and confirm before adding to it. |
| Heavy jargon in the table or entries | One short plain sentence each — triage, not internals docs. |
