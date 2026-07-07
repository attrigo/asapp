---
name: asapp-prepare-version
description: >
  Post-release TODO.md housekeeping: drops the just-released version's section, then prepares every task
  of the next version — decomposing the vague ones into subtasks and polishing the rest.
  Use when a release just shipped and you want to clear the released version out of TODO.md and get the
  next version's tasks clear and ready to start — step 4 of the dev flow, right after asapp-release.
  Triggers: /asapp-prepare-version, prepare the next version, prep the TODO for the next version, drop the
  released version and refine what's next, clean up TODO after the release, get the next version's tasks ready.
  Do NOT use to refine a single entry (use asapp-refine-task), to review the shipped code (use asapp-review-version),
  to run the release itself (use asapp-release), or to plan / implement a task (use brainstorming / writing-plans).
---

# Prepare Version

The post-release TODO housekeeping step (step 4 of the dev flow). Two phases:

- **(A) Drop** the just-released version's section from `TODO.md`.
- **(B) Prepare** every task of the next version — decompose the vague, polish the rest, flag the stragglers. Fanned out to subagents, one per task.

Runs *after* `asapp-release`, *before* work on the next version starts.

**Core principle:** you refine the backlog; you never plan or build it.

- Tasks are scoped *what/why* outcomes a developer could pick up and brainstorm.
- The *how* (libraries, config keys, annotations, file paths, edits) stays out.
- A prepared entry is a **backlog item, not an implementation plan.**
- Touch **only `TODO.md`**.

> **This is not `asapp-refine-task`.** That skill refines **one** entry on demand; this one sweeps the **whole next-version section** and adds the post-release drop. It applies the TODO conventions (`.claude/rules/todo.md`) — read them and apply, don't restate.

## Usage

- `/asapp-prepare-version` — auto-resolve both versions, run Phase A then B.
- `/asapp-prepare-version <next-ver>` — override the prepare target (e.g. `0.6.0`); Phase A still drops the released section if present.

## This skill is NOT

- **Implementation planning or design** — no *how*, no architecture. That is brainstorming / writing-plans.
- **Task execution** — you rewrite the backlog; you do not implement it.
- **Release mechanics** — no bump, tag, spec archival, or push. That is `asapp-release`.
- **Code review** — never judge shipped code. That is `asapp-review-version`.

## Process

### Step 0: Set up progress tracking

**Before any other step**, create these five tracking tasks with the task tool; mark each `in_progress` on start and `completed` when done:

1. Resolve versions (Step 1)
2. Drop the released section (Step 2)
3. Fan out one subagent per main task (max 3 parallel) and aggregate (Step 3)
4. Present the consolidated delivery; apply on approval (Step 4)
5. Wrap up (Step 5)

Keep Task 4 `in_progress` across the wait for approval; complete it only once the delivery lands or the user stops.

### Step 1: Resolve the two versions (Phase A)

Resolve from authoritative sources, not by guessing:

- **Released version (drop target)** = the latest git tag: `git describe --tags --abbrev=0` (e.g. `v0.4.0`).
- **Next version (prepare target)** = root `pom.xml` `<version>` minus `-SNAPSHOT` (e.g. `0.5.0-SNAPSHOT` → `0.5.0`), or the `<next-ver>` argument.

State the plan in one line before mutating anything — e.g. `Dropping ## 0.4.0, preparing ## 0.5.0`. Sections run in ascending order, so the released section is normally on top with the next section right below.

### Step 2: Drop the released section (Phase A)

- **If a `## <released-ver> · …` section exists:** delete it **wholesale** — from its `## ` header to the next `## ` header, including the trailing `---` divider. No completeness or preservation audit: `asapp-release` already gated it complete, history lives in git + the GitHub Release, and the edit is recoverable with `git checkout TODO.md`.
- **If no section matches the tag** (already dropped, or the release hasn't run yet): **skip Phase A** and go to Step 3. This targeting guard is the only "check" — never a content audit.
- **The drop target is always the latest tag** — released by definition, never the `-SNAPSHOT`. If someone says "I released X" but there is no `vX` tag, X is not released: skip the drop and recommend `asapp-release` first. Never trust the narrative over `git` / `pom` — it would delete unreleased work.

### Step 3: Fan out — one subagent per main task (Phase B)

Enumerate every **top-level task** in the next-version section. Each top-level task **with its subtasks and notes** is one work unit (a bare one-liner is a unit too).

Dispatch **one subagent per work unit**, at most **3 running concurrently** — run in waves if there are more. Prefer the most specific `.claude/agents/` match; `documentation-engineer` (TODO / backlog prose is its remit) is the default, `general-purpose` a last resort.

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

### Step 4: Aggregate and deliver once (Phase B)

- Collect every subagent's result and assemble the full refined section, **in original bucket + task order** (Features → Bugfix → Technical → Docs & Tooling → Decisions).
- Normalize across units: consistent `(scope)` usage, no duplication introduced between tasks; tidy the `Goal:` line if it reads poorly.
- Present **one** delivery: the entire refined section as a single fenced block, plus a consolidated rationale (one line per task that changed) and a **Flags** list (premature / misplaced / stale items needing a decision).
- Wait for confirmation. On approval, apply it to `TODO.md` in one pass. Never edit ahead of confirmation. If the user asks for changes, revise and re-present.

### Step 5: Wrap-up

- One line: which section was dropped (or that Phase A was skipped), and counts — tasks decomposed / polished / flagged.
- Remind the user: only `TODO.md` changed — no code, no release step. Flagged items are theirs to route.

## Wording conventions

Defer to `.claude/rules/todo.md` for wording, scope, and decomposition; each subagent reads it and applies it to any entry it decomposes. The one version-level nuance on top: the *named-tool-is-the-deliverable* guard (Step 3).

## Relationship to sibling skills

| Skill | Scope | Drops released version | Delivery | When |
|-------|-------|------------------------|----------|------|
| `asapp-refine-task` | one TODO entry | no | one block | ad hoc — an entry is too big / vague |
| **`asapp-prepare-version`** | the whole next-version section | **yes (Phase A)** | **one delivery (parallel fan-out)** | **step 4 — after release, before work starts** |
| `asapp-review-version` | the released version's diff | no (reviews, logs nothing) | readiness report | step 2 — before release |
| `asapp-release` | the whole version | no (archives, tags, pushes) | the release itself | step 3 |

Everything about *how* to refine an entry lives in `.claude/rules/todo.md`; this skill owns the version-level fan-out and the post-release drop.

## Hard rules

- **Touch only `TODO.md`** — no code, no other file.
- **No *how*** — subtasks are scoped outcomes, never coding steps.
- **No release mechanics** — no bump, tag, archival, or push.
- **No code review** — never judge shipped code; that is `asapp-review-version`.
- **Drop wholesale** — the released section (and its trailing `---`) goes without a preservation audit; skip only when nothing matches the released tag.
- **Delegate the analysis** — one subagent per main task, max 3 parallel; the main context only aggregates and delivers.
- **One consolidated delivery** — propose the whole refined section once; confirm, then apply. Never edit ahead, never deliver in pieces.
- **Reference, don't duplicate** — subagents defer to `.claude/rules/todo.md` for all conventions.
- **Stay in the section** — never reorder buckets or auto-promote from Backlog; move a flagged entry only on confirmation.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Starting Step 1 before creating the five tracking tasks | Do Step 0 first — create the tasks, then begin. |
| Analyzing the tasks inline in the main context | Fan out — one subagent per main task (max 3 parallel); the main context only aggregates. |
| Delivering per task or per bucket, in pieces | Aggregate every subagent result into ONE delivery of the whole refined section. |
| Decomposing every task into subtasks | Per-task judgment — a clean, well-scoped one-liner stays one line. |
| Subtasks name libraries, config keys, annotations, or file paths | That is *how*. Restate as a scoped outcome — per the rule's Wording. |
| Generalizing a named artifact away (`Hikari dashboard` → "connection-pool dashboard") | When the tool *is* the deliverable, keep the name; strip vendor names only when they are the *how*. |
| A subagent guessing instead of grounding its task | Each subagent skims the real code / config first, to catch premature or stale entries. |
| Silently moving a misplaced / premature entry | Flag it with a reason; relocate only on confirmation. Default is refine in place. |
| Leaving an orphan `---` after dropping the section | Delete the section *and* its trailing divider, header to next `## `. |
| Auditing the released section for completeness before dropping | Drop wholesale — `asapp-release` already gated it; history is in git + the Release. |
| Dropping a section because someone said "I released X", with no `vX` tag | Not tagged = not released. Drop only the latest-tag section; if it is gone or still the SNAPSHOT, skip and recommend `asapp-release` first. |
| Editing `TODO.md` before the user confirms the delivery | Propose the whole refined section first; apply only on approval. |
| Doing the work / planning the how / running release steps | This skill only prepares the backlog; route those elsewhere. |
| Reordering buckets or promoting Backlog items | Refine in place; keep bucket order; never auto-promote. |
