package com.braseiro.dnd5e;

import org.json.JSONObject;

/**
 * Sincronização offline opcional do Mundo Vivo. Converte tempo real em tempo
 * de campanha sem chamar IA e reutiliza o mesmo WorldStateEngine transacional.
 */
public final class LivingWorldCatchUpEngine {
    private final AppDb db;
    private final LivingWorldEngine living;
    private final WorldStateEngine world;
    public LivingWorldCatchUpEngine(AppDb database){db=database;living=new LivingWorldEngine(database);world=new WorldStateEngine(database);}

    public JSONObject preview(long now){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();try{
            JSONObject settings=living.state();
            boolean enabled=settings.optInt("enabled",1)==1&&settings.optInt("background_simulation_enabled",0)==1;
            long last=settings.optLong("last_background_sync_at",0L);
            double rate=settings.optDouble("background_game_hours_per_real_day",24d);
            int max=settings.optInt("background_max_hours",72);
            boolean started=enabled&&db.getStoryState().optInt("started",0)==1;
            out=LivingWorldCatchUpRules.preview(enabled,started,last,now,rate,max);
            int gameHours=out.optInt("game_hours",0);
            if(gameHours>0)out.put("world_tick",world.preview("MUNDO_VIVO_TEMPO_REAL: "+gameHours+" horas"));
        }catch(Exception e){try{out.put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){AppLog.ignored("LivingWorldCatchUp","Falha ao registrar erro.",ignored);}}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized boolean commit(JSONObject preview){/*BR_JSON_WRAP*/try{
        if(preview==null||!preview.optBoolean("preview_only",false))return false;
        try{
            if(!preview.optBoolean("enabled",false)||preview.optBoolean("initialize_only",false)||preview.optInt("game_hours",0)<=0){living.markBackgroundSyncNow();preview.put("preview_only",false).put("committed",true);return true;}
            JSONObject tick=preview.optJSONObject("world_tick");if(tick==null||!world.commit(tick))return false;
            living.markBackgroundSyncNow();preview.put("preview_only",false).put("committed",true);
            db.addAuditEvent("mundo_vivo","Sincronização offline",preview.optInt("game_hours")+" hora(s) de campanha processadas","{}",preview.toString(),true);return true;
        }catch(Exception e){AppLog.e("LivingWorldCatchUp","Falha na sincronização offline.",e);return false;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}
