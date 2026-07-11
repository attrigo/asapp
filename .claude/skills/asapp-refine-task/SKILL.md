---
name: asapp-refine-task
description: >
  Rewrites a vague, high-level backlog entry into a clearer parent task plus smaller, well-scoped subtasks for TODO.md.
  Use when the user wants to refine, decompose, break down, split, scope, or rewrite a TODO.md task, idea, or concept
  into smaller actionable subtasks, or says a backlog entry is too big, vague, or unscoped to hand to brainstorming.
  Triggers: /asapp-refine-task, refine task, refine TODO, decompose task, break down task, split this task, scope this entry.
  Do NOT use for implementation planning or design (use brainstorming/writing-plans), for executing the task itself,
  for editing task status or checkboxes in TODO.md, or to prepare the whole next version's backlog (use asapp-prepare-version).
---

# Refine Task

Turn one vague, high-level `TODO.md` entry into a clearer **parent task** plus a few **smaller, well-scoped subtasks**, so the downstream SDD flow (brainstorming → writing-plans) receives tight, focused input.

**Core principle:** subtasks describe the *what* and *why* at a scoped level — outcomes a developer could pick up and brainstorm. The *how* (libraries, config keys, annotations, file paths, step-by-step changes) stays out; it belongs in brainstorming. A refined entry is a **backlog item, not an implementation plan**.

## Usage

- `/asapp-refine-task <line-number>` — refine the entry at that line of `TODO.md`.
- `/asapp-refine-task <quoted or named task>` — locate the matching entry and refine it.
- `/asapp-refine-task` — ask which entry to refine.

## Process

### 0. Set up progress tracking

**Before any other step**, create these four tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when done:

1. Locate the task and clarify intent (Steps 1–2)
2. Gather project context via a subagent (Step 3)
3. Compose the refined block — parent, subtasks, and extras (Steps 4–6)
4. Propose and edit `TODO.md` on approval (Step 7)

Keep task 4 `in_progress` across the wait for the user's approval; mark it `completed` only once the edit lands or the user declines.

### 1. Locate and read the task

- Resolve the input to one `TODO.md` entry (line number, or quoted / named text). If several entries plausibly match, show the candidates and confirm.
- Record where it lives — version section + bucket, or Backlog area, and indent level — so the refined block returns to the same place.
- Skip a closed entry — one already done (`[X]`) or dropped (a `Decisions` `**Rejected:**` / `**Dropped:**`).

### 2. Clarify intent

Judge whether purpose, scope, goal, and domain are clear from the entry plus the repo.

- **Clear** — state your read in one line and continue; don't over-ask.
- **Genuinely ambiguous** — one batched `AskUserQuestion` round, only questions whose answers change the outcome (e.g. "learning a technology, or solving a concrete pain?").

### 3. Gather project context

**Delegate the grounding to a single `Explore` subagent** — keep the reading out of the main context; only its concise report returns. Go inline only when the entry is trivially small (grounds against ~1–2 files), where a dispatch costs more than it saves.

Tell the subagent to:
- Inspect the involved resources — related code, docs, config, matching `.claude/rules/` files, recent commits.
- Find the real footprint and constraints, and flag anything the task names that is **not yet in the stack**.
- Return a concise grounding report — paths, constraints, footprint — not file dumps.

This grounding **informs** the subtasks; it does not become them. Resist turning every file it surfaces into a subtask.

### 4. Rewrite the parent task

Reframe the parent to its underlying goal or capability, or keep it if already a good scoped goal (Decomposition and Wording conventions — `.claude/rules/todo.md`).

### 5. Decompose into subtasks

Apply the **Decomposition** and **Wording** conventions (`.claude/rules/todo.md`) to every subtask.

### 6. Propose extra improvements

- Suggest genuinely *additive* subtasks when they clearly strengthen the work (resilience, validation, docs, observability) — beyond the literal 1:1 translation. No per-feature test subtask (see the rule's Decomposition).
- Research the web **only** when the topic is fast-moving or you are unsure of current best practice; otherwise propose from existing knowledge.
- Keep extras few — don't pad. Flag which subtasks are proposals in the Step 7 rationale, keeping the markdown itself clean.

### 7. Deliver — propose, then edit on approval

1. Show the refined block in a fenced code block, plus a **one-line** rationale naming which subtasks are proposed extras.
2. Wait for approval.
3. On approval, edit `TODO.md` in place — replace the original entry and its existing children with the refined block; preserve placement and indentation.
4. If the user declines or requests changes, adjust and re-show; don't edit until approved.

## Output format

Match the entry's location — a **version** section or the **Backlog** — exactly.

- **Version parent** — keeps its `- [ ]` checkbox and `(scope)` tag; subtasks are nested `- [ ]`, inheriting the scope (no tag of their own).
- **Backlog parent** — bare `*`, no checkbox, no scope; subtasks are nested `*` (see `.claude/rules/todo.md`).

**Example — version entry (reframe to the goal, the canonical case):**

Before:
```markdown
- [ ] (http-clients) Replace REST clients with declarative HTTP clients
```
After:
```markdown
- [ ] (http-clients) Improve HTTP clients
    - [ ] Refactor REST clients to declarative HTTP clients
    - [ ] Use circuit breaker pattern
    - [ ] Use retry pattern
```
The parent rises from a literal swap to the capability; the resilience patterns are proposed extras.

**Example — Backlog entry (Backlog shape):**

Before:
```markdown
* Enrich task domain (dates, status, estimation, labels, subtasks, assignee)
```
After:
```markdown
* Enrich the task domain
    * Add scheduling fields to tasks
    * Add status and estimation to tasks
    * Add labels and assignees to tasks
    * Support subtasks
```
The grab-bag parenthetical splits into a few coherent subtasks; bare `*`, no checkbox or scope.

## Guardrails

- **Write only `TODO.md`**, and only after the user approves the block.
- **Stay in place** — never move an entry between sections or touch unrelated entries.
- **Never invent a scope** — choose from the vocabulary in `.claude/rules/todo.md`.
