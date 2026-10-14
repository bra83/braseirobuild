#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# Use a real org.json implementation, never the unrelated snapshot-only QA fixture.
JSON_SOURCES="${1:?Pass directory containing real org/json Java sources}"
WORK="$(mktemp -d /tmp/braseiro-xmm-save-tests.XXXXXX)"
trap 'rm -rf "$WORK"' EXIT
javac -encoding UTF-8 -sourcepath "$ROOT/app/src/main/java" -d "$WORK" "$JSON_SOURCES"/org/json/*.java \
    "$ROOT"/tools/github_qa/src/org/junit/*.java \
    "$ROOT"/app/src/main/java/com/braseiro/dnd5e/CombatActionReceiptGuard.java \
    "$ROOT"/app/src/main/java/com/braseiro/dnd5e/XmmSaveEffectLedger.java \
    "$ROOT"/app/src/test/java/com/braseiro/dnd5e/XmmSaveEffectLedgerTest.java \
    "$ROOT"/tools/github_qa/src/RunXmmSaveEffects.java
java -cp "$WORK" RunXmmSaveEffects
