"""Contract for replacing direct hex acquisition with official rules UI."""
from pathlib import Path
import importlib.util
import pytest
P=Path(__file__).with_name("patch_claim_hex_ui.py")
spec=importlib.util.spec_from_file_location("claimui",P)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

def test_direct_bypass_replaced_and_dialog_selects_allowed_skills():
    src='public class Demo {\n    private void showKingmakerHexDialog(){\n        else if(a==2)kingmakerRepository.claimHex(h,value(notes));\n    }\n}'
    out=m.transform(src)
    assert 'else if(a==2)kingmakerRepository.claimHex(' not in out
    assert 'showKingmakerClaimHexDialog(h,value(notes));return;' in out
    assert 'kingmakerRepository.resolveClaimHex(' in out
    assert 'String[] skills={"exploration","intrigue","magic","wilderness"}' in out
    assert 'screenCache.clear();showKingmaker();' in out

def test_source_drift_and_reapplication_rejected():
    fixture='    private void showKingmakerHexDialog(){\nelse if(a==2)kingmakerRepository.claimHex(h,value(notes));\n}'
    transformed=m.transform(fixture)
    with pytest.raises(RuntimeError):m.transform(transformed)
    with pytest.raises(RuntimeError):m.transform("some other Android Activity")

def test_version_guard_preserves_stable_source(tmp_path):
    f=tmp_path/m.REL
    f.parent.mkdir(parents=True)
    source='    private void showKingmakerHexDialog(){\nelse if(a==2)kingmakerRepository.claimHex(h,value(notes));\n}'
    f.write_text(source,encoding="utf-8")
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.36")
    with pytest.raises(RuntimeError):m.apply(tmp_path)
    assert f.read_text()==source
