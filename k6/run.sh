#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

usage() {
  cat <<'EOF'
Usage:
  ./run.sh up
  ./run.sh down
  ./run.sh mixed [profile] [run_id]
  ./run.sh party [profile] [run_id]
  ./run.sh capture [run_id]

Docker:
  docker compose --profile manual run --rm k6
EOF
}

run_compose() {
  cd "$SCRIPT_DIR"
  docker compose "$@"
}

run_local() {
  test_mode=$1
  load_profile=$2
  run_id=${3:-}

  if ! command -v k6 >/dev/null 2>&1; then
    echo "k6 command not found. Install k6 locally before running host-based load tests." >&2
    exit 1
  fi

  if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a
    . "$SCRIPT_DIR/.env"
    set +a
  fi

  if [ -z "${BASE_URL:-}" ] || [ "${BASE_URL}" = "http://host.docker.internal:8080" ]; then
    BASE_URL="http://localhost:8080"
  fi

  if [ -n "$run_id" ]; then
    RUN_ID=$run_id
  elif [ -z "${RUN_ID:-}" ] || [ "${RUN_ID}" = "mixed_run8" ]; then
    RUN_ID=$(date +%Y%m%d_%H%M%S)
  fi

  K6_PROMETHEUS_RW_SERVER_URL=${K6_PROMETHEUS_RW_SERVER_URL:-http://localhost:9090/api/v1/write}
  K6_PROMETHEUS_RW_TREND_STATS=${K6_PROMETHEUS_RW_TREND_STATS:-"p(95),p(99),min,max,avg"}

  export BASE_URL
  export RUN_ID
  export TEST_MODE=$test_mode
  export LOAD_PROFILE=$load_profile
  export ENABLE_PARTY_WRITE=${ENABLE_PARTY_WRITE:-true}
  export K6_PROMETHEUS_RW_SERVER_URL
  export K6_PROMETHEUS_RW_TREND_STATS

  mkdir -p "$SCRIPT_DIR/results"

  exec k6 run \
    -o experimental-prometheus-rw \
    --summary-export="$SCRIPT_DIR/results/${RUN_ID}.json" \
    "$SCRIPT_DIR/main.js"
}

run_docker_default() {
  if [ -z "${RUN_ID:-}" ] || [ "${RUN_ID}" = "mixed_run8" ]; then
    RUN_ID=$(date +%Y%m%d_%H%M%S)
  fi

  export RUN_ID
  mkdir -p /work/k6/results

  exec k6 run \
    -o experimental-prometheus-rw \
    --summary-export="/work/k6/results/${RUN_ID}.json" \
    /work/k6/main.js
}

command=${1:-docker}
if [ $# -gt 0 ]; then
  shift
fi

case "$command" in
  docker)
    run_docker_default
    ;;
  up)
    run_compose up -d prometheus grafana
    ;;
  down)
    run_compose stop prometheus grafana
    ;;
  mixed)
    run_local mixed "${1:-high}" "${2:-}"
    ;;
  party)
    run_local party_write "${1:-stress}" "${2:-}"
    ;;
  capture)
    run_local party_write stress "${1:-party_write_host_stress_capture}"
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    echo "Unknown command: $command" >&2
    echo >&2
    usage >&2
    exit 1
    ;;
esac
