package com.braseiro.pathfinder2e;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/** Cache de orientação PF2e. Nunca concede autoridade mecânica por si só. */
public final class EssentialRulesCache {
    private static final String PROFILE="pathfinder_2e";
    private static final ConcurrentHashMap<String,JSONObject> CACHE=new ConcurrentHashMap<String,JSONObject>();
    private EssentialRulesCache(){}
    public static void warm(AppDb db){
        if(db==null)return;JSONObject out=new JSONObject();JSONArray evidence=new JSONArray();
        try{
            String[] qs={
                "Pathfinder 2e Remaster three actions reaction degrees of success critical success critical failure natural 20 natural 1",
                "Pathfinder 2e proficiency untrained trained expert master legendary level checks DC",
                "Pathfinder 2e dying wounded doomed persistent damage recovery check",
                "Pathfinder 2e exploration travel activities encounter initiative multiple attack penalty"
            };
            for(String q:qs){
                java.util.List<JSONObject> rows=db.searchManualLocalPagesFast(q,false,8);
                for(int i=0;i<rows.size()&&evidence.length()<16;i++){
                    JSONObject row=rows.get(i);if(isPf2AuthorityCandidate(row))evidence.put(row);
                }
            }
            out.put("world",PROFILE).put("ready",evidence.length()>0).put("evidence",evidence)
               .put("warmed_at",System.currentTimeMillis()).put("mechanics_authority",false)
               .put("purpose","PF2e Remaster framing only; deterministic engine or matched PF2e evidence is required for mechanics");
            CACHE.put(PROFILE,out);
        }catch(Exception e){AppLog.w("EssentialRulesCache","Falha ao aquecer cache PF2e essencial.",e);}
    }
    private static boolean isPf2AuthorityCandidate(JSONObject row){
        if(row==null)return false;
        String title=(row.optString("manual_title",row.optString("title",row.optString("book","")))+" "+row.optString("rule_profile","")).toLowerCase(Locale.US);
        if(title.contains("d&d")||title.contains("dnd")||title.contains("mystara")||title.contains("pathfinder 1")||title.contains("pathfinder1")||title.contains("pf1"))return false;
        return title.contains("pathfinder")||title.contains("player core")||title.contains("gm core")||title.contains("monster core")||title.contains("remaster")||title.contains("lost omens");
    }
    public static JSONObject snapshot(String world){
        JSONObject x=CACHE.get(PROFILE);try{return x==null?new JSONObject().put("world",PROFILE).put("ready",false).put("mechanics_authority",false):new JSONObject(x.toString());}catch(Exception e){return new JSONObject();}
    }
}
