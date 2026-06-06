#!/usr/bin/env bash
#
# Run the deterministic regression plan against the running docker-compose stack.
# `jmeter -n` always exits 0, so this script makes the gate real: it parses the .jtl and exits non-zero if ANY sample/assertion failed.
#
# Run with --help for usage.
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# paths (ENV_FILE/RESULTS_DIR/SCRIPTS_DIR) + log + preflight() + run_plan()
. "$SCRIPT_DIR/scripts/common.sh"

usage() {
  cat <<'EOF'
Usage: run-regression.sh [--no-preflight] [-J<prop>=<val> ...]

Run the deterministic regression plan against the running stack. This is the
pre-release gate: it parses the .jtl and exits non-zero if any sample or
assertion failed (jmeter -n itself always exits 0).

Options (must precede any -J args):
  --no-preflight   skip the service readiness checks
  --help           show this help and exit

Any -J<name>=<value> is forwarded to JMeter (defaults in env/local.properties).

Examples:
  run-regression.sh
  run-regression.sh --no-preflight
  run-regression.sh -Jauth.host=staging.example.com
EOF
}

PREFLIGHT=1
while [[ "${1:-}" == --* ]]; do
  case "$1" in
    --help)         usage; exit 0 ;;
    --no-preflight) PREFLIGHT=0; shift ;;
    --)             shift; break ;;
    *)              echo "WARNING: unknown option '$1'; forwarding it to JMeter" >&2; break ;;
  esac
done

# Steps traced as [x/y]: provision, [pre-flight], run, evaluate.
STEP_TOTAL=$(( PREFLIGHT == 1 ? 4 : 3 ))

log_step "Provisioning JMeter"
JMETER_BIN="$("$SCRIPTS_DIR/ensure-jmeter.sh")"

(( PREFLIGHT == 1 )) && preflight

run_plan regression "$SCRIPT_DIR/asapp-regression.jmx" "$@"

log_step "Evaluating results"

# Sanity check: JTL must have at least one data row (header + 1 sample).
line_count="$(wc -l < "$JTL")"
if (( line_count < 2 )); then
  log_detail "REGRESSION FAILED: JTL has no sample data (JMeter may not have run any samplers)."
  log_detail "Report: $REPORT/index.html"
  exit 1
fi

# Gate: parse the JTL (a CSV) and count failed samples. awk reads the file line by line; -F','
# splits each line into fields ($1, $2, ...) on commas, and NR is the current line number.
#   - NR==1 (header row): scan the column names to find which field is "success", remember its
#     index in `col`, then `next` to skip to the following line.
#   - every later row: if that row's "success" field is the string "false", bump the counter `c`.
#   - END (after the last row): if no "success" column was ever found the JTL is malformed -> abort;
#     otherwise print the failure count. `c+0` forces a number so an unset `c` prints 0, not "".
fail_count="$(awk -F',' '
  NR==1 { for (i=1;i<=NF;i++) if ($i=="success") col=i; next }
  col && $col=="false" { c++ }
  END { if (!col) { print "ERROR: no success column in JTL" > "/dev/stderr"; exit 1 }
        print c+0 }' "$JTL")"

if (( fail_count > 0 )); then
  log_detail "REGRESSION FAILED: $fail_count failed sample(s)/assertion(s)."
  log_detail "Report: $REPORT/index.html"
  exit 1
fi
log_detail "REGRESSION PASSED (0 failures)."
log_detail "Report: $REPORT/index.html"
