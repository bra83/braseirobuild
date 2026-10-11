"""Production-source contracts: R36 UI, RP/XP and Kingdom accounting.
Not Android-device, SQLite transaction, or whole-campaign certification.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java").read_text(encoding="utf-8")
UI=(ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")

def test_claim_hex_ui_calls_rule_resolver_instead_of_direct_acquisition():
    assert 'showKingmakerClaimHexDialog(h,value(notes));return;' in UI
    assert 'kingmakerRepository.resolveClaimHex(' in UI
    assert 'else if(a==2)kingmakerRepository.claimHex(' not in UI

def test_expansion_expert_and_tax_bonuses_do_not_stack():
    assert 'expansionExpert="claim_hex".equals(activity)&&s.optInt("level",1)>=4?2:0' in JAVA
    assert 'Math.max(taxes,expansionExpert)-claimPenalty' in JAVA

def test_kingdom_rp_xp_and_reputation_rules_are_wired():
    assert 'int surplus=Math.max(0,before.optInt("resource_points"))' in JAVA
    assert 'int surplus=Math.min(120,Math.max(0,before.optInt("resource_points")))' not in JAVA
    assert 'before.optInt("fame",0)+1' in JAVA
    assert 'before.optInt("infamy",0)+1' in JAVA
    assert 'if("CRITICAL_SUCCESS".equals(result.optString("degree")))gainReputationPoint();' in JAVA

def test_trade_only_tax_rejected_before_dice_persistence():
    marker='public JSONObject resolveCollectTaxes(int naturalRoll,String skillKey,String ruinChoice,String notes)'
    s=JAVA[JAVA.index(marker):]
    assert 'if(!"trade".equals(skill))throw new IllegalArgumentException' in s
    assert s.index('if(!"trade".equals(skill))') < s.index('recordCheck("collect_taxes"')
