"""Synthetic prepayment transform regression; not an Android or SQLite E2E test."""
from pathlib import Path
import subprocess
import sys

HERE = Path(__file__).resolve().parent
PATCH = HERE / "patch_claim_hex_prepaid.py"
PATH = "android/app/src/main/java/com/braseiro/pathfinder2e/KingmakerRepository.java"
FIXTURE = """
public JSONObject claimHex(String hexId,String notes)throws Exception{
if(s.optInt("resource_points",0)<1)throw new IllegalStateException("Reivindicar o hex custa 1 RP e o Reino não possui esse recurso.");
v.put("resource_points",before.optInt("resource_points")-1);
recordRpSpent(1);if(firstClaim&&!foundingHex)
}
public JSONObject resolveClaimHex(String hexId,int naturalRoll,String skillKey,String notes)throws Exception{
        JSONObject check=recordCheck("claim_hex","Reivindicar Hex "+clean(hexId),"Guardião",
claimHex(hexId,notes); // The successful path already deducts exactly 1 RP.
adjustState(new JSONObject().put("resource_points",-1),"Reivindicar Hex, Kingmaker p. 522");
}
"""

def run_patch(tmp_path, version="9.0.37"):
    src = tmp_path / PATH
    src.parent.mkdir(parents=True, exist_ok=True)
    src.write_text(FIXTURE, encoding="utf-8")
    (tmp_path / "version.properties").write_text("VERSION_NAME="+version, encoding="utf-8")
    p = subprocess.run([sys.executable, str(PATCH), str(tmp_path)],
        text=True, capture_output=True)
    return p, src

def test_payment_and_ledger_before_check(tmp_path):
    p,src = run_patch(tmp_path)
    assert p.returncode == 0, p.stderr
    result=src.read_text(encoding="utf-8")
    part=result[result.index("public JSONObject resolveClaimHex("):]
    charge='adjustState(new JSONObject().put("resource_points",-1),"Claim Hex: custo antecipado de 1 RP");'
    ledger="recordRpSpent(1);"
    assert part.count(charge)==1
    assert part.count(ledger)==1
    assert part.index(charge)<part.index(ledger)<part.index('recordCheck("claim_hex"')

def test_success_and_failure_never_double_charge(tmp_path):
    p,src=run_patch(tmp_path)
    assert p.returncode==0, p.stderr
    code=src.read_text(encoding="utf-8")
    assert "if(!prepaid)recordRpSpent(1);if(firstClaim&&!foundingHex)" in code
    assert "claimHex(hexId,notes,true)" in code
    assert "before.optInt(\"resource_points\")-(prepaid?0:1)" in code
    assert 'adjustState(new JSONObject().put("resource_points",-1),"Reivindicar Hex, Kingmaker p. 522");' not in code

def test_reapplication_fails_without_mutating(tmp_path):
    p,src=run_patch(tmp_path)
    assert p.returncode==0,p.stderr
    before=src.read_text(encoding="utf-8")
    repeat=subprocess.run([sys.executable,str(PATCH),str(tmp_path)],capture_output=True,text=True)
    assert repeat.returncode!=0
    assert src.read_text(encoding="utf-8")==before

def test_wrong_version_rejected(tmp_path):
    p,src=run_patch(tmp_path,version="9.0.36")
    assert p.returncode!=0
    assert src.read_text(encoding="utf-8")==FIXTURE
