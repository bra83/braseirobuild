package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Event sourcing leve: registra fatos de mutação sem substituir o estado materializado. */
public final class WorldDomainEventLedger {
    private final AppDb db;
    public WorldDomainEventLedger(AppDb database){db=database;}

    public void recordEnvelopeChecked(String commitId,JSONObject envelope) throws Exception {/*BR_JSON_WRAP*/try{
        if(commitId==null||commitId.trim().length()<16)throw new IllegalArgumentException("Commit de cena ausente no ledger.");
        int seq=0;
        seq=recordArray(commitId,envelope,"state_updates","HERO_STATE_CHANGED","hero",seq);
        JSONObject combat=envelope.optJSONObject("combat");if(combat!=null&&!"none".equalsIgnoreCase(combat.optString("action","none")))record(commitId,seq++,"COMBAT_CHANGED","combat",combat.optString("encounter_id",""),combat);
        JSONObject scene=envelope.optJSONObject("scene_state");if(scene!=null&&scene.length()>0)record(commitId,seq++,"SCENE_STATE_CHANGED","scene",scene.optString("hex_id",scene.optString("location","")),scene);
        seq=recordArray(commitId,envelope,"npc_updates","NPC_MEMORY_CHANGED","npc",seq);
        seq=recordArray(commitId,envelope,"npc_cognition_updates","NPC_COGNITION_CHANGED","npc",seq);
        seq=recordArray(commitId,envelope,"knowledge_updates","KNOWLEDGE_CHANGED","knowledge",seq);
        seq=recordArray(commitId,envelope,"quest_updates","QUEST_CHANGED","quest",seq);
        seq=recordArray(commitId,envelope,"faction_updates","FACTION_CHANGED","faction",seq);
        seq=recordArray(commitId,envelope,"guild_updates","GUILD_CHANGED","guild",seq);
        seq=recordArray(commitId,envelope,"clock_updates","WORLD_CLOCK_CHANGED","clock",seq);
        seq=recordArray(commitId,envelope,"world_events","WORLD_EVENT_SCHEDULED","world_event",seq);
        seq=recordArray(commitId,envelope,"event_resolutions","WORLD_EVENT_RESOLVED","world_event",seq);
        seq=recordArray(commitId,envelope,"pending_resolutions","PENDING_ACTION_RESOLVED","pending_action",seq);
        seq=recordArray(commitId,envelope,"journal_updates","JOURNAL_CHANGED","journal",seq);
        seq=recordArray(commitId,envelope,"map_updates","MAP_CHANGED","map",seq);
        seq=recordArray(commitId,envelope,"tactical_updates","TACTICAL_CHANGED","tactical",seq);
        JSONObject tick=envelope.optJSONObject("_local_tick");if(tick!=null&&tick.length()>0){
            record(commitId,seq++,"WORLD_TIME_ADVANCED","world","",tick);
            JSONObject weather=findObject(tick,"weather_system");if(weather!=null&&(weather.optBoolean("weather_changed",false)||weather.optString("event","").length()>0||weather.optString("aftermath","").length()>0))record(commitId,seq++,"WEATHER_CHANGED","weather",weather.optString("climate_profile",weather.optString("event","")),weather);
            seq=recordWeatherDelays(commitId,tick,seq);
        }
        JSONObject summary=new JSONObject().put("proposal_hash",envelope.optJSONObject("_consequence_proposal")==null?"":envelope.optJSONObject("_consequence_proposal").optString("proposal_hash")).put("grounding_fingerprint",envelope.optJSONObject("_consequence_proposal")==null?"":envelope.optJSONObject("_consequence_proposal").optString("grounding_fingerprint")).put("mutation_events",seq);
        record(commitId,9999,"SCENE_COMMITTED","scene_commit",commitId,summary);
        // Facts are written only after the host has accepted and committed the scene.
        AdventureContinuityEngine continuity=new AdventureContinuityEngine(db);
        continuity.recordConfirmed(commitId,envelope);
        continuity.commitKnowledgeTransfers(envelope);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}


    private int recordWeatherDelays(String commitId,JSONObject root,int seq)throws Exception{
        if(root==null)return seq;for(String key:new String[]{"faction_actions","npc_actions","activity_updates","troupe_updates"}){JSONArray a=findArray(root,key);if(a==null)continue;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&x.optBoolean("weather_delayed",false)){String entity=x.optString("id",x.optString("name",key+":"+i));record(commitId,seq++,"SCHEDULE_DELAYED_BY_WEATHER",key,entity,x);}}}return seq;
    }
    private static JSONObject findObject(JSONObject root,String key){if(root==null)return null;JSONObject direct=root.optJSONObject(key);if(direct!=null)return direct;java.util.Iterator<String> it=root.keys();while(it.hasNext()){Object v=root.opt(it.next());if(v instanceof JSONObject){JSONObject found=findObject((JSONObject)v,key);if(found!=null)return found;}}return null;}
    private static JSONArray findArray(JSONObject root,String key){if(root==null)return null;JSONArray direct=root.optJSONArray(key);if(direct!=null)return direct;java.util.Iterator<String> it=root.keys();while(it.hasNext()){Object v=root.opt(it.next());if(v instanceof JSONObject){JSONArray found=findArray((JSONObject)v,key);if(found!=null)return found;}}return null;}
    private int recordArray(String commitId,JSONObject env,String key,String type,String entityType,int seq)throws Exception{JSONArray a=env.optJSONArray(key);if(a==null)return seq;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null){String entity=x.optString("id",x.optString("name",x.optString("character",x.optString("title",""))));record(commitId,seq++,type,entityType,entity,x);}}return seq;}
    private void record(String commitId,int seq,String type,String entityType,String entityId,JSONObject payload)throws Exception{db.appendDomainEventChecked(commitId+":"+String.format(java.util.Locale.US,"%04d",seq)+":"+type,commitId,type,entityType,entityId,payload,"consequence_engine");}
}
