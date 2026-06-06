#!/usr/bin/env bash
#
# Run the nested-loop stress plan against the running docker-compose stack and generate an HTML dashboard.
# Stress is for OBSERVATION (watch Grafana :3000) - correctness gating lives in run-regression.sh, so there is no .jtl gate here.
#
# Run with --help for usage.
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# paths (ENV_FILE/RESULTS_DIR/SCRIPTS_DIR) + log + preflight() + run_plan()
. "$SCRIPT_DIR/scripts/common.sh"

validate_java_home() {
  [[ -n "${1:-}" ]] || { echo "ERROR: --java-home requires a path" >&2; exit 1; }
}

usage() {
  cat <<'EOF'
Usage: run-stress.sh [--java-home <path>] [--no-preflight] [-J<prop>=<val> ...]

Run the nested-loop stress plan against the running stack for OBSERVATION
(watch Grafana at http://localhost:3000). No pass/fail gate - that lives in
run-regression.sh.

Options (must precede any -J args):
  --java-home <path>   JDK 17/21 for JMeter; falls back to JAVA_HOME
  --no-preflight       skip the service readiness checks
  --help               show this help and exit

Any -J<name>=<value> is forwarded to JMeter (defaults in env/local.properties):
  threads, rampup, duration, loops, users.per.pass.max, tasks.per.user.max

Examples:
  run-stress.sh --java-home '/path/to/jdk-17'
  run-stress.sh -Jthreads=100 -Jduration=600
  run-stress.sh -Jloops=5            # bounded, fully self-cleaning run
EOF
}

PREFLIGHT=1
JAVA_HOME_OPT=""
while [[ "${1:-}" == --* ]]; do
  case "$1" in
    --help)         usage; exit 0 ;;
    --no-preflight) PREFLIGHT=0; shift ;;
    --java-home)    validate_java_home "${2:-}"; JAVA_HOME_OPT="$2"; shift 2 ;;
    --)             shift; break ;;
    *)              echo "WARNING: unknown option '$1'; forwarding it to JMeter" >&2; break ;;
  esac
done

# Steps traced as [x/y]: resolve Java, provision, [pre-flight], run.
STEP_TOTAL=$(( PREFLIGHT == 1 ? 4 : 3 ))

# JMeter's bundled Groovy 3.0.20 cannot run on a JDK newer than Java 21.
# Resolve & validate a Java 17/21 JVM (--java-home or JAVA_HOME) and export JAVA_HOME so the
# JMeter launcher below uses it.
# Validate before the download so a misconfigured JVM aborts instantly.
. "$SCRIPT_DIR/scripts/resolve-java.sh" "$JAVA_HOME_OPT"

log_step "Provisioning JMeter"
JMETER_BIN="$("$SCRIPTS_DIR/ensure-jmeter.sh")"

(( PREFLIGHT == 1 )) && preflight

log_detail "Watch live metrics in Grafana: http://localhost:3000"

run_plan stress "$SCRIPT_DIR/asapp-stress.jmx" "$@"

log_detail "STRESS run complete."
log_detail "Report: $REPORT/index.html"
