"""R36 official 73-row catalog generator contracts. Synthetic transformation only."""
import importlib.util
import json
from pathlib import Path
import pytest

P=Path(__file__).with_name("patch_catalog_expansion.py")
sp=importlib.util.spec_from_file_location("r36_catalog_expansion",P)
m=importlib.util.module_from_spec(sp)
sp.loader.exec_module(m)
rows=m.official_manifest()

def fake_rules():
    # The 18 official entries were corrected by the previous transformation.
    special={"ALCHEMY LABORATORY":"alchemist","STABLE":"stables"}
    prior=["academy","alchemist","bank","barracks","brewery","castle",
       "cathedral","embassy","garrison","granary","library","monument",
       "pier","smithy","stables","temple","theater","town_hall"]
    method='public JSONObject structure(String key){\n String k=key;\n'
    for key in prior:method+=f'if("{key}".equals(k)){{return new JSONObject();}}\n'
    return method+'return null;\n}\n    public JSONObject next(){return new JSONObject();}\n'

def test_manifest_has_73_unique_canonical_keys():
    assert len(rows)==73
    assert len({m.key_for(r["official_name"]) for r in rows})==73
    assert len([r for r in rows if r["lots"] is None])==6
    assert len([r for r in rows if " or " in r["cost_text"]])==2

def test_generated_catalog_preserves_all_prior_entries():
    before=fake_rules()
    after,report=m.transform_rules(before,rows)
    assert len(report["generated"])==55
    assert len(report["existing"])>=18
    for key in report["generated"]:
        assert f'if("{key}".equals(r36OfficialKey))' in after
    for key in ["academy","castle","town_hall","alchemist","stables"]:
        assert f'if("{key}".equals(k))' in after
    assert 'special_effects_semantic_certification","PENDING"' in after
    assert 'construction_dc' in after
    with pytest.raises(ValueError,match="twice"):
        m.transform_rules(after,rows)

def test_mixed_skill_options_do_not_accept_fake_or_skill():
    skills,rank=m.skill_data("Defense (master), Industry (master), Magic (master), or Statecraft (master)")
    assert skills==["defense","industry","magic","statecraft"]
    assert rank=="master"

def test_alternative_costs_and_special_placement_are_explicit():
    bridge=next(r for r in rows if r["official_name"]=="BRIDGE")
    watch=next(r for r in rows if r["official_name"]=="WATCHTOWER")
    assert 'requires_special_placement",true' in m.construct_expression(bridge,"bridge")
    assert 'requires_cost_choice",true' in m.construct_expression(bridge,"bridge")
    assert 'requires_cost_choice",true' in m.construct_expression(watch,"watchtower")
    assert 'requires_cost_choice",true' not in m.construct_expression(rows[0],"academy")

def test_build_repair_fail_closed_without_tender_and_lots():
    anchor='if(town.length()==0||def==null)throw new IllegalArgumentException("Assentamento ou estrutura inválidos.");'
    result=m.transform_repository(anchor+"\n"+anchor)
    assert result.count('requires_special_placement')==2
    assert result.count('requires_cost_choice')==2
    with pytest.raises(ValueError,match="drift"):
        m.transform_repository(anchor)

def test_source_drift_fails_closed():
    with pytest.raises(ValueError):
        m.transform_rules(fake_rules().replace('structure(String key)','notStructure(String key)'),rows)
    with pytest.raises(ValueError,match="No original"):
        m.transform_rules('public JSONObject structure(String key){} \n    public JSONObject next() {}',rows)
