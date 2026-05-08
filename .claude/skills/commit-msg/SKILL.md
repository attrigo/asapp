---
name: commit-msg
description: >
  Generates a conventional commit message from session changes.
  Use when the user wants to generate a commit message, write a commit, draft a git commit,
  summarize changes for a commit, or asks what their commit message should say.
  Triggers: /commit-msg, commit message, conventional commit, git message, summarize changes.
  Do NOT use for actually committing, staging, or pushing code — this skill only generates message text.
---

# Commit Message Generator

Generate a conventional commit message based on changes made during this Claude Code session.

## Usage

- `/commit-msg` - Generate commit message from session changes

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

1. **Determine commit type** based on what was changed.

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
- For breaking changes, add `!` after scope: `feat(authentication)!: remove endpoint`
- Reference issues when applicable: `Closes #123`, `Refs #456`

**Body format** (when a body is included):
- Optional lead paragraph (1-3 sentences) explaining the WHY — motivation, root cause, or constraint. Omit when the subject already conveys the why and the bullets stand on their own.
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

**Example 2: Breaking change**

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

---

**Example 3: Cross-module (refactoring)**

Detected changes:
- Modified error handling in all three services
- Updated GlobalExceptionHandler in each

Generated commit message:
```
refactor(api): standardize error handling across services
```

Reasoning: Structural improvement affecting multiple services, API layer scope used for cross-cutting concern.

---

**Example 4: Bullets-only body (multiple sub-actions, why is obvious from subject)**

Detected changes:
- Renamed credential env vars across all services
- Simplified credential values

Generated commit message:
```
refactor(docker): clean up service credential env vars and values

- Rename MANAGEMENT_USERNAME/PASSWORD → SERVICE_USERNAME/PASSWORD to better reflect credential scope
- Rename CONFIG_URI/USERNAME/PASSWORD → CONFIG_SERVER_URI/USERNAME/PASSWORD for consistency
- Simplify all credential values to user/secret for easier local testing
```

Reasoning: Multiple distinct sub-actions warrant a body; the subject alone conveys the why ("clean up"), so no lead paragraph is needed.

---

**Example 5: Lead paragraph + bullets (non-obvious why)**

Detected changes:
- Removed AuthenticationManager bean from tasks and users services
- Kept the bean in authentication service

Generated commit message:
```
fix(security): drop AuthenticationManager bean from tasks and users

Exposing @Bean AuthenticationManager in a proxyBeanMethods=false class
caused a StackOverflowError in the actuator filter chain.

- Drop the AuthenticationManager bean from tasks and users services
- Keep the bean in authentication service where CredentialsAuthenticatorAdapter requires it
```

Reasoning: The why (StackOverflowError root cause) is non-obvious from the subject and warrants a lead paragraph; bullets list the surgical changes.

## Git Safety

- NEVER execute `git commit` — only generate the message
- NEVER stage files or modify the working tree
- If changes span multiple unrelated concerns, suggest splitting into multiple commits
- Never commit secrets (.env, credentials.json, private keys) — warn if detected in diff

## Important Notes

- Always run git commands to see actual state, don't rely solely on conversation
- If unsure between types, prefer the one that best describes the primary intent
- For test changes mixed with production code, use the production code type
