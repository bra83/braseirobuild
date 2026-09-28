package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Memória estruturada de cena e NPCs. O texto narrativo continua sendo exibido,
 * mas o estado persistente impede que clima, local, ameaças, pistas e relações
 * sejam reinventados a cada resposta.
 */
public final class SceneStateEngine {
    private final AppDb db;

    public SceneStateEngine(AppDb db) { this.db = db; }

    public JSONObject apply(JSONObject envelope, String narration, JSONObject mapState) throws Exception {
        JSONObject incoming = envelope == null ? null : envelope.optJSONObject("scene_state");
        JSONObject current = db.getSceneState();
        try {
            JSONObject merged = new JSONObject(current.toString());
            if (incoming != null) {
                copyNonEmpty(incoming, merged,
                        "location", "hex_id", "time_of_day", "clock_hour", "weather", "lighting",
                        "danger", "objective", "present_npcs", "threats", "clues", "pending_consequences");
                // O modelo às vezes devolve o exemplo do contrato (dia 1). O relógio da
                // campanha nunca pode andar para trás por causa disso.
                int currentDay = Math.max(1, current.optInt("day_number", 1));
                int incomingDay = Math.max(1, incoming.optInt("day_number", currentDay));
                merged.put("day_number", Math.max(currentDay, incomingDay));
            }
            if (mapState != null && mapState.optString("current_hex_id").length() > 0)
                merged.put("hex_id", mapState.optString("current_hex_id"));
            infer(merged, narration);
            SceneTemporalResolver.Result temporal=SceneTemporalResolver.resolve(
                    current.optString("time_of_day","Manhã"),current.optDouble("clock_hour",9d),current.has("clock_hour"),
                    incoming==null?"":incoming.optString("time_of_day"),incoming==null?0d:incoming.optDouble("clock_hour",0d),incoming!=null&&incoming.has("clock_hour"),
                    narration,merged.optString("lighting",""));
            merged.put("clock_hour",temporal.hour);merged.put("time_of_day",temporal.label);merged.put("lighting",temporal.lighting);
            JSONArray npcs = envelope == null ? null : envelope.optJSONArray("npc_updates");
            mergeNpcUpdatesIntoPresence(merged,npcs);
            db.saveSceneStateChecked(merged);
            synchronizePresentNpcs(merged);
            db.addAuditEvent("cena", "Estado de cena atualizado",
                    merged.optString("location") + " • " + merged.optString("weather"),
                    current.toString(), merged.toString(), true);
            return merged;
        } catch (Exception e) {
            throw new Exception("Falha ao persistir o estado integrado da cena.",e);
        }
    }


    /**
     * Um NPC atualizado na cena deve aparecer no elenco mesmo quando o modelo
     * esquece de repeti-lo em scene_state.present_npcs. Uma saída explícita é
     * a única situação que remove o NPC automaticamente.
     */
    private void mergeNpcUpdatesIntoPresence(JSONObject scene,JSONArray updates) throws Exception {
        JSONArray present=presentArray(scene);
        if(updates!=null)for(int i=0;i<updates.length();i++){
            JSONObject npc=updates.optJSONObject(i);if(npc==null)continue;
            String name=GeneratedTextSanitizer.scalar(npc.optString("name"),"name");if(name.length()==0||SceneEntityGrounding.isNonNpcIdentity(db,scene,name))continue;
            boolean departed=npc.optInt("active",1)==0||npc.optBoolean("left_scene",false)||npc.optBoolean("departed",false)||!npc.optBoolean("present",true)||isDepartureStatus(npc.optString("status"));
            removePresentByName(present,name);
            JSONObject linked=new JSONObject(npc.toString());linked.put("name",name);
            if(!linked.has("last_seen")||linked.optString("last_seen").trim().length()==0)linked.put("last_seen",scene.optString("location","Cena atual"));
            if(!linked.has("hex_id")||linked.optString("hex_id").trim().length()==0)linked.put("hex_id",scene.optString("hex_id",""));
            linked.put("active",departed?0:1);db.upsertNpcMemoryChecked(linked);
            if(!departed){JSONObject visible=new JSONObject().put("name",name);copyVisibleNpcField(npc,visible,"token_uri");copyVisibleNpcField(npc,visible,"role");copyVisibleNpcField(npc,visible,"summary");copyVisibleNpcField(npc,visible,"visual_description");present.put(visible);}
        }
        scene.put("present_npcs",present);
    }

    private JSONArray presentArray(JSONObject scene){
        Object raw=scene.opt("present_npcs");if(raw instanceof JSONArray)return copyArray((JSONArray)raw);
        try{return new JSONArray(scene.optString("present_npcs_json","[]"));}catch(Exception ignored){return new JSONArray();}
    }

    private static JSONArray copyArray(JSONArray source){try{return new JSONArray(source.toString());}catch(Exception ignored){return new JSONArray();}}
    private static void copyVisibleNpcField(JSONObject from,JSONObject to,String key)throws Exception{String value=from.optString(key).trim();if(value.length()>0)to.put(key,value);}
    private static void removePresentByName(JSONArray present,String name)throws Exception{for(int i=present.length()-1;i>=0;i--){Object value=present.opt(i);String candidate=value instanceof JSONObject?((JSONObject)value).optString("name",((JSONObject)value).optString("label")):String.valueOf(value);if(name.equalsIgnoreCase(candidate.trim()))present.remove(i);}}
    private static boolean isDepartureStatus(String raw){String value=normalize(raw);return value.contains("saiu")||value.contains("partiu")||value.contains("deixou a cena")||value.contains("ausente")||value.contains("departed")||value.contains("left scene");}


    /**
     * Liga o elenco visível da cena à memória persistente. Objetos completos
     * podem criar/atualizar NPCs; nomes simples apenas registram a aparição e
     * preservam ficha, segredos e token já existentes.
     */
    private void synchronizePresentNpcs(JSONObject scene) throws Exception {
        Object raw=scene.opt("present_npcs");
        JSONArray present=raw instanceof JSONArray?(JSONArray)raw:new JSONArray(scene.optString("present_npcs_json","[]"));
        for(int i=0;i<present.length();i++){
            Object value=present.opt(i);JSONObject update=new JSONObject();
            if(value instanceof JSONObject)update=new JSONObject(value.toString());
            else if(value!=null&&String.valueOf(value).trim().length()>0)update.put("name",String.valueOf(value).trim());
            String name=update.optString("name").trim();if(name.length()==0||SceneEntityGrounding.isNonNpcIdentity(db,scene,name))continue;
            if(update.optBoolean("transient",false)||update.optBoolean("anonymous",false))continue;
            update.put("last_seen",scene.optString("location","Cena atual"));
            update.put("hex_id",scene.optString("hex_id",""));
            update.put("active",1);
            db.upsertNpcMemoryChecked(update);
        }
    }

    public String contextSummary() {
        JSONObject s = db.getSceneState();
        StringBuilder b = new StringBuilder();
        append(b, "Dia", String.valueOf(Math.max(1, s.optInt("day_number", 1))));
        append(b, "Local", s.optString("location"));
        append(b, "Hex", s.optString("hex_id"));
        append(b, "Horário", s.optString("time_of_day"));
        append(b, "Clima", s.optString("weather"));
        append(b, "Iluminação", s.optString("lighting"));
        append(b, "Perigo", s.optString("danger"));
        append(b, "Objetivo", s.optString("objective"));
        append(b, "NPCs presentes", jsonText(s.optString("present_npcs_json", "[]")));
        append(b, "Ameaças", jsonText(s.optString("threats_json", "[]")));
        append(b, "Pistas", jsonText(s.optString("clues_json", "[]")));
        append(b, "Consequências pendentes", jsonText(s.optString("pending_consequences_json", "[]")));
        JSONArray memories = db.listNpcMemories(12);
        if (memories.length() > 0) {
            b.append("MEMÓRIA DE NPCS:\n");
            for (int i = 0; i < memories.length(); i++) {
                JSONObject n = memories.optJSONObject(i);
                b.append("• ").append(n.optString("name"))
                        .append(": atitude ").append(n.optString("attitude", "neutra"))
                        .append("; relação ").append(n.optString("relationship", "ainda indefinida"));
                if (n.optString("goals").length() > 0) b.append("; objetivo ").append(n.optString("goals"));
                if (n.optString("debts").length() > 0) b.append("; dívida/promessa ").append(n.optString("debts"));
                b.append(".\n");
            }
        }
        return b.toString();
    }

    private void infer(JSONObject state, String narration) throws Exception {
        String n = normalize(narration);
        if (state.optString("weather").length() == 0) {
            if (contains(n, "chuva", "tempestade", "trovao")) state.put("weather", "Chuva ou tempestade");
            else if (contains(n, "neve", "nevasca")) state.put("weather", "Neve");
            else if (contains(n, "nevoa", "bruma")) state.put("weather", "Névoa");
        }
        if (contains(n, "combate", "emboscada", "ataca", "ameaça", "perigo"))
            state.put("danger", "Alto");
        else if (state.optString("danger").length() == 0) state.put("danger", "Baixo");
        if (state.optString("lighting").length() == 0) {
            if (contains(n, "escuridao", "escuro", "sombras")) state.put("lighting", "Escuridão ou luz baixa");
            else if (contains(n, "tocha", "lareira", "fogueira")) state.put("lighting", "Luz de fogo");
        }
    }

    private static void copyNonEmpty(JSONObject from, JSONObject to, String... keys) throws Exception {
        for (String key : keys) {
            if (!from.has(key) || from.isNull(key)) continue;
            Object value = from.get(key);
            if (value instanceof String && ((String) value).trim().length() == 0) continue;
            to.put(key, value);
        }
    }

    private static void append(StringBuilder b, String label, String value) {
        if (value == null || value.trim().length() == 0) return;
        b.append(label).append(": ").append(value.trim()).append(".\n");
    }

    private static String jsonText(String raw) {
        return raw == null ? "" : raw.replace("[", "").replace("]", "").replace("\"", "");
    }

    private static boolean contains(String text, String... values) {
        for (String value : values) if (text.contains(normalize(value))) return true;
        return false;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(new Locale("pt", "BR")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", " ").trim();
    }
}
