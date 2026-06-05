# JMeter load tests

Two standalone [Apache JMeter](https://jmeter.apache.org/) plans that run from the CLI against the running `docker-compose` stack. They live **outside** the
Maven build and CI nothing here runs during `mvn verify`.

- **`asapp-regression.jmx`** — a single deterministic pass over every functional endpoint, with correctness assertions. The automated pre-release go/no-go
  gate (replaces the old manual click-through).
- **`asapp-stress.jmx`** — the same comprehensive read+write journey run by many concurrent threads (nested random-count loops scale the data volume), for
  observation in Grafana.

```
tools/jmeter/
├── asapp-regression.jmx   # deterministic gate plan
├── asapp-stress.jmx        # nested-loop load plan
├── env/local.properties  # service URLs and load knobs
├── scripts/ensure-jmeter.sh  # auto-download on first run
├── scripts/jmeter-version.properties  # pinned JMeter version + SHA-512
├── run-regression.sh       # pre-release gate runner
└── run-stress.sh           # observation runner
```

## Prerequisites

- The full stack is up: `docker-compose up -d` (build images first with `mvn spring-boot:build-image` if the `0.4.0-SNAPSHOT` images aren't present). All
  services must be healthy.
- **Internet on first run only** — the run scripts auto-download a pinned JMeter into `.runtime/` (gitignored) and verify its SHA-512. Subsequent runs reuse the
  cached engine.
- Java is already required by the project; no separate JMeter install is needed.
- Scripts are bash; on Windows run them via Git for Windows' bundled `bash` (the same `bash` that runs the git hooks), e.g.
  `bash tools/jmeter/run-regression.sh`.

## Run

```bash
# Pre-release gate: deterministic full journey, asserts correctness, exits non-zero on any failure.
bash tools/jmeter/run-regression.sh

# Tunable concurrent load for observation (default: 20 threads for 300s).
bash tools/jmeter/run-stress.sh
```

Both scripts pre-flight the stack by polling each service's `/readyz` probe and abort if any is down.
Skip it with `--no-preflight`.

## Tune

All knobs are JMeter properties (defaults in `env/local.properties`); override per run with `-J<name>=<value>` — extra args are forwarded straight to JMeter:

```bash
bash tools/jmeter/run-stress.sh -Jthreads=100 -Jduration=600 -Jusers.per.pass.max=8 -Jtasks.per.user.max=8
bash tools/jmeter/run-stress.sh -Jloops=5    # bounded, fully self-cleaning run (leaves DB as found)
```

| Property                     | Default                  | Meaning                                                                                       |
|------------------------------|--------------------------|-----------------------------------------------------------------------------------------------|
| `threads`                    | `20`                     | concurrent virtual users (stress)                                                             |
| `rampup`                     | `20`                     | ramp-up seconds (stress)                                                                      |
| `duration`                   | `300`                    | seconds the threads keep looping the journey                                                  |
| `loops`                      | `-1`                     | loop cap per thread; `-1` = run for the full `duration`; `>=1` = bounded, fully self-cleaning |
| `users.per.pass.max`         | `10`                     | owner users per pass = `__Random(1, this)`                                                    |
| `tasks.per.user.max`         | `10`                     | tasks per owner user = `__Random(1, this)`                                                    |
| `password`                   | `L0adT3st!Pass`          | password for all load users                                                                   |
| `auth.* / tasks.* / users.*` | localhost / docker ports | per-service `scheme`/`host`/`port`/`context`                                                  |

## Reports

Each run writes a timestamped `.jtl` log and an HTML dashboard under `results/` (gitignored):

```
results/regression-<timestamp>-report/index.html
results/stress-<timestamp>-report/index.html
```

Open `index.html` in a browser. During a stress run, watch live metrics in **Grafana** (http://localhost:3000, admin/secret) — request rate, latency, JVM/DB
pool metrics.

## Run modes & cleanup

- A **loops-bounded** stress run (`-Jloops=N`, every pass completes) and the regression run leave the databases exactly as they found them.
- A **duration-bounded** run (`loops=-1`) is cut off when time elapses, so the final in-flight pass per thread may leave orphan rows — all uniquely named (
  `loadtest_<uuid>`), so re-runs never collide.
  Reset volumes occasionally with `docker-compose down -v` if they accumulate.

## Authoring / debugging the plans

The same downloaded engine can be launched in **GUI mode** to edit the `.jmx` files. Provision it, then open it with the env properties loaded:

```bash
JMETER_BIN="$(bash tools/jmeter/scripts/ensure-jmeter.sh)"
"$JMETER_BIN" -q tools/jmeter/env/local.properties        # macOS/Linux/Git-Bash
# Windows: tools\jmeter\.runtime\apache-jmeter-5.6.3\bin\jmeter.bat -q tools\jmeter\env\local.properties
```

## Upgrading JMeter

Edit the three lines in `scripts/jmeter-version.properties` (version, URL, SHA-512 from the published `apache-jmeter-<version>.zip.sha512`), delete `.runtime/`,
and re-run — the script re-downloads and re-verifies.
