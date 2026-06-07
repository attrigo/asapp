# JMeter Load Test Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a standalone JMeter load-testing tool under `tools/jmeter/` with a deterministic regression plan (an automated pre-release go/no-go gate) and a tunable nested-loop stress plan (observed live in Grafana), both run from bash against the running `docker-compose` stack.

**Architecture:** A self-provisioning bash harness downloads a pinned, checksum-verified JMeter on first use into a gitignored `.runtime/`. Two `.jmx` test plans read all hosts/ports/load-knobs as JMeter properties from `env/local-docker.properties` (overridable with `-J`). The regression plan runs a single deterministic full journey (1 thread, 1 loop) with status + field assertions; its run script parses the `.jtl` and exits non-zero on any failure. The stress plan runs the same comprehensive read+write journey under many threads with two nested random-count loops, self-cleaning per pass. Nothing here runs in `mvn verify` or CI.

**Tech Stack:** Apache JMeter 5.6.3 (core only — JSON Extractor, JSON Assertion, ForEach/Loop controllers, JSR223/Groovy are all bundled; no Plugins Manager needed), bash (run on Windows via Git for Windows' bundled `sh`, the same `sh` that runs the repo's git hooks), the three running services from `docker-compose.yaml`.

---

## Source spec

`docs/superpowers/specs/2026-06-04-jmeter-load-test-design.md` (approved). Read it before starting — this plan implements it section-by-section.

## Ground-truth reference (verified against the codebase)

These were read from the actual controllers/DTOs and config — they OVERRIDE any stale examples in `README.md`.

**Base URLs (servlet context path = service name):**

| Service | Base URL | Auth on `/api/**` |
|---|---|---|
| authentication | `http://localhost:8080/asapp-authentication-service` | `POST /api/users` and `/api/auth/**` are public; everything else needs `Authorization: Bearer <accessToken>` |
| tasks | `http://localhost:8081/asapp-tasks-service` | all `/api/**` need Bearer |
| users | `http://localhost:8082/asapp-users-service` | all `/api/**` need Bearer |

**Readiness probes (management port + service context path, all `permitAll`, no auth):**
- `http://localhost:8090/asapp-authentication-service/readyz`
- `http://localhost:8091/asapp-tasks-service/readyz`
- `http://localhost:8092/asapp-users-service/readyz`

**Request bodies (camelCase, post-refactor):**

```jsonc
// POST {auth}/api/users        (public)   username=@Email, password=@Size(8,64), role ∈ {ADMIN,USER}
{ "username": "...", "password": "...", "role": "USER" }
// POST {auth}/api/auth/token   (public)
{ "username": "...", "password": "..." }
// POST {auth}/api/auth/refresh (public)   token in BODY
{ "refreshToken": "..." }
// POST {auth}/api/auth/revoke  (public)   token in BODY
{ "accessToken": "..." }
// PUT  {auth}/api/users/{id}
{ "username": "...", "password": "...", "role": "USER" }

// POST/PUT {users}/api/users    email=@Email, phoneNumber=@Pattern ^(\d{3}[- ]?){2}\d{3}$
{ "firstName": "Load", "lastName": "Test", "email": "...", "phoneNumber": "666-555-444" }

// POST/PUT {tasks}/api/tasks    userId=@NotBlank, title=@NotBlank, description optional
{ "userId": "...", "title": "...", "description": "..." }
```

**Response bodies & status — CRITICAL for JMeter extractors/assertions:**

| Endpoint | Status | Response body (verbatim fields) |
|---|---|---|
| `POST {auth}/api/users` | 201 | `{ "userId": "<uuid>" }` |
| `POST {auth}/api/auth/token` | 200 | `{ "accessToken": "...", "refreshToken": "..." }` |
| `POST {auth}/api/auth/refresh` | 200 | `{ "accessToken": "...", "refreshToken": "..." }` |
| `POST {auth}/api/auth/revoke` | 204 | (empty) |
| `GET {auth}/api/users/{id}` | 200 | `{ "userId", "username", "password", "role" }` |
| `GET {auth}/api/users` | 200 | `[ {...}, ... ]` |
| `PUT {auth}/api/users/{id}` | 200 | `{ "userId": "<uuid>" }` |
| `DELETE {auth}/api/users/{id}` | 204 | (empty) |
| `POST {users}/api/users` | 201 | `{ "userId": "<uuid>" }` |
| `GET {users}/api/users/{id}` | 200 | `{ "userId", "firstName", "lastName", "email", "phoneNumber", "taskIds": ["<uuid>", ...] }` (enriched fan-out) |
| `GET {users}/api/users?ids=` | 200 | `[ { "userId", "firstName", "lastName", "email", "phoneNumber" }, ... ]` |
| `PUT {users}/api/users/{id}` | 200 | `{ "userId": "<uuid>" }` |
| `DELETE {users}/api/users/{id}` | 204 | (empty) |
| `POST {tasks}/api/tasks` | 201 | `{ "taskId": "<uuid>" }` |
| `GET {tasks}/api/tasks/{id}` | 200 | `{ "taskId", "userId", "title", "description", "startDate", "endDate" }` |
| `GET {tasks}/api/tasks?ids=` | 200 | `[ { "taskId", "userId", "title", ... }, ... ]` |
| `GET {tasks}/api/tasks/user/{id}` | 200 | `[ { "taskId", "userId", "title", ... }, ... ]` (path var is the userId) |
| `GET {tasks}/api/tasks` | 200 | `[ {...}, ... ]` |
| `PUT {tasks}/api/tasks/{id}` | 200 | `{ "taskId": "<uuid>" }` |
| `DELETE {tasks}/api/tasks/{id}` | 204 | (empty) |

**`ids` query param** is literally `ids` on both batch endpoints, validated `@Size(max=50)` → never query more than 50 ids at once.

## Spec reconciliation notes (intentional deviations — not drift)

1. **Create/update endpoints return only the id**, not the full resource. So spec §7's "assert echoed `email`" (step 3) and "assert echoed `title`" (step 4) are **not possible from those response bodies** — those create responses are `{ "userId" }` / `{ "taskId" }` only. Resolution: on creates/updates we assert the status code + that the id field is captured; the title/userId equality assertions live on the **GET** steps where the field is actually returned (regression step 5). Updates (PUT) assert status `200` only — verifying the mutated value would require a follow-up GET the approved journey does not include; out of scope.
2. **"array size = 1" / "array contains id"** are asserted pragmatically: a JSON Assertion on `$[0].<idField>` equals the queried id (proves the queried element is returned), and/or a Response Assertion `Contains <uuid>` on the body (UUIDs are unique per run, so substring containment is reliable). Exact array-length counting is not worth a JSR223 assertion for the gate's purpose.
3. **Stress step 14 (`tasks?ids=<accumulated>`)** caps the id list to the most-recent **50** (endpoint `@Size(max=50)`); with default maxes a pass can accumulate up to 100 task ids.
4. **Selective staging:** `.gitignore` for `.runtime/` and `results/` is committed in Task 4 (per the spec's commit order), but those dirs already exist by Task 1/2 (JMeter download + run output). Every commit step below uses explicit `git add <paths>` — **never** `git add -A`/`git add .` — so generated artifacts are never staged.

## File structure

```
tools/jmeter/
├── README.md                       # Task 5  — prerequisites, run, tune, reports, GUI
├── asapp-regression.jmx            # Task 2  — deterministic full-journey plan
├── asapp-stress.jmx                # Task 3  — nested-loop load plan
├── env/
│   └── local-docker.properties     # Task 2  — default hosts/ports + load knobs
├── lib/
│   └── jmeter-version.properties   # Task 1  — pinned version + URL + SHA-512
├── run-regression.sh               # Task 2  — entry script + .jtl gate
├── run-stress.sh                   # Task 3  — entry script (observation)
├── scripts/
│   └── ensure-jmeter.sh            # Task 1  — download/verify/unzip JMeter
├── .runtime/                       # gitignored (Task 4) — auto-downloaded JMeter
└── results/                        # gitignored (Task 4) — .jtl + HTML dashboards
```
Root files touched: `.gitignore` (Task 4), `README.md` (Task 5), `TODO.md` (Task 6).

## Prerequisites for verification steps

Steps that **run** a plan need the full stack up and are slow/heavy (not part of `mvn verify`). Bring it up once:

```bash
mvn spring-boot:build-image      # only if the 0.4.0-SNAPSHOT images aren't built yet
docker-compose up -d
docker-compose ps                # all healthy
```

All bash commands below run via Git for Windows' `sh` (the Bash tool). Tools used (`curl`, `unzip`, `sha512sum`, `jar`, `awk`, `date`, `mktemp`) are present in Git Bash + the project's Java 25.

---

## Task 1: JMeter provisioning (pinned version + auto-download)

Implements spec §5. Produces a checksum-verified, cached, host-install-free JMeter.

**Files:**
- Create: `tools/jmeter/lib/jmeter-version.properties`
- Create: `tools/jmeter/scripts/ensure-jmeter.sh`

- [ ] **Step 1: Write the pinned-version metadata**

`tools/jmeter/lib/jmeter-version.properties` — plain `KEY=VALUE`, sourced as shell by `ensure-jmeter.sh`:

```properties
# Apache JMeter pinned engine. Bump these three lines to upgrade.
# SHA-512 is the published checksum from archive.apache.org (apache-jmeter-<v>.zip.sha512).
JMETER_VERSION=5.6.3
JMETER_URL=https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.zip
JMETER_SHA512=387fadca903ee0aa30e3f2115fdfedb3898b102e6b9fe7cc3942703094bd2e65b235df2b0c6d0d3248e74c9a7950a36e42625fd74425368342c12e40b0163076
```

- [ ] **Step 2: Write `ensure-jmeter.sh`**

`tools/jmeter/scripts/ensure-jmeter.sh` — idempotent; prints the binary path on stdout, logs to stderr:

```bash
#!/usr/bin/env bash
#
# Idempotently provision a pinned Apache JMeter into tools/jmeter/.runtime/.
# Prints the absolute path to the jmeter binary on stdout (logs go to stderr),
# so callers can do: JMETER_BIN="$(scripts/ensure-jmeter.sh)".
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JMETER_TOOL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"   # tools/jmeter
RUNTIME_DIR="$JMETER_TOOL_DIR/.runtime"
VERSION_FILE="$JMETER_TOOL_DIR/lib/jmeter-version.properties"

# Load JMETER_VERSION / JMETER_URL / JMETER_SHA512
# shellcheck disable=SC1090
. "$VERSION_FILE"

JMETER_DIR="$RUNTIME_DIR/apache-jmeter-${JMETER_VERSION}"
JMETER_BIN="$JMETER_DIR/bin/jmeter"

# 1. Already provisioned -> return cached binary.
if [ -x "$JMETER_BIN" ]; then
  echo "$JMETER_BIN"
  exit 0
fi

echo "Provisioning Apache JMeter ${JMETER_VERSION} (first run only)..." >&2
mkdir -p "$RUNTIME_DIR"

TMP_ZIP="$(mktemp "${TMPDIR:-/tmp}/jmeter-XXXXXX.zip")"
trap 'rm -f "$TMP_ZIP"' EXIT

# 2. Download.
echo "Downloading $JMETER_URL" >&2
curl -fSL --retry 3 -o "$TMP_ZIP" "$JMETER_URL"

# 3. Verify SHA-512 (abort on mismatch).
echo "Verifying SHA-512..." >&2
if command -v sha512sum >/dev/null 2>&1; then
  actual_sha="$(sha512sum "$TMP_ZIP" | awk '{print $1}')"
else
  actual_sha="$(shasum -a 512 "$TMP_ZIP" | awk '{print $1}')"
fi
if [ "$actual_sha" != "$JMETER_SHA512" ]; then
  echo "ERROR: SHA-512 mismatch for $JMETER_URL" >&2
  echo "  expected: $JMETER_SHA512" >&2
  echo "  actual:   $actual_sha" >&2
  exit 1
fi

# 4. Unzip (prefer unzip; fall back to the JDK's jar since Java 25 is required anyway).
echo "Extracting into $RUNTIME_DIR ..." >&2
if command -v unzip >/dev/null 2>&1; then
  unzip -q "$TMP_ZIP" -d "$RUNTIME_DIR"
else
  ( cd "$RUNTIME_DIR" && jar xf "$TMP_ZIP" )
fi

# jar/unzip may drop the executable bit on the launcher scripts.
chmod +x "$JMETER_DIR/bin/jmeter" 2>/dev/null || true

if [ ! -x "$JMETER_BIN" ]; then
  echo "ERROR: expected binary not found/executable: $JMETER_BIN" >&2
  exit 1
fi

echo "$JMETER_BIN"
```

- [ ] **Step 3: Verify first-run download + verification + caching (requires internet)**

Run:
```bash
sh tools/jmeter/scripts/ensure-jmeter.sh
```
Expected: stderr shows "Provisioning... / Downloading... / Verifying SHA-512... / Extracting...", and stdout prints a path ending in `tools/jmeter/.runtime/apache-jmeter-5.6.3/bin/jmeter`. Exit code `0`.

- [ ] **Step 4: Verify idempotency (no re-download on second run)**

Run:
```bash
sh tools/jmeter/scripts/ensure-jmeter.sh
```
Expected: **no** "Downloading" line on stderr; stdout immediately prints the same binary path; exit `0`.

- [ ] **Step 5: Verify the checksum gate aborts on tamper**

Run (point the script at a bad checksum without editing the committed file):
```bash
# Confirm the binary launches and reports the pinned version:
"$(sh tools/jmeter/scripts/ensure-jmeter.sh)" --version 2>&1 | head -n 5
```
Expected: prints an ASCII "Apache JMeter" banner including `5.6.3`. (The tamper-abort path is exercised in Task 7's validation against the real `lib/jmeter-version.properties`; the logic is the `actual_sha != JMETER_SHA512` branch above.)

- [ ] **Step 6: Commit**

```bash
git add tools/jmeter/lib/jmeter-version.properties tools/jmeter/scripts/ensure-jmeter.sh
git commit -m "build(jmeter): add auto-download provisioning scripts and pinned version"
```
(Do not stage `tools/jmeter/.runtime/` — it is gitignored in Task 4; stage only the two listed files.)

---

## Task 2: Regression plan, shared env, and run-regression gate

Implements spec §6 (config), §7 (regression journey), §10 (run script + gate), §12 (validation).

**Files:**
- Create: `tools/jmeter/env/local-docker.properties`
- Create: `tools/jmeter/asapp-regression.jmx` (authored in the JMeter GUI per the build-spec below, saved to this path)
- Create: `tools/jmeter/run-regression.sh`

### Shared config & JMeter element conventions (used by both plans)

- [ ] **Step 1: Write the shared properties file**

`tools/jmeter/env/local-docker.properties` (passed to JMeter with `-q`; every value overridable per run with `-J<name>=<value>`):

```properties
# --- Service bases (servlet context path = service name) ---
auth.scheme=http
auth.host=localhost
auth.port=8080
auth.context=/asapp-authentication-service
tasks.scheme=http
tasks.host=localhost
tasks.port=8081
tasks.context=/asapp-tasks-service
users.scheme=http
users.host=localhost
users.port=8082
users.context=/asapp-users-service

# --- Load knobs (stress plan) ---
threads=20
rampup=20
duration=300
loops=-1
users.per.pass.max=10
tasks.per.user.max=10

# --- Credentials (meets the 8-64 char password rule) ---
password=L0adT3st!Pass
```

**Authoring convention for every `.jmx` element below (build in the GUI, save as XML):**

Legend used in the build-specs:
- `[HTTP Request] "NN name" METHOD` → set **Server Name** / **Port Number** / **Protocol** / **Method** / **Path** as given. For bodies, use the **Body Data** tab (raw). UUIDs/text need no URL-encoding.
- `└ [JSON Extractor] Name | JSONPath | MatchNo` → JSON Extractor (a.k.a. JSON post-processor): *Names of created variables*, *JSON Path expressions*, *Match No.*
- `└ [Response Assertion] Field | Type | Pattern` → Response Assertion: *Field to Test* + *Pattern Matching Rules* + the pattern.
- `└ [JSON Assertion] JSONPath = Expected` → JSON Assertion with "Validate against expected value" on, "Match as regular expression" off.
- `+ Bearer` → add a **child** HTTP Header Manager named `Bearer` holding one header `Authorization: Bearer ${accessToken}`. A child Header Manager **merges** with the plan-level `Content-Type` one, so the protected request sends both headers. Add `+ Bearer` ONLY where shown.
- **Response Assertion → "Field to Test" = `Response Code`**, "Pattern Matching Rules" = **Equals** for an exact code (e.g. `201`); = **Matches** for a regex (e.g. `2\d\d`). For body containment use **Field = `Text Response`**, rule = **Contains**.

### Build `asapp-regression.jmx`

Open the bundled GUI to author it:
```bash
JMETER_BIN="$(sh tools/jmeter/scripts/ensure-jmeter.sh)"
# Windows: tools/jmeter/.runtime/apache-jmeter-5.6.3/bin/jmeter.bat
"$JMETER_BIN" -q tools/jmeter/env/local-docker.properties &
```
Then build exactly this tree and **Save As** `tools/jmeter/asapp-regression.jmx`.

- [ ] **Step 2: Test Plan + shared config elements**

```
Test Plan  "asapp-regression"
├─ [User Defined Variables] "props (property->variable bridge)"
│     auth.scheme   = ${__P(auth.scheme,http)}
│     auth.host     = ${__P(auth.host,localhost)}
│     auth.port     = ${__P(auth.port,8080)}
│     auth.context  = ${__P(auth.context,/asapp-authentication-service)}
│     tasks.scheme  = ${__P(tasks.scheme,http)}
│     tasks.host    = ${__P(tasks.host,localhost)}
│     tasks.port    = ${__P(tasks.port,8081)}
│     tasks.context = ${__P(tasks.context,/asapp-tasks-service)}
│     users.scheme  = ${__P(users.scheme,http)}
│     users.host    = ${__P(users.host,localhost)}
│     users.port    = ${__P(users.port,8082)}
│     users.context = ${__P(users.context,/asapp-users-service)}
│     password      = ${__P(password,L0adT3st!Pass)}
│
└─ [Thread Group] "regression"   Number of Threads=1, Ramp-up=1, Loop Count=1, scheduler OFF
   ├─ [User Parameters] "per-pass vars"   "Update Once Per Iteration" = ON
   │     username       = loadtest_${__UUID}@asapp.test
   │     userEmail      = user_${__UUID}@asapp.test
   │     taskTitle      = task_${__threadNum}_${__Random(1,1000000)}
   │     taskTitleUpd   = task_${__threadNum}_upd_${__Random(1,1000000)}
   ├─ [HTTP Header Manager] "Content-Type"   Content-Type: application/json
   └─ (samplers 01..17 below, in order)
```

- [ ] **Step 3: Auth setup samplers (01–02, public)**

```
[HTTP Request] "01 register"  POST
  Server=${auth.host}  Port=${auth.port}  Protocol=${auth.scheme}
  Path=${auth.context}/api/users
  Body(raw): {"username":"${username}","password":"${password}","role":"USER"}
  └ [JSON Extractor]  authUserId | $.userId | 1
  └ [Response Assertion]  Response Code | Equals | 201

[HTTP Request] "02 token"  POST
  Server=${auth.host}  Port=${auth.port}  Protocol=${auth.scheme}
  Path=${auth.context}/api/auth/token
  Body(raw): {"username":"${username}","password":"${password}"}
  └ [JSON Extractor]  accessToken  | $.accessToken  | 1
  └ [JSON Extractor]  refreshToken | $.refreshToken | 1
  └ [Response Assertion]  Response Code | Equals | 200
```

- [ ] **Step 4: Cross-service create + reads (03–07, protected)**

```
[HTTP Request] "03 create user"  POST  + Bearer
  Server=${users.host} Port=${users.port} Protocol=${users.scheme}
  Path=${users.context}/api/users
  Body(raw): {"firstName":"Load","lastName":"Test","email":"${userEmail}","phoneNumber":"666-555-444"}
  └ [JSON Extractor]  userId | $.userId | 1
  └ [Response Assertion]  Response Code | Equals | 201

[HTTP Request] "04 create task"  POST  + Bearer
  Server=${tasks.host} Port=${tasks.port} Protocol=${tasks.scheme}
  Path=${tasks.context}/api/tasks
  Body(raw): {"userId":"${userId}","title":"${taskTitle}","description":"load test task"}
  └ [JSON Extractor]  taskId | $.taskId | 1
  └ [Response Assertion]  Response Code | Equals | 201

[HTTP Request] "05 get task"  GET  + Bearer
  Server=${tasks.host} Port=${tasks.port} Protocol=${tasks.scheme}
  Path=${tasks.context}/api/tasks/${taskId}
  └ [Response Assertion]  Response Code | Equals | 200
  └ [JSON Assertion]  $.title  = ${taskTitle}
  └ [JSON Assertion]  $.userId = ${userId}

[HTTP Request] "06 tasks by ids"  GET  + Bearer
  Server=${tasks.host} Port=${tasks.port} Protocol=${tasks.scheme}
  Path=${tasks.context}/api/tasks?ids=${taskId}
  └ [Response Assertion]  Response Code | Equals | 200
  └ [JSON Assertion]  $[0].taskId = ${taskId}      # "array size = 1": queried id is the element

[HTTP Request] "07 tasks by user"  GET  + Bearer
  Server=${tasks.host} Port=${tasks.port} Protocol=${tasks.scheme}
  Path=${tasks.context}/api/tasks/user/${userId}
  └ [Response Assertion]  Response Code | Equals | 200
  └ [Response Assertion]  Text Response | Contains | ${taskId}
```

- [ ] **Step 5: Mid-journey refresh + updates + fan-out reads (08–12)**

```
[HTTP Request] "08 refresh"  POST            # public; token in body; overwrites the token vars
  Server=${auth.host} Port=${auth.port} Protocol=${auth.scheme}
  Path=${auth.context}/api/auth/refresh
  Body(raw): {"refreshToken":"${refreshToken}"}
  └ [JSON Extractor]  accessToken  | $.accessToken  | 1
  └ [JSON Extractor]  refreshToken | $.refreshToken | 1
  └ [Response Assertion]  Response Code | Equals | 200

[HTTP Request] "09 update task"  PUT  + Bearer    # Bearer now carries the NEW accessToken
  Server=${tasks.host} Port=${tasks.port} Protocol=${tasks.scheme}
  Path=${tasks.context}/api/tasks/${taskId}
  Body(raw): {"userId":"${userId}","title":"${taskTitleUpd}","description":"load test task updated"}
  └ [Response Assertion]  Response Code | Equals | 200      # PUT returns {taskId} only; no echoed title

[HTTP Request] "10 get user (enriched)"  GET  + Bearer
  Server=${users.host} Port=${users.port} Protocol=${users.scheme}
  Path=${users.context}/api/users/${userId}
  └ [Response Assertion]  Response Code | Equals | 200
  └ [Response Assertion]  Text Response | Contains | ${taskId}   # taskIds[] contains the task (fan-out)

[HTTP Request] "11 users by ids"  GET  + Bearer
  Server=${users.host} Port=${users.port} Protocol=${users.scheme}
  Path=${users.context}/api/users?ids=${userId}
  └ [Response Assertion]  Response Code | Equals | 200
  └ [JSON Assertion]  $[0].userId = ${userId}

[HTTP Request] "12 update user"  PUT  + Bearer
  Server=${users.host} Port=${users.port} Protocol=${users.scheme}
  Path=${users.context}/api/users/${userId}
  Body(raw): {"firstName":"Load","lastName":"Updated","email":"${userEmail}","phoneNumber":"666-555-444"}
  └ [Response Assertion]  Response Code | Equals | 200      # PUT returns {userId} only
```

- [ ] **Step 6: Cleanup + revoke + negative check (13–17)**

```
[HTTP Request] "13 delete task"  DELETE  + Bearer
  Server=${tasks.host} Port=${tasks.port} Protocol=${tasks.scheme}
  Path=${tasks.context}/api/tasks/${taskId}
  └ [Response Assertion]  Response Code | Equals | 204

[HTTP Request] "14 delete user"  DELETE  + Bearer
  Server=${users.host} Port=${users.port} Protocol=${users.scheme}
  Path=${users.context}/api/users/${userId}
  └ [Response Assertion]  Response Code | Equals | 204

[HTTP Request] "15 delete auth user"  DELETE  + Bearer
  Server=${auth.host} Port=${auth.port} Protocol=${auth.scheme}
  Path=${auth.context}/api/users/${authUserId}
  └ [Response Assertion]  Response Code | Equals | 204

[HTTP Request] "16 revoke"  POST              # public; current accessToken in body
  Server=${auth.host} Port=${auth.port} Protocol=${auth.scheme}
  Path=${auth.context}/api/auth/revoke
  Body(raw): {"accessToken":"${accessToken}"}
  └ [Response Assertion]  Response Code | Equals | 204

[HTTP Request] "17 revoked token rejected"  GET  + Bearer   # reuses the now-revoked accessToken
  Server=${tasks.host} Port=${tasks.port} Protocol=${tasks.scheme}
  Path=${tasks.context}/api/tasks/${taskId}
  └ [Response Assertion]  Response Code | Equals | 401       # auth fails before the deleted-resource 404
```

Save the plan (`tools/jmeter/asapp-regression.jmx`).

- [ ] **Step 7: Write `run-regression.sh` (with the real go/no-go gate)**

`tools/jmeter/run-regression.sh`:

```bash
#!/usr/bin/env bash
#
# Run the deterministic regression plan against the running docker-compose stack.
# `jmeter -n` always exits 0, so this script makes the gate real: it parses the
# .jtl and exits non-zero if ANY sample/assertion failed.
#
#   ./run-regression.sh                 # pre-flight + run + gate
#   ./run-regression.sh --no-preflight  # skip readiness checks
#   ./run-regression.sh -Jauth.host=...  # extra args are forwarded to JMeter
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/env/local-docker.properties"
PLAN="$SCRIPT_DIR/asapp-regression.jmx"
RESULTS_DIR="$SCRIPT_DIR/results"

preflight() {
  local urls=(
    "http://localhost:8090/asapp-authentication-service/readyz"
    "http://localhost:8091/asapp-tasks-service/readyz"
    "http://localhost:8092/asapp-users-service/readyz"
  )
  echo "Pre-flight: checking service readiness..." >&2
  local u
  for u in "${urls[@]}"; do
    if ! curl -fsS -o /dev/null --max-time 5 "$u"; then
      echo "ERROR: service not ready: $u" >&2
      echo "Bring the stack up first: docker-compose up -d" >&2
      exit 1
    fi
    echo "  UP: $u" >&2
  done
}

PREFLIGHT=1
if [ "${1:-}" = "--no-preflight" ]; then PREFLIGHT=0; shift; fi

JMETER_BIN="$("$SCRIPT_DIR/scripts/ensure-jmeter.sh")"
[ "$PREFLIGHT" -eq 1 ] && preflight

mkdir -p "$RESULTS_DIR"
TS="$(date +%Y%m%d-%H%M%S)"
JTL="$RESULTS_DIR/regression-$TS.jtl"
REPORT="$RESULTS_DIR/regression-$TS-report"

"$JMETER_BIN" -n \
  -t "$PLAN" \
  -q "$ENV_FILE" \
  -l "$JTL" \
  -e -o "$REPORT" \
  -Jjmeter.save.saveservice.output_format=csv \
  -Jjmeter.save.saveservice.print_field_names=true \
  "$@"

# Gate: find the 'success' column by header name, count false rows.
fail_count="$(awk -F',' '
  NR==1 { for (i=1;i<=NF;i++) if ($i=="success") col=i; next }
  col && $col=="false" { c++ }
  END { print c+0 }' "$JTL")"

if [ "$fail_count" -gt 0 ]; then
  echo "REGRESSION FAILED: $fail_count failed sample(s)/assertion(s). Report: $REPORT/index.html" >&2
  exit 1
fi
echo "REGRESSION PASSED (0 failures). Report: $REPORT/index.html"
```

- [ ] **Step 8: Verify the regression plan passes against a healthy stack** *(requires the stack — see Prerequisites)*

Run:
```bash
sh tools/jmeter/run-regression.sh
```
Expected: pre-flight prints three `UP:` lines; JMeter runs the 17 samplers; final line `REGRESSION PASSED (0 failures). Report: .../index.html`; exit `0`. Confirm no failures in the dashboard's "Statistics" table.

- [ ] **Step 9: Verify the gate fails loudly on a regression**

Run (forces a failure by stopping one service):
```bash
docker-compose stop asapp-tasks-service
sh tools/jmeter/run-regression.sh --no-preflight   # skip pre-flight so JMeter actually runs and fails
echo "exit=$?"
docker-compose start asapp-tasks-service
```
Expected: `REGRESSION FAILED: <n> failed sample(s)/assertion(s)...` on stderr and `exit=1`. (Proves the gate is real.) Wait for the service to be healthy again before re-running Step 8.

- [ ] **Step 10: Commit**

```bash
git add tools/jmeter/env/local-docker.properties tools/jmeter/asapp-regression.jmx tools/jmeter/run-regression.sh
git commit -m "test(jmeter): add regression test plan and run scripts"
```
(Stage only those three files; never the generated `results/`.)

---

## Task 3: Stress plan + run-stress.sh

Implements spec §8 (nested-loop journey), §8.2–8.4 (structure/ids/assertions), §10 (run script). Reuses the proven element patterns from Task 2 (UDV bridge, per-pass User Parameters, Content-Type Header Manager, `+ Bearer` child header, JSON Extractors, raw bodies).

**Files:**
- Create: `tools/jmeter/asapp-stress.jmx` (GUI build-spec below)
- Create: `tools/jmeter/run-stress.sh`

### Build `asapp-stress.jmx`

- [ ] **Step 1: Test Plan + shared config + property-driven Thread Group**

```
Test Plan  "asapp-stress"
├─ [User Defined Variables] "props"   # SAME 13 entries as regression Step 2, PLUS the load knobs:
│     threads            = ${__P(threads,20)}
│     rampup             = ${__P(rampup,20)}
│     duration           = ${__P(duration,300)}
│     loops              = ${__P(loops,-1)}
│     users.per.pass.max = ${__P(users.per.pass.max,10)}
│     tasks.per.user.max = ${__P(tasks.per.user.max,10)}
│
└─ [Thread Group] "stress"
   │   Number of Threads = ${threads}
   │   Ramp-up (seconds) = ${rampup}
   │   Loop Count        = ${loops}          # -1 = infinite (bounded by scheduler); >=1 = bounded
   │   Scheduler         = ON
   │   Duration (seconds)= ${duration}
   │   Action on sampler error = Continue
   ├─ [User Parameters] "per-pass vars"  "Update Once Per Iteration" = ON
   │     username = loadtest_${__UUID}@asapp.test
   ├─ [HTTP Header Manager] "Content-Type"   Content-Type: application/json
   ├─ [Response Assertion] "2xx (light)"     Field=Response Code | Type=Matches | Pattern=2\d\d
   └─ (samplers/controllers 01..21 below)
```

The single thread-group-level Response Assertion applies to **every** sampler in scope (spec §8.4: light, no SLA). JSR223 pre/post-processors are not samplers, so they are not asserted.

- [ ] **Step 2: Auth setup (01–06)**

```
[HTTP Request] "01 register"  POST           # public
  Server=${auth.host} Port=${auth.port} Protocol=${auth.scheme}
  Path=${auth.context}/api/users
  Body(raw): {"username":"${username}","password":"${password}","role":"USER"}
  └ [JSR223 PreProcessor] "init pass lists"   Language=groovy   (script below — clears prior-pass ids)
  └ [JSON Extractor]  authUserId | $.userId | 1

[HTTP Request] "02 token"  POST              # public
  Path=${auth.context}/api/auth/token  (Server/Port/Protocol = auth.*)
  Body(raw): {"username":"${username}","password":"${password}"}
  └ [JSON Extractor]  accessToken  | $.accessToken  | 1
  └ [JSON Extractor]  refreshToken | $.refreshToken | 1

[HTTP Request] "03 get auth user"  GET  + Bearer
  Path=${auth.context}/api/users/${authUserId}  (auth.*)

[HTTP Request] "04 update auth user"  PUT  + Bearer
  Path=${auth.context}/api/users/${authUserId}  (auth.*)
  Body(raw): {"username":"${username}","password":"${password}","role":"USER"}

[HTTP Request] "05 get all auth users"  GET  + Bearer
  Path=${auth.context}/api/users  (auth.*)

[HTTP Request] "06 refresh"  POST            # public; overwrites token vars, used onward
  Path=${auth.context}/api/auth/refresh  (auth.*)
  Body(raw): {"refreshToken":"${refreshToken}"}
  └ [JSON Extractor]  accessToken  | $.accessToken  | 1
  └ [JSON Extractor]  refreshToken | $.refreshToken | 1
```

`[JSR223 PreProcessor] "init pass lists"` script (re-initializes the two thread-local id lists each pass so ForEach cleanup never touches already-deleted ids — spec §8.3):

```groovy
['taskIds','userIds'].each { p ->
    int n = (vars.get(p + 'Count') ?: '0') as int
    for (int i = 1; i <= n; i++) { vars.remove(p + '_' + i) }
    vars.put(p + 'Count', '0')
}
```

- [ ] **Step 3: Outer users loop (x) with inner tasks loop (y) — samplers 07–16**

```
[Loop Controller] "x: users loop"   Loop Count = ${__Random(1,${users.per.pass.max})}
├─ [HTTP Request] "07 create user"  POST  + Bearer
│    Path=${users.context}/api/users  (users.*)
│    Body(raw): {"firstName":"Load","lastName":"Test","email":"user_${__UUID}@asapp.test","phoneNumber":"666-555-444"}
│    └ [JSON Extractor]  userId | $.userId | 1
│    └ [JSR223 PostProcessor] "append userId"  groovy:
│         int n = (vars.get('userIdsCount') ?: '0') as int
│         n++; vars.put('userIdsCount', n.toString()); vars.put('userIds_' + n, vars.get('userId'))
├─ [HTTP Request] "08 get user (cold fan-out)"  GET  + Bearer
│    Path=${users.context}/api/users/${userId}  (users.*)
├─ [HTTP Request] "09 update user"  PUT  + Bearer
│    Path=${users.context}/api/users/${userId}  (users.*)
│    Body(raw): {"firstName":"Load","lastName":"Updated","email":"user_${__UUID}@asapp.test","phoneNumber":"666-555-444"}
├─ [HTTP Request] "10 get all users"  GET  + Bearer
│    Path=${users.context}/api/users  (users.*)
├─ [Loop Controller] "y: tasks loop"   Loop Count = ${__Random(1,${tasks.per.user.max})}
│   ├─ [HTTP Request] "11 create task"  POST  + Bearer
│   │    Path=${tasks.context}/api/tasks  (tasks.*)
│   │    Body(raw): {"userId":"${userId}","title":"task_${__threadNum}_${__Random(1,1000000)}","description":"load test task"}
│   │    └ [JSON Extractor]  taskId | $.taskId | 1
│   │    └ [JSR223 PostProcessor] "append taskId"  groovy:
│   │         int n = (vars.get('taskIdsCount') ?: '0') as int
│   │         n++; vars.put('taskIdsCount', n.toString()); vars.put('taskIds_' + n, vars.get('taskId'))
│   ├─ [HTTP Request] "12 get task"  GET  + Bearer
│   │    Path=${tasks.context}/api/tasks/${taskId}  (tasks.*)
│   ├─ [HTTP Request] "13 update task"  PUT  + Bearer
│   │    Path=${tasks.context}/api/tasks/${taskId}  (tasks.*)
│   │    Body(raw): {"userId":"${userId}","title":"task_${__threadNum}_upd_${__Random(1,1000000)}","description":"updated"}
│   └─ [HTTP Request] "14 tasks by ids"  GET  + Bearer
│        └ [JSR223 PreProcessor] "build ids csv (cap 50)"  groovy:
│             int n = (vars.get('taskIdsCount') ?: '0') as int
│             if (n < 1) { vars.put('taskIdsCsv',''); return }
│             int from = Math.max(1, n - 49)
│             def ids = (from..n).collect { vars.get('taskIds_' + it) }.findAll { it != null }
│             vars.put('taskIdsCsv', ids.join(','))
│        Path=${tasks.context}/api/tasks?ids=${taskIdsCsv}  (tasks.*)
├─ [HTTP Request] "15 tasks by user"  GET  + Bearer
│    Path=${tasks.context}/api/tasks/user/${userId}  (tasks.*)
└─ [HTTP Request] "16 get user (warm fan-out)"  GET  + Bearer
     Path=${users.context}/api/users/${userId}  (users.*)
```

- [ ] **Step 4: Once-per-pass read + self-cleaning teardown (17–21)**

```
[HTTP Request] "17 get all tasks"  GET  + Bearer
  Path=${tasks.context}/api/tasks  (tasks.*)

[ForEach Controller] "delete created tasks"
  Input variable prefix = taskIds
  Output variable name  = curTaskId
  "Add '_' before number?" = ON   (start/end index blank)
└─ [HTTP Request] "18 delete task"  DELETE  + Bearer
     Path=${tasks.context}/api/tasks/${curTaskId}  (tasks.*)

[ForEach Controller] "delete created users"
  Input variable prefix = userIds
  Output variable name  = curUserId
  "Add '_' before number?" = ON
└─ [HTTP Request] "19 delete user"  DELETE  + Bearer
     Path=${users.context}/api/users/${curUserId}  (users.*)

[HTTP Request] "20 delete auth user"  DELETE  + Bearer
  Path=${auth.context}/api/users/${authUserId}  (auth.*)

[HTTP Request] "21 revoke"  POST              # public; last auth action (logout)
  Path=${auth.context}/api/auth/revoke  (auth.*)
  Body(raw): {"accessToken":"${accessToken}"}
```

The ForEach controllers iterate `taskIds_1..N` / `userIds_1..N` (the `*Count` vars don't match the `*_<number>` pattern, so they're skipped). Each completed pass deletes everything it created → DB left as found (spec §8.2 loops-bounded mode). Save as `tools/jmeter/asapp-stress.jmx`.

- [ ] **Step 5: Write `run-stress.sh`**

`tools/jmeter/run-stress.sh` (observation run — no gate; same pre-flight + ensure-jmeter as regression):

```bash
#!/usr/bin/env bash
#
# Run the nested-loop stress plan against the running docker-compose stack and
# generate an HTML dashboard. Stress is for OBSERVATION (watch Grafana :3000) -
# correctness gating lives in run-regression.sh, so there is no .jtl gate here.
#
#   ./run-stress.sh
#   ./run-stress.sh --no-preflight
#   ./run-stress.sh -Jthreads=100 -Jduration=600 -Jusers.per.pass.max=8 -Jtasks.per.user.max=8
#   ./run-stress.sh -Jloops=5            # bounded, fully self-cleaning run
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/env/local-docker.properties"
PLAN="$SCRIPT_DIR/asapp-stress.jmx"
RESULTS_DIR="$SCRIPT_DIR/results"

preflight() {
  local urls=(
    "http://localhost:8090/asapp-authentication-service/readyz"
    "http://localhost:8091/asapp-tasks-service/readyz"
    "http://localhost:8092/asapp-users-service/readyz"
  )
  echo "Pre-flight: checking service readiness..." >&2
  local u
  for u in "${urls[@]}"; do
    if ! curl -fsS -o /dev/null --max-time 5 "$u"; then
      echo "ERROR: service not ready: $u" >&2
      echo "Bring the stack up first: docker-compose up -d" >&2
      exit 1
    fi
    echo "  UP: $u" >&2
  done
}

PREFLIGHT=1
if [ "${1:-}" = "--no-preflight" ]; then PREFLIGHT=0; shift; fi

JMETER_BIN="$("$SCRIPT_DIR/scripts/ensure-jmeter.sh")"
[ "$PREFLIGHT" -eq 1 ] && preflight

mkdir -p "$RESULTS_DIR"
TS="$(date +%Y%m%d-%H%M%S)"
JTL="$RESULTS_DIR/stress-$TS.jtl"
REPORT="$RESULTS_DIR/stress-$TS-report"

"$JMETER_BIN" -n \
  -t "$PLAN" \
  -q "$ENV_FILE" \
  -l "$JTL" \
  -e -o "$REPORT" \
  -Jjmeter.save.saveservice.output_format=csv \
  -Jjmeter.save.saveservice.print_field_names=true \
  "$@"

echo "STRESS run complete. Report: $REPORT/index.html"
echo "Watch live metrics in Grafana: http://localhost:3000"
```

- [ ] **Step 6: Verify a short bounded run is clean and self-cleaning** *(requires the stack)*

Run a tiny bounded run (so it completes fast and leaves the DB as found):
```bash
sh tools/jmeter/run-stress.sh -Jthreads=2 -Jloops=1 -Jduration=60 -Jusers.per.pass.max=2 -Jtasks.per.user.max=2
```
Expected: pre-flight `UP:` lines; JMeter runs; final lines print the report path + Grafana URL; exit `0`. Open the dashboard and confirm the error % is ~0 in the Statistics table.

- [ ] **Step 7: Verify the bounded run left no orphans**

Run (each thread deletes everything it created; a loop-bounded run should net zero new rows — query the count of load users):
```bash
sh tools/jmeter/run-regression.sh --no-preflight >/dev/null 2>&1 || true   # ensure a token-capable path exists
# Inspect via the API instead of the DB: list all auth users and confirm none are leftover loadtest_*.
# (Simplest check: re-run the bounded stress and confirm it never errors on the unique-username constraint.)
sh tools/jmeter/run-stress.sh -Jthreads=2 -Jloops=1 -Jduration=60 -Jusers.per.pass.max=2 -Jtasks.per.user.max=2
```
Expected: the second bounded run also completes with ~0 errors (no `409`/`400` from a username collision — every pass mints a unique `loadtest_<uuid>`). If orphans ever accumulate from interrupted duration-bounded runs, reset with `docker-compose down -v` (documented in the README).

- [ ] **Step 8: Commit**

```bash
git add tools/jmeter/asapp-stress.jmx tools/jmeter/run-stress.sh
git commit -m "test(jmeter): add stress test plan and run scripts"
```

---

## Task 4: Gitignore runtime and results

Implements spec §11.

**Files:**
- Modify: `.gitignore` (append a section)

- [ ] **Step 1: Append the ignore rules**

Add to the end of `.gitignore` (after the `### Claude Code ###` block):

```gitignore

### JMeter load tests ###
tools/jmeter/.runtime/
tools/jmeter/results/
```

- [ ] **Step 2: Verify the generated dirs are now ignored**

Run:
```bash
git status --short tools/jmeter/
git check-ignore tools/jmeter/.runtime tools/jmeter/results
```
Expected: `git status` shows no `.runtime/` or `results/` entries (only tracked sources, if any unstaged); `git check-ignore` echoes both paths (confirming they match an ignore rule).

- [ ] **Step 3: Commit**

```bash
git add .gitignore
git commit -m "chore(jmeter): gitignore runtime and results"
```

---

## Task 5: Tool README + root pointer

Implements spec §11 (tool README; root README pointer).

**Files:**
- Create: `tools/jmeter/README.md`
- Modify: `README.md` (Project Structure tree + a Load Testing note under Development)

- [ ] **Step 1: Write `tools/jmeter/README.md`**

```markdown
# JMeter load tests

Two standalone [Apache JMeter](https://jmeter.apache.org/) plans that run from the CLI against the
running `docker-compose` stack. They live **outside** the Maven build and CI — nothing here runs
during `mvn verify`.

- **`asapp-regression.jmx`** — a single deterministic pass over every functional endpoint, with
  correctness assertions. The automated pre-release go/no-go gate (replaces the old manual
  click-through).
- **`asapp-stress.jmx`** — the same comprehensive read+write journey run by many concurrent threads
  (nested random-count loops scale the data volume), for observation in Grafana.

## Prerequisites

- The full stack is up: `docker-compose up -d` (build images first with `mvn spring-boot:build-image`
  if the `0.4.0-SNAPSHOT` images aren't present). All services must be healthy.
- **Internet on first run only** — the run scripts auto-download a pinned JMeter into `.runtime/`
  (gitignored) and verify its SHA-512. Subsequent runs reuse the cached engine.
- Java is already required by the project; no separate JMeter install is needed.
- Scripts are bash; on Windows run them via Git for Windows' bundled `sh` (the same `sh` that runs
  the git hooks), e.g. `sh tools/jmeter/run-regression.sh`.

## Run

```bash
# Pre-release gate: deterministic full journey, asserts correctness, exits non-zero on any failure.
sh tools/jmeter/run-regression.sh

# Tunable concurrent load for observation (default: 20 threads for 300s).
sh tools/jmeter/run-stress.sh
```

Both scripts pre-flight the stack by polling each service's `/readyz` probe and abort if any is down.
Skip it with `--no-preflight`.

## Tune

All knobs are JMeter properties (defaults in `env/local-docker.properties`); override per run with
`-J<name>=<value>` — extra args are forwarded straight to JMeter:

```bash
sh tools/jmeter/run-stress.sh -Jthreads=100 -Jduration=600 -Jusers.per.pass.max=8 -Jtasks.per.user.max=8
sh tools/jmeter/run-stress.sh -Jloops=5    # bounded, fully self-cleaning run (leaves DB as found)
```

| Property | Default | Meaning |
|---|---|---|
| `threads` | `20` | concurrent virtual users (stress) |
| `rampup` | `20` | ramp-up seconds (stress) |
| `duration` | `300` | seconds the threads keep looping the journey |
| `loops` | `-1` | loop cap per thread; `-1` = run for the full `duration`; `>=1` = bounded, fully self-cleaning |
| `users.per.pass.max` | `10` | owner users per pass = `__Random(1, this)` |
| `tasks.per.user.max` | `10` | tasks per owner user = `__Random(1, this)` |
| `password` | `L0adT3st!Pass` | password for all load users |
| `auth.* / tasks.* / users.*` | localhost / docker ports | per-service `scheme`/`host`/`port`/`context` |

## Reports

Each run writes a timestamped `.jtl` log and an HTML dashboard under `results/` (gitignored):

```
results/regression-<timestamp>-report/index.html
results/stress-<timestamp>-report/index.html
```

Open `index.html` in a browser. During a stress run, watch live metrics in **Grafana**
(http://localhost:3000, admin/secret) — request rate, latency, JVM/DB pool metrics.

## Run modes & cleanup

- A **loops-bounded** stress run (`-Jloops=N`, every pass completes) and the regression run leave the
  databases exactly as they found them.
- A **duration-bounded** run (`loops=-1`) is cut off when time elapses, so the final in-flight pass
  per thread may leave orphan rows — all uniquely named (`loadtest_<uuid>`), so re-runs never collide.
  Reset volumes occasionally with `docker-compose down -v` if they accumulate.

## Authoring / debugging the plans

The same downloaded engine can be launched in **GUI mode** to edit the `.jmx` files. Provision it,
then open it with the env properties loaded:

```bash
JMETER_BIN="$(sh tools/jmeter/scripts/ensure-jmeter.sh)"
"$JMETER_BIN" -q tools/jmeter/env/local-docker.properties        # macOS/Linux/Git-Bash
# Windows: tools\jmeter\.runtime\apache-jmeter-5.6.3\bin\jmeter.bat -q tools\jmeter\env\local-docker.properties
```

## Upgrading JMeter

Edit the three lines in `lib/jmeter-version.properties` (version, URL, SHA-512 from the published
`apache-jmeter-<version>.zip.sha512`), delete `.runtime/`, and re-run — the script re-downloads and
re-verifies.
```

- [ ] **Step 2: Fix and extend the root README Project Structure tree**

In `README.md`, replace the malformed `tools/` block (currently a `└──` followed by a `├──`):

```
├── tools/                                   # Monitoring tools
│   └── grafana/                             # Grafana dashboards
│   ├── prometheus/                          # Prometheus config
```

with:

```
├── tools/                                   # Monitoring & load-testing tools
│   ├── grafana/                             # Grafana dashboards
│   ├── prometheus/                          # Prometheus config
│   └── jmeter/                              # JMeter load tests (regression + stress)
```

- [ ] **Step 3: Add a Load Testing note under Development**

In `README.md`, immediately after the `### Generate Documentation` subsection (the fenced block ending with `mvn clean verify -Pfull`) and before the `---` that closes the Development section, insert:

```markdown
### Load Testing

JMeter regression and stress plans run against the running Docker stack, outside the Maven build and CI:

\`\`\`bash
# Pre-release go/no-go gate (deterministic full journey, asserts correctness)
sh tools/jmeter/run-regression.sh

# Tunable concurrent load to watch in Grafana (http://localhost:3000)
sh tools/jmeter/run-stress.sh -Jthreads=50 -Jduration=600
\`\`\`

See [tools/jmeter/README.md](tools/jmeter/README.md) for prerequisites, tuning, and where reports land.
```

(Write the fenced block with real backticks, not the escaped `\`` shown here.)

- [ ] **Step 4: Verify the README renders sanely**

Run:
```bash
git diff -- README.md
```
Expected: the `tools/` tree now lists `grafana`, `prometheus`, `jmeter` with aligned `├──`/`└──` glyphs, and a new `### Load Testing` subsection appears under Development with a working relative link to `tools/jmeter/README.md`.

- [ ] **Step 5: Commit**

```bash
git add tools/jmeter/README.md README.md
git commit -m "docs(jmeter): add tools/jmeter README and root pointer"
```

---

## Task 6: Mark the TODO item done

Implements spec §11 (TODO housekeeping).

**Files:**
- Modify: `TODO.md` (line 25 under v0.4.0 → Quick Wins → Technical Improvements)

- [ ] **Step 1: Tick the checkbox**

In `TODO.md`, change:

```markdown
    * [ ] Add load test with JMeter
```

to:

```markdown
    * [X] Add load test with JMeter
```

- [ ] **Step 2: Verify exactly one line changed**

Run:
```bash
git diff -- TODO.md
```
Expected: a single `-`/`+` pair flipping `[ ]` → `[X]` on the "Add load test with JMeter" line; no other edits. (Note: `TODO.md` may already have unrelated working-tree changes from earlier — stage only this line's change is not possible at file granularity, so confirm the only difference vs. the intended baseline is this checkbox before committing.)

- [ ] **Step 3: Commit**

```bash
git add TODO.md
git commit -m "docs(todo): mark JMeter load test as done"
```

---

## Task 7: Reviews, full validation, and PR

Implements spec §12 (validation) and §14 (review + PR).

- [ ] **Step 1: Run the full validation matrix** *(requires the stack)*

```bash
# Provisioning: tamper test — a corrupted checksum must abort.
cp tools/jmeter/lib/jmeter-version.properties /tmp/jmeter-version.bak
sed -i 's/^JMETER_SHA512=.*/JMETER_SHA512=deadbeef/' tools/jmeter/lib/jmeter-version.properties
rm -rf tools/jmeter/.runtime
sh tools/jmeter/scripts/ensure-jmeter.sh; echo "exit=$?"      # expect: SHA-512 mismatch + exit=1
cp /tmp/jmeter-version.bak tools/jmeter/lib/jmeter-version.properties
sh tools/jmeter/scripts/ensure-jmeter.sh >/dev/null && echo "re-provision OK"

# Regression: clean pass on a healthy stack.
sh tools/jmeter/run-regression.sh                            # expect: REGRESSION PASSED (0 failures)

# Stress: default-shaped run produces a dashboard; load visible in Grafana.
sh tools/jmeter/run-stress.sh -Jduration=60                  # short smoke; expect report path printed
```
Expected: tamper run prints the SHA-512 mismatch and `exit=1`; re-provision succeeds; regression passes with 0 failures; stress produces `results/stress-<ts>-report/index.html` and traffic shows in Grafana. Confirm `tools/jmeter/.runtime/` and `tools/jmeter/results/` remain untracked (`git status --short tools/jmeter/`).

- [ ] **Step 2: Dispatch the reviewers against the branch diff**

Per spec §14 and the project's subagent roster (`.claude/agents/`), dispatch in parallel:
- **`devops-engineer`** — review the provisioning + run scripts (`ensure-jmeter.sh`, `run-regression.sh`, `run-stress.sh`, `lib/jmeter-version.properties`): checksum/download robustness, portability of the `sh` scripts on Git-Bash, the `.jtl` gate correctness, `.gitignore` coverage.
- **`architect-reviewer`** — review overall fit: layout under `tools/`, the out-of-Maven/CI boundary, the regression-vs-stress responsibility split, and any drift from the approved spec (especially the §"Spec reconciliation notes" above).

Address findings before opening the PR. Re-run Step 1's regression check if scripts or plans change.

- [ ] **Step 3: Open the PR**

```bash
git push -u origin setup-load-test
gh pr create --base main --title "test(jmeter): add standalone JMeter regression and stress load tests" --body "<conventional-commits body per the commit-msg skill>"
```
PR body: bulleted summary of the six commits (provisioning, regression plan + gate, stress plan, gitignore, docs, TODO), the out-of-CI boundary, and the documented spec reconciliations. Follow Conventional Commits + the bulleted-body format from the `commit-msg` skill.

---

## Self-review (author checklist — already applied)

- **Spec coverage:** §4 layout → Tasks 1–6 + Task 4 ignores; §5 provisioning → Task 1; §6 config/shared elements → Task 2 Step 1–2 + conventions; §7 regression journey (17 steps) → Task 2 Steps 3–6; §8 stress journey (21 steps, nested loops, ForEach, id accumulation) → Task 3; §8.3 per-pass re-init → JSR223 "init pass lists"; §8.4 light 2xx assertion → Task 3 Step 1; §9 token lifecycle → regression refresh/revoke (steps 8/16/17) + stress per-pass mint/refresh/revoke; §10 run scripts + pre-flight + `-e -o` reporting + `.jtl` gate → Task 2 Step 7 / Task 3 Step 5; §11 README/gitignore/TODO/root pointer → Tasks 4–6; §12 validation → Task 7 Step 1 + Task 2 Steps 8–9 + Task 3 Steps 6–7; §13 out-of-scope honored (no CI, no maven plugin, core JMeter only); §14 commits/reviews/PR → commit steps + Task 7.
- **Placeholder scan:** real SHA-512, real URLs/ports/paths, verbatim bodies, verbatim Groovy, complete scripts. No "TBD"/"add error handling"/"similar to".
- **Type/name consistency:** id field names match the codebase (`userId`, `taskId`, `accessToken`, `refreshToken`); JMeter vars consistent across tasks (`authUserId`, `userId`, `taskId`, `accessToken`, `refreshToken`, `username`, `userEmail`, `taskTitle`, `taskTitleUpd`, accumulation `taskIds_N`/`taskIdsCount`, `userIds_N`/`userIdsCount`, `curTaskId`/`curUserId`, `taskIdsCsv`); `ids` query param verbatim; `+ Bearer` applied only to protected requests.
- **Reconciliations documented:** create/update return only `{id}` → assertions adapted; array checks pragmatic; `ids` capped at 50; selective `git add` to respect the spec's late gitignore commit.
```