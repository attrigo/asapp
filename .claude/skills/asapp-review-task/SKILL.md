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

The final review gate before a task closes: delegate a thorough review of the current branch to subagents, present prioritized findings, then route the ones you select — **apply-now** to a report file, **deferred** to `TODO.md`. Runs before the manual close.

**Core principle:** every finding has a **kind** — an **issue** (something is wrong) or an **improvement** (something could be better) — and a **disposition** — **apply now** (fix before closing → report file) or **defer** (a separate concern for later → `TODO.md`). The two are independent; `asapp-resolve-review-issues` fixes the apply-now findings afterwards.

## Usage

- `/asapp-review-task <line-number>` — review the branch for the task at that line of `TODO.md`.
- `/asapp-review-task <quoted or named task>` — locate the matching task, then review.
- `/asapp-review-task` — ask which task.

## Process

### 0. Set up progress tracking

**Before any other step**, create these five tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when done:

1. Locate the task and determine review scope (Step 1)
2. Run the review and consolidate (Step 2)
3. Present findings and take the user's selection (Steps 3–4)
4. Route the selected findings (Step 5)
5. Wrap up (Step 6)

Keep task 3 `in_progress` across the wait for the user's selection; move on only once they answer.

### 1. Locate and determine review scope

Do this up front; review nothing yet.

1. **Resolve the task** — turn the input (line number, or quoted / named text) into one `TODO.md` entry.
2. **Scope** — the files touched on the branch, excluding design specs. This diff is the review **anchor** (see *Depth* in Step 2):
   ```bash
   git diff main...HEAD --name-only -- . ':(exclude)docs/superpowers/**'
   ```
3. **Confirm only if in doubt** — if the task and scope are unambiguous, state your read in one line and go to Step 2. Stop only when the task match is genuinely ambiguous; don't over-ask.

### 2. Delegate the review, then consolidate

Dispatch review subagents **in parallel** over the branch diff (design specs excluded); each returns concise findings, not file dumps.

- **Always** — `code-reviewer` (line-level quality) and `architect-reviewer` (layering, design, structure).
- **Only when security-relevant files changed** (auth / security config, JWT / token handling, filter chains, crypto, secrets, new endpoints) — `security-auditor`.

**Depth** — read the full changed files, not just the hunks; follow outward only into code the diff reaches — callers, collaborators, covering tests, dependent config — enough to judge correctness and completeness. Not a whole-repo audit. That reach is what surfaces findings beyond the changed lines: an un-updated caller, an absent test, a config that should have changed too.

Tell each reviewer to:
- Judge the code on its own merits — ignore specs / plans; no drift findings.
- Classify each finding by **kind** — an **issue** (something wrong) or an **improvement** (something better).
- Suggest a **severity** (must-fix / should-fix / nice-to-have), **effort**, **impact**, and **disposition** (apply now / defer).
- Capture each finding's resolution context — the fields in `.claude/rules/review-report.md`. This is context the reviewer already holds; recording it now spares the resolver rediscovering it.

Then **consolidate**: dedupe overlaps, merge into one list, assign each an `ID`.

### 3. Present the findings

A **summary table** sorted by severity (highest first) — the scannable index:

| ID | Title | Kind | Severity | Effort | Impact | Disposition |
|----|-------|------|----------|--------|--------|-------------|

then one **detail block** per finding — the fields of `.claude/rules/review-report.md`, without the checkbox (nothing is resolved yet). Write in terms a non-specialist could act on.

- **Kind** issue / improvement · **Severity** must-fix / should-fix / nice-to-have · **Effort** S/M/L · **Impact** High/Med/Low · **Disposition** apply now / defer.
- **Apply now** = fix before closing this task (→ report file); **Defer** = a separate concern for later (→ `TODO.md`). Either kind can take either disposition.

### 4. Ask which to act on, then wait

Ask which findings to act on (`AskUserQuestion` or a clear numbered list); the user may override any suggested **disposition** (apply now ↔ defer). **Wait for the selection before routing anything.**

### 5. Route the selected findings

Route **only the selected findings** — apply-now to the report file, deferred to `TODO.md`.

**Apply now → `docs/reviews/<task-slug>-review.md`** (`<task-slug>` = a short kebab-case slug from the task title, e.g. `docs/reviews/find-tasks-by-ids-review.md`). Create `docs/reviews/` if absent; overwrite an existing report for the same task. Lead with:

- title — `# Task Review — <task title>`
- anchor line — `` `main...HEAD` · <N> files ``
- disclaimer — apply-now findings only (deferred routed to `TODO.md`); code review only, nothing committed

then the findings per `.claude/rules/review-report.md` (summary column **Kind**).

**Defer → `TODO.md`** — a new top-level entry formatted for its destination (see `.claude/rules/todo.md`):

- into a **version** → under the right bucket as `- [ ] (scope) <finding>`;
- into the **Backlog** → under the matching `#### <scope>`, as a bare `* <finding>`.

If no home fits, **propose a new bucket / scope and confirm before creating it**. Word each entry per the TODO Wording conventions; preserve file structure, touching only the relevant spots.

### 6. Wrap-up

- Summarize what was routed where — apply-now → the report file (give its path); deferred → which `TODO.md` sections.
- Remind the user: nothing was committed. Next is `asapp-resolve-review-issues` to fix the apply-now findings, then their manual close (merge, etc.).

## Delegation

| Concern | Use |
|---------|-----|
| Line-level code quality | `code-reviewer` |
| Architecture, layering, design drift | `architect-reviewer` |
| Security-relevant changes | `security-auditor` |
| Locate / understand touched code | `Explore` |
| IDE problems & inspections | IntelliJ MCP (`get_file_problems`, `run_inspection_kts`) |

## Guardrails

- **Review and report only** — never change code, commit, push, or merge; closing the task is the user's manual step.
- **The only writes are the report** (`docs/reviews/<task-slug>-review.md`) **and `TODO.md`**, and only after the user selects findings.
- **Exclude `docs/superpowers/**`** — never flag a spec as outdated or drifted (reconciled at close).
- **Delegate all reviewing to subagents** — keep the main context clean.
