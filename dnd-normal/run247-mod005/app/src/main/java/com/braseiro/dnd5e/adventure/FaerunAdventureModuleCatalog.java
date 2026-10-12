package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulos de aventura não lineares para o cenário de Forgotten Realms.
 *
 * Distinção editorial estrita:
 * 1. Aventuras oficiais publicadas em FRAiF (fidelidade canônica exata):
 *    - "The Tomb of Damara" (FRAiF Ch 2, p. 86) — Nível 3. Lady Khara Sulwood, Bonegnasher, resgate da família Silverstar e Orbe de Damara.
 *    - "The Lost Library of Lethchauntos" (FRAiF Ch 7 / FRAiF-TLLoL) — Nível 1 (introdutória). Old Skull Inn, Myrna Nor vs Powell "the Owl" Niccam, disputa pelo tomo Nathlum.
 *
 * 2. Módulos de campanha originais compatíveis com o cenário (ganchos de ambientação):
 *    - "The Heir's Trial" (Waterdeep, política dos Lordes Mascarados).
 *    - "The Archmage's Legacy" (Anauroch e ruínas netherenses).
 *    - "Conspiracy in the Under-Cellar" (Baldur's Gate, intriga urbana e culto dos Três Mortos).
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
        final boolean isOfficialPublished;
        final Scene[] scenes;

        Spec(String s, String t, String i, int l, boolean official, String p, Scene... x) {
            slug = s; title = t; id = i; level = l; isOfficialPublished = official; premise = p; scenes = x;
        }
    }

    private static Scene s(String t, String id, String loc, String beat) {
        return new Scene(t, id, loc, beat);
    }

    private static final Spec[] SPECS = {
        // --- 1. Aventura Oficial Publicada (FRAiF Ch 2, p. 86) ---
        new Spec("tomb_damara", "The Tomb of Damara", "fraif_03", 3, true,
            "Lady Khara Sulwood convoca os heróis em Shadowdale para entrar na Tumba de Damara e destruir o warlord ogro espectral Bonegnasher, resgatando a família Silverstar e impedindo o saque do lendário Orbe de Damara.",
            s("Barrack and Great Hall", "03a", "tomb_barracks", "Entrada no castelo em ruínas; dois ghouls espreitam no quartel e atacam se houver barulho no grande salão."),
            s("Prison & Silverstar Rescue", "03b", "tomb_prison", "Os quatro membros da família Silverstar são interrogados por um Hobgoblin Captain e dois Hobgoblin Warriors; a jovem Sara Silverstar revela a chave mágica da tumba após o resgate."),
            s("Bonegnasher's Den", "03c", "tomb_bonegnasher_lair", "Confronto tático contra o colossal espírito espectral de Bonegnasher, que tenta usar magia necrótica para forçar o selo do sepulcro."),
            s("Damara's Tomb & Conclusion", "03d", "tomb_crypt_chamber", "Abertura da porta mágica com a chave de Sara Silverstar, purificação da guardiã Damara, recuperação do Orbe de Damara e pagamento de Lady Sulwood.")),

        // --- 2. Aventura Oficial Publicada (FRAiF Ch 7 / FRAiF-TLLoL) ---
        new Spec("lost_library", "The Lost Library of Lethchauntos", "fraif_04", 1, true,
            "No Old Skull Inn em Shadowdale, a herdeira Myrna Nor contrata aventureiros iniciantes para alcançar a Biblioteca Perdida de Lethchauntos em Cormanthor e recuperar o tomo lendário Nathlum antes que sua rival Powell 'the Owl' Niccam o venda para o Zhentarim.",
            s("The Old Skull Inn & Journey", "04a", "shadowdale_old_skull_inn", "Audiência com Myrna Nor no salão rústico da estalagem e marcha de 100 milhas pelas trilhas antigas de Cormanthor até a entrada oculta na colina."),
            s("Powell's Vanguard", "04b", "lethchauntos_outer_halls", "Exploração dos corredores iluminados por tochas deixadas pelos capangas de Powell; portas trancadas e armadilhas com teste de Investigação/Ladinagem CD 15."),
            s("The Athenaeum Confrontation", "04c", "lethchauntos_athenaeum", "Confronto nos salões de pergaminhos contra Powell 'the Owl' Niccam e seus mercenários antes que o códice seja retirado do pedestal."),
            s("The Nathlum Preserved", "04d", "lethchauntos_sanctum", "Recuperação do tomo Nathlum, salvando os segredos de cura e venenos de Lethchauntos e entregando o legado à custódia de Myrna Nor.")),

        // --- 3. Módulos Originais de Campanha Inspirados no Cenário ---
        new Spec("heir_trial", "The Heir's Trial", "fraif_01", 1, false,
            "[Aventura Original] Em Waterdeep, um herdeiro legítimo do trono é ameaçado por conspiradores ligados à Rede Negra; o grupo deve protegê-lo e descobrir as provas de sua linhagem real.",
            s("Council of Masks", "01a", "waterdeep_palace", "O conselho dos Lordes Mascarados de Waterdeep recebe a reivindicação do trono sob protestos do emissário dos Zhentarim."),
            s("Streets of Shadows", "01b", "waterdeep_dock_ward", "Emboscada nos becos do Distrito das Docas; o grupo pode lutar ou usar Furtividade para evadir."),
            s("Vault of Proof", "01c", "waterdeep_city_of_dead", "Investigação da cripta ancestral na Cidade dos Mortos onde está selado o selo real com teste de Investigação CD 14."),
            s("Coronation or Exile", "01d", "waterdeep_palace", "Confronto político final onde a escolha do herdeiro sela o destino de Waterdeep e gera consequências de renome permanente.")),

        new Spec("archmage_legacy", "The Archmage's Legacy", "fraif_02", 5, false,
            "[Aventura Original] Com o sumiço do Arquimago mestre nas fronteiras de Anauroch, os aprendizes devem recuperar o grimório legado antes que cultistas netherenses o corrompam.",
            s("The Desert Outpost", "02a", "anauroch_edge", "Chegada ao posto avançado no limiar das dunas onde rastros de tempestade mágica foram avistados."),
            s("The Shattered Tower", "02b", "netheril_tower", "Exploração da torre semidestruída com armadilhas de glifos arcanos e teste de Arcano CD 15."),
            s("The Weave Fracture", "02c", "anauroch_ruins", "Confronto com espectros do Weave que tentam rasgar o grimório; escolha de restaurar a Trama ou selar a torre."),
            s("Sanctum Claimed", "02d", "demiplane_tower", "Abertura do portal demiplanar e estabelecimento da posse legítima da torre de pesquisa.")),

        new Spec("baldurs_undercellar", "Conspiracy in the Under-Cellar", "fraif_05", 2, false,
            "[Aventura Original] Nos corredores sombrios do Under-Cellar em Baldur's Gate, agentes dos Três Mortos conspiram com mercadores corruptos para envenenar os reservatórios da cidade.",
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
            int officialCount = 0;
            int originalCount = 0;
            for (Spec s : SPECS) {
                if (s.isOfficialPublished) officialCount++;
                else originalCount++;
            }
            for (AdventureModule m : all()) nodes += m.getNodes().size();
            return o.put("source", SOURCE_CODE).put("modules", SPECS.length)
                    .put("official_published_adventures", officialCount)
                    .put("original_campaign_hook_modules", originalCount)
                    .put("nodes", nodes)
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
                ts.add(new AdventureTransition("advance_" + (i + 1), "scene_" + (i + 2), "Avançar para o próximo ato após a escolha do grupo.", "", "", false, null, null));
                if (i == 1) {
                    ts.add(new AdventureTransition("diplomatic_branch", "scene_4", "Resolver por via diplomática, negociação ou infiltração direta, pulando combate frontal.", "", "", false, null, null));
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
                // Resgate da família Silverstar e negociação/arrombamento
                ck = new JSONObject();
                try { ck.put("skill", "Stealth").put("dc", 13).put("requires_check", true); } catch (Exception ignored) {}
            } else if ("04b".equals(sc.id)) {
                // Portas da Biblioteca de Lethchauntos (Good quality lock DC 15)
                ck = new JSONObject();
                try { ck.put("skill", "Thieves' Tools").put("dc", 15).put("requires_check", true); } catch (Exception ignored) {}
            } else if ("05b".equals(sc.id)) {
                ck = new JSONObject();
                try { ck.put("skill", "Perception").put("dc", 14).put("requires_check", true); } catch (Exception ignored) {}
            }
            nodes.put(id, new AdventureNode(id, sp.title + " — " + sc.title, sc.beat, directives(), sc.loc, null, ts, ck, null));
        }

        JSONObject meta = new JSONObject();
        try {
            meta.put("source_book", SOURCE_BOOK).put("source_code", SOURCE_CODE).put("slug", sp.slug)
                .put("recommended_level", sp.level)
                .put("is_official_published", sp.isOfficialPublished)
                .put("runtime_tier", sp.isOfficialPublished ? "official_published_adventure_adaptation" : "original_scenario_campaign_hook")
                .put("source_fidelity", sp.isOfficialPublished ? "official_published_adventure_exact" : "original_scenario_compatible")
                .put("non_linear", true).put("auto_rolls", false);
        } catch (Exception ignored) {}

        return new AdventureModule("fraif_" + sp.slug, sp.title + (sp.isOfficialPublished ? " (FRAiF Oficial)" : " (FRAiF Original)"), "1.0.0", "setup", nodes, meta);
    }

    private static String directives() {
        return "Manter consistência com a história oficial de Faerûn. Exigir rolagens explícitas do jogador para perícias sem auto-rolls.";
    }
}
