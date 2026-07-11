---
name: asapp-prepare-version
description: >
  Post-release TODO.md housekeeping: prepares every task of the next version — decomposing the vague ones
  into subtasks and polishing the rest.
  Use when a release just shipped and the user wants the next version's tasks clear and ready to start,
  right after asapp-release.
  Triggers: /asapp-prepare-version, prepare the next version, prep the TODO for the next version, refine
  what's next, get the next version's tasks ready.
  Do NOT use to refine a single entry (use asapp-refine-task), to review the shipped code (use asapp-review-version),
  to run the release itself (use asapp-release), or to plan / implement a task (use brainstorming / writing-plans).
---

# Prepare Version

The post-release TODO housekeeping step: prepare every task of the next version — decompose the vague, polish the rest, flag the stragglers. Fanned out to subagents, one per task.

**Core principle:** you refine the backlog; you never plan or build it.

Unlike `asapp-refine-task`, which refines one entry on demand, this sweeps the whole next-version section.

## Usage

- `/asapp-prepare-version` — auto-resolve the next version and prepare its tasks.
- `/asapp-prepare-version <next-ver>` — override the prepare target (e.g. `0.6.0`).

## Process

### 0. Set up progress tracking

**Before any other step**, create these four tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when done:

1. Resolve the next version (Step 1)
2. Fan out one subagent per task (Step 2)
3. Aggregate, present the delivery, and apply on approval (Step 3)
4. Wrap up (Step 4)

Keep task 3 `in_progress` across the wait for approval; complete it only once the delivery lands or the user stops.

### 1. Resolve the next version

- **Prepare target** = root `pom.xml` `<version>` minus `-SNAPSHOT` (e.g. `0.5.0-SNAPSHOT` → `0.5.0`), or the `<next-ver>` argument — resolve it, don't guess.
- State the plan in one line before mutating anything (e.g. `Preparing ## 0.5.0`).
- If `TODO.md` has no `## X.Y.Z` section for that version, stop and ask the user how to proceed — don't guess a different version or invent a section.

### 2. Fan out — one subagent per task

Enumerate every **top-level task** in the next-version section; each top-level task with its subtasks and notes is one work unit (a bare one-liner is a unit too).

Dispatch **one subagent per unit**, **≤5 running at once**, until every unit has run. Default agent: `documentation-engineer` (backlog prose is its remit). Give each its task + subtasks / notes verbatim, its bucket, `.claude/rules/todo.md`, and repo access plus the full `TODO.md` (to spot a dependency a later version introduces). Tell it to:

- **Ground the task** — skim the related code / docs / config; check whether it names something **not yet in the stack** or no longer matches reality.
- **Apply `.claude/rules/todo.md`** (Wording, Scope, Decomposition) — read and apply, don't restate.
- **Judge before decomposing** — don't decompose by reflex:
  - vague / oversized / terse → parent + scoped subtasks per Decomposition;
  - already well-scoped → keep the structure, tighten wording, fix a wrong `(scope)` / bucket;
  - stale / duplicate / superseded / out-of-theme / misplaced / premature → flag with a one-line reason, don't rewrite it away;
  - a `Decisions` entry → cleanup only, never decompose.
- **Keep a named tool that is the deliverable** — `Add Hikari Grafana dashboard` / `Add Redis dashboard` stay specific; strip a vendor name only when it is the *how*.
- **Return** — the proposed refined block (or "keep as-is"), a one-line rationale, and any flags; nothing else.

### 3. Aggregate and deliver once

- Assemble every subagent's result into the full refined section, in **original bucket + task order** (Features → Bugfix → Technical → Docs & Tooling → Decisions).
- Normalize across units — consistent `(scope)` usage, no duplication introduced between tasks; tidy the `Goal:` line if it reads poorly.
- Present **one** delivery: the whole refined section as a single fenced block, a consolidated rationale (one line per changed task), and a **Flags** list (premature / misplaced / stale items needing a decision).
- Wait for confirmation, then apply to `TODO.md` in one pass. If the user asks for changes, revise and re-present.

### 4. Wrap-up

- One line: which version was prepared, and counts — tasks decomposed / polished / flagged.
- Remind the user: only `TODO.md` changed — no code, no release step. Flagged items are theirs to route.

## Examples

**Example 1 — decompose a vague entry:**

Before (in the next-version section):
```markdown
- [ ] (tasks) Improve task search
```
After:
```markdown
- [ ] (tasks) Improve task search
    - [ ] Support filtering tasks by status and assignee
    - [ ] Support full-text search on task title and description
    - [ ] Add pagination to search results
```
Rationale: the one-liner names no scoped outcome; the subagent decomposes it into self-contained subtasks per the rule's Decomposition conventions.

**Example 2 — flag a superseded entry (no rewrite):**

Entry (in the next-version section):
```markdown
- [ ] (auth) Add OAuth2 login via Keycloak
```
Flag (surfaced in the consolidated Flags list; the entry itself is left unchanged): superseded — the stack already ships JWT-based auth (`asapp-authentication-service`); confirm whether OAuth2 is still wanted before refining.

Rationale: stale / duplicate / superseded / out-of-theme / premature entries get a one-line reason, never a silent rewrite (Step 2 — Judge before decomposing).

## Guardrails

- **Touch only `TODO.md`** — no code, no other file.
- **No *how*, no execution** — subtasks stay scoped outcomes, never coding steps; implementing or designing is brainstorming / writing-plans.
- **No release mechanics** — no bump, tag, archival, or push; that is `asapp-release`.
- **No code review** — never judge shipped code; that is `asapp-review-version`.
- **Delegate the analysis** — the main context only aggregates and delivers.
- **One consolidated delivery** — propose the whole refined section once; confirm, then apply. Never edit ahead, never deliver in pieces.
- **Stay in the section** — never reorder buckets or auto-promote from Backlog; move a flagged entry only on confirmation.
