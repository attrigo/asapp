#!/usr/bin/env bash
#
# Shared setup and helpers sourced by run-regression.sh and run-stress.sh.
# Do NOT add `set -euo pipefail` here: this file is sourced, so the parent script's strict mode already applies, and an `exit` below aborts the run.
#

# Resolve paths relative to this file (tools/jmeter/scripts/common.sh) so the run scripts don't
# each recompute them.
JMETER_TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"   # tools/jmeter
SCRIPTS_DIR="$JMETER_TOOL_DIR/scripts"
ENV_FILE="$JMETER_TOOL_DIR/env/local.properties"
RESULTS_DIR="$JMETER_TOOL_DIR/results"

# --- execution tracing ----------------------------------------------------------
# Run scripts set STEP_TOTAL to the number of phases they will run.
STEP=0
STEP_TOTAL=0

# _trace <step-number> <message>: one "[HH:MM:SS] [n/total] message" line on stderr.
_trace() {
  printf '[%s] [%s/%s] %s\n' "$(date +%H:%M:%S)" "$1" "$STEP_TOTAL" "$2" >&2
}

# log_step <message>: advance to the next step and trace it.
log_step() {
  STEP=$((STEP + 1))
  _trace "$STEP" "$1"
}

# log_step_end <message>: trace another line for the current step without advancing
log_step_end() {
  _trace "$STEP" "$1"
}

# preflight: check the three docker-compose services answer their readiness probe
# before a run starts.
# Exits 1 with guidance if any service is down.
preflight() {
  # /readyz is published on each service's MAIN port (Spring Boot's
  # management.endpoint.health.probes.add-additional-paths=true).
  local urls=(
    "http://localhost:8080/asapp-authentication-service/readyz"
    "http://localhost:8081/asapp-tasks-service/readyz"
    "http://localhost:8082/asapp-users-service/readyz"
  )
  log_step "Pre-flight: checking service readiness"
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

# run_plan <label> <plan-file> [extra jmeter args...]
# Runs JMeter non-GUI against <plan-file> and writes a timestamped .jtl, .log and HTML report
# under results/.
#   Inputs : JMETER_BIN, ENV_FILE, RESULTS_DIR
#   Outputs: JTL, LOG, REPORT (set for the caller to read afterwards)
run_plan() {
  local label="$1" plan="$2"
  shift 2

  mkdir -p "$RESULTS_DIR"
  local ts
  ts="$(date +%Y%m%d-%H%M%S)"
  JTL="$RESULTS_DIR/$label-$ts.jtl"
  LOG="$RESULTS_DIR/$label-$ts.log"
  REPORT="$RESULTS_DIR/$label-$ts-report"

  log_step "Running $label plan: $plan"
  local start rc=0
  start="$(date +%s)"
  "$JMETER_BIN" -n \
    -t "$plan" \
    -q "$ENV_FILE" \
    -l "$JTL" \
    -j "$LOG" \
    -e -o "$REPORT" \
    -Jjmeter.save.saveservice.output_format=csv \
    -Jjmeter.save.saveservice.print_field_names=true \
    "$@" || rc=$?
  log_step_end "Finished $label plan: $(( $(date +%s) - start ))s elapsed (exit $rc)"
  (( rc == 0 )) || exit "$rc"
}
