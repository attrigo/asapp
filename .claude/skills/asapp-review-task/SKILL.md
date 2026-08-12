---
name: asapp-review-task
description: >
  Use when a TODO.md task's implementation and earlier fix pass are done and a fresh, thorough review
  of the current branch is wanted — surfacing bugs, gaps, and improvement ideas — before closing it.
  Triggers: /asapp-review-task, final review, review the branch before closing, review the current status of
  the app, find issues and improvements before I close this task, audit the changes.
  Do NOT use to fix or commit anything (it only reviews and reports — use asapp-resolve-review-issues to fix
  the reported findings), to refine or decompose a task (use asapp-refine-task), to review an external pull
  request (use the PR review tools), or to review a whole shipped version before release (use
  asapp-review-version).
---

# Review Task

The final review gate before a task closes: delegate a thorough review of the current branch to subagents, present prioritized findings, then record every one of them in a report file. Runs before the manual close.

**Core principle:** every finding has a kind — an issue (something is wrong) or an improvement (something could be better) — and every finding is recorded in the report.

## Usage

- `/asapp-review-task <line-number>` — review the branch for the task at that line of `TODO.md`.
- `/asapp-review-task <quoted or named task>` — locate the matching task, then review.
- `/asapp-review-task` — ask which task.

## Process

### 0. Set up progress tracking

**Before any other step**, create these five tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when done:

1. Locate the task and determine review scope (Step 1)
2. Run the review and consolidate (Step 2)
3. Present the findings (Step 3)
4. Write the report (Step 4)
5. Wrap up (Step 5)

### 1. Locate and determine review scope

Do this up front; review nothing yet.

1. **Resolve the task** — turn the input (line number, or quoted / named text) into one `TODO.md` entry.
2. **Scope** — the files touched on the branch, excluding design specs. This diff is the review anchor (see *Depth* in Step 2):
   ```bash
   git diff main...HEAD --stat -- . ':(exclude)docs/superpowers/**'
   ```
3. **Right-size** — the diff is trivial only when all three hold: no production logic changed (docs, comments, config values, mechanical renames), nothing security-bearing touched, and it fits in one read (~≤3 files, ≤30 changed lines). When in doubt, it is not trivial.
4. **State your read** — task, scope, and verdict (`trivial → review inline` / `dispatch`) in one line, then go to Step 2. Stop only when the task match is genuinely ambiguous; don't over-ask.

### 2. Run the review, then consolidate

From Step 1's verdict:

- **Trivial** — review it inline; dispatch nothing.
- **Otherwise** — dispatch in parallel over the branch diff (design specs excluded); each returns concise findings, not file dumps:
  - **Always** — one `code-reviewer` (line-level quality and structural fit).
  - **Only when security-relevant files changed** (auth / security config, JWT / token handling, filter chains, crypto, secrets, new endpoints) — one `security-auditor`.

Depth: read the full changed files, not just the hunks; follow outward only into code the diff reaches — callers, collaborators, covering tests, dependent config — enough to judge correctness and completeness. Not a whole-repo audit. That reach is what surfaces findings beyond the changed lines: an un-updated caller, an absent test, a config that should have changed too.

Every review — inline or delegated — must:
- Judge the code on its own merits — ignore specs / plans; no drift findings.
- Classify each finding by kind — an issue (something wrong) or an improvement (something better).
- Suggest a severity (must-fix / should-fix / nice-to-have), effort, and impact.
- Capture each finding's resolution context — read `.claude/rules/review-report.md` first and hold every field to the shape and caps it defines. This is context the reviewer already holds; recording it now spares the resolver rediscovering it.

Then consolidate: merge into one list (deduping where reviewers overlap) and assign each an `ID`.

### 3. Present the findings

A summary table sorted by severity (highest first) — the whole chat output:

| ID | Title | Kind | Severity | Effort | Impact |
|----|-------|------|----------|--------|--------|

- Kind: issue / improvement · Severity: must-fix / should-fix / nice-to-have · Effort: S/M/L · Impact: High/Med/Low.

### 4. Write the report

Write it on every run — a trivial inline review and one that found nothing included; the report is the record that the review happened.

Write to `docs/reviews/<task-slug>-review.md` (`<task-slug>` = a short kebab-case slug from the task title, e.g. `docs/reviews/find-tasks-by-ids-review.md`). Create `docs/reviews/` if absent; overwrite an existing report for the same task. Lead with:

- title — `# Task Review — <task title>`
- anchor line — `` `main...HEAD` · <N> files ``

then the findings per `.claude/rules/review-report.md` (summary column Kind).

### 5. Wrap-up

- Give the report path and the finding counts.
- Remind the user: nothing was committed. Next is `asapp-resolve-review-issues` to work through the findings, then their manual close (merge, etc.).

## Delegation

| Concern | Use |
|---------|-----|
| Code quality and structural fit | `code-reviewer` |
| Security-relevant changes | `security-auditor` |
| Locate / understand touched code | `Explore` |
| IDE problems & inspections | IntelliJ MCP (`get_file_problems`, `run_inspection_kts`) |

## Guardrails

- **Review and report only** — never change code, commit, push, or merge; closing the task is the user's manual step.
- **The only write is the report** at `docs/reviews/<task-slug>-review.md`.
- **Exclude `docs/superpowers/**`** — never flag a spec as outdated or drifted (reconciled at close).
- **Never review inline unless Step 1 called the diff trivial** — keep the main context clean.
