package com.braseiro.pathfinder2e;
import android.content.Context;
import org.json.JSONObject;
/** Test-only stub: the real implementation depends on Chaquopy. */
public final class BarbaraSupervisor {
    public BarbaraSupervisor(Context context){}
    public JSONObject plan(String action,String importance,boolean narrative,boolean nonAdvancing,String location,String imprint){return new JSONObject();}
    public JSONObject plan(String action,String importance,boolean narrative,boolean nonAdvancing,String location,String imprint,String inputChannel){return new JSONObject();}
    public JSONObject validate(JSONObject envelope,String action,String importance,String occasion){try{return new JSONObject().put("valid",true).put("bypassed",true);}catch(Exception e){return new JSONObject();}}
    public JSONObject validate(JSONObject envelope,String action,String importance,String occasion,String inputChannel){try{return new JSONObject().put("valid",true).put("bypassed",true);}catch(Exception e){return new JSONObject();}}
    public void markExperienced(String location,String imprint){}
    public JSONObject diagnostic(){return new JSONObject();}
}
