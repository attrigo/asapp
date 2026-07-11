#!/usr/bin/env bash
#
# Run the deterministic regression plan against the running docker-compose stack.
# `jmeter -n` always exits 0, so this script makes the gate real: it reads the run's error count and exits non-zero if ANY sample/assertion failed.
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
pre-release gate: it checks the run's error count and exits non-zero if any
sample or assertion failed (jmeter -n itself always exits 0).

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
    *)              echo "ERROR: unknown option '$1' (see --help)" >&2; exit 2 ;;
  esac
done

# Steps traced as [x/y]: provision, [pre-flight], run, evaluate.
STEP_TOTAL=$(( PREFLIGHT == 1 ? 4 : 3 ))

log_step "Provisioning JMeter"
JMETER_BIN="$("$SCRIPTS_DIR/ensure-jmeter.sh")"

(( PREFLIGHT == 1 )) && preflight

run_plan regression "$SCRIPT_DIR/asapp-regression.jmx" "$@"

log_step "Evaluating results"
evaluate_gate "REGRESSION"
