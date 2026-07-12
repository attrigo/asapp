# Rename `asapp-rest-clients` → `asapp-http-clients` — Design

**Date:** 2026-06-26
**Status:** Implemented
**Targets:** `libs/asapp-http-clients` (module dir + `pom.xml`), `libs/pom.xml`, `services/pom.xml`, `asapp-users-service/pom.xml`, docs (`README.md` ×4, `CLAUDE.md`), `TODO.md`

---

## 1. Context

`TODO.md` (Version 0.4.0 → Quick Wins → Technical Improvements → Improve HTTP
clients) drives this work:

> Rename library asapp-rest-clients to asapp-http-clients

The library was created as `asapp-rest-clients` when the clients were plain
`RestClient` calls. They were since refactored into declarative `@HttpExchange`
interfaces (commit-series under the "Improve HTTP clients" item), so the "rest"
in the name no longer reflects what the library is — a set of protocol-agnostic
HTTP client contracts and DTOs. This task aligns the name with that reality.

Relevant state today:

- The library holds three files, all under package `com.bcn.asapp.clients`:
  `TasksHttpClient`, `TasksByUserIdResponse`, and `TasksHttpClientTests`.
- The package name (`...clients`) contains no "rest" token — the only "rest"
  references are the Maven coordinate (`artifactId`/`<name>`/`<description>`) and
  prose in docs.
- The single consumer is `asapp-users-service`, which declares the dependency by
  artifact coordinate; it imports the client by its Java package, not its
  artifact name.
- No CI workflow, `docker-compose`, or application-properties file references the
  artifact name.

## 2. Scope — change set (in order)

1. **Move the module directory** — `git mv libs/asapp-rest-clients libs/asapp-http-clients`.
   Preserves Git history; the `src/.../com/bcn/asapp/clients/` tree underneath is
   unchanged.
2. **Module `pom.xml`** — `artifactId` and `<name>` → `asapp-http-clients`;
   `<description>` "ASAPP REST clients library" → "ASAPP HTTP clients library".
3. **`libs/pom.xml`** — `<module>asapp-rest-clients</module>` → `asapp-http-clients`.
4. **`services/pom.xml`** — `dependencyManagement` `<artifactId>` → `asapp-http-clients`.
5. **`asapp-users-service/pom.xml`** — dependency `<artifactId>` → `asapp-http-clients`.
6. **Docs prose** — swap the name/description references in: root `README.md`,
   `CLAUDE.md`, the library's `README.md`, `asapp-commons-url/README.md`, and
   `asapp-users-service/README.md`.
7. **`TODO.md`** — check the box on the rename line.

## 3. Out of scope

- **Java package `com.bcn.asapp.clients`** — kept as-is (contains no "rest"; renaming
  would be churn with no naming gain). Decided during brainstorming.
- **`RestClientConfiguration` / `RestClientConfigurationIT`** in `asapp-users-service`
  — these name Spring's `org.springframework.web.client.RestClient` HTTP
  infrastructure, not the library; unrelated to this rename.
- **Dated design specs under `docs/superpowers/specs/`** — historical records,
  left untouched.

## 4. Verification

`mvn clean install` from the repo root: Maven must resolve the new coordinate at
every reference point (module reactor, dependencyManagement, consumer dependency)
and the existing build/tests must pass. A green build is the end-to-end proof the
rename is wired correctly. A repo-wide search for `asapp-rest-clients` must return
zero hits outside the historical specs.

## 5. Post-implementation notes

This spec was written before implementation; no separate plan file was produced for this small, well-scoped rename. The core change shipped substantially as designed — the module directory, the Maven coordinate, every reactor/dependency reference, the docs prose, and the `TODO.md` checkbox were all renamed `asapp-rest-clients` → `asapp-http-clients` exactly as specified, and the build is green with zero `asapp-rest-clients` references outside historical specs.

As always, the canonical implementation is the current state of the renamed `libs/asapp-http-clients` module and its `asapp-users-service` consumer on this branch, not this document.

Notable deltas:

- **Java package renamed `com.bcn.asapp.clients` → `com.bcn.asapp.http.clients` (reverses § 3 "Out of scope").** The original design kept the package as-is, judging a rename to be pure churn. A post-implementation review flagged the asymmetry between the new module name (`asapp-http-clients`) and the unchanged package (`...clients`), and the decision was reversed so the package matches the module. This touched the library sources (`TasksHttpClient`, `TasksByUserIdResponse`, `TasksHttpClientTests`), the `asapp-users-service` consumers (`TasksHttpClientConfiguration`, `TasksGatewayAdapter`, `RestClientConfigurationIT`, `TasksGatewayAdapterTests`), and the library `README.md`.

For future client-library edits, treat the `com.bcn.asapp.http.clients` sources and the consumer imports as the template; this spec is preserved as a record of the original design intent.
