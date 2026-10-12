package com.braseiro.pathfinder2e;
import org.json.JSONObject;
/** Stub transacional usado apenas pelo teste JVM do motor de eventos. */
public final class GuildEngine {
    private final AppDb db;
    public GuildEngine(AppDb database){db=database;}
    public void applyUpdates(JSONObject envelope){
        if(envelope!=null && envelope.optJSONArray("guild_updates")!=null && envelope.optJSONArray("guild_updates").length()>0)
            db.effects.add("guild:"+envelope.optJSONArray("guild_updates").length());
    }
}
