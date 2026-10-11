package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

/** Canonical long-term history. It projects already-consumed facts; it never invents mechanics. */
public final class WorldHistoryEngine {
    private final AppDb db;
    public WorldHistoryEngine(AppDb database){db=database;AppDb.ensureSixZeroSchema(db.getWritableDatabase());}

    public synchronized JSONObject ingestCanonicalFacts(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();int added=0;
        try{
            String profile=db.getActiveMapProfile();SQLiteDatabase sql=db.getWritableDatabase();
            List<JSONObject> rows=db.queryList("SELECT id,title,body,kind,event_key,source_kind,created_at FROM journal WHERE world_profile=? AND canonical_fact=1 ORDER BY created_at,id",new String[]{profile});
            int day=Math.max(1,db.getSceneState().optInt("day_number",1));
            for(JSONObject j:rows){
                String key=j.optString("event_key").trim();if(key.isEmpty())key="journal:"+j.optLong("id");
                if(db.queryOne("SELECT id FROM world_history_events WHERE world_profile=? AND event_key=?",new String[]{profile,key}).length()>0)continue;
                String body=j.optString("body").trim();if(body.isEmpty())continue;
                ContentValues v=new ContentValues();v.put("world_profile",profile);v.put("event_key",key);v.put("day_number",day);v.put("era_label",worldSeed().optString("era_label","Era atual"));v.put("scope",inferScope(j));v.put("location",db.getSceneState().optString("location"));v.put("title",j.optString("title","Acontecimento"));v.put("summary",limit(body,1400));v.put("actors_json",inferActors(body).toString());v.put("causes_json","[]");v.put("consequences_json","[]");v.put("tags_json",inferTags(j).toString());v.put("significance",significance(j));v.put("source_kind",j.optString("source_kind","journal"));v.put("source_id",String.valueOf(j.optLong("id")));v.put("canonical",1);v.put("created_at",j.optLong("created_at",System.currentTimeMillis()));
                if(sql.insertWithOnConflict("world_history_events",null,v,SQLiteDatabase.CONFLICT_IGNORE)>0)added++;
            }
            out.put("ingested",added).put("total",count()).put("world_seed",worldSeed());
        }catch(Exception e){try{out.put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){AppLog.ignored("WorldHistoryEngine","Falha tolerada na camada 6.0.",ignored);}}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject worldSeed(){return db.queryOne("SELECT * FROM world_seeds WHERE world_profile=?",new String[]{db.getActiveMapProfile()});}
    public int count(){return db.queryOne("SELECT COUNT(*) n FROM world_history_events WHERE world_profile=?",new String[]{db.getActiveMapProfile()}).optInt("n",0);}
    public JSONArray recent(int limit){return new JSONArray(db.queryList("SELECT * FROM world_history_events WHERE world_profile=? ORDER BY day_number DESC,significance DESC,id DESC LIMIT "+Math.max(1,Math.min(500,limit)),new String[]{db.getActiveMapProfile()}));}
    public JSONArray important(int limit){return new JSONArray(db.queryList("SELECT * FROM world_history_events WHERE world_profile=? AND significance>=50 ORDER BY significance DESC,day_number DESC,id DESC LIMIT "+Math.max(1,Math.min(500,limit)),new String[]{db.getActiveMapProfile()}));}

    public JSONObject context(){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();try{o.put("seed",worldSeed()).put("recent",recent(18)).put("important",important(18)).put("contract","A história registra fatos consumados; intenção, rumor e sugestão não viram fato sem evento canônico.");}catch(Exception ignored){AppLog.ignored("WorldHistoryEngine","Falha tolerada na camada 6.0.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized String ensureSeed(String lineage){/*BR_JSON_WRAP*/try{
        JSONObject current=worldSeed();String seed=current.optString("seed_id");if(!seed.isEmpty())return seed;
        seed=db.getActiveMapProfile()+"-"+Long.toHexString(System.currentTimeMillis());ContentValues v=new ContentValues();v.put("world_profile",db.getActiveMapProfile());v.put("seed_id",seed);v.put("lineage_name",lineage==null?"Mundo persistente":lineage);v.put("origin_campaign",db.getCampaign().optString("name"));v.put("generation",1);v.put("era_label","Era atual");v.put("metadata_json","{}");v.put("created_at",System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().insertWithOnConflict("world_seeds",null,v,SQLiteDatabase.CONFLICT_REPLACE);return seed;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static int significance(JSONObject j){String k=(j.optString("kind")+" "+j.optString("title")+" "+j.optString("body")).toLowerCase(Locale.ROOT);int s=25;if(k.matches(".*(morte|morreu|guerra|rei|rainha|destruid|conquist|fundad|tratado|aliança|alianca|fortaleza|cidade|guilda|dragão|dragao|artefato).*"))s+=35;if(k.matches(".*(combate|missão|missao|contrato|facção|faccao|evento).*"))s+=15;return Math.min(100,s);}
    private static String inferScope(JSONObject j){String k=(j.optString("kind")+" "+j.optString("title")).toLowerCase(Locale.ROOT);if(k.contains("mundo")||k.contains("guerra"))return "regional";if(k.contains("facção")||k.contains("guilda"))return "organizacional";return "local";}
    private static JSONArray inferTags(JSONObject j){/*BR_JSON_WRAP*/try{JSONArray a=new JSONArray();String k=(j.optString("kind")+" "+j.optString("title")+" "+j.optString("body")).toLowerCase(Locale.ROOT);String[] tags={"combate","missão","guilda","facção","viagem","morte","aliança","cidade","ruína","artefato","contrato","guerra"};for(String t:tags)if(k.contains(t))a.put(t);return a;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONArray inferActors(String body){/*BR_JSON_WRAP*/try{JSONArray a=new JSONArray();if(body==null)return a;java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\b([A-ZÁÉÍÓÚÂÊÔÃÕÇ][\\p{L}'-]+(?:\\s+[A-ZÁÉÍÓÚÂÊÔÃÕÇ][\\p{L}'-]+){0,2})").matcher(body);java.util.HashSet<String> seen=new java.util.HashSet<String>();while(m.find()&&a.length()<8){String n=m.group(1).trim();if(n.length()>2&&seen.add(n.toLowerCase(Locale.ROOT)))a.put(n);}return a;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String limit(String s,int n){if(s==null)return "";return s.length()<=n?s:s.substring(0,n)+"…";}
}
