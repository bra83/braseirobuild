#!/usr/bin/env bash
# D&D NORMAL: unattended source-bound Codex batches, SHA-guarded integration.
# Never starts in a suspended Codespace; no paid API key is required when Codex is authenticated via ChatGPT.
set -Eeuo pipefail
SELF="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
ROOT="$(cd "$SELF/.." && pwd -P)"
REPO='bra83/braseirobuild'
BRANCH='antigravity/dnd-normal-fechamento-gates'
GIT_DIR="$(git -C "$ROOT" rev-parse --absolute-git-dir)"
STATE="$GIT_DIR/dnd-autopilot"
mkdir -p "$STATE/logs"
PID="$STATE/pid"
PAUSED="$STATE/paused"
PENDING="$STATE/pending"
POLICY="$ROOT/scripts/dnd-autopilot-policy.md"
INTERVAL="${DND_AUTOPILOT_INTERVAL_SECONDS:-3600}"
case "$INTERVAL" in ''|*[!0-9]*) echo 'INVALID_INTERVAL'; exit 2;; esac
if (( INTERVAL < 3600 )); then echo 'MINIMUM_INTERVAL_3600_SECONDS'; exit 2; fi
now(){ date -u +%Y-%m-%dT%H:%M:%SZ; }
report(){ printf '%s %s\n' "$(now)" "$*" | tee "$STATE/status"; }
requirements(){
  [[ "$(git -C "$ROOT" branch --show-current)" == "$BRANCH" ]] || { report 'BLOCKED_WRONG_BRANCH'; return 2; }
  [[ -z "$(git -C "$ROOT" status --porcelain --untracked-files=normal)" ]] || { report 'BLOCKED_DIRTY_MAIN_WORKTREE'; return 2; }
  command -v gh >/dev/null || { report 'BLOCKED_GITHUB_CLI_NOT_INSTALLED'; return 2; }
  gh auth status >/dev/null 2>&1 || { report 'BLOCKED_GITHUB_CLI_NOT_AUTHENTICATED'; return 2; }
  command -v codex >/dev/null || { report 'BLOCKED_CODEX_CLI_NOT_INSTALLED'; return 2; }
  local mode
  mode="$(codex login status 2>&1)" || { report 'BLOCKED_CODEX_LOGIN_REQUIRED'; return 2; }
  if ! grep -qi 'ChatGPT' <<< "$mode"; then
    report 'BLOCKED_CODEX_CHATGPT_LOGIN_REQUIRED_TO_AVOID_PAID_API'; return 2
  fi
  command -v python3 >/dev/null || { report 'BLOCKED_PYTHON3_REQUIRED'; return 2; }
  command -v java >/dev/null || { report 'BLOCKED_JAVA_REQUIRED'; return 2; }
  [[ -d "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/NONEXISTENT}}" ]] || { report 'BLOCKED_ANDROID_SDK_REQUIRED'; return 2; }
  [[ -f "$POLICY" ]] || { report 'BLOCKED_POLICY_MISSING'; return 2; }
}
ci_for_sha(){
  local sha="$1" status parent meta
  status="$(gh api "repos/$REPO/actions/workflows/dnd-normal-automation-ci.yml/runs?head_sha=$sha&per_page=30" \
    | python3 -c 'import json,sys; sha=sys.argv[1]; runs=json.load(sys.stdin).get("workflow_runs",[]); r=next((x for x in runs if x.get("head_sha")==sha),None); print("MISSING" if not r else (r.get("conclusion") or r.get("status") or "UNKNOWN"))' "$sha")" || return 3
  if [[ "$status" != MISSING ]]; then printf '%s\n' "$status"; return 0; fi
  # Only a verified one-file post-CI handoff commit can inherit parent CI.
  meta="$(gh api "repos/$REPO/commits/$sha")" || return 3
  parent="$(printf '%s' "$meta" | python3 -c 'import json,sys; d=json.load(sys.stdin); f=d.get("files") or []; p=d.get("parents") or []; m=d.get("commit",{}).get("message",""); ok=len(p)==1 and len(f)==1 and f[0].get("filename")=="docs/HANDOFF_DND_ATUAL.md" and m.startswith("docs(dnd): update automatic handoff for "); print(p[0].get("sha","") if ok else "")')" || return 3
  if [[ "$parent" =~ ^[a-f0-9]{40}$ ]]; then
    status="$(gh api "repos/$REPO/actions/workflows/dnd-normal-automation-ci.yml/runs?head_sha=$parent&per_page=30" \
      | python3 -c 'import json,sys; sha=sys.argv[1]; runs=json.load(sys.stdin).get("workflow_runs",[]); r=next((x for x in runs if x.get("head_sha")==sha),None); print("MISSING" if not r else (r.get("conclusion") or r.get("status") or "UNKNOWN"))' "$parent")" || return 3
    [[ "$status" != success ]] || echo "HANDOFF_ONLY_PARENT_CI_VERIFIED=$parent" >&2
  fi
  printf '%s\n' "$status"
}
validate_changes(){
  local work="$1" item
  local -a files=()
  mapfile -d '' -t files < <( { git -C "$work" diff --name-only -z HEAD; git -C "$work" ls-files --others --exclude-standard -z; } | sort -zu )
  if (( ${#files[@]} == 0 )); then report 'NO_CODE_CHANGES'; return 1; fi
  for item in "${files[@]}"; do
    case "$item" in
      dnd-normal/run247-mod005/app/src/main/java/com/braseiro/dnd5e/*|\
      dnd-normal/run247-mod005/app/src/test/java/com/braseiro/dnd5e/*|\
      dnd-normal/run247-mod005/tools/*|\
      dnd-normal/run247-mod005/docs/editorial-evidence/*) ;;
      *) report "BLOCKED_UNAPPROVED_PATH=$item"; return 2 ;;
    esac
    case "${item,,}" in
      *mystara*|*secret*|*credential*|*keystore*|*.pdf|*adventure*book*)
        report "BLOCKED_PROTECTED_PATH=$item"; return 2;;
    esac
  done
  printf '%s\0' "${files[@]}" > "$STATE/files.nul"
  report "ALLOWED_CHANGED_FILES=${#files[@]}"
}
regression(){
  local work="$1" log="$2" p="$work/dnd-normal/run247-mod005"
  (
    set -Eeuo pipefail
    cd "$p"
    echo "SOURCE_GIT_SHA=$(git rev-parse HEAD)"
    python3 -m unittest discover -s tools -p 'test_verify_xmm_503_source.py' -v
    python3 tools/verify_xmm_503_source.py --json-out build/xmm-503/source-report.json
    python3 - <<'PY'
import json
p=json.load(open('build/xmm-503/source-report.json',encoding='utf-8'))
assert p['source_monsters_compared']==503 and p['editorial_sections_compared']==36
assert p['source_field_and_catalog_gate_pass']
assert not p['full_monster_combat_automation_certified']
print('SOURCE_503_36=PASS')
PY
    if [[ -f package.json ]]; then
      command -v npm >/dev/null || { echo 'INFRA_NPM_MISSING'; exit 3; }
      npm ci && npx tsc --noEmit
    else
      echo 'TYPESCRIPT_NOT_APPLICABLE_NO_DND_PACKAGE_JSON'
    fi
    bash ./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest --no-daemon --console=plain
    python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
p=Path('app/build/test-results/testDebugUnitTest')
files=list(p.glob('TEST-*.xml'))
assert files,'XML_SUITE_MISSING'
totals={k:0 for k in ('tests','failures','errors','skipped')}
for f in files:
 t=ET.parse(f).getroot()
 for k in totals: totals[k]+=int(t.attrib.get(k,0))
assert totals['tests']>0
assert totals['failures']==totals['errors']==totals['skipped']==0,totals
print('ANDROID_REGRESSION_XML_PASS',totals,'SUITES',len(files))
PY
  ) > "$log" 2>&1
}
integrate_pending(){
  [[ -f "$PENDING" ]] || return 1
  local base ticket work ci current
  IFS='|' read -r base ticket work < "$PENDING"
  [[ -n "$base" && -n "$ticket" && -d "$work" ]] || { report 'BLOCKED_INVALID_PENDING_WORKTREE'; return 2; }
  ci="$(ci_for_sha "$base")" || { report 'INFRA_CI_QUERY_FAILED'; return 3; }
  [[ "$ci" == success ]] || { report "PENDING_BASE_CI_NOT_GREEN=$ci BRANCH=$ticket"; return 2; }
  git -C "$ROOT" fetch --no-tags origin "$BRANCH" > "$STATE/fetch.log" 2>&1 || { report 'INFRA_FETCH_FAILED'; return 3; }
  current="$(git -C "$ROOT" rev-parse FETCH_HEAD)"
  [[ "$current" == "$base" ]] || { report "BLOCKED_REMOTE_HEAD_ADVANCED_BASE=$base CURRENT=$current BRANCH=$ticket"; return 2; }
  [[ -z "$(git -C "$work" status --porcelain)" ]] || { report "BLOCKED_PENDING_WORKTREE_DIRTY=$work"; return 2; }
  if ! git -C "$work" push origin "HEAD:refs/heads/$BRANCH" > "$STATE/push.log" 2>&1; then
    report "BLOCKED_PUSH_FAILED_SAFE_FF_ONLY_BRANCH=$ticket"; return 2
  fi
  report "PUSHED_CODE_SHA=$(git -C "$work" rev-parse HEAD) BASE=$base"
  rm -f "$PENDING"
  # Preserve commit in the canonical branch; remove the clean worktree only.
  git -C "$ROOT" worktree remove "$work" || report "WORKTREE_REMOVE_MANUALLY=$work"
}
develop_once(){
  requirements || return 2
  if [[ -f "$PENDING" ]]; then integrate_pending; return $?; fi
  # Pull only with the verified safe sync gate, without rewriting local history.
  bash "$SELF/codespace-dnd-sync.sh" once || { report 'BLOCKED_SYNC_OR_BASE_REGRESSION'; return 2; }
  local base ci ticket work stamp log
  base="$(git -C "$ROOT" rev-parse HEAD)"
  ci="$(ci_for_sha "$base")" || { report 'INFRA_CI_QUERY_FAILED'; return 3; }
  if [[ "$ci" != success ]]; then
    report "BASE_CI_NOT_GREEN=$ci SHA=$base; CODE_CAN_BE_PREPARED_BUT_PUSH_WAITS_FOR_GREEN_CI"
  fi
  if [[ "$(cat "$STATE/last-attempt" 2>/dev/null || true)" == "$base" ]]; then
    report "ALREADY_ATTEMPTED_BASE_SHA=$base"; return 0
  fi
  stamp="$(date -u +%Y%m%dT%H%M%SZ)-$$"
  ticket="automation/dnd-xmm-$stamp"
  work="${TMPDIR:-/tmp}/braseiro-dnd-xmm-$stamp"
  log="$STATE/logs/$stamp.log"
  printf '%s\n' "$base" > "$STATE/last-attempt"
  git -C "$ROOT" worktree add --no-checkout -b "$ticket" "$work" "$base" > "$STATE/worktree.log" 2>&1 || { report 'BLOCKED_WORKTREE_CREATE'; return 2; }
  if ! git -C "$work" sparse-checkout set dnd-normal/run247-mod005 > "$STATE/sparse.log" 2>&1; then
    report "BLOCKED_SPARSE_DND_CHECKOUT_WORKTREE=$work"; return 2
  fi
  if [[ ! -f "$work/dnd-normal/run247-mod005/gradlew" ]]; then
    report "BLOCKED_DND_PROJECT_NOT_CHECKED_OUT=$work"; return 2
  fi
  report "CODEX_SESSION_STARTED=$ticket WORKTREE=$work"
  if ! codex exec -C "$work/dnd-normal/run247-mod005" --sandbox workspace-write - < "$POLICY" > "$log" 2>&1; then
    report "CODEX_FAILED_NO_INTEGRATION_LOG=$log"; return 2
  fi
  [[ "$(git -C "$work" rev-parse HEAD)" == "$base" ]] || { report "BLOCKED_AGENT_CHANGED_GIT_HISTORY=$ticket"; return 2; }
  validate_changes "$work" || { report "NO_SAFE_CHANGES_LOG=$log"; return 2; }
  if ! regression "$work" "$STATE/logs/$stamp-tests.log"; then
    report "TESTS_FAILED_NO_PUSH_BRANCH=$ticket LOG=$STATE/logs/$stamp-tests.log"; return 2
  fi
  (
    cd "$work"
    git -c core.quotePath=false add --pathspec-from-file="$STATE/files.nul" --pathspec-file-nul
    git diff --cached --quiet && exit 1
    git commit -m "feat(dnd-xmm): implement source-bound mechanical batch [$stamp]"
  ) >> "$log" 2>&1 || { report "BLOCKED_COMMIT_FAILURE_BRANCH=$ticket"; return 2; }
  printf '%s|%s|%s\n' "$base" "$ticket" "$work" > "$PENDING"
  report "MECHANICAL_BATCH_TESTED_WAITING_SAFE_PUSH=$ticket SHA=$(git -C "$work" rev-parse HEAD)"
  integrate_pending
}
monitor(){
  if command -v flock >/dev/null; then
    exec 9>"$STATE/agent.lock"
    flock -n 9 || exit 0
  fi
  trap 'rm -f "$PID"' EXIT
  echo "$BASHPID" > "$PID"
  while [[ ! -f "$PAUSED" ]]; do
    develop_once || true
    sleep "$INTERVAL"
  done
}
launch(){
  if [[ -f "$PAUSED" ]]; then echo 'AUTOPILOT_PAUSED'; return 0; fi
  if [[ -f "$PID" ]] && kill -0 "$(cat "$PID")" 2>/dev/null; then
    echo "ALREADY_RUNNING_PID=$(cat "$PID")"; return 0
  fi
  nohup bash "$SELF/codespace-dnd-autopilot.sh" monitor </dev/null >> "$STATE/monitor.log" 2>&1 &
  echo "AUTOPILOT_STARTED_PID=$!"
}
install(){
  [[ "$(git -C "$ROOT" branch --show-current)" == "$BRANCH" ]] || { echo 'WRONG_BRANCH'; return 2; }
  # ChatGPT login is supported by codex CLI; no mandatory paid API key.
  if ! command -v codex >/dev/null; then
    command -v npm >/dev/null || { echo 'NPM_REQUIRED_TO_INSTALL_CODEX_CLI'; return 2; }
    npm install -g @openai/codex || return 2
  fi
  local mode
  mode="$(codex login status 2>&1)" || {
    echo 'CODEX_LOGIN_REQUIRED: run "codex login", then repeat this same install command.'
    return 2
  }
  if ! grep -qi 'ChatGPT' <<< "$mode"; then
    echo 'CODEX_CHATGPT_AUTH_REQUIRED: run "codex logout && codex login" (avoids API-key charges).'
    return 2
  fi
  bash "$SELF/codespace-dnd-sync.sh" install || return 2
  command -v gh >/dev/null && gh auth status >/dev/null 2>&1 || { echo 'GH_AUTH_REQUIRED: gh auth login'; return 2; }
  local hook='# BRASEIRO_DND_AUTOPILOT_2026' rcfile="$HOME/.bashrc" escaped
  printf -v escaped '%q' "$SELF/codespace-dnd-autopilot.sh"
  touch "$rcfile"
  if ! grep -Fq "$hook" "$rcfile"; then
    printf '\n%s\nif [ -f %s ]; then bash %s auto-start >/dev/null 2>&1; fi\n' "$hook" "$escaped" "$escaped" >> "$rcfile"
  fi
  rm -f "$PAUSED"
  launch
  echo 'AUTOPILOT_INSTALLED_WITHOUT_PAID_API_KEY_REQUIREMENT'
}
case "${1:-status}" in
  install) install;;
  start) requirements && rm -f "$PAUSED" && launch;;
  auto-start) if [[ ! -f "$PAUSED" ]]; then launch; fi;;
  status)
    echo "ROOT=$ROOT BRANCH=$BRANCH"
    if [[ -f "$PID" ]] && kill -0 "$(cat "$PID")" 2>/dev/null; then echo 'AGENT=RUNNING'; else echo 'AGENT=STOPPED'; fi
    [[ ! -f "$PAUSED" ]] || echo 'AGENT=PAUSED'
    [[ ! -f "$PENDING" ]] || echo "PENDING=$(cat "$PENDING")"
    cat "$STATE/status" 2>/dev/null || true
    ;;
  stop)
    touch "$PAUSED"
    if [[ -f "$PID" ]]; then kill "$(cat "$PID")" 2>/dev/null || true; fi
    report 'AGENT_PAUSED'
    ;;
  once) develop_once;;
  monitor) monitor;;
  *) echo 'USAGE: install|start|status|stop|once'; exit 2;;
esac
