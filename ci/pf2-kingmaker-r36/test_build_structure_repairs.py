"""Official repair transformation and persistence contracts (synthetic, non-Android)."""
import importlib.util
from pathlib import Path
import pytest
P=Path(__file__).with_name("patch_build_structure_repairs.py")
spec=importlib.util.spec_from_file_location("repair",P)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
fixture="""
int modifier=rules.skillModifier(s.optInt("level",1),score,skill.optString("rank"),skill.optInt("item_bonus"),leadership,vacancy-ruin+circumstanceBonus-claimPenalty,s.optInt("unrest"));
v.put("item",skill.optInt("item_bonus"));
    public JSONObject resolveRepairStructure(String settlementKey, String structureKey, String notes) throws Exception {
        return repairStructure(settlementKey,structureKey,notes);
    }
    public JSONObject resolveUpgradeSettlement(
return resolveRepairStructure(p.optString("settlement_key"), p.optString("structure_key"), notes);
"""
def test_repair_is_prepaid_and_applies_item_bonus():
    out=m.transform(fixture)
    assert 'Math.max(skill.optInt("item_bonus"),2)' in out
    assert '.put("item",civicItemBonus)' in out
    block=out[out.index('public JSONObject resolveRepairStructure(String settlementKey,String structureKey,int naturalRoll'):out.index('public JSONObject resolveUpgradeSettlement(')]
    assert block.index('adjustState(new JSONObject().put("resource_points",-rp)') < block.index('recordCheck("repair_structure"')
    assert 'recordRpSpent(rp)' in block
    assert 'def.optInt("lumber",0)/2' in block
    assert 'def.optInt("rp",0)/2' in block
    assert '"rubble_"+structure' in block
    assert 'v.put("effects_json",' in block
    assert 'applyStructureSkillBonuses()' in block
    assert 'refreshSettlementOccupancy(townKey)' in block
    assert 'lumber/2' in block
    assert 'db.runInTransactionChecked(' in block
    assert 'nat, skill, notes' in out
def test_fail_closed_on_drift_and_reapply():
    with pytest.raises(RuntimeError):m.transform("other game")
    with pytest.raises(RuntimeError):m.transform(m.transform(fixture))
def test_version_rejects_old_source_without_changes(tmp_path):
    p=tmp_path/m.REL;p.parent.mkdir(parents=True)
    p.write_text(fixture)
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.36")
    with pytest.raises(RuntimeError):m.apply(tmp_path)
    assert p.read_text()==fixture
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.37")
    m.apply(tmp_path)
    assert 'civicItemBonus' in p.read_text()
