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
    res = subprocess.run([javac, "-encoding", "UTF-8", "-cp", str(android_jar), "-d", str(out_classes), str(SEMANTIC_FILE)],
                         capture_output=True, text=True)
    assert res.returncode == 0, f"javac failed: {res.stderr}\n{res.stdout}"
