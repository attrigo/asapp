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
ENV_FILE="$SCRIPT_DIR/env/local.properties"
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

# Sanity check: JTL must have at least one data row (header + 1 sample).
line_count="$(wc -l < "$JTL")"
if [ "$line_count" -lt 2 ]; then
  echo "REGRESSION FAILED: JTL has no sample data (JMeter may not have run any samplers). Report: $REPORT/index.html" >&2
  exit 1
fi

# Gate: find the 'success' column by header name, count false rows.
fail_count="$(awk -F',' '
  NR==1 { for (i=1;i<=NF;i++) if ($i=="success") col=i; next }
  col && $col=="false" { c++ }
  END { if (!col) { print "ERROR: no success column in JTL" > "/dev/stderr"; exit 1 }
        print c+0 }' "$JTL")"

if [ "$fail_count" -gt 0 ]; then
  echo "REGRESSION FAILED: $fail_count failed sample(s)/assertion(s). Report: $REPORT/index.html" >&2
  exit 1
fi
echo "REGRESSION PASSED (0 failures). Report: $REPORT/index.html"
