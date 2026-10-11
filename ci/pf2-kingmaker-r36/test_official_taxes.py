"""R36 A01 official Kingmaker p.530 source and migration behavioral protections.
Tests run on extracted R35 source after patch. Actual APK build compiles Java.
"""
from pathlib import Path
import re
SRC=Path(__file__).resolve().parents[1]/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java"
def source():return SRC.read_text(encoding="utf-8")
def test_collect_taxes_no_fake_rp():
    s=source()
    body=s.split("public JSONObject resolveCollectTaxes(int naturalRoll, String skillKey, String notes, String criticalFailureRuin)",1)[1].split("\n    public JSONObject ",1)[0]
    assert 'put("rp_gain",0)' in body
    assert 'economy_circumstance_bonus' in body
    assert '"resource_points", 1' not in body and '"resource_points", 2' not in body
    assert 'p.530' in body
def test_collect_taxes_four_results_and_previous_turn():
    s=source()
    body=s.split("public JSONObject resolveCollectTaxes(int naturalRoll, String skillKey, String notes, String criticalFailureRuin)",1)[1].split("\n    public JSONObject ",1)[0]
    for label in ('CRITICAL_SUCCESS','SUCCESS','FAILURE','taxedLastTurn?2:1','"unrest",2','"ruin_choice"','"corruption"'):
        assert label in (body+s),label
    assert 'COUNT(*) AS total FROM kingmaker_turn_activities' in body
def test_economy_bonus_used_and_logged_in_real_kingdom_checks():
    s=source()
    a=s.split("public JSONObject recordCheck(String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)",1)[1].split("\n    public ",1)[0]
    assert '"economy".equals(ability)' in a
    assert 'optInt("collect_taxes_economy_bonus",0)' in a
    assert 'vacancy-ruin+taxBonus' in a
    assert '.put("collect_taxes_economy_bonus",taxBonus)' in a
