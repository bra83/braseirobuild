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
            s("Sanctum Claimed", "02d", "demiplane_tower", "Abertura do portal demiplanar e estabelecimento da posse legítima da torre de pesquisa.")),
        new Spec("tomb_damara", "The Tomb of Damara", "fraif_03", 3,
            "Nas colinas de Cormanthor, a tumba ancestral de Damara é violada por saqueadores; heróis devem impedir o despertar da maldição sepulcral.",
            s("Entrance in Cormanthor", "03a", "cormanthor_woods", "Trilhas esquecidas sob a copa élfica conduzem às portas de granito seladas com runas arcanas."),
            s("Hall of Statues", "03b", "damara_antechamber", "Câmara dos guardiões de pedra onde um teste de História CD 13 revela a sequência segura dos glifos."),
            s("The Sarcophagus Seal", "03c", "damara_crypt", "Confronto com o guardião espectral e a decisão crucial: purificar o túmulo ou reclamar o Orbe de Damara."),
            s("Dawn in the Dales", "03d", "dalelands_meadows", "Retorno às Terras dos Vales com reconhecimento das comunidades rurais e benção de Lathander.")),
        new Spec("lost_library", "The Lost Library of Lethchauntos", "fraif_04", 7,
            "Expedição às profundezas da lendária Biblioteca Perdida de Lethchauntos para recuperar códices primordiais do Weave antes de agentes de Thay.",
            s("The Submerged Vaults", "04a", "lethchauntos_exterior", "Navegação por canais subterrâneos inundados e desativação das comportas mágicas."),
            s("Athenaeum of Whispers", "04b", "lethchauntos_scriptorium", "Salão de leitura protegido por pergaminhos autômatos e teste de Arcano CD 16."),
            s("Shadows of Thay", "04c", "lethchauntos_inner_sanctum", "Confronto diplomático ou bélico contra magos vermelhos que tentam rasgar o selo do repositório."),
            s("Legacy Preserved", "04d", "lethchauntos_apex", "Consagração do santuário sob tutela dos Harpistas e proteção permanente do repositório.")),
        new Spec("baldurs_undercellar", "Conspiracy in the Under-Cellar", "fraif_05", 2,
            "Nos corredores sombrios do Under-Cellar em Baldur's Gate, agentes dos Três Mortos conspiram com mercadores corruptos para envenenar os reservatórios da cidade.",
            s("Shadows of the Lower City", "05a", "baldurs_lower_city", "Investigação nas tavernas portuárias para identificar o contato dos contrabandistas."),
            s("The Under-Cellar Cisterns", "05b", "baldurs_undercellar_cistern", "Descida pelos esgotos labirínticos com teste de Percepção CD 14 para evitar armadilhas de gás."),
            s("Altar of the Three", "05c", "baldurs_shrine_dead_three", "Interrupção do ritual de contaminação com escolha de capturar o líder ou destruir os frascos tóxicos."),
            s("Gates of Dawn", "05d", "baldurs_seatower", "Relatório aos Punhos Flamejantes e aumento de reputação nas facções de Baldur's Gate."))
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
            } else if ("03b".equals(sc.id)) {
                ck = new JSONObject();
                try { ck.put("skill", "History").put("dc", 13).put("requires_check", true); } catch (Exception ignored) {}
            } else if ("04b".equals(sc.id)) {
                ck = new JSONObject();
                try { ck.put("skill", "Arcana").put("dc", 16).put("requires_check", true); } catch (Exception ignored) {}
            } else if ("05b".equals(sc.id)) {
                ck = new JSONObject();
                try { ck.put("skill", "Perception").put("dc", 14).put("requires_check", true); } catch (Exception ignored) {}
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
