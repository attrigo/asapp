# Drop "Rest" from API-contract class names — design spec

**Date**: 2026-06-29
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.4.0 → Quick Wins → Technical Improvements → Other → "Remove "Rest" word in non-involved Rest classes "*RestAPI" classes by "*API" and "*RestAPIURL" by "*APIURL""
**Services affected**: `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service`, `asapp-commons-url`, `asapp-http-clients`

## 1. Context

The inbound web tier carries the `Rest` token on three kinds of class:

- the API **contract** interfaces (`*RestAPI`) that declare the endpoints and OpenAPI annotations,
- the shared URL **constant** classes (`*RestAPIURL`) in `asapp-commons-url`,
- the `@RestController` **adapters** (`*RestController`) that implement the interfaces.

On the contract interface and the URL constants, `Rest` is redundant: the interface is the API *contract* (what the service exposes), and the constants are just endpoint paths. Neither is REST-the-protocol — that concern belongs to the adapter. This spec removes `Rest` from the contract interfaces and the URL constants, and from the one test whose product is the contract itself, while keeping `Rest` everywhere REST/HTTP is genuinely the subject.

This is a pure rename + reference-update refactor. No behaviour, no HTTP contract, no endpoint path, and no constant *value* changes.

## 2. Goals

- Rename `*RestAPI` → `*API` and `*RestAPIURL` → `*APIURL`, updating every reference so the build stays green.
- Rename the contract-documentation tests `*RestControllerDocumentationIT` → `*APIDocumentationIT` and repoint them at the `*API` interface (see §6 for the rationale).
- Scrub `REST` from the prose *inside* the renamed contract interfaces (Javadoc summary + `@Tag` description).
- Keep the project's AI-instruction surfaces (`.claude/rules/rest.md`, `.claude/agents/code-reviewer.md`) consistent so the path-scoped rule keeps matching after the rename.

## 3. Non-goals

- **No behaviour, contract, or path change.** Endpoint URLs, HTTP verbs, status codes, request/response payloads, validation, and security are all untouched. Constant *names* (`TASKS_ROOT_PATH`, …) and *values* are unchanged — only the class that holds them is renamed.
- **No rename of REST-adapter classes.** `*RestController`, `*RestControllerIT`, `*RestControllerTests` keep `Rest` — their subject is the HTTP adapter (see §6).
- **No rename of REST tooling classes.** `RestDocsWebMvcTestContext`, `RestDocsConstrainedFields` (Spring **REST Docs**), and `RestClientConfiguration` (`RestClient`) keep `Rest` — it names the actual library/tool.
- **No package restructuring.** The contract interface, its adapter, and their tests stay co-located in `infrastructure/<aggregate>/in/` per `architecture.md`. No new `api/` sub-package is introduced.
- **No edit to historical specs** under `docs/superpowers/specs/`. They are point-in-time records (already on the pre-rename `com.bcn` package) and stay frozen.
- **No README prose change.** "REST API endpoint constants" in `asapp-commons-url/README.md` is accurate English for a library of REST endpoint paths; only class-name references are updated.

## 4. Renames — 12 files via `git mv`

All renames use `git mv` (per `CLAUDE.md`), never delete-and-recreate.

### 4.1 API contract interfaces — `*RestAPI` → `*API`

| From | To |
|---|---|
| `services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskRestAPI.java` | `TaskAPI.java` |
| `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationRestAPI.java` | `AuthenticationAPI.java` |
| `services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserRestAPI.java` | `UserAPI.java` |
| `services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/user/in/UserRestAPI.java` | `UserAPI.java` |

### 4.2 URL constant classes — `*RestAPIURL` → `*APIURL`

| From | To |
|---|---|
| `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/tasks/TaskRestAPIURL.java` | `TaskAPIURL.java` |
| `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/authentication/AuthenticationRestAPIURL.java` | `AuthenticationAPIURL.java` |
| `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/authentication/UserRestAPIURL.java` | `UserAPIURL.java` |
| `libs/asapp-commons-url/src/main/java/com/attrigo/asapp/url/users/UserRestAPIURL.java` | `UserAPIURL.java` |

### 4.3 Contract-documentation tests — `*RestControllerDocumentationIT` → `*APIDocumentationIT`

| From | To |
|---|---|
| `services/asapp-tasks-service/src/test/java/com/attrigo/asapp/tasks/infrastructure/task/in/TaskRestControllerDocumentationIT.java` | `TaskAPIDocumentationIT.java` |
| `services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/authentication/in/AuthenticationRestControllerDocumentationIT.java` | `AuthenticationAPIDocumentationIT.java` |
| `services/asapp-authentication-service/src/test/java/com/attrigo/asapp/authentication/infrastructure/user/in/UserRestControllerDocumentationIT.java` | `UserAPIDocumentationIT.java` |
| `services/asapp-users-service/src/test/java/com/attrigo/asapp/users/infrastructure/user/in/UserRestControllerDocumentationIT.java` | `UserAPIDocumentationIT.java` |

> Two classes share the simple name `UserAPI` (auth + users services) and two share `UserAPIURL` (commons-url `authentication` + `users` packages). They live in distinct packages, so there is no collision — same as before the rename.

## 5. Edits inside the renamed files

- **API interfaces:** rename the interface declaration; scrub the two `REST` prose hits:
  - Javadoc summary `* REST API contract for <X> operations.` → `* API contract for <X> operations.`
  - `@Tag(... description = "REST API contract for <X>")` → `"API contract for <X>"` (the `@Tag` *name* is unchanged).
- **URL classes:** rename the class declaration and the private constructor.
- **Documentation ITs:** rename the class declaration; repoint the Javadoc `Tests {@link <X>RestController> …}` to `{@link <X>API>}` (the interface is in the same package — no import needed) and drop `REST` from the summary (e.g. `Tests {@link TaskAPI} documentation.`). The coverage list ("Generates API documentation snippets …") is already `Rest`-free and stays.

## 6. Naming rationale — what keeps "Rest" and why

The dividing line is **subject**, not mechanism: a class keeps `Rest` when REST/HTTP is what it *is about*, and drops it when REST is merely how it is delivered.

| Class | Keeps / drops | Reason |
|---|---|---|
| `*RestAPI` → `*API` | drops | The transport-agnostic API *contract*. OpenAPI annotations live here, but the contract (operations, params, payloads) is not REST-specific. |
| `*RestAPIURL` → `*APIURL` | drops | Endpoint path constants — data, not a REST mechanism. |
| `*RestControllerDocumentationIT` → `*APIDocumentationIT` | drops | Its **product** is the contract documentation (`api-guide.adoc` snippets: fields, params, status codes). MockMvc is incidental; the subject is the `*API` contract — so it now mirrors the interface. |
| `*RestController` | keeps | The `@RestController` HTTP adapter. REST *is* its subject. |
| `*RestControllerIT` | keeps | Verifies HTTP-stack behaviour that has no meaning in the abstract contract: 415 content-type negotiation, `Failed to convert` path-variable binding, `Failed to read request`, `No static resource` routing fallback, the `instance` request-URI, and validation → RFC 7807 `ProblemDetail` translation. Subject = the REST adapter. |
| `*RestControllerTests` | keeps | Unit-tests the controller (delegation to use cases, mapping). Subject = the REST adapter. |
| `RestDocsWebMvcTestContext`, `RestDocsConstrainedFields` | keep | Named after the Spring **REST Docs** tool they wrap (a proper noun). |
| `RestClientConfiguration` (+ IT) | keeps | Named after the `RestClient` it configures. |

## 7. Reference updates (no rename — update class name / import only)

The authoritative list is every file matching `RestAPI` or `RestAPIURL` outside the renamed files themselves and the excluded docs (§3). Grouped:

- **Controllers (`implements *API` + import):** `TaskRestController`, `AuthenticationRestController`, `UserRestController` (auth), `UserRestController` (users).
- **Tests importing the interfaces and/or URL constants:**
  - tasks: `TaskRestControllerIT`, `TaskRestControllerTests`, `TaskE2EIT`
  - authentication: `AuthenticationRestControllerIT`, `UserRestControllerIT`, `AuthenticationE2EIT`, `UserE2EIT`, `PasswordEncoderIT`
  - users: `UserRestControllerIT`, `UserRestControllerTests`, `UserE2EIT`, `TasksGatewayAdapterIT`, `RestClientConfigurationIT`, `ResilienceConfigurationIT`
- **`asapp-http-clients`:** `TasksHttpClient`, `TasksHttpClientTests` (static imports of `TaskRestAPIURL` constants).
- **Documentation:** `libs/asapp-commons-url/README.md` — import snippets (lines ~49, ~55) and the Reference list (lines ~91–93); class-name references only.
- **AI-instruction surfaces (permission-gated — the developer approves these edits):**
  - `.claude/rules/rest.md` — frontmatter glob `**/infrastructure/**/*RestAPI.java` → `*API.java`; the line-39 prose `*RestAPI.java` → `*API.java`; section heading "REST API Interface Pattern" → "API Interface Pattern".
  - `.claude/agents/code-reviewer.md` — the `rest.md` path glob `*RestAPI.java` → `*API.java`.

## 8. Verification & build policy

Per the project's Maven policy, commands that execute integration/E2E tests are slow and **must not** be run autonomously — stop and ask the developer first.

- **Autonomous:** `mvn spotless:apply` (formatting) and a compile + unit-test build with ITs/E2EITs skipped (e.g. `-DskipITs`). This proves the rename compiles and unit tests pass.
- **Requires the developer's go-ahead:** any IT/E2EIT run — `mvn clean verify` or `mvn clean install` (the `install` phase runs Failsafe). Many of the updated files are ITs/E2EITs, so a full green build needs this; request it explicitly before running.

## 9. Risks

- **`*API.java` glob over-match.** After the rename the only `*API.java` files under `infrastructure/**` are the four renamed interfaces, so the updated `.claude/rules/rest.md` glob matches exactly them (verified: no other `…API.java` class exists in those packages).
- **Missed reference.** Mitigated by a final repo-wide search for `RestAPI`/`RestAPIURL` after the edits — only `TODO.md` (the task entry) and the frozen historical specs should remain.
- **Stale generated REST Docs snippets.** Snippet IDs (`get-task-by-id`, …) are string literals inside the doc tests, not derived from class names, so renaming the test class does not change `api-guide.adoc` output.

## 10. Post-implementation notes

This spec and its plan (`docs/superpowers/plans/2026-06-29-drop-rest-from-api-class-names.md`) were written before implementation. The core change shipped substantially as designed — the `*RestAPI` interfaces, `*RestAPIURL` constant classes, and `*RestControllerDocumentationIT` tests were renamed to drop the redundant `Rest` token, every reference was updated, and the `Rest`-keeps list in §6 was honoured. As built, the canonical implementation is the current state of the renamed classes, the `asapp-commons-url` README, the three services' `api-guide.adoc` files, the `*API` interface Javadoc, and `.claude/rules/rest.md` + `.claude/agents/code-reviewer.md` on this branch — not this document.

A post-implementation review pass extended the `Rest`-scrub into prose that the original spec had deliberately left untouched. **Notable deltas:**

- **README prose was rewritten (reverses §3 "No README prose change").** §3 kept "REST API endpoint constants" in `libs/asapp-commons-url/README.md` as accurate English, planning to change only class-name references. On review, the redundant `Rest` token read inconsistently against the new `*APIURL` class names, so the tagline and Overview prose were changed to "Centralized endpoint URL constants" — aligning the README's wording with the renamed classes it documents.
- **`api-guide.adoc` overview lines were scrubbed (not covered by the spec).** The spec only addressed the AsciiDoc guides in §9 (confirming snippet IDs are unaffected). Each service's `src/docs/asciidoc/api-guide.adoc` Overview line was changed from "REST API documentation for the ASAPP <X> Service." to "API contract documentation for the ASAPP <X> Service.", carrying the contract-vs-adapter framing of §6 through to the published guide prose.
- **An inbound-HTTP-entry-point note was added to the interfaces (beyond §5's prose scrub).** §5 only removed the two `REST` prose hits from each interface's Javadoc summary and `@Tag`. Each of the four `*API` interfaces (`TaskAPI`, `AuthenticationAPI`, and both `UserAPI`) additionally gained the Javadoc line "Serves as the service's inbound HTTP entry point, implemented by the controller.", clarifying the interface's role now that its name no longer carries `Rest`.

For future API-naming or doc edits, treat the renamed classes, the README, the `api-guide.adoc` guides, and the `*API` interface Javadoc on this branch as the template; this spec is preserved as a record of the original design intent.
