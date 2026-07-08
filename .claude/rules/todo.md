---
paths:
  - "TODO.md"
---

## Structure

- Each version is a section `## <ver> · <theme>` followed by a one-line `Goal:`. Separate top-level sections with `---`.
- Work sits in a fixed, ordered set of buckets; show a bucket only when it has items:
  1. **Features** — user- or API-visible behavior
  2. **Bugfix** — defect fixes
  3. **Technical** — internal change, no user-visible behavior (refactors, perf, security, tests, deps, migrations)
  4. **Docs & Tooling** — documentation and dev / build / CI / AI tooling, not the app itself
  5. **Decisions** — resolved research / rejected ideas (no checkboxes)
- `## Backlog` holds unscheduled candidates and keeps its own shape: `### <bucket>` → `#### <scope>` headings → plain `*` bullets (no checkboxes, no inline tag). Promote an item into a version when it's picked up.

## Tasks

- One flat checklist per bucket — no sub-headings.
- `- [ ]` open · `- [X]` done. Nest `- [ ]` subtasks only when they're real steps of the parent.
- Every top-level task opens with a `(scope)` tag; subtasks inherit it.
- Guidance is nested and untracked: `- **Note:** …`, `- **Warning:** …`.
- **Decisions** use `- **<outcome>:** …` (Rejected, Dropped, …) — no checkbox.

## Wording

Every task and subtask line:

- Leads with an imperative verb (Add, Publish, Support, Refactor…).
- Is concise — a short phrase, ~10 words; sentence case, no trailing period.
- Uses plain language, no implementation terms (libraries, config keys, annotations, file paths); a named pattern is fine when it *is* the unit of work.
- Is one coherent unit — no colons introducing embedded lists.

## Decomposition

When refining a vague or oversized entry into subtasks:

- Decompose only when needed — a clean, well-scoped one-liner stays one line.
- Reframe the parent to the underlying goal when that's clearer than a literal restatement.
- Aim for 2–6 subtasks, each a self-contained, commit-sized outcome — not a partial step meaningless until a sibling lands.
- No per-feature test subtask — the SDD flow builds tests inside each; add one only when a new test tier or test infrastructure is the deliverable.

## Scopes

The `(scope)` ≈ the task's Conventional Commit scope. Two axes: **bucket = kind of change, scope = area.**

- **domain** — the service / bounded-context the change belongs to (open set; a new service is automatically valid): e.g. `auth` `notifications` `tasks` `users`
- **concerns** — `api` `architecture` `clients` `config` `error-handling` `observability` `persistence` `security` `tests`
- **tooling** — `ai` `build` `ci` `deps` `docs`

Features are scoped by **domain**; Technical / Docs & Tooling by **concern or tooling**.

### Adding a scope

- Fit the task to an existing area first — new tech is usually an instance of one.
- A new service needs no change — domain is open.
- Add a new concern / tooling scope only when a new area *recurs*: a generic noun at the same altitude, never tool-specific (`openapi`, `git-hooks`).
- One-offs take the nearest existing scope.

## Review findings

- **Manual notes** — issues the developer logs by hand as **plain nested bullets** (`- <issue>`) one level under the task/subtask they concern.
- **Deferred review findings** — `asapp-review-task` routes findings it *defers* as ordinary top-level entries (per Structure/Wording above).
