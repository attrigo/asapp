# JMeter load tests

Three standalone [Apache JMeter](https://jmeter.apache.org/) plans that run from the CLI against the running `docker-compose` stack (outside the Maven build and
CI: nothing here runs during `mvn verify`).

- **`asapp-regression.jmx`**: a single deterministic pass over every functional endpoint, with correctness assertions. The automated pre-release go/no-go gate (
  replaces the old manual click-through).
- **`asapp-stress.jmx`**: the same comprehensive read+write journey run by many concurrent threads (nested random-count loops scale the data volume), for
  observation in Grafana. The default stack is `docker,dev`, where the BootUI SQL-trace adds minor per-query overhead (as Swagger and DEBUG logging do); for
  representative numbers run stress against a `docker,prod` stack, where BootUI is force-disabled.
- **`asapp-tooling-exposure.jmx`**: a black-box check that the project tooling (Swagger UI, OpenAPI docs, Actuator, BootUI console) is exposed as the active
  environment profile dictates — `--profile dev` asserts it is ON (docs reachable, full Actuator, `dev` active, BootUI console reachable), `--profile prod` asserts
  it is locked down (docs 404, narrow Actuator, `env` 404, BootUI console 401).

## Layout

```
tools/jmeter/
├── asapp-regression.jmx        # pre-release gate plan (business journey)
├── asapp-stress.jmx            # concurrent load plan
├── asapp-tooling-exposure.jmx  # per-profile tooling exposure plan
├── run-regression.sh           # entrypoint: regression
├── run-stress.sh               # entrypoint: stress
├── run-tooling-exposure.sh     # entrypoint: tooling exposure
├── env/
│   └── local.properties        # default tunables (-J overrides)
└── scripts/                    # shared internals
    ├── common.sh               # paths, logging, preflight, run_plan, evaluate_gate
    ├── ensure-jmeter.sh        # download + SHA-512 verify engine
    ├── resolve-java.sh         # locate/validate Java 17/21
    └── jmeter-version.properties  # pinned version/URL/sum
```

## Prerequisites

- The full stack is up and healthy: `docker-compose up -d` (build images first with `mvn spring-boot:build-image` if images aren't present).
- **Internet on first run only**: the run scripts auto-download a pinned JMeter into `.runtime/` (gitignored) and verify its SHA-512. Subsequent runs reuse the
  cached engine.
- No separate JMeter install is needed.
- **The stress plan needs Java 17 or 21.** JMeter 5.6.3 bundles Groovy 3.0.20, which cannot run on newer JVMs. Point JMeter at a 17/21 JDK with the
  `--java-home` flag (falls back to `JAVA_HOME`); `run-stress.sh` validates this and fails fast with guidance.
- **`run-tooling-exposure.sh --profile prod` needs a locked-down stack**: start it with `SPRING_PROFILES_ACTIVE=…,prod` before running.
- Scripts are bash; on Windows run them via Git for Windows' bundled `bash`.

## Run

Run these from inside the `tools/jmeter/` directory:

```bash
cd tools/jmeter

# Pre-release gate: deterministic full journey, asserts correctness, exits non-zero on any failure.
./run-regression.sh

# Tunable concurrent load for observation (default: 20 threads for 300s).
# Stress needs Java 17/21 (see Prerequisites); pass the JDK with --java-home or rely on JAVA_HOME:
./run-stress.sh --java-home '/path/to/jdk-17'

# Per-profile tooling exposure (dev = open tooling, prod = locked down).
# The stack must be running with the matching profile (default compose stack = docker,dev):
./run-tooling-exposure.sh                  # asserts the dev posture
./run-tooling-exposure.sh --profile prod   # against a docker,prod stack
```

```bash
# Skip the /readyz pre-flight checks (both scripts poll each service first and abort if any is down):
./run-regression.sh --no-preflight

# See the full option list (either script):
./run-stress.sh --help
```

### Tune

All knobs are JMeter properties (defaults in `env/local.properties`); override per run with `-J<name>=<value>`.

Extra `-J`/`-G`/`-D` args pass straight to JMeter; an unknown `--option` is rejected (to pass a JMeter long option, put it after a `--`):

```bash
./run-stress.sh -Jthreads=100 -Jduration=600 -Jusers.per.pass.max=8 -Jtasks.per.user.max=8
./run-stress.sh -Jloops=5    # bounded, fully self-cleaning run (leaves DB as found)
```

| Property                        | Default                  | Meaning                                                                                       |
|---------------------------------|--------------------------|-----------------------------------------------------------------------------------------------|
| `threads`                       | `20`                     | concurrent virtual users (stress)                                                             |
| `rampup`                        | `20`                     | ramp-up seconds (stress)                                                                      |
| `duration`                      | `300`                    | seconds the threads keep looping the journey                                                  |
| `loops`                         | `-1`                     | loop cap per thread; `-1` = run for the full `duration`; `>=1` = bounded, fully self-cleaning |
| `users.per.pass.max`            | `10`                     | owner users per pass = `__Random(1, this)`                                                    |
| `tasks.per.user.max`            | `10`                     | tasks per owner user = `__Random(1, this)`                                                    |
| `password`                      | `L0adT3st!Pass`          | password for all load users                                                                   |
| `auth.* / tasks.* / users.*`    | localhost / docker ports | per-service `scheme`/`host`/`port`/`context`                                                  |
| `profile`                       | `dev`                    | tooling-exposure: environment profile to assert (`dev`/`prod`); prefer `--profile`            |
| `<svc>.mgmt.port`               | `8090` / `8091` / `8092` | per-service Actuator management port (tooling-exposure plan)                                  |
| `mgmt.username / mgmt.password` | `user` / `secret`        | Actuator Basic-auth credentials (tooling-exposure plan)                                       |

A loops-bounded run (`-Jloops=N`) and the regression run leave the databases exactly as they found them. A duration-bounded run (`loops=-1`) may leave orphan
rows from the final in-flight pass; reset volumes occasionally with `docker-compose down -v` if they accumulate.

## Observability

During a stress run, watch live metrics in **Grafana** (http://localhost:3000, admin/secret): request rate, latency, JVM/DB pool metrics.

## Reports

Each run writes three timestamped artifacts under `results/` (gitignored), where `<plan>` is `regression` or `stress`:

```
results/<plan>-<timestamp>.jtl          # raw sample results (CSV)
results/<plan>-<timestamp>.log          # JMeter engine log
results/<plan>-<timestamp>-report/      # HTML dashboard
```

Open the report's `index.html` in a browser; check the `.log` to diagnose a failed or empty run.

## Authoring / debugging the plans

The same downloaded engine can be launched in **GUI mode** to edit the `.jmx` files. Provision it, then open it with the env properties loaded (run from `tools/jmeter/`):

```bash
JMETER_BIN="$(./scripts/ensure-jmeter.sh)"
"$JMETER_BIN" -q env/local.properties
```

When authoring or running the **stress** plan in the GUI, launch it under Java 17/21 too; the same Groovy constraint applies (see Prerequisites).

## Upgrading JMeter

Edit the three lines in `scripts/jmeter-version.properties` (version, URL, SHA-512 from the published `apache-jmeter-<version>.zip.sha512`), delete `.runtime/`,
and re-run; the script re-downloads and re-verifies.
