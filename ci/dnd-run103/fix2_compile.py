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

# Authorized content ingestion: retain existing error semantics while containing Android's checked JSONException.
ingest=orch.parent/'AuthorizedContentIngestionBatchEngine.java'
if not ingest.exists():
    raise SystemExit('AuthorizedContentIngestionBatchEngine.java not found')
a=ingest.read_text(encoding='utf-8')
if 'import org.json.JSONException;' not in a:
    a=a.replace('import org.json.JSONArray;import org.json.JSONObject;','import org.json.JSONArray;import org.json.JSONObject;import org.json.JSONException;',1)
old_catch='}catch(Exception e){return o.put("requested",n).put("installed",accepted.length()).put("blocked",blocked.length()+1).put("authority","blocked_batch_error");}}'
new_catch='}catch(Exception e){try{return o.put("requested",n).put("installed",accepted.length()).put("blocked",blocked.length()+1).put("authority","blocked_batch_error");}catch(JSONException ignored){return o;}}}'
if old_catch in a:
    a=a.replace(old_catch,new_catch,1)
old_wrap='private static JSONObject wrap(String k,String n,boolean ok,JSONObject detail){return new JSONObject().put("kind",k).put("name",n==null?"":n).put("installed",ok).put("detail",detail==null?new JSONObject():detail);}'
new_wrap='private static JSONObject wrap(String k,String n,boolean ok,JSONObject detail){try{return new JSONObject().put("kind",k).put("name",n==null?"":n).put("installed",ok).put("detail",detail==null?new JSONObject():detail);}catch(JSONException e){return new JSONObject();}}'
if old_wrap in a:
    a=a.replace(old_wrap,new_wrap,1)
old_block='private static JSONObject block(String k,String n,String why){return new JSONObject().put("kind",k).put("name",n==null?"":n).put("installed",false).put("authority",why);}'
new_block='private static JSONObject block(String k,String n,String why){try{return new JSONObject().put("kind",k).put("name",n==null?"":n).put("installed",false).put("authority",why);}catch(JSONException e){return new JSONObject();}}'
if old_block in a:
    a=a.replace(old_block,new_block,1)
ingest.write_text(a,encoding='utf-8')

# Package-wide fail-closed repair for one-line catch fallbacks which return a chained JSONObject.put().
# Android's org.json.JSONException is checked, so a put() inside the catch body itself must also be contained.
catch_fallbacks=0
for jp in orch.parent.glob('*.java'):
    src=jp.read_text(encoding='utf-8')
    out=[]; changed=False
    for line in src.splitlines(True):
        raw=line.rstrip('\r\n'); nl=line[len(raw):]
        ci=raw.find('catch(')
        if ci>=0:
            marker=raw.find('){return ',ci)
            if marker>=0 and '.put(' in raw[marker:] and raw.endswith(';}}') and '){try{return ' not in raw[ci:]:
                expr=raw[marker+9:-3]
                var=expr.split('.put(',1)[0].strip()
                if var.replace('_','').isalnum() and var[0].isalpha():
                    raw=raw[:marker+2]+'try{return '+expr+';}catch(Exception ignoredJson){return '+var+';}}}'
                    catch_fallbacks+=1; changed=True
        out.append(raw+nl)
    if changed:
        jp.write_text(''.join(out),encoding='utf-8')

print(f'FIX7_OK master_orchestrator_wrapped={wrapped} authorized_ingestion_patched=true helpers_contained=true catch_fallbacks={catch_fallbacks}')
