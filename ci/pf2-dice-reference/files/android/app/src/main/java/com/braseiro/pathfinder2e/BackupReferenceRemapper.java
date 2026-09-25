package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reescreve referências internas depois que um backup é importado.
 *
 * IDs SQLite são globais no arquivo e não podem ser preservados com segurança
 * quando apenas uma campanha é substituída. Por isso, todas as referências
 * conhecidas são remapeadas explicitamente; uma referência antiga sem destino
 * válido vira zero em vez de apontar acidentalmente para outro registro.
 */
public final class BackupReferenceRemapper {
    public static final class Maps {
        public final Map<Long,Long> characters;
        public final Map<Long,Long> rolls;
        public final Map<Long,Long> combatants;
        public final Map<Long,Long> factions;
        public final Map<Long,Long> events;
        public final Map<Long,Long> clocks;
        public final Map<Long,Long> troupes;
        public final Map<Long,Long> activities;
        public final Map<Long,Long> quests;
        public final Map<Long,Long> scenes;
        public final Map<Long,Long> maps;
        public final Map<Long,Long> routes;
        public final Map<Long,Long> transactions;
        public final Map<String,String> stringIds;
        public final Map<String,String> effectKeys;

        public Maps(Map<Long,Long> characters,Map<Long,Long> rolls,Map<Long,Long> combatants,
                    Map<Long,Long> factions,Map<Long,Long> events,Map<Long,Long> clocks){
            this(characters,rolls,combatants,factions,events,clocks,null,null,null,null,null,null,null,null,null);
        }
        public Maps(Map<Long,Long> characters,Map<Long,Long> rolls,Map<Long,Long> combatants,
                    Map<Long,Long> factions,Map<Long,Long> events,Map<Long,Long> clocks,
                    Map<Long,Long> troupes,Map<Long,Long> activities,Map<Long,Long> quests,
                    Map<Long,Long> scenes,Map<Long,Long> maps,Map<Long,Long> routes,Map<Long,Long> transactions,
                    Map<String,String> stringIds,Map<String,String> effectKeys){
            this.characters=safe(characters);this.rolls=safe(rolls);this.combatants=safe(combatants);
            this.factions=safe(factions);this.events=safe(events);this.clocks=safe(clocks);
            this.troupes=safe(troupes);this.activities=safe(activities);this.quests=safe(quests);this.scenes=safe(scenes);this.maps=safe(maps);this.routes=safe(routes);this.transactions=safe(transactions);
            this.stringIds=safeStrings(stringIds);this.effectKeys=safeStrings(effectKeys);
        }
        private static Map<Long,Long> safe(Map<Long,Long> source){return source==null?Collections.<Long,Long>emptyMap():source;}
        private static Map<String,String> safeStrings(Map<String,String> source){return source==null?Collections.<String,String>emptyMap():source;}
    }

    private static final Pattern EMBEDDED_ID=Pattern.compile("(?i)\\b(character|actor|roll|combatant|faction|event|clock|troupe|activity|quest|scene|map|route|transaction):([0-9]+)\\b");

    private BackupReferenceRemapper(){}

    public static JSONObject remapObject(JSONObject source,Maps maps)throws Exception{
        JSONObject out=new JSONObject();if(source==null)return out;Iterator<String> keys=source.keys();
        while(keys.hasNext()){
            String key=keys.next();Object value=source.opt(key);
            if(value instanceof JSONObject)out.put(key,remapObject((JSONObject)value,maps));
            else if(value instanceof JSONArray)out.put(key,remapArray((JSONArray)value,maps));
            else if(isIdKey(key)&&value instanceof Number)out.put(key,remapId(key,((Number)value).longValue(),maps));
            else if(isIdKey(key)&&value instanceof String&&((String)value).matches("\\d+"))out.put(key,remapId(key,Long.parseLong((String)value),maps));
            else if(value instanceof String)out.put(key,remapString(key,(String)value,maps));
            else out.put(key,value==null?JSONObject.NULL:value);
        }
        return out;
    }

    public static JSONArray remapArray(JSONArray source,Maps maps)throws Exception{
        JSONArray out=new JSONArray();if(source==null)return out;
        for(int i=0;i<source.length();i++){
            Object value=source.opt(i);
            if(value instanceof JSONObject)out.put(remapObject((JSONObject)value,maps));
            else if(value instanceof JSONArray)out.put(remapArray((JSONArray)value,maps));
            else if(value instanceof String)out.put(remapString("",(String)value,maps));
            else out.put(value==null?JSONObject.NULL:value);
        }
        return out;
    }

    public static long remapId(String key,long oldId,Maps maps){
        if(oldId<=0)return 0L;Map<Long,Long> selected=mapFor(key,maps);Long value=selected.get(oldId);return value==null?0L:value.longValue();
    }

    private static boolean isIdKey(String key){
        return "roll_id".equals(key)||"actor_character_id".equals(key)||"character_id".equals(key)||"owner_character_id".equals(key)
                ||"related_character_id".equals(key)||"combatant_id".equals(key)||"faction_id".equals(key)||"event_id".equals(key)||"clock_id".equals(key)
                ||"troupe_id".equals(key)||"activity_id".equals(key)||"quest_id".equals(key)||"scene_id".equals(key)||"map_id".equals(key)||"route_id".equals(key)||"transaction_id".equals(key);
    }

    private static Map<Long,Long> mapFor(String key,Maps maps){
        if(maps==null)return Collections.emptyMap();
        if("roll_id".equals(key))return maps.rolls;
        if("combatant_id".equals(key))return maps.combatants;
        if("faction_id".equals(key))return maps.factions;
        if("event_id".equals(key))return maps.events;
        if("clock_id".equals(key))return maps.clocks;
        if("troupe_id".equals(key))return maps.troupes;
        if("activity_id".equals(key))return maps.activities;
        if("quest_id".equals(key))return maps.quests;
        if("scene_id".equals(key))return maps.scenes;
        if("map_id".equals(key))return maps.maps;
        if("route_id".equals(key))return maps.routes;
        if("transaction_id".equals(key))return maps.transactions;
        return maps.characters;
    }

    public static String remapString(String key,String value,Maps maps){
        if(value==null||maps==null)return value==null?"":value;String exact=maps.stringIds.get(value);if(exact!=null)value=exact;
        boolean exactEffect=false;if("effect_key".equals(key)||"receipt_key".equals(key)){String mapped=maps.effectKeys.get(value);if(mapped!=null){value=mapped;exactEffect=true;}}
        String result=value;if(!exactEffect){Matcher matcher=EMBEDDED_ID.matcher(value);StringBuffer out=new StringBuffer();
        while(matcher.find()){
            String prefix=matcher.group(1),idKey=prefixKey(prefix);long oldId=Long.parseLong(matcher.group(2)),newId=remapId(idKey,oldId,maps);
            matcher.appendReplacement(out,Matcher.quoteReplacement(prefix+":"+newId));
        }
        matcher.appendTail(out);result=out.toString();}
        for(Map.Entry<String,String> entry:maps.stringIds.entrySet())if(entry.getKey()!=null&&entry.getKey().length()>=8&&result.contains(entry.getKey()))result=result.replace(entry.getKey(),entry.getValue());
        for(Map.Entry<String,String> entry:maps.effectKeys.entrySet())if(entry.getKey()!=null&&entry.getKey().length()>0&&result.contains(entry.getKey()))result=result.replace(entry.getKey(),entry.getValue());
        return result;
    }
    private static String prefixKey(String prefix){String p=prefix.toLowerCase();if("actor".equals(p)||"character".equals(p))return "character_id";return p+"_id";}

    /** Utilitário de teste e de chamadas que ainda não têm todos os mapas. */
    public static Maps maps(Map<Long,Long> characters,Map<Long,Long> rolls){
        return new Maps(characters,rolls,new HashMap<Long,Long>(),new HashMap<Long,Long>(),new HashMap<Long,Long>(),new HashMap<Long,Long>());
    }
}
