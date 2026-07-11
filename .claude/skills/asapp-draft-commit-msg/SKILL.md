---
name: asapp-draft-commit-msg
description: >
  Generates a conventional commit message from a change set — session history, a diff, a commit
  range, or a specific file set.
  Use when the user wants to generate a commit message, write a commit, draft a git commit,
  summarize changes for a commit, or asks what their commit message should say.
  Triggers: /asapp-draft-commit-msg, commit message, conventional commit, git message, summarize changes.
  Do NOT use for actually committing, staging, or pushing code — this skill only generates message text.
---

# Commit Message Generator

Draft a Conventional Commit message from a change set — this session's Edit/Write history by default, or a caller-supplied diff, commit range (`main..<branch>`), or file set. **Core principle:** describe the WHY and WHAT of the change, never the HOW.

## Usage

- `/asapp-draft-commit-msg` — draft a message from the current change set: session Edit/Write history by default, or a caller-supplied diff, commit range, or file set.

## Process

### 1. Gather the change set

- Pick the source of truth: session Edit/Write history (default) · a caller-supplied diff or commit range (`main..<branch>`, e.g. `asapp-close-task`'s spec commit or squash message) · a caller-supplied file set.
- Get the actual changes:
  - Session — if files are staged, `git diff --staged`; else `git diff`. Check `git status --porcelain`.
  - Commit range — `git log main..<branch>` and `git diff main..<branch>`.
- List the touched paths and map each to a scope via **Module → scope** (Reference).
- If the set spans unrelated concerns, say so and suggest splitting into separate commits.

### 2. Choose type and scope

- **Type** — from what changed (**Types**, Reference). Test changes mixed with production code take the production type. Unsure between types → the one describing the primary intent.
- **Scope** — top-down, first match wins:
  1. Entirely infrastructure (docker / database / liquibase) → the component (`docker`, `database`, `liquibase`).
  2. Spans multiple modules under one unifying technical concern → the broader scope (`api`, `security`, `config`).
  3. One module, centered on a specific well-known feature → the feature (`jwt`, `validation`, `factories`, `tests`).
  4. One module → the module name.
  5. Multiple modules, no unifying theme → the most general applicable scope, or the most impacted module.
- **Breaking change** — detect removed public endpoint/method, changed signature or return type, renamed config property, or a backward-incompatible schema change (dropped/renamed column). If breaking, mark it (**Format**, Reference).

### 3. Draft

- Subject-only when it's a single conceptual change with no nuance. Add a body when the diff holds ≥2 distinct logical changes, or the why isn't obvious from the subject.
- When a body is needed, lead with a 1–3 sentence paragraph only if it carries a why the subject and bullets can't; otherwise go straight to bullets.
- Follow **Format** and **Body rules** (Reference).

### 4. Output

Print the message in a code block, then one sentence naming the chosen type and scope.

## Reference

### Format

```
<type>(<scope>): <description>

[optional body — WHY paragraph and/or change bullets]

[optional footer(s)]
```

- Subject ≤ 72 characters, imperative mood ("add", not "added"/"adds"), no trailing period.
- **Breaking change** — `!` after the type/scope, and/or a `BREAKING CHANGE:` footer stating impact and migration path.
- **Footers** — `BREAKING CHANGE:`, `Closes #N`, `Refs #N` — follow the bullets, separated by a blank line.

### Body rules

- **Lead paragraph** (optional) — 1–3 sentences of why (motivation, root cause, constraint). One line, no hard-wrapping. Omit when the subject already conveys the why.
- **Bullets** (required once a body exists) — `-` + single space, first letter capitalized, imperative mood (same as the subject), no trailing period, one change per bullet on a single line (no nesting, no multi-line bullets).

### Types

Standard Conventional Commits types apply (`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `revert`). This project's history disambiguates three more:

| Type | Use for |
|------|---------|
| `build` | Build system or dependency changes (Maven POM, plugin versions) |
| `ci` | CI pipeline / workflow config |
| `chore` | Maintenance that is neither build nor CI (repo housekeeping, misc cleanup) |

### Module → scope

Map the touched paths to a scope (scope vocabulary: `.claude/rules/todo.md`):

| Path | Scope |
|------|-------|
| `services/asapp-authentication-service/...` | `authentication` |
| `services/asapp-config-service/...` | `config` |
| `services/asapp-discovery-service/...` | `discovery` |
| `services/asapp-tasks-service/...` | `tasks` |
| `services/asapp-users-service/...` | `users` |
| `libs/asapp-commons-url/...` | `api` (endpoint URL constants) |
| `libs/asapp-http-clients/...` | `clients` |
| `docs/...` | `docs` |

**Exception** — spec/plan files under `docs/superpowers/specs/...` and `docs/superpowers/plans/...` scope to the task's module, not `docs` (e.g. `docs(tasks): mark find-tasks-by-ids design spec as implemented`).

### Examples

Two representative shapes; see [examples.md](examples.md) for cross-module, bullets-only, and lead-paragraph-plus-bullets cases.

**Single-line** — test-only change in one service:
```
test(authentication): improve factory method naming consistency
```

**Multi-line, breaking** — endpoint removal:
```
feat(authentication)!: remove deprecated verify endpoint

- Remove the /api/auth/verify endpoint from the REST API
- Remove the corresponding controller method and tests

BREAKING CHANGE: /api/auth/verify endpoint no longer available; clients should use /api/auth/token with token introspection
```

## Guardrails

- **Generate text only** — never run `git commit`, never stage or modify the working tree, never push.
- **Never emit secrets** — if the diff touches `.env`, `credentials.json`, private keys, or similar, warn instead of embedding their contents.
