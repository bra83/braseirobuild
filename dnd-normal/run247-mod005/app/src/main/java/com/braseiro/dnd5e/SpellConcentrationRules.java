package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Shared 2014/2024 concentration timeline; callers persist the returned copy atomically. */
public final class SpellConcentrationRules {
    private SpellConcentrationRules() {}
    public static JSONObject begin(JSONObject sheet,String profile,String spell,int round,int durationRounds){
        // Keep the persisted timeline bounded and prevent round+duration int overflow.
        if(sheet==null||spell==null||spell.trim().isEmpty()||round<1||durationRounds<1||
            durationRounds>14400||(long)round+(long)durationRounds>Integer.MAX_VALUE)
            return fail("invalid_concentration_start");
        if(!"2014".equals(profile)&&!"2024".equals(profile))return fail("unsupported_rules_profile");
        try{
            JSONObject out=new JSONObject(sheet.toString());
            JSONObject active=new JSONObject().put("spell",spell.trim()).put("profile",profile)
                .put("started_round",round).put("expires_round",round+durationRounds)
                .put("duration_rounds",durationRounds).put("concentration",true);
            out.put("active_concentration",active).put("concentration_spell",spell.trim());
            return out;
        }catch(Exception e){return fail("concentration_state_copy_failed");}
    }
    public static JSONObject advance(JSONObject sheet,int round){
        if(sheet==null||round<1)return fail("invalid_round");
        try{
            JSONObject out=new JSONObject(sheet.toString()),active=out.optJSONObject("active_concentration");
            if(active==null)return out;
            if(round>=active.optInt("expires_round",Integer.MAX_VALUE)){
                out.remove("active_concentration");out.remove("concentration_spell");
            }
            return out;
        }catch(Exception e){return fail("concentration_state_copy_failed");}
    }
    public static JSONObject concentrationSave(JSONObject sheet,int damage,int natural,int bonus,int dc){
        if(sheet==null||damage<0||natural<1||natural>20||bonus<-30||bonus>60||dc<1||dc>40)return fail("invalid_concentration_save");
        try{
            JSONObject out=new JSONObject(sheet.toString()),active=out.optJSONObject("active_concentration");
            if(active==null)return out.put("concentration_save_skipped",true);
            boolean success=natural!=1&&(natural==20||natural+bonus>=dc);
            out.put("concentration_save_natural",natural).put("concentration_save_dc",dc)
                .put("concentration_maintained",success);
            if(!success){out.remove("active_concentration");out.remove("concentration_spell");}
            return out;
        }catch(Exception e){return fail("concentration_state_copy_failed");}
    }
    public static JSONObject end(JSONObject sheet){
        if(sheet==null)return fail("missing_concentration_state");
        try{JSONObject out=new JSONObject(sheet.toString());out.remove("active_concentration");out.remove("concentration_spell");return out;}
        catch(Exception e){return fail("concentration_state_copy_failed");}
    }
    private static JSONObject fail(String error){try{return new JSONObject().put("concentration_blocked",error);}catch(Exception e){return new JSONObject();}}
}
