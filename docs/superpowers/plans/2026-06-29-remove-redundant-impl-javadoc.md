# Remove Redundant Implementation Javadoc — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Javadoc from overriding implementation methods when the implemented port/interface already documents the same contract, so each contract is documented once, on its declaration.

**Architecture:** A pure documentation cleanup. Java inherits a method's doc comment automatically when the override has no comment of its own, so deleting a redundant block changes nothing in generated Javadoc or IDE help. No production behavior changes; no test changes. Work is split one commit per service module so each can be reviewed independently.

**Tech Stack:** Java 25, Spring Boot 4, Maven (Spotless formatter via `asapp_formatter.xml`), pre-commit hooks (Spotless check, LF line-ending check, Conventional Commit check).

**Spec:** `docs/superpowers/specs/2026-06-29-remove-redundant-impl-javadoc-design.md`

## Global Constraints

- **Criterion (redundant-subset):** Remove an `@Override` method's Javadoc when the implemented port/interface method's Javadoc conveys *everything* the implementation's Javadoc says (interface ⊇ implementation) — this covers both byte-identical copies and shorter subsets.
- **Keep-and-extend safety valve:** If the implementation's Javadoc documents behavior the port does **not** (extra `@throws`, caching, thread-safety, impl-specific notes), do **not** delete it. Keep it, reduce the duplicated contract prose to a single `{@inheritDoc}`, and retain only the extra note. Expected to be rare — call out every such case in the commit body.
- **Never touch:** class-level Javadoc, constructor Javadoc (constructors are not overrides), or interface/port Javadoc. Only overriding *method* Javadoc is in scope.
- **`@Override` always stays.** Only the `/** … */` block above it is removed.
- **Out of scope files** (overrides of framework/library types or `Object`) are listed per spec and must be left untouched: `GlobalExceptionHandler`, `JwtAuthenticationFilter`, `JwtAuthenticationToken`, `JwtAuthenticationEntryPoint`, `HttpBasicAuthenticationEntryPoint`, `CustomUserDetailsService`, `ValidRoleValidator`, `JwtInterceptor`, `JdbcConversionsConfiguration`, and the `domain` entities' `equals`/`hashCode`.
- **Formatting:** after edits, run `mvn spotless:apply` to collapse any blank-line artifacts left by removal; staged files must use LF endings (hook-enforced).
- **Commits:** Conventional Commits with a bulleted body; end the message with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer (accepted by the hook, see the spec commit).
- **No version, dependency, or behavior changes anywhere.**

## Per-method procedure (applies to every file in Tasks 2–4)

For each `@Override` method in a listed file:
1. Open the implemented port/interface named in the task's table and locate the same method.
2. Compare the implementation method's Javadoc against the port method's Javadoc.
3. **Port covers everything the impl says →** delete the entire `/** … */` block above the method; leave exactly one blank line before `@Override`.
4. **Impl says strictly more →** apply the keep-and-extend valve (`{@inheritDoc}` + the extra note only).
5. Leave class-level and constructor Javadoc alone.

## File Structure

29 implementation files across three service modules are touched (method-Javadoc removal only), plus one rule file:

- `.claude/rules/code-style.md` — record the convention (Task 1).
- `asapp-tasks-service` — 6 files (Task 2).
- `asapp-users-service` — 7 files (Task 3).
- `asapp-authentication-service` — 16 files (Task 4).

Tasks 2–4 are mutually independent: each touches only its own module's classes and ports, and can be reviewed and merged on its own.

---

### Task 1: Record the convention in code-style.md

**Files:**
- Modify: `.claude/rules/code-style.md` (the `## Javadoc` section)

**Interfaces:**
- Consumes: nothing.
- Produces: the project rule that Tasks 2–4 apply. No code dependency.

> ⚠️ **Permission gate:** Writes under `.claude/` are blocked by the auto-mode classifier. When this edit is denied, **ask the developer to approve it** — do not work around the gate.

- [ ] **Step 1: Add the rule bullet**

In `.claude/rules/code-style.md`, the `## Javadoc` section currently reads:

```markdown
## Javadoc

- `@since` is mandatory on all production public classes and interfaces — use the module's current POM version (e.g., `@since 0.2.0`)
- `@see` ONLY for framework/library classes (Spring, MapStruct)
- Summary line must start with a verb: "Stores…", "Validates…", "Handles…"
```

Append one bullet after the last item:

```markdown
- Don't repeat an interface or superclass method's Javadoc on an overriding implementation — omit the comment and let Javadoc inherit it. Add a comment only when the implementation documents behavior beyond the contract, and then use `{@inheritDoc}` to inherit and extend.
```

- [ ] **Step 2: Commit**

```bash
git add .claude/rules/code-style.md
git commit -m "docs(rules): omit Javadoc that duplicates an inherited contract

- Add a code-style rule: don't restate an interface or superclass method's
  Javadoc on an overriding implementation — let Javadoc inherit it, and use
  {@inheritDoc} only to extend the contract with impl-specific behavior
- Codifies the convention applied by the implementation-Javadoc cleanup so new
  code does not reintroduce the duplication

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

Expected: hook prints "Commit message meets Conventional Commit standards"; commit succeeds.

---

### Task 2: asapp-tasks-service

**Files** (remove redundant `@Override` method Javadoc per the criterion; the right column is the port whose Javadoc to compare against, and the candidate-method count):

- Modify: `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/application/task/in/service/CreateTaskService.java` → port `CreateTaskUseCase` (1)
- Modify: `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/application/task/in/service/DeleteTaskService.java` → port `DeleteTaskUseCase` (1)
- Modify: `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/application/task/in/service/ReadTaskService.java` → port `ReadTaskUseCase` (4)
- Modify: `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/application/task/in/service/UpdateTaskService.java` → port `UpdateTaskUseCase` (1)
- Modify: `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/out/TaskRepositoryAdapter.java` → port `TaskRepository` (6)
- Modify: `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskRestController.java` → interface `TaskApi` (6)

**Interfaces:**
- Consumes: the Task 1 rule (convention only).
- Produces: nothing other tasks depend on.

**Note on `TaskRestController`:** every controller method's Javadoc is a strict subset of the richer `TaskApi` Javadoc (which carries the `@Operation` description + response-code list) → all are removable. While doing so, note in the commit body that this resolves the pre-existing `createTask` `@return` drift ("information" on the controller vs "identifier" on `TaskApi`); the interface text is authoritative.

- [ ] **Step 1: Remove redundant method Javadoc**

For each of the 6 files above, apply the per-method procedure. Ports live at:
- `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/application/task/in/*UseCase.java`
- `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/application/task/out/TaskRepository.java`
- `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskApi.java`

Leave class-level and constructor Javadoc intact.

- [ ] **Step 2: Normalize formatting**

```bash
cd services/asapp-tasks-service && mvn spotless:apply
```
Expected: BUILD SUCCESS; `git diff` shows only Javadoc-block deletions (and at most blank-line collapses), no code-line changes.

- [ ] **Step 3: Verify it still compiles**

```bash
cd services/asapp-tasks-service && mvn -o spotless:check compile
```
Expected: BUILD SUCCESS. (Comment removal cannot break Javadoc generation — inheritance resolves the removed comments — so no test or `javadoc:javadoc` run is required for a behavior-neutral doc change. Run `mvn -pl services/asapp-tasks-service javadoc:javadoc` only if extra confidence is wanted.)

- [ ] **Step 4: Review the diff**

```bash
git --no-pager diff
```
Expected: only `/** … */` blocks above `@Override` methods removed in the 6 listed files; no out-of-scope file touched; `@Override` annotations all present.

- [ ] **Step 5: Commit**

```bash
git add services/asapp-tasks-service
git commit -m "docs(tasks): drop implementation Javadoc inherited from ports

- Remove @Override method Javadoc on the task use-case services, the
  TaskRepository adapter, and TaskRestController where the implemented port or
  TaskApi interface already documents the same contract; Java inherits it
- Resolve the createTask @return drift by deleting the stale controller copy and
  deferring to the authoritative TaskApi description
- No behavior, signature, or interface-Javadoc changes

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: asapp-users-service

**Files:**

- Modify: `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/application/user/in/service/CreateUserService.java` → port `CreateUserUseCase` (1)
- Modify: `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/application/user/in/service/DeleteUserService.java` → port `DeleteUserUseCase` (1)
- Modify: `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/application/user/in/service/ReadUserService.java` → port `ReadUserUseCase` (3)
- Modify: `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/application/user/in/service/UpdateUserService.java` → port `UpdateUserUseCase` (1)
- Modify: `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/out/UserRepositoryAdapter.java` → port `UserRepository` (5)
- Modify: `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/out/TasksGatewayAdapter.java` → port `TasksGateway` (1)
- Modify: `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/UserRestController.java` → interface `UserApi` (5)

**Interfaces:**
- Consumes: the Task 1 rule. Independent of Tasks 2 and 4.
- Produces: nothing other tasks depend on.

- [ ] **Step 1: Remove redundant method Javadoc**

Apply the per-method procedure to the 7 files. Ports live under:
- `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/application/user/in/*UseCase.java`
- `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/application/user/out/{UserRepository,TasksGateway}.java`
- `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/UserApi.java`

Leave class-level and constructor Javadoc intact.

- [ ] **Step 2: Normalize formatting**

```bash
cd services/asapp-users-service && mvn spotless:apply
```
Expected: BUILD SUCCESS; diff shows only Javadoc deletions.

- [ ] **Step 3: Verify it still compiles**

```bash
cd services/asapp-users-service && mvn -o spotless:check compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Review the diff**

```bash
git --no-pager diff
```
Expected: only the 7 listed files changed; only override-method Javadoc removed; no out-of-scope file touched.

- [ ] **Step 5: Commit**

```bash
git add services/asapp-users-service
git commit -m "docs(users): drop implementation Javadoc inherited from ports

- Remove @Override method Javadoc on the user use-case services, the
  UserRepository and TasksGateway adapters, and UserRestController where the
  implemented port or UserApi interface already documents the same contract
- No behavior, signature, or interface-Javadoc changes

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: asapp-authentication-service

**Files:**

- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/user/in/service/CreateUserService.java` → port `CreateUserUseCase` (1)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/user/in/service/DeleteUserService.java` → port `DeleteUserUseCase` (1)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/user/in/service/ReadUserService.java` → port `ReadUserUseCase` (2)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/user/in/service/UpdateUserService.java` → port `UpdateUserUseCase` (1)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/in/service/AuthenticateService.java` → port `AuthenticateUseCase` (1)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/in/service/RefreshAuthenticationService.java` → port `RefreshAuthenticationUseCase` (1)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/application/authentication/in/service/RevokeAuthenticationService.java` → port `RevokeAuthenticationUseCase` (1)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/user/out/UserRepositoryAdapter.java` → port `UserRepository` (4)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/user/out/PasswordServiceAdapter.java` → port `PasswordService` (1)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/out/TokenVerifierAdapter.java` → port `TokenVerifier` (2)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/out/RedisJwtStore.java` → port `TokenStore` (4)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/out/JwtAuthenticationRepositoryAdapter.java` → port `JwtAuthenticationRepository` (7)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/out/CredentialsAuthenticatorAdapter.java` → port `CredentialsAuthenticator` (1)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/security/JwtIssuer.java` → port `TokenIssuer` (2)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserRestController.java` → interface `UserApi` (5)
- Modify: `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationRestController.java` → interface `AuthenticationApi` (3)

**Interfaces:**
- Consumes: the Task 1 rule. Independent of Tasks 2 and 3.
- Produces: nothing.

> **Watch the `security/` package:** `JwtIssuer` (implements the project port `TokenIssuer`) **is** in scope and lives in `infrastructure/security/`, not in an `out/` package — don't miss it. The neighbouring `CustomUserDetailsService` (implements Spring's `UserDetailsService`) is **out of scope** — leave it.

- [ ] **Step 1: Remove redundant method Javadoc**

Apply the per-method procedure to the 16 files. Ports live under:
- `…/application/user/in/*UseCase.java`, `…/application/authentication/in/*UseCase.java`
- `…/application/user/out/{UserRepository,PasswordService}.java`
- `…/application/authentication/out/{TokenVerifier,TokenStore,JwtAuthenticationRepository,CredentialsAuthenticator,TokenIssuer}.java`
- `…/infrastructure/user/in/UserApi.java`, `…/infrastructure/authentication/in/AuthenticationApi.java`

Leave class-level and constructor Javadoc intact.

- [ ] **Step 2: Normalize formatting**

```bash
cd services/asapp-authentication-service && mvn spotless:apply
```
Expected: BUILD SUCCESS; diff shows only Javadoc deletions.

- [ ] **Step 3: Verify it still compiles**

```bash
cd services/asapp-authentication-service && mvn -o spotless:check compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Review the diff**

```bash
git --no-pager diff
```
Expected: only the 16 listed files changed; only override-method Javadoc removed; `JwtIssuer` included; `CustomUserDetailsService` and all other framework-override files untouched.

- [ ] **Step 5: Commit**

```bash
git add services/asapp-authentication-service
git commit -m "docs(authentication): drop implementation Javadoc inherited from ports

- Remove @Override method Javadoc on the user and authentication use-case
  services, the persistence/token/credential adapters, the JwtIssuer token-issuer
  implementation, and both REST controllers where the implemented port or *Api
  interface already documents the same contract
- Leave framework-override classes (filters, entry points, UserDetailsService,
  exception handler) and class/constructor Javadoc untouched
- No behavior, signature, or interface-Javadoc changes

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Completion / handoff

After Tasks 1–4, the implementation is done. The remaining steps follow the developer's per-task workflow and are **not** part of this plan: run `review-task`, then `resolve-review-issues` for any findings, then `close-task` (which integrates the branch and ticks the TODO.md entry). Marking the spec `Implemented` is handled at close time per project precedent.

## Self-Review

- **Spec coverage:** criterion (Global Constraints + procedure); scope in/out (Global Constraints + each task's file list + watch-notes); removal mechanism (procedure Step 3, no bare `{@inheritDoc}`); controller drift fix (Task 2 note + commit); rule in code-style.md (Task 1); verification (each task Steps 2–4). All spec sections map to a task.
- **Placeholder scan:** none — every step has concrete files, commands, expected output, and full commit messages.
- **Type/path consistency:** every port path matches the project package layout (`application/<aggregate>/in|out`, `infrastructure/<aggregate>/in`); candidate counts match the repository grep; `JwtIssuer`'s `security/` location is flagged explicitly.
