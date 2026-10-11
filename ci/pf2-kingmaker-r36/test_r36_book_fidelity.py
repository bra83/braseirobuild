"""Source contract guards for canonical Kingmaker PDF p. 522 / 530 / 531.
No claim of whole-book certification: these guards detect regression of verified fixes.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SOURCE=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java").read_text(encoding="utf-8")

def method_body(signature):
    pos=SOURCE.index(signature)
    boundary=SOURCE.find("\n    public ",pos+len(signature))
    return SOURCE[pos:boundary if boundary>=0 else pos+6000]

def test_collect_taxes_grants_economy_bonus_not_invented_rp():
    s=method_body('public JSONObject resolveCollectTaxes(int naturalRoll,String skillKey,String ruinChoice,String notes)')
    assert '"economy_circumstance_bonus"' in s
    assert '"resource_points", 2' not in s and '"resource_points", 1' not in s
    assert '"SUCCESS".equals(degree)?(repeated?1:0)' in s
    assert '"CRITICAL_FAILURE".equals(degree)?2:0' in s
    assert 'change.put(ruin,1)' in s
    assert 'Só é permitido Coletar Impostos uma vez' in s

def test_tax_circumstance_on_future_economy_checks_from_active_turn_log():
    s=method_body('public JSONObject recordCheck(String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)')
    assert 'taxesCircumstanceBonus(turn.optLong("id"))' in s
    assert '"economy".equals(ability)' in s
    assert '"collect_taxes".equals(activity)' in s
    assert '.put("taxes_circumstance",taxes)' in s

def test_quell_unrest_is_d6_with_explicit_ruin_alternative():
    s=method_body('public JSONObject resolveQuellUnrest(int naturalRoll,String skillKey,int successD6,int failureD4,')
    assert 'successD6<1||successD6>6' in s
    assert '"CRITICAL_SUCCESS".equals(d)?-successD6' in s
    assert '"CRITICAL_FAILURE".equals(d)&&"unrest".equals(choice)?failureD4:0' in s
    assert 'change.put(a,1).put(b,1)' in s
    assert 'limite de uma tentativa por turno' in s

def test_failed_claim_hex_consumes_one_rp_and_critical_stability_penalty():
    s=method_body('public JSONObject resolveClaimHex(String hexId,int naturalRoll,String skillKey,String notes)')
    assert 'state().optInt("resource_points")<1' in s
    assert 'new JSONObject().put("resource_points",-1)' in s
    assert 'stability_circumstance_penalty' in s
    assert 'Math' not in s[:140]  # No helper-generated invented bonus.
    assert '.put("unrest",1)' not in s

def test_claim_penalty_persists_to_next_kingdom_turn_and_expires():
    s=SOURCE
    assert "a.degree='CRITICAL_FAILURE' AND t.turn_number>=? AND t.turn_number<=?" in s
    assert '"turn_number-1"' not in s
    assert '"stability".equals(ability)?claimHexStabilityPenalty(s.optInt("turn_number")):0' in s

def test_version_and_non_intentional_scope():
    assert "VERSION_NAME=9.0.37" in (ROOT/"version.properties").read_text()

def test_collect_taxes_rejects_other_skills_before_check():
    s=method_body('public JSONObject resolveCollectTaxes(int naturalRoll,String skillKey,String ruinChoice,String notes)')
    guard='if(!"trade".equals(skill))throw new IllegalArgumentException'
    assert guard in s
    assert s.index(guard) < s.index('recordCheck("collect_taxes"')
