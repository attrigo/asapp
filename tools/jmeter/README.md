# JMeter load tests

Two standalone [Apache JMeter](https://jmeter.apache.org/) plans that run from the CLI against the running `docker-compose` stack (outside the Maven build and
CI: nothing here runs during `mvn verify`).

- **`asapp-regression.jmx`**: a single deterministic pass over every functional endpoint, with correctness assertions. The automated pre-release go/no-go gate (
  replaces the old manual click-through).
- **`asapp-stress.jmx`**: the same comprehensive read+write journey run by many concurrent threads (nested random-count loops scale the data volume), for
  observation in Grafana.

```
tools/jmeter/
├── asapp-regression.jmx
├── asapp-stress.jmx
├── env/local.properties
├── scripts/ensure-jmeter.sh
├── scripts/resolve-java.sh
├── scripts/jmeter-version.properties
├── run-regression.sh
└── run-stress.sh
```

## Prerequisites

- The full stack is up and healthy: `docker-compose up -d` (build images first with `mvn spring-boot:build-image` images aren't present).
- **Internet on first run only**: the run scripts auto-download a pinned JMeter into `.runtime/` (gitignored) and verify its SHA-512. Subsequent runs reuse the
  cached engine.
- No separate JMeter install is needed.
- **The stress plan needs Java 17 or 21.** JMeter 5.6.3 bundles Groovy 3.0.20, which cannot run on newer JVMs; its JSR223/Groovy scripts fail with an
  `Unsupported class file major version` error. Point JMeter at a 17/21 JDK via `JMETER_JAVA_HOME` (leaving your project `JAVA_HOME` untouched); `run-stress.sh`
  validates this and fails fast with guidance.
- Scripts are bash; on Windows run them via Git for Windows' bundled `bash`.

## Run

```bash
# Pre-release gate: deterministic full journey, asserts correctness, exits non-zero on any failure.
bash tools/jmeter/run-regression.sh

# Tunable concurrent load for observation (default: 20 threads for 300s).
# Stress needs Java 17/21 (see Prerequisites); point JMeter at it once per shell:
export JMETER_JAVA_HOME='C:\Program Files\Java\jdk-17.0.12'   # Windows example path
bash tools/jmeter/run-stress.sh
```

Both scripts pre-flight the stack by polling each service's `/readyz` probe and abort if any is down. Skip it with `--no-preflight`.

## Tune

All knobs are JMeter properties (defaults in `env/local.properties`); override per run with `-J<name>=<value>`; extra args are forwarded straight to JMeter:

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

The same downloaded engine can be launched in **GUI mode** to edit the `.jmx` files. Provision it, then open it with the env properties loaded:

```bash
JMETER_BIN="$(bash tools/jmeter/scripts/ensure-jmeter.sh)"
"$JMETER_BIN" -q tools/jmeter/env/local.properties
```

When authoring or running the **stress** plan in the GUI, launch it under Java 17/21 too; the same Groovy constraint applies (see Prerequisites).

## Upgrading JMeter

Edit the three lines in `scripts/jmeter-version.properties` (version, URL, SHA-512 from the published `apache-jmeter-<version>.zip.sha512`), delete `.runtime/`,
and re-run; the script re-downloads and re-verifies.
