"""Regression checks against the REAL checked-in R35 MainActivity (1 MB)."""
from pathlib import Path
import importlib.util
import pytest

ROOT = Path(__file__).resolve().parents[1]
PATCH_FILE = Path(__file__).with_name("patch_galaxy_a57_compat.py")
BASE_FILE = ROOT / "pf2-r36-source-inspection" / "android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java"

def mod():
    spec = importlib.util.spec_from_file_location("r36_a57_compat", PATCH_FILE)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result

def test_a57_real_mainactivity_safe_area_touch_targets_and_tts_unchanged():
    m = mod()
    original = BASE_FILE.read_text(encoding="utf-8")
    revised = m.transform(original)
    assert len(m.PATCHES) == 11
    for old,new in m.PATCHES:
        assert original.count(old) == 1
        assert revised.count(new) == 1
    assert "if(parent instanceof View)applySystemBarInsets((View)parent);" in revised
    assert "bottom=Math.max(bottom,ime.bottom);" in revised
    assert "WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()" in revised
    assert "setContentView(shellHost);" in revised
    assert "applySystemBarInsets(shellHost);" in revised
    assert "new LinearLayout.LayoutParams(dp(48),dp(48))" in revised
    assert "new LinearLayout.LayoutParams(-2,dp(48))" in revised
    assert "new LinearLayout.LayoutParams(-1,dp(56))" in revised
    # Reversible source patch: no TTS, navigation, game rules or save logic removed.
    rolled_back = revised
    for old,new in reversed(m.PATCHES):
        rolled_back = rolled_back.replace(new,old,1)
    assert rolled_back == original
    for feature in ("SHERPA_KOKORO_ENGINE_PACKAGE", "com.samsung.SMT",
                    "DatabaseMigrationGuard.beforeOpen(this)",
                    "navigate(final String n)", "showCampaign()"):
        assert feature in revised

def test_a57_real_predictive_back_and_legacy_navigation():
    """Validate the actual R35 MainActivity transformation, including callbacks."""
    m = mod()
    original = m.transform(BASE_FILE.read_text(encoding="utf-8"))
    revised = m.transform_back(original)
    for old, new in m.BACK_CHANGES:
        assert original.count(old) == 1
        assert revised.count(new) == 1
    for invariant in (
        'installR36BackNavigation();',
        'registerOnBackInvokedCallback(',
        'unregisterOnBackInvokedCallback(r36BackCallback);',
        'public void onBackPressed(){handleR36BackNavigation();}',
        'if(navigationInProgress)return;',
        'navigate("Aventura");',
        'finish();',
        'DatabaseMigrationGuard.beforeOpen(this)',
        'SHERPA_KOKORO_ENGINE_PACKAGE',
        'com.samsung.SMT',
    ):
        assert invariant in revised, invariant
    reversed_source = revised
    for old, new in reversed(m.BACK_CHANGES):
        reversed_source = reversed_source.replace(new, old, 1)
    assert reversed_source == original


def test_a57_back_rejects_second_patch_atomically():
    m = mod()
    original = m.transform(BASE_FILE.read_text(encoding="utf-8"))
    patched = m.transform_back(original)
    with pytest.raises(RuntimeError, match="back-navigation source anchor changed"):
        m.transform_back(patched)
    assert patched.count('registerOnBackInvokedCallback(') == 2


def test_a57_patch_rejects_r35_and_changes_no_source(tmp_path):
    m = mod()
    version = tmp_path / "version.properties"
    version.write_text("VERSION_NAME=9.0.36\n",encoding="utf-8")
    target = tmp_path / m.MAIN
    target.parent.mkdir(parents=True,exist_ok=True)
    contents = BASE_FILE.read_text(encoding="utf-8")
    target.write_text(contents,encoding="utf-8")
    with pytest.raises(RuntimeError,match="never modify R35"):
        m.apply(tmp_path)
    assert target.read_text(encoding="utf-8") == contents

def test_a57_patch_applies_atomically_to_disposable_r36(tmp_path):
    m = mod()
    (tmp_path / "version.properties").write_text("VERSION_NAME=9.0.37\n",encoding="utf-8")
    target=tmp_path / m.MAIN
    target.parent.mkdir(parents=True,exist_ok=True)
    original=BASE_FILE.read_text(encoding="utf-8")
    target.write_text(original,encoding="utf-8")
    changed=m.apply(tmp_path)
    assert target.read_text(encoding="utf-8")==changed
    with pytest.raises(RuntimeError,match="anchor changed"):
        m.apply(tmp_path)
    assert target.read_text(encoding="utf-8")==changed
