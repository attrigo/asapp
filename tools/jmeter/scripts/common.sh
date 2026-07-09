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

# log_detail <message>: an indented detail line under the current step (stderr, like the traces).
log_detail() {
  printf '  %s\n' "$1" >&2
}

# log_blank: a blank spacer line on stderr.
log_blank() {
  printf '\n' >&2
}

# prop <dotted.key>: print the value of <dotted.key> from $ENV_FILE, or nothing if
# absent. Ignores comment lines (optional leading whitespace then '#'), matches the
# key exactly against the text before the FIRST '=', and trims surrounding whitespace
# from the value (everything after that first '=', so the value itself can contain '=').
prop() {
  awk -F= -v k="$1" '
    $0 !~ /^[[:space:]]*#/ && $1 == k {
      sub(/^[^=]*=/, "")
      gsub(/^[[:space:]]+|[[:space:]]+$/, "")
      print
      exit
    }
  ' "$ENV_FILE"
}

# preflight: check the three docker-compose services answer their readiness probe
# before a run starts. Each service's URL is derived from env/local.properties
# (<svc>.scheme/.host/.port/.context) - the same config the .jmx plans load - so
# there is a single source of truth for where each service lives.
# /readyz is published on each service's MAIN port (Spring Boot's
# management.endpoint.health.probes.add-additional-paths=true), not the
# *.mgmt.port used by the tooling-exposure plan.
# Exits 1 with guidance if any service is down, or if its config is incomplete.
preflight() {
  log_step "Pre-flight: checking service readiness"
  local svc scheme host port context url
  for svc in auth tasks users; do
    scheme="$(prop "$svc.scheme")"
    host="$(prop "$svc.host")"
    port="$(prop "$svc.port")"
    context="$(prop "$svc.context")"

    if [[ -z "$scheme" ]]; then
      echo "ERROR: missing '$svc.scheme' in $ENV_FILE" >&2
      exit 1
    fi
    if [[ -z "$host" ]]; then
      echo "ERROR: missing '$svc.host' in $ENV_FILE" >&2
      exit 1
    fi
    if [[ -z "$port" ]]; then
      echo "ERROR: missing '$svc.port' in $ENV_FILE" >&2
      exit 1
    fi
    if [[ -z "$context" ]]; then
      echo "ERROR: missing '$svc.context' in $ENV_FILE" >&2
      exit 1
    fi

    url="$scheme://$host:$port$context/readyz"
    if ! curl -fsS -o /dev/null --max-time 5 "$url"; then
      echo "ERROR: service not ready: $url" >&2
      echo "Bring the stack up first: docker-compose up -d" >&2
      exit 1
    fi
    log_detail "UP: $url"
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
  log_blank

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

  log_blank
  log_step_end "Finished $label plan: $(( $(date +%s) - start ))s elapsed"

  (( rc == 0 )) || exit "$rc"
}

# evaluate_gate <label>
# Make a `jmeter -n` run a real pass/fail gate: read the run's total error count from the
# dashboard's statistics.json (structured JSON, unlike the .jtl whose quoted messages embed
# commas/newlines and would defeat a column parse). One awk line pulls Total.errorCount;
# a non-zero count, or a missing file, fails the gate.
#   Inputs: REPORT (set by run_plan)
evaluate_gate() {
  local label="$1" errors
  errors="$(awk '/"Total"[[:space:]]*:/{t=1} t&&/"errorCount"/{match($0,/[0-9]+/); print substr($0,RSTART,RLENGTH); exit}' "$REPORT/statistics.json")"
  if (( ${errors:-1} > 0 )); then
    log_detail "$label FAILED."
    log_detail "Report: $REPORT/index.html"
    exit 1
  fi
  log_detail "$label PASSED."
  log_detail "Report: $REPORT/index.html"
}
