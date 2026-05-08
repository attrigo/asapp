# Design — Bulleted body in `commit-msg` skill

**Date:** 2026-05-07
**Status:** Approved
**Owner:** attrigo
**Source:** TODO.md v0.4.0 → CI/CD → "Update commit-msg skill to include a bulleted body in generated commit messages"
**Skill affected:** `.claude/skills/commit-msg/SKILL.md`

## 1. Problem

The current `commit-msg` skill generates a Conventional Commit message but leaves the body shape unspecified: it merely says *"Use multi-line body only when changes are complex or breaking"* and presents one prose-body example. As a result, generated messages are inconsistent — some are subject-only, some are flowing prose, some are bulleted, some mix a lead paragraph with bullets — and the skill cannot reliably reproduce the style we actually want.

We want a single, well-defined body format that the skill produces every time a body is needed: an optional lead paragraph (the WHY) followed by a required bulleted list (the WHAT, one change per bullet).

## 2. Goals

- The skill produces commit messages with a deterministic, repeatable body shape.
- Subject-only commits remain valid for trivial changes — bullets are not forced where they add no value.
- The new format reflects the style already present in the better-written existing commits (`5fc91483`, `4faefec1`, `a6d8eac3`, `bade26db`).
- The change is contained in `SKILL.md`; no code, tests, or other tooling are affected.

## 3. Non-goals

- Moving the skill's operations into a dedicated agent (separate TODO item, line 22).
- Changing how the skill gathers changes, detects scope, or detects breaking changes.
- Adding any automated validation for generated messages.
- Reformatting historical commits.

## 4. Specification

### 4.1 Subject line (unchanged)

```
<type>(<scope>): <description>
```

- Imperative mood, no trailing period, ≤72 characters.
- Type and scope rules unchanged from the current skill.

### 4.2 Body — when to include

Include a body **only when the subject cannot capture the change alone**. The subject suffices when the change is a single conceptual action with no nuance (e.g., *"docs(security): improve Javadoc on authentication entry points"*).

Add a body when **either** of the following is true:

- The diff contains 2+ distinct sub-actions worth listing separately, **or**
- The why is non-obvious and not carried by the subject.

If neither applies, the message is subject-only.

### 4.3 Body — shape

When a body is present, it has two parts:

1. **Lead paragraph** — *optional*. 1–3 sentences explaining the WHY (motivation, root cause, constraint). Omit when the subject already conveys the why and the bullets stand on their own.
2. **Bulleted list** — *required when a body is present*. One change per bullet:
   - Marker: `-` followed by a single space.
   - First letter capitalized.
   - Imperative mood (`Add`, `Rename`, `Fix` — same tense as the subject).
   - No trailing period.
   - Single line per bullet (no nested bullets, no multi-line bullets).

The lead paragraph (if present) is separated from the bullets by a blank line.

### 4.4 Footers (unchanged)

`BREAKING CHANGE:` and issue references (`Closes #N`, `Refs #N`) follow the bulleted list, separated by a blank line.

### 4.5 Worked examples

**Subject-only** (single conceptual change, no nuance):

```
docs(security): improve Javadoc on authentication entry points
```

**Bullets only** (multiple sub-actions, why is obvious from subject):

```
refactor(docker): clean up service credential env vars and values

- Rename MANAGEMENT_USERNAME/PASSWORD → SERVICE_USERNAME/PASSWORD to better reflect credential scope
- Rename CONFIG_URI/USERNAME/PASSWORD → CONFIG_SERVER_URI/USERNAME/PASSWORD for consistency
- Simplify all credential values to user/secret for easier local testing
```

**Lead + bullets** (non-obvious why):

```
fix(security): remove AuthenticationManager bean from tasks and users services

Exposing @Bean AuthenticationManager in a proxyBeanMethods=false class
caused a StackOverflowError in the actuator filter chain.

- Drop the AuthenticationManager bean from tasks and users services
- Keep the bean in authentication service where CredentialsAuthenticatorAdapter requires it
```

**Bullets + breaking-change footer**:

```
feat(authentication)!: remove deprecated verify endpoint

- Remove the /api/auth/verify endpoint from the REST API
- Remove the corresponding controller method and tests

BREAKING CHANGE: /api/auth/verify endpoint no longer available; clients should use /api/auth/token with token introspection
```

## 5. Changes to `SKILL.md`

The edits are localized to three regions of the existing file.

### 5.1 Step 2: Analyze Changes (lines 83–105)

Add a 5th item to the Step 2 checklist:

> **Decide on body structure**: count distinct logical changes in the diff. If ≥2, or if the why is non-obvious from the subject alone, include a body. Determine whether a lead paragraph adds context the subject can't carry.

### 5.2 Step 3: Generate Message (lines 108–130)

- Replace the current "Multi-line format" template with the new shape: subject → optional lead → required bullets → optional footers.
- Replace the rule *"Use multi-line body only when changes are complex or breaking"* with *"Include a body only when the subject cannot capture the change alone (single conceptual change with no nuance → subject-only)"*.
- Add a "Body format" sub-block specifying:
  - Lead-paragraph rule (1–3 sentences, optional, WHY).
  - Bullet rules (dash, capitalized first letter, imperative, no trailing period, one change per bullet, no nesting).

### 5.3 Examples section (lines 136–184)

- **Example 1** (test factory rename) — keep as-is; valid subject-only case.
- **Example 2** (breaking change) — keep, rewrite the body to use bullets per §4.3, preserve the `BREAKING CHANGE:` footer.
- **Example 3** (cross-module refactor) — keep as-is; valid subject-only case.
- **Add Example 4**: bullets-only body (e.g., the `refactor(docker)` style from §4.5).
- **Add Example 5**: lead + bullets (e.g., the `fix(security): remove AuthenticationManager` style from §4.5).

### 5.4 Out of scope

Step 1 (Gather Changes), Step 4 (Output), Git Safety, and Important Notes are not affected by this change.

## 6. Validation

This is a documentation/prompt change with no code or tests.

1. **Self-consistency check** after editing `SKILL.md`:
   - Subject-line rules (Step 2 #1–#3) and the new body rules (Step 3) do not contradict each other.
   - Each of the 5 examples obeys its own rule (dashes, capitalized, no trailing period, imperative).
   - The "include a body when…" rule in Step 3 matches the "decide on body structure" guidance in Step 2.

2. **Manual dry-run** against 3 recent commits, confirming the skill would produce the expected shape:
   - `8cec104f docs(security): improve Javadoc on authentication entry points` → subject-only.
   - `5fc91483 refactor(docker): clean up service credential env vars and values` → bullets, no lead.
   - `bade26db feat(api): add Spring REST Docs to authentication, tasks, and users services` → lead + bullets.

3. **No automated tests.** First real-world validation comes the next time `/commit-msg` runs and produces a message that is either accepted or corrected.

## 7. Git workflow

- Single commit on the current branch (`improve-ci-cd`):
  ```
  docs(commit-msg): require bulleted body in generated commit messages
  ```
- The TODO.md entry at line 21 stays for now and will be checked off as part of release housekeeping (matches the existing pattern in `c3daa0b4`).