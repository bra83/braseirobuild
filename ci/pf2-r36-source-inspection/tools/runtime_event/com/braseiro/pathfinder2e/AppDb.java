package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Banco transacional em memória usado para executar o motor real de eventos na JVM. */
public class AppDb {
    public interface CheckedTransaction { void run() throws Exception; }

    public final Map<Long,JSONObject> events=new LinkedHashMap<Long,JSONObject>();
    public final List<JSONObject> pending=new ArrayList<JSONObject>();
    public final Set<String> receipts=new LinkedHashSet<String>();
    public final List<String> effects=new ArrayList<String>();
    public final List<String> audits=new ArrayList<String>();
    public JSONObject scene=new JSONObject();
    public boolean failQuestUpdate=false;

    public void putEvent(JSONObject event)throws Exception{events.put(event.optLong("id"),copy(event));}
    public JSONObject getWorldEvent(long id){try{JSONObject row=events.get(id);return row==null?new JSONObject():copy(row);}catch(Exception error){throw new IllegalStateException(error);}}
    public boolean hasEffectReceipt(String key){return receipts.contains(key);}

    public synchronized void runInTransactionChecked(CheckedTransaction work)throws Exception{
        Snapshot before=new Snapshot(this);
        try{work.run();}catch(Exception failure){before.restore(this);throw failure;}
    }

    public long addPendingTransactionUnique(String title,String summary,JSONObject payload,String effectKey)throws Exception{
        for(JSONObject row:pending)if(effectKey.equals(row.optString("effect_key"))&&"pending".equals(row.optString("status")))return row.optLong("id");
        long id=pending.size()+1L;pending.add(new JSONObject().put("id",id).put("title",title).put("summary",summary).put("payload_json",copy(payload)).put("effect_key",effectKey).put("status","pending"));return id;
    }
    public void markWorldEventPendingReview(long id,String error)throws Exception{JSONObject row=requiredEvent(id);row.put("status","awaiting_review").put("consequence_status","pending_review").put("last_error",error);}
    public void addAuditEvent(String category,String title,String summary,String before,String after,boolean reversible){audits.add(category+":"+title);}
    public void recordEffectReceipt(String key,String source){receipts.add(key);}
    public void markWorldEventConsequenceChecked(long id,boolean applied,String error)throws Exception{JSONObject row=requiredEvent(id);row.put("consequence_status",applied?"applied":"failed").put("status",applied?"resolved":"scheduled").put("application_receipt",applied?row.optString("transaction_key"):"");}

    public long addKnowledgeFactChecked(String subject,String content,String truth,String visibility,String knownBy,String reveal,String source){effects.add("knowledge:"+content);return effects.size();}
    public long applyQuestUpdateChecked(JSONObject value)throws Exception{if(failQuestUpdate)throw new IllegalStateException("falha intermediária simulada");effects.add("quest:"+(value==null?"":value.optString("title")));return effects.size();}
    public long applyFactionUpdateChecked(JSONObject value){effects.add("faction:"+(value==null?"":value.optString("name")));return effects.size();}
    public long applyWorldClockUpdateChecked(JSONObject value){effects.add("clock:"+(value==null?"":value.optString("name")));return effects.size();}
    public long addWorldEventChecked(JSONObject value){effects.add("event:"+(value==null?"":value.optString("title")));return effects.size();}
    public void upsertNpcMemoryChecked(JSONObject value){effects.add("npc:"+(value==null?"":value.optString("name")));}
    public void upsertNpcCognitiveStateChecked(JSONObject value){effects.add("cognition:"+(value==null?"":value.optString("name",value.optString("npc_name"))));}
    public long addJournalChecked(String title,String body,String kind){effects.add("journal:"+title);return effects.size();}
    public void applyMapDirectorUpdateChecked(JSONObject value){effects.add("map:"+(value==null?"":value.toString()));}
    public void applyTacticalDirectorUpdateChecked(JSONObject value){effects.add("tactical:"+(value==null?"":value.toString()));}
    public JSONObject getSceneState(){try{return copy(scene);}catch(Exception error){throw new IllegalStateException(error);}}
    public void saveSceneStateChecked(JSONObject value)throws Exception{scene=copy(value);effects.add("scene");}

    private JSONObject requiredEvent(long id){JSONObject row=events.get(id);if(row==null)throw new IllegalStateException("evento inexistente");return row;}
    private static JSONObject copy(JSONObject value)throws Exception{return new JSONObject((value==null?new JSONObject():value).toString());}
    private static List<JSONObject> copyList(List<JSONObject> source)throws Exception{List<JSONObject> out=new ArrayList<JSONObject>();for(JSONObject item:source)out.add(copy(item));return out;}
    private static Map<Long,JSONObject> copyMap(Map<Long,JSONObject> source)throws Exception{Map<Long,JSONObject> out=new LinkedHashMap<Long,JSONObject>();for(Map.Entry<Long,JSONObject> item:source.entrySet())out.put(item.getKey(),copy(item.getValue()));return out;}

    private static final class Snapshot {
        final Map<Long,JSONObject> events;
        final List<JSONObject> pending;
        final Set<String> receipts;
        final List<String> effects;
        final List<String> audits;
        final JSONObject scene;
        Snapshot(AppDb db)throws Exception{events=copyMap(db.events);pending=copyList(db.pending);receipts=new LinkedHashSet<String>(db.receipts);effects=new ArrayList<String>(db.effects);audits=new ArrayList<String>(db.audits);scene=copy(db.scene);}
        void restore(AppDb db)throws Exception{db.events.clear();db.events.putAll(copyMap(events));db.pending.clear();db.pending.addAll(copyList(pending));db.receipts.clear();db.receipts.addAll(receipts);db.effects.clear();db.effects.addAll(effects);db.audits.clear();db.audits.addAll(audits);db.scene=copy(scene);}
    }
}
