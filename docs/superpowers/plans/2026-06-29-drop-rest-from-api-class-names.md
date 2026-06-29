# Drop "Rest" from API-Contract Class Names Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the API-contract interfaces, URL-constant classes, and contract-documentation tests to drop the redundant `Rest` token, updating every reference so the build stays green.

**Architecture:** Pure rename + reference-update refactor across `asapp-commons-url`, `asapp-http-clients`, and the three services. No behaviour, HTTP contract, endpoint path, or constant *value* changes. The rename is applied as ordered, unique-token substring substitutions (`RestAPIURL`→`APIURL` *before* `RestAPI`→`API`, so the longer token is consumed first and never half-rewritten), each paired with a `git mv` of the defining files. The build is green after every task.

**Tech Stack:** Java 25, Spring Boot 4, Maven multi-module reactor, Spotless (Eclipse formatter), ripgrep + sed for mechanical substitution.

## Global Constraints

- Rename files with `git mv <old> <new>` only — never delete-and-recreate.
- Constant names (`TASKS_ROOT_PATH`, …), constant *values*, and endpoint paths are unchanged. Only class names and the prose inside the renamed interfaces change.
- `Rest` is kept on `*RestController`, `*RestControllerIT`, `*RestControllerTests`, `RestDocsWebMvcTestContext`, `RestDocsConstrainedFields`, `RestClientConfiguration` — do not rename these.
- **Maven policy:** never run a command that executes integration/E2E tests autonomously. `mvn clean verify` and `mvn clean install` (the `install` phase runs Failsafe) are **slow** — stop and ask the developer. The only autonomous build is `mvn clean install -DskipITs` (compiles everything incl. IT sources, runs unit `*Tests`, skips `*IT`/`*E2EIT`).
- Run `mvn spotless:apply` before every commit (import order shifts when class names change).
- `.claude/` edits hit a permission gate — if denied, hand the exact edits to the developer; do not work around it.
- LF line endings are enforced (`.gitattributes` + pre-commit hook). Conventional Commits.
- Stage only the files each task touches (`git add <paths>`), never `git add -A` — the working tree has an unrelated `TODO.md` edit and untracked spec/plan docs.

---

## Task 0: Branch + commit the design docs

**Files:**
- Create branch off `main`
- Add: `docs/superpowers/specs/2026-06-29-drop-rest-from-api-class-names-design.md`, `docs/superpowers/plans/2026-06-29-drop-rest-from-api-class-names.md`

- [ ] **Step 1: Create the feature branch**

```bash
git checkout -b refactor/drop-rest-from-api-names
```

Expected: `Switched to a new branch 'refactor/drop-rest-from-api-names'`

- [ ] **Step 2: Commit the spec and this plan**

```bash
git add docs/superpowers/specs/2026-06-29-drop-rest-from-api-class-names-design.md docs/superpowers/plans/2026-06-29-drop-rest-from-api-class-names.md
git commit -m "docs(specs): add design + plan for dropping Rest from API class names"
```

Expected: one commit created; `git status` shows the unrelated `TODO.md` modification still unstaged.

---

## Task 1: Rename URL-constant classes `*RestAPIURL` → `*APIURL`

Renames `TaskRestAPIURL`, `AuthenticationRestAPIURL`, and both `UserRestAPIURL` classes, plus every `.java` reference (API interfaces' static imports, `asapp-http-clients`, and all importing tests). Done first so the longer `RestAPIURL` token is consumed before Task 2 touches `RestAPI`.

**Files:**
- Rename: `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/tasks/TaskRestAPIURL.java` → `TaskAPIURL.java`
- Rename: `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/authentication/AuthenticationRestAPIURL.java` → `AuthenticationAPIURL.java`
- Rename: `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/authentication/UserRestAPIURL.java` → `UserAPIURL.java`
- Rename: `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/users/UserRestAPIURL.java` → `UserAPIURL.java`
- Modify (references, via bulk substitution): all `.java` under `services/` and `libs/` containing `RestAPIURL` — the 4 interfaces, `asapp-http-clients` `TasksHttpClient` + `TasksHttpClientTests`, and the tests `Task/User/Authentication *RestControllerIT`, `*RestControllerDocumentationIT`, `*E2EIT`, plus `TasksGatewayAdapterIT`, `RestClientConfigurationIT`, `ResilienceConfigurationIT`, `PasswordEncoderIT`.

- [ ] **Step 1: `git mv` the four defining files**

```bash
git mv libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/tasks/TaskRestAPIURL.java libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/tasks/TaskAPIURL.java
git mv libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/authentication/AuthenticationRestAPIURL.java libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/authentication/AuthenticationAPIURL.java
git mv libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/authentication/UserRestAPIURL.java libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/authentication/UserAPIURL.java
git mv libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/users/UserRestAPIURL.java libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/users/UserAPIURL.java
```

Expected: `git status` shows 4 renames.

- [ ] **Step 2: Substitute the `RestAPIURL` token in all Java sources**

This renames the class declarations, the private constructors, and every static import in one pass. The token is unique, so the substring swap is exact.

```bash
grep -rl --include='*.java' 'RestAPIURL' services libs | xargs sed -i 's/RestAPIURL/APIURL/g'
```

Expected: `TaskRestAPIURL`→`TaskAPIURL`, `UserRestAPIURL`→`UserAPIURL`, `AuthenticationRestAPIURL`→`AuthenticationAPIURL` everywhere.

- [ ] **Step 3: Verify no `RestAPIURL` token remains in sources**

```bash
rg 'RestAPIURL' services libs --type java
```

Expected: no output (exit code 1).

- [ ] **Step 4: Apply formatting**

```bash
mvn spotless:apply
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Build and run unit tests (ITs skipped)**

```bash
mvn clean install -DskipITs
```

Expected: `BUILD SUCCESS` across all modules.

- [ ] **Step 6: Commit**

```bash
git add services libs
git commit -m "refactor(commons-url): rename *RestAPIURL classes to *APIURL"
```

---

## Task 2: Rename API-contract interfaces `*RestAPI` → `*API` + scrub prose

Renames `TaskRestAPI`, `AuthenticationRestAPI`, and both `UserRestAPI` interfaces, updates the four controllers' `implements` clause (same package — no import line), and removes `REST` from the interface Javadoc summary and `@Tag` description. Safe to substring-swap `RestAPI`→`API` now because Task 1 already turned every `…RestAPIURL` into `…APIURL`.

**Files:**
- Rename: `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskRestAPI.java` → `TaskAPI.java`
- Rename: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationRestAPI.java` → `AuthenticationAPI.java`
- Rename: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserRestAPI.java` → `UserAPI.java`
- Rename: `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/UserRestAPI.java` → `UserAPI.java`
- Modify (`implements`): `TaskRestController.java:50`, `AuthenticationRestController.java:40`, `UserRestController.java:47` (auth), `UserRestController.java:49` (users)
- Modify (prose): the four interface files above

- [ ] **Step 1: `git mv` the four interface files**

```bash
git mv services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskRestAPI.java services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskAPI.java
git mv services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationRestAPI.java services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationAPI.java
git mv services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserRestAPI.java services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserAPI.java
git mv services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/UserRestAPI.java services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/UserAPI.java
```

Expected: `git status` shows 4 renames.

- [ ] **Step 2: Substitute the `RestAPI` token in all Java sources**

Renames each interface declaration and the matching `implements` clause in its controller.

```bash
grep -rl --include='*.java' 'RestAPI' services libs | xargs sed -i 's/RestAPI/API/g'
```

Expected: `TaskRestAPI`→`TaskAPI`, `UserRestAPI`→`UserAPI`, `AuthenticationRestAPI`→`AuthenticationAPI` (interface decls + the 4 `implements`).

- [ ] **Step 3: Scrub `REST` from the interface prose**

Only the four interfaces contain the literal `REST API contract`.

```bash
sed -i 's/REST API contract/API contract/g' \
  services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskAPI.java \
  services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationAPI.java \
  services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserAPI.java \
  services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/UserAPI.java
```

Expected: each interface's Javadoc summary now reads `* API contract for … operations.` and each `@Tag(description = "API contract for …")`.

- [ ] **Step 4: Verify no `RestAPI` token and no `REST API contract` remain in sources**

```bash
rg 'RestAPI' services libs --type java ; rg 'REST API contract' services libs --type java
```

Expected: no output from either (exit code 1).

- [ ] **Step 5: Apply formatting**

```bash
mvn spotless:apply
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Build and run unit tests (ITs skipped)**

```bash
mvn clean install -DskipITs
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add services libs
git commit -m "refactor(api): rename *RestAPI contract interfaces to *API"
```

---

## Task 3: Rename contract-documentation tests `*RestControllerDocumentationIT` → `*APIDocumentationIT`

Renames the four documentation ITs and repoints each Javadoc `{@link}` from the controller to the `*API` interface (its subject is the contract, not the adapter). The full class-name token has no external references.

**Files:**
- Rename: `services/asapp-tasks-service/src/test/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskRestControllerDocumentationIT.java` → `TaskAPIDocumentationIT.java`
- Rename: `services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationRestControllerDocumentationIT.java` → `AuthenticationAPIDocumentationIT.java`
- Rename: `services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserRestControllerDocumentationIT.java` → `UserAPIDocumentationIT.java`
- Rename: `services/asapp-users-service/src/test/java/com/attrigo/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java` → `UserAPIDocumentationIT.java`

- [ ] **Step 1: `git mv` the four documentation ITs**

```bash
git mv services/asapp-tasks-service/src/test/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskRestControllerDocumentationIT.java services/asapp-tasks-service/src/test/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskAPIDocumentationIT.java
git mv services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationRestControllerDocumentationIT.java services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationAPIDocumentationIT.java
git mv services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserRestControllerDocumentationIT.java services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserAPIDocumentationIT.java
git mv services/asapp-users-service/src/test/java/com/attrigo/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java services/asapp-users-service/src/test/java/com/attrigo/asapp/users/infrastructure/user/in/UserAPIDocumentationIT.java
```

Expected: `git status` shows 4 renames.

- [ ] **Step 2: Rename the class declaration token**

```bash
grep -rl --include='*.java' 'RestControllerDocumentationIT' services | xargs sed -i 's/RestControllerDocumentationIT/APIDocumentationIT/g'
```

Expected: `class TaskAPIDocumentationIT …` etc.

- [ ] **Step 3: Repoint the Javadoc summary from the controller to the interface**

The capture group maps `Task`→`Task`, `User`→`User`, `Authentication`→`Authentication`, so the link target becomes the matching `*API` interface and `REST API documentation` becomes `contract documentation`.

```bash
grep -rl --include='*.java' 'RestController} REST API documentation' services | xargs sed -i -E 's/\{@link ([A-Za-z]+)RestController\} REST API documentation/{@link \1API} contract documentation/'
```

Expected: each summary now reads e.g. `* Tests {@link TaskAPI} contract documentation.`

- [ ] **Step 4: Verify the old token and old summary are gone**

```bash
rg 'RestControllerDocumentationIT|RestController} REST API documentation' services --type java
```

Expected: no output (exit code 1).

- [ ] **Step 5: Apply formatting**

```bash
mvn spotless:apply
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Compile everything (ITs skipped — validates IT test-compilation)**

```bash
mvn clean install -DskipITs
```

Expected: `BUILD SUCCESS` (test sources, including the renamed ITs, compile).

- [ ] **Step 7: Commit**

```bash
git add services
git commit -m "test(api): rename *RestControllerDocumentationIT to *APIDocumentationIT"
```

---

## Task 4: Update the `asapp-commons-url` README

The only documentation file referencing the renamed classes. Prose ("REST API endpoint constants") is left as accurate English — only class-name references change.

**Files:**
- Modify: `libs/asapp-commons-url/README.md` (import snippets ~lines 49 & 55; Reference list ~lines 91-93)

- [ ] **Step 1: Substitute the class-name token in the README**

```bash
sed -i 's/RestAPIURL/APIURL/g' libs/asapp-commons-url/README.md
```

Expected: `AuthenticationRestAPIURL`→`AuthenticationAPIURL`, `TaskRestAPIURL`→`TaskAPIURL`, `UserRestAPIURL`→`UserAPIURL` in the import examples and the Reference bullet list.

- [ ] **Step 2: Verify**

```bash
rg 'RestAPIURL' libs/asapp-commons-url/README.md
```

Expected: no output (exit code 1).

- [ ] **Step 3: Commit**

```bash
git add libs/asapp-commons-url/README.md
git commit -m "docs(commons-url): update README references to *APIURL"
```

---

## Task 5: Retarget the `.claude` rule and reviewer-agent globs (permission-gated)

The path-scoped rule globs on `*RestAPI.java`; after Task 2 that pattern matches nothing, so the rule would silently stop applying. Retarget to `*API.java` (verified safe: the only `…API.java` files under `infrastructure/**` are the four renamed interfaces).

> **Permission gate:** edits under `.claude/` are blocked by the auto-mode classifier. Attempt the edits; if denied, give the developer the exact diffs below and ask them to apply. Do not work around the gate.

**Files:**
- Modify: `.claude/rules/rest.md` — frontmatter glob (line 3), prose (line 39), section heading
- Modify: `.claude/agents/code-reviewer.md` — the `rest.md` path glob (line 39)

- [ ] **Step 1: `.claude/rules/rest.md` — frontmatter glob (line 3)**

Change:
```
  - "**/infrastructure/**/*RestAPI.java"
```
to:
```
  - "**/infrastructure/**/*API.java"
```

- [ ] **Step 2: `.claude/rules/rest.md` — section heading**

Change `## REST API Interface Pattern` to `## API Interface Pattern`.

- [ ] **Step 3: `.claude/rules/rest.md` — prose (line 39)**

Change:
```
- Whenever a `*RestAPI.java` or its `api-guide.adoc` changes, update the other to keep description, status codes, and parameters in sync
```
to:
```
- Whenever a `*API.java` or its `api-guide.adoc` changes, update the other to keep description, status codes, and parameters in sync
```

- [ ] **Step 4: `.claude/agents/code-reviewer.md` — path glob (line 39)**

In the `rest.md` paths list, change `**/infrastructure/**/*RestAPI.java` to `**/infrastructure/**/*API.java`.

- [ ] **Step 5: Verify**

```bash
rg 'RestAPI' .claude
```

Expected: no output (exit code 1).

- [ ] **Step 6: Commit**

```bash
git add .claude/rules/rest.md .claude/agents/code-reviewer.md
git commit -m "docs(claude): retarget rest rule and reviewer globs to *API.java"
```

---

## Task 6: Full integration verification (developer-gated)

No code changes — a final gate confirming the renamed ITs/E2EITs pass end-to-end.

- [ ] **Step 1: Repo-wide residual check**

```bash
rg 'RestAPIURL|RestAPI|RestControllerDocumentationIT'
```

Expected: matches only in `TODO.md` (the task entry) and the frozen `docs/superpowers/specs/2026-05-*`, `2026-06-27-*` historical specs. No matches under `services/`, `libs/`, or `.claude/`.

- [ ] **Step 2: Request the full IT/E2EIT build from the developer**

Per the Maven policy, do not run this autonomously. Ask the developer to run (or approve running):

```bash
mvn clean verify
```

Expected: `BUILD SUCCESS` with all `*IT` and `*E2EIT` green. The merged commits are squashed into `main` later via the close-task skill (which also checks the `TODO.md` box on line 60).

---

## Self-Review

**1. Spec coverage:** §4.1 interfaces → Task 2; §4.2 URL classes → Task 1; §4.3 documentation ITs → Task 3; §5 prose scrub → Task 2 Step 3 + Task 3 Step 3; §7 controllers/tests/http-clients references → Tasks 1-2 (bulk substitution); §7 README → Task 4; §7 `.claude` surfaces → Task 5; §8 build policy → Global Constraints + per-task Step using `-DskipITs`, full `verify` deferred to Task 6; §9 glob over-match risk → Task 5 verified note. All spec sections map to a task.

**2. Placeholder scan:** No TBD/TODO/"handle edge cases". Every step has an exact command and expected output. The `[A-Za-z]+` in Task 3 Step 3 is a real regex capture, not a placeholder.

**3. Type/name consistency:** Token mappings are consistent across tasks — `RestAPIURL`→`APIURL` (Task 1) precedes `RestAPI`→`API` (Task 2) so the longer token is never half-rewritten; documentation-IT rename uses the full `RestControllerDocumentationIT` token (Task 3), which does not collide with the retained `RestController`/`RestControllerIT` names. Verification greps confirm zero residual tokens after each task.
