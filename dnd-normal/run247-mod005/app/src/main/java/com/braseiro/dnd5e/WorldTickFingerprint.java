package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Iterator;
import java.security.MessageDigest;

/** Immutable identity for the scene snapshot a world tick was previewed from. */
public final class WorldTickFingerprint {
    private WorldTickFingerprint() {}
    public static String of(String profile,int day,double hour,String location,String hex){
        String raw=(profile==null?"":profile)+"|"+Math.max(1,day)+"|"+Double.toString(hour)+"|"+(location==null?"":location)+"|"+(hex==null?"":hex);
        return hash(raw);
    }
    /** Context binds weather, objectives, participants and other scene inputs too. */
    public static String of(String profile,int day,double hour,String location,String hex,String sceneContext){
        String raw=(profile==null?"":profile)+"|"+Math.max(1,day)+"|"+Double.toString(hour)+"|"+(location==null?"":location)+"|"+(hex==null?"":hex)+"|"+(sceneContext==null?"":sceneContext);
        return hash(raw);
    }
    /** Hashes the immutable preview payload while ignoring commit bookkeeping. */
    public static String payload(JSONObject report){
        if(report==null)return "";
        try{
            // MasterOrchestrator adds prompt-only material (for example its
            // environmental director) to the local-tick object after preview.
            // Seal exactly the fields consumed by WorldStateEngine.commit.
            JSONObject payload=new JSONObject();
            String[] fields={"world_tick_fingerprint","world_clocks_fingerprint","hours_requested","hours_advanced",
                    "long_rest","short_rest","long_rest_benefits","day_before","day_after","clock_hour_before",
                    "clock_hour_after","time_before","time_after","travel_rules","company_resources_consumed",
                    "living_world_enabled","events_triggered","event_consequence_preview","clocks_advanced",
                    "living_world_simulation","error"};
            for(String key:fields)if(report.has(key))payload.put(key,sealValue(report.opt(key)));
            return CanonicalJson.sha256(payload);
        }
        catch(Exception e){throw new IllegalStateException("Não foi possível selar a prévia do mundo",e);}
    }
    private static Object sealValue(Object value)throws Exception{
        if(value instanceof JSONObject){
            JSONObject source=(JSONObject)value,copy=new JSONObject();Iterator<String> keys=source.keys();
            while(keys.hasNext()){String key=keys.next();if("world_tick_payload_fingerprint".equals(key)||"commit_error".equals(key)||"committed".equals(key)||"world_6".equals(key)||"timed_character_recoveries".equals(key))continue;copy.put(key,sealValue(source.opt(key)));}
            return copy;
        }
        if(value instanceof JSONArray){JSONArray source=(JSONArray)value,copy=new JSONArray();for(int i=0;i<source.length();i++)copy.put(sealValue(source.opt(i)));return copy;}
        return value;
    }
    private static String hash(String raw){
        try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(raw.getBytes("UTF-8"));StringBuilder out=new StringBuilder();for(int i=0;i<12;i++)out.append(String.format(java.util.Locale.US,"%02x",bytes[i]&255));return out.toString();}
        catch(Exception e){throw new IllegalStateException("SHA-256 indisponível",e);}
    }
}
