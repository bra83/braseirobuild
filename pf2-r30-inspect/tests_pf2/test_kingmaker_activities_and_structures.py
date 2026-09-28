from pathlib import Path
import json
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/braseiro/pathfinder2e"
JAVA_HOME = Path("C:/Program Files/Android/Android Studio/jbr")
JAVAC = str(JAVA_HOME / "bin/javac.exe") if (JAVA_HOME / "bin/javac.exe").exists() else "javac"
JAVA = str(JAVA_HOME / "bin/java.exe") if (JAVA_HOME / "bin/java.exe").exists() else "java"

def read(name):
    return (SRC / name).read_text(encoding="utf-8")

RULES = read("KingmakerRulesEngine.java")
REPO = read("KingmakerRepository.java")
SERVICE = read("KingmakerService.java")
CATALOG = json.loads((ROOT / "android/app/src/main/assets/pathfinder/kingmaker/kingmaker_catalog.json").read_text(encoding="utf-8"))

def test_canonical_structures_catalog_completeness():
    expected_structures = [
        "house", "tavern", "mill", "smithy", "market", "temple", "cathedral", "barracks",
        "wooden_palisade", "stone_wall", "castle", "brewery", "library", "academy", "bank",
        "granary", "stables", "town_hall", "festival_ground", "garrison", "embassy", "theater",
        "pier", "alchemist", "graveyard", "monument"
    ]
    assert len(expected_structures) == 26
    for st in expected_structures:
        assert f'"{st}"' in RULES
    assert "structuresCatalog" in RULES
    assert "structuresCatalog" in SERVICE
    assert "applyStructureSkillBonuses" in REPO
    assert "buildStructure" in REPO
    assert "demolishStructure" in REPO
    assert "repairStructure" in REPO
    assert "upgradeSettlement" in REPO

def test_canonical_activities_catalog_completeness():
    leadership_activities = [
        "new_leadership", "proclamation", "rest_and_relax", "supernatural_solution",
        "take_charge", "send_diplomatic_envoy", "pledge_of_fealty", "request_foreign_aid", "quell_unrest"
    ]
    region_activities = [
        "claim_hex", "abandon_hex", "clear_hex", "reconnoiter_hex",
        "build_roads", "irrigate", "establish_settlement", "work_sites", "fortify_hex"
    ]
    civic_activities = [
        "build_structure", "demolish", "repair_structure", "upgrade_settlement"
    ]
    commerce_activities = [
        "collect_taxes", "improve_lifestyle", "tap_treasury", "trade_commodities",
        "manage_trade_agreements", "establish_trade_agreement", "capital_investment"
    ]
    all_activities = leadership_activities + region_activities + civic_activities + commerce_activities
    assert len(all_activities) == 29

    for act in all_activities:
        assert f'"{act}"' in RULES
        assert f'"{act}".equals(k)' in REPO or f'"{act}"' in REPO

    assert "activityCatalog" in RULES
    assert "activityCatalog" in SERVICE
    assert "resolveKingdomActivity" in REPO
    assert "resolveActivity" in SERVICE

def test_unrest_anarchy_and_ruin_mechanics():
    # Anarchy threshold
    assert "ANARCHY_UNREST_THRESHOLD=20" in RULES or "ANARCHY_UNREST_THRESHOLD = 20" in RULES
    assert "isAnarchy" in RULES
    assert "isRestorativeActivity" in RULES
    assert "canPerformActivity" in RULES
    assert "isAnarchy" in SERVICE

    # Restorative activities permitted in Anarchy
    for rest in ("quell_unrest", "rest_and_relax", "proclamation", "supernatural_solution", "improve_lifestyle"):
        assert f'"{rest}"' in RULES

    # Ruin to Ability mapping
    assert 'ruinForAbility' in RULES
    assert 'abilityForRuin' in RULES
    assert 'ruinLabel' in RULES
    assert '"corruption"' in RULES and '"culture"' in RULES
    assert '"crime"' in RULES and '"economy"' in RULES
    assert '"decay"' in RULES and '"stability"' in RULES
    assert '"strife"' in RULES and '"loyalty"' in RULES

    # Unrest penalty formula
    assert "unrestPenalty" in RULES
    assert "value>=15?4:value>=10?3:value>=5?2:value>=1?1:0" in RULES or "unrest >= 15 ? 4" in RULES or "value >= 15" in RULES

    # Anarchy blocking check in recordCheck
    assert "O Reino está em Anarquia (Unrest >= 20)" in REPO

def test_event_phase_dc_decay_and_catalog():
    assert "BASE_EVENT_DC=16" in RULES or "BASE_EVENT_DC = 16" in RULES
    assert "nextEventDc" in RULES
    assert "checkEventOccurs" in RULES
    assert "eventsCatalog" in RULES
    assert "resolveEventPhaseCheck" in REPO
    assert "resolveKingdomEvent" in REPO
    assert "resolveEventPhaseCheck" in SERVICE
    assert "resolveKingdomEvent" in SERVICE

    expected_events = [
        "good_harvest", "mineral_discovery", "trade_fair", "visiting_scholar", "visiting_dignitaries",
        "new_settlers", "patriotic_inspiration", "wandering_monsters", "bandit_activity", "crop_blight",
        "epidemic", "political_unrest", "building_collapse", "court_scandal", "settlement_fire", "natural_disaster"
    ]
    assert len(expected_events) == 16
    for ev in expected_events:
        assert f'"{ev}"' in RULES

def test_service_facades_wiring():
    facade_methods = [
        "buildStructure", "demolishStructure", "repairStructure", "upgradeSettlement",
        "resolveActivity", "resolveEventPhaseCheck", "resolveKingdomEvent", "quellUnrest",
        "isAnarchy", "structuresCatalog", "activityCatalog", "eventsCatalog"
    ]
    for method in facade_methods:
        assert f"public JSONObject {method}" in SERVICE or f"public JSONArray {method}" in SERVICE or f"public boolean {method}" in SERVICE

def test_catalog_json_reflects_anarchy_and_events():
    assert CATALOG["resources"]["unrest_penalties"]["20+"] == "Anarquia"
    assert CATALOG["resources"]["anarchy"]["threshold"] == 20
    assert "quell_unrest" in CATALOG["resources"]["anarchy"]["restorative_activities"]
    assert CATALOG["events"]["event_phase_check"]["initial_dc"] == 16
    assert CATALOG["events"]["event_phase_check"]["decay_on_no_event"] == 5
    assert CATALOG["events"]["event_phase_check"]["reset_dc_on_event"] == 16

def test_rules_engine_direct_execution():
    """Compiles and executes a lightweight test harness invoking KingmakerRulesEngine methods."""
    out_dir = ROOT / "build_test_classes_activities"
    out_dir.mkdir(parents=True, exist_ok=True)
    test_java = ROOT / "tests_pf2/java/TestKingmakerActivitiesAndStructures.java"
    test_java.parent.mkdir(parents=True, exist_ok=True)
    test_code = """package com.braseiro.pathfinder2e;
import org.json.JSONArray;
import org.json.JSONObject;

public class TestKingmakerActivitiesAndStructures {
    public static void main(String[] args) throws Exception {
        KingmakerRulesEngine engine = new KingmakerRulesEngine();

        // 1. Test Anarchy and Restorative checks
        if (!KingmakerRulesEngine.isAnarchy(20)) throw new AssertionError("Unrest 20 deve ser Anarquia");
        if (!KingmakerRulesEngine.isAnarchy(25)) throw new AssertionError("Unrest 25 deve ser Anarquia");
        if (KingmakerRulesEngine.isAnarchy(19)) throw new AssertionError("Unrest 19 não deve ser Anarquia");
        if (!KingmakerRulesEngine.isRestorativeActivity("quell_unrest")) throw new AssertionError("quell_unrest deve ser restauradora");
        if (!KingmakerRulesEngine.isRestorativeActivity("improve_lifestyle")) throw new AssertionError("improve_lifestyle deve ser restauradora");
        if (KingmakerRulesEngine.isRestorativeActivity("claim_hex")) throw new AssertionError("claim_hex não deve ser restauradora");
        if (KingmakerRulesEngine.canPerformActivity("claim_hex", 20)) throw new AssertionError("claim_hex bloqueado na Anarquia");
        if (!KingmakerRulesEngine.canPerformActivity("quell_unrest", 20)) throw new AssertionError("quell_unrest permitido na Anarquia");

        // 2. Test Event Phase DC decay
        int dc = 16;
        dc = KingmakerRulesEngine.nextEventDc(dc, false);
        if (dc != 11) throw new AssertionError("16 -> 11 ao não ocorrer evento");
        dc = KingmakerRulesEngine.nextEventDc(dc, false);
        if (dc != 6) throw new AssertionError("11 -> 6 ao não ocorrer evento");
        dc = KingmakerRulesEngine.nextEventDc(dc, false);
        if (dc != 1) throw new AssertionError("6 -> 1 ao não ocorrer evento");
        dc = KingmakerRulesEngine.nextEventDc(dc, false);
        if (dc != 1) throw new AssertionError("1 -> 1 (mínimo 1)");
        dc = KingmakerRulesEngine.nextEventDc(dc, true);
        if (dc != 16) throw new AssertionError("Reset para 16 ao ocorrer evento");

        // 3. Test Catalogs
        JSONArray structures = engine.structuresCatalog();
        if (structures.length() != 26) throw new AssertionError("Catálogo de estruturas deve conter 26 estruturas, obtido: " + structures.length());

        JSONArray activities = engine.activityCatalog();
        if (activities.length() != 29) throw new AssertionError("Catálogo de atividades deve conter 29 atividades, obtido: " + activities.length());

        JSONArray events = engine.eventsCatalog();
        if (events.length() != 16) throw new AssertionError("Catálogo de eventos deve conter 16 eventos, obtido: " + events.length());

        // 4. Test Ruin Mapping
        if (!"corruption".equals(KingmakerRulesEngine.ruinForAbility("culture"))) throw new AssertionError("Culture -> Corruption");
        if (!"crime".equals(KingmakerRulesEngine.ruinForAbility("economy"))) throw new AssertionError("Economy -> Crime");
        if (!"decay".equals(KingmakerRulesEngine.ruinForAbility("stability"))) throw new AssertionError("Stability -> Decay");
        if (!"strife".equals(KingmakerRulesEngine.ruinForAbility("loyalty"))) throw new AssertionError("Loyalty -> Strife");

        System.out.println("TESTS_PASSED_OK");
    }
}
"""
    tools_dir = ROOT / "tools"
    test_java.write_text(test_code, encoding="utf-8")
    try:
        compile_cmd = [
            JAVAC, "-encoding", "UTF-8", "-d", str(tools_dir),
            str(ROOT / "tools/runtime_json/org/json/JSONObject.java"),
            str(ROOT / "tools/runtime_json/org/json/JSONArray.java"),
            str(ROOT / "tools/runtime_json/org/json/JSONException.java"),
            str(SRC / "Pathfinder2eRulesEngine.java"),
            str(SRC / "KingmakerWarfareEngine.java"),
            str(SRC / "KingmakerRulesEngine.java"),
            str(test_java),
        ]
        res = subprocess.run(compile_cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
        assert res.returncode == 0, f"Compilation failed: {res.stderr}"

        run_cmd = [
            JAVA, "-cp", str(tools_dir),
            "com.braseiro.pathfinder2e.TestKingmakerActivitiesAndStructures"
        ]
        run_res = subprocess.run(run_cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
        assert run_res.returncode == 0, f"Execution failed: {run_res.stderr}"
        assert "TESTS_PASSED_OK" in run_res.stdout
    finally:
        if test_java.exists():
            test_java.unlink()

