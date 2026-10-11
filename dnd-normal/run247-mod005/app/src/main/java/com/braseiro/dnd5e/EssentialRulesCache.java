package com.braseiro.dnd5e;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ConcurrentHashMap;

/** Pequeno cache em memória das regras gerais; não substitui evidência específica para mecânicas. */
public final class EssentialRulesCache {
    private static final ConcurrentHashMap<String,JSONObject> CACHE=new ConcurrentHashMap<String,JSONObject>();
    private EssentialRulesCache(){}
    public static void warm(AppDb db,boolean mystara){/*BR_JSON_WRAP*/try{
        if(db==null)return;String world=mystara?"mystara":"dnd";JSONObject out=new JSONObject();JSONArray evidence=new JSONArray();
        try{
            String[] qs=mystara?new String[]{"initiative encounter reaction morale travel movement saving throw","rest healing exploration encounter reaction"}:new String[]{"ability check saving throw advantage disadvantage initiative encounter","travel pace perception social interaction short rest long rest","attack damage resistance condition death saving throw"};
            for(String q:qs){java.util.List<JSONObject> rows=db.searchManualLocalPagesFast(q,mystara,4);for(int i=0;i<rows.size()&&evidence.length()<10;i++)evidence.put(rows.get(i));}
            out.put("world",world).put("ready",evidence.length()>0).put("evidence",evidence).put("warmed_at",System.currentTimeMillis()).put("mechanics_authority",false).put("purpose","general framing only; specific mechanics require matched evidence or deterministic engine");
            CACHE.put(world,out);
        }catch(Exception e){AppLog.w("EssentialRulesCache","Falha ao aquecer cache essencial.",e);}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public static JSONObject snapshot(String world){/*BR_JSON_WRAP*/try{JSONObject x=CACHE.get("mystara".equalsIgnoreCase(world)?"mystara":"dnd");try{return x==null?new JSONObject().put("ready",false):new JSONObject(x.toString());}catch(Exception e){return new JSONObject();}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}
