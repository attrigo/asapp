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

1. **Determine commit type** based on what was changed. For test changes mixed with production code, use the production code type.

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

The /api/auth/verify endpoint has been removed. Clients should use
the /api/auth/token endpoint with token introspection instead.

BREAKING CHANGE: /api/auth/verify endpoint no longer available
```

Reasoning: Endpoint removal is a breaking change; multi-line body explains migration path.

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

## Git Safety

- NEVER execute `git commit` — only generate the message
- NEVER stage files or modify the working tree
- If changes span multiple unrelated concerns, suggest splitting into multiple commits
- Never commit secrets (.env, credentials.json, private keys) — warn if detected in diff
