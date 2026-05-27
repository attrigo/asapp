# Bulleted Body in `commit-msg` Skill — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update the `commit-msg` skill so generated commit messages use a deterministic body shape — optional lead paragraph (WHY) followed by a required bulleted list (WHAT, one change per bullet) — while keeping subject-only valid for trivial changes.

**Architecture:** Documentation/prompt change only. The skill at `.claude/skills/commit-msg/SKILL.md` is edited in three localized regions (Step 2 checklist, Step 3 message-generation rules, Examples section). No code, tests, or other tooling are affected.

**Tech Stack:** Markdown (skill prompt), Git (commit), Conventional Commits 1.0.0 spec compliance.

**Spec:** `docs/superpowers/specs/2026-05-07-commit-msg-bulleted-body-design.md`

---

## File Structure

Single file modified, no files created or deleted:

- **Modify:** `.claude/skills/commit-msg/SKILL.md` — three localized regions:
  - Step 2 *(Analyze Changes)*: append a 5th checklist item about deciding on body structure.
  - Step 3 *(Generate Message)*: replace the multi-line template, replace one rule, add a "Body format" sub-block.
  - Examples section: rewrite Example 2's body, add Example 4 (bullets only), add Example 5 (lead + bullets).

---

## Task 1: Update Step 3 (Generate Message) — core format spec

This task lands the new body-format rules. We do Step 3 first because the new rules drive what the other regions need to be consistent with.

**Files:**
- Modify: `.claude/skills/commit-msg/SKILL.md` (lines 108–130 — section *Step 3: Generate Message*)

- [ ] **Step 1: Read the file to confirm exact current content**

Run: Read `.claude/skills/commit-msg/SKILL.md`
Expected: Lines 108–130 match the snippet shown in the spec §5.2 (current "Multi-line format" template + "Rules" list with the *"Use multi-line body only when changes are complex or breaking"* rule).

If the lines have shifted, locate the section by header `### Step 3: Generate Message` and adjust line ranges in subsequent steps.

- [ ] **Step 2: Replace the entire Step 3 section with the new content**

Use Edit tool with the following:

`old_string`:
````
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
````

`new_string`:
````
### Step 3: Generate Message

**Single-line format** (most cases — single conceptual change with no nuance):
```
<type>(<scope>): <description>
```

**Multi-line format** (when the subject cannot capture the change alone):
```
<type>(<scope>): <description>

<optional lead paragraph: 1-3 sentences explaining the WHY>

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
- Reference issues when applicable: `Closes #123`, `Refs #456`

**Body format** (when a body is included):
- Optional lead paragraph (1-3 sentences) explaining the WHY — motivation, root cause, or constraint. Omit when the subject already conveys the why and the bullets stand on their own.
- Required bulleted list of changes:
  - Marker: `-` followed by a single space
  - First letter capitalized
  - Imperative mood (`Add`, `Rename`, `Fix` — same tense as the subject)
  - No trailing period
  - One change per bullet, single line per bullet (no nested bullets, no multi-line bullets)
- Footers (`BREAKING CHANGE:`, `Closes #N`, `Refs #N`) follow the bulleted list, separated by a blank line.
````

- [ ] **Step 3: Verify the edit applied correctly**

Run: Read `.claude/skills/commit-msg/SKILL.md` (focus on the *Step 3: Generate Message* region).
Expected:
- The "Multi-line format" template now shows `<optional lead paragraph...>` and bulleted lines.
- The "Rules" list contains *"Include a body only when the subject cannot capture the change alone..."* (no longer mentions "complex or breaking").
- A new "Body format" sub-block follows the Rules list with bullet-formatting details.

**No commit yet.** SKILL.md will be partially inconsistent (rules updated but examples still match the old format) until Tasks 2 and 3 complete.

---

## Task 2: Update Step 2 (Analyze Changes) — body-decision checklist

Add a 5th item to the Step 2 numbered list so the analysis phase explicitly produces the input the new Step 3 rules need.

**Files:**
- Modify: `.claude/skills/commit-msg/SKILL.md` (lines 100–105 — end of Step 2)

- [ ] **Step 1: Apply the edit**

Use Edit tool with the following:

`old_string`:
````
4. **Detect breaking changes**:
   - Removed public API endpoints or methods
   - Changed method signatures or return types
   - Renamed configuration properties
   - Changed database schema without migration
   - If breaking: use `!` suffix on type and/or `BREAKING CHANGE:` footer
````

`new_string`:
````
4. **Detect breaking changes**:
   - Removed public API endpoints or methods
   - Changed method signatures or return types
   - Renamed configuration properties
   - Changed database schema without migration
   - If breaking: use `!` suffix on type and/or `BREAKING CHANGE:` footer

5. **Decide on body structure**:
   - Count distinct logical changes in the diff. If ≥2, or if the why is non-obvious from the subject alone, include a body.
   - If a body is needed, decide whether a lead paragraph adds context the subject can't carry. If not, go straight to bullets.
````

- [ ] **Step 2: Verify the edit applied correctly**

Run: Read `.claude/skills/commit-msg/SKILL.md` (focus on Step 2).
Expected: Step 2 now has 5 numbered items; #5 is *"Decide on body structure"* with the two sub-bullets.

**No commit yet.** Examples in the next task still need updating.

---

## Task 3: Update Examples section

Refresh the examples to be consistent with the new format. Examples 1 and 3 remain valid subject-only cases and stay as-is. Example 2 gets its body rewritten to use bullets per the new spec. Two new examples (4 and 5) demonstrate the bullets-only and lead+bullets cases.

**Files:**
- Modify: `.claude/skills/commit-msg/SKILL.md` (lines 136–184 — section *## Examples*)

- [ ] **Step 1: Rewrite Example 2's generated message and reasoning**

Use Edit tool with the following:

`old_string`:
````
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
````

`new_string`:
````
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
````

- [ ] **Step 2: Append Example 4 and Example 5 after Example 3**

Use Edit tool with the following:

`old_string`:
````
Generated commit message:
```
refactor(api): standardize error handling across services
```

Reasoning: Structural improvement affecting multiple services, API layer scope used for cross-cutting concern.

## Git Safety
````

`new_string`:
````
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
fix(security): remove AuthenticationManager bean from tasks and users services

Exposing @Bean AuthenticationManager in a proxyBeanMethods=false class
caused a StackOverflowError in the actuator filter chain.

- Drop the AuthenticationManager bean from tasks and users services
- Keep the bean in authentication service where CredentialsAuthenticatorAdapter requires it
```

Reasoning: The why (StackOverflowError root cause) is non-obvious from the subject and warrants a lead paragraph; bullets list the surgical changes.

## Git Safety
````

- [ ] **Step 3: Verify the edits applied correctly**

Run: Read `.claude/skills/commit-msg/SKILL.md` (focus on the *## Examples* section).
Expected:
- Example 1 unchanged (test factory rename, subject-only).
- Example 2 body now uses dash-prefixed bullets (no period); footer still present and now also includes the migration guidance.
- Example 3 unchanged (cross-module refactor, subject-only).
- Example 4 present (bullets only, `refactor(docker)` style).
- Example 5 present (lead + bullets, `fix(security)` style).
- The *## Git Safety* heading still follows after the examples.

---

## Task 4: Self-consistency check (per spec §6.1)

Validate the file holistically before committing. Fixes to anything found here go inline in `SKILL.md`.

**Files:**
- Read-only: `.claude/skills/commit-msg/SKILL.md`

- [ ] **Step 1: Re-read the full file**

Run: Read `.claude/skills/commit-msg/SKILL.md`

- [ ] **Step 2: Verify rule alignment**

Check:
- Step 2 #5 trigger ("≥2 distinct logical changes, or non-obvious why") matches Step 3 rule ("Include a body only when the subject cannot capture the change alone").
- Step 3 "Body format" sub-block bullet rules (dash + space, capitalized, imperative, no trailing period) are not contradicted anywhere else in the file.

If any contradiction is found, edit the offending region and repeat this step.

- [ ] **Step 3: Verify each example obeys its own rules**

For each of Examples 1–5, check the generated message:
- Subject ≤72 chars, imperative, no trailing period.
- If body present: starts after a blank line; bullets use `- ` prefix; each bullet starts with a capital letter; each bullet uses imperative mood; no bullet ends with a period; no nested or multi-line bullets.
- If lead paragraph present: 1-3 sentences, separated from bullets by a blank line.
- If footer present: separated from bullets by a blank line; `BREAKING CHANGE:` or `Closes #N` form.

Specific spot-checks:
- Example 1 subject `test(authentication): improve factory method naming consistency` (subject-only, 57 chars).
- Example 2 bullets `- Remove the /api/auth/verify endpoint from the REST API`, `- Remove the corresponding controller method and tests` — no periods, imperative, capitalized.
- Example 3 subject `refactor(api): standardize error handling across services` (subject-only, 56 chars).
- Example 4 bullets all start with `- ` + capital + imperative verb (`Rename`, `Rename`, `Simplify`); none end with a period.
- Example 5 lead is two sentences ending with a period (sentence punctuation, not bullet rule); bullets `- Drop ...`, `- Keep ...` no trailing period.

If any example violates its rule, edit it.

---

## Task 5: Manual dry-run validation (per spec §6.2)

Apply the new rules to three real recent commits and verify the skill would now produce the expected shape. This is a thinking exercise, not a tool invocation — read the rules in the updated `SKILL.md`, mentally apply them to each commit, and check the predicted shape against the listed expectation.

**Files:**
- Read-only: `.claude/skills/commit-msg/SKILL.md`

- [ ] **Step 1: Dry-run case A — subject-only commit**

Commit: `8cec104f docs(security): improve Javadoc on authentication entry points`
- Single conceptual change: improve Javadoc.
- Step 2 #5 check: 1 logical change; why is obvious from subject. → no body.
- Predicted output: subject-only.
- ✅ Matches the actual commit (no body in the original).

- [ ] **Step 2: Dry-run case B — bullets-only commit**

Commit: `5fc91483 refactor(docker): clean up service credential env vars and values`
- Multiple sub-actions: 2 renames + 1 simplification.
- Step 2 #5 check: ≥2 logical changes; why is obvious from subject ("clean up"). → body, no lead.
- Predicted output: subject + 3 bullets.
- ✅ Matches Example 4 in the updated SKILL.md.

- [ ] **Step 3: Dry-run case C — lead + bullets commit**

Commit: `bade26db feat(api): add Spring REST Docs to authentication, tasks, and users services`
- Multiple sub-actions: dependencies, dedicated test classes, helpers, Asciidoctor template, AsciiDoc files, Maven phase, README updates, rules update.
- Step 2 #5 check: ≥2 logical changes; the why ("add documentation infrastructure") could come from either the subject or a lead. Given the breadth, a lead paragraph framing the WHAT-being-added (e.g., "Add documentation infrastructure to the three main services:") adds context the subject can't carry on its own.
- Predicted output: subject + lead + bullets.
- ✅ Matches the actual commit's structure.

If any dry-run produces an unexpected shape, return to Task 4 and tighten the rules in `SKILL.md`. (Do not adjust the dry-run cases themselves to make them pass — that defeats the validation.)

---

## Task 6: Commit

Single commit on the current branch (`improve-ci-cd`).

**Files:**
- Stage: `.claude/skills/commit-msg/SKILL.md`

- [ ] **Step 1: Confirm staging state**

Run: `git status --porcelain`
Expected: only `.claude/skills/commit-msg/SKILL.md` shows as modified (`M` or ` M`). The previously-untracked `docs/superpowers/specs/2026-05-07-commit-msg-bulleted-body-design.md` and `docs/superpowers/plans/2026-05-07-commit-msg-bulleted-body.md` are part of separate commits (the spec was committed earlier; the plan commit is also separate from this one).

If anything else is modified or untracked that shouldn't be in this commit, stage selectively in the next step.

- [ ] **Step 2: Stage and commit**

Run:
```bash
git add .claude/skills/commit-msg/SKILL.md
git commit -m "docs(commit-msg): require bulleted body in generated commit messages"
```

The commit subject is intentionally subject-only because, under the *current* (pre-edit, since this commit is what lands the new rules) skill rules, a single conceptual change with no nuance is subject-only — and the change is exactly that. Even under the *new* rules being landed, this commit is a single conceptual change to the skill.

- [ ] **Step 3: Verify the commit landed**

Run: `git log -1 --pretty=format:"%h %s"`
Expected: `<sha> docs(commit-msg): require bulleted body in generated commit messages`

Run: `git status`
Expected: working tree clean (or only unrelated untracked files).