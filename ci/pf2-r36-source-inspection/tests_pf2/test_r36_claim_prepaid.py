"""Source contracts for the official Claim Hex RP prepayment fix.

These checks inspect the patched production Java; they are NOT a substitute
for an Android/SQLite end-to-end test.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java").read_text(encoding="utf-8")

def section(start,end):
    a=JAVA.index(start)
    b=JAVA.index(end,a+len(start))
    return JAVA[a:b]

def test_claim_hex_pays_before_check_and_not_after():
    m=section("public JSONObject resolveClaimHex(", "\n    public ")
    payment='adjustState(new JSONObject().put("resource_points",-1),"Claim Hex: custo antecipado de 1 RP");'
    assert m.count(payment)==1
    assert m.index(payment)<m.index('recordCheck("claim_hex"')
    assert 'claimHex(hexId,notes,true)' in m
    assert 'adjustState(new JSONObject().put("resource_points",-1),"Reivindicar Hex' not in m

def test_public_claim_hex_keeps_non_prepaid_behavior():
    m=section("public JSONObject claimHex(String hexId,String notes)", "\n    public ")
    assert 'return claimHex(hexId,notes,false)' in m
    assert 'if(!prepaid&&s.optInt("resource_points",0)<1)' in m
    assert 'before.optInt("resource_points")-(prepaid?0:1)' in m
    assert 'if(!prepaid)recordRpSpent(1)' in m

def test_rejected_claims_are_checked_before_payment():
    m=section("public JSONObject resolveClaimHex(", "\n    public ")
    debit=m.index("custo antecipado de 1 RP")
    for guard in ('reconnoitered','cleared','adjacentToClaimed','resource_points','maxClaims',
                  'naturalRoll<1||naturalRoll>20','skill(skill).length()==0','isAnarchy'):
        assert guard in m[:debit], guard

def test_claim_critical_failure_penalty_survives_and_no_unrest_added():
    m=section("public JSONObject resolveClaimHex(", "\n    public ")
    assert '"stability_circumstance_penalty"' in m
    assert '"stability_penalty_through_turn"' in m
    assert '.put("unrest",1)' not in m
    assert '"CRITICAL_FAILURE"' in m

def test_prepaid_claim_hex_persists_rp_expense_once_before_roll():
    m=section("public JSONObject resolveClaimHex(", "\n    public ")
    debit='adjustState(new JSONObject().put("resource_points",-1),"Claim Hex: custo antecipado de 1 RP");'
    spent="recordRpSpent(1); // Persist expense exactly once before the skill check."
    assert m.count(debit)==1
    assert m.count(spent)==1
    assert m.index(debit)<m.index(spent)<m.index('recordCheck("claim_hex"')
    assert 'claimHex(hexId,notes,true)' in m
