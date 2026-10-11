package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Seleção aleatória ponderada de um ponto inicial já catalogado. */
public final class AdventureStartSelector {
    private AdventureStartSelector() {}

    public static JSONObject choose(List<JSONObject> candidates,String recentJson,long seed){
        if(candidates==null||candidates.isEmpty())return new JSONObject();
        JSONArray recent=parseArray(recentJson);ArrayList<JSONObject> pool=new ArrayList<JSONObject>();
        String mostRecentHex=recent.length()>0?recent.optJSONObject(0)==null?"":recent.optJSONObject(0).optString("hex_id",""):"";
        for(JSONObject candidate:candidates){if(candidate==null||candidate.optString("hex_id","").length()==0)continue;if(candidates.size()>1&&candidate.optString("hex_id").equalsIgnoreCase(mostRecentHex))continue;pool.add(candidate);}
        if(pool.isEmpty())pool.addAll(candidates);
        Random random=new Random(seed^System.nanoTime()^System.currentTimeMillis());double total=0d;ArrayList<Double> weights=new ArrayList<Double>();
        for(JSONObject candidate:pool){double weight=Math.max(1d,candidate.optDouble("start_weight",50d));String kind=fold(candidate.optString("start_kind","terrain"));String type=fold(candidate.optString("start_location_type",""));String region=fold(candidate.optString("start_location_region",""));String hex=fold(candidate.optString("hex_id",""));
            if("location".equals(kind))weight*=1.35d;else weight*=0.80d;
            for(int i=0;i<recent.length()&&i<8;i++){JSONObject old=recent.optJSONObject(i);if(old==null)continue;double age=1d/(i+1d);if(hex.equals(fold(old.optString("hex_id",""))))weight*=Math.max(.08d,1d-.82d*age);if(type.length()>0&&type.equals(fold(old.optString("type",""))))weight*=Math.max(.35d,1d-.45d*age);if(region.length()>0&&region.equals(fold(old.optString("region",""))))weight*=Math.max(.45d,1d-.35d*age);}
            double jitter=.82d+random.nextDouble()*.36d;weight=Math.max(.25d,weight*jitter);weights.add(weight);total+=weight;
        }
        if(total<=0d)return pool.get(random.nextInt(pool.size()));double roll=random.nextDouble()*total;
        for(int i=0;i<pool.size();i++){roll-=weights.get(i);if(roll<=0d)return pool.get(i);}return pool.get(pool.size()-1);
    }

    public static String remember(String recentJson,JSONObject chosen){/*BR_JSON_WRAP*/try{
        JSONArray old=parseArray(recentJson),out=new JSONArray();if(chosen!=null){JSONObject item=new JSONObject();try{item.put("hex_id",chosen.optString("hex_id",""));item.put("kind",chosen.optString("start_kind",""));item.put("type",chosen.optString("start_location_type",chosen.optString("terrain_primary","")));item.put("region",chosen.optString("start_location_region",""));item.put("name",chosen.optString("start_location_name",""));out.put(item);}catch(Exception ignored){AppLog.ignored("AdventureStartSelector","Falha tolerada ao registrar histórico de início.",ignored);}}
        for(int i=0;i<old.length()&&out.length()<8;i++){JSONObject item=old.optJSONObject(i);if(item==null)continue;String hex=item.optString("hex_id","");boolean duplicate=false;for(int j=0;j<out.length();j++){JSONObject existing=out.optJSONObject(j);if(existing!=null&&hex.length()>0&&hex.equalsIgnoreCase(existing.optString("hex_id",""))){duplicate=true;break;}}if(!duplicate)out.put(item);}return out.toString();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray parseArray(String raw){try{return raw==null||raw.trim().length()==0?new JSONArray():new JSONArray(raw);}catch(Exception ignored){return new JSONArray();}}
    private static String fold(String value){return value==null?"":value.trim().toLowerCase(Locale.US);}
}
