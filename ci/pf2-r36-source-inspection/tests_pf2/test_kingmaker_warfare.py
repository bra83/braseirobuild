import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
JAVA_HOME = Path("C:/Program Files/Android/Android Studio/jbr")
JAVAC = str(JAVA_HOME / "bin/javac.exe") if (JAVA_HOME / "bin/javac.exe").exists() else "javac"
JAVA = str(JAVA_HOME / "bin/java.exe") if (JAVA_HOME / "bin/java.exe").exists() else "java"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


def test_warfare_engine_implements_all_14_conditions():
    engine = read("KingmakerWarfareEngine.java")
    conditions = [
        "COND_CONCEALED", "COND_DEFEATED", "COND_DESTROYED", "COND_DISTANT",
        "COND_EFFICIENT", "COND_ENGAGED", "COND_FORTIFIED", "COND_LOST",
        "COND_MIRED", "COND_OUTFLANKED", "COND_PINNED", "COND_ROUTED",
        "COND_SHAKEN", "COND_WEARY"
    ]
    for cond in conditions:
        assert cond in engine

    pt_terms = [
        "Oculto", "Derrotado", "Destruído", "Distante",
        "Eficiente", "Engajado", "Fortificado", "Perdido",
        "Atolado", "Flanqueado", "Encurralado", "Desbaratado",
        "Abalado", "Exausto"
    ]
    for term in pt_terms:
        assert term in engine


def test_warfare_engine_implements_all_basic_and_tactical_actions():
    engine = read("KingmakerWarfareEngine.java")
    basic = [
        "advance", "battle", "battle_melee", "battle_ranged",
        "disengage", "guard", "rally", "retreat"
    ]
    for b in basic:
        assert f'"{b}"' in engine

    tactics = [
        "all_out_assault", "battlefield_medicine", "counterattack",
        "covering_fire", "defensive_stance", "dirty_fighting",
        "false_retreat", "feint", "outflank",
        "overwhelming_bombardment", "taunt"
    ]
    for t in tactics:
        assert f'"{t}"' in engine

    pt_action_labels = [
        "Avançar", "Ataque Corpo a Corpo", "Ataque à Distância",
        "Desengajar", "Guardar", "Reagrupar", "Retirar",
        "Assalto Total", "Medicina de Campanha", "Contra-ataque",
        "Fogo de Cobertura", "Postura Defensiva", "Combate Sujo",
        "Falsa Retirada", "Finta", "Flanquear",
        "Bombardeio Avassalador", "Provocar"
    ]
    for label in pt_action_labels:
        assert label in engine


def test_defeat_destruction_recovery_and_reinforcement_mechanics():
    engine = read("KingmakerWarfareEngine.java")
    assert "resolveDamageAgainstDefeated" in engine
    assert "checkArmyDestruction" in engine
    assert "postBattleRecovery" in engine
    assert "calculateCasualties" in engine
    assert "reinforceArmy" in engine
    assert "flat < 15" in engine
    assert "flatCheckRoll" in engine or "flat_check" in engine
    assert "rout_threshold" in engine
    assert "getShakenValue" in engine
    assert "setShaken" in engine


def test_repository_and_service_wire_warfare_actions():
    repo = read("KingmakerRepository.java")
    service = read("KingmakerService.java")

    for token in (
        "resolveWarfareAction", "applyPostBattleRecovery",
        "calculateArmyCasualties", "reinforceArmy",
        "checkArmyDestruction", "updateArmyConditions", "setArmyTactics",
        "warfare()"
    ):
        assert token in repo

    for token in (
        "resolveWarfareAction", "postBattleRecovery",
        "armyCasualties", "reinforceArmy",
        "checkArmyDestruction", "updateArmyConditions", "setArmyTactics",
        "warfareCatalog", "warfare()"
    ):
        assert token in service

    assert 'ensureColumn(sql,"kingmaker_armies","scouting"' in repo


def test_warfare_pure_jvm_harness_passes():
    out_dir = ROOT / "build_test_classes_warfare"
    out_dir.mkdir(parents=True, exist_ok=True)
    compile_cmd = [
        JAVAC, "-encoding", "UTF-8", "-d", str(out_dir),
        str(ROOT / "tools/runtime_json/org/json/JSONObject.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONArray.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONException.java"),
        str(SRC / "Pathfinder2eRulesEngine.java"),
        str(SRC / "KingmakerWarfareEngine.java"),
        str(ROOT / "tests_pf2/java/KingmakerWarfareEngineHarness.java"),
    ]
    res_comp = subprocess.run(compile_cmd, capture_output=True, text=True)
    assert res_comp.returncode == 0, f"Compilation failed: {res_comp.stderr}"

    run_cmd = [
        JAVA, "-cp", str(out_dir),
        "com.braseiro.pathfinder2e.KingmakerWarfareEngineHarness"
    ]
    res_run = subprocess.run(run_cmd, capture_output=True, text=True)
    assert res_run.returncode == 0, f"Execution failed: {res_run.stderr}"
    assert "KINGMAKER_WARFARE_ENGINE_PASS" in res_run.stdout
