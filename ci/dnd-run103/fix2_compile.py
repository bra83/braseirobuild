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

# Exact compile repair for ReleaseInvariantEngine fallback. Preserve fail-closed metadata when possible.
release_inv=orch.parent/'ReleaseInvariantEngine.java'
if release_inv.exists():
    ri=release_inv.read_text(encoding='utf-8')
    old='}catch(Exception e){return o.put("schema","release_invariants_v1").put("valid",false).put("problem_count",1).put("authority","invariant_audit_error");}}'
    new='}catch(Exception e){try{o.put("schema","release_invariants_v1").put("valid",false).put("problem_count",1).put("authority","invariant_audit_error");}catch(Exception ignored){}return o;}}'
    if old in ri:
        ri=ri.replace(old,new,1)
        release_inv.write_text(ri,encoding='utf-8')

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

# Statblock turn runtime: contain Android's checked JSONException without changing successful-path behavior.
turn=orch.parent/'StatblockTurnEngine.java'
if turn.exists():
    src=turn.read_text(encoding='utf-8')
    out=[]; turn_wrapped=0
    for line in src.splitlines(True):
        raw=line.rstrip('\r\n'); nl=line[len(raw):]
        s=raw.lstrip()
        if '.put(' in raw and s.startswith(('public ','private ','protected ')) and 'try{' not in raw and raw.rstrip().endswith('}'):
            brace=raw.find('{')
            if brace>=0:
                raw=raw[:brace+1]+'try{'+raw[brace+1:-1]+'}catch(Exception e){throw new IllegalStateException(e);}}'
                turn_wrapped+=1
        out.append(raw+nl)
    turn.write_text(''.join(out),encoding='utf-8')
else:
    turn_wrapped=0

# Authorized atomic content batch: single one-line method with checked Android JSON writes.
atomic=orch.parent/'AuthorizedContentAtomicBatchEngine.java'
if atomic.exists():
    src=atomic.read_text(encoding='utf-8')
    old='public static synchronized JSONObject install(JSONArray rows){'
    if old in src and old+'try{' not in src:
        pos=src.index(old)+len(old)
        end=src.rfind('}\n}')
        if end<0:
            end=src.rfind('}}')
        if end<0:
            raise SystemExit('AuthorizedContentAtomicBatchEngine install boundary not found')
        src=src[:pos]+'try{'+src[pos:end]+'}catch(Exception e){throw new IllegalStateException(e);}'+src[end:]
        atomic.write_text(src,encoding='utf-8')

# Release closure diagnostic helper: contain Android's checked JSONException.
closure=orch.parent/'ReleaseClosureDiagnosticEngine.java'
if closure.exists():
    src=closure.read_text(encoding='utf-8')
    old='private static JSONObject line(JSONObject x){if(x==null)x=new JSONObject();return new JSONObject().put("known",x.optInt("known",0)).put("executable",x.optInt("executable",0)).put("expected",x.opt("expected")).put("pending",x.opt("pending")).put("complete",x.optBoolean("complete",x.optBoolean("feature_effects_compiled",false)));}'
    new='private static JSONObject line(JSONObject x){try{if(x==null)x=new JSONObject();return new JSONObject().put("known",x.optInt("known",0)).put("executable",x.optInt("executable",0)).put("expected",x.opt("expected")).put("pending",x.opt("pending")).put("complete",x.optBoolean("complete",x.optBoolean("feature_effects_compiled",false)));}catch(Exception e){throw new IllegalStateException(e);}}'
    if old in src:
        closure.write_text(src.replace(old,new,1),encoding='utf-8')

# Faerun SRD monster batch: contain Android's checked JSONException without changing data or public API.
faerun=orch.parent/'Srd51FaerunCoreMonsterEngine.java'
if not faerun.exists():
    raise SystemExit('Srd51FaerunCoreMonsterEngine.java not found')
f=faerun.read_text(encoding='utf-8')
if 'import org.json.JSONException;' not in f:
    f=f.replace('import org.json.JSONObject;','import org.json.JSONObject;\nimport org.json.JSONException;',1)
if 'public static JSONObject install(){try{' not in f:
    f=f.replace('public static JSONObject install(){','public static JSONObject install(){try{',1)
boundary='    }\n\n    private static JSONObject monster'
if boundary in f:
    f=f.replace(boundary,'    }catch(JSONException e){throw new IllegalStateException(e);}}\n\n    private static JSONObject monster',1)
for a,b in [
('private static JSONObject monster(String id,int ac,int hp,String type,double cr,JSONObject movement,JSONObject abilities,int passive,JSONArray actions,JSONArray traits){','private static JSONObject monster(String id,int ac,int hp,String type,double cr,JSONObject movement,JSONObject abilities,int passive,JSONArray actions,JSONArray traits) throws JSONException{'),
('private static JSONObject multi(JSONObject s,String... sequence){','private static JSONObject multi(JSONObject s,String... sequence) throws JSONException{'),
('private static JSONObject movement(int walk,int fly,int climb,int swim){','private static JSONObject movement(int walk,int fly,int climb,int swim) throws JSONException{'),
('private static JSONObject abilities(int str,int dex,int con,int intel,int wis,int cha){','private static JSONObject abilities(int str,int dex,int con,int intel,int wis,int cha) throws JSONException{'),
('private static JSONObject trait(String name,String mechanic){','private static JSONObject trait(String name,String mechanic) throws JSONException{'),
('private static JSONObject attack(String name,int toHit,String damage,String type,int reach){','private static JSONObject attack(String name,int toHit,String damage,String type,int reach) throws JSONException{'),
('private static JSONObject attackSaveRider(String name,int toHit,String damage,String type,int reach,String saveAbility,int dc,String condition,String effect){','private static JSONObject attackSaveRider(String name,int toHit,String damage,String type,int reach,String saveAbility,int dc,String condition,String effect) throws JSONException{'),
('private static JSONObject save(String name,String ability,int dc,String damage,String onSave,String condition,String effect,String recharge){','private static JSONObject save(String name,String ability,int dc,String damage,String onSave,String condition,String effect,String recharge) throws JSONException{')
]:
    f=f.replace(a,b,1)
faerun.write_text(f,encoding='utf-8')


# Robust package-wide containment for remaining methods that contain JSON put() calls.
# This parser works by brace positions, so multiple methods on the same physical line are handled safely.
def _mask_java(src):
    out=list(src)
    i=0; n=len(src); state='code'
    while i<n:
        if state=='code':
            if src.startswith('//',i):
                out[i]=out[i+1]=' '; i+=2; state='line'; continue
            if src.startswith('/*',i):
                out[i]=out[i+1]=' '; i+=2; state='block'; continue
            if src[i]=='"':
                out[i]=' '; i+=1; state='string'; continue
            if src[i]=="'":
                out[i]=' '; i+=1; state='char'; continue
            i+=1
        elif state=='line':
            if src[i]=='\n': state='code'
            else: out[i]=' '
            i+=1
        elif state=='block':
            if src.startswith('*/',i):
                out[i]=out[i+1]=' '; i+=2; state='code'
            else:
                if src[i] not in '\r\n': out[i]=' '
                i+=1
        elif state in ('string','char'):
            q='"' if state=='string' else "'"
            if src[i]=='\\':
                out[i]=' '
                if i+1<n:
                    out[i+1]=' '; i+=2
                else: i+=1
            elif src[i]==q:
                out[i]=' '; i+=1; state='code'
            else:
                if src[i] not in '\r\n': out[i]=' '
                i+=1
    return ''.join(out)

def _matching_open(mask, close_pos, op='(', cl=')'):
    d=0
    for j in range(close_pos,-1,-1):
        ch=mask[j]
        if ch==cl: d+=1
        elif ch==op:
            d-=1
            if d==0: return j
    return -1

def _matching_close(mask, open_pos, op='{', cl='}'):
    d=0
    for j in range(open_pos,len(mask)):
        ch=mask[j]
        if ch==op: d+=1
        elif ch==cl:
            d-=1
            if d==0: return j
    return -1

def _method_openings(mask):
    controls={'if','for','while','switch','catch','synchronized','try','do','else','new'}
    found=[]
    for p,ch in enumerate(mask):
        if ch!='{': continue
        j=p-1
        while j>=0 and mask[j].isspace(): j-=1
        # optional throws clause before {
        if j>=0 and mask[j]!=')':
            k=j
            while k>=0 and mask[k] not in ';{}': k-=1
            seg=mask[k+1:j+1]
            idx=seg.rfind(')')
            if idx<0: continue
            j=k+1+idx
        if j<0 or mask[j]!=')': continue
        q=_matching_open(mask,j,'(',')')
        if q<0: continue
        k=q-1
        while k>=0 and mask[k].isspace(): k-=1
        e=k
        while k>=0 and (mask[k].isalnum() or mask[k] in "_$"): k-=1
        name=mask[k+1:e+1]
        if not name or name in controls: continue
        # declaration starts after previous statement/class delimiter
        d=k
        while d>=0 and mask[d] not in ';{}': d-=1
        head=mask[d+1:p]
        if '=' in head or '->' in head: continue
        if not any(x in head for x in ('public ','private ','protected ','static ','final ','synchronized ','native ','abstract ','default ','strictfp ')):
            # allow package-private typed methods, but reject control-flow fragments
            toks=head.strip().split()
            if len(toks)<2: continue
        found.append(p)
    return found

json_wrapped=0
for jp in [orch.parent/'TashaWorldPhenomenaEngine.java', orch.parent/'StatblockCombatFlowEngine.java', orch.parent/'TashaFeatCompiledRulesEngine.java']:
    if not jp.exists():
        continue
    src=jp.read_text(encoding='utf-8')
    mask=_mask_java(src)
    edits=[]
    for op in _method_openings(mask):
        cl=_matching_close(mask,op,'{','}')
        if cl<0: continue
        body_mask=mask[op+1:cl]
        body_src=src[op+1:cl]
        if '.put(' not in body_mask: continue
        if '/*BR_JSON_WRAP*/' in body_src: continue
        # Preserve existing RuntimeException behavior; only checked exceptions are converted.
        pre='/*BR_JSON_WRAP*/try{'
        post='}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}'
        edits.append((op+1,pre))
        edits.append((cl,post))
        json_wrapped+=1
    if edits:
        for pos,txt in sorted(edits,reverse=True):
            src=src[:pos]+txt+src[pos:]
        jp.write_text(src,encoding='utf-8')


print(f'FIX8_OK master_orchestrator_wrapped={wrapped} authorized_ingestion_patched=true helpers_contained=true catch_fallbacks={catch_fallbacks} faerun_json_checked=true json_wrapped={json_wrapped}')
