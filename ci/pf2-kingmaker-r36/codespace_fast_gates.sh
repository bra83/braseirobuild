#!/usr/bin/env bash
# Source-only official Kingmaker R36 gates in GitHub Codespaces. NEVER an APK build.
set -Eeuo pipefail
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
BRANCH="pf2-kingmaker-r36-official-rule-fixes-20261011"
if [[ "$(git branch --show-current)" != "$BRANCH" ]]; then
  echo "WRONG_BRANCH: select $BRANCH" >&2
  exit 2
fi
MODE="fast"
if [[ $# -ge 1 ]]; then MODE="$1"; fi
if [[ "$MODE" != "fast" && "$MODE" != "full" ]]; then
  echo "Usage: bash ci/pf2-kingmaker-r36/codespace_fast_gates.sh fast|full" >&2
  exit 2
fi
for tool in curl unzip sha256sum javac java python gh; do
  command -v "$tool" >/dev/null || { echo "MISSING_TOOL: $tool" >&2; exit 2; }
done
ZIP="$HOME/.cache/braseiro-r36/r35-source.zip"
SHA="86d9a6fd26e4139e084c8e4069f49ef53e0cf8654e0f9f5662b92f9183941aa6"
mkdir -p "$(dirname "$ZIP")"
if [[ ! -f "$ZIP" ]] || ! printf '%s  %s\n' "$SHA" "$ZIP" | sha256sum -c - >/dev/null 2>&1; then
  TOKEN="$(gh auth token)"
  curl --fail --silent --show-error --location --retry 3 \
    -H "Authorization: Bearer $TOKEN" \
    -H "Accept: application/octet-stream" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    -o "$ZIP.part" "https://api.github.com/repos/bra83/braseirobuild/releases/assets/629875743"
  [[ "$(stat -c %s "$ZIP.part")" = "92975058" ]] || { echo "R35_SIZE_MISMATCH" >&2; exit 3; }
  printf '%s  %s\n' "$SHA" "$ZIP.part" | sha256sum -c -
  mv "$ZIP.part" "$ZIP"
fi
printf '%s  %s\n' "$SHA" "$ZIP" | sha256sum -c -
WORK="$(mktemp -d -t kingmaker-r36-XXXXXXXX)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/source" "$WORK/classes"
unzip -q "$ZIP" -d "$WORK/source"
PROJECT=""
while IFS= read -r -d '' file; do
  base="$(dirname "$file")"
  if [[ -f "$base/gradlew.bat" ]]; then
    [[ -z "$PROJECT" ]] || { echo "MULTIPLE_R35_ROOTS" >&2; exit 3; }
    PROJECT="$base"
  fi
done < <(find "$WORK/source" -name version.properties -type f -print0)
[[ -n "$PROJECT" ]] || { echo "CANONICAL_R35_ROOT_NOT_FOUND" >&2; exit 3; }
export PF2_R36_PROJECT="$PROJECT"
[[ -d "$PROJECT/tests_pf2" ]] || { echo "CANONICAL_TESTS_MISSING" >&2; exit 3; }
python -m pytest -q \
  ci/pf2-kingmaker-r36/test_catalog_expansion.py \
  ci/pf2-kingmaker-r36/test_official_build_ui.py \
  ci/pf2-kingmaker-r36/test_structure_effects_metadata.py \
  ci/pf2-kingmaker-r36/test_runtime_json_integer.py \
  ci/pf2-kingmaker-r36/test_legacy_javac_dependencies.py \
  ci/pf2-kingmaker-certification/test_chapters_1_11_audit.py
for patch in \
  patch.py patch_runtime_json_integer.py patch_claim_hex_prepaid.py patch_kingdom_accounting.py \
  patch_claim_hex_ui.py patch_build_structure.py patch_build_structure_repairs.py \
  patch_structure_catalog_values.py patch_multi_structure.py patch_catalog_expansion.py \
  patch_official_build_ui.py patch_chapter11_gate.py patch_chapter9_canonical_anchors.py \
  patch_consumption_atomic.py patch_turn_phase_atomic.py patch_war_atomic.py \
  patch_semantic_domains.py patch_legacy_javac_dependencies.py; do
  python "ci/pf2-kingmaker-r36/$patch" "$PROJECT"
done
grep -Fx 'VERSION_NAME=9.0.37' "$PROJECT/version.properties"
python ci/pf2-kingmaker-r36/audit_catalog_expansion.py \
  --project "$PROJECT" --out "$WORK/catalog.json" --require-73
python ci/pf2-kingmaker-certification/audit_structure_catalog.py \
  --project "$PROJECT" --out "$WORK/structures.json" --require-zero-mismatches
javac -encoding UTF-8 -d "$WORK/classes" "$PROJECT/tools/JavaSyntaxValidator.java"
java -cp "$WORK/classes" JavaSyntaxValidator "$PROJECT/android/app/src/main/java"
SRC="$PROJECT/android/app/src/main/java/com/braseiro/pathfinder2e"
mapfile -d '' -t jsonsrc < <(find "$PROJECT/tools/runtime_json/org/json" -maxdepth 1 -type f -name '*.java' -print0)
javac -encoding UTF-8 -d "$WORK/classes" \
  "$SRC/KingmakerStructureSemantics.java" "$SRC/KingmakerCampaignGate.java" \
  "$SRC/KingmakerCompanionSemantics.java" "$SRC/KingmakerLeadershipSemantics.java" \
  ci/pf2-kingmaker-r36/semantic/R36BookSemanticsHarness.java \
  "${jsonsrc[@]}"
java -cp "$WORK/classes" com.braseiro.pathfinder2e.R36BookSemanticsHarness
for test in \
  test_r36_book_fidelity.py test_chapter11_production.py test_r36_claim_prepaid.py \
  test_build_structure_production.py test_build_structure_repairs_production.py \
  test_r36_ui_and_accounting_contract.py test_official_structure_production.py; do
  cp "ci/pf2-kingmaker-r36/$test" "$PROJECT/tests_pf2/$test"
done
cp ci/pf2-kingmaker-certification/kingmaker_official_structures_p544_552.json \
  "$PROJECT/tests_pf2/"
(cd "$PROJECT" && python -m pytest -q \
  tests_pf2/test_r36_book_fidelity.py tests_pf2/test_chapter11_production.py \
  tests_pf2/test_r36_claim_prepaid.py tests_pf2/test_build_structure_production.py \
  tests_pf2/test_build_structure_repairs_production.py \
  tests_pf2/test_r36_ui_and_accounting_contract.py \
  tests_pf2/test_official_structure_production.py)
if [[ "$MODE" = "full" ]]; then
  (cd "$PROJECT" && python -m pytest -q tests_pf2)
fi
OUT="$ROOT/.git/pf2-r36-codespace-evidence"
mkdir -p "$OUT"
python ci/pf2-kingmaker-certification/audit_chapters_1_11.py \
  --project "$PROJECT" --out "$OUT/KINGMAKER_CHAPTERS_1_11.json" --fail-on-critical
python ci/pf2-kingmaker-certification/official_part_semantic_audit.py \
  --project "$PROJECT" --out "$OUT"
python ci/pf2-kingmaker-certification/crosswalk_runtime_audit.py \
  --project "$PROJECT" --out "$OUT"
echo "R36_CODESPACE_GATES=PASS MODE=$MODE"
echo "R36_APK_ASSEMBLED=false"
echo "R36_ANDROID_E2E_CERTIFIED=false"
echo "EVIDENCE=$OUT"
