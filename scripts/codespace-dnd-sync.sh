#!/usr/bin/env bash
# Braseiro D&D Normal — one installation, safe fast-forward, SHA-scoped regression.
set -Eeuo pipefail
BRANCH='antigravity/dnd-normal-fechamento-gates'
REPOSITORY='bra83/braseirobuild'
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd -P)"
PROJECT="$ROOT/dnd-normal/run247-mod005"
GIT_DIR="$(git -C "$ROOT" rev-parse --absolute-git-dir)"
STATE="$GIT_DIR/dnd-normal-sync"
mkdir -p "$STATE/logs"
PIDFILE="$STATE/monitor.pid"
DISABLED="$STATE/disabled"
LAST="$STATE/last-tested-sha"
STATUS="$STATE/status"
INTERVAL="${DND_SYNC_INTERVAL_SECONDS:-120}"
case "$INTERVAL" in ''|*[!0-9]*) echo 'INVALID_POLL_INTERVAL'; exit 2 ;; esac
if (( INTERVAL < 120 )); then echo 'MIN_POLL_INTERVAL_120'; exit 2; fi
now(){ date -u +%Y-%m-%dT%H:%M:%SZ; }
putstatus(){ printf '%s\n' "$(now) $*" > "$STATUS"; printf '%s\n' "$(now) $*"; }
ensure_branch(){
  local current
  current="$(git -C "$ROOT" symbolic-ref --quiet --short HEAD || true)"
  if [[ "$current" != "$BRANCH" ]]; then putstatus "BLOCKED_WRONG_BRANCH=$current"; return 2; fi
}
clean_tree(){
  if [[ -n "$(git -C "$ROOT" status --porcelain --untracked-files=normal)" ]]; then
    putstatus 'BLOCKED_LOCAL_CHANGES'; return 2
  fi
}
publish_status(){
  local sha="$1" result="$2" description="$3"
  if [[ "${DND_PUBLISH_GITHUB_STATUS:-1}" != 1 ]] || ! command -v gh >/dev/null; then
    echo 'GITHUB_STATUS_SKIPPED_NO_GH_OR_DISABLED'; return 0
  fi
  if ! gh auth status >/dev/null 2>&1; then echo 'GITHUB_STATUS_SKIPPED_UNAUTHORIZED'; return 0; fi
  local state='failure'
  [[ "$result" == PASS ]] && state='success'
  gh api -X POST "repos/$REPOSITORY/statuses/$sha" -f state="$state" \
    -f context='braseiro/dnd/codespace-regression' \
    -f description="$description" >/dev/null 2>&1 || echo 'GITHUB_STATUS_PERMISSION_DENIED_OR_API_FAILED'
}
run_tests(){
  local sha="$1" log="$STATE/logs/$sha.log" result=PASS failure_kind='' rc=0
  putstatus "RUNNING_SHA=$sha"
  {
    echo "UTC=$(now) BRANCH=$BRANCH SHA=$sha PROJECT=$PROJECT"
    if [[ ! -f "$PROJECT/gradlew" || ! -d "$PROJECT/tools" ]]; then
      echo 'INFRA_DND_PROJECT_MISSING'; result=INFRA; failure_kind=PROJECT_NOT_FOUND
    elif ! command -v python3 >/dev/null; then
      echo 'INFRA_PYTHON3_MISSING'; result=INFRA; failure_kind=PYTHON_NOT_FOUND
    else
      echo 'STAGE=PYTHON_SOURCE_REGRESSION'
      (cd "$PROJECT" && python3 -m unittest discover -s tools -p 'test_verify_xmm_503_source.py' -v \
       && python3 tools/verify_xmm_503_source.py --json-out build/xmm-503/source-report.json) || {
        rc=$?; result=FAIL; failure_kind=SOURCE_REGRESSION; echo "SOURCE_TEST_EXIT=$rc";
      }
      if [[ "$result" == PASS && -f "$PROJECT/package.json" ]]; then
        echo 'STAGE=TYPESCRIPT'
        if ! command -v npm >/dev/null; then
          result=INFRA; failure_kind=NPM_NOT_FOUND; echo 'INFRA_NPM_NOT_FOUND'
        else
          (cd "$PROJECT" && npm ci && npx tsc --noEmit) || {
            rc=$?; result=FAIL; failure_kind=TYPESCRIPT_REGRESSION; echo "TYPESCRIPT_EXIT=$rc";
          }
        fi
      elif [[ "$result" == PASS ]]; then
        echo 'TYPESCRIPT=NOT_APPLICABLE_NO_DND_PACKAGE_JSON'
      fi
      if [[ "$result" == PASS ]]; then
        if ! command -v java >/dev/null || [[ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]]; then
          result=INFRA; failure_kind=JAVA_OR_ANDROID_SDK_MISSING
          echo 'INFRA_JAVA_OR_ANDROID_SDK_MISSING'
        else
          echo 'STAGE=ANDROID_COMPILE_AND_ROBOLECTRIC_SQLITE_REGRESSION'
          (cd "$PROJECT" && bash ./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest \
            --no-daemon --console=plain) || {
              rc=$?; result=FAIL; failure_kind=GRADLE_REGRESSION; echo "GRADLE_EXIT=$rc";
            }
        fi
      fi
    fi
    echo "RESULT=$result REASON=$failure_kind SHA=$sha UTC=$(now)"
  } > "$log" 2>&1
  printf '%s\n' "$sha" > "$LAST"
  putstatus "SHA=$sha RESULT=$result REASON=$failure_kind LOG=$log"
  publish_status "$sha" "$result" "Codespace regression $result ($failure_kind)"
  [[ "$result" == PASS ]]
}
sync_once(){
  ensure_branch || return 2
  clean_tree || return 2
  local old target after
  old="$(git -C "$ROOT" rev-parse HEAD)"
  if ! git -C "$ROOT" fetch --no-tags origin "$BRANCH" > "$STATE/last-fetch.log" 2>&1; then
    putstatus 'INFRA_FETCH_FAILED'; return 3
  fi
  target="$(git -C "$ROOT" rev-parse FETCH_HEAD)"
  if ! git -C "$ROOT" merge-base --is-ancestor "$old" "$target"; then
    putstatus "BLOCKED_DIVERGENT_HISTORY_LOCAL=$old REMOTE=$target"; return 2
  fi
  if [[ "$old" != "$target" ]]; then
    clean_tree || return 2
    if ! git -C "$ROOT" pull --ff-only origin "$BRANCH" > "$STATE/last-pull.log" 2>&1; then
      putstatus "BLOCKED_FF_ONLY_PULL_FAILED LOCAL=$old REMOTE=$target"; return 2
    fi
    after="$(git -C "$ROOT" rev-parse HEAD)"
    if [[ "$after" != "$target" ]]; then putstatus 'BLOCKED_POST_PULL_SHA_MISMATCH'; return 2; fi
    putstatus "FAST_FORWARD_OLD=$old NEW=$after"
  fi
  after="$(git -C "$ROOT" rev-parse HEAD)"
  if [[ "$(cat "$LAST" 2>/dev/null || true)" != "$after" ]]; then
    run_tests "$after" || return 1
  fi
}
monitor(){
  trap 'rm -f "$PIDFILE"' EXIT
  echo "$BASHPID" > "$PIDFILE"
  while [[ ! -f "$DISABLED" ]]; do
    sync_once || true
    sleep "$INTERVAL"
  done
}
launch(){
  if [[ -f "$DISABLED" ]]; then echo 'PAUSED'; return 0; fi
  if [[ -f "$PIDFILE" ]] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "ALREADY_RUNNING_PID=$(cat "$PIDFILE")"; return 0
  fi
  rm -f "$PIDFILE"
  nohup bash "$SCRIPT_DIR/codespace-dnd-sync.sh" monitor </dev/null >> "$STATE/daemon.log" 2>&1 &
  echo "STARTED_PID=$!"
}
install(){
  ensure_branch || return 2
  local rcfile="$HOME/.bashrc" marker='# BRASEIRO_DND_AUTOSYNC_2026'
  local quoted
  printf -v quoted '%q' "$SCRIPT_DIR/codespace-dnd-sync.sh"
  touch "$rcfile"
  if ! grep -Fq "$marker" "$rcfile"; then
    printf '\n%s\nif [ -f %s ]; then bash %s auto-start >/dev/null 2>&1; fi\n' "$marker" "$quoted" "$quoted" >> "$rcfile"
  fi
  if [[ -f "$HOME/.zshrc" ]] && ! grep -Fq "$marker" "$HOME/.zshrc"; then
    printf '\n%s\nif [ -f %s ]; then bash %s auto-start >/dev/null 2>&1; fi\n' "$marker" "$quoted" "$quoted" >> "$HOME/.zshrc"
  fi
  rm -f "$DISABLED"
  launch
  echo "INSTALLED_ROOT=$ROOT"
}
case "${1:-status}" in
 install) install ;;
 start) rm -f "$DISABLED"; ensure_branch && launch ;;
 auto-start) if [[ ! -f "$DISABLED" ]]; then ensure_branch && launch; fi ;;
 status)
   printf 'ROOT=%s\nBRANCH=%s\n' "$ROOT" "$BRANCH"
   if [[ -f "$PIDFILE" ]] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then echo 'MONITOR=RUNNING'; else echo 'MONITOR=STOPPED'; fi
   [[ ! -f "$DISABLED" ]] || echo 'MONITOR=PAUSED'
   cat "$STATUS" 2>/dev/null || true
   echo "LAST_TESTED_SHA=$(cat "$LAST" 2>/dev/null || echo none)"
   ;;
 stop)
   touch "$DISABLED"
   if [[ -f "$PIDFILE" ]]; then kill "$(cat "$PIDFILE")" 2>/dev/null || true; rm -f "$PIDFILE"; fi
   putstatus 'MONITOR=PAUSED_BY_USER'
   ;;
 once) sync_once ;;
 monitor) monitor ;;
 *) echo 'USAGE: install|start|status|stop|once'; exit 2 ;;
esac
