---
name: resolve-review-issues
description: >
  Resolves the review issues you logged under a completed task in TODO.md, one issue at a time.
  Use when the initial implementation of a TODO.md task is done — typically by the superpowers SDD
  flow — and you have written issues, review notes, or follow-ups as bullets below that task and want
  them worked through and committed.
  Triggers: /resolve-review-issues, resolve review issues, fix the issues I logged under this task,
  work through my review notes, address the review findings in TODO.
  Do NOT use to perform a code review (you log the issues yourself first; for delegated review use the
  review agents), to refine or decompose a task (use refine-task), or to implement a brand-new task
  from a plan (use the SDD flow).
---

# Resolve Review Issues

Work through the review issues **you already logged** beneath a `TODO.md` task — the issues from your
manual review after the implementation landed. For each issue: explore, propose,
apply, commit. One issue, one commit, then the next.

**Core principle:** this skill *resolves issues you identified*; it does not hunt for new ones. It works
**strictly one issue at a time**, **never modifies a file before you approve the fix**, and treats each
resolved issue as **its own commit**. Exploration and fixes are **delegated to subagents** to keep the
main context clean.

## Usage

- `/resolve-review-issues <line-number>` — resolve issues under the task at that line of `TODO.md`
- `/resolve-review-issues <quoted or named task>` — locate the matching task, then resolve its issues
- `/resolve-review-issues` (no argument) — ask which task

## This skill is NOT

- **Not a code reviewer** — you (or a delegated review agent) already found the issues; this resolves them.
- **Not task decomposition** — to split or rewrite a backlog entry, use `refine-task`.
- **Not fresh implementation** — to build a new task from a plan, use the SDD flow (brainstorming → writing-plans → subagents).
- **Not autonomous** — every code change waits for your explicit approval. Nothing is modified before you pick a solution.

## Process

### Step 1: Locate, gather context, confirm ONCE

Do all of this up front, then present **one** confirmation. Do not solution anything yet.

1. **Resolve the task** — turn the input (line number, or quoted/named text) into a single `TODO.md` entry. If the match is ambiguous, show candidates and confirm.
2. **Enumerate the issues** — read the bullets logged beneath the task. These are the review issues you added (distinct from the implementation subtasks — usually appended after them, or grouped under an `Issues:`/`Review:` label). List them in order; do **not** analyze them.
3. **Auto-discover supporting context** (note paths only — do not open the files yet):
   - **Spec** — `docs/superpowers/specs/YYYY-MM-DD-<slug>-design.md` matched to the task slug.
   - **Plan** — `docs/superpowers/plans/YYYY-MM-DD-<slug>.md` matched to the task slug (may not exist).
   - **Commit range** — from `git log`, the contiguous block of recent commits implementing the task. Propose `<first>..<last>`.
4. **Confirm** — present the task, the **ordered issue list**, the matched spec/plan paths, and the proposed commit range. Wait for the user to confirm or correct. This single confirmation replaces the manual fill-in-the-blanks.

> **Never read the full spec or plan** — they are too big. Only their paths matter here; the relevant slice is loaded per-issue by the exploration subagent.

### Step 2: Per-issue loop (go one by one)

Process issues **strictly in order, one at a time**. Do not analyze or propose solutions for later issues until the current one is committed.

```dot
digraph per_issue {
    "Next issue" [shape=box];
    "Clear?" [shape=diamond];
    "Ask user (AskUserQuestion)" [shape=box];
    "Explore via subagent" [shape=box];
    "Propose solutions, recommend one (AskUserQuestion)" [shape=box];
    "Apply chosen fix via subagent" [shape=box];
    "Approved?" [shape=diamond];
    "Commit (draft-commit-msg)" [shape=box];
    "More issues?" [shape=diamond];
    "Wrap-up" [shape=doublecircle];

    "Next issue" -> "Clear?";
    "Clear?" -> "Ask user (AskUserQuestion)" [label="no"];
    "Clear?" -> "Explore via subagent" [label="yes"];
    "Ask user (AskUserQuestion)" -> "Explore via subagent";
    "Explore via subagent" -> "Propose solutions, recommend one (AskUserQuestion)";
    "Propose solutions, recommend one (AskUserQuestion)" -> "Apply chosen fix via subagent";
    "Apply chosen fix via subagent" -> "Approved?";
    "Approved?" -> "Apply chosen fix via subagent" [label="changes requested"];
    "Approved?" -> "Commit (draft-commit-msg)" [label="yes"];
    "Commit (draft-commit-msg)" -> "More issues?";
    "More issues?" -> "Next issue" [label="yes"];
    "More issues?" -> "Wrap-up" [label="no"];
}
```

For the current issue:

- **a. Understand** — read it; state its intent and purpose in one or two lines. If genuinely unclear, **stop and ask** (`AskUserQuestion`) before exploring.
- **b. Explore (delegate)** — dispatch a subagent to investigate the involved code, the relevant commits from the range, and **only the relevant slice** of the spec/plan. Pick the most specific agent (a review agent like `code-reviewer`/`architect-reviewer` to assess, or a domain specialist to locate). It returns a concise findings report — not file dumps.
- **c. Propose** — from the findings, propose one or several solutions **with a recommended one**, via `AskUserQuestion`. Name the trade-offs briefly.
- **d. Wait** — wait for the user's choice. **Do not modify any file before this.**
- **e. Apply (delegate)** — dispatch the most specific specialist subagent (`spring-boot-developer`, `test-automator`, `devops-engineer`, `documentation-engineer`, `claude-docs-maintainer`, …) to apply the chosen fix. Follow `.claude/rules/` and TDD where code is involved. Use IntelliJ MCP for IDE-grade operations (safe rename refactor, reformat, inspections) when appropriate.
- **f. Review & approve** — show the user what changed (diff/summary). Wait for approval. If changes are requested, iterate (back to **e**) before committing.
- **g. Commit** — build the message with the `draft-commit-msg` skill, then commit **only this issue's changes**. One issue = one commit. If the logged issues are checkboxes, optionally tick the resolved one in `TODO.md`.
- **h. Continue** — move to the next issue. Repeat until none remain.

### Step 3: Wrap-up

- Summarize what was resolved: issue → commit, in order.
- **Do not** mark the parent task complete or merge — closing the task (merge to main, etc.) is the user's manual step.

## Delegation & tools — quick reference

| Situation | Use |
|-----------|-----|
| Locate / understand code for an issue | `Explore`, or `code-reviewer` / `architect-reviewer` |
| Apply a production-code fix | `spring-boot-developer` |
| Add or adjust tests | `test-automator` |
| CI / Docker / observability fix | `devops-engineer` |
| README / changelog / docs fix | `documentation-engineer` |
| Agent / rule / skill (`.claude/`) fix | `claude-docs-maintainer` |
| Safe rename, reformat, inspections | IntelliJ MCP |
| Build the commit message | `draft-commit-msg` skill |
| A fix triggers a test failure or bug | `superpowers:systematic-debugging` |

Pick the **most specific** agent from `.claude/agents/`; `general-purpose` is a last resort.

## Hard rules

- **Never modify a file until the user approves the chosen solution** for the current issue.
- **One issue at a time** — Step 1 only *enumerates* the issues; never analyze or propose solutions for all of them up front.
- **Never fully read the spec or plan** — load only the slice an issue needs, via the exploration subagent.
- **Keep the main context clean** — delegate both exploration and fixes to subagents.
- **One issue → one commit**, message built with `draft-commit-msg`.
- **Do not close the parent task or merge** — that is the user's manual step.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Analyzing or proposing solutions for all issues at the start | Enumerate only in Step 1; solution one issue at a time. |
| Editing code before the user picks a solution | Propose first; apply only after approval. |
| Reading the whole spec or plan file | Load only the relevant slice, via the exploration subagent. |
| Doing exploration or fixes in the main session | Delegate to subagents to keep context clean. |
| Batching several issues into one commit | One issue = one commit. |
| Reaching for `general-purpose` | Pick the most specific `.claude/agents/` match. |
| Hunting for new issues | This skill only resolves the issues already logged. |
| Marking the task done or merging at the end | That is the user's manual close. |
| Skipping the single up-front confirmation | Confirm task, issue list, spec/plan, and commit range once before starting. |
