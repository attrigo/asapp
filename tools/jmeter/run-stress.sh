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
ENV_FILE="$SCRIPT_DIR/env/local.properties"
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

# The stress plan uses JSR223/Groovy; JMeter's bundled Groovy 3.0.20 cannot run on
# Java 25. Resolve & validate a Java 17/21 JVM (JMETER_JAVA_HOME or JAVA_HOME) and
# export JAVA_HOME so the JMeter launcher below uses it. Validate before the download
# so a misconfigured JVM aborts instantly.
. "$SCRIPT_DIR/scripts/resolve-java.sh"

JMETER_BIN="$("$SCRIPT_DIR/scripts/ensure-jmeter.sh")"
[ "$PREFLIGHT" -eq 1 ] && preflight

mkdir -p "$RESULTS_DIR"
TS="$(date +%Y%m%d-%H%M%S)"
JTL="$RESULTS_DIR/stress-$TS.jtl"
LOG="$RESULTS_DIR/stress-$TS.log"
REPORT="$RESULTS_DIR/stress-$TS-report"

"$JMETER_BIN" -n \
  -t "$PLAN" \
  -q "$ENV_FILE" \
  -l "$JTL" \
  -j "$LOG" \
  -e -o "$REPORT" \
  -Jjmeter.save.saveservice.output_format=csv \
  -Jjmeter.save.saveservice.print_field_names=true \
  "$@"

echo "STRESS run complete. Report: $REPORT/index.html"
echo "Watch live metrics in Grafana: http://localhost:3000"
