---
name: refine-task
description: >
  Rewrites a vague, high-level backlog entry into a clearer parent task plus smaller, well-scoped subtasks for TODO.md.
  Use when the user wants to refine, decompose, break down, split, scope, or rewrite a TODO.md task, idea, or concept
  into smaller actionable subtasks, or says a backlog entry is too big, vague, or unscoped to hand to brainstorming.
  Triggers: /refine-task, refine task, refine TODO, decompose task, break down task, split this task, scope this entry.
  Do NOT use for implementation planning or design (use brainstorming/writing-plans), for executing the task itself,
  or for editing task status or checkboxes in TODO.md.
---

# Refine Task

Turn one vague, high-level `TODO.md` entry into a clearer **parent task** plus a few **smaller, well-scoped subtasks**, so the downstream SDD flow (brainstorming → writing-plans) receives tight, focused input.

**Core principle:** subtasks describe the *what* and *why* at a scoped level — outcomes a developer could pick up and brainstorm. The *how* (libraries, config keys, annotations, file paths, step-by-step changes) stays out; it belongs in brainstorming. A refined entry is a **backlog item, not an implementation plan**.

## Usage

- `/refine-task <line-number>` — refine the entry at that line of `TODO.md`
- `/refine-task <quoted or named task>` — locate the matching entry and refine it
- `/refine-task` (no argument) — ask which entry to refine

## This skill is NOT

- **Not an implementation plan** — subtasks are scoped deliverables, never coding steps. If a subtask names a dependency, config key, or file, you have dropped a level — pull back up.
- **Not design or brainstorming** — no architecture, no trade-offs, no *how*.
- **Not task execution** — you rewrite the backlog entry; you do not implement it.

## Process

### Step 1: Locate and read the task

- Resolve the input to a single `TODO.md` entry: a line number, or quoted/named text matched against the file.
- If the match is ambiguous (several plausible entries), show the candidates and confirm before continuing.
- Record where it lives: version section, category, and indent level. The refined block goes back in the **same place** — never move an entry between sections, and never refine one already complete (`[X]`) or cancelled (`~~struck~~`).

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

- Break the parent into smaller, scoped, domain-focused subtasks. Each is one coherent unit of work that could land as a single commit without leaving the system half-built — a self-contained increment, not a partial step that is meaningless until a sibling is done. Subtasks may run in sequence; each must still stand on its own.
- **Granularity test:** aim for roughly 2–6 subtasks. If you are producing 10+ items that mention config keys, file names, or sequential edits, you have slipped into implementation planning — collapse them back up to backlog-sized outcomes.
- **No per-feature test subtask** — the SDD flow builds each subtask test-first, so its tests are implicit. Add a testing subtask only when testing itself is the deliverable: a new test tier (load, end-to-end, contract), test infrastructure, or an architecture suite — named as that capability, never a generic "add tests".
- Apply the Wording conventions below to every subtask.

### Step 6: Propose extra improvements

- Suggest genuinely *additive* subtasks when they clearly strengthen the work (resilience, validation, docs, observability) — beyond the literal 1:1 translation. Do not propose a test subtask here; follow the test rule in Step 5.
- Research the web **only** when the topic is fast-moving or you are unsure of current best practice; otherwise propose from existing knowledge.
- Keep extras optional and few — do not pad. Flag which subtasks are proposals in the chat rationale (Step 7), keeping the markdown itself clean.

## Wording conventions

Apply to the parent task **and** every subtask:

- **Lead with an imperative verb** — Add, Publish, Send, Support, Migrate, Refactor…
- **Concise** — a short phrase, ideally around 10 words.
- **Plain language, no implementation terms** — avoid *how*: library names, config keys, annotations, file paths, vendor specifics (e.g. "in-process", "AFTER_COMMIT", "outbound port", `spring.flyway.*`). Named capabilities or well-known patterns are fine when they *are* the unit of work (e.g. "Use circuit breaker pattern", "Use retry pattern").
- **Sentence case, no trailing period.**
- **One coherent unit per line** — join two actions with "and" only when they are genuinely one piece of work. No colons introducing embedded lists.

## Output format

Match the `TODO.md` structure exactly:

```markdown
* <Parent task>
    * [ ] <Subtask>
    * [ ] <Subtask>
    * [ ] <Subtask>
```

- Preserve the entry's version/category placement and indentation.
- The parent becomes a grouping header with **no checkbox** (like `* Improve HTTP clients`); a leaf entry's `[ ]` is dropped because the entry now owns subtasks. Each subtask carries a `[ ]` marker.
- Replace the targeted entry (and any existing children of it) with the refined block. Never touch unrelated entries.

## Delivery (propose, then edit on approval)

1. Show the refined block in a fenced code block, plus a **one-line** rationale that names which subtasks are proposed extras.
2. Wait for the user's approval.
3. On approval, edit `TODO.md` in place: replace the original entry with the refined block, preserving placement and indentation, leaving the rest of the file untouched.
4. If the user declines or requests changes, adjust and re-show — do not edit until approved.

## Examples

**Example 1 — reframe to the goal (the canonical case):**

Before:
```markdown
* Replace REST clients by declarative HTTP clients
```
After:
```markdown
* Improve HTTP clients
    * [ ] Refactor REST clients by declarative HTTP clients
    * [ ] Use circuit breaker pattern
    * [ ] Use retry pattern
```
Rationale: the parent rises from a literal swap to the capability ("Improve HTTP clients"); resilience patterns are proposed extras beyond the original ask.

**Example 2 — compress and keep the *how* out:**

Before:
```markdown
* Replace Liquibase by Flyway
```
After:
```markdown
* Migrate database migrations to Flyway
    * [ ] Migrate database migration tooling to Flyway
    * [ ] Convert existing changesets to Flyway migrations
    * [ ] Preserve compatibility with already-migrated databases
    * [ ] Update migration conventions and docs
    * [ ] Validate migrations on fresh and existing databases
```
Rationale: the source leaf `* [ ] Replace Liquibase by Flyway` becomes a bare grouping parent with five scoped subtasks instead of an eleven-step implementation checklist; the last subtask is a proposed safety extra.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Subtasks name libraries, config keys, annotations, or file paths | That is *how*. Restate as a scoped outcome in plain language. |
| Subtasks are full sentences with colons and embedded lists | Compress to ~10-word imperative phrases; one unit per line. |
| 10+ micro-steps mirroring an implementation plan | You dropped a level. Pull back up to backlog-sized outcomes (aim 2–6). |
| Adding a per-feature "add tests for X" subtask | TDD delivers tests inside each subtask. Add a test subtask only when a new test tier or test infrastructure is the deliverable. |
| A subtask only makes sense once another is finished | It's half a change. Re-cut the boundary so each subtask stands as its own commit. |
| Restating the title verbatim as the parent | Reframe to the underlying goal or capability when clearer. |
| Turning every file found in Step 3 into a subtask | Context grounds the decomposition; it is not the decomposition. |
| Asking clarifying questions when intent is clear | Only ask when genuinely ambiguous; otherwise state your read and proceed. |
| Keeping a `[ ]` checkbox on a parent that now owns subtasks | Parents are bare grouping headers; only subtasks carry `[ ]`. |
| Editing `TODO.md` before approval, or moving an entry between sections | Propose first; on approval, replace in place and keep the original placement. |
| Adding narration around the block | Output the block; keep the rationale to one line. |
