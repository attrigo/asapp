---
name: review-task
description: >
  Use when the implementation and your earlier fix pass for a TODO.md task are done and you want a
  fresh, thorough review of the current branch — surfacing bugs, gaps, and improvement ideas — before
  closing it.
  Triggers: /review-task, final review, review the branch before closing, review the current status of
  the app, find issues and improvements before I close this task, audit the changes.
  Do NOT use to fix or commit anything (it only reviews and logs — use resolve-review-issues to fix the
  logged findings), to refine or decompose a task (use refine-task), or to review an external pull
  request (use the PR review tools).
---

# Review Task

The final review gate before a task closes. Delegate a thorough review of the current branch to
subagents, present **one** prioritized findings table, then log the findings you select into `TODO.md`.

**Core principle:** review and log **only** — change no code, make no commits. The only file you
edit is `TODO.md`, and only after the user selects findings. Surface three kinds: real **issues**,
**missing/incomplete/improvable** work, and **out-of-scope ideas** for later. `resolve-review-issues`
fixes the in-scope findings afterwards.

## Usage

- `/review-task <line-number>` — review the branch for the task at that line of `TODO.md`
- `/review-task <quoted or named task>` — locate the matching task, then review
- `/review-task` (no argument) — ask which task

## Process

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
- This outward reach surfaces the *missing/incomplete* findings: an un-updated caller, an absent test, a config that should have changed.

Tell every reviewer to:

- **Judge the code on its own merits** — ignore the spec and plan; no "spec is outdated" or drift findings.
- **Classify each finding** into one of the three buckets (issue / missing-improvable / out-of-scope).
- **Suggest** a priority, effort, impact, and scope per finding.

Then **consolidate** the returns: dedupe overlaps, merge into one list, assign each an `ID`.

### Step 3: Present the prioritized table

One table, **sorted by priority** (highest first):

| ID | Title | Priority | Effort | Impact | Scope | Description | Recommended action |
|----|-------|----------|--------|--------|-------|-------------|--------------------|

- **Priority** High/Med/Low · **Effort** S/M/L · **Impact** High/Med/Low · **Scope** In/Out.
- **In scope** = completes or corrects what this task built / belongs to its goal. **Out of scope** = valid, but a separate concern for later.
- **Description** and **Recommended action** are each **one short, plain sentence** — what is wrong and what to do, in terms a non-specialist could act on.

### Step 4: Ask which to manage, then wait

Ask the user which findings to log (`AskUserQuestion` or a clear numbered list). The user may override any suggested scope. **Wait for the selection. Do not edit `TODO.md` before this.**

### Step 5: Log the selected findings to `TODO.md`

Log **only the selected findings**.

- **In scope** → a plain `*` bullet nested one level under the subtask it concerns:

  ```markdown
  * <main task>
      * [ ] <subtask the finding concerns>
          * <in-scope finding>
  ```

- **Out of scope** → a normal entry in the most appropriate existing section (e.g. `Security`, `Observability`, `Tech`, `CI/CD`, `Tools`, `Doc`, or the relevant version); if none fits, **propose a new section and confirm before creating it**:

  ```markdown
  ### <Section>
  * <out-of-scope finding>
  ```

- Word each entry like `refine-task` conventions: imperative, ~10 words, plain language, sentence case, no trailing period. Preserve file structure; touch only the relevant spots.

### Step 6: Wrap-up

- Summarize what was logged and where (in-scope under the task; out-of-scope → which sections).
- Remind the user: nothing was committed; next is `resolve-review-issues` to fix the in-scope findings, then their manual close (merge, etc.).

## Delegation & tools — quick reference

| Situation | Use |
|-----------|-----|
| Line-level code quality | `code-reviewer` |
| Architecture, layering, design drift | `architect-reviewer` |
| Security-relevant changes | `security-auditor` |
| Locate / understand touched code | `Explore` |
| IDE problems & inspections | IntelliJ MCP (`get_file_problems`, `run_inspection_kts`) |
| Framing the review pass | `superpowers:requesting-code-review` |
| A finding needs deeper diagnosis | `superpowers:systematic-debugging` |

Pick the **most specific** agent from `.claude/agents/`; `general-purpose` is a last resort.

## Hard rules

- **Review and log only** — never change code, never commit, never push.
- The **only file you may edit is `TODO.md`**, and only after the user selects findings.
- **Confirm in Step 1 only when the task match is in doubt** — otherwise state your read and proceed.
- **Ignore the spec and plan** — exclude `docs/superpowers/` from review; never flag the spec as outdated or drifted (reconciled at close).
- **Keep the main context clean** — delegate the review to subagents.
- **Do not close the task or merge** — that is the user's manual step.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Editing `TODO.md` before the user selects | Present table → wait for selection → then log. |
| Putting out-of-scope items under the task | Only in-scope goes under the task; out-of-scope goes to its section. |
| Creating a new section silently | Propose and confirm before adding to it. |
| Heavy jargon in the table or entries | One short plain sentence each — triage, not internals docs. |
