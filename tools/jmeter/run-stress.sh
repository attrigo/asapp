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
# shellcheck source=scripts/common.sh
. "$SCRIPT_DIR/scripts/common.sh"   # paths (ENV_FILE/RESULTS_DIR/SCRIPTS_DIR) + preflight() + run_plan()

PREFLIGHT=1
if [[ "${1:-}" == "--no-preflight" ]]; then PREFLIGHT=0; shift; fi

# The stress plan uses JSR223/Groovy; JMeter's bundled Groovy 3.0.20 cannot run on a
# JDK newer than Java 21. Resolve & validate a Java 17/21 JVM (JMETER_JAVA_HOME or
# JAVA_HOME) and export JAVA_HOME so the JMeter launcher below uses it. Validate before
# the download so a misconfigured JVM aborts instantly.
. "$SCRIPT_DIR/scripts/resolve-java.sh"

JMETER_BIN="$("$SCRIPTS_DIR/ensure-jmeter.sh")"
(( PREFLIGHT == 1 )) && preflight

run_plan stress "$SCRIPT_DIR/asapp-stress.jmx" "$@"

echo "STRESS run complete. Report: $REPORT/index.html"
echo "Watch live metrics in Grafana: http://localhost:3000"
