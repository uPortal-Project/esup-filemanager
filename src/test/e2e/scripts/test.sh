#!/usr/bin/env bash
set -Eeuo pipefail
runner_pid=''
terminate() {
  code=$1
  trap - INT TERM
  if [[ -n "$runner_pid" ]]; then
    kill -INT "$runner_pid" 2>/dev/null || true
    wait "$runner_pid" || true
  fi
  exit "$code"
}
# Let Playwright flush JUnit, HTML, traces and videos before the container exits.
trap 'chmod -R a+rwX /results' EXIT
trap 'terminate 130' INT
trap 'terminate 143' TERM
python scripts/wait.py
node node_modules/@playwright/test/cli.js test "$@" &
runner_pid=$!
status=0
wait "$runner_pid" || status=$?
runner_pid=''
if [[ "${E2E_VIDEO:-}" == on ]]; then
  python scripts/video.py || { [[ "$status" != 0 ]] || status=1; }
fi
exit "$status"
