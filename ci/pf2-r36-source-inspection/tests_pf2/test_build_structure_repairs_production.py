"""Real R36 Java source guards for Build Structure repair rules.
These are NOT device-level SQLite or endurance tests."""
from pathlib import Path
source=(Path(__file__).resolve().parents[1]/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java").read_text(encoding="utf-8")
a=source.index('public JSONObject resolveRepairStructure(String settlementKey,String structureKey,int naturalRoll,')
b=source.index('public JSONObject resolveUpgradeSettlement(',a)
method=source[a:b]
def test_half_cost_paid_before_check():
    assert 'def.optInt("rp",0)/2' in method
    for k in ("lumber","stone","ore","luxuries"):
        assert f'def.optInt("{k}",0)/2' in method
    assert method.index('adjustState(new JSONObject().put("resource_points",-rp)')<method.index('recordCheck("repair_structure"')
    assert 'recordRpSpent(rp);' in method
    assert 'db.runInTransactionChecked(' in method
def test_critical_rubble_and_critical_commodity_refund_persisted():
    assert 'v.put("structure_key","rubble_"+structure)' in method
    assert '.put("construction_status","rubble")' in method
    assert 'applyStructureSkillBonuses();' in method
    assert 'refreshSettlementOccupancy(townKey);' in method
    assert 'oldEffects.put("construction_status","complete").put("damaged",false)' in method
    for k in ("lumber","stone","ore","luxuries"):assert f'put("{k}",{k}/2)' in method
def test_existing_repair_item_bonus_does_not_stack():
    assert '"repair_structure".equals(activity)?Math.max(skill.optInt("item_bonus"),2)' in source
    assert '.put("item",civicItemBonus)' in source
def test_activity_dispatcher_uses_real_roll_and_skill():
    assert 'return resolveRepairStructure(p.optString("settlement_key"), p.optString("structure_key"), nat, skill, notes);' in source

def test_repair_prepayment_survives_failure_without_same_turn_exploit():
    assert 'oldEffects.optBoolean("repair_cost_prepaid",false)' in method
    assert 'if(repairPending&&oldEffects.optInt("last_attempt_turn",-1)==turn.optInt("turn_number"))' in method
    assert 'if(!repairPending)' in method
    assert 'put("rp_spent",repairPending?0:rp)' in method
    assert '.put("repair_cost_prepaid",true)' in method
    assert 'oldEffects.remove("repair_cost_prepaid")' in method
