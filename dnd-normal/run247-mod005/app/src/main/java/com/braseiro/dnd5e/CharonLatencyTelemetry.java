package com.braseiro.dnd5e;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Telemetria local, sem envio externo, do tempo até a primeira palavra Charon. */
public final class CharonLatencyTelemetry {
    private final SharedPreferences p;private long start=0,request=0,audio=0;
    public CharonLatencyTelemetry(Context c){p=c.getSharedPreferences("charon_latency_v5",Context.MODE_PRIVATE);}
    public synchronized void begin(){start=System.currentTimeMillis();request=0;audio=0;}
    public synchronized void request(){if(start==0)start=System.currentTimeMillis();request=System.currentTimeMillis();}
    public synchronized void firstAudio(){if(start==0)start=System.currentTimeMillis();audio=System.currentTimeMillis();}
    public synchronized void play(){long now=System.currentTimeMillis();if(start<=0)return;p.edit().putLong("last_total_ms",now-start).putLong("last_request_ms",request>0?request-start:-1).putLong("last_network_ms",audio>0&&request>0?audio-request:-1).putLong("last_audio_to_play_ms",audio>0?now-audio:-1).putLong("last_at",now).apply();start=request=audio=0;}
    public JSONObject snapshot(){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();try{o.put("last_total_ms",p.getLong("last_total_ms",-1)).put("text_to_request_ms",p.getLong("last_request_ms",-1)).put("request_to_audio_ms",p.getLong("last_network_ms",-1)).put("audio_to_play_ms",p.getLong("last_audio_to_play_ms",-1)).put("last_at",p.getLong("last_at",0));}catch(Exception ignored){AppLog.ignored("CharonLatencyTelemetry","Falha tolerada registrada na consolidação 5.0.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}
