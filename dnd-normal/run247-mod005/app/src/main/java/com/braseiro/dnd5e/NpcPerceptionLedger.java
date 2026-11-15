package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Perceived != registered. Uses the existing narrative_discovery_state SQLite
 * ledger, scoped by the active world profile and observer; no schema migration.
 * A name enters the observer's public knowledge only when an ACCEPTED narration
 * actually says the name, or an explicit observation establishes it.
 */
public final class NpcPerceptionLedger {
    private NpcPerceptionLedger(){}
    private static String key(String observer,JSONObject npc){
        String stable=npc.optString("id",npc.optString("name","")).trim();
        return (observer==null?"party":observer.trim())+"|"+stable.toLowerCase(Locale.ROOT);
    }

    public static JSONArray publicOnstage(AppDb db,JSONObject scene,String acceptedNarration,String observer){
        JSONArray visible=new JSONArray(),raw=FictionTurnIntent.present(scene);
        for(int i=0;i<raw.length();i++)try{
            JSONObject npc=raw.optJSONObject(i);
            if(npc==null||npc.optBoolean("departed",false)||npc.optBoolean("left_scene",false)
                    ||npc.optInt("active",1)==0)continue;
            String name=npc.optString("name","").trim();
            if(name.isEmpty())continue;
            JSONObject record=db.getNarrativeDiscovery("npc_observed",key(observer,npc));
            boolean introduced=record.length()>0
                ||(acceptedNarration!=null && acceptedNarration.toLowerCase(Locale.ROOT)
                        .contains(name.toLowerCase(Locale.ROOT)));
            JSONObject pub=new JSONObject()
                .put("stable_ref",key(observer,npc))
                .put("is_identified_by_observer",introduced)
                .put("name_known_to_observer",introduced?name:"")
                .put("role",npc.optString("role",""))
                .put("visible_description",npc.optString("visual_description",record.optString("imprint","")))
                .put("visible_clothing",npc.optString("clothing",""))
                .put("visible_equipment",npc.optString("visible_equipment",""))
                .put("visible_weapons",npc.optString("visible_weapons",""))
                .put("visibility_of_weapons_established",npc.has("visible_weapons"))
                .put("identified_source",record.length()>0?"previously_accepted":(introduced?"accepted_current_scene":"not_yet_presented"));
            visible.put(pub);
        }catch(Exception ignored){}
        return visible;
    }

    public static void commitAcceptedPresentation(AppDb db,JSONObject scene,String narration,String observer){
        if(db==null||narration==null||narration.trim().isEmpty())return;
        JSONArray raw=FictionTurnIntent.present(scene);
        for(int i=0;i<raw.length();i++){
            JSONObject npc=raw.optJSONObject(i);if(npc==null||npc.optBoolean("anonymous",false)
                    ||npc.optBoolean("departed",false)||npc.optBoolean("left_scene",false))continue;
            String name=npc.optString("name","").trim();
            if(name.isEmpty()||!narration.toLowerCase(Locale.ROOT)
                    .contains(name.toLowerCase(Locale.ROOT)))continue;
            String k=key(observer,npc);
            JSONObject previous=db.getNarrativeDiscovery("npc_observed",k);
            if(previous.length()>0)continue; // one first-presentation receipt, not a tick per mention
            String imprint=npc.optString("visual_description","").trim();
            db.recordNarrativeDiscovery("npc_observed",k,name,imprint,
                    Math.max(1,scene.optInt("day_number",1)));
        }
    }

    public static String promptPolicy(){
        return "VISIBILIDADE POR OBSERVADOR: NPC cadastrado NÃO é NPC apresentado. " +
          "Use somente onstage_perception para aparências e nomes conhecidos. " +
          "Se name_known_to_observer estiver vazio, descreva a pessoa sem revelar seu nome. " +
          "Se visibility_of_weapons_established=false, não afirme que está desarmado: " +
          "diga o que consegue ou não consegue distinguir. Armas ocultas, inventários, " +
          "segredos, relações e pensamentos não são perceptíveis gratuitamente. " +
          "Aparência e roupas já estabelecidas não podem mudar sem causa registrada. ";
    }
}
