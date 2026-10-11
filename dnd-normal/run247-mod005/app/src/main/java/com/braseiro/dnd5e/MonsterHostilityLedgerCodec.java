package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.Map;

/** Typed JSON codec for a directed hostility ledger, exercised on the JVM.
 * No allegiance inference. The database is responsible for resolving IDs in
 * the active world before storing or using them. */
public final class MonsterHostilityLedgerCodec {
    public static final String KEY="combat_hostility_v1";
    public static final String SCHEMA="monster_hostility_v1";
    private MonsterHostilityLedgerCodec() { }
    public static long[] decodeIds(long actorId,JSONArray entries) {
        if(entries==null||entries.length()>MonsterHostilityLedgerRules.MAX_HOSTILES)
            throw new IllegalArgumentException("invalid_hostility_entries");
        long[] ids=new long[entries.length()];
        for(int i=0;i<ids.length;i++){
            Object raw=entries.opt(i);
            if(!(raw instanceof Number)||raw instanceof Float||raw instanceof Double)
                throw new IllegalArgumentException("untyped_hostility_id");
            ids[i]=((Number)raw).longValue();
            if(!String.valueOf(raw).equals(String.valueOf(ids[i])))
                throw new IllegalArgumentException("invalid_hostility_integer");
        }
        return MonsterHostilityLedgerRules.canonical(actorId,ids);
    }
    public static long[] read(long actorId,String worldProfile,JSONObject resources) {
        if(resources==null||!resources.has(KEY))return new long[0];
        JSONObject ledger=resources.optJSONObject(KEY);
        if(ledger==null||!SCHEMA.equals(ledger.optString("schema"))||
            worldProfile==null||!worldProfile.equals(ledger.optString("world_profile"))||
            ledger.optLong("actor_id",0)!=actorId||ledger.optInt("revision",0)<1)
            throw new IllegalArgumentException("invalid_hostility_header");
        JSONArray entries=ledger.optJSONArray("hostile_ids");
        long[] raw=rawIds(actorId,entries);
        long[] canonical=MonsterHostilityLedgerRules.canonical(actorId,raw);
        if(!Arrays.equals(raw,canonical))
            throw new IllegalArgumentException("unsorted_hostility_ids");
        return canonical;
    }
    /** RUN231: imported SQLite combatant PKs change. Remap BOTH the ledger
     * owner and every declared hostile in the same backup transaction. Missing
     * references reject the import (no silent corruption or guessed allegiance). */
    public static JSONObject remapForRestore(long oldActorId,String profile,
        JSONObject resources,Map<Long,Long> idMap) {
        if(idMap==null||resources==null||!resources.has(KEY))return resources;
        long[] oldIds=read(oldActorId,profile,resources);
        Long newActor=idMap.get(oldActorId);
        if(newActor==null||newActor<=0)
            throw new IllegalArgumentException("unmapped_hostility_actor");
        long[] newIds=new long[oldIds.length];
        for(int i=0;i<oldIds.length;i++){
            Long mapped=idMap.get(oldIds[i]);
            if(mapped==null||mapped<=0)
                throw new IllegalArgumentException("unmapped_hostility_target");
            newIds[i]=mapped;
        }
        Arrays.sort(newIds);
        JSONObject oldLedger=resources.optJSONObject(KEY);
        JSONObject rewritten=build(newActor,profile,oldLedger.optInt("revision"),newIds);
        try{return new JSONObject(resources.toString()).put(KEY,rewritten);}
        catch(Exception invalid){throw new IllegalArgumentException("hostility_restore_serialization_failed",invalid);}
    }
    private static long[] rawIds(long actorId,JSONArray entries) {
        if(entries==null||entries.length()>MonsterHostilityLedgerRules.MAX_HOSTILES)
            throw new IllegalArgumentException("invalid_hostility_entries");
        long[] ids=new long[entries.length()];
        for(int i=0;i<ids.length;i++){
            Object raw=entries.opt(i);
            if(!(raw instanceof Number)||raw instanceof Float||raw instanceof Double)
                throw new IllegalArgumentException("untyped_hostility_id");
            ids[i]=((Number)raw).longValue();
            if(!String.valueOf(raw).equals(String.valueOf(ids[i])))
                throw new IllegalArgumentException("invalid_hostility_integer");
        }
        return ids;
    }
    public static JSONObject build(long actorId,String worldProfile,int revision,long[] canonicalIds) {
        if(worldProfile==null||worldProfile.trim().isEmpty()||revision<1)
            throw new IllegalArgumentException("invalid_hostility_header");
        long[] sorted=MonsterHostilityLedgerRules.canonical(actorId,canonicalIds);
        if(!Arrays.equals(sorted,canonicalIds))
            throw new IllegalArgumentException("unsorted_hostility_ids");
        JSONArray ids=new JSONArray();for(long id:sorted)ids.put(id);
        try{return new JSONObject().put("schema",SCHEMA).put("world_profile",worldProfile)
            .put("actor_id",actorId).put("revision",revision).put("hostile_ids",ids);}
        catch(Exception failure){throw new IllegalStateException("hostility_serialization_failed",failure);}
    }
}
