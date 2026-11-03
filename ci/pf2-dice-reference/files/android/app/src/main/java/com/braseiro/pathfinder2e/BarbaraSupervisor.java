package com.braseiro.pathfinder2e;

import android.content.Context;
import android.content.SharedPreferences;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONObject;

/**
 * Ponte determinística para o Motor Barbara 1.1.2 embutido no APK.
 *
 * O Braseiro continua sendo autoridade de persistência, regras, mapa, áudio,
 * fichas, combate e Mundo Vivo. Barbara supervisiona o contrato narrativo e a
 * descoberta do mundo, sem uma segunda chamada Gemini.
 */
public final class BarbaraSupervisor {
    private static final String PREFS="barbara_supervisor_pathfinder2e";
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
            if(!Python.isStarted())Python.start(new AndroidPlatform(context));
            module=Python.getInstance().getModule("braseiro_barbara_adapter");
            JSONObject health=new JSONObject(module.callAttr("health_json").toString());
            available=health.optBoolean("ok",false)&&"pathfinder_2e".equals(health.optString("system_id"));
            lastError=available?"":"health_or_system_mismatch";
        }catch(Throwable failure){
            available=false;lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Motor Barbara indisponível; o Mestre legado permanece ativo.",failure);
        }
    }

    public boolean isAvailable(){return available;}

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

    public JSONObject plan(String action,String importance,boolean narrative,boolean nonAdvancing,String location,String imprint){return plan(action,importance,narrative,nonAdvancing,location,imprint,null);}

    public JSONObject plan(String action,String importance,boolean narrative,boolean nonAdvancing,String location,String imprint,String inputChannel){
        String occasion=storyOccasion(narrative,nonAdvancing,location,imprint);
        JSONObject fallback=new JSONObject();
        try{
            fallback.put("available",available).put("story_occasion",occasion).put("host_world_authority",true).put("duplicate_gemini_call",false).put("input_channel",inputChannel==null?JSONObject.NULL:inputChannel);
            if(!available||module==null)return fallback;
            PyObject channel=inputChannel==null?PyObject.fromJava(null):PyObject.fromJava(inputChannel);
            JSONObject out=new JSONObject(module.callAttr("plan_json",action==null?"":action,importance==null?"normal":importance,narrative,nonAdvancing,occasion,channel).toString());
            out.put("available",true).put("story_occasion",occasion);
            return out;
        }catch(Throwable failure){
            available=false;lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Falha ao montar plano Barbara; preservando o plano host.",failure);
            return fallback;
        }
    }

    public JSONObject validate(JSONObject envelope,String action,String importance,String occasion){return validate(envelope,action,importance,occasion,null);}

    public JSONObject validate(JSONObject envelope,String action,String importance,String occasion,String inputChannel){
        JSONObject report=new JSONObject();
        try{
            if(!available||module==null)return report.put("valid",true).put("bypassed",true).put("reason","barbara_unavailable");
            PyObject channel=inputChannel==null?PyObject.fromJava(null):PyObject.fromJava(inputChannel);
            return new JSONObject(module.callAttr("validate_envelope_json",envelope==null?"{}":envelope.toString(),action==null?"":action,importance==null?"normal":importance,occasion==null?"continuation":occasion,channel).toString());
        }catch(Throwable failure){
            lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Validação Barbara indisponível; mantendo validações Java existentes.",failure);
            try{return report.put("valid",true).put("bypassed",true).put("reason","barbara_validation_error");}catch(Exception ignored){return new JSONObject();}
        }
    }

    public void markExperienced(String location,String imprint){
        if(prefs==null)return;
        try{
            JSONObject known=locationImprints();if(location!=null&&!location.trim().isEmpty())known.put(location,imprint==null?"":imprint);
            prefs.edit().putBoolean(KEY_STARTED,true).putString(KEY_LOCATIONS,known.toString()).apply();
        }catch(Exception failure){AppLog.w("BarbaraSupervisor","Não foi possível persistir descoberta Barbara.",failure);}
    }

    public JSONObject diagnostic(){
        JSONObject out=new JSONObject();
        try{out.put("available",available).put("embedded",true).put("last_error",lastError).put("campaign_started",prefs!=null&&prefs.getBoolean(KEY_STARTED,false)).put("known_locations",locationImprints().length());}
        catch(Exception ignored){AppLog.ignored("BarbaraSupervisor","Falha ao montar diagnóstico Barbara.",ignored);}
        return out;
    }
}
