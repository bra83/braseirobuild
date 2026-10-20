"""Test verification for Kingmaker Turn & Leadership Activities Checklist."""
import re
import subprocess
from pathlib import Path
import pytest

ROOT = Path(__file__).resolve().parents[2]
SEMANTIC_FILE = Path(__file__).parent / "semantic/KingmakerTurnChecklist.java"
PATCH_SCRIPT = Path(__file__).parent / "patch_kingmaker_turn_checklist.py"

def test_checklist_structure_and_coverage():
    assert SEMANTIC_FILE.exists(), f"Missing {SEMANTIC_FILE}"
    content = SEMANTIC_FILE.read_text(encoding="utf-8")

    # Match all add(...) calls
    pattern = re.compile(r'add\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,', re.MULTILINE)
    matches = pattern.findall(content)

    assert len(matches) == 63, f"Expected exactly 63 activities, found {len(matches)}"

    phase_counts = {}
    for item_id, phase, phase_label, name_pt, name_en in matches:
        phase_counts[phase] = phase_counts.get(phase, 0) + 1
        assert len(item_id) > 0
        assert len(phase_label) > 0
        assert len(name_pt) > 0
        assert len(name_en) > 0

    assert phase_counts["UPKEEP"] == 5, f"Expected 5 UPKEEP, got {phase_counts.get('UPKEEP')}"
    assert phase_counts["COMMERCE"] == 5, f"Expected 5 COMMERCE, got {phase_counts.get('COMMERCE')}"
    assert phase_counts["LEADERSHIP"] == 25, f"Expected 25 LEADERSHIP, got {phase_counts.get('LEADERSHIP')}"
    assert phase_counts["REGION"] == 14, f"Expected 14 REGION, got {phase_counts.get('REGION')}"
    assert phase_counts["CIVIC"] == 2, f"Expected 2 CIVIC, got {phase_counts.get('CIVIC')}"
    assert phase_counts["ARMY"] == 12, f"Expected 12 ARMY, got {phase_counts.get('ARMY')}"

def test_checklist_javac_compilation(tmp_path):
    # Check for android.jar
    sdk_paths = [
        Path("C:/Users/bcruz/AppData/Local/Android/Sdk/platforms/android-36/android.jar"),
        Path("C:/Users/bcruz/AppData/Local/Android/Sdk/platforms/android-35/android.jar"),
        Path("C:/Program Files/Android/Android Studio/plugins/android/lib/android.jar")
    ]
    android_jar = None
    for p in sdk_paths:
        if p.exists():
            android_jar = p
            break

    if not android_jar:
        pytest.skip("android.jar not found in standard paths")

    javac_candidates = [
        Path("C:/Program Files/Android/Android Studio/jbr/bin/javac.exe"),
        "javac"
    ]
    javac = None
    for c in javac_candidates:
        if isinstance(c, Path) and c.exists():
            javac = str(c)
            break
        elif isinstance(c, str):
            try:
                subprocess.run([c, "-version"], capture_output=True, check=True)
                javac = c
                break
            except Exception:
                pass

    if not javac:
        pytest.skip("javac not found")

    out_classes = tmp_path / "classes"
    out_classes.mkdir()
    # Production has KingmakerRepository on the Android application classpath.
    # Compile the checklist against its exact public read-only contract so the
    # isolated test keeps checking real Java types instead of hiding an error.
    repository_stub = tmp_path / "KingmakerRepository.java"
    repository_stub.write_text("""
package com.braseiro.pathfinder2e;
import org.json.JSONObject;
import java.util.List;
import java.util.ArrayList;
public class KingmakerRepository {
    public JSONObject state(){return new JSONObject();}
    public JSONObject activeTurn(){return new JSONObject();}
    public int vacantLeadershipRoles(){return 0;}
    public List<JSONObject> leaders(){return new ArrayList<JSONObject>();}
}
""", encoding="utf-8")
    res = subprocess.run([javac, "-encoding", "UTF-8", "-cp", str(android_jar), "-d", str(out_classes), str(SEMANTIC_FILE), str(repository_stub)],
                         capture_output=True, text=True)
    assert res.returncode == 0, f"javac failed: {res.stderr}\n{res.stdout}"


def _apply_physical_dice_patch(tmp_path):
    """Execute the production patch against the existing inspected Android class."""
    import importlib.util
    import shutil

    reference = ROOT / "ci/pf2-r36-source-inspection/android/app/src/main/java/com/braseiro/pathfinder2e/Dice3DOverlay.java"
    assert reference.is_file(), "Canonical physical-dice test fixture is missing"
    target = tmp_path / "Dice3DOverlay.java"
    shutil.copyfile(reference, target)
    spec = importlib.util.spec_from_file_location("kingmaker_turn_checklist_patch", PATCH_SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.patch_player_initiated_dice(tmp_path)
    return module, target


def test_player_must_explicitly_launch_physical_dice(tmp_path):
    """Opening a dice request may not initiate a hidden or automatic roll."""
    patch, target = _apply_physical_dice_patch(tmp_path)
    content = target.read_text(encoding="utf-8")
    assert "BRASEIRO_DICE3D_EXPLICIT_USER_LAUNCH_V1" in content
    assert 'launch.setText("LANÇAR DADOS 3D")' in content
    assert 'if(done[0]||!dialog.isShowing()||!started[0])return;' in content
    assert 'started[0]=true;' in content
    assert 'lastNs[0]=System.nanoTime();diceView.postOnAnimation(ticker);' in content
    assert "ww.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);diceView.postOnAnimation(ticker);" not in content
    assert "public static boolean enabled(SharedPreferences prefs){return true;}" in content
    assert "PhysicalDiceEngine.create(physicalFormula,seed)" in content
    assert "session.step()" in content
    assert "apply.setOnClickListener" in content
    assert "callback.onResult(new RollResult(" in content
    previous = content
    patch.patch_player_initiated_dice(tmp_path)
    assert target.read_text(encoding="utf-8") == previous


def test_physical_dice_patch_rejects_unknown_source_before_writing(tmp_path):
    patch, target = _apply_physical_dice_patch(tmp_path)
    original = target.read_text(encoding="utf-8")
    target.write_text(original.replace(
        "BRASEIRO_DICE3D_EXPLICIT_USER_LAUNCH_V1", "MISSING_PATCH_MARKER"
    ).replace(
        'launch.setText("LANÇAR DADOS 3D")', 'launch.setText("ALTERADO")'
    ), encoding="utf-8")
    modified = target.read_text(encoding="utf-8")
    with pytest.raises(RuntimeError, match="SOURCE_DRIFT"):
        patch.patch_player_initiated_dice(tmp_path)
    assert target.read_text(encoding="utf-8") == modified


def test_advisor_uses_only_kingdom_state_without_secret_or_random_rolls():
    source = SEMANTIC_FILE.read_text(encoding="utf-8")
    start = source.index("public static List<AdvisorTip> advisorTips(")
    end = source.index("public static boolean hasUrgentAdvisorTip(", start)
    advisor = source[start:end]
    for contract in ("repository.state()", "repository.vacantLeadershipRoles()",
                     "repository.leaders()", "repository.activeTurn()"):
        assert contract in advisor
    assert 'leader.optInt("vacant",0)' in advisor
    assert "honestidade secreta de um NPC" in advisor
    assert "new Random(" not in advisor
    assert "new java.util.Random(" not in advisor
    assert "db.getWritableDatabase()" not in advisor
    patch = PATCH_SCRIPT.read_text(encoding="utf-8")
    assert patch.count("KingmakerTurnChecklist.showAdvisorDialog(MainActivity.this,kingmakerRepository)") >= 2
    assert "hasUrgentAdvisorTip(kingmakerRepository)" in patch


def test_guide_does_not_claim_kingdom_tax_rewards_rp_or_invent_dice():
    src = SEMANTIC_FILE.read_text(encoding="utf-8")
    taxes = src[src.index('add("collect_taxes"'):src.index('add("improve_lifestyle"')]
    unrest = src[src.index('add("quell_unrest"'):src.index('add("recruit_army"')]
    claim = src[src.index('add("claim_hex"'):src.index('add("clear_hex"')]
    assert "Não gera PR." in taxes
    assert "Sucesso crítico: +2" in taxes
    assert "Sucesso ou falha: +1" in taxes
    assert "reduz Agitação em 1d6" in unrest
    assert "duas Ruínas distintas" in unrest
    assert "cobra exatamente 1 PR ANTES do teste" in claim
    assert "sem nova cobrança no sucesso" in claim
