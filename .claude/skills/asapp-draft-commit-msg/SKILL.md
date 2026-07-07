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

Generate a conventional commit message from a change set — this session's history by default, or an externally supplied diff, commit range (e.g. `main..<branch>`), or specific file set.

## Usage

- `/asapp-draft-commit-msg` - Generate a commit message from the current change set (session history by default, or a caller-supplied diff/commit range/file set)

## Conventional Commit Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Commit Types

Standard Conventional Commits types apply (`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `revert`).

This project's history shows these three are worth disambiguating:

| Type | Use for |
|------|---------|
| `build` | Build system or dependency changes (Maven POM, plugin versions) |
| `ci` | CI pipeline / workflow config |
| `chore` | Maintenance that is neither build nor CI (repo housekeeping, misc cleanup) |

### Breaking Changes

```
# Exclamation mark after type/scope
feat!: remove deprecated endpoint

# BREAKING CHANGE footer
feat: allow config to extend other configs

BREAKING CHANGE: `extends` key behavior changed
```

## Instructions

### Step 1: Gather Changes

Identify the source of truth for this run:
- **Session history** (the default) — Edit/Write tool calls in the conversation
- **An externally supplied diff or commit range** (e.g. `main..<branch>`) — passed in by the caller, such as `asapp-close-task`'s spec commit or squash message
- **A specific file set** — passed in by the caller

- Extract the file paths touched by the source of truth
- Auto-detect affected module(s) from file paths:
  - `services/asapp-authentication-service/...` → "authentication"
  - `services/asapp-config-service/...` → "config"
  - `services/asapp-discovery-service/...` → "discovery"
  - `services/asapp-tasks-service/...` → "tasks"
  - `services/asapp-users-service/...` → "users"
  - `libs/asapp-commons-url/...` → "api" (endpoint URL constants)
  - `libs/asapp-http-clients/...` → "clients" (matches the `.claude/rules/todo.md` scope vocabulary)
  - `docs/...` → "docs"
  - **Exception**: `docs/superpowers/specs/...` and `docs/superpowers/plans/...` (spec/plan files) scope to **the task's module**, not "docs" — e.g. `docs(tasks): mark find-tasks-by-ids design spec as implemented`
- Review the source of truth to understand the intent and purpose of changes

For session-based runs, also check current staging state:
```bash
# If files are staged, use staged diff
git diff --staged

# If nothing staged, use working tree diff
git diff

# Check status
git status --porcelain
```

For an externally supplied commit range, use it directly:
```bash
git log main..<branch>
git diff main..<branch>
```

### Step 2: Analyze Changes

1. **Determine commit type** based on what was changed. For test changes mixed with production code, use the production code type.

2. **Determine scope** (the module/feature affected). Evaluate top-down; stop at the first match:
   - **If** the change is entirely infrastructure (docker, database, liquibase) → use the component (`docker`, `database`, `liquibase`).
   - **Else if** the change spans multiple modules under one unifying technical concern → use the broader scope (`api`, `security`, `config`).
   - **Else if** the change is confined to a single module and centers on a specific, well-known feature within it → use the feature name (`jwt`, `validation`, `factories`, `tests`).
   - **Else if** the change is confined to a single module → use the module name.
   - **Else** (multiple modules, no unifying theme) → use the most general applicable scope, or the most impacted module.

3. **Understand intent** from the source of truth's context:
   - What problem was being solved?
   - What feature was being added?
   - What was refactored and why?
   - Focus on the WHY and WHAT, not the HOW

4. **Detect breaking changes**:
   - Removed public API endpoints or methods
   - Changed method signatures or return types
   - Renamed configuration properties
   - Changed the DB schema in a backward-incompatible way (e.g. dropped/renamed a column)
   - If breaking: use `!` suffix on type and/or `BREAKING CHANGE:` footer (see *Breaking Changes* above)

5. **Decide on body structure**:
   - Count distinct logical changes in the diff. If ≥2, or if the why is non-obvious from the subject alone, include a body.
   - If a body is needed, decide whether a lead paragraph adds context the subject can't carry. If not, go straight to bullets.

### Step 3: Generate Message

**Single-line format** (most cases — single conceptual change with no nuance):
```
<type>(<scope>): <description>
```

**Multi-line format** (when the subject cannot capture the change alone):
```
<type>(<scope>): <description>

[optional: 1-3 sentences explaining the WHY]

- <bullet describing one change>
- <bullet describing another change>

<optional footer(s)>
```

**Rules:**
- Subject line max 72 characters
- Use imperative mood ("add" not "added" or "adds")
- No period at end of subject line
- Focus on what and why, not how
- Include a body only when the subject cannot capture the change alone (single conceptual change with no nuance → subject-only)
- For breaking changes, see *Breaking Changes* above
- Reference issues when applicable: `Closes #123`, `Refs #456`

**Body format** (when a body is included):
- Optional lead paragraph (1-3 sentences) explaining the WHY — motivation, root cause, or constraint. Write it on a single line; do not hard-wrap at any column. Omit when the subject already conveys the why and the bullets stand on their own.
- Required bulleted list of changes (one or more bullets):
  - Marker: `-` followed by a single space
  - First letter capitalized
  - Imperative mood (`Add`, `Rename`, `Fix` — same mood as the subject line)
  - No trailing period
  - One change per bullet, single line per bullet (no nested bullets, no multi-line bullets)
- Footers (`BREAKING CHANGE:`, `Closes #N`, `Refs #N`) follow the bulleted list, separated by a blank line.

### Step 4: Output

Display the generated commit message in a code block. Briefly explain the chosen type and scope in one sentence.

## Examples

**Example 1: Single-line format (test refactoring)**

Detected changes:
- Modified 3 test factory files in `asapp-authentication-service`
- Renamed wither methods for consistency

Generated commit message:
```
test(authentication): improve factory method naming consistency
```

Reasoning: Changes are test-only (`*Tests.java` files), focused on authentication service, improving code quality without changing behavior.

---

**Example 2: Multi-line format (breaking change)**

Detected changes:
- Removed deprecated `/api/auth/verify` endpoint
- Updated REST API interface and controller
- Removed related tests

Generated commit message:
```
feat(authentication)!: remove deprecated verify endpoint

- Remove the /api/auth/verify endpoint from the REST API
- Remove the corresponding controller method and tests

BREAKING CHANGE: /api/auth/verify endpoint no longer available; clients should use /api/auth/token with token introspection
```

Reasoning: Endpoint removal is a breaking change; bulleted body lists the discrete actions, and the `BREAKING CHANGE:` footer documents the impact and migration path.

See [examples.md](examples.md) for more (cross-module refactoring, bullets-only body, lead paragraph + bullets).

## Git Safety

- NEVER execute `git commit` — only generate the message
- NEVER stage files or modify the working tree
- If changes span multiple unrelated concerns, suggest splitting into multiple commits
- Never commit secrets (.env, credentials.json, private keys) — warn if detected in diff

## Important Notes

- If unsure between types, prefer the one that best describes the primary intent
