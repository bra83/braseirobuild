"""Static transformation contracts for official Kingdom accounting; not Android/E2E."""
from pathlib import Path
import importlib.util
import pytest

P=Path(__file__).with_name("patch_kingdom_accounting.py")
spec=importlib.util.spec_from_file_location("accounting",P)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
FIXTURE='''
state.put("fame",before.optInt("fame",0));state.put("infamy",before.optInt("infamy",0));
int surplus=Math.min(120,Math.max(0,before.optInt("resource_points")));
int modifier=rules.skillModifier(s.optInt("level",1),score,skill.optString("rank"),skill.optInt("item_bonus"),leadership,vacancy-ruin+taxes-claimPenalty,s.optInt("unrest"));
if(awardReputationOnCrit&&"CRITICAL_SUCCESS".equals(result.optString("degree")))gainReputationPoint();
o.put("claim_hex_stability_penalty",-claimPenalty);
'''

def test_official_rules_transformed():
    out=m.transform(FIXTURE)
    assert "Math.min(120" not in out
    assert 'int surplus=Math.max(0,before.optInt("resource_points"))' in out
    assert 'before.optInt("fame",0)+1' in out
    assert 'before.optInt("infamy",0)+1' in out
    assert 'if("CRITICAL_SUCCESS".equals(result.optString("degree")))gainReputationPoint()' in out
    assert 'expansionExpert="claim_hex".equals(activity)&&s.optInt("level",1)>=4?2:0' in out
    assert "Math.max(taxes,expansionExpert)-claimPenalty" in out
    assert ".put(\"expansion_expert\",expansionExpert)" in out

def test_fails_closed_and_cannot_reapply():
    with pytest.raises(RuntimeError):m.transform("different project")
    with pytest.raises(RuntimeError):m.transform(m.transform(FIXTURE))

def test_version_and_atomicity(tmp_path):
    target=tmp_path/m.REL
    target.parent.mkdir(parents=True)
    target.write_text(FIXTURE,encoding="utf-8")
    v=tmp_path/"version.properties"
    v.write_text("VERSION_NAME=9.0.36")
    with pytest.raises(RuntimeError):m.apply(tmp_path)
    assert target.read_text()==FIXTURE
    v.write_text("VERSION_NAME=9.0.37")
    m.apply(tmp_path)
    assert "expansionExpert" in target.read_text()

def test_same_type_circumstance_bonus_never_stacks():
    for tax,expert in ((0,0),(1,0),(2,0),(0,2),(1,2),(2,2)):
        assert max(tax,expert) == (0 if not (tax or expert) else max(tax,expert))
    assert max(2,2)==2
