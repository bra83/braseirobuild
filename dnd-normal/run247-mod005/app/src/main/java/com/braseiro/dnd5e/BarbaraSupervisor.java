package com.braseiro.dnd5e;

import android.content.Context;
import android.app.ActivityManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import org.json.JSONObject;

/**
 * Ponte fail-closed para o Motor Barbara 1.1.3 vendorizado no APK.
 *
 * AppDb, motores D&D/Mystara, Atlas, dados físicos, combate e Mundo Vivo continuam
 * sendo autoridade. Barbara supervisiona política narrativa/integração e nunca
 * realiza uma segunda chamada Gemini nem grava uma segunda base canônica.
 */
public final class BarbaraSupervisor {
    private final Context context;
    private PyObject module;
    private volatile boolean available;
    private volatile boolean initialized;
    private String lastError="",motorVersion="";
    private JSONObject requiredContract=new JSONObject();
    private JSONObject deviceProfile=new JSONObject();

    // Constructing the Master on the Android main thread must NEVER start Chaquopy.
    // The Python engine is prepared after first paint, and plan() waits for it on the
    // existing background game-bus executor before any narration can be generated.
    public BarbaraSupervisor(Context ctx){context=ctx==null?null:ctx.getApplicationContext();}

    /** Non-blocking warm-up; never launched from Activity.onCreate on the UI thread. */
    public void warmUpAsync(){if(context!=null&&!initialized)AppExecutors.io().execute(new Runnable(){
        @Override public void run(){initialize();}
    });}

    private synchronized void initialize(){
        if(initialized)return;
        if(context==null){available=false;lastError="context_unavailable";initialized=true;return;}
        try{
            if(!Python.isStarted())Python.start(new AndroidPlatform(context));
            module=Python.getInstance().getModule("braseiro_barbara_adapter");
            JSONObject health=new JSONObject(module.callAttr("health_json").toString());
            motorVersion=health.optString("barbara_version","");
            requiredContract=health.optJSONObject("host_contract_required");if(requiredContract==null)requiredContract=new JSONObject();
            try{deviceProfile=new JSONObject(module.callAttr("configure_device_profile_json",collectDeviceMetrics().toString()).toString());}
            catch(Throwable profileFailure){deviceProfile=new JSONObject();AppLog.w("BarbaraSupervisor","Perfil de dispositivo 1.1.3 não pôde ser configurado; regras/estado permanecem inalterados.",profileFailure);}
            String contractProblem=BarbaraHostContractV1.compatibilityProblem(BarbaraHostContractV1.descriptor(),requiredContract);
            available=health.optBoolean("ok",false)&&"dnd".equals(health.optString("system_id"))&&contractProblem.length()==0;
            lastError=available?"":(contractProblem.length()>0?"host_contract_mismatch: "+contractProblem:"health_failed");
            if(!available&&contractProblem.length()>0)AppLog.w("BarbaraSupervisor","Motor Barbara recusado pelo contrato host: "+contractProblem,null);
        }catch(Throwable failure){
            available=false;lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Motor Barbara indisponível; validações Java permanecem ativas.",failure);
        }finally{initialized=true;}
    }


    private JSONObject collectDeviceMetrics(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            DisplayMetrics dm=context.getResources().getDisplayMetrics();
            int width=dm.widthPixels,height=dm.heightPixels;double refresh=60.0;
            try{DisplayManager manager=(DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);Display display=manager==null?null:manager.getDisplay(Display.DEFAULT_DISPLAY);if(display!=null){DisplayMetrics real=new DisplayMetrics();display.getRealMetrics(real);if(real.widthPixels>0)width=real.widthPixels;if(real.heightPixels>0)height=real.heightPixels;refresh=display.getRefreshRate();}}catch(Throwable ignored){AppLog.ignored("BarbaraDeviceProfile","DisplayMetrics avançadas indisponíveis.",ignored);}
            ActivityManager am=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();if(am!=null)am.getMemoryInfo(mi);
            out.put("manufacturer",Build.MANUFACTURER==null?"":Build.MANUFACTURER).put("model",Build.MODEL==null?"":Build.MODEL).put("sdkInt",Build.VERSION.SDK_INT)
                    .put("widthPx",width).put("heightPx",height).put("density",dm.density).put("densityDpi",dm.densityDpi).put("refreshHz",refresh)
                    .put("memoryMb",mi.totalMem/(1024L*1024L)).put("lowRam",am!=null&&am.isLowRamDevice())
                    .put("safeInsets",new JSONObject().put("left",0).put("top",0).put("right",0).put("bottom",0));
        }catch(Exception e){AppLog.w("BarbaraDeviceProfile","Falha ao coletar métricas de apresentação.",e);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public boolean isAvailable(){return initialized&&available;}
    private String liveContractProblem(){return BarbaraHostContractV1.compatibilityProblem(BarbaraHostContractV1.descriptor(),requiredContract);}
    private String systemId(String requested){return "mystara".equalsIgnoreCase(requested)?"mystara":"dnd";}

    public JSONObject systemContract(String requestedSystem){/*BR_JSON_WRAP*/try{
        JSONObject fallback=new JSONObject();String sid=systemId(requestedSystem);
        try{
            fallback.put("available",false).put("system_id",sid);
            String cp=liveContractProblem();if(cp.length()>0)return fallback.put("host_contract_problem",cp);
            if(!available||module==null)return fallback;
            JSONObject out=new JSONObject(module.callAttr("system_contract_json",sid).toString());
            JSONObject req=out.optJSONObject("host_contract_required");if(req!=null){String p=BarbaraHostContractV1.compatibilityProblem(BarbaraHostContractV1.descriptor(),req);if(p.length()>0)return fallback.put("host_contract_problem",p);}
            return out.put("available",true);
        }catch(Throwable failure){
            lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Falha ao ler contrato de sistema do Barbara.",failure);
            try{return fallback.put("error","system_contract_error");}catch(Exception ignored){return new JSONObject();}
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject validateState(JSONObject hostState,String requestedSystem){/*BR_JSON_WRAP*/try{
        JSONObject report=new JSONObject();String sid=systemId(requestedSystem);
        try{
            String cp=liveContractProblem();if(cp.length()>0)return report.put("valid",false).put("reason","host_contract_invalid").put("detail",cp);
            if(!available||module==null)return report.put("valid",true).put("bypassed",true).put("reason","barbara_unavailable").put("system_id",sid);
            JSONObject out=new JSONObject(module.callAttr("state_adapter_json",hostState==null?"{}":hostState.toString(),sid).toString());
            JSONObject req=out.optJSONObject("host_contract_required");if(req!=null){String p=BarbaraHostContractV1.compatibilityProblem(BarbaraHostContractV1.descriptor(),req);if(p.length()>0)return report.put("valid",false).put("reason","host_contract_changed").put("detail",p);}
            return out;
        }catch(Throwable failure){
            lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Falha na projeção CampaignState; host preservado.",failure);
            try{return report.put("valid",false).put("reason","state_adapter_error").put("system_id",sid);}catch(Exception ignored){return new JSONObject();}
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject plan(String action,String importance,boolean narrative,boolean nonAdvancing,String occasion){
        return plan(action,importance,narrative,nonAdvancing,occasion,null,"dnd");
    }
    public JSONObject plan(String action,String importance,boolean narrative,boolean nonAdvancing,String occasion,String inputChannel,String requestedSystem){/*BR_JSON_WRAP*/try{
        // MainActivity prepares the game bus with AppExecutors.io(), never the UI.
        // Do not silently drop Barbara policy while its prewarm is still pending.
        initialize();
        String storyOccasion=occasion==null||occasion.trim().isEmpty()?"continuation":occasion.trim();String sid=systemId(requestedSystem);JSONObject fallback=new JSONObject();
        try{
            fallback.put("available",available).put("story_occasion",storyOccasion).put("system_id",sid).put("input_channel",inputChannel==null?"":inputChannel).put("host_world_authority",true).put("duplicate_gemini_call",false).put("fallback_java_policy",true).put("host_contract",BarbaraHostContractV1.descriptor());
            String cp=liveContractProblem();if(cp.length()>0){available=false;lastError="host_contract_mismatch: "+cp;return fallback.put("available",false).put("host_contract_problem",cp);}
            if(!available||module==null)return fallback;
            JSONObject out=new JSONObject(module.callAttr("plan_json",action==null?"":action,importance==null?"normal":importance,narrative,nonAdvancing,storyOccasion,inputChannel,sid).toString());
            JSONObject remoteRequired=out.optJSONObject("host_contract_required");if(remoteRequired!=null){String dynamic=BarbaraHostContractV1.compatibilityProblem(BarbaraHostContractV1.descriptor(),remoteRequired);if(dynamic.length()>0){available=false;lastError="dynamic_contract_mismatch: "+dynamic;return fallback.put("available",false).put("host_contract_problem",dynamic);}}
            out.put("available",true).put("story_occasion",storyOccasion).put("fallback_java_policy",false).put("host_contract",BarbaraHostContractV1.descriptor());return out;
        }catch(Throwable failure){
            available=false;lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Falha ao montar plano Barbara; política Java equivalente foi preservada.",failure);return fallback;
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject validate(JSONObject envelope,String action,String importance,String occasion){return validate(envelope,action,importance,occasion,null,"dnd");}
    public JSONObject validate(JSONObject envelope,String action,String importance,String occasion,String inputChannel,String requestedSystem){/*BR_JSON_WRAP*/try{
        JSONObject report=new JSONObject();String sid=systemId(requestedSystem);
        try{
            String hostProblem=liveContractProblem();if(hostProblem.length()>0)return report.put("valid",false).put("bypassed",false).put("reason","host_contract_invalid").put("detail",hostProblem);
            if(!available||module==null)return report.put("valid",true).put("bypassed",true).put("reason","barbara_unavailable_java_guard_required").put("host_contract",BarbaraHostContractV1.descriptor());
            JSONObject remote=new JSONObject(module.callAttr("validate_envelope_json",envelope==null?"{}":envelope.toString(),action==null?"":action,importance==null?"normal":importance,occasion==null?"continuation":occasion,inputChannel,sid).toString());
            JSONObject req=remote.optJSONObject("host_contract_required");if(req!=null){String p=BarbaraHostContractV1.compatibilityProblem(BarbaraHostContractV1.descriptor(),req);if(p.length()>0)return report.put("valid",false).put("bypassed",false).put("reason","host_contract_changed").put("detail",p);}return remote;
        }catch(Throwable failure){
            lastError=failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage());
            AppLog.w("BarbaraSupervisor","Validação Barbara indisponível; o guarda Java continua obrigatório.",failure);
            try{return report.put("valid",true).put("bypassed",true).put("reason","barbara_validation_error_java_guard_required");}catch(Exception ignored){return new JSONObject();}
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject diagnostic(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{out.put("available",available).put("initialized",initialized).put("embedded",true).put("last_error",lastError).put("barbara_version",motorVersion).put("state_authority","sqlite_host").put("duplicate_gemini_call",false).put("host_contract",BarbaraHostContractV1.descriptor()).put("required_contract",requiredContract).put("device_profile",deviceProfile).put("contract_problem",liveContractProblem()).put("dnd_system_module",systemContract("dnd")).put("mystara_system_module",systemContract("mystara"));}
        catch(Exception ignored){AppLog.ignored("BarbaraSupervisor","Falha ao montar diagnóstico Barbara.",ignored);}return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}
