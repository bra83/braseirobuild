package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulos de aventura não lineares compilados de Forgotten Realms: Adventures in Faerûn (FRAiF).
 * Suportam escolhas livres, transições ramificadas e consequências persistentes no SQLite.
 */
public final class FaerunAdventureModuleCatalog {
    private FaerunAdventureModuleCatalog() {}

    public static final String SOURCE_BOOK = "Forgotten Realms: Adventures in Faerûn";
    public static final String SOURCE_CODE = "FRAiF";

    private static final class Scene {
        final String title, id, loc, beat;
        Scene(String t, String i, String l, String b) { title = t; id = i; loc = l; beat = b; }
    }

    private static final class Spec {
        final String slug, title, id, premise;
        final int level;
        final Scene[] scenes;
        Spec(String s, String t, String i, int l, String p, Scene... x) {
            slug = s; title = t; id = i; level = l; premise = p; scenes = x;
        }
    }

    private static Scene s(String t, String id, String loc, String beat) {
        return new Scene(t, id, loc, beat);
    }

    private static final Spec[] SPECS = {
        new Spec("heir_trial", "The Heir's Trial", "fraif_01", 1,
            "Em Waterdeep, um herdeiro legítimo do trono é ameaçado por conspiradores ligados à Rede Negra; o grupo deve protegê-lo e descobrir as provas de sua linhagem real.",
            s("Council of Masks", "01a", "waterdeep_palace", "O conselho dos Lordes Mascarados de Waterdeep recebe a reivindicação do trono sob protestos do emissário dos Zhentarim."),
            s("Streets of Shadows", "01b", "waterdeep_dock_ward", "Emboscada nos becos do Distrito das Docas; o grupo pode lutar ou usar Furtividade para evadir."),
            s("Vault of Proof", "01c", "waterdeep_city_of_dead", "Investigação da cripta ancestral na Cidade dos Mortos onde está selado o selo real com teste de Investigação CD 14."),
            s("Coronation or Exile", "01d", "waterdeep_palace", "Confronto político final onde a escolha do herdeiro sela o destino de Waterdeep e gera consequências de renome permanente.")),
        new Spec("archmage_legacy", "The Archmage's Legacy", "fraif_02", 5,
            "Com o sumiço do Arquimago mestre nas fronteiras de Anauroch, os aprendizes devem recuperar o grimório legado antes que cultistas netherenses o corrompam.",
            s("The Desert Outpost", "02a", "anauroch_edge", "Chegada ao posto avançado no limiar das dunas onde rastros de tempestade mágica foram avistados."),
            s("The Shattered Tower", "02b", "netheril_tower", "Exploração da torre semidestruída com armadilhas de glifos arcanos e teste de Arcano CD 15."),
            s("The Weave Fracture", "02c", "anauroch_ruins", "Confronto com espectros do Weave que tentam rasgar o grimório; escolha de restaurar a Trama ou selar a torre."),
            s("Sanctum Claimed", "02d", "demiplane_tower", "Abertura do portal demiplanar e estabelecimento da posse legítima da torre de pesquisa."))
    };

    public static List<AdventureModule> all() {
        List<AdventureModule> out = new ArrayList<>();
        for (Spec x : SPECS) out.add(build(x));
        return out;
    }

    public static AdventureModule byId(String id) {
        if (id == null) return null;
        for (AdventureModule m : all()) if (m.getId().equalsIgnoreCase(id.trim())) return m;
        return null;
    }

    public static JSONObject coverage() {
        JSONObject o = new JSONObject();
        try {
            int nodes = 0;
            for (AdventureModule m : all()) nodes += m.getNodes().size();
            return o.put("source", SOURCE_CODE).put("modules", SPECS.length).put("nodes", nodes)
                    .put("non_linear_branches", true).put("auto_rolls", false);
        } catch (Exception e) {
            return o;
        }
    }

    private static AdventureModule build(Spec sp) {
        Map<String, AdventureNode> nodes = new LinkedHashMap<>();
        List<AdventureTransition> setupTransitions = new ArrayList<>();
        setupTransitions.add(new AdventureTransition("begin", "scene_1", "Iniciar a aventura quando os heróis aceitarem a missão.", "", "", false, null, null));
        nodes.put("setup", new AdventureNode("setup", sp.title + " — Premissa", sp.premise, directives(), sp.scenes[0].loc, null, setupTransitions, null, null));

        for (int i = 0; i < sp.scenes.length; i++) {
            Scene sc = sp.scenes[i];
            String id = "scene_" + (i + 1);
            List<AdventureTransition> ts = new ArrayList<>();
            if (i + 1 < sp.scenes.length) {
                // Transição principal
                ts.add(new AdventureTransition("advance_" + (i + 1), "scene_" + (i + 2), "Avançar para o próximo ato após a escolha do grupo.", "", "", false, null, null));
                // Transição alternativa (ramificação não linear)
                if (i == 1) {
                    ts.add(new AdventureTransition("diplomatic_branch", "scene_4", "Resolver por via diplomática ou infiltração direta, pulando combate frontal.", "", "", false, null, null));
                }
            }
            JSONObject ck = null;
            if ("01c".equals(sc.id)) {
                ck = new JSONObject();
                try { ck.put("skill", "Investigation").put("dc", 14).put("requires_check", true); } catch (Exception ignored) {}
            } else if ("02b".equals(sc.id)) {
                ck = new JSONObject();
                try { ck.put("skill", "Arcana").put("dc", 15).put("requires_check", true); } catch (Exception ignored) {}
            }
            nodes.put(id, new AdventureNode(id, sp.title + " — " + sc.title, sc.beat, directives(), sc.loc, null, ts, ck, null));
        }

        JSONObject meta = new JSONObject();
        try {
            meta.put("source_book", SOURCE_BOOK).put("source_code", SOURCE_CODE).put("slug", sp.slug)
                .put("recommended_level", sp.level).put("non_linear", true).put("auto_rolls", false);
        } catch (Exception ignored) {}

        return new AdventureModule("fraif_" + sp.slug, sp.title + " (FRAiF)", "1.0.0", "setup", nodes, meta);
    }

    private static String directives() {
        return "Manter consistência com a história oficial de Faerûn. Exigir rolagens explícitas do jogador para perícias sem auto-rolls.";
    }

    private static JSONObject check(String id) {
        JSONObject o = new JSONObject();
        try { o.put("scene_id", id).put("requires_check", false); } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject reward() {
        JSONObject o = new JSONObject();
        try { o.put("auto_award", false).put("requires_manual_confirmation", true); } catch (Exception ignored) {}
        return o;
    }
}
