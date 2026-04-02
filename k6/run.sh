#!/bin/sh
set -eu

PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
export PATH

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

usage() {
  cat <<'EOF'
Usage:
  ./run.sh run
  ./run.sh run [label]
  ./run.sh up
  ./run.sh down

Required env for current runner:
  LEADER_CREDENTIALS=email:password[,email:password...]
  MEMBER_CREDENTIALS=email:password[,email:password...]

Optional env:
  BASE_URL=http://localhost:8080
  AUTO_PREPARE_WRITE_BANK=true
EOF
}

sanitize_segment() {
  printf '%s' "$1" \
    | tr '[:upper:]' '[:lower:]' \
    | sed 's/[^a-z0-9]/_/g; s/_\{2,\}/_/g; s/^_//; s/_$//'
}

resolve_preset_name() {
  if [ ! -f "$SCRIPT_DIR/test-plan.json" ]; then
    echo "manual"
    return
  fi

  jq -r '.preset // "manual"' "$SCRIPT_DIR/test-plan.json" 2>/dev/null || echo "manual"
}

run_compose() {
  cd "$SCRIPT_DIR"
  docker compose "$@"
}

bool_is_true() {
  value=$(printf '%s' "${1:-}" | tr '[:upper:]' '[:lower:]')
  case "$value" in
    1|true|yes|on)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

read_repo_env_value() {
  key=$1
  repo_env="$SCRIPT_DIR/../.env"

  if [ ! -f "$repo_env" ]; then
    return 1
  fi

  grep "^${key}=" "$repo_env" | head -n 1 | cut -d= -f2-
}

prepare_write_bank() {
  preset_name=$1

  case "$preset_name" in
    realistic_*|party_write_contention)
      ;;
    *)
      return 0
      ;;
  esac

  if ! bool_is_true "${AUTO_PREPARE_WRITE_BANK:-true}"; then
    return 0
  fi

  if ! command -v docker >/dev/null 2>&1; then
    echo "docker command not found. Install Docker or set AUTO_PREPARE_WRITE_BANK=false." >&2
    exit 1
  fi

  db_name=${DB_NAME:-$(read_repo_env_value DB_NAME || true)}
  db_password=${DB_PASSWORD:-$(read_repo_env_value DB_PASSWORD || true)}
  db_container=${DB_CONTAINER_NAME:-matchduo_db-mysql}

  if [ -z "${db_name:-}" ] || [ -z "${db_password:-}" ]; then
    echo "DB_NAME/DB_PASSWORD could not be resolved for automatic write-bank preparation." >&2
    exit 1
  fi

  echo "Preparing write-bank data for preset: $preset_name"
  docker exec -i "$db_container" mysql -uroot -p"$db_password" "$db_name" < "$SCRIPT_DIR/sql/load-test-seed.sql"
  docker exec -i "$db_container" mysql -uroot -p"$db_password" "$db_name" < "$SCRIPT_DIR/sql/reset-party-write.sql"
}

run_current() {
  run_label=${1:-}
  preset_name=$(sanitize_segment "$(resolve_preset_name)")

  if ! command -v k6 >/dev/null 2>&1; then
    echo "k6 command not found. Install k6 locally before running host-based load tests." >&2
    exit 1
  fi

  if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a
    . "$SCRIPT_DIR/.env"
    set +a
  fi

  if [ -z "${BASE_URL:-}" ]; then
    BASE_URL="http://localhost:8080"
  fi

  prepare_write_bank "$preset_name"

  if [ -z "${RUN_ID:-}" ]; then
    if ! command -v jq >/dev/null 2>&1; then
      echo "jq command not found. Install jq locally before running the current k6 runner." >&2
      exit 1
    fi

    timestamp=$(date +%Y%m%d_%H%M%S)
    RUN_ID="${preset_name:-manual}_${timestamp}"

    if [ -n "$run_label" ]; then
      normalized_label=$(sanitize_segment "$run_label")
      if [ -n "$normalized_label" ]; then
        RUN_ID="${preset_name:-manual}_${normalized_label}_${timestamp}"
      fi
    fi
  fi

  K6_PROMETHEUS_RW_SERVER_URL=${K6_PROMETHEUS_RW_SERVER_URL:-http://localhost:9090/api/v1/write}
  K6_PROMETHEUS_RW_TREND_STATS=${K6_PROMETHEUS_RW_TREND_STATS:-"p(95),p(99),min,max,avg"}

  export BASE_URL
  export RUN_ID
  export K6_PROMETHEUS_RW_SERVER_URL
  export K6_PROMETHEUS_RW_TREND_STATS

  mkdir -p "$SCRIPT_DIR/results"

  exec k6 run \
    -o experimental-prometheus-rw \
    --summary-export="$SCRIPT_DIR/results/${RUN_ID}.json" \
    "$SCRIPT_DIR/main.js"
}

command=${1:-run}
if [ $# -gt 0 ]; then
  shift
fi

case "$command" in
  run)
    run_current "$@"
    ;;
  up)
    run_compose up -d prometheus grafana
    ;;
  down)
    run_compose stop prometheus grafana
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
