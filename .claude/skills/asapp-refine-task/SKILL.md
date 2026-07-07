---
name: asapp-refine-task
description: >
  Rewrites a vague, high-level backlog entry into a clearer parent task plus smaller, well-scoped subtasks for TODO.md.
  Use when the user wants to refine, decompose, break down, split, scope, or rewrite a TODO.md task, idea, or concept
  into smaller actionable subtasks, or says a backlog entry is too big, vague, or unscoped to hand to brainstorming.
  Triggers: /asapp-refine-task, refine task, refine TODO, decompose task, break down task, split this task, scope this entry.
  Do NOT use for implementation planning or design (use brainstorming/writing-plans), for executing the task itself,
  or for editing task status or checkboxes in TODO.md.
---

# Refine Task

Turn one vague, high-level `TODO.md` entry into a clearer **parent task** plus a few **smaller, well-scoped subtasks**, so the downstream SDD flow (brainstorming → writing-plans) receives tight, focused input.

**Core principle:** subtasks describe the *what* and *why* at a scoped level — outcomes a developer could pick up and brainstorm. The *how* (libraries, config keys, annotations, file paths, step-by-step changes) stays out; it belongs in brainstorming. A refined entry is a **backlog item, not an implementation plan**.

## Usage

- `/asapp-refine-task <line-number>` — refine the entry at that line of `TODO.md`
- `/asapp-refine-task <quoted or named task>` — locate the matching entry and refine it
- `/asapp-refine-task` (no argument) — ask which entry to refine

## This skill is NOT

- **Not an implementation plan** — subtasks are scoped deliverables, never coding steps. If a subtask names a dependency, config key, or file, you have dropped a level — pull back up.
- **Not design or brainstorming** — no architecture, no trade-offs, no *how*.
- **Not task execution** — you rewrite the backlog entry; you do not implement it.

## Process

### Step 0: Set up progress tracking

**Before any other step**, create these four tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when it's done:

1. Locate the task and clarify intent (Steps 1–2)
2. Gather project context (Step 3)
3. Compose the refined block — parent, subtasks, and extras (Steps 4–6)
4. Propose and edit `TODO.md` on approval (Delivery)

Keep Task 4 `in_progress` across the wait for the user's approval; mark it `completed` only once the edit lands or the user declines.

### Step 1: Locate and read the task

- Resolve the input to a single `TODO.md` entry: a line number, or quoted/named text matched against the file.
- If the match is ambiguous (several plausible entries), show the candidates and confirm before continuing.
- Record where it lives: a version section (and its bucket) or a Backlog area, and the indent level. The refined block goes back in the **same place** — never move an entry between sections, and never refine one already complete (`[X]`) or dropped (a `Decisions` entry: `**Rejected:**` / `**Dropped:**`).

### Step 2: Clarify intent

- Judge whether purpose, scope, goal, and domain are clear from the entry plus the repo.
- **If clear:** state your read in one line and continue. Do not over-ask.
- **If genuinely ambiguous:** ask the user — one batched round via `AskUserQuestion`, only questions whose answers change the outcome (e.g. "is this for learning a technology, or solving a concrete pain?").

### Step 3: Gather project context

- Inspect the involved resources before rewriting: related code, docs, config, matching files in `.claude/rules/`, and recent commits.
- Find the real footprint and constraints so the decomposition is grounded, not guessed. This grounding is essential — but it informs the subtasks; it does not become them. Resist turning every file you find into a subtask.

### Step 4: Rewrite the parent task

- Reframe to the underlying goal or capability when that is clearer than a literal restatement — e.g. `Replace REST clients by declarative HTTP clients` → `Improve HTTP clients`.
- Keep it if the original is already a good scoped goal.
- Apply the Wording conventions below.

### Step 5: Decompose into subtasks

Apply the rule's **Decomposition** conventions (`.claude/rules/todo.md`). In practice:

- Break the parent into smaller, scoped, domain-focused subtasks, each commit-sized and self-contained (they may run in sequence, but each stands on its own).
- If you're producing 10+ items that name config keys, file names, or sequential edits, you have slipped into implementation planning — collapse them back up to backlog-sized outcomes.
- Apply the Wording conventions below to every subtask.

### Step 6: Propose extra improvements

- Suggest genuinely *additive* subtasks when they clearly strengthen the work (resilience, validation, docs, observability) — beyond the literal 1:1 translation. Do not propose a test subtask here; follow the rule's **Decomposition** test guidance.
- Research the web **only** when the topic is fast-moving or you are unsure of current best practice; otherwise propose from existing knowledge.
- Keep extras optional and few — do not pad. Flag which subtasks are proposals in the chat rationale (Step 7), keeping the markdown itself clean.

## Wording conventions

Apply the TODO **Wording** conventions (`.claude/rules/todo.md`) to the parent task and every subtask.

## Output format

Match the structure of the entry's location — a **version** section or the **Backlog** — exactly.

**Version entry** (a `0.x` section):

```markdown
- [ ] (scope) <Parent task>
    - [ ] <Subtask>
    - [ ] <Subtask>
    - [ ] <Subtask>
```

The parent keeps its `- [ ]` checkbox and carries a `(scope)` tag; each subtask is a `- [ ]` and inherits that scope (no tag of its own).

**Backlog entry:**

```markdown
* <Parent task>
    * <Subtask>
    * <Subtask>
```

Bare `*` bullets, no checkboxes, no scope — the Backlog shape (see `.claude/rules/todo.md`).

- Preserve the entry's placement and indentation; never move it between sections.
- Choose the `(scope)` from the vocabulary in `.claude/rules/todo.md` — never invent a new scope.
- Replace the targeted entry (and any existing children of it) with the refined block. Never touch unrelated entries.

## Delivery (propose, then edit on approval)

1. Show the refined block in a fenced code block, plus a **one-line** rationale that names which subtasks are proposed extras.
2. Wait for the user's approval.
3. On approval, edit `TODO.md` in place: replace the original entry with the refined block, preserving placement and indentation, leaving the rest of the file untouched.
4. If the user declines or requests changes, adjust and re-show — do not edit until approved.

## Examples

**Example 1 — reframe to the goal (the canonical case):**

Before (a version entry):
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
Rationale: the parent rises from a literal swap to the capability ("Improve HTTP clients"); resilience patterns are proposed extras beyond the original ask.

**Example 2 — a Backlog entry (Backlog shape, no checkboxes or scope):**

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
Rationale: a Backlog entry stays in Backlog style — bare `*`, no checkboxes, no `(scope)` — while the grab-bag parenthetical splits into a few coherent subtasks.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Starting Step 1 before creating the four tracking tasks | Do Step 0 first — create the tasks, then begin. |
| Subtasks name libraries, config keys, annotations, or file paths | That is *how*. Restate as a scoped outcome in plain language. |
| Subtasks are full sentences with colons and embedded lists | Compress to ~10-word imperative phrases; one unit per line. |
| 10+ micro-steps mirroring an implementation plan | You dropped a level. Pull back up to backlog-sized outcomes (aim 2–6). |
| Adding a per-feature "add tests for X" subtask | TDD delivers tests inside each subtask. Add a test subtask only when a new test tier or test infrastructure is the deliverable. |
| A subtask only makes sense once another is finished | It's half a change. Re-cut the boundary so each subtask stands as its own commit. |
| Restating the title verbatim as the parent | Reframe to the underlying goal or capability when clearer. |
| Turning every file found in Step 3 into a subtask | Context grounds the decomposition; it is not the decomposition. |
| Asking clarifying questions when intent is clear | Only ask when genuinely ambiguous; otherwise state your read and proceed. |
| Wrong parent shape for the location | Version parent = `- [ ] (scope) …` (keeps its checkbox); Backlog parent = bare `*` (no checkbox, no scope). |
| Editing `TODO.md` before approval, or moving an entry between sections | Propose first; on approval, replace in place and keep the original placement. |
| Adding narration around the block | Output the block; keep the rationale to one line. |
