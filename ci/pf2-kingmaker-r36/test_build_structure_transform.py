"""Build Structure fail-closed and official consequence contracts.
Synthetic strings are not Android execution or live SQLite evidence.
"""
from pathlib import Path
import importlib.util
import pytest

P=Path(__file__).with_name("patch_build_structure.py")
spec=importlib.util.spec_from_file_location("buildfix",P)
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

FIXTURE="""
public JSONObject buildStructure(final String settlementKey, final String structureKey, final String notes) throws Exception {
if (s.optInt("resource_points", 0) < rpCost) {
if (s.optInt("lumber", 0) < lumberCost) {
if (s.optInt("stone", 0) < stoneCost) {
if (s.optInt("ore", 0) < oreCost) {
if (s.optInt("luxuries", 0) < luxuriesCost) {
sv.put("resource_points", s.optInt("resource_points") - rpCost);
sv.put("lumber", s.optInt("lumber") - lumberCost);
sv.put("stone", s.optInt("stone") - stoneCost);
sv.put("ore", s.optInt("ore") - oreCost);
sv.put("luxuries", s.optInt("luxuries") - luxuriesCost);
recordRpSpent(rpCost);
}
    public JSONObject demolishStructure(
    public JSONObject resolveBuildStructure(String settlementKey, String structureKey, int naturalRoll, String skillKey, String notes) throws Exception {
        JSONObject check=recordCheck("build_structure");
    }
    public JSONObject resolveDemolish(
"""

def test_official_civic_rules_and_persistence_contract():
    x=m.transform(FIXTURE)
    part=x[x.index("public JSONObject resolveBuildStructure("):]
    assert 'new JSONObject().put("resource_points",-rp)' in part
    assert part.index('adjustState(cost,')<part.index('recordCheck("build_structure"')
    assert 'recordRpSpent(rp)' in part
    assert 'if(!prepaid)recordRpSpent(rpCost);' in x
    assert 'buildStructure(townKey,structureKey,notes,true)' in part
    assert '"construction_status",rubble?"rubble":"unfinished"' in part
    assert '"under_construction_"+chosenKey' in part
    assert '"rubble_"+chosenKey' in part
    assert 'refreshSettlementOccupancy(townKey)' in part
    assert 'lumber/2' in part and 'stone/2' in part and 'ore/2' in part and 'luxuries/2' in part
    assert 'put("resource_points",rp/2)' not in part
    assert 'db.runInTransactionChecked(' in part
    assert '"costs_stay_spent",true' in part

def test_drift_and_reapply_are_rejected():
    updated=m.transform(FIXTURE)
    with pytest.raises(RuntimeError):m.transform(updated)
    with pytest.raises(RuntimeError):m.transform("irrelevant Java source")

def test_wrong_version_does_not_mutate_file(tmp_path):
    p=tmp_path/m.REL
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(FIXTURE)
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.36")
    with pytest.raises(RuntimeError):m.apply(tmp_path)
    assert p.read_text()==FIXTURE
    (tmp_path/"version.properties").write_text("VERSION_NAME=9.0.37")
    m.apply(tmp_path)
    assert 'cost_prepaid' in p.read_text()

def test_fail_closed_if_cost_debit_anchor_drifts():
    s=FIXTURE.replace('sv.put("stone", s.optInt("stone") - stoneCost);',"// moved elsewhere")
    with pytest.raises(RuntimeError):m.transform(s)

def test_unfinished_retry_is_gated_by_kingdom_turn():
    updated=m.transform(FIXTURE)
    resolver=updated[updated.index("public JSONObject resolveBuildStructure("):]
    assert 'previous.optInt("last_attempt_turn",-1)==turn.optInt("turn_number")' in resolver
    assert 'Obra inacabada: a próxima tentativa deve ocorrer em outro turno' in resolver
    assert '.put("last_attempt_turn",turn.optInt("turn_number"))' in resolver
    assert resolver.index('last_attempt_turn",-1') < resolver.index('adjustState(cost,')
