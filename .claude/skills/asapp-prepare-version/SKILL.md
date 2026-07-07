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

**Core principle:** you refine the backlog; you never plan or build it — see Hard rules for the full boundary.

> **This is not `asapp-refine-task`.** That skill refines **one** entry on demand; this one sweeps the **whole next-version section**. It applies the TODO conventions (`.claude/rules/todo.md`) — read them and apply, don't restate.

## Usage

- `/asapp-prepare-version` — auto-resolve the next version and prepare its tasks.
- `/asapp-prepare-version <next-ver>` — override the prepare target (e.g. `0.6.0`).

## Process

### Step 0: Set up progress tracking

**Before any other step**, create these four tracking tasks with the task tool; mark each `in_progress` on start and `completed` when done:

1. Resolve the next version (Step 1)
2. Fan out one subagent per main task (max 5 running at once) and aggregate (Step 2)
3. Present the consolidated delivery; apply on approval (Step 3)
4. Wrap up (Step 4)

Keep Task 3 `in_progress` across the wait for approval; complete it only once the delivery lands or the user stops.

### Step 1: Resolve the next version

Resolve from an authoritative source, not by guessing:

- **Next version (prepare target)** = root `pom.xml` `<version>` minus `-SNAPSHOT` (e.g. `0.5.0-SNAPSHOT` → `0.5.0`), or the `<next-ver>` argument.

State the plan in one line before mutating anything — e.g. `Preparing ## 0.5.0`. The next section is normally on top.

If `TODO.md` has no `## X.Y.Z` section for the resolved version, stop — there is nothing to prepare. Ask the user how to proceed rather than guessing a different version or inventing a section.

### Step 2: Fan out — one subagent per main task

Enumerate every **top-level task** in the next-version section. Each top-level task **with its subtasks and notes** is one work unit (a bare one-liner is a unit too).

Dispatch **one subagent per work unit**, keeping **no more than 5 running at once**, until every unit has run. `documentation-engineer` (TODO / backlog prose is its remit) is the default agent for this skill.

Give each subagent its task + subtasks/notes verbatim, its bucket, the TODO conventions (`.claude/rules/todo.md`), and repo access (plus the full `TODO.md`, so it can spot a dependency a later version introduces). Tell it to:

- **Ground its task** — skim the related code / docs / config; check whether the task names something **not yet in the stack** or no longer matches reality.
- **Read `.claude/rules/todo.md`** and apply its Wording, Scope, and Decomposition conventions.
- **Judge the entry — don't decompose by reflex:**
  - vague / oversized / terse → parent + 2–6 scoped subtasks per the conventions; no per-feature test subtask;
  - already well-scoped → keep the structure; tighten wording, fix a wrong `(scope)` / bucket; a clean one-liner stays one line;
  - stale / duplicate / superseded / out-of-theme / premature → flag with a one-line reason, don't rewrite it away;
  - a Decisions-bucket entry → cleanup only; never decompose.
- **Apply the two guards:**
  - *a named tool is often the deliverable* — keep `Add Hikari Grafana dashboard` / `Add Redis dashboard` specific; strip a vendor name only when it is the *how*, not when it is the deliverable;
  - *stay in the section* — never reorder buckets or promote from Backlog; if the task is misplaced or premature, flag and propose a move, don't move it.
- **Return**: the proposed refined block for its task (or "keep as-is"), a one-line rationale, and any flags — nothing else.

### Step 3: Aggregate and deliver once

- Collect every subagent's result and assemble the full refined section, **in original bucket + task order** (Features → Bugfix → Technical → Docs & Tooling → Decisions).
- Normalize across units: consistent `(scope)` usage, no duplication introduced between tasks; tidy the `Goal:` line if it reads poorly.
- Present **one** delivery: the entire refined section as a single fenced block, plus a consolidated rationale (one line per task that changed) and a **Flags** list (premature / misplaced / stale items needing a decision).
- Wait for confirmation. On approval, apply it to `TODO.md` in one pass. Never edit ahead of confirmation. If the user asks for changes, revise and re-present.

### Step 4: Wrap-up

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

Rationale: stale / duplicate / superseded / out-of-theme / premature entries get a one-line reason, never a silent rewrite (Step 2 — Judge the entry).

## Hard rules

- **Touch only `TODO.md`** — no code, no other file.
- **No *how*, no execution** — subtasks are scoped outcomes, never coding steps; you rewrite the backlog, never implement or design it (that's brainstorming / writing-plans).
- **No release mechanics** — no bump, tag, archival, or push. That is `asapp-release`.
- **No code review** — never judge shipped code; that is `asapp-review-version`.
- **Delegate the analysis** — one subagent per main task, max 5 running at once; the main context only aggregates and delivers.
- **One consolidated delivery** — propose the whole refined section once; confirm, then apply. Never edit ahead, never deliver in pieces.
- **Reference, don't duplicate** — subagents defer to `.claude/rules/todo.md` for all conventions.
- **Stay in the section** — never reorder buckets or auto-promote from Backlog; move a flagged entry only on confirmation.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Starting Step 1 before creating the four tracking tasks | Do Step 0 first — create the tasks, then begin. |
| Analyzing the tasks inline in the main context | Fan out — one subagent per main task (max 5 running at once); the main context only aggregates. |
| Delivering per task or per bucket, in pieces | Aggregate every subagent result into ONE delivery of the whole refined section. |
| Decomposing every task into subtasks | Per-task judgment — a clean, well-scoped one-liner stays one line. |
| Subtasks name libraries, config keys, annotations, or file paths | That's *how* (see Hard rules); restate as a scoped outcome per the rule's Wording. |
| Generalizing a named artifact away (`Hikari dashboard` → "connection-pool dashboard") | When the tool *is* the deliverable, keep the name; strip vendor names only when they are the *how*. |
| A subagent guessing instead of grounding its task | Each subagent skims the real code / config first, to catch premature or stale entries. |
| Silently moving a misplaced / premature entry | Flag it with a reason; relocate only on confirmation. Default is refine in place. |
| Editing `TODO.md` before the user confirms the delivery | Propose the whole refined section first; apply only on approval. |
| Doing the work / planning the how / running release steps | This skill only prepares the backlog; route those elsewhere. |
| Reordering buckets or promoting Backlog items | Refine in place; keep bucket order; never auto-promote. |
