#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
java = root / "android/app/src/main/java/com/braseiro/pathfinder2e"
main = (java / "MainActivity.java").read_text("utf-8")
master = (java / "MasterOrchestrator.java").read_text("utf-8")
diversifier = (java / "SceneOptionDiversifier.java").read_text("utf-8")
validator = (java / "MasterResponseValidator.java").read_text("utf-8")
version = (root / "version.properties").read_text("utf-8")

checks = []
def check(label, condition): checks.append((label, bool(condition)))

version_match = re.search(r"VERSION_NAME=(\d+)\.(\d+)\.(\d+)", version)
version_tuple = tuple(map(int, version_match.groups())) if version_match else (0, 0, 0)
check("versão 4.3.4+", version_tuple >= (4, 3, 4))
check("diversificador presente", "class SceneOptionDiversifier" in diversifier)
check("histórico persistente de múltiplas cenas", any(x in diversifier for x in ("MAX_HISTORY = 12", "MAX_HISTORY = 24", "MAX_HISTORY = 60")))
check("máximo de seis sugestões", "MAX_OPTIONS = 6" in diversifier and "out.length()<6" in validator)
check("igualdade e paráfrase são comparadas", all(token in diversifier for token in ("jaccard >= 0.64", "containment >= 0.78", "x.contains(y)", "y.contains(x)", "trigramDice")))
check("fallback usa contexto concreto", all(token in diversifier for token in ("present_npcs_json", "clues_json", "threats_json", "location")))
check("options vazio não força sugestão genérica", "isGroundedInScene" in diversifier and "painel nunca desapareça" not in diversifier and "int wanted=hasScene?MAX_OPTIONS" not in diversifier)
check("MainActivity integra histórico persistente", all(token in main for token in ("SceneOptionDiversifier.parseHistory", "SceneOptionDiversifier.appendHistory", "SceneOptionDiversifier.diversify")) and "getSceneOptionHistory" in main)
check("nova aventura limpa histórico", "saveSceneOptionHistory(\"[]\")" in main)
check("prompt recebe opções anteriores", 'story.put("avoid_options"' in master and "story.avoid_options" in main)
check("prompt proíbe repetição e paráfrase", "Não repita, não parafraseie" in main)
check("cache narrativo desativado na leitura", "if(plan==null||plan.narrative)return \"\"" in master)
check("cache narrativo desativado na gravação", "if(plan==null||plan.narrative||raw==null" in master)
check("fingerprint inclui história independente", "story_signature=" in master and "updated_at" in master and "last_narration" in master)
check("contexto emergencial preserva story", 'emergency.put("story"' in master)
check("changelog documenta a causa", (root / "CHANGELOG_4.3.4.md").is_file() and "cache" in (root / "CHANGELOG_4.3.4.md").read_text("utf-8").lower())

failed = [label for label, ok in checks if not ok]
for label, ok in checks:
    print(("OK  " if ok else "FALHA  ") + label)
print(f"RESULTADO: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.")
if failed:
    sys.exit(1)
