#!/usr/bin/env bash
#
# Run the nested-loop stress plan against the running docker-compose stack and
# generate an HTML dashboard. Stress is for OBSERVATION (watch Grafana :3000) -
# correctness gating lives in run-regression.sh, so there is no .jtl gate here.
#
#   ./run-stress.sh
#   ./run-stress.sh --java-home '/path/to/jdk-17'
#   ./run-stress.sh --no-preflight
#   ./run-stress.sh -Jthreads=100 -Jduration=600 -Jusers.per.pass.max=8 -Jtasks.per.user.max=8
#   ./run-stress.sh -Jloops=5            # bounded, fully self-cleaning run
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/common.sh
. "$SCRIPT_DIR/scripts/common.sh"   # paths (ENV_FILE/RESULTS_DIR/SCRIPTS_DIR) + preflight() + run_plan()

# validate_java_home <value>: the --java-home flag needs a non-empty path argument
# (the JDK itself is validated later by resolve-java.sh).
validate_java_home() {
  [[ -n "${1:-}" ]] || { echo "ERROR: --java-home requires a path" >&2; exit 1; }
}

# Script flags (must precede any JMeter -J args, which are forwarded as-is):
#   --no-preflight       skip the readiness checks
#   --java-home <path>   JDK for JMeter (Java 17/21); falls back to JAVA_HOME
PREFLIGHT=1
JAVA_HOME_OPT=""
while [[ "${1:-}" == --* ]]; do
  case "$1" in
    --no-preflight) PREFLIGHT=0; shift ;;
    --java-home)    validate_java_home "${2:-}"; JAVA_HOME_OPT="$2"; shift 2 ;;
    --)             shift; break ;;
    *)              break ;;  # unknown --flag: leave it for JMeter
  esac
done

# Steps traced as [x/y]: resolve Java, provision, [pre-flight], run.
STEP_TOTAL=$(( PREFLIGHT == 1 ? 4 : 3 ))

# The stress plan uses JSR223/Groovy; JMeter's bundled Groovy 3.0.20 cannot run on a
# JDK newer than Java 21. Resolve & validate a Java 17/21 JVM (--java-home or JAVA_HOME)
# and export JAVA_HOME so the JMeter launcher below uses it. Validate before the download
# so a misconfigured JVM aborts instantly.
. "$SCRIPT_DIR/scripts/resolve-java.sh" "$JAVA_HOME_OPT"

log_step "Provisioning JMeter"
JMETER_BIN="$("$SCRIPTS_DIR/ensure-jmeter.sh")"

(( PREFLIGHT == 1 )) && preflight

echo "Watch live metrics in Grafana: http://localhost:3000"
run_plan stress "$SCRIPT_DIR/asapp-stress.jmx" "$@"

echo "STRESS run complete. Report: $REPORT/index.html"
