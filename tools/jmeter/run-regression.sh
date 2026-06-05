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
# shellcheck source=scripts/common.sh
. "$SCRIPT_DIR/scripts/common.sh"   # paths (ENV_FILE/RESULTS_DIR/SCRIPTS_DIR) + preflight() + run_plan()

PREFLIGHT=1
if [[ "${1:-}" == "--no-preflight" ]]; then PREFLIGHT=0; shift; fi

JMETER_BIN="$("$SCRIPTS_DIR/ensure-jmeter.sh")"
(( PREFLIGHT == 1 )) && preflight

run_plan regression "$SCRIPT_DIR/asapp-regression.jmx" "$@"

# Sanity check: JTL must have at least one data row (header + 1 sample).
line_count="$(wc -l < "$JTL")"
if (( line_count < 2 )); then
  echo "REGRESSION FAILED: JTL has no sample data (JMeter may not have run any samplers). Report: $REPORT/index.html" >&2
  exit 1
fi

# Gate: find the 'success' column by header name, count false rows.
fail_count="$(awk -F',' '
  NR==1 { for (i=1;i<=NF;i++) if ($i=="success") col=i; next }
  col && $col=="false" { c++ }
  END { if (!col) { print "ERROR: no success column in JTL" > "/dev/stderr"; exit 1 }
        print c+0 }' "$JTL")"

if (( fail_count > 0 )); then
  echo "REGRESSION FAILED: $fail_count failed sample(s)/assertion(s). Report: $REPORT/index.html" >&2
  exit 1
fi
echo "REGRESSION PASSED (0 failures). Report: $REPORT/index.html"
