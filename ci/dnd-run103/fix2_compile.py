from pathlib import Path
import sys,re
main=Path(sys.argv[1]); srd=Path(sys.argv[2]); orch=Path(sys.argv[3])

# MainActivity: remove the single extra class-closing brace and import Normalizer.
s=main.read_text(encoding='utf-8')
needle='String storyTrace=turn.optString("resolution_excerpt","");if(storyTrace.length()>0)parent.addView(text("NA HISTÓRIA: "+storyTrace,compact?10:11,Color.WHITE,false));}}\n    }'
repl='String storyTrace=turn.optString("resolution_excerpt","");if(storyTrace.length()>0)parent.addView(text("NA HISTÓRIA: "+storyTrace,compact?10:11,Color.WHITE,false));}\n    }'
if s.count(needle)==1:
    s=s.replace(needle,repl,1)
if 'import java.text.Normalizer;' not in s:
    if 'import java.util.Locale;' in s:
        s=s.replace('import java.util.Locale;','import java.util.Locale;\nimport java.text.Normalizer;',1)
    else:
        pos=s.find('\n',s.find('package '))+1
        s=s[:pos]+'import java.text.Normalizer;\n'+s[pos:]
main.write_text(s,encoding='utf-8')

# SRD bestiary: Android org.json put() is checked; keep public API stable and contain it here.
t=srd.read_text(encoding='utf-8')
if 'import org.json.JSONException;' not in t:
    if 'import org.json.JSONArray;import org.json.JSONObject;' in t:
        t=t.replace('import org.json.JSONArray;import org.json.JSONObject;','import org.json.JSONArray;import org.json.JSONObject;import org.json.JSONException;',1)
    else:
        pos=t.find('\n',t.find('package '))+1
        t=t[:pos]+'import org.json.JSONException;\n'+t[pos:]
if 'public static JSONObject install(){try{' not in t:
    t=t.replace('public static JSONObject install(){','public static JSONObject install(){try{',1)
old='return new JSONObject().put("scope_declared",scoped).put("statblocks_installed",installed).put("coverage",AuthorizedBestiaryScopeEngine.coverage()).put("license","CC-BY-4.0").put("source","official D&D SRD 5.1");}'
new='return new JSONObject().put("scope_declared",scoped).put("statblocks_installed",installed).put("coverage",AuthorizedBestiaryScopeEngine.coverage()).put("license","CC-BY-4.0").put("source","official D&D SRD 5.1");}catch(JSONException e){throw new IllegalStateException(e);}}'
if old in t:
    t=t.replace(old,new,1)
for a,b in [
('private static JSONObject abilities(int str,int dex,int con,int intel,int wis,int cha){','private static JSONObject abilities(int str,int dex,int con,int intel,int wis,int cha) throws JSONException{'),
('private static JSONObject trait(String name,String mechanic){','private static JSONObject trait(String name,String mechanic) throws JSONException{'),
('private static void enrich(JSONObject s,JSONObject movement,JSONObject abilities,JSONObject senses,JSONArray traits){','private static void enrich(JSONObject s,JSONObject movement,JSONObject abilities,JSONObject senses,JSONArray traits) throws JSONException{'),
('private static JSONObject stat(String id,int ac,int hp,String type,double cr,JSONArray actions,String loc){','private static JSONObject stat(String id,int ac,int hp,String type,double cr,JSONArray actions,String loc) throws JSONException{'),
('private static JSONObject action(String name,int toHit,String damage,String damageType,int reach){','private static JSONObject action(String name,int toHit,String damage,String damageType,int reach) throws JSONException{')]:
    t=t.replace(a,b,1)
srd.write_text(t,encoding='utf-8')

# MasterOrchestrator: preserve signatures/behavior. Only one-line JSONObject-building adapters
# are wrapped so Android's checked JSONException cannot leak through their existing API.
o=orch.read_text(encoding='utf-8')
if 'import org.json.JSONException;' not in o:
    if 'import org.json.JSONObject;' in o:
        o=o.replace('import org.json.JSONObject;','import org.json.JSONObject;\nimport org.json.JSONException;',1)
    else:
        pos=o.find('\n',o.find('package '))+1
        o=o[:pos]+'import org.json.JSONException;\n'+o[pos:]
out=[]; wrapped=0
for line in o.splitlines(True):
    raw=line.rstrip('\r\n')
    nl=line[len(raw):]
    stripped=raw.strip()
    if (stripped.startswith('public JSONObject ') and
        '{return ' in stripped and stripped.endswith(';}') and
        'new JSONObject().put(' in stripped and
        'catch(JSONException' not in stripped):
        p=raw.index('{return ')
        prefix=raw[:p]
        body=raw[p+1:-1]
        raw=prefix+'{try{'+body+'}catch(JSONException e){throw new IllegalStateException(e);}}'
        wrapped+=1
    out.append(raw+nl)
orch.write_text(''.join(out),encoding='utf-8')
if wrapped < 1:
    raise SystemExit('MasterOrchestrator: no JSONObject adapter methods wrapped')
print(f'FIX4_OK master_orchestrator_wrapped={wrapped} json_exception_imported=true')
