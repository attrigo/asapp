---
name: review-guidelines
description: >
  Review code against project guidelines to find violations.
  Use when the user asks to review, check, audit, or validate code against
  project rules, conventions, or guidelines.
  Triggers on requests like "review controllers for guideline violations",
  "check if tests follow our guidelines", "find convention violations",
  "audit code quality against our rules", "review domain classes".
---

# Guidelines Review

Review source code against the project's guidelines defined in `.claude/rules/` to find violations, report them in structured tables, and offer to fix them.

## Trigger

This skill is automatically invoked when the user asks to review, check, audit, or validate code against project rules, conventions, or guidelines. Examples:

- "Review all controllers from authentication service for guideline violations"
- "Check if the test classes in tasks service follow our guidelines"
- "Find violations in the domain layer of users service"
- "Review all factories for convention issues"
- "Audit the security classes against our rules"

## Instructions

### Step 1: Determine Review Scope

Parse the user's request to identify the **service** and **layer/component** to review.

**Service mapping** (aliases to paths):

| Alias | Path |
|-------|------|
| "authentication", "auth" | `services/asapp-authentication-service` |
| "users", "user" | `services/asapp-users-service` |
| "tasks", "task" | `services/asapp-tasks-service` |
| "all" or unspecified | All services |

**Layer/Component mapping** (to glob patterns):

| Scope keyword | Source pattern |
|---------------|---------------|
| "controllers", "rest" | `**/infrastructure/**/in/**/*Controller.java` and `**/infrastructure/**/in/**/*RestAPI.java` |
| "domain" | `**/domain/**/*.java` |
| "application", "services", "use cases" | `**/application/**/*.java` |
| "infrastructure" | `**/infrastructure/**/*.java` |
| "mappers" | `**/mapper/**/*Mapper.java` |
| "repositories", "adapters" | `**/infrastructure/**/out/**/*.java` |
| "entities" | `**/out/entity/**/*.java` |
| "security" | `**/infrastructure/security/**/*.java` |
| "config" | `**/infrastructure/config/**/*.java` |
| "tests", "test" | `src/test/**/*.java` |
| "unit tests" | `**/*Tests.java` |
| "integration tests" | `**/*IT.java`, `**/*E2EIT.java` |
| "factories" | `**/testutil/**/*Factory.java` |
| specific file/class name | Target that file directly |

If the user specifies a combination (e.g., "controller tests"), combine the patterns accordingly (e.g., `**/in/**/*RestControllerTests.java`).

### Step 2: Discover and Load Guidelines

1. **Glob** all `.md` files in `.claude/rules/`
2. **Read each rule file** completely
3. **Extract** the `paths:` frontmatter to know which file patterns each rule applies to
4. **Build a rule registry** mapping each rule to its applicable file patterns and sections

If **no rules** in `.claude/rules/` match the files being reviewed, inform the user:

> "No guidelines found in `.claude/rules/` that apply to [scope]. Guidelines for [layer] have not been created yet."

Then stop — do not invent rules.

### Step 3: Find Files to Review

Use **Glob** with the patterns from Step 1 (scoped to the identified service path) to find all matching files.

If the result set is **large (30+ files)**, inform the user of the count and ask whether to:
- Proceed with all files
- Narrow the scope
- Review in batches

### Step 4: Match Rules to Files

For each file found, determine which rules apply by matching the file path against each rule's `paths:` glob patterns. A single file may match multiple rules (e.g., a `*Tests.java` matches both `testing-core.md` and `testing-unit.md`).

Build a review plan:
```
file: SomeTests.java
  applicable rules: testing-core.md, testing-unit.md

file: SomeFactory.java
  applicable rules: testing-core.md, testing-factories.md
```

If a file matches **no rules**, skip it silently.

### Step 5: Review Each File

For each file with applicable rules:

1. **Read the full file content**
2. **Check every section of every applicable rule** — do not skip any
3. **For each violation found, record**:
   - Rule file and section name
   - Severity level
   - Why the code is wrong (concrete explanation)
   - The violating code snippet (max 3-5 lines, use `...` for omissions)
   - Specific recommendation to fix it

**Severity Levels**:

| Severity | Icon | Criteria |
|----------|------|----------|
| Critical | `C` | Breaks architecture principles, violates hexagonal dependency rules, wrong test slice, security issues |
| Major | `M` | Violates naming conventions, structural patterns, test organization, annotation ordering, missing required patterns |
| Minor | `m` | Style preferences, suboptimal patterns, assertion order, minor convention deviations |

**Rules for reviewing**:
- **No false positives**: Only flag **clear** rule violations. If unsure, it is NOT a violation.
- **Evidence-based**: Every violation must cite the specific rule section and include the actual code.
- **Be thorough**: Check every rule section against every file. Do not take shortcuts.

### Step 6: Handle Unclear Guidelines

If a guideline is **ambiguous, contradictory, or insufficient** to determine compliance:

1. **Research**: Search the web for established best practices on that topic
2. **Present findings**: Show the user:
   - The unclear guideline (quote it)
   - The code in question
   - Best practices found online
   - Your recommended interpretation
3. **Suggest guideline update**: Propose specific text to add/change in the rule file

**Do NOT guess** compliance when a guideline is unclear — always follow this flow.

### Step 7: Output Format

Present results grouped by **package**, then by **file**:

---

**Example output:**

### `com.bcn.asapp.authentication.application.user.in.service`

#### `DeleteUserServiceTests.java`

| # | Sev | Rule | Violation | Code | Recommendation |
|---|-----|------|-----------|------|----------------|
| 1 | C | testing-unit / Test Setup | Domain test must not use mocks | `@ExtendWith(MockitoExtension.class)` | Remove Mockito; test domain logic directly |
| 2 | M | testing-core / Naming | Test name missing condition | `deleteUser_Success()` | Rename to `deleteUser_WhenUserExists_...` |
| 3 | m | testing-core / Variables | Result variable not named `actual` | `var result = service.delete(id)` | Rename to `var actual = ...` |

---

**Output rules**:
- Group files under their **package** heading
- Sort violations within each file by severity (Critical first, then Major, then Minor)
- Use **sequential numbering** per file (restart at 1 for each file)
- Keep code snippets **concise**
- **Omit files with zero violations** — do not list compliant files in the tables
- If a package has no violations across any file, omit the entire package

### Step 8: Summary Statistics

After all violation tables, output:

---

**Example:**

## Summary

| Metric | Value |
|--------|-------|
| Files reviewed | 24 |
| Files with violations | 8 |
| Total violations | 31 |
| Critical | 3 |
| Major | 18 |
| Minor | 10 |
| **Compliance score** | **66.7%** (16/24 files clean) |

**Rules applied**: testing-core.md, testing-unit.md, testing-factories.md

---

### Step 9: Offer Auto-Fix

After the summary, ask:

> Would you like me to fix these violations? Options:
> 1. **Fix all** — apply all recommended fixes
> 2. **Fix by severity** — fix only Critical, or Critical + Major
> 3. **Fix specific files** — choose which files to fix
> 4. **Skip** — no fixes, review only

If the user accepts:
- Apply fixes **file by file** using the Edit tool
- After each file, briefly note what was changed
- After all fixes, show an **updated compliance score**

## Important Notes

- **Thoroughness**: Read every applicable rule section for every file. Never skip rules.
- **Only `.claude/rules/`**: Never invent rules or use guidelines from other locations.
- **Scope discipline**: Only review files matching the user's requested scope.
- **Dynamic discovery**: Always discover rules fresh from `.claude/rules/` — never hardcode rule content.
- **No noise**: If a file is fully compliant, do not mention it. Only report violations.
