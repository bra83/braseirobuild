#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRC="$ROOT/app/src/main/java/com/braseiro/dnd5e/adventure"
TEST="$ROOT/app/src/test/java/com/braseiro/dnd5e/adventure"
WORK="${TMPDIR:-/tmp}/braseiro_github_qa_$$"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes"
javac -encoding UTF-8 -d "$WORK/classes"   "$ROOT"/tools/github_qa/src/org/json/*.java   "$ROOT"/tools/github_qa/src/org/junit/*.java   "$ROOT"/tools/github_qa/src/com/braseiro/dnd5e/adventure/*.java   "$SRC"/{AdventureState,TomeEntityBridge,UniversalEffectProposalPort,UniversalEffectTransaction,LoreweaverOutcomePort,YearZeroDicePort,StarWarsNarrativeDicePort,AetherTableRollIntentPort,UniversalAiProposalPort,UniversalLiveSession,UniversalGithubModules,UniversalRpgEngine,UniversalGithubLab}.java   "$TEST/UniversalGithubModulesIntegrationTest.java"   "$ROOT/tools/github_qa/src/RunGithubTests.java"   "$ROOT/tools/github_qa/src/RunLab.java"
java -cp "$WORK/classes" RunGithubTests
java -cp "$WORK/classes" RunLab
