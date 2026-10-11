#!/usr/bin/env python3
"""Kingmaker R36 follow-up: charge Claim Hex before the skill check, exactly once."""
from pathlib import Path
import sys

root=Path(sys.argv[1]).resolve()
v=root/"version.properties"
p=root/"android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java"
if not v.is_file() or "VERSION_NAME=9.0.37" not in v.read_text(encoding="utf-8"):
    raise RuntimeError("Expected verified, already-patched R36 project")
s=p.read_text(encoding="utf-8")
def one(old,new,label):
    global s
    if s.count(old)!=1: raise RuntimeError(f"{label}: source drift ({s.count(old)})")
    s=s.replace(old,new,1)

one('public JSONObject claimHex(String hexId,String notes)throws Exception{',
    'public JSONObject claimHex(String hexId,String notes)throws Exception{\n        return claimHex(hexId,notes,false);\n    }\n    private JSONObject claimHex(String hexId,String notes,boolean prepaid)throws Exception{',
    "prepaid overload")
one('if(s.optInt("resource_points",0)<1)throw new IllegalStateException("Reivindicar o hex custa 1 RP e o Reino não possui esse recurso.");',
    'if(!prepaid&&s.optInt("resource_points",0)<1)throw new IllegalStateException("Reivindicar o hex custa 1 RP e o Reino não possui esse recurso.");',
    "prepaid guard")
one('v.put("resource_points",before.optInt("resource_points")-1);',
    'v.put("resource_points",before.optInt("resource_points")-(prepaid?0:1));',
    "success debit")
one('recordRpSpent(1);if(firstClaim&&!foundingHex)',
    'if(!prepaid)recordRpSpent(1);if(firstClaim&&!foundingHex)',
    "success accounting")
one('        JSONObject check=recordCheck("claim_hex","Reivindicar Hex "+clean(hexId),"Guardião",',
    '        if(naturalRoll<1||naturalRoll>20)throw new IllegalArgumentException("d20 deve ser de 1 a 20.");\n'
    '        if(skill(skill).length()==0)throw new IllegalArgumentException("Perícia não configurada.");\n'
    '        JSONObject beforePay=state();\n'
    '        if(rules.isAnarchy(beforePay.optInt("unrest"))||beforePay.optInt("anarchy")==1)\n'
    '            throw new IllegalStateException("Não é permitido reivindicar hex em Anarquia.");\n'
    '        adjustState(new JSONObject().put("resource_points",-1),"Claim Hex: custo antecipado de 1 RP");\n'
    '        JSONObject check=recordCheck("claim_hex","Reivindicar Hex "+clean(hexId),"Guardião",',
    "prepay before roll")
one('claimHex(hexId,notes); // The successful path already deducts exactly 1 RP.',
    'claimHex(hexId,notes,true); // RP paid before check.',
    "success uses prepaid path")
one('adjustState(new JSONObject().put("resource_points",-1),"Reivindicar Hex, Kingmaker p. 522");',
    '// RP already paid before the check.',
    "failure must not double charge")
p.write_text(s,encoding="utf-8")
print("R36_CLAIM_HEX_PREPAID_APPLIED")
