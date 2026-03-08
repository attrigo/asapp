---
name: commit-msg
description: Generate conventional commit messages from session changes or module-specific git changes. Use when you need to create a commit message that follows project conventions.
argument-hint: "[module]"
---

# Commit Message Generator

Generate a conventional commit message based on changes made during this Claude Code session or from a specific module. **Does NOT commit** — only outputs the message.

## Usage

- `/commit-msg` - Generate commit message from session changes
- `/commit-msg <module>` - Generate commit message for specific module (e.g., "authentication", "users", "tasks")

## Conventional Commit Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Commit Types

| Type | Purpose |
|------|---------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Formatting/style (no logic) |
| `refactor` | Code refactor (no feature/fix) |
| `perf` | Performance improvement |
| `test` | Add/update tests |
| `build` | Build system/dependencies |
| `ci` | CI/config changes |
| `chore` | Maintenance/misc |
| `revert` | Revert commit |

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

**If module argument provided (`$ARGUMENTS`):**
- Map short name to full service path:
  - "authentication" or "auth" → `services/asapp-authentication-service`
  - "users" or "user" → `services/asapp-users-service`
  - "tasks" or "task" → `services/asapp-tasks-service`
  - "commons" → `libs/` (for shared libraries)
- Run `git status` to see modified files in that module
- Run `git diff` scoped to that module directory to see actual changes

**If no argument (session-based):**
- Analyze the conversation history for Edit/Write tool calls
- Extract file paths that were modified during this session
- Auto-detect affected module(s) from file paths:
  - `services/asapp-authentication-service/...` → "authentication"
  - `services/asapp-users-service/...` → "users"
  - `services/asapp-tasks-service/...` → "tasks"
  - `libs/...` → "commons"
  - `docs/...` → "docs"
- Review the conversation to understand the intent and purpose of changes

Also check current staging state:
```bash
# If files are staged, use staged diff
git diff --staged

# If nothing staged, use working tree diff
git diff

# Check status
git status --porcelain
```

### Step 2: Analyze Changes

1. **Determine commit type** from the Commit Types table above based on what was changed

2. **Determine scope** (the module/feature affected):
   - If single module: use module name (authentication, users, tasks)
   - If specific feature: use feature name (jwt, validation, factories, tests)
   - If cross-cutting: use broader scope (api, security, config)
   - If multiple modules: use most general scope or most impacted module
   - If infrastructure: use component (docker, database, liquibase)

3. **Understand intent** from conversation context:
   - What problem was being solved?
   - What feature was being added?
   - What was refactored and why?
   - Focus on the WHY and WHAT, not the HOW

4. **Detect breaking changes**:
   - Removed public API endpoints or methods
   - Changed method signatures or return types
   - Renamed configuration properties
   - Changed database schema without migration
   - If breaking: use `!` suffix on type and/or `BREAKING CHANGE:` footer

### Step 3: Generate Message

**Single-line format** (most cases):
```
<type>(<scope>): <description>
```

**Multi-line format** (complex changes or breaking changes):
```
<type>(<scope>): <description>

<body: explain what and why, not how>

<footer(s)>
```

**Rules:**
- Subject line max 72 characters
- Use imperative mood ("add" not "added" or "adds")
- No period at end of subject line
- Focus on what and why, not how
- Use multi-line body only when changes are complex or breaking
- Reference issues when applicable: `Closes #123`, `Refs #456`

### Step 4: Output

Display the generated commit message in a code block, along with:

**Do NOT execute `git commit`** — only output the message for the user to review and use.

## Examples

**Example 1: Session-based (test refactoring)**

Detected changes:
- Modified 3 test factory files in `asapp-authentication-service`
- Renamed wither methods for consistency

Generated commit message:
```
test(authentication): improve factory method naming consistency
```

Reasoning: Changes are test-only (`*Tests.java` files), focused on authentication service, improving code quality without changing behavior.

---

**Example 2: Module-specific (new feature)**

Command: `/commit-msg users`

Detected changes:
- Added email validation to UserProfile domain class
- Updated tests

Generated commit message:
```
feat(users): add email validation to user profile
```

Reasoning: New functionality added to users service, enhances existing feature.

---

**Example 3: Breaking change**

Detected changes:
- Removed deprecated `/api/auth/verify` endpoint
- Updated REST API interface and controller
- Removed related tests

Generated commit message:
```
feat(authentication)!: remove deprecated verify endpoint

The /api/auth/verify endpoint has been removed. Clients should use
the /api/auth/token endpoint with token introspection instead.

BREAKING CHANGE: /api/auth/verify endpoint no longer available
```

Reasoning: Endpoint removal is a breaking change; multi-line body explains migration path.

---

**Example 4: Cross-module (refactoring)**

Detected changes:
- Modified error handling in all three services
- Updated GlobalExceptionHandler in each

Generated commit message:
```
refactor(api): standardize error handling across services
```

Reasoning: Structural improvement affecting multiple services, API layer scope used for cross-cutting concern.

## Git Safety

- NEVER execute `git commit` — only generate the message
- NEVER stage files or modify the working tree
- If changes span multiple unrelated concerns, suggest splitting into multiple commits
- Never commit secrets (.env, credentials.json, private keys) — warn if detected in diff

## Important Notes

- Always run git commands to see actual state, don't rely solely on conversation
- If unsure between types, prefer the one that best describes the primary intent
- For test changes mixed with production code, use the production code type
