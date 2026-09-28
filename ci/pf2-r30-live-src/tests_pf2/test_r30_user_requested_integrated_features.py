import hashlib
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
ASSET = ROOT / "android/app/src/main/assets/narrative/PERFIL_TECNICO_MESTRE_HUMANO_IMERSIVO_VTT.md"
JAVA_HOME = Path("C:/Program Files/Android/Android Studio/jbr")
JAVAC = str(JAVA_HOME / "bin/javac.exe") if (JAVA_HOME / "bin/javac.exe").exists() else "javac"
JAVA = str(JAVA_HOME / "bin/java.exe") if (JAVA_HOME / "bin/java.exe").exists() else "java"

def read(name):
    return (SRC / name).read_text(encoding="utf-8")

def test_kingdom_turn_guide_uses_rules_engine_behaviorally():
    out = ROOT / "build_test_classes_turn_guide"
    out.mkdir(parents=True, exist_ok=True)
    cmd = [
        JAVAC, "-encoding", "UTF-8", "-d", str(out),
        str(ROOT / "tools/runtime_json/org/json/JSONObject.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONArray.java"),
        str(ROOT / "tools/runtime_json/org/json/JSONException.java"),
        str(SRC / "Pathfinder2eRulesEngine.java"),
        str(SRC / "KingmakerWarfareEngine.java"),
        str(SRC / "KingmakerRulesEngine.java"),
        str(ROOT / "tests_pf2/java/KingmakerTurnGuideRulesHarness.java"),
    ]
    c = subprocess.run(cmd, capture_output=True, text=True)
    assert c.returncode == 0, c.stderr
    r = subprocess.run([JAVA, "-cp", str(out), "com.braseiro.pathfinder2e.KingmakerTurnGuideRulesHarness"], capture_output=True, text=True)
    assert r.returncode == 0, r.stderr
    assert "KINGMAKER_TURN_GUIDE_RULES_HARNESS_PASS" in r.stdout

def test_per_character_control_is_player_or_master_and_persistent():
    db = read("AppDb.java")
    party = read("PartyDirector.java")
    main = read("MainActivity.java")
    assert 'return "gm"' in db and 'return "player"' in db
    assert "saveCharacterControl" in db and "getCharacterControl" in db
    assert "hasGmControlledHeroes" in party
    assert "controlPolicyForPrompt" in party
    assert "pending_test/combat/transação estruturada" in party
    assert "Jogador — eu controlo" in main
    assert "Mestre — Barbara controla" in main
    assert "ASSUMIR CONTROLE" in main
    assert "PASSAR AO MESTRE" in main

def test_master_runtime_receives_control_policy_and_human_narrative_direction():
    orchestrator = read("MasterOrchestrator.java")
    profile = read("HumanMasterNarrativeProfile.java")
    party = read("PartyDirector.java")
    assert 'root.put("party_control_policy",party.controlPolicyForPrompt())' in orchestrator
    assert "party.hasGmControlledHeroes()" in orchestrator
    assert 'root.put("human_master_narrative_direction"' in orchestrator
    assert "humanNarrativeProfile.direction(action,root,bPlan)" in orchestrator
    assert "mechanics_rule" in party
    for scene in ("OPENING","TRANSITION","TRAVEL","ARRIVAL","EXPLORATION","SOCIAL","INVESTIGATION","COMBAT","CHASE","DANGER","HORROR","DISCOVERY","REVELATION","REST","DOWNTIME","CLIMAX"):
        assert scene in profile
    assert "RULES ENGINE -> EVENT RESULT -> NARRATIVE DIRECTOR -> GM VOICE" in profile
    assert "Nunca decidir ação, intenção, fala ou emoção do personagem controlado pelo JOGADOR" in profile

def test_canonical_narrator_profile_asset_is_exact_and_runtime_loaded():
    assert ASSET.exists(), "perfil narrativo canônico não foi empacotado"
    data = ASSET.read_bytes()
    assert hashlib.sha256(data).hexdigest() == "8e63dbdf963abd1bed88e7173de3adc22d1501557647bcf4a9d95ee096f75852"
    text = data.decode("utf-8")
    assert "# PERFIL TÉCNICO CANÔNICO — MESTRE HUMANO IMERSIVO PARA VTT" in text
    assert "# 115. RELAÇÃO COM O MOTOR BÁRBARA" in text
    assert "# 116. PIPELINE RECOMENDADO" in text
    profile = read("HumanMasterNarrativeProfile.java")
    assert "PERFIL_TECNICO_MESTRE_HUMANO_IMERSIVO_VTT.md" in profile
    assert "source_sha256" in profile
    assert "DIRECAO_NARRATIVA" in profile

def test_kingdom_guide_is_not_a_parallel_manual():
    guide = read("KingmakerTurnGuide.java")
    rules = read("KingmakerRulesEngine.java")
    main = read("MainActivity.java")
    assert "rules.turnProcedureGuide()" in guide
    assert "rules.activityGuideCatalog()" in guide
    assert "repository.activeTurn()" in guide
    assert "repository.vacantLeadershipRoles()" in guide
    assert "repository.settlements()" in guide
    assert "repository.tradeAgreements()" not in guide or "repository." in guide
    assert "activityGuideCatalog()" in rules
    assert "KingmakerRepository.resolveKingdomActivity" in rules
    assert "ⓘ GUIA COMPLETO DO TURNO" in main
    assert "showKingdomTurnGuideDialog" in main
    assert "IR PARA A PRÓXIMA DECISÃO" in main

def test_narrative_layer_does_not_replace_mechanical_authority():
    orchestrator = read("MasterOrchestrator.java")
    profile = read("HumanMasterNarrativeProfile.java")
    assert "authority" in orchestrator
    assert "O aplicativo é a autoridade mecânica e persistente" in orchestrator
    assert "truth_priority" in profile
    assert "estado mecânico real" in profile
    assert "regras implementadas" in profile
    assert "Não invente regra, sucesso ou falha" in profile
