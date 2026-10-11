package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Persistent narrative auxiliary state, isolated by campaign and mirrored in party backups. */
public final class DynamicMasterState {
    private static final String FILE="braseiro_dynamic_master_v2";
    private static final String PARTY_KEY="human_master_dynamic_v2";
    private DynamicMasterState(){}

    private static String scope(AppDb db){
        try{
            JSONObject campaign=db.getCampaign();
            String id=campaign.optString("id","").trim();
            if(id.isEmpty())id=campaign.optString("name","").trim();
            if(id.isEmpty())id="default_campaign";
            return Integer.toHexString(id.hashCode());
        }catch(Exception ignored){return "default_campaign";}
    }
    private static SharedPreferences pref(Context c){return c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
    public static JSONObject read(Context c,AppDb db){
        try{
            JSONObject party=db.getPartyState();
            JSONObject session=new JSONObject(party.optString("session_preferences_json","{}"));
            JSONObject allCampaigns=session.optJSONObject(PARTY_KEY);
            JSONObject saved=allCampaigns==null?null:allCampaigns.optJSONObject(scope(db));
            if(saved!=null && saved.length()>0)return new JSONObject(saved.toString());
        }catch(Exception ignored){}
        try{return new JSONObject(pref(c).getString("state_"+scope(db),"{}"));}
        catch(Exception ignored){return new JSONObject();}
    }
    public static void write(Context c,AppDb db,JSONObject state){
        if(state==null)return;
        String serialized=state.toString();
        pref(c).edit().putString("state_"+scope(db),serialized).apply();
        try{
            JSONObject party=db.getPartyState();
            if(party==null||party.length()==0)return;
            JSONObject session=new JSONObject(party.optString("session_preferences_json","{}"));
            JSONObject allCampaigns=session.optJSONObject(PARTY_KEY);
            if(allCampaigns==null)allCampaigns=new JSONObject();
            allCampaigns.put(scope(db),new JSONObject(serialized));
            session.put(PARTY_KEY,allCampaigns);
            party.put("session_preferences_json",session.toString());
            db.savePartyState(party);
        }catch(Exception ignored){/* backup persistence is best effort; local copy remains */}
    }
    public static JSONArray recent(JSONObject state,String key){
        JSONArray a=state==null?null:state.optJSONArray(key);
        return a==null?new JSONArray():a;
    }
    public static JSONArray prepend(JSONArray source,String value,int max){
        JSONArray out=new JSONArray();
        if(value!=null&&!value.trim().isEmpty())out.put(value.trim());
        if(source!=null)for(int i=0;i<source.length()&&out.length()<Math.max(1,max);i++){
            String v=source.optString(i,"").trim();
            if(!v.isEmpty()&&!v.equalsIgnoreCase(value))out.put(v);
        }
        return out;
    }
    public static JSONObject readLocation(JSONObject state,String locationKey){
        JSONObject locations=state==null?null:state.optJSONObject("locations");
        JSONObject o=locations==null?null:locations.optJSONObject(locationKey);
        return o==null?new JSONObject():o;
    }
    /** Add only confirmed world facts, capped, in the persistent local history. */
    public static void rememberCommittedLocationEvent(JSONObject state,String location,String description){
        if(state==null||location==null||location.trim().isEmpty()||description==null||description.trim().isEmpty())return;
        rememberLocation(state,location,"rotina própria, memória do lugar e pessoas que continuam vivendo");
        String key=Integer.toHexString(location.toLowerCase(java.util.Locale.ROOT).hashCode());
        JSONObject loc=readLocation(state,key);
        if(loc.length()==0)return;
        String summary=description.trim();if(summary.length()>240)summary=summary.substring(0,240);
        JSONArray prior=recent(loc,"history");
        JSONArray next=prepend(prior,summary,12);
        try{
            loc.put("history",next);
            JSONObject all=state.optJSONObject("locations");
            if(all!=null){all.put(key,loc);state.put("locations",all);}
        }catch(Exception ignored){}
    }

    public static void rememberLocation(JSONObject state,String location,String identity){
        if(state==null||location==null||location.trim().isEmpty())return;
        String key=Integer.toHexString(location.toLowerCase(java.util.Locale.ROOT).hashCode());
        JSONObject loc=state.optJSONObject("locations");if(loc==null)loc=new JSONObject();
        if(loc.optJSONObject(key)!=null)return; // never overwrite established identity
        JSONObject item=new JSONObject();
        try{
            item.put("name",location).put("identity",identity).put("history",new JSONArray());
            loc.put(key,item);state.put("locations",loc);
        }catch(Exception ignored){}
    }
}
