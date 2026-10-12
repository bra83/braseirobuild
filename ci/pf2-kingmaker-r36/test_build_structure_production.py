"""Source-level official Build Structure contract on actual R36 Java,
NOT an Android/SQLite runtime test."""
from pathlib import Path
src=(Path(__file__).resolve().parents[1]/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java").read_text(encoding="utf-8")
a=src.index("public JSONObject resolveBuildStructure(")
b=src.index("public JSONObject resolveDemolish(",a)
s=src[a:b]

def test_spending_is_before_roll_inside_transaction():
    assert 'db.runInTransactionChecked(' in s
    assert 'adjustState(cost,"Build Structure: custo antecipado de RP e commodities")' in s
    assert s.index('adjustState(cost,') < s.index('recordCheckAtSettlement(townKey,"build_structure"')
    assert 'recordRpSpent(rp)' in s
    assert 'cost_paid_before_check' in s

def test_unfinished_and_rubble_are_persisted_and_occupy_lots():
    assert '"under_construction_"+chosenKey' in s
    assert '"rubble_"+chosenKey' in s
    assert 'insertWithOnConflict("kingmaker_structures"' in s
    assert 'v.put("effects_json",' in s
    assert '.put("construction_status",rubble?"rubble":"unfinished")' in s
    assert 'refreshSettlementOccupancy(townKey)' in s
    assert 'must_demolish' in s

def test_success_reuses_prepaid_cost_and_refunds_half_commodities_only():
    assert 'buildStructure(townKey,structureKey,notes,true,buildOptions)' in s
    for k in ("lumber","stone","ore","luxuries"):
        assert f'put("{k}",{k}/2)' in s
    assert 'put("resource_points",rp/2)' not in s
    assert 'if(!prepaid)recordRpSpent(rpCost);' in src

def test_fail_closed_checks_before_paying():
    portion=s[:s.index('adjustState(cost,')]
    for key in ("naturalRoll<1","prereq.length()>0","used+lots>","s.optInt(\"resource_points\")<rp","s.optInt(\"lumber\")<lumber","rubbleKey"):
        assert key in portion,key
