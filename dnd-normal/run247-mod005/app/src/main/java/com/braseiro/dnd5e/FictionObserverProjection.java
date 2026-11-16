package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Player-observable context only, never a copy of the internal GM bus.
 * In an observation round, NPC objectives, beliefs, secret IDs, upcoming
 * events and undiscovered clues MUST NOT reach the language model.
 */
public final class FictionObserverProjection {
    private FictionObserverProjection(){}
    private static final String[] SAFE_BUS={
        "world_profile","master_protocol","source_of_truth_manifest",
        "combat_state_machine","essential_rules_cache","campaign","story",
        "active_heroes_full","map","combat_state","rules_boundary"
    };
    private static final String[] SAFE_SCENE={
        "day_number","clock_hour","location","time_of_day","weather",
        "lighting","visibility","objective"
    };
    public static JSONObject visibleScene(JSONObject scene){
        JSONObject safe=new JSONObject();
        if(scene==null)return safe;
        for(String key:SAFE_SCENE)try{
            if(scene.has(key)&&!scene.isNull(key))safe.put(key,scene.opt(key));
        }catch(Exception ignored){}
        return safe;
    }
    public static JSONObject bus(JSONObject internal,JSONObject scene,JSONArray perceived){
        JSONObject safe=new JSONObject();
        if(internal!=null)for(String key:SAFE_BUS)try{
            if(internal.has(key)&&!internal.isNull(key))safe.put(key,internal.opt(key));
        }catch(Exception ignored){}
        try{
            safe.put("scene",visibleScene(scene));
            safe.put("onstage_perception",perceived==null?new JSONArray():perceived);
            safe.put("observer_only",true);
        }catch(Exception ignored){}
        return safe;
    }
}
