package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Recovery-only snapshot for the explicitly selected local Master.
 *
 * This path MUST NOT call MasterOrchestrator.prepare, RAG, Gemini or mutable
 * world-tick previews. It reads committed SQLite facts and builds a small
 * read-only context to preserve the scene after failed remote preparation.
 * No action is applied here; the normal validation/commit gate owns that.
 */
public final class OfflineMasterRecoveryContext {
    private OfflineMasterRecoveryContext() {}

    public static MasterOrchestrator.Plan fromSaved(AppDb db,String action,String channel) throws Exception {
        if(db==null)throw new IllegalArgumentException("Banco de campanha indisponível.");
        JSONObject scene=db.getSceneState(),story=db.getStoryState();
        if(story.optInt("started",0)!=1 || story.optString("last_narration","").trim().isEmpty())
            throw new IllegalStateException("Não existe uma cena confirmada para recuperar.");
        if(scene.optString("location","").trim().isEmpty())
            throw new IllegalStateException("Local da cena não foi confirmado. Nenhuma posição será inventada.");
        JSONObject bus=new JSONObject()
            .put("world_profile",db.getActiveMapProfile())
            .put("active_heroes_full",new JSONArray(db.listActiveCharacters()))
            .put("npcs",db.listSceneNpcs(12))
            .put("scene",scene)
            .put("story",story);
        return fromSnapshot(scene,story,bus,action,channel);
    }

    /** Pure constructor for source-driven regression tests. */
    static MasterOrchestrator.Plan fromSnapshot(JSONObject scene,JSONObject story,
            JSONObject bus,String action,String channel) throws Exception {
        if(scene==null||story==null||bus==null)
            throw new IllegalArgumentException("Estado salvo incompleto.");
        if(scene.optString("location","").trim().isEmpty())
            throw new IllegalArgumentException("Local da cena ausente.");
        JSONObject minimal=new JSONObject()
            .put("scene",new JSONObject(scene.toString()))
            .put("story",new JSONObject(story.toString()))
            .put("game_bus",new JSONObject(bus.toString()))
            .put("rules_frame",new JSONObject()
                .put("rule_retrieval_status","bypassed_for_explicit_offline_recovery")
                .put("suggested_check",JSONObject.NULL)
                .put("mechanics_authority","host_only")
                .put("test_policy","Sem procedimento verificado: não declarar sucesso, falha, dano ou recompensa."))
            .put("offline_recovery",new JSONObject()
                .put("read_only_snapshot",true)
                .put("original_scene_retained",true)
                .put("rule_search_skipped",true)
                .put("external_ai_skipped",true));
        return MasterOrchestrator.Plan.forTest(
            action==null?"":action,minimal.toString(),"continuation",
            channel==null||channel.trim().isEmpty()?"PLAYER_ACTION":channel);
    }
}
