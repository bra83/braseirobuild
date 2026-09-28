package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Grounding de identidade da cena.
 *
 * O mapa/local e os heróis são autoridades diferentes do elenco de NPCs.
 * Esta classe impede que um nome de lugar (ex.: "Nidal") seja promovido a NPC
 * apenas porque apareceu antes de um verbo na prosa, e impede que um herói seja
 * duplicado como NPC. NPCs novos continuam permitidos, desde que não colidam com
 * identidades canônicas e sejam estruturados pelo Mestre.
 */
public final class SceneEntityGrounding {
    private SceneEntityGrounding() { }

    public static Set<String> placeAliases(AppDb db, JSONObject scene) {
        HashSet<String> out = new HashSet<String>();
        if (db == null) return out;
        JSONObject effective = scene == null ? db.getSceneState() : scene;
        addPlaceAliases(out, effective.optString("location", ""));
        addPlaceAliases(out, NarrativeLocationNaturalizer.currentNarrativeLabel(db));
        try {
            JSONObject map = db.getMapState();
            String packageId = map.optString("map_package_id", "");
            String hex = map.optString("current_hex_id", "");
            if (packageId.length() > 0 && hex.length() > 0) {
                List<JSONObject> locations = db.getMapLocationsForHex(packageId, hex);
                for (JSONObject location : locations) {
                    if (location == null) continue;
                    String status = normalize(location.optString("placement_status", ""));
                    if (status.contains("review") || status.contains("approx") || status.contains("unassigned")) continue;
                    addPlaceAliases(out, location.optString("name", ""));
                }
                JSONObject note = db.getMapHexNote(packageId, hex);
                addPlaceAliases(out, note.optString("title", note.optString("note_title", "")));
            }
        } catch (Exception ignored) { AppLog.ignored("SceneEntityGrounding", "Não foi possível expandir aliases cartográficos.", ignored); }
        return out;
    }

    public static Set<String> nonNpcAliases(AppDb db, JSONObject scene) {
        HashSet<String> out = new HashSet<String>(placeAliases(db, scene));
        if (db != null) {
            try {
                for (JSONObject hero : db.listActiveCharacters()) addAlias(out, hero.optString("name", ""));
            } catch (Exception ignored) { AppLog.ignored("SceneEntityGrounding", "Não foi possível ler heróis ativos.", ignored); }
        }
        return out;
    }

    public static boolean isPlaceIdentity(AppDb db, JSONObject scene, String name) {
        String candidate = normalize(name);
        return candidate.length() > 0 && placeAliases(db, scene).contains(candidate);
    }

    public static boolean isPlayerIdentity(AppDb db, String name) {
        String candidate = normalize(name); if (candidate.length() == 0 || db == null) return false;
        try { for (JSONObject hero : db.listActiveCharacters()) if (candidate.equals(normalize(hero.optString("name", "")))) return true; }
        catch (Exception ignored) { AppLog.ignored("SceneEntityGrounding", "Não foi possível conferir identidade de herói.", ignored); }
        return false;
    }

    public static boolean isNonNpcIdentity(AppDb db, JSONObject scene, String name) {
        String candidate = normalize(name); if (candidate.length() == 0) return true;
        return nonNpcAliases(db, scene).contains(candidate);
    }

    public static JSONArray filterPresentNpcs(AppDb db, JSONObject scene, Object raw, JSONArray warnings) {
        JSONArray source = toArray(raw), out = new JSONArray();
        HashSet<String> seen = new HashSet<String>();
        for (int i=0;i<source.length();i++) {
            Object value = source.opt(i); JSONObject item = value instanceof JSONObject ? (JSONObject)value : null;
            String name = item != null ? GeneratedTextSanitizer.scalar(item.optString("name", item.optString("label", "")), "name") : GeneratedTextSanitizer.scalar(String.valueOf(value), "name");
            String key = normalize(name);
            if (name.length() == 0 || key.length() == 0) continue;
            if (isNonNpcIdentity(db, scene, name)) {
                if (warnings != null) warnings.put("'"+name+"' foi removido do elenco de NPCs porque é local ou personagem do jogador.");
                continue;
            }
            if (!seen.add(key)) continue;
            try {
                if (item != null) { JSONObject copy = new JSONObject(item.toString()); copy.put("name", name); out.put(copy); }
                else out.put(new JSONObject().put("name", name));
            } catch (Exception ignored) { AppLog.ignored("SceneEntityGrounding", "Participante inválido ignorado.", ignored); }
        }
        return out;
    }

    public static JSONObject promptPolicy(AppDb db, JSONObject scene) {
        JSONObject out = new JSONObject();
        try {
            JSONArray places = new JSONArray(); for (String value : placeAliases(db, scene)) places.put(value);
            JSONArray heroes = new JSONArray(); if (db != null) for (JSONObject hero : db.listActiveCharacters()) if (hero.optString("name").trim().length()>0) heroes.put(hero.optString("name").trim());
            JSONArray present = filterPresentNpcs(db, scene, scene == null ? null : scene.opt("present_npcs_json"), null);
            out.put("place_aliases", places).put("heroes", heroes).put("present_npcs", present);
            out.put("rule", "Local nunca é NPC. Herói nunca é NPC. Não renomeie um NPC já presente. Todo NPC novo nomeado que entrar fisicamente na cena deve usar exatamente o mesmo nome em narration, scene_state.present_npcs e npc_updates na mesma resposta. Figurantes anônimos podem ser transitórios, mas devem constar em scene_state.present_npcs quando atuarem em primeiro plano.");
        } catch (Exception ignored) { AppLog.ignored("SceneEntityGrounding", "Falha ao montar política de identidade.", ignored); }
        return out;
    }

    public static String promptDirective(AppDb db, JSONObject scene) {
        JSONObject policy = promptPolicy(db, scene);
        return "GROUNDING DE ENTIDADES DA CENA (OBRIGATÓRIO): "+policy.toString()+"\n"+
                "Nunca trate qualquer item de place_aliases como pessoa, voz, autoridade ou NPC. Nunca use o nome de um herói como NPC. Se a cena já lista um NPC, preserve o nome exatamente; não o substitua por outro personagem para cumprir a mesma função. Se introduzir um NPC novo nomeado, registre-o estruturalmente na MESMA resposta em scene_state.present_npcs e npc_updates. Se mencionar guardas, mercadores, aldeões ou outro grupo atuando em primeiro plano, inclua esse grupo em scene_state.present_npcs como figurante transitório/anônimo.\n";
    }

    public static boolean isWeakAutoNpc(JSONObject npc) {
        if (npc == null || npc.length() == 0) return true;
        String summary = normalize(npc.optString("summary", ""));
        boolean genericSummary = summary.length()==0 || summary.equals("npc") || summary.contains("presente na cena atual") || summary.contains("npc presente") || summary.equals("presente");
        boolean noIdentity = npc.optString("goals", "").trim().length()==0 && npc.optString("secrets", "").trim().length()==0 && npc.optString("knowledge", "").trim().length()==0 && npc.optString("debts", "").trim().length()==0 && npc.optString("visual_description", "").trim().length()==0;
        String stats = npc.optString("stats_json", "").trim();
        boolean noStats = stats.length()==0 || "{}".equals(stats);
        return genericSummary && noIdentity && noStats;
    }

    private static JSONArray toArray(Object raw) {
        if (raw instanceof JSONArray) return (JSONArray)raw;
        if (raw instanceof String) try { return new JSONArray(String.valueOf(raw)); } catch (Exception ignored) { return new JSONArray(); }
        return new JSONArray();
    }

    private static void addPlaceAliases(Set<String> out, String raw) {
        String n = normalize(raw); if (n.length()==0) return; out.add(n);
        String stripped = n.replaceFirst("^(?:a|o|as|os|na|no|nas|nos|em|nesta|neste|nessa|nesse)\\s+", "");
        out.add(stripped);
        String generic = stripped.replaceFirst("^(?:regiao|cidade|vila|aldeia|territorio|reino|forte|fortaleza|acampamento|posto|mercado|distrito|bairro|porto|ruinas|ruina|templo|estrada|floresta|planicie|pantano|montanha|colinas?)\\s+(?:de|do|da|dos|das)\\s+", "");
        if (generic.length()>=2) out.add(generic);
        int de = stripped.lastIndexOf(" de "); if (de>=0 && de+4<stripped.length()) out.add(stripped.substring(de+4).trim());
        int da = stripped.lastIndexOf(" da "); if (da>=0 && da+4<stripped.length()) out.add(stripped.substring(da+4).trim());
        int dox = stripped.lastIndexOf(" do "); if (dox>=0 && dox+4<stripped.length()) out.add(stripped.substring(dox+4).trim());
    }
    private static void addAlias(Set<String> out,String raw){String n=normalize(raw);if(n.length()>0)out.add(n);}
    public static String normalize(String value){if(value==null)return "";String n=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return n.replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();}
}
