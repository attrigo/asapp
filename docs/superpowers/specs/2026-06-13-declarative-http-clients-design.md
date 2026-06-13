# Refactor REST clients → Declarative HTTP clients — Design

**Date:** 2026-06-13
**Status:** Approved (pre-implementation)
**Targets:** `asapp-rest-clients`, `asapp-users-service`

---

## 1. Context

`TODO.md` (Version 0.4.0 → Quick Wins → Technical Improvements → Improve HTTP
clients) drives this work:

> Refactor REST clients by declarative HTTP clients

Today, inter-service HTTP is hand-written. The `asapp-rest-clients` library
ships:

- `TasksClient` — interface returning `List<UUID>`.
- `TasksRestClient` — manual `RestClient` implementation that builds the URI
  via `UriHandler`/`DefaultUriHandler`, maps `TasksByUserIdResponse → UUID`,
  and swallows `RestClientException` into an empty list (graceful degradation).
- `TasksClientConfiguration` — auto-configuration gated on
  `asapp.client.tasks.base-url`.

The single consumer, `asapp-users-service`, supplies a `@LoadBalanced`
`RestClient` bean carrying the `JwtInterceptor` and a redirect-disabled JDK
request factory, and bridges to the domain through `TasksGatewayAdapter`
(implements the `TasksGateway` port).

Spring Boot 4.0 / Spring Framework 7 make HTTP clients declarative: an
`@HttpExchange` interface becomes the client proxy, registered via
`@ImportHttpServices` and configured per *group*. This removes the hand-written
implementation, the URI-building utilities, and the bespoke auto-configuration.

**Only one client exists** (users-service → tasks-service), so the refactor is
contained, but the design generalizes to future clients and to the next two
TODO items (circuit breaker, retry).

---

## 2. Decisions (from brainstorming)

1. **`asapp-rest-clients` becomes a pure-contract library.** It keeps only the
   `@HttpExchange` interface(s) and response DTOs. Registration, group
   configuration (base-url, JWT, load-balancing), and DTO→domain mapping move to
   the consuming service.
2. **Graceful degradation stays, in the adapter.** `TasksGatewayAdapter` wraps
   the declarative call in `try/catch (RestClientException)` → log warning →
   return empty list. This preserves exact current behavior. The upcoming
   circuit-breaker task can later refactor this into a resilience4j fallback.
3. **Interface name `TasksHttpClient`, group name `tasks`** (replacing
   `TasksClient` + `TasksRestClient`).

---

## 3. Detailed Design

### 3.1 Library — declarative interface (new)

`libs/asapp-rest-clients/.../clients/tasks/TasksHttpClient.java`

```java
@HttpExchange
public interface TasksHttpClient {

    @GetExchange(TASKS_GET_BY_USER_ID_FULL_PATH)   // "/api/tasks/user/{id}"
    List<TasksByUserIdResponse> getTasksByUserId(@PathVariable("id") UUID id);
}
```

- Imports: `org.springframework.web.service.annotation.{HttpExchange,GetExchange}`,
  `org.springframework.web.bind.annotation.PathVariable`.
- Returns the **raw DTO list** — mapping to `UUID` moves to the adapter.
- `TasksByUserIdResponse` is unchanged and stays in the library.

### 3.2 Library — deletions

- `TasksClient.java`, `TasksRestClient.java`, `TasksClientConfiguration.java`
- `util/UriHandler.java`, `util/DefaultUriHandler.java`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Tests: `TasksRestClientTests.java`, `util/DefaultUriHandlerTests.java`

> **File ops:** use `git mv TasksClient.java TasksHttpClient.java` to seed the
> new interface (preserves history; contents are then rewritten). The remaining
> files are plain deletions.

### 3.3 Library — `pom.xml` slimming

Drop now-unused optional deps: `spring-context`, `spring-boot-autoconfigure`,
`slf4j-api` (logging moved to the consumer). Keep `spring-web` (provides
`@HttpExchange`) and `jackson-annotations` (DTO). Test deps unchanged
(`spring-boot-starter-test`, `jackson-databind`) — still needed for the contract
test in §6.

### 3.4 Consumer — group registration & configuration

`asapp-users-service` `RestClientConfiguration` evolves (keeps its name — it
still configures RestClient-backed infrastructure):

- **Add** `@ImportHttpServices(group = "tasks", types = TasksHttpClient.class)`.
- **Keep** the `@Primary` plain `RestClient.Builder` and the isolated
  `DefaultEurekaClientHttpRequestFactorySupplier` (Eureka needs both).
- **Remove** the `@LoadBalanced RestClient.Builder` and the standalone
  `restClient` bean — only the deleted library code consumed them.
- **Add** a `RestClientHttpServiceGroupConfigurer` that, for the `tasks` group,
  applies the redirect-disabled `JdkClientHttpRequestFactory` and the
  `JwtInterceptor`:

```java
@Bean
RestClientHttpServiceGroupConfigurer tasksHttpServiceGroupConfigurer() {
    return groups -> groups.forEachClient((group, clientBuilder) ->
        clientBuilder.requestFactory(redirectDisabledJdkFactory())
                     .requestInterceptor(new JwtInterceptor()));
}
```

Load-balancing wiring is detailed in §5.

### 3.5 Consumer — adapter absorbs mapping + fallback

`TasksGatewayAdapter` injects the `TasksHttpClient` proxy and takes over the
logic that left the library:

```java
@Override
public List<UUID> getTaskIdsByUserId(UserId userId) {
    try {
        var tasks = tasksHttpClient.getTasksByUserId(userId.value());
        if (tasks == null) {
            logger.warn("Null response from Tasks Service for user {}. Returning empty list.", userId.value());
            return List.of();
        }
        return tasks.stream().map(TasksByUserIdResponse::taskId).toList();
    } catch (RestClientException e) {
        logger.warn("Failed to retrieve tasks for user {}: {}. Returning empty list.", userId.value(), e.getMessage());
        return List.of();
    }
}
```

- The previous null-`userId` `IllegalArgumentException` guard is **dropped** —
  the `UserId` value object already guarantees a non-null value, so the
  type system enforces the precondition.

---

## 4. Config property migration

Rename `asapp.client.tasks.base-url` → **`spring.http.serviceclient.tasks.base-url`**
(Spring Boot 4's group property). Value unchanged.

| File | Change |
|---|---|
| `asapp-users-service/src/main/resources/application.properties` | key rename (`http://asapp-tasks-service/asapp-tasks-service`) |
| `asapp-users-service/src/main/resources/application-docker.properties` | key rename; still `=${ASAPP_CLIENT_TASKS_BASE_URL}` |
| `asapp-users-service/src/test/resources/application.properties` | key rename (`http://localhost:8081/asapp-tasks-service`) |

`docker-compose.yaml` is **unchanged** — the `ASAPP_CLIENT_TASKS_BASE_URL` env
var is arbitrary and keeps feeding the renamed property. `central-config` is
unaffected (the client base-url lives in the consumer's local properties).

---

## 5. Load-balancing + JWT wiring (the one technical risk)

Today, the `@LoadBalanced` builder gives three things: Eureka service-id
resolution, the `JwtInterceptor`, and the redirect-disabled factory. The latter
two are reattached through the group configurer (§3.4). Load-balancing is the
open question:

- **(a) Transparent support (target).** Spring Cloud 2025.1 advertises automatic
  load-balancing for HTTP service groups. If present on the classpath, the
  service-id host (`asapp-tasks-service`) resolves with no extra wiring.
- **(b) Manual fallback.** If the service-id host is not resolved by (a), apply
  the Spring Cloud load-balancer interceptor explicitly inside the group
  configurer, alongside `JwtInterceptor`.

**Plan:** target (a); a short spike during implementation confirms whether the
service-id resolves end-to-end. If not, fall back to (b). Either way JWT
propagation flows through the group configurer — our token propagation is custom
(re-emit the inbound bearer token), **not** OAuth, so Spring Security 7's
`@ClientRegistrationId` does not apply.

---

## 6. Testing strategy

- **Library — `TasksHttpClientTests` (new).** Contract test: bind
  `MockRestServiceServer` to a `RestClient.Builder`, build an
  `HttpServiceProxyFactory`, create the proxy, and verify the GET to
  `/api/tasks/user/{id}` plus JSON-array deserialization into
  `List<TasksByUserIdResponse>`. Replaces coverage lost by deleting
  `TasksRestClientTests`/`DefaultUriHandlerTests`.
- **Consumer — `TasksGatewayAdapterTests` (new).** Mockito unit test on the
  `TasksHttpClient` proxy: success mapping, empty list, null body → empty,
  `RestClientException` → empty.
- **Consumer — `RestClientConfigurationTests`.** Update for the new beans
  (group configurer present; `@LoadBalanced` builder / `restClient` bean gone).
- **Unchanged:** `JwtInterceptor` and `JwtInterceptorTests`; existing users→tasks
  integration / E2E tests should pass as-is (they exercise behavior, not the
  client mechanism).
- **Verify:** `mvn clean verify`. (ITs are slow — confirm with the developer
  before running per project convention.)

---

## 7. Documentation & Claude-file updates

**READMEs:**

- `libs/asapp-rest-clients/README.md` — significant rewrite: Overview & Key
  Features (drop "URI building utilities" / "Pre-configured REST clients" →
  declarative `@HttpExchange` contracts); Usage (base-url property rename;
  injection example switches to the declarative interface + `@ImportHttpServices`
  registration); Reference → Clients list; External Resources link (Spring
  RestClient → Spring HTTP Interfaces).
- `README.md` (root, line 303) — `asapp-rest-clients/` description tweak.
- `services/asapp-users-service/README.md` — line 204 (`Spring RestClient` →
  `Spring HTTP Interfaces (@HttpExchange)`); line 361 dependency wording.
- `CLAUDE.md` (root, line 7) — `asapp-rest-clients` description → declarative
  HTTP client interfaces + DTOs.

**Claude rule (needs developer approval to apply — `.claude/` is gated):**

- `.claude/rules/development-patterns.md` — rewrite "Service-to-Service Calls":
  calls go through an injected declarative `@HttpExchange` client proxy; JWT
  propagation stays automatic via the group configurer's interceptor.

**Not touched:** historical specs under `docs/superpowers/specs/`; `api-guide.adoc`
(server endpoints unchanged — client-side refactor only).

---

## 8. Sequencing

1. **Library:** `git mv` interface, write `@HttpExchange` contract, delete impl /
   config / util / imports, slim `pom.xml`, add `TasksHttpClientTests`.
2. **Consumer:** add `@ImportHttpServices` + group configurer, shrink
   `RestClientConfiguration`, rewrite `TasksGatewayAdapter`, rename property
   (all three envs), update `RestClientConfigurationTests`, add
   `TasksGatewayAdapterTests`.
3. **Spike + verify:** confirm load-balancing path (§5); run `mvn clean verify`.
4. **Docs:** READMEs + `CLAUDE.md`; prepare `.claude/rules` text for developer.
5. **TODO.md:** tick line 29 (`[ ]` → `[X]`).

---

## 9. Out of scope

- Circuit breaker (TODO line 30) and retry (TODO line 31) — separate tasks. The
  adapter-level fallback (§2.2) is the seam they will build on.
- New clients (auth, users) — none exist today.
- Changes to tasks-service endpoints.
- OAuth client registration (`@ClientRegistrationId`).

---

## 10. Acceptance criteria

- `TasksHttpClient` declarative interface lives in the library; no hand-written
  `RestClient` implementation, `UriHandler`, or bespoke auto-config remains.
- `asapp-users-service` registers the proxy, and JWT propagation + load
  balancing work end-to-end (integration/E2E green).
- Graceful degradation preserved: tasks-service down → empty list, user lookup
  still succeeds.
- Base-url property renamed across local, docker, and test configs.
- All READMEs + `CLAUDE.md` updated; `.claude/rules` change prepared for the
  developer.
- `mvn clean verify` is green.

---

## 11. Open questions for the plan

- Load-balancing integration — transparent (a) vs manual interceptor (b);
  resolved by the §5 spike.
- Exact `RestClientHttpServiceGroupConfigurer` API for scoping to the `tasks`
  group and adding the load-balancer interceptor — confirm against the running
  Spring Boot 4.0.5 / Spring Cloud 2025.1 versions during implementation.

---

## 12. Post-implementation notes

_(to be filled in after implementation)_
