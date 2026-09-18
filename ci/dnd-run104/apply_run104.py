#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: apply_run104.py <project-root>")

root=Path(sys.argv[1])
main=root/"android/app/src/main/java/com/braseiro/dnd5e/MainActivity.java"
version=root/"version.properties"
if not main.is_file() or not version.is_file():
    raise SystemExit("RUN104 baseline files missing")

s=main.read_text(encoding="utf-8")

old_update='if(details!=null){java.util.Iterator<String> keys=details.keys();while(keys.hasNext()){String key=keys.next();copy.put(key,details.opt(key));}}'
new_update='if(details!=null){java.util.Iterator<String> keys=details.keys();while(keys.hasNext()){String key=keys.next();Object value=details.opt(key);if(value==null||value==JSONObject.NULL)copy.remove(key);else copy.put(key,value);}}'

old_cond='if(id.equals(turnId)&&currentScene.equals(turn.optString("scene_fingerprint",currentScene))&&"available".equals(turn.optString("promotion_status","available"))){allowed=true;selectedTurn=turn;break;}'
new_cond='String status=turn.optString("promotion_status","available");if(id.equals(turnId)&&currentScene.equals(turn.optString("scene_fingerprint",currentScene))&&("available".equals(status)||"cancelled".equals(status)||status.length()==0)){allowed=true;selectedTurn=turn;break;}'

old_details='String advisory=masterPromotionAdvisoryContext(selectedTurn);JSONObject promotionDetails=new JSONObject();try{promotionDetails.put("hint_authority","advisory_only");if(advisory.length()>0)promotionDetails.put("promotion_advisory",advisory);}catch(Exception ignored){AppLog.ignored("MasterConversation","Falha ao registrar contexto consultivo da promoção.",ignored);}'
new_details='String advisory=masterPromotionAdvisoryContext(selectedTurn);boolean retrying="cancelled".equals(selectedTurn.optString("promotion_status","available"));JSONObject promotionDetails=new JSONObject();try{promotionDetails.put("hint_authority","advisory_only");if(advisory.length()>0)promotionDetails.put("promotion_advisory",advisory);if(retrying){promotionDetails.put("retry_count",selectedTurn.optInt("retry_count",0)+1);String[] stale={"test_key","test_label","test_ability","test_skill","test_mode","test_dc","test_source_rule_id","roll_id","roll_actor","roll_formula","roll_mode","roll_natural","roll_total","roll_outcome","roll_source","resolved_after_roll","resolved_without_roll","resolution_excerpt","resolution_scene_fingerprint"};for(String key:stale)promotionDetails.put(key,JSONObject.NULL);}}catch(Exception ignored){AppLog.ignored("MasterConversation","Falha ao registrar contexto consultivo da promoção.",ignored);}'

for old,new,label in (
    (old_update,new_update,"remove-null-details"),
    (old_cond,new_cond,"cancelled-retry-gate"),
    (old_details,new_details,"retry-trace-reset"),
):
    count=s.count(old)
    if count != 1:
        raise SystemExit(f"RUN104 {label}: expected exactly 1 source match, found {count}")
    s=s.replace(old,new,1)

main.write_text(s,encoding="utf-8")

v=version.read_text(encoding="utf-8")
if "VERSION_NAME=6.6.2\n" not in v or "VERSION_CODE=606200\n" not in v:
    raise SystemExit("RUN104 version baseline mismatch")
v=v.replace("VERSION_NAME=6.6.2\n","VERSION_NAME=6.6.2-r104\n",1)
v=v.replace("VERSION_CODE=606200\n","VERSION_CODE=606204\n",1)
version.write_text(v,encoding="utf-8")

checks=[
    'TENTAR NOVAMENTE NA HISTÓRIA',
    '("available".equals(status)||"cancelled".equals(status)||status.length()==0)',
    'retry_count',
    'if(value==null||value==JSONObject.NULL)copy.remove(key);else copy.put(key,value)',
    'GM_HELP',
    'Trate isto agora como PLAYER_ACTION',
    'resolution_excerpt',
]
final=main.read_text(encoding="utf-8")
for check in checks:
    if check not in final:
        raise SystemExit("RUN104 contract missing: "+check)

print("RUN104_PATCH_APPLIED")
print("PACKAGE=com.braseiro.dnd5e")
print("VERSION_NAME=6.6.2-r104")
print("VERSION_CODE=606204")
