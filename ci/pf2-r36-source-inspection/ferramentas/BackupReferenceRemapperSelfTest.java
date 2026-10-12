package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public final class BackupReferenceRemapperSelfTest {
    private static int checks=0;
    public static void main(String[] args)throws Exception{
        Map<Long,Long> characters=map(11,101),rolls=map(22,202),combatants=map(33,303),factions=map(44,404),events=map(55,505),clocks=map(66,606);
        Map<Long,Long> troupes=map(77,707),activities=map(88,808),quests=map(99,909),scenes=map(12,120),mapsById=map(13,130),routes=map(14,140),transactions=map(15,150);
        Map<String,String> stringIds=new HashMap<String,String>();stringIds.put("scene-old","scene-new");Map<String,String> effectKeys=new HashMap<String,String>();effectKeys.put("faction:44:step:1","faction:404:step:1");
        BackupReferenceRemapper.Maps maps=new BackupReferenceRemapper.Maps(characters,rolls,combatants,factions,events,clocks,troupes,activities,quests,scenes,mapsById,routes,transactions,stringIds,effectKeys);
        JSONObject source=new JSONObject().put("roll_id",22).put("actor_character_id",11).put("nested",new JSONObject()
                .put("combatant_id",33).put("faction_id",44).put("event_id",55).put("clock_id",66)
                .put("troupe_id",77).put("activity_id",88).put("quest_id",99).put("scene_id",12).put("map_id",13).put("route_id",14).put("transaction_id",15)
                .put("effect_key","faction:44:step:1").put("text_ref","activity:88 / roll:22 / scene-old")
                .put("list",new JSONArray().put(new JSONObject().put("character_id",11)).put("troupe:77")));
        JSONObject out=BackupReferenceRemapper.remapObject(source,maps);
        check(out.optLong("roll_id")==202,"roll_id remapeado");
        check(out.optLong("actor_character_id")==101,"ator remapeado");
        JSONObject nested=out.optJSONObject("nested");
        check(nested.optLong("combatant_id")==303,"combatente remapeado");
        check(nested.optLong("faction_id")==404,"facção remapeada");
        check(nested.optLong("event_id")==505,"evento remapeado");
        check(nested.optLong("clock_id")==606,"relógio remapeado");
        check(nested.optLong("troupe_id")==707&&nested.optLong("activity_id")==808&&nested.optLong("quest_id")==909,"Mundo Vivo remapeado");
        check(nested.optLong("scene_id")==120&&nested.optLong("map_id")==130&&nested.optLong("route_id")==140&&nested.optLong("transaction_id")==150,"cena, mapa, rota e transação remapeados");
        check("faction:404:step:1".equals(nested.optString("effect_key")),"effect_key remapeada sem colisão");
        check(nested.optString("text_ref").contains("activity:808")&&nested.optString("text_ref").contains("roll:202")&&nested.optString("text_ref").contains("scene-new"),"IDs inseridos em strings remapeados");
        check("troupe:707".equals(nested.optJSONArray("list").optString(1)),"ID em string de array remapeado");
        check(nested.optJSONArray("list").optJSONObject(0).optLong("character_id")==101,"ID em array remapeado");
        JSONObject missing=BackupReferenceRemapper.remapObject(new JSONObject().put("roll_id",999).put("character_id",999),maps);
        check(missing.optLong("roll_id")==0,"rolagem órfã neutralizada");
        check(missing.optLong("character_id")==0,"personagem órfão neutralizado");
        check("texto".equals(BackupReferenceRemapper.remapObject(new JSONObject().put("note","texto"),maps).optString("note")),"texto preservado");
        System.out.println("OK: "+checks+" verificações do remapeamento de backup.");
    }
    private static Map<Long,Long> map(long a,long b){Map<Long,Long> out=new HashMap<Long,Long>();out.put(a,b);return out;}
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
