# Implementation Plan Template

---

## Document structure

Every implementation plan generated from this template must contain the following top-level sections in this order:

### 1. Header
Document metadata. Must include:
- **Branch:** feature branch name
- **Date:** date the plan was created
- **Author:** plan author

### 2. Audience
A short paragraph stating:
- This document is addressed to Claude Code
- Claude Code is the implementer — it will execute every step in this plan
- The "How to read this document" section must be read in full before executing any step

### 3. How to read this document
Must include the following verbatim:
- The `## Plan Body Schema` section — so Claude Code has the full structural contract in context
- The execution rules:
  - If anything is unclear or ambiguous at any point, stop and ask the user for clarification before proceeding — never assume or infer intent.
  - Execute steps strictly in order. Do not skip or reorder.
  - Before starting a phase or step, verify all preconditions are met. Stop and report if any fail.
  - If a `[edit]` before-snippet does not match exactly, stop and report — do not attempt a best-guess edit.
  - When a `[new-file]` step references a snippet file (`Content: see \`docs/snippets/...\``), read the snippet file first, then write its content to the target path.
  - If a `[transform]` rule matches unexpected sites, stop and report before applying.
  - When scope is `[derived]`, re-run the derivation command after completing all changes — it must return 0 results.
  - After completing a step, verify all acceptance criteria before moving to the next step.
  - If an acceptance criteria check fails, stop and report — do not proceed.
  - Once all acceptance criteria for a step pass, suggest a commit message for the changes made, then stop and wait for the user to review, approve, and commit manually before proceeding to the next step.

### 4. Overview
A summary containing:
- The goal of the implementation plan
- The list of phases with a one-line description of each
- Total scope (approximate number of files affected)
- Key constraints or accepted limitations

### 5. Plan body
The core of the document. Every phase and step must strictly follow the Phase/Step structure defined in `## Plan Body Template` — no additional fields, no omitted required fields.

### 6. References
A list of all external documents, tickets, and links referenced in the plan. Required — include even if empty.

| Title | Description | Link |
|-------|-------------|------|
| Document or ticket title | What it contains and when to consult it | URL or file path |

---

## Plan Body Schema

### General rules
- A step using explicit scope should touch no more than ~10 files. If more, use derived scope with a transform rule instead.

### Precondition types
| Type | Meaning | Claude Code action |
|------|---------|-------------------|
| `[phase]` | Entire phase must be complete | Verify against plan execution state |
| `[step]` | Specific step must be complete | Verify against plan execution state |
| `[branch]` | External branch must be merged | Run `git log` / `git branch` to confirm |
| `[file]` | File must exist | Check file exists at path |
| `[class]` | Class must exist | Grep for class definition |
| `[env]` | Local tool/runtime prerequisite verifiable by a shell command (e.g. `java -version`) | Run the specified command and assert the expected output |
| `[user]` | Prerequisite requiring manual user action (not verifiable by Claude) | Stop, list every `[user]` requirement, and wait for explicit user confirmation before proceeding |

### Acceptance criteria types
| Type | Meaning | Claude Code action |
|------|---------|-------------------|
| `[grep]` | Pattern must have at least one match | Run grep, assert non-empty |
| `[no-match]` | Pattern must return 0 results | Run grep, assert empty |
| `[compile]` | Module must compile without errors | Run mvn compile |
| `[test]` | Specific test(s) must pass | Run mvn test |
| `[file-exists]` | File must exist at path | Check file exists |
| `[manual]` | Human must run a command/tool and verify output by reading it | Stop and instruct the user exactly what to run and what to look for |

### Scope modes
- `[explicit]` — when the set is small and known (≤ ~10 files): list exact file paths
- `[derived]` — when the set is large or dynamic: provide a grep/glob command that resolves the file list at execution time

> Use `[explicit]` or `[derived]`, never both in the same step.
> For bulk steps, scope MUST be `[derived]`, not a file list.

### Changes types
All change types are optional and combinable within a single step.

| Type | When to use | Format |
|------|-------------|--------|
| `[new-file]` | A new file must be created, or an existing file must be completely replaced | Path + full content inline **or** path + reference to a snippet file |
| `[edit]` | Small known set of files (explicit scope) | Before/after snippet per site |
| `[transform]` | Large/dynamic set of files (derived scope) | Generalized find/replace rule |
| `[delete]` | Code or files must be removed | What to remove and where |
| `[command]` | A shell or Maven command must be executed | Full command + expected outcome |

> `[edit]` pairs with explicit scope. `[transform]` pairs with derived scope.
> `[new-file]` replaces the existing file when the file already exists — git history is preserved.

#### Snippet files
When a `[new-file]` target is large (≥ ~50 lines), extract its content to a dedicated file under `docs/snippets/` and reference it from the step instead of inlining it. This keeps the plan within Claude Code's read limit and makes diffs cleaner.

- **Naming convention:** `docs/snippets/phase{N}-step{N.M}-FileName.ext`
- **Reference syntax** (in place of the inline content block):
  ```
  Content: see `docs/snippets/phase1-step1.3-JwtDecoder.java`
  ```

---

## Plan Body Template

# Phase N — Title
- **Context:** (optional) what the phase delivers and what state the codebase is in when it starts
- **Preconditions:** (optional)
  - `<precondition-type>` reference
- **Acceptance criteria:** (optional)
  - `<criteria-type>` condition

## Step N.M — Title
- **Preconditions:** (optional)
  - `<precondition-type>` reference
- **Scope:** _(pick one mode only — never both)_
  - `[explicit]` `path/to/File.java`
  - `[derived]` `grep -r "..." src/ --include="*.java" -l`
  - **Exclusions:** (optional)
    | File | Reason |
    |------|--------|
    | `File.java` | covered in step X.Y |
- **Changes:** _(all types are optional and combinable; include only what is needed)_
  - `[new-file]` `path/to/NewFile.java` — full content below _(inline)_
  - `[new-file]` `path/to/NewFile.java`
    Content: see `docs/snippets/phaseN-stepN.M-NewFile.java` _(snippet file — use when content ≥ ~50 lines)_
  - `[edit]` before/after snippet per site
  - `[transform]` find: pattern / replace: pattern
  - `[delete]` what to remove and where
  - `[command]` shell or Maven command to execute
- **Notes:** (optional)
  - edge cases, gotchas, accepted limitations
- **Acceptance criteria:**
  - `<criteria-type>` condition

---

## Example

# MyFeature Implementation Plan

## Header

- **Branch:** feature/short-description
- **Date:** 2026-01-01
- **Author:** author name

## Audience

This document is addressed to Claude Code. You are the implementer — you will execute every step in this plan.
Read the "How to read this document" section in full before executing any step.

## How to read this document

### Plan Body Schema
_(reproduced verbatim from the Implementation Plan Template — `## Plan Body Schema` section)_

### Execution rules
- If anything is unclear or ambiguous at any point, stop and ask the user for clarification before proceeding — never assume or infer intent.
- Execute steps strictly in order. Do not skip or reorder.
- Before starting a step, verify all preconditions are met. Stop and report if any fail.
- If a `[edit]` before-snippet does not match exactly, stop and report — do not attempt a best-guess edit.
- When a `[new-file]` step references a snippet file (`Content: see \`docs/snippets/...\``), read the snippet file first, then write its content to the target path.
- If a `[transform]` rule matches unexpected sites, stop and report before applying.
- When scope is `[derived]`, re-run the derivation command after completing all changes — it must return 0 results.
- After completing a step, verify all acceptance criteria before moving to the next step.
- If an acceptance criteria check fails, stop and report — do not proceed.
- Once all acceptance criteria for a step pass, suggest a commit message for the changes made, then stop and wait for the user to review, approve, and commit manually before proceeding to the next step.

## Overview

- **Goal:** brief description of what this plan implements and why
- **Phases:**
  - Phase 1 — one-line description
  - Phase 2 — one-line description
- **Total scope:** ~N files affected
- **Key constraints:** any accepted limitations or known trade-offs

## Plan body

# Phase 1 — Add `completedAt` to the Task domain
- **Context:** Adds the new field end-to-end across the domain model and the database schema. The service must remain deployable on an existing database, so a Liquibase changeset is required.
- **Preconditions:**
  - `[branch]` develop is up to date
- **Acceptance criteria:**
  - `[file-exists]` `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/domain/Task.java`
  - `[compile]` `mvn compile -pl services/asapp-tasks-service`

## Step 1.1 — Add `completedAt` field to `Task`
- **Scope:**
  - `[explicit]` `services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/domain/Task.java`
- **Changes:**
  - `[edit]`
    ```java
    // Before
        private Instant endDate;
    }
    ```
    ```java
    // After:
        private Instant endDate;

        private Instant completedAt;
    }
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -r "completedAt" services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/domain/Task.java`
  - `[compile]` `mvn compile -pl services/asapp-tasks-service`

# Phase 2 — Add `completedAt` to the Task database schema

## Step 2.1 — Add Liquibase changeset for `completed_at` column
- **Preconditions:**
  - `[step]` Step 1.1 complete
- **Scope:**
  - `[explicit]` `services/asapp-tasks-service/src/main/resources/liquibase/db/changelog/v0.2.0/changesets/20260404_1_add_completed_at_to_task.xml`
- **Changes:**
  - `[new-file]` `services/asapp-tasks-service/src/main/resources/liquibase/db/changelog/v0.2.0/changesets/20260404_1_add_completed_at_to_task.xml`
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                           https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

        <!-- Add completedAt timestamp to task table -->
        <changeSet id="20260404_1_add_completed_at_to_task" author="attrigo">
            <preConditions onFail="MARK_RAN">
                <not>
                    <columnExists tableName="task" columnName="completed_at"/>
                </not>
            </preConditions>
            <addColumn tableName="task">
                <column name="completed_at" type="TIMESTAMP WITH TIME ZONE"/>
            </addColumn>
            <rollback>
                <dropColumn tableName="task" columnName="completed_at"/>
            </rollback>
        </changeSet>

    </databaseChangeLog>
    ```
- **Acceptance criteria:**
  - `[file-exists]` `services/asapp-tasks-service/src/main/resources/liquibase/db/changelog/v0.2.0/changesets/20260404_1_add_completed_at_to_task.xml`

# Phase 3 — Update test infrastructure

## Step 3.1 — Add `completedAt` to all `TaskMother` builder methods
- **Preconditions:**
  - `[phase]` Phase 2 complete
- **Scope:**
  - `[derived]` `grep -rl "class TaskMother" services/ --include="*.java"`
- **Changes:**
  - `[transform]`
    ```java
    // Find:
                .endDate(TASK_END_DATE)
    ```
    ```java
    // Replace:
                .endDate(TASK_END_DATE)
                .completedAt(null)
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -r "\.completedAt(" services/ --include="*.java"`

## Step 3.2 — Remove deprecated `withNoEndDate()` helper
- **Preconditions:**
  - `[step]` Step 3.1 complete
- **Scope:**
  - `[derived]` `grep -rl "withNoEndDate" services/ --include="*.java"`
- **Changes:**
  - `[delete]`
    ```java
    public static Task withNoEndDate() {
        return aTask().endDate(null).build();
    }
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -r "withNoEndDate" services/ --include="*.java"`
  - `[test]` `mvn test -pl services/asapp-tasks-service`

## References

| Title | Description | Link |
|-------|-------------|------|
| ASAPP-123 | Ticket tracking the `completedAt` feature request | https://github.com/org/repo/issues/123 |
