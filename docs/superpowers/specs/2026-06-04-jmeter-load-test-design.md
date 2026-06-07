# JMeter load test — design spec

**Date**: 2026-06-04
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.4.0 → Quick Wins → Technical Improvements → "Add load test with JMeter"
**Scope**: New tooling under `tools/jmeter/`; no production code changes.

## 1. Context

Before cutting a release, the current pre-flight check is **manual**: bring up the whole stack with `docker-compose up -d` (services, PostgreSQL ×3, Redis, Prometheus, Grafana), then click through the main flows by hand while watching Grafana. This is slow, inconsistent, and not repeatable.

This spec replaces that manual pass with two automated [Apache JMeter](https://jmeter.apache.org/) test plans, run from the CLI against the running `docker-compose` stack:

- **A regression plan** — a single deterministic pass over every functional endpoint, with correctness assertions. The automated go/no-go gate that replaces the manual click-through.
- **A stress plan** — the same comprehensive read+write journey run by many concurrent threads (nested loops scale the data volume), observed live in Grafana (`:3000`).

Both run **outside** the Maven build and CI. They assume the stack is already up. Nothing here runs during `mvn verify`.

### 1.1 System under test (as wired by `docker-compose.yaml`)

Three functional services, each behind a **servlet context path** equal to its name, exposed on a host port:

| Service | Host base URL | Auth on `/api/**`? |
|---|---|---|
| authentication | `http://localhost:8080/asapp-authentication-service` | `/api/auth/**` and `POST /api/users` are public; everything else requires a Bearer token |
| tasks | `http://localhost:8081/asapp-tasks-service` | all `/api/**` require a Bearer token |
| users | `http://localhost:8082/asapp-users-service` | all `/api/**` require a Bearer token |

All three share the same `ASAPP_SECURITY_JWT_SECRET`, so **one access token issued by the authentication service is accepted by all three**. Access tokens live **5 minutes** (`ASAPP_SECURITY_ACCESS_TOKEN_EXPIRATION_TIME=300000`); refresh tokens live **1 hour**.

There is no seeded user (the Prometheus service account was removed in v0.3.0), and there is no cross-service referential integrity yet (v0.9.0). A plan therefore creates everything it needs and may use any UUID string as a task's `userId`.

## 2. Goals

- Automate the full pre-release regression pass as a single repeatable command that fails loudly on any functional regression.
- Generate tunable concurrent read/write load whose effects are observable in the existing Grafana/Prometheus stack.
- Keep the plans **self-cleaning** against a persistent database: the regression plan and any loop-bounded stress run leave nothing behind; a duration-bounded stress run may leave only its final in-flight pass per thread (uniquely named, so re-runs never collide).
- Require **zero manual tooling install** — the run scripts provision JMeter themselves.
- Stay consistent with repository conventions: live under `tools/`, documented with a README, results gitignored.

## 3. Non-goals

- **No CI integration.** These plans need the full stack up and are slow/noisy; they are a local pre-release tool, not a `mvn verify` step. (A separate backlog item covers Gatling.)
- **No `jmeter-maven-plugin` / Maven coupling.** Explicitly rejected during design in favour of a standalone CLI model.
- **No custom Grafana dashboard.** The provisioned dashboards plus the services' Micrometer/Prometheus metrics are sufficient for observation.
- **No distributed/remote JMeter** (single-node load generation only).
- **No shared cross-thread test data.** Each virtual user creates and cleans up only its own data (see §8.3).
- **No pass/fail SLA thresholds on the stress plan.** Stress is for observation; only the regression plan asserts correctness.
- **No bundling of the JMeter binary in git.** It is auto-downloaded to a gitignored directory.

## 4. Repository layout

```
tools/jmeter/
├── README.md                       # prerequisites, how to run, how to tune, where reports land
├── asapp-regression.jmx            # deterministic full-journey plan (§7)
├── asapp-stress.jmx                # deterministic nested-loop load plan (§8)
├── env/
│   └── local-docker.properties     # default hosts/ports + load knobs (§6)
├── lib/
│   └── jmeter-version.properties    # pinned JMeter version + SHA-512 (§5)
├── run-regression.sh
├── run-stress.sh
├── scripts/
│   └── ensure-jmeter.sh            # download/verify/unzip JMeter into .runtime/ (§5)
├── .runtime/                       # auto-downloaded JMeter (gitignored)
└── results/                        # .jtl logs + HTML dashboards (gitignored)
```

`tools/jmeter/` sits alongside the existing `tools/prometheus/` and `tools/grafana/` directories.

**Scripts are bash-only (`.sh`).** This matches the repository's only other hand-written scripts — the shell git hooks `git/hooks/pre-commit` and `git/hooks/commit-msg` — and its all-bash README. On Windows they run via Git for Windows' bundled `sh` (the same `sh` that already executes the git hooks). No PowerShell variants.

## 5. JMeter engine provisioning (auto-download)

**Decision: A — the run scripts download a pinned JMeter on first use.** No host install, no Maven, version pinned and reproducible.

- **Pinned version**: Apache JMeter **5.6.3** (current stable; runs on the project's Java 25).
- **Source**: `https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.zip`, verified against the published SHA-512. The expected URL and checksum live in `lib/jmeter-version.properties` so a version bump is a one-line change.
- **Target**: extracted to `tools/jmeter/.runtime/apache-jmeter-5.6.3/` (gitignored).
- **`scripts/ensure-jmeter.sh`** logic, idempotent:
  1. If `.runtime/apache-jmeter-5.6.3/bin/jmeter` exists → return its path.
  2. Else download the zip to a temp file, verify SHA-512 (abort on mismatch), unzip into `.runtime/` (via `unzip` if present, else the JDK's `jar xf` as a portable fallback since Java 25 is required anyway), delete the temp file, return the binary path.
- The `run-*` scripts call `ensure-jmeter` first, then invoke the returned binary. The same downloaded JMeter can be launched in **GUI mode** (`.runtime/apache-jmeter-5.6.3/bin/jmeter`) for authoring/debugging the `.jmx` files.
- **First run requires internet** (~no-op afterward; the binary is cached). This is documented in the README.

## 6. Common configuration

All tunables are JMeter **properties**, read in the `.jmx` via `${__P(name,default)}`. Defaults live in `env/local-docker.properties` (passed with `-q`); any value is overridable per run with `-J<name>=<value>`.

| Property | Default | Meaning |
|---|---|---|
| `auth.scheme` / `auth.host` / `auth.port` / `auth.context` | `http` / `localhost` / `8080` / `/asapp-authentication-service` | authentication service base |
| `tasks.scheme` / `tasks.host` / `tasks.port` / `tasks.context` | `http` / `localhost` / `8081` / `/asapp-tasks-service` | tasks service base |
| `users.scheme` / `users.host` / `users.port` / `users.context` | `http` / `localhost` / `8082` / `/asapp-users-service` | users service base |
| `threads` | `20` | concurrent virtual users (stress) |
| `rampup` | `20` | ramp-up seconds (stress) |
| `duration` | `300` | seconds the threads keep looping the journey (scheduler stop) |
| `loops` | `-1` | loop cap per thread; `-1` = infinite (run for the full `duration`); set ≥ 1 for a bounded, fully self-cleaning run |
| `users.per.pass.max` | `10` | owner users per pass = `__Random(1, this)` (the `x` loop) |
| `tasks.per.user.max` | `10` | tasks per owner user = `__Random(1, this)` (the `y` loop) |
| `password` | `L0adT3st!Pass` | password used for all load users (meets 8–64 char rule) |

**Shared elements** in both plans:

- **Property→variable bridge**: a **User Defined Variables** element maps each property to a variable once (e.g. `password=${__P(password,L0adT3st!Pass)}`, `auth.host=${__P(auth.host,localhost)}`, …) so the rest of the plan reads `${password}`, `${auth.host}`, etc.
- **Per-pass username**: the load user's username is generated *once per pass* into a `${username}` variable — a User Parameters pre-processor (update once per iteration) or a JSR223 PreProcessor doing `username = loadtest_${__UUID}@asapp.test` — and reused by both the register and token requests. (Inlining `${__UUID}` in each body would otherwise yield two different usernames and the token would fail.)
- **HTTP Header Manager**: `Content-Type: application/json`. The `Authorization: Bearer ${accessToken}` header is added only on the **protected** requests (everything except the public `POST /api/users` register and `/api/auth/{token,refresh,revoke}`), where `accessToken` is a thread-local variable.
- **HTTP Request Defaults** are *not* shared, because the three services have different host/port/context; each request resolves its base from the per-service properties above.
- Sample identifiers are randomised with built-in functions: `${__UUID}`, `${__Random(1,1000000)}`, `${__threadNum}`, `${__time}`.

**Valid sample payloads** (derived from the request DTOs and domain validation patterns):

```jsonc
// POST {auth}/api/users   (registration, public)  — username must be email format, role ∈ {ADMIN, USER}
{ "username": "${username}", "password": "${password}", "role": "USER" }   // ${username} = loadtest_<uuid>@asapp.test, generated once per pass

// POST {auth}/api/auth/token
{ "username": "${username}", "password": "${password}" }

// POST {users}/api/users   — email must match the email pattern; phoneNumber must match ^(\d{3}[- ]?){2}\d{3}$
{ "firstName": "Load", "lastName": "Test", "email": "user_${__UUID}@asapp.test", "phoneNumber": "666-555-444" }

// POST {tasks}/api/tasks   — userId is any non-blank string; no FK enforcement yet
{ "userId": "${userId}", "title": "task_${__threadNum}_${__Random(1,1000000)}", "description": "load test task" }
```

## 7. `asapp-regression.jmx` — deterministic full journey

A single Thread Group: **1 thread, 1 loop**. Every step has a **Response Assertion** on the HTTP status and, where noted, a **JSON Assertion** on a key field. Variables captured via **JSON Extractor**. Ordered so the cross-service enrichment is meaningful (the task references the created user):

| # | Request | Expect | Capture / assert |
|---|---|---|---|
| 1 | `POST {auth}/api/users` (public) | 201 | capture `authUserId`; the registered username |
| 2 | `POST {auth}/api/auth/token` | 200 | capture `accessToken`, `refreshToken` |
| 3 | `POST {users}/api/users` | 201 | capture `userId`; assert echoed `email` |
| 4 | `POST {tasks}/api/tasks` (`userId` = step 3) | 201 | capture `taskId`; assert echoed `title` |
| 5 | `GET {tasks}/api/tasks/{taskId}` | 200 | assert `title` and `userId` |
| 6 | `GET {tasks}/api/tasks?ids={taskId}` | 200 | assert array size = 1 |
| 7 | `GET {tasks}/api/tasks/user/{userId}` | 200 | assert array contains `taskId` |
| 8 | `POST {auth}/api/auth/refresh` (`refreshToken`) | 200 | **mid-journey**: capture **new** `accessToken`, `refreshToken`; old pair now invalid — use the new token from here on |
| 9 | `PUT {tasks}/api/tasks/{taskId}` | 200 | assert updated `title` |
| 10 | `GET {users}/api/users/{userId}` | 200 | **fan-out**: assert enriched `taskIds` contains `taskId` |
| 11 | `GET {users}/api/users?ids={userId}` | 200 | assert array size = 1 |
| 12 | `PUT {users}/api/users/{userId}` | 200 | assert updated field |
| 13 | `DELETE {tasks}/api/tasks/{taskId}` | 204 | — |
| 14 | `DELETE {users}/api/users/{userId}` | 204 | — |
| 15 | `DELETE {auth}/api/users/{authUserId}` | 204 | self-cleanup of the registration |
| 16 | `POST {auth}/api/auth/revoke` (`accessToken`) | 204 | revoke the current session |
| 17 | `GET {tasks}/api/tasks/{taskId}` (revoked token) | 401 | **negative check**: proves revocation works (auth fails before the deleted-resource 404) |

Steps 3–7 carry `Authorization: Bearer ${accessToken}` (from step 2); after the mid-journey refresh, steps 9–15 carry the **new** token. Steps 8 and 16 (`refresh`/`revoke`) are public and pass the token in the request **body**, not the header; step 17 deliberately reuses the revoked token to assert `401`. The whole journey runs well within the 5-minute token life — the refresh in step 8 is a feature test (placed after some real usage), not an expiry workaround.

**Failure behaviour**: `jmeter -n` always exits `0`, even when samplers or assertions fail — so the run script makes the gate real by parsing the `.jtl` (any row with `success=false`) and exiting non-zero if anything failed. That is what turns this into a clean go/no-go signal. Assertions use the `accessToken`/ids captured earlier; a failure early in the chain naturally fails the dependent steps.

## 8. `asapp-stress.jmx` — deterministic nested-loop journey

A deterministic, comprehensive journey run by many concurrent threads — same shape as the regression plan, scaled for load. Each thread repeats a full **self-contained pass** for `duration` seconds (or a fixed `loops` count), generating real read+write traffic across all three services to watch in Grafana. Per-pass volume is randomized (random user and task counts) so the load isn't perfectly uniform. Correctness gating stays in the regression plan; assertions here are light.

### 8.1 Journey (per thread, repeated for `duration` / `loops`)

```
Thread Group  (threads=${threads}, ramp-up=${rampup}, scheduler → duration=${duration}s, loop count=${loops})
│
│  ── auth setup ──
├─  1. POST   {auth}/api/users                 register loadtest_${__UUID}@asapp.test → capture authUserId
├─  2. POST   {auth}/api/auth/token            → capture accessToken, refreshToken
├─  3. GET    {auth}/api/users/{authUserId}    read auth user
├─  4. PUT    {auth}/api/users/{authUserId}    update auth user
├─  5. GET    {auth}/api/users                 read all auth users
├─  6. POST   {auth}/api/auth/refresh          → new tokens (used onward)
│
├─ Loop Controller ×${__Random(1,${users.per.pass.max})}   # owner users, random 1..max (the x loop)
│   ├─  7. POST {users}/api/users              → capture userId; append to userIds
│   ├─  8. GET  {users}/api/users/{userId}     read user (no tasks yet → empty fan-out)
│   ├─  9. PUT  {users}/api/users/{userId}     update user
│   ├─ 10. GET  {users}/api/users              get all users
│   ├─ Loop Controller ×${__Random(1,${tasks.per.user.max})}   # tasks for this user, random 1..max (the y loop)
│   │   ├─ 11. POST {tasks}/api/tasks (userId)        → capture taskId; append to taskIds
│   │   ├─ 12. GET  {tasks}/api/tasks/{taskId}        read task
│   │   ├─ 13. PUT  {tasks}/api/tasks/{taskId}        update task
│   │   └─ 14. GET  {tasks}/api/tasks?ids=<accumulated taskIds>   read by ids
│   ├─ 15. GET  {tasks}/api/tasks/user/{userId}   tasks by user
│   └─ 16. GET  {users}/api/users/{userId}         enriched read — fan-out now returns real taskIds
│
├─ 17. GET    {tasks}/api/tasks                get all tasks (once per pass)
│
│  ── cleanup (still authenticated) ──
├─ 18. ForEach taskIds → DELETE {tasks}/api/tasks/{id}
├─ 19. ForEach userIds → DELETE {users}/api/users/{id}
├─ 20. DELETE {auth}/api/users/{authUserId}    delete auth user
└─ 21. POST   {auth}/api/auth/revoke           revoke — last auth action (logout)
```

### 8.2 Structure notes

- **Closed load model**: a stock JMeter Thread Group holds a constant `threads` concurrency — each thread loops the journey back-to-back for `duration` (a finishing pass immediately restarts on the *same reused* thread; each pass registers a fresh `loadtest_${__UUID}` user, so the SUT still sees new sessions). Throughput self-adjusts to the system's speed; push harder by raising `threads`. Open/arrival-rate and stepping models (JMeter Plugins) are out of scope (§13).
- **Deterministic shape, randomized volume**: no probabilistic operation mix — just two nested **Loop Controllers** whose counts are `__Random(1, users.per.pass.max)` × `__Random(1, tasks.per.user.max)`, re-rolled per pass / per user. The *sequence* is fixed; only the *quantity* varies, so the load isn't perfectly flat — and it needs far less scripting than a probability-weighted operation mix would.
- **Self-contained passes**: each pass mints and tears down its own auth user + tokens, so token expiry is never a cross-pass concern. Threads keep starting fresh passes until `duration` elapses (or the `loops` cap is hit); raise `duration`, `threads`, or the per-pass maxes for a heavier run.
- **Run modes & cleanup**: a `loops`-bounded run (`loops ≥ 1`, every pass completes) leaves the databases exactly as it found them. A `duration`-bounded run (`loops = -1`) is cut off when time elapses, so the final in-flight pass per thread may leave orphan rows — all uniquely named (`loadtest_${__UUID}`), so re-runs never collide; reset volumes (`docker-compose down -v`) occasionally if they accumulate.
- **Fan-out coverage**: step 8 exercises the enriched users→tasks read while the user has no tasks (empty result), and step 16 exercises it after the task loop (populated) — Grafana shows the cross-service path both cold and warm.
- **Endpoint coverage**: every functional endpoint of all three services is hit each pass — auth user CRUD + token/refresh/revoke, users CRUD + batch + get-all, tasks CRUD + batch + by-user + get-all.

### 8.3 Id accumulation & cleanup

- Each pass starts by (re)initializing two empty thread-local lists — `taskIds`, `userIds` — then accumulates the ids it creates into them via a small **JSR223 PostProcessor** (Groovy) after each create (append-only; no random selection). Re-initializing per pass keeps the ForEach cleanup from touching ids already deleted by an earlier pass.
- Cleanup uses **ForEach Controllers** over those lists to delete every task and user the pass created, then deletes the auth user and revokes the token. Each *completed* pass leaves the databases exactly as it found them; only an interrupted pass (a `duration` cutoff, or a mid-run crash) leaves orphan rows — see the run-modes note in §8.2.
- Unique-by-construction data (`loadtest_${__UUID}` usernames, etc.) means concurrent threads and repeated runs never collide on the unique-username constraint.

### 8.4 Assertions

Light by design — the stress plan is for observation, not correctness gating. A basic Response Assertion checks the expected status per verb (`2xx`; no `404`, since each thread only reads/updates/deletes its own live data and cleanup runs last). No SLA thresholds; latency/throughput/error-rate are read from Grafana.

## 9. Authentication & token lifecycle

- **Regression** (§7): registers, gets a token, exercises it, then calls `refresh` mid-journey and uses the new token for the rest; finishes by revoking the token and asserting a follow-up call returns `401`. Completes well within the 5-minute token life.
- **Stress** (§8): every pass is self-contained — it registers its own auth user, mints tokens, refreshes once (step 6), and revokes at the end (step 21). Because each pass creates and discards its own tokens, expiry is never a cross-pass concern.
- **Very large single pass**: only if `users.per.pass.max × tasks.per.user.max` is cranked so high that one pass exceeds the 5-minute access-token life would a token expire mid-pass; then add a `refresh` inside the users loop. The default sizes stay well under it.

## 10. Reporting & run scripts

Two thin entry-point scripts (`run-regression.sh`, `run-stress.sh`; bash, run on Windows via Git for Windows' bundled `sh`), each: (1) call `ensure-jmeter`, (2) optionally pre-flight the stack, (3) run JMeter non-GUI generating an HTML dashboard, (4) print the report path.

```
jmeter -n \
  -t tools/jmeter/asapp-<plan>.jmx \
  -q tools/jmeter/env/local-docker.properties \
  -l tools/jmeter/results/<plan>-<timestamp>.jtl \
  -e -o tools/jmeter/results/<plan>-<timestamp>-report
```

- **Pre-flight (optional, on by default)**: poll each functional service's whitelisted readiness probe — `/readyz` on its management port, under the service context path (e.g. `http://localhost:8091/asapp-tasks-service/readyz`; auth `8090`, tasks `8091`, users `8092`) — and abort with a clear message if any is not UP, so a run never starts against a half-booted stack.
- **Overrides**: scripts forward extra args to JMeter, e.g. `./run-stress.sh -Jthreads=100 -Jduration=600 -Jusers.per.pass.max=8 -Jtasks.per.user.max=8`.
- Each run writes a timestamped `.jtl` + HTML report under `results/`.

## 11. Documentation, ignore, TODO

- **`tools/jmeter/README.md`**: prerequisites (Docker stack up via `docker-compose up -d`; internet for first JMeter download; Java already present); how to run each plan; how to tune via `-J`; where reports land; how to open the HTML dashboard; how to watch Grafana (`:3000`) during a stress run; how to launch the bundled JMeter GUI for editing the `.jmx` files.
- **`.gitignore`**: add `tools/jmeter/.runtime/` and `tools/jmeter/results/`.
- **`TODO.md`**: tick `[ ] Add load test with JMeter` → `[X]` under v0.4.0 → Quick Wins → Technical Improvements (separate housekeeping commit).
- **Root `README.md`** (minimal): add `tools/jmeter/` to the *Project Structure* tree and a short *Load Testing* note under *Development* linking to `tools/jmeter/README.md`. All run/tuning detail stays in the tool README; the root just makes it discoverable, consistent with how `tools/` and testing are already surfaced there.

## 12. Validation

- **Regression**: against a fresh `docker-compose up -d`, `run-regression` completes with **0 errors / 0 failed assertions**; a forced failure (e.g. stop one service) makes it exit non-zero — proving the gate works.
- **Stress**: `run-stress` with defaults runs for `duration` (300 s), produces the HTML dashboard, and the load is visible in Grafana (request rate, latency, JVM/DB metrics). A loop-bounded run (`-Jloops=N`) leaves the DB as it started; consecutive runs never collide on the unique-username constraint.
- **Provisioning**: deleting `.runtime/` and re-running re-downloads and verifies JMeter; a tampered checksum aborts the run.

## 13. Out of scope / YAGNI

CI integration · `jmeter-maven-plugin` · Gatling (separate backlog) · custom Grafana dashboards · distributed/remote JMeter · open/arrival-rate & stepping thread groups (JMeter Plugins) · shared cross-thread data · SLA threshold gating on stress · committing the JMeter binary.

## 14. Git workflow

Single feature branch (`setup-load-test`, already checked out) off `main`. Commits sliced per concern, each self-contained:

1. `build(jmeter): add auto-download provisioning scripts and pinned version`
2. `test(jmeter): add regression test plan and run scripts`
3. `test(jmeter): add stress test plan and run scripts`
4. `chore(jmeter): gitignore runtime and results`
5. `docs(jmeter): add tools/jmeter README and root pointer`
6. `docs(todo): mark JMeter load test as done`

Before opening the PR: dispatch the relevant reviewers (`devops-engineer` for the scripts/provisioning, `architect-reviewer` for overall fit) against the branch diff and address findings. PR title/body follow Conventional Commits + bulleted body per the `commit-msg` skill.

## 15. Post-implementation notes

This spec was written before implementation. The initial slice was built from the plan
(`docs/superpowers/plans/2026-06-05-jmeter-load-test.md`) and then refined through a
manual review and debugging pass. The overall shape and the out-of-Maven/CI boundary
shipped as designed; several lower-level decisions were corrected or extended once the
plans ran against the real stack. The sections above describe the original design intent;
**the canonical implementation is the current state of `tools/jmeter/`**. Notable deltas:

- **`JSONPathExtractor` renamed to `JSONPostProcessor` in JMeter 5.6.3.** The spec
  referenced the old element name; the plans use `JSONPostProcessor` throughout.

- **Preflight probes moved to main ports (8080–8082), not management ports (8090–8092).**
  The spec listed the management ports for `/readyz`; the actual services expose the probe
  on the main port via `management.endpoint.health.probes.add-additional-paths`, so polling
  the management ports returned 404 for a healthy stack. Both run scripts now poll
  `http://localhost:808{0,1,2}/<context>/readyz`.

- **Revoke step dropped from both plans' teardowns.** `DELETE /api/users/{id}` cascades
  to clear the user's JWT store, so the explicit `POST /api/auth/revoke` that followed
  it was redundant and returned 401 (user already gone) instead of the asserted 204. The
  revoke step was removed from both teardowns. As a consequence the regression plan's
  **negative revocation check (original step 17)** was also removed — it relied on the
  revoked token being independently valid, which the cascade invalidation makes
  untestable at teardown time.

- **Regression plan restructured for full endpoint coverage.** The original 17-step
  journey interleaved the three services and skipped several read endpoints (auth
  get-by-id, auth update, auth get-all, users get-all, tasks get-all). The plan was
  reshaped to mirror the stress journey structure (auth CRUD → token lifecycle → users
  CRUD → tasks CRUD → enriched fan-out reads → cleanup), giving complete functional
  coverage of every endpoint across all three services.

- **Stress plan: nested Loop Controllers replaced with While Controllers.** JMeter 5.6.3
  nested Loop Controllers only execute their body on each thread's **first pass** through
  the parent; on every subsequent pass they are skipped (the controller marks itself
  "done" and is never re-initialized). The users loop and tasks loop were replaced with
  While Controllers driven by per-pass counters (`usersLoopTarget`/`usersLoopIdx`,
  `tasksLoopTarget`/`tasksLoopIdx`), which re-evaluate their condition on every parent
  iteration. ForEach cleanup controllers were unaffected — they iterate correctly across
  passes.

- **Stress plan requires Java 17 or 21 (not Java 25).** JMeter 5.6.3 bundles Groovy
  3.0.20, which cannot parse Java 25 class files (major version 69). The stress plan's
  JSR223/Groovy scripts (id-list initialisation, append post-processors, loop counters)
  crashed at runtime with `Unsupported class file major version 69`. A new
  `scripts/resolve-java.sh` was added; it locates a Groovy-compatible JDK and is sourced
  by `run-stress.sh` before JMeter is invoked. `run-regression.sh` has no Groovy and
  runs on the project's Java 25 without modification.

- **`.jtl` gate replaced with `statistics.json` gate.** The spec proposed parsing the
  `.jtl` CSV with `awk` to count `success=false` rows. The `.jtl` format embeds commas
  and newlines inside quoted response/assertion messages; the column parse shifted on
  those rows and silently passed real failures. The gate now reads `Total.errorCount` from
  the dashboard's `statistics.json` (structured JSON, immune to embedded delimiters).

- **Layout changes from the §4 spec tree:**
  - `lib/jmeter-version.properties` → `scripts/jmeter-version.properties` (co-located
    with its only consumer `ensure-jmeter.sh`).
  - `env/local-docker.properties` → `env/local.properties` (the localhost defaults serve
    a native local run as well, not only docker-compose).
  - Two new scripts added: `scripts/resolve-java.sh` (Groovy-compatible JVM selection for
    the stress plan) and `scripts/common.sh` (shared preflight, JMeter invocation, and
    path setup extracted from both run scripts to eliminate duplication).

- **Run-script hardening added post-initial-implementation:**
  - Unknown `--option` arguments are now rejected with exit 2 (previously forwarded to
    JMeter, which rejected them but exited 0 — causing a false pass on a run that never
    executed).
  - `--java-home <path>` flag on `run-stress.sh` replaces the `JMETER_JAVA_HOME`
    environment variable for selecting the stress JVM per run.

**For future load-test edits**, treat `tools/jmeter/` as the canonical source and
`tools/jmeter/README.md` as the operator reference. The key design decisions (out-of-Maven
boundary, self-provisioning harness, regression-gate vs. stress-observation split,
per-pass self-cleaning) are preserved exactly as designed; the fixes above are all
implementation-level corrections surfaced by running against the real stack.