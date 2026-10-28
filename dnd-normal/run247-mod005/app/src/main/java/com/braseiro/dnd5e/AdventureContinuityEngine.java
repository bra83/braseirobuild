package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/**
 * Bridge between committed game facts and knowledge in later adventures.
 * Witnesses are explicit persisted references, never invented from narration.
 */
public final class AdventureContinuityEngine {
    private final AppDb db;
    public AdventureContinuityEngine(AppDb database){db=database;}
    private void ensureSchema(){
        db.getWritableDatabase().execSQL("CREATE TABLE IF NOT EXISTS braseiro_adventure_causal_facts ("+
                "world_scope TEXT NOT NULL, commit_key TEXT NOT NULL, actor_character_id INTEGER NOT NULL, "+
                "adventure_id TEXT NOT NULL, event_json TEXT NOT NULL, created_at INTEGER NOT NULL, "+
                "PRIMARY KEY(world_scope,commit_key))");
    }
    private String worldScope(String world,String timeline){
        return db.getActiveMapProfile()+":"+world+":"+timeline;
    }
    private JSONObject activePackage(){
        try{return db.queryOne("SELECT active_json,world_id,timeline_id FROM braseiro_adventure_packages "+
                "WHERE scope=? LIMIT 1",new String[]{db.getActiveMapProfile()+":"+db.getCampaign().optString("name","D&D Normal")});}
        catch(Exception missingImportTable){return new JSONObject();}
    }
    /** Must be invoked exclusively from an already-authorized scene commit transaction. */
    public int recordConfirmed(String commitId,JSONObject envelope)throws Exception{
        if(commitId==null||commitId.isEmpty()||envelope==null)return 0;
        ensureSchema();
        JSONObject active=activePackage();if(active.length()==0)return 0;
        JSONObject pkg=new JSONObject(active.optString("active_json","{}"));
        String adventureId=pkg.optString("id","");if(adventureId.isEmpty())return 0;
        String key=worldScope(active.optString("world_id"),active.optString("timeline_id","principal"));
        int wrote=0;
        JSONArray journal=envelope.optJSONArray("journal_updates");
        for(int i=0;journal!=null&&i<journal.length();i++){
            JSONObject event=journal.optJSONObject(i);
            if(event==null||!event.optBoolean("canonical_fact",false))continue;
            long actor=event.optLong("actor_character_id",0);
            if(actor<=0 || db.getCharacter(actor).length()==0)continue;
            // A canonical deed is still history even when nobody witnessed it.
            // Missing witnesses must NOT create fame or NPC knowledge by inference.
            JSONArray witnesses=event.optJSONArray("witness_npc_ids");
            JSONArray verified=new JSONArray();Set<Long> seen=new HashSet<>();
            for(int k=0;witnesses!=null&&k<witnesses.length();k++){
                long npcId=witnesses.optLong(k,0);
                if(npcId>0&&seen.add(npcId)&&db.getNpcMemory(npcId).length()>0)verified.put(npcId);
            }
            JSONObject fact=new JSONObject();
            fact.put("id",commitId+":"+i).put("adventure_id",adventureId)
                .put("actor_character_id",actor).put("witness_npc_ids",verified)
                .put("event_key",event.optString("event_key",commitId+":"+i))
                .put("title",limited(event.optString("title","Feito confirmado"),160))
                .put("summary",limited(event.optString("body",event.optString("summary","")),850))
                .put("location",limited(db.getSceneState().optString("location",""),150))
                .put("game_day",db.getSceneState().optInt("day_number",1))
                .put("visibility",verified.length()>0?"witnessed_only":"private_unwitnessed")
                .put("source","canonical_journal_commit");
            ContentValues values=new ContentValues();
            values.put("world_scope",key);values.put("commit_key",commitId+":"+i);
            values.put("actor_character_id",actor);values.put("adventure_id",adventureId);
            values.put("event_json",fact.toString());values.put("created_at",System.currentTimeMillis());
            long result=db.getWritableDatabase().insertWithOnConflict(
                "braseiro_adventure_causal_facts",null,values,SQLiteDatabase.CONFLICT_IGNORE);
            if(result>0)wrote++;
        }
        return wrote;
    }
    /** Scoped knowledge: only the participating NPC can recognize the hero. */
    public JSONArray factsKnownByNpc(long npcId,int max){
        JSONArray out=new JSONArray();
        if(npcId<=0)return out;
        try{
            ensureSchema();
            JSONObject current=activePackage();
            if(current.length()==0)return out;
            String scope=worldScope(current.optString("world_id"),current.optString("timeline_id","principal"));
            for(JSONObject row:db.queryList("SELECT event_json FROM braseiro_adventure_causal_facts "+
                    "WHERE world_scope=? ORDER BY created_at DESC LIMIT 250",new String[]{scope})){
                JSONObject fact=new JSONObject(row.optString("event_json","{}"));
                JSONArray witnesses=fact.optJSONArray("witness_npc_ids");
                for(int i=0;witnesses!=null&&i<witnesses.length();i++){
                    if(witnesses.optLong(i)==npcId){out.put(fact);break;}
                }
                if(out.length()>=max)break;
            }
        }catch(Exception ignored){/* no speculative recognition on corrupt context */}
        return out;
    }
    /** Private GM context; hides unknown deeds from NPCs without direct evidence. */
    public JSONObject sceneContext(){
        JSONObject result=new JSONObject();
        try{
            JSONObject scene=db.getSceneState();
            JSONArray present=new JSONArray(scene.optString("present_npcs_json","[]"));
            JSONArray known=new JSONArray();
            for(int i=0;i<present.length()&&i<12;i++){
                JSONObject npc=present.optJSONObject(i);
                long id=npc==null?present.optLong(i,0):npc.optLong("id",0);
                if(id<=0)continue;
                JSONArray facts=factsKnownByNpc(id,12);
                if(facts.length()>0)known.put(new JSONObject().put("npc_id",id).put("verified_facts",facts));
            }
            result.put("present_npc_knowledge",known);
            result.put("recognition_policy","NPCs só reconhecem feitos confirmados se foram testemunhas registradas. Boatos necessitam propagação causal; a ausência de evidência não autoriza conhecimento.");
        }catch(Exception ignored){}
        return result;
    }
    private static String limited(String s,int n){return s==null?"":s.substring(0,Math.min(s.length(),n));}
}
