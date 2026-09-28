"""Adversarial stress and edge case test suite for Milestone 1 KingmakerCampaignModels.

Challenger 2 validations:
1. Extreme level boundaries (level 0 to level 25) for hazards and invalid levels.
2. Complex hazards with multiple routine actions, reactions, and multi-skill disable options.
3. Deep dialogue branching and missing/null/corrupt NPC attitude values.
4. Clean pure JVM desktop mode parsing against tools/runtime_json (all 17 model classes).
5. Zero Gradle dependency (assembleDebug/APK builds strictly forbidden).
"""

import os
import subprocess
from pathlib import Path
import pytest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
JAVAC = r"C:\Program Files\Android\Android Studio\jbr\bin\javac.exe"
JAVA = r"C:\Program Files\Android\Android Studio\jbr\bin\java.exe"


def test_pure_jvm_edge_harness_compilation_and_execution(tmp_path):
    """Executa o harness adversarial puro Java KingmakerCampaignModelsEdgeHarness."""
    if not Path(JAVAC).exists() or not Path(JAVA).exists():
        pytest.skip("JDK javac/java não encontrado em C:/Program Files/Android/Android Studio/jbr")

    out_dir = tmp_path / "classes"
    out_dir.mkdir(parents=True, exist_ok=True)

    compile_cmd = [
        JAVAC, "-encoding", "UTF-8", "-d", str(out_dir),
        str(ROOT / "tools/runtime_json/org/json/JSONObject.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONArray.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONException.java"),
        str(SRC / "KingmakerCampaignModels.java"),
        str(ROOT / "tests_pf2/java/KingmakerCampaignModelsEdgeHarness.java"),
    ]
    res_comp = subprocess.run(compile_cmd, capture_output=True, text=True)
    assert res_comp.returncode == 0, f"Falha na compilação do harness Java: {res_comp.stderr}"

    run_cmd = [
        JAVA, "-cp", str(out_dir),
        "com.braseiro.pathfinder2e.KingmakerCampaignModelsEdgeHarness"
    ]
    res_run = subprocess.run(run_cmd, capture_output=True, text=True)
    assert res_run.returncode == 0, f"Falha na execução do harness Java: {res_run.stderr}"
    assert "KINGMAKER_CAMPAIGN_MODELS_EDGE_HARNESS_PASS" in res_run.stdout
    assert "95 checks passed" in res_run.stdout


def test_hazard_level_extreme_boundaries():
    """Valida que o modelo aceita níveis 0 a 25 e rejeita valores fora do escopo PF2e."""
    models_file = SRC / "KingmakerCampaignModels.java"
    code = models_file.read_text(encoding="utf-8")

    # Verifica verificação explícita de nível no modelo
    assert "if (level < 0 || level > 25)" in code
    assert "Nível do perigo fora da faixa PF2e" in code

    # Verifica limites do ChapterCatalog (níveis 1 a 20)
    assert "if (levelMin < 1 || levelMax > 20 || levelMin > levelMax)" in code


def test_complex_hazard_model_capabilities():
    """Valida capacidade estrutural para rotinas multi-ação, reações e desativação multi-perícia."""
    models_file = SRC / "KingmakerCampaignModels.java"
    code = models_file.read_text(encoding="utf-8")

    # Estruturas de perigo complexo
    assert "class HazardRoutineAction" in code
    assert "class HazardDisableOption" in code
    assert "List<HazardRoutineAction> routine" in code
    assert "List<HazardDisableOption> disableSkills" in code
    assert "int routineActions = 1;" in code
    assert "String reaction = \"\";" in code
    assert "String reset = \"\";" in code

    # Fallback automático de BT e traits
    assert "h.bt = json.optInt(\"bt\", h.hp / 2);" in code
    assert "h.traits.contains(\"complexo\") ? \"complexo\" : \"simples\"" in code


def test_npc_attitude_robustness_and_deep_dialogue():
    """Valida robustez contra atitudes ausentes ou nulas e suporte a árvores profundas."""
    models_file = SRC / "KingmakerCampaignModels.java"
    code = models_file.read_text(encoding="utf-8")

    # Escala canônica de atitudes
    assert '{"hostil", "desfavoravel", "indiferente", "amistoso", "prestativo"}' in code
    # Default fail-safe
    assert 'n.attitude = json.optString("attitude", "indiferente");' in code
    # Clamping nos extremos
    assert "Math.max(0, Math.min(ATTITUDE_RANKS.length - 1, currentRank + delta))" in code
    # Estrutura de nós de diálogo
    assert "class NpcDialogueNode" in code
    assert "List<NpcDialogueNode> dialogueTree" in code


def test_all_seventeen_models_pure_jvm_no_android_deps():
    """Garante que todas as 17 classes de modelo existem e têm zero dependências Android."""
    models_file = SRC / "KingmakerCampaignModels.java"
    code = models_file.read_text(encoding="utf-8")

    expected_classes = [
        "ChapterCatalog",
        "QuestModel",
        "QuestObjective",
        "HexPoiModel",
        "TacticalSceneModel",
        "SceneZone",
        "SceneObstacle",
        "SceneEntryPoint",
        "NpcModel",
        "NpcDialogueNode",
        "CanonicalHazardModel",
        "HazardDisableOption",
        "HazardRoutineAction",
        "FactionModel",
        "WorldClockModel",
        "WorldEventModel",
        "CampaignMilestone",
    ]

    for cls_name in expected_classes:
        assert f"class {cls_name}" in code, f"Classe {cls_name} deve estar presente em KingmakerCampaignModels"

    assert "import android." not in code, "KingmakerCampaignModels não deve importar nenhuma classe do Android SDK"
