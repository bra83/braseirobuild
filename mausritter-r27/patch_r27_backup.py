#!/usr/bin/env python3
"""R27 stage4: mirror Atlas localStorage to approved app storage and portable backup."""
from pathlib import Path
import sys
root=Path(sys.argv[1])
java=root/'android/app/src/main/java/com/braseiro/dnd'
atlas=root/'android/app/src/main/assets/mausritter_atlas/runtime/atlas_vivo.js'
db=java/'AppDb.java'; main=java/'MainActivity.java'
s=db.read_text(encoding='utf-8')
needle='''        root.put("derivedIndexesExcluded",true);'''
if s.count(needle)!=1: raise AssertionError('export anchor changed')
s=s.replace(needle,'''        if("mausritter".equals(profile)){
            JSONObject atlasSnapshots=new JSONObject();
            android.content.SharedPreferences store=appContext.getSharedPreferences("settings",Context.MODE_PRIVATE);
            final String prefix="mausritter_atlas_snapshot_";
            for(java.util.Map.Entry<String,?> entry:store.getAll().entrySet()){
                if(!entry.getKey().startsWith(prefix)||!(entry.getValue() instanceof String))continue;
                String adventure=entry.getKey().substring(prefix.length());
                if(!adventure.matches("[A-Za-z0-9_-]{1,100}"))continue;
                try{atlasSnapshots.put(adventure,new JSONObject((String)entry.getValue()));}
                catch(Exception ignored){AppLog.w("AtlasBackup","Snapshot antigo ignorado: "+adventure,null);}
            }
            root.put("mausritterAtlasRuntime",atlasSnapshots);
        }
'''+needle)
needle='''        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            JSONObject cp=root.optJSONObject("campaign");'''
if s.count(needle)!=1: raise AssertionError('import anchor changed')
s=s.replace(needle,'''        final Map<Long,Long> atlasCharacterIdMap=new HashMap<Long,Long>();
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            JSONObject cp=root.optJSONObject("campaign");''')
needle='''            Map<Long,Long> characterIdMap=new HashMap<Long,Long>();
            JSONArray chars=root.optJSONArray("characters");'''
if s.count(needle)!=1: raise AssertionError('character map anchor changed')
s=s.replace(needle,'''            Map<Long,Long> characterIdMap=atlasCharacterIdMap;
            JSONArray chars=root.optJSONArray("characters");''')
needle='''            if(root.has("journal")){db.delete("journal","world_profile=?",new String[]{backupProfile});JSONArray j=root.optJSONArray("journal");if(j!=null)for(int i=0;i<j.length();i++){JSONObject x=j.getJSONObject(i);addJournal(x.optString("title"),x.optString("body"),x.optString("kind"));}}'''
if s.count(needle)!=1: raise AssertionError('journal restore anchor changed')
s=s.replace(needle,'''            if(root.has("journal")){db.delete("journal","world_profile=?",new String[]{backupProfile});JSONArray j=root.optJSONArray("journal");if(j!=null)for(int i=0;i<j.length();i++){JSONObject x=j.getJSONObject(i);String key=x.optString("event_key","");if(key.trim().isEmpty())addJournal(x.optString("title"),x.optString("body"),x.optString("kind"));else if(addJournalFact(x.optString("title"),x.optString("body"),x.optString("kind"),key,x.optString("source_kind","system"))<0)throw new IllegalStateException("Duplicação de recibo no backup: "+key);}}''')
# insertion after SQL importAll transaction, not inside first block/other methods
start=s.index('    public synchronized void importAll(JSONObject root) throws Exception {')
end=s.index('    private void restoreSixZeroRows(',start)
block=s[start:end]
needle='''        }finally{db.endTransaction();}
    }
'''
if block.count(needle)!=1:raise AssertionError('importAll end changed')
block=block.replace(needle,'''        }finally{db.endTransaction();}
        if("mausritter".equals(backupProfile)&&root.optJSONObject("mausritterAtlasRuntime")!=null)
            restoreMausritterAtlasSnapshots(root.optJSONObject("mausritterAtlasRuntime"),atlasCharacterIdMap);
    }

    private static String remapAtlasMouseId(String old,Map<Long,Long> ids){
        try{long value=Long.parseLong(old);Long mapped=ids.get(value);
            return mapped==null?old:String.valueOf(mapped);
        }catch(Exception ignored){return old==null?"":old;}
    }
    private void restoreMausritterAtlasSnapshots(JSONObject backups,Map<Long,Long> idMap)throws Exception{
        android.content.SharedPreferences store=appContext.getSharedPreferences("settings",Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor=store.edit();
        String campaignRunId=store.getString("mausritter_campaign_run_id","");
        java.util.Iterator<String> adventures=backups.keys();
        while(adventures.hasNext()){
            String adventureId=adventures.next();
            if(!adventureId.matches("[A-Za-z0-9_-]{1,100}"))continue;
            JSONObject record=backups.optJSONObject(adventureId);
            if(record==null)continue;
            JSONObject state=record.optJSONObject("runtime");
            if(state==null)continue;
            for(String key:new String[]{"positions","trails"}){
                JSONObject current=state.optJSONObject(key);
                if(current==null)continue;
                JSONObject mapped=new JSONObject();
                java.util.Iterator<String> it=current.keys();
                while(it.hasNext()){String id=it.next();
                    mapped.put(remapAtlasMouseId(id,idMap),current.opt(id));}
                state.put(key,mapped);
            }
            state.put("selectedMouseId",remapAtlasMouseId(state.optString("selectedMouseId",""),idMap));
            JSONObject light=state.optJSONObject("activeLight");
            if(light!=null)light.put("mouseId",remapAtlasMouseId(light.optString("mouseId",""),idMap));
            state.put("_updatedAt",System.currentTimeMillis()+1000L);
            record.put("runtime",state).put("campaignRunId",campaignRunId);
            editor.putString("mausritter_atlas_snapshot_"+adventureId,record.toString());
        }
        if(!editor.commit())throw new IllegalStateException("O banco foi restaurado, mas os snapshots do Atlas não foram gravados.");
    }

''')
s=s[:start]+block+s[end:];db.write_text(s,encoding='utf-8')
mainText=main.read_text(encoding='utf-8')
anchor='''        @JavascriptInterface public String getNativeState()'''
if mainText.count(anchor)!=1:raise AssertionError('Atlas bridge native anchor changed')
code=r'''
        @JavascriptInterface public String readAtlasRuntimeSnapshot(String adventureId){
            if(adventureId==null||!adventureId.matches("[A-Za-z0-9_-]{1,100}"))return "";
            try{
                String raw=prefs.getString("mausritter_atlas_snapshot_"+adventureId,"");
                if(raw.length()==0)return "";
                JSONObject envelope=new JSONObject(raw);
                if(!ensureMausritterCampaignRunId().equals(envelope.optString("campaignRunId","")))return "";
                JSONObject payload=envelope.optJSONObject("runtime");
                return payload==null?"":payload.toString();
            }catch(Exception error){return "";}
        }
        @JavascriptInterface public String saveAtlasRuntimeSnapshot(String raw){
            try{
                if(raw==null||raw.length()>1500000)return "invalid-size";
                JSONObject envelope=new JSONObject(raw);
                String adventureId=envelope.optString("adventureId","");
                if(!adventureId.matches("[A-Za-z0-9_-]{1,100}"))return "invalid-adventure";
                if(!ensureMausritterCampaignRunId().equals(envelope.optString("campaignRunId","")))return "stale-campaign";
                JSONObject runtime=envelope.optJSONObject("runtime");
                if(runtime==null)return "invalid-runtime";
                String record=new JSONObject().put("campaignRunId",envelope.optString("campaignRunId"))
                    .put("runtime",runtime).toString();
                if(!prefs.edit().putString("mausritter_atlas_snapshot_"+adventureId,record).commit())
                    return "disk-error";
                return "ok";
            }catch(Exception error){AppLog.w("AtlasBackup","Falha ao espelhar estado de exploração",error);return "error";}
        }

'''
mainText=mainText.replace(anchor,code+anchor)
# Lift action composition above Samsung/Google keyboard on Android 11+, avoid fixed keyboard size.
needle='''                left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;'''
if mainText.count(needle)!=1:raise AssertionError('IME anchor changed')
mainText=mainText.replace(needle,'''                left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;
                android.graphics.Insets keyboard=insets.getInsets(WindowInsets.Type.ime());
                bottom=Math.max(bottom,keyboard.bottom);''')
main.write_text(mainText,encoding='utf-8')
t=atlas.read_text(encoding='utf-8')
old='''  function runtime() {
    try { return { ...defaultRuntime(), ...JSON.parse(localStorage.getItem(storageKey()) || '{}') }; }
    catch (_) { return defaultRuntime(); }
  }
  function saveRuntime(value) { localStorage.setItem(storageKey(), JSON.stringify(value)); }'''
new='''  function runtime() {
    let local={};
    try{local=JSON.parse(localStorage.getItem(storageKey())||'{}');}catch(_){}
    try{
      const raw=bridge('readAtlasRuntimeSnapshot',state.adventure.id);
      if(typeof raw==='string'&&raw.startsWith('{')){
        const mirror=JSON.parse(raw);
        if(Number(mirror._updatedAt||0)>Number(local._updatedAt||0)){
          localStorage.setItem(storageKey(),JSON.stringify(mirror));
          local=mirror;
        }
      }
    }catch(error){console.warn('Native Atlas recovery not available',error);}
    return {...defaultRuntime(),...local};
  }
  function saveRuntime(value) {
    value._updatedAt=Math.max(Date.now(),Number(value._updatedAt||0)+1);
    localStorage.setItem(storageKey(),JSON.stringify(value));
    const nativeRun=String(state.native?.campaignRunId||'');
    if(nativeRun){
      const status=bridge('saveAtlasRuntimeSnapshot',JSON.stringify({
        adventureId:state.adventure.id,campaignRunId:nativeRun,runtime:value}));
      if(status!==null&&status!=='ok')console.warn('Atlas mirror not committed:',status);
    }
  }'''
if t.count(old)!=1:raise AssertionError('Runtime original implementation changed')
t=t.replace(old,new)
# No visual grid by default, and never reset a user's explicit toggle on resume.
t=t.replace('season: state.adventure.defaultSeason, numbers: false, grid: true, weather:', 'season: state.adventure.defaultSeason, numbers: false, grid: false, weather:')
t=t.replace('seedData.numbers=false;seedData.grid=true;saveRuntime(seedData);','seedData.numbers=false;saveRuntime(seedData);')
t=t.replace('data.numbers=false;data.grid=true;saveRuntime(data);','data.numbers=false;saveRuntime(data);')
atlas.write_text(t,encoding='utf-8')
print('D09/D12: IME insets; bidirectional Atlas native mirror, backup export/restore with ID remap; no forced grid')
