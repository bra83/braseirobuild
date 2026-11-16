package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Opt-in local trace, never uploaded. No API key, bearer token, voice audio,
 * password or keystore material may be written. Debug state is best effort:
 * failures must never stop a game turn or hold the UI/AI queues.
 */
public final class FictionTurnTrace {
    public static final String PREF_ENABLED="fiction_turn_trace_opt_in";
    private static final long MAX_BYTES=512L*1024L;
    private static final Object FILE_LOCK=new Object();
    private static final Pattern API_KEY=Pattern.compile("AIza[0-9A-Za-z_-]{15,}");
    private static final Pattern BEARER=Pattern.compile("(?i)Bearer\\s+[a-z0-9._~+/=-]{8,}");
    private static final Pattern JSON_SECRET=Pattern.compile(
        "(?i)(\"(?:api[_-]?key|secret|password|access[_-]?token|refresh[_-]?token|authorization)\"\\s*:\\s*\")[^\"]*(\")");
    private FictionTurnTrace(){}

    public static boolean isEnabled(SharedPreferences preferences){
        return preferences!=null&&preferences.getBoolean(PREF_ENABLED,false);
    }
    public static File destination(Context context){
        return new File(context.getFilesDir(),"braseiro_immersive_turn_diagnostics.jsonl");
    }
    public static void setEnabled(final Context context,final SharedPreferences preferences,boolean on){
        if(preferences==null)return;
        synchronized(FILE_LOCK){
            preferences.edit().putBoolean(PREF_ENABLED,on).apply();
            if(!on&&context!=null){
                try{File file=destination(context);if(file.exists()&&!file.delete())
                    AppLog.w("FictionTrace","Could not delete disabled private trace",null);}
                catch(Exception ignored){}
            }
        }
    }
    public static String redact(String raw){
        if(raw==null)return "";
        String safe=JSON_SECRET.matcher(raw).replaceAll("$1[REDACTED]$2");
        safe=BEARER.matcher(safe).replaceAll("Bearer [REDACTED]");
        safe=API_KEY.matcher(safe).replaceAll("[REDACTED_API_KEY]");
        return safe;
    }
    public static void append(final Context context,final SharedPreferences preferences,
                              final String phase,final JSONObject values){
        if(context==null||!isEnabled(preferences))return;
        final Context app=context.getApplicationContext();
        final String line;
        try{
            JSONObject event=new JSONObject();
            event.put("schema",1).put("local_only",true)
                .put("at",System.currentTimeMillis())
                .put("phase",phase==null?"":phase);
            if(values!=null)event.put("data",values);
            String sanitized=redact(event.toString());
            if(sanitized.length()>12000){
                sanitized=new JSONObject().put("schema",1).put("local_only",true)
                    .put("phase",phase==null?"":phase).put("oversize_event_dropped",true)
                    .put("original_length",sanitized.length()).toString();
            }
            line=sanitized+"\n";
        }catch(Exception ignored){return;}
        AppExecutors.serial().execute(new Runnable(){public void run(){
            synchronized(FILE_LOCK){
            if(!isEnabled(preferences))return; // opt-out wins over queued writes
            try{
                File f=destination(app);
                // Rotate to a bounded, private file. Diagnostic never takes
                // authority over gameplay persistence or game state.
                if(f.exists()&&f.length()+line.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES){
                    if(!f.delete())return;
                }
                try(FileOutputStream stream=new FileOutputStream(f,true)){
                    stream.write(line.getBytes(StandardCharsets.UTF_8));
                }
            }catch(Exception e){AppLog.e("FictionTrace","Nonfatal local trace write error",e);}
            }
        }});
    }
}
