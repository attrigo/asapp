---
name: asapp-resolve-review-issues
description: >
  Use to work through and commit already-identified review issues, from either of two sources: issues
  the developer logged as plain nested bullets under a TODO.md task, or findings in a review report under
  docs/reviews/*.md (written by asapp-review-task or asapp-review-version). Both share one per-issue flow.
  Triggers: /asapp-resolve-review-issues, resolve review issues, fix the issues I logged under this task,
  work through my review notes, address the review findings, resolve the findings in the review report,
  fix the readiness report, work through a docs/reviews file.
  Do NOT use to perform a code review (the findings are identified first — for delegated review use the
  review skills/agents), to refine or decompose a task (use asapp-refine-task), or to implement a
  brand-new task from a plan (use the SDD flow).
---

# Resolve Review Issues

Work through already-identified review issues and commit the fixes — **one issue at a time**, with exploration and fixes **delegated to subagents** while the main context orchestrates. Issues come from either of two sources (see **Sources**); the resolution flow is the same for both.

**Core principle:** this skill resolves issues already identified — it does not hunt for new ones. See **Guardrails** for the full constraints.

## Usage

- `/asapp-resolve-review-issues <line-number>` — **TODO:** resolve issues under the task at that line of `TODO.md`.
- `/asapp-resolve-review-issues <docs/reviews/….md>` — **Report:** resolve the findings in that report.
- `/asapp-resolve-review-issues` (no argument) — ask which source (a TODO task vs a report), then proceed.

## Process

### 1. Locate and gather context

Do all of this up front. Do not solution anything yet.

1. **Resolve the source and target** — the argument picks the source (see *Usage*): the `TODO.md` task at the given line, or the report at the given path. With no argument, ask which source, offering the current task's TODO issues and any `docs/reviews/*.md`.
2. **Get onto a fix branch** —
   - If on `resolve-issues-version-<ver>`, reuse the current branch (it holds the work).
   - If not, switch to `resolve-issues-version-<ver>`, creating it off `main` if absent. `<ver>` = the report's `v<ver>`, else the task's `## <ver>` section in `TODO.md`. Name the branch in one line.
3. **Enumerate the issues** — list every un-resolved issue **in the order it appears in the source**. Do **not** analyze them yet.
   - **TODO:** the plain nested bullets under the task/subtask.
   - **Report:** the un-ticked `- [ ]` finding blocks, top to bottom; skip any already ticked `- [x]`.
4. **Gather supporting context** — source-specific:
   - **TODO:** auto-discover, noting paths only (do not open the files yet):
      - the **spec** — `docs/superpowers/specs/YYYY-MM-DD-<slug>-design.md`
      - the **plan** — `docs/superpowers/plans/YYYY-MM-DD-<slug>.md` (may not exist)
      - a **commit range** from the branch point vs `main` (`git log main..HEAD` → propose `<first>..<last>`).
   - **Report:** none up front.
      - Each finding's **Location**, **Description**, **Why it matters**, optional **Evidence**, **Recommended action**, and optional **Resolver notes** are the brief.
      - The per-issue exploration subagent (Step 2b) confirms the involved code **from the finding's Location** — and reads any spec slice it needs — on demand.
      - Do not derive a commit range or open any spec/plan here.
5. **Confirm only if in doubt** — if the source, target, issue list (and, for TODO, the spec/plan and commit range) are unambiguous, state your read in one line and go straight to Step 2.

### 2. Per-issue loop (go one by one)

Process issues **strictly in order, one at a time**. Do not analyze or propose solutions for later issues until the current one is committed.

For the current issue:

- **a. Understand** — read it; state its intent and purpose in one or two lines. If genuinely unclear, **stop and ask** (`AskUserQuestion`) before exploring.
- **b. Explore (delegate)** — dispatch a subagent to investigate the involved code and **only the relevant slice** of any spec/plan (in TODO mode, also the relevant commits from the range). Pick the most specific agent. It returns a concise findings report — not file dumps.
  - **In report mode, start from the finding's Location** — go straight to those sites to confirm the issue and reach what the fix touches, rather than rediscovering them.
- **c. Propose** — do two things, in order:
  1. **Print a short context block first** (plain text, *before* any `AskUserQuestion`) so the choice is legible on its own — a tight, scannable bullet list from (a) and the (b) findings: **what** the issue is · **why it matters** (impact / risk) · **where** (the affected code). As many bullets as the decision needs, no more; not prose.
      - In report mode, **lift this from the finding** — its Description, Why it matters, and Location — extending only with what (b) confirmed.
  2. **Then propose** one or several solutions **with a recommended one**, via `AskUserQuestion`, naming each trade-off briefly. **Fold in any Resolver notes on the finding** (gotchas, constraints, ordering). (A report nice-to-have may carry no **Recommended action** — propose one anyway; don't assume one was given.)
- **d. Wait** — wait for the user's choice.
- **e. Apply** — how you apply depends on whether the fix changes runtime behavior:
  - **Behavioral fix** (bug fix, logic change, new validation or edge case): **drive `superpowers:test-driven-development` from the main context** — it owns the RED→GREEN→refactor loop and its checkpoints. The TDD skill delegates the small concrete steps — authoring the failing test, then the production fix — to the most specific specialist subagent.
  - **Non-behavioral fix** (docs, comments, formatting, config without logic, pure rename): dispatch the most specific specialist subagent to apply the change directly.
  - Always follow `.claude/rules/`.
  - Use IntelliJ MCP for IDE-grade operations (safe rename refactor, reformat, inspections) when appropriate.
- **f. Review & approve** — show the user what changed (diff/summary). Wait for approval. If changes are requested, iterate (back to **e**) before committing.
- **g. Mark done** — apply the source's mark-done edit (see **Sources**) in the working tree, as a progress marker. **Do not commit it.**
  - **Report:** also add an **Applied:** bullet as the finding's last bullet, briefly summarizing what was actually applied.
  - **TODO:** nothing extra.
- **h. Commit** — stage and commit **only the fix**. Build the message with the `asapp-draft-commit-msg` skill, then commit.
- **i. Continue** — move to the next issue. Repeat until none remain.

### 3. Wrap-up

- Summarize what was resolved: issue → commit, in order.
- The mark-done edits (removed `TODO.md` bullets / ticked report boxes) stay **uncommitted** in the working tree.

## Sources

The invocation argument picks the source. Only two things differ between the sources — **where issues are read** and **how a resolved issue is marked done**. Everything else (the per-issue loop, delegation, one-issue-one-commit) is shared.

| Source | Issues are read from | Mark an issue done by |
|--------|----------------------|-----------------------|
| **TODO** | plain nested bullets `- <issue>` one level under the task/subtask they concern | **removing** the `- <issue>` line from `TODO.md` |
| **Report** | un-ticked finding blocks (`- [ ] **M1 — …**`) | **ticking** `- [ ]` → `- [x]` and appending an **Applied:** bullet as last bullet summarizing what was applied |

- **TODO:** `- **Note:** …` / `- **Warning:** …` bullets are *context*, not issues; do not enumerate them.
- **Report:** one report mode covers both report kinds (`<task-slug>-review.md` from `asapp-review-task` and `v<ver>-readiness-report.md` from `asapp-review-version`); their finding format and mark-done are identical.

## Delegation

| Situation | Use |
|-----------|-----|
| Locate / understand code for an issue | `Explore`, or `code-reviewer` / `architect-reviewer` |
| Apply a production-code fix | `spring-boot-developer` |
| Add or adjust tests | `test-automator` |
| CI / Docker / observability fix | `devops-engineer` |
| README / changelog / docs fix | `documentation-engineer` |
| Agent / rule / skill (`.claude/`) fix | `claude-docs-maintainer` |
| Safe rename, reformat, inspections | IntelliJ MCP |
| Build the commit message | `asapp-draft-commit-msg` skill |
| A fix triggers a test failure or bug | `superpowers:systematic-debugging` |

## Guardrails

- **Never modify a file until the user approves** the chosen solution for the current issue.
- **One issue at a time** — Step 1 only *enumerates* the issues; never analyze or propose solutions for all of them up front.
- **Process issues in source order** — never re-sort or regroup them.
- **Never fully read the spec or plan** — in TODO mode, load only the slice an issue needs via the exploration subagent; in report mode, gather no spec/plan/commit context up front.
- **Keep the main context clean** — delegate exploration and fix authoring to subagents; the main context orchestrates.
- **Never commit onto `main`** — work on `resolve-issues-version-<ver>` (Step 1).
- **One issue → one commit, the fix only** — never stage or commit `TODO.md` or `docs/reviews/` changes; the mark-done edit stays in the working tree.
- **Do not close the task, merge, or release** — that is the user's separate manual step (`asapp-close-task` / `asapp-release`).
