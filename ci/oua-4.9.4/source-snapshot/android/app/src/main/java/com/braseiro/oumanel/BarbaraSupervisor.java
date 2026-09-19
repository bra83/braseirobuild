package com.braseiro.oumanel;

import android.content.Context;
import android.content.SharedPreferences;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Ponte local para Motor Barbara 1.1.3, vendorizado no APK de O Um Anel.
 *
 * O host continua autoridade de IndexedDB, regras, Atlas, RAG, áudio, Mundo Vivo,
 * Gemini e commits. Barbara só supervisiona contrato/política e valida projeções.
 */
public final class BarbaraSupervisor {
    private static final String PREFS="barbara_supervisor_one_ring";
    private static final String KEY_STARTED="campaign_started";
    private static final String KEY_LOCATIONS="location_imprints";
    private final Context context;
    private final SharedPreferences prefs;
    private PyObject module;
    private boolean available;
    private String lastError="";

    public BarbaraSupervisor(Context ctx){
        context=ctx==null?null:ctx.getApplicationContext();
        prefs=context==null?null:context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        initialize();
    }

    private synchronized void initialize(){
        if(context==null){available=false;lastError="context_unavailable";return;}
        try{
            if(!Python.isStarted()) Python.start(new AndroidPlatform(context));
            module=Python.getInstance().getModule("braseiro_oumanel_barbara_adapter");
            JSONObject health=new JSONObject(module.callAttr("health_json").toString());
            available=health.optBoolean("ok",false)
                    &&"the_one_ring".equals(health.optString("system_id"))
                    &&"1.1.3".equals(health.optString("barbara_version"));
            lastError=available?"":"health_failed";
        }catch(Throwable failure){
            available=false;
            lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Motor Barbara indisponível; O Um Anel host permanece ativo.",failure);
        }
    }

    public boolean isAvailable(){return available;}

    public JSONObject health(){
        try{
            if(module==null) return new JSONObject().put("ok",false).put("available",false).put("error",lastError);
            JSONObject out=new JSONObject(module.callAttr("health_json").toString());
            out.put("available",available);return out;
        }catch(Throwable failure){
            return error("health_error",failure,false);
        }
    }

    public JSONObject oneRingContract(){
        try{
            if(!available||module==null)return new JSONObject().put("available",false);
            JSONObject out=new JSONObject(module.callAttr("one_ring_contract_json").toString());
            out.put("available",true);return out;
        }catch(Throwable failure){return error("one_ring_contract_error",failure,false);}
    }

    public JSONObject validateState(JSONObject hostState){
        try{
            if(!available||module==null)return new JSONObject().put("valid",true).put("bypassed",true).put("reason","barbara_unavailable");
            return new JSONObject(module.callAttr("state_adapter_json",hostState==null?"{}":hostState.toString()).toString());
        }catch(Throwable failure){return error("state_adapter_error",failure,false);}
    }

    private JSONObject locationImprints(){
        if(prefs==null)return new JSONObject();
        try{return new JSONObject(prefs.getString(KEY_LOCATIONS,"{}"));}catch(Exception ignored){return new JSONObject();}
    }

    public String storyOccasion(boolean narrative,boolean nonAdvancing,String location,String imprint){
        if(!narrative||nonAdvancing)return "continuation";
        if(prefs==null)return "continuation";
        boolean started=prefs.getBoolean(KEY_STARTED,false);
        if(!started)return "campaign_opening";
        if(location==null||location.trim().isEmpty())return "continuation";
        JSONObject known=locationImprints();
        if(!known.has(location))return "first_arrival";
        String previous=known.optString(location,"");
        if(imprint!=null&&!imprint.isEmpty()&&!imprint.equals(previous))return "changed_return";
        return "continuation";
    }

    public JSONObject plan(String action,String importance,boolean narrative,boolean nonAdvancing,String location,String imprint,String inputChannel,String requestedOccasion){
        String occasion = requestedOccasion == null ? "" : requestedOccasion.trim();
        if (!("campaign_opening".equals(occasion) || "first_arrival".equals(occasion) || "changed_return".equals(occasion) || "continuation".equals(occasion))) {
            occasion=storyOccasion(narrative,nonAdvancing,location,imprint);
        }
        if (!narrative || nonAdvancing || "GM_HELP".equals(inputChannel)) occasion="continuation";
        JSONObject fallback=new JSONObject();
        try{
            fallback.put("available",available).put("story_occasion",occasion).put("host_world_authority",true).put("duplicate_gemini_call",false).put("input_channel",inputChannel);
            if(!available||module==null)return fallback;
            JSONObject out=new JSONObject(module.callAttr("plan_json",action==null?"":action,importance==null?"normal":importance,narrative,nonAdvancing,occasion,inputChannel).toString());
            out.put("available",true).put("story_occasion",occasion);return out;
        }catch(Throwable failure){
            lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Falha ao montar plano Barbara; plano host preservado.",failure);
            return fallback;
        }
    }

    public JSONObject validate(JSONObject envelope,String action,String importance,String occasion,String inputChannel){
        try{
            if(!available||module==null)return new JSONObject().put("valid",true).put("bypassed",true).put("reason","barbara_unavailable");
            return new JSONObject(module.callAttr("validate_envelope_json",envelope==null?"{}":envelope.toString(),action==null?"":action,importance==null?"normal":importance,occasion==null?"continuation":occasion,inputChannel).toString());
        }catch(Throwable failure){
            lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Validação Barbara indisponível; validações host preservadas.",failure);
            try{return new JSONObject().put("valid",true).put("bypassed",true).put("reason","barbara_validation_error");}catch(Exception ignored){return new JSONObject();}
        }
    }

    public JSONObject interpretOneRing(JSONArray featResults,JSONArray successResults,int target,String mode,boolean weary,int modifier,JSONObject hostRoll){
        try{
            if(!available||module==null)return new JSONObject().put("available",false).put("valid",true).put("bypassed",true);
            Object hostTotal=hostRoll!=null&&hostRoll.has("total")?Integer.valueOf(hostRoll.optInt("total")):null;
            Object hostSuccess=hostRoll!=null&&hostRoll.has("success")?Boolean.valueOf(hostRoll.optBoolean("success")):null;
            Object hostLevel=hostRoll!=null&&hostRoll.has("successLevel")?hostRoll.optString("successLevel"):null;
            Object hostTengwar=hostRoll!=null&&hostRoll.has("tengwarCount")?Integer.valueOf(hostRoll.optInt("tengwarCount")):null;
            Object hostFeat=hostRoll!=null&&hostRoll.has("featResult")?Integer.valueOf(hostRoll.optInt("featResult")):null;
            Object hostMiserable=hostRoll!=null&&hostRoll.has("miserable")?Boolean.valueOf(hostRoll.optBoolean("miserable")):Boolean.FALSE;
            JSONObject out=new JSONObject(module.callAttr(
                    "interpret_one_ring_roll_json",
                    featResults==null?"[]":featResults.toString(),
                    successResults==null?"[]":successResults.toString(),
                    target,mode==null?"normal":mode,weary,modifier,
                    hostTotal,hostSuccess,hostLevel,hostTengwar,hostFeat,hostMiserable
            ).toString());
            out.put("available",true).put("valid",true);return out;
        }catch(Throwable failure){return error("one_ring_interpretation_mismatch",failure,false);}
    }

public JSONObject configureDeviceProfile(JSONObject metrics){
    try{
        if(!available||module==null)return new JSONObject().put("available",false);
        JSONObject out=new JSONObject(module.callAttr("configure_device_profile_json",metrics==null?"{}":metrics.toString()).toString());
        out.put("available",true);return out;
    }catch(Throwable failure){return error("device_profile_config_error",failure,false);}
}

public JSONObject deviceProfile(){
    try{
        if(!available||module==null)return new JSONObject().put("available",false);
        JSONObject out=new JSONObject(module.callAttr("device_profile_json").toString());
        out.put("available",true);return out;
    }catch(Throwable failure){return error("device_profile_error",failure,false);}
}

public JSONObject supervisorInspect(JSONObject hostState,String text,String importance,String inputChannel){
    try{
        if(!available||module==null)return new JSONObject().put("available",false).put("bypassed",true);
        JSONObject out=new JSONObject(module.callAttr("supervisor_inspect_json",hostState==null?"{}":hostState.toString(),text==null?"":text,importance==null?"normal":importance,inputChannel).toString());
        out.put("available",true);return out;
    }catch(Throwable failure){return error("supervisor_inspect_error",failure,false);}
}

public JSONObject supervisorFinalizeNarrative(JSONObject hostState,String text,String requestId,String narration,String importance,String inputChannel,JSONObject resolution,JSONObject presentation){
    try{
        if(!available||module==null)return new JSONObject().put("available",false).put("valid",true).put("bypassed",true);
        JSONObject out=new JSONObject(module.callAttr(
            "supervisor_finalize_narrative_json",
            hostState==null?"{}":hostState.toString(), text==null?"":text, requestId==null?"":requestId,
            narration==null?"":narration, importance==null?"normal":importance, inputChannel,
            resolution==null?null:resolution.toString(), presentation==null?null:presentation.toString()
        ).toString());
        out.put("available",true);
        String source=out.optString("narration_source");
        boolean providerAccepted="host_provider".equals(source);
        boolean presentationFallback="deterministic_fallback".equals(source);
        out.put("valid",providerAccepted||presentationFallback);
        out.put("needs_repair",presentationFallback);
        return out;
    }catch(Throwable failure){return error("supervisor_finalize_error",failure,false);}
}

public JSONObject audioScenePlan(JSONArray events,JSONObject contextJson,JSONObject catalog,JSONObject mix,boolean narrationActive){
    try{
        if(!available||module==null)return new JSONObject().put("available",false).put("bypassed",true);
        JSONObject out=new JSONObject(module.callAttr(
            "audio_scene_plan_json", events==null?"[]":events.toString(),
            contextJson==null?null:contextJson.toString(), catalog==null?null:catalog.toString(),
            mix==null?null:mix.toString(), narrationActive
        ).toString());
        out.put("available",true);return out;
    }catch(Throwable failure){return error("audio_scene_plan_error",failure,false);}
}

public JSONArray visiblePois(JSONArray canonical,JSONArray campaign,JSONArray procedural,boolean gm){
    try{
        if(!available||module==null)return new JSONArray();
        return new JSONArray(module.callAttr("poi_visible_json",canonical==null?"[]":canonical.toString(),campaign==null?"[]":campaign.toString(),procedural==null?"[]":procedural.toString(),gm).toString());
    }catch(Throwable failure){lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());return new JSONArray();}
}

    public void markExperienced(String location,String imprint){
        if(prefs==null)return;
        try{
            JSONObject known=locationImprints();
            if(location!=null&&!location.trim().isEmpty())known.put(location,imprint==null?"":imprint);
            prefs.edit().putBoolean(KEY_STARTED,true).putString(KEY_LOCATIONS,known.toString()).apply();
        }catch(Exception failure){AppLog.w("BarbaraSupervisor","Não foi possível persistir somente o índice de descoberta Barbara.",failure);}
    }

    public JSONObject diagnostic(){
        JSONObject out=health();
        try{
            out.put("embedded",true)
                    .put("last_error",lastError)
                    .put("campaign_started",prefs!=null&&prefs.getBoolean(KEY_STARTED,false))
                    .put("known_locations",locationImprints().length())
                    .put("system_module",available?oneRingContract():new JSONObject().put("available",false));
        }catch(Exception ignored){}
        return out;
    }

    private JSONObject error(String code,Throwable failure,boolean valid){
        lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
        AppLog.w("BarbaraSupervisor",code,failure);
        try{return new JSONObject().put("available",available).put("valid",valid).put("reason",code).put("error",lastError);}catch(Exception ignored){return new JSONObject();}
    }
}
