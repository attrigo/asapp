---
name: review-task
description: >
  Performs a final, delegated review of a task's branch and logs the chosen findings into TODO.md,
  before the task is closed.
  Use when the implementation and your earlier fix pass for a TODO.md task are done and you want a fresh,
  thorough review of the current branch — surfacing bugs, gaps, and improvement ideas — before closing it.
  Triggers: /review-task, final review, review the branch before closing, review the current status of
  the app, find issues and improvements before I close this task, audit the changes.
  Do NOT use to fix or commit anything (it only reviews and logs — use resolve-review-issues to fix the
  logged findings), to refine or decompose a task (use refine-task), or to review an external pull
  request (use the PR review tools).
---

# Review Task

The final review gate before you close a task. Delegate a thorough review of the
current branch, present **one** prioritized findings table, then log the findings you pick into
`TODO.md`. Pairs with `resolve-review-issues`, which fixes the in-scope findings afterwards.

**Core principle:** this skill **reviews and logs only** — it changes **no code** and makes **no
commits**. The only file it edits is `TODO.md`, and only after you select which findings to keep. It
surfaces three kinds of findings: real **issues**, **missing/incomplete/improvable** work, and
**out-of-scope ideas** for later.

## Usage

- `/review-task <line-number>` — review the branch for the task at that line of `TODO.md`
- `/review-task <quoted or named task>` — locate the matching task, then review
- `/review-task` (no argument) — ask which task

## This skill is NOT

- **Not a fixer** — it logs findings; use `resolve-review-issues` to resolve them (your point 9).
- **Not your manual first review** — this is the delegated *final* pass before close.
- **Not task decomposition** — to split or rewrite a backlog entry, use `refine-task`.
- **Not a PR reviewer** — for an external pull request, use the PR review tools.
- **Never changes code and never commits or pushes.**

## Process

### Step 1: Locate, gather context, confirm ONCE

Do all of this up front, then present **one** confirmation. Do not review anything yet.

1. **Resolve the task** — turn the input (line number, or quoted/named text) into a single `TODO.md` entry. If ambiguous, show candidates and confirm.
2. **Determine the review scope** — the files touched on the current branch: `git diff main...HEAD --name-only`. This is what the reviewers focus on (app-aware, but diff-centred).
3. **Note supporting context** (paths only — do not open the files):
   - **Spec** — `docs/superpowers/specs/YYYY-MM-DD-<slug>-design.md` matched to the task slug.
   - **Plan** — `docs/superpowers/plans/YYYY-MM-DD-<slug>.md` matched to the task slug (may not exist).
4. **Confirm** — present the task, the review scope (touched files/areas), and the matched spec/plan paths. Wait for the user to confirm or correct.

> **Never read the full spec or plan** — they are too big. Reviewers load only the slice they need.

### Step 2: Delegate the review — fan out, then consolidate

Dispatch review subagents **in parallel** over the branch diff, each returning concise findings (not file dumps):

- **Always:** `code-reviewer` (line-level quality) **and** `architect-reviewer` (layering, design, drift from the spec).
- **Conditionally:** `security-auditor` — **only when security-relevant files changed**: auth or security config, JWT/token handling, filter chains, crypto, secrets, or new endpoints.

Each reviewer classifies its findings into the three buckets (issue / missing-improvable / out-of-scope) and suggests a priority, effort, impact, and scope. Then **consolidate**: dedupe overlaps, merge into one list, assign each finding an `ID`.

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

Log **only the selected findings** — this is the only file write.

- **In scope** → new `* [ ] <finding>` child bullets directly under the task entry, after its existing subtasks, matching indentation. This is exactly the format `resolve-review-issues` reads.
- **Out of scope** → the most appropriate existing section/subsection (e.g. `Security`, `Observability`, `Tech`, `CI/CD`, `Tools`, `Doc`, or the relevant version). If no fitting section exists, **propose a new section and confirm before creating it.**
- Word each entry like `refine-task` conventions: imperative, ~10 words, plain language, sentence case, no trailing period. Preserve file structure; touch only the relevant spots.

### Step 6: Wrap-up

- Summarize what was logged and where (in-scope under the task; out-of-scope → which sections).
- Remind the user: nothing was committed; next is `resolve-review-issues` to fix the in-scope findings, then their manual close (merge, etc.).

## TODO insertion patterns

In scope — under the task:
```markdown
* <task>
    * [X] <existing subtask>
    * [ ] <new in-scope finding>
```

Out of scope — existing or new section:
```markdown
### <Section>
* <existing entry>
* <new out-of-scope finding>
```

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
- **One up-front confirmation** (Step 1), then proceed.
- **Never fully read the spec or plan** — reviewers load only the slice they need.
- **Keep the main context clean** — delegate the review to subagents.
- **Do not close the task or merge** — that is the user's manual step.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Changing code or committing | This skill only reviews and logs; `resolve-review-issues` does the fixing. |
| Editing `TODO.md` before the user selects | Present table → wait for selection → then log. |
| Running one big review in the main session | Fan out to review subagents; consolidate; keep context clean. |
| Reading the whole spec or plan | Reviewers load only the slice they need. |
| Heavy jargon in the table or entries | One short plain sentence each — triage, not internals docs. |
| Logging every finding | Log only the findings the user selected. |
| Putting out-of-scope items under the task | Only in-scope goes under the task; out-of-scope goes to its section. |
| Creating a new section silently | Propose and confirm a new section before adding to it. |
| Closing or merging the task at the end | That is the user's manual step. |
