package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache efêmero de procedimentos por cena.
 *
 * Reutiliza a mesma prova local enquanto a posição/tempo/combate da cena não
 * muda. Não persiste consequências e não transforma a IA em autoridade de
 * regra; apenas evita repetir a mesma busca RAG para o mesmo procedimento.
 */
public final class SceneRuleCache {
    private static final int MAX_ENTRIES=12;
    private String activeScope="";
    private final LinkedHashMap<String,RuleRetrievalEngine.Result> entries=
            new LinkedHashMap<String,RuleRetrievalEngine.Result>(16,0.75f,true){
                protected boolean removeEldestEntry(Map.Entry<String,RuleRetrievalEngine.Result> eldest){return size()>MAX_ENTRIES;}
            };

    public synchronized RuleRetrievalEngine.Result get(String scope,String key){
        rotate(scope);
        RuleRetrievalEngine.Result found=entries.get(safe(key));
        if(found==null)return null;
        JSONObject bundle=copy(found.bundle);
        try{
            String original=bundle.optString("status","ready");
            bundle.put("cache_hit",true).put("cache_scope",activeScope).put("cache_origin_status",original).put("status","scene_rule_cache_hit");
        }catch(Exception ignored){/* cache é melhoria de desempenho; falha de metadado não invalida a prova */}
        return new RuleRetrievalEngine.Result(bundle,copyRows(found.hits));
    }

    public synchronized void put(String scope,String key,RuleRetrievalEngine.Result result){
        if(result==null||result.bundle==null)return;
        String status=result.bundle.optString("status","");
        int evidence=result.bundle.optInt("evidence_count",result.hits==null?0:result.hits.size());
        if(evidence<=0||"retrieval_timeout".equals(status)||"error".equals(status)||"index_unavailable".equals(status))return;
        rotate(scope);
        JSONObject bundle=copy(result.bundle);
        try{bundle.put("cache_hit",false).put("cache_scope",activeScope);}catch(Exception ignored){}
        entries.put(safe(key),new RuleRetrievalEngine.Result(bundle,copyRows(result.hits)));
    }

    public synchronized void clear(){activeScope="";entries.clear();}
    public synchronized int size(){return entries.size();}

    private void rotate(String scope){String next=safe(scope);if(!next.equals(activeScope)){activeScope=next;entries.clear();}}

    public static String scope(JSONObject scene,JSONObject map,JSONObject combat){
        String material=(map==null?"":map.optString("map_package_id"))+"|"+
                (map==null?"":map.optString("current_hex_id"))+"|"+
                (scene==null?"":scene.optString("location"))+"|"+
                (scene==null?0:scene.optInt("day_number",0))+"|"+
                (scene==null?"":scene.optString("time_of_day",scene.optString("period")))+"|"+
                (combat==null?0:combat.optInt("running",0))+"|"+
                (combat==null?0:combat.optInt("round_no",0));
        return sha(material);
    }

    private static java.util.List<JSONObject> copyRows(java.util.List<JSONObject> rows){
        java.util.ArrayList<JSONObject> out=new java.util.ArrayList<JSONObject>();if(rows==null)return out;
        for(JSONObject row:rows)out.add(copy(row));return out;
    }
    private static JSONObject copy(JSONObject value){try{return new JSONObject(value==null?"{}":value.toString());}catch(Exception e){return new JSONObject();}}
    private static String safe(String value){return value==null?"":value;}
    private static String sha(String value){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] bytes=md.digest(safe(value).getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(int i=0;i<12;i++)b.append(String.format(java.util.Locale.US,"%02x",bytes[i]));return b.toString();}catch(Exception e){return Integer.toHexString(safe(value).hashCode());}}
}
