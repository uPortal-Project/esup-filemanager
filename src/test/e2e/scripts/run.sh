#!/usr/bin/env bash
set -Eeuo pipefail
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../../.." && pwd)
campaign=${1:-smoke}
[[ $# -eq 0 ]] || shift
case "$campaign" in
  smoke|full) ;;
  clean)
    [[ $# -eq 0 ]] || { echo 'Usage: run.sh clean' >&2; exit 2; }
    exec git -C "$ROOT" clean -fdX -- src/test/e2e/
    ;;
  *) echo 'Usage: run.sh clean or run.sh {smoke|full} [--s3-mode=direct|classic] [--video] [--keep] [-- PLAYWRIGHT_ARGS...]' >&2; exit 2 ;;
esac

export S3_MODE=direct
export E2E_VIDEO=retain-on-failure
keep=false
playwright_args=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --s3-mode=direct) S3_MODE=direct ;;
    --s3-mode=classic) S3_MODE=classic ;;
    --video) E2E_VIDEO=on ;;
    --keep) keep=true ;;
    --) shift; playwright_args=("$@"); break ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
  shift
done

# The same application image supports both S3 paths at runtime.
export S3_PRESIGNED_URLS_ENABLED=true
if [[ "$S3_MODE" == classic ]]; then
  S3_PRESIGNED_URLS_ENABLED=false
fi

docker compose version >/dev/null
docker info >/dev/null
export E2E_RUN="efm-$(date -u +%Y%m%d%H%M%S)-$$-${RANDOM}"
export E2E_CAMPAIGN="$campaign"
export E2E_RESULTS="$ROOT/src/test/e2e/results/$E2E_RUN"
mkdir -p "$E2E_RESULTS"
compose=(docker compose -p "$E2E_RUN" -f "$ROOT/src/etc/docker-compose.e2e.yml")
compose_pid=''

# Waiting on a child lets the wrapper handle interruption and run its cleanup.
run_step() {
  "${compose[@]}" "$@" &
  compose_pid=$!
  wait "$compose_pid"
  compose_pid=''
}

cleanup() {
  status=$?
  trap - EXIT INT TERM
  set +e
  if docker inspect "$E2E_RUN-runner" >/dev/null 2>&1; then
    docker stop --time 20 "$E2E_RUN-runner" >/dev/null 2>&1
  fi
  if [[ -n "$compose_pid" ]]; then
    kill -TERM "$compose_pid" 2>/dev/null
    wait "$compose_pid" 2>/dev/null
  fi
  "${compose[@]}" logs --no-color >"$E2E_RESULTS/compose.log" 2>&1
  "${compose[@]}" ps -a >"$E2E_RESULTS/services.txt" 2>&1
  if [[ "$keep" == true ]]; then
    echo "Stack retained. Clean up with: docker compose -p $E2E_RUN -f $E2E_RESULTS/compose.yml down --volumes --remove-orphans"
  else
    "${compose[@]}" down --volumes --remove-orphans --timeout 20 || { [[ $status != 0 ]] || status=1; }
  fi
  echo "Results: $E2E_RESULTS"
  if [[ -s "$E2E_RESULTS/campaign.mp4" ]]; then
    echo "Campaign video: $E2E_RESULTS/campaign.mp4"
  fi
  exit "$status"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
"${compose[@]}" config >"$E2E_RESULTS/compose.yml"
run_step build
run_step up -d app-e2e
run_step run -T --name "$E2E_RUN-runner" --no-deps playwright "${playwright_args[@]}"
