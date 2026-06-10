#!/usr/bin/env bash
#
# Run the tooling-exposure plan against the running docker-compose stack.
# Validates per-profile exposure of the project tooling (Swagger, OpenAPI docs, Actuator):
#   --profile dev   asserts the tooling is exposed (Swagger/docs reachable, full Actuator, active dev profile)
#   --profile prod  asserts the tooling is locked down (Swagger/docs 404, narrow Actuator, env endpoint 404)
# `jmeter -n` always exits 0, so this script reads the run's error count and exits non-zero if ANY
# sample/assertion failed.
#
# IMPORTANT: the stack must already be running with the MATCHING environment profile:
#   --profile dev    stack started with docker,dev   (the default compose stack)
#   --profile prod   stack started with docker,prod
#
# Run with --help for usage.
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# paths (ENV_FILE/RESULTS_DIR/SCRIPTS_DIR) + log + preflight() + run_plan() + evaluate_gate()
. "$SCRIPT_DIR/scripts/common.sh"

usage() {
  cat <<'EOF'
Usage: run-tooling-exposure.sh [--profile dev|prod] [--no-preflight] [-J<prop>=<val> ...]

Validate per-profile tooling exposure against the running stack, then exit non-zero
if any assertion failed (jmeter -n itself always exits 0).

The stack must already be running with the matching environment profile:
  --profile dev    stack is docker,dev   (the default compose stack)
  --profile prod   stack is docker,prod

Options (must precede any -J args):
  --profile <p>    environment profile to assert: dev (default) or prod
  --no-preflight   skip the service readiness checks
  --help           show this help and exit

Any -J<name>=<value> is forwarded to JMeter (defaults in env/local.properties).

Examples:
  run-tooling-exposure.sh                  # asserts the dev (open tooling) posture
  run-tooling-exposure.sh --profile prod   # asserts the prod (locked down) posture
  run-tooling-exposure.sh --no-preflight
EOF
}

PREFLIGHT=1
PROFILE=dev
while [[ "${1:-}" == --* ]]; do
  case "$1" in
    --help)         usage; exit 0 ;;
    --profile)      [[ -n "${2:-}" ]] || { echo "ERROR: --profile requires dev or prod" >&2; exit 2; }
                    PROFILE="$2"; shift 2 ;;
    --no-preflight) PREFLIGHT=0; shift ;;
    --)             shift; break ;;
    *)              echo "ERROR: unknown option '$1' (see --help)" >&2; exit 2 ;;
  esac
done

case "$PROFILE" in
  dev|prod) ;;
  *) echo "ERROR: --profile must be 'dev' or 'prod' (got '$PROFILE')" >&2; exit 2 ;;
esac

# Steps traced as [x/y]: provision, [pre-flight], run, evaluate.
STEP_TOTAL=$(( PREFLIGHT == 1 ? 4 : 3 ))

log_step "Provisioning JMeter"
JMETER_BIN="$("$SCRIPTS_DIR/ensure-jmeter.sh")"

(( PREFLIGHT == 1 )) && preflight

run_plan tooling-exposure "$SCRIPT_DIR/asapp-tooling-exposure.jmx" -Jprofile="$PROFILE" "$@"

log_step "Evaluating results"
evaluate_gate "TOOLING EXPOSURE ($PROFILE)"
