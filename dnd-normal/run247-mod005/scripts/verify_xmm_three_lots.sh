#!/usr/bin/env bash
# Fast, dependency-free gate for the three *independent* XMM damage/save cores.
# This does NOT certify combat persistence, source equivalence or Android E2E.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CORE="$ROOT/app/src/main/java/com/braseiro/dnd5e"
TEST="$ROOT/app/src/test/java/com/braseiro/dnd5e"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "=== XMM LOT 01: compiled core + 784 assertions ==="
mkdir -p "$WORK/01"
javac --release 8 -d "$WORK/01" \
  "$CORE/XmmDamageOrSaveBranch01.java" \
  "$TEST/XmmDamageOrSaveBranch01Scenarios.java"
java -cp "$WORK/01" com.braseiro.dnd5e.XmmDamageOrSaveBranch01Scenarios

echo "=== XMM LOT 02: source-contract comparison + Java core ==="
python3 "$ROOT/tools/test_xmm_damage_or_save_branch02.py"

echo "=== XMM LOT 03: compiled core + 65 assertions ==="
mkdir -p "$WORK/03"
javac --release 8 -d "$WORK/03" \
  "$CORE/DamageMitigationRules.java" \
  "$CORE/XmmDamageOrSaveBranch03.java" \
  "$ROOT/tools/branch03/XmmDamageOrSaveBranch03Test.java"
java -cp "$WORK/03" com.braseiro.dnd5e.XmmDamageOrSaveBranch03Test

echo "XMM_INDEPENDENT_THREE_LOTS=PASS"
echo "XMM_ANDROID_COMBAT_INTEGRATION=NOT_TESTED"
echo "XMM_SQLITE_BACKUP21=NOT_TESTED"
echo "XMM_FULL_SOURCE_SEMANTIC_CERTIFICATION=NOT_TESTED"
