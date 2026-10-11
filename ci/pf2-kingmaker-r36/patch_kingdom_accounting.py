#!/usr/bin/env python3
"""R36 official Kingdom accounting: Fame/Infamy, surplus XP, Expansion Expert.
Apply only after patch.py and patch_claim_hex_prepaid.py to verified R35/R36 source.
Kingmaker AP 2e Kingdom advancement / Fame and Infamy, PF2e bonus stacking.
Fail closed before any source writes. Never touches maps, TTS or other VTTs.
"""
from pathlib import Path
import sys
REL=Path("android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java")

def once(s,old,new,label):
    n=s.count(old)
    if n!=1: raise RuntimeError(f"{label}: expected unique source anchor, got {n}")
    return s.replace(old,new,1)

def transform(s):
    s=once(s,
        'int surplus=Math.min(120,Math.max(0,before.optInt("resource_points")))',
        'int surplus=Math.max(0,before.optInt("resource_points"))',
        "RP into kingdom XP without artificial 120 cap")
    s=once(s,
        'state.put("fame",before.optInt("fame",0));state.put("infamy",before.optInt("infamy",0));',
        'String reputation=before.optString("reputation_mode","fame");'
        'int reputationCap=before.optInt("level",1)>=20?4:3;'
        'state.put("fame","infamy".equals(reputation)?0:Math.min(reputationCap,before.optInt("fame",0)+1));'
        'state.put("infamy","infamy".equals(reputation)?Math.min(reputationCap,before.optInt("infamy",0)+1):0);',
        "Fame or Infamy at start of turn")
    s=once(s,
        'if(awardReputationOnCrit&&"CRITICAL_SUCCESS".equals(result.optString("degree")))gainReputationPoint();',
        'if("CRITICAL_SUCCESS".equals(result.optString("degree")))gainReputationPoint();',
        "Critical kingdom checks earn reputation")
    # patch.py is authoritative for Expansion Expert +2 and circumstance stacking.
    # Never re-apply or overwrite this official modifier correction.
    if 'int expansionExpert="claim_hex".equals(activity)&&s.optInt("level",1)>=4?2:0;' not in s:
        raise RuntimeError("Expansion Expert was not applied by core R36 patch")
    if 'int circumstanceBonus=Math.max(taxes,expansionExpert);' not in s:
        raise RuntimeError("Kingdom circumstance bonuses must not stack")
    return s

def apply(root):
    root=Path(root).resolve()
    version=root/"version.properties";path=root/REL
    if not version.is_file() or "VERSION_NAME=9.0.37" not in version.read_text(encoding="utf-8"):
        raise RuntimeError("Refuse source without verified R36 version")
    before=path.read_text(encoding="utf-8")
    after=transform(before)
    path.write_text(after,encoding="utf-8")
    return path

if __name__=="__main__":
    if len(sys.argv)!=2:raise SystemExit("usage: patch_kingdom_accounting.py <R36_project>")
    print("R36_KINGDOM_ACCOUNTING_APPLIED",apply(sys.argv[1]))
