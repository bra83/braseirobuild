package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulos de aventura não lineares para Eberron: Forge of the Artificer (EFA).
 *
 * Módulos:
 * 1. efa_sharn_inquisitive: "The Enemy of My Enemy" — Investigação noir em Sharn nas torres altas e baixas.
 * 2. efa_morgrave_expedition: "Race For The Crab Temple" — Expedição arqueológica de Morgrave na selva/ruínas com dirigível elemental.
 */
public final class EberronAdventureModuleCatalog {
    private EberronAdventureModuleCatalog() {}

    public static final String SOURCE_BOOK = "Eberron: Forge of the Artificer";
    public static final String SOURCE_CODE = "EFA";

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
        new Spec("sharn_inquisitive", "The Enemy of My Enemy", "efa_01", 3,
            "Em Sharn, um magnata da Casa Cannith é assassinado em sua carruagem celeste suspensa; o grupo é contratado por um inquisitivo particular independente para descobrir qual das facções armou o golpe antes que os Sentinel Marshals de Deneith fechem os distritos.",
            s("The Raintorn Skyway", "01a", "sharn_skyway", "A cena do crime entre as torres mais nobres; carruagem perfurada por virotes de besta arcanos e teste de Investigação CD 13."),
            s("The Clifftop Chase", "01b", "sharn_clifftop", "Perseguição aos informantes do clã Boromar pelos terraços e pontes de suspensão sob chuva ácida."),
            s("Descent into the Cogs", "01c", "sharn_cogs", "Infiltração nos níveis inferiores de Sharn onde forjadores clandestinos montam armaduras proibidas."),
            s("Confrontation or Alliance", "01d", "sharn_lower_dura", "Confronto com os mandantes da Casa rival ou negociação de dossiê para selar trégua.")),
        new Spec("morgrave_expedition", "Race For The Crab Temple", "efa_02", 5,
            "A Universidade Morgrave comissiona uma expedição rápida a bordo de um Dirigível Elemental de Lyrandar rumo ao lendário Templo do Caranguejo antes que espiões de Aundair alcancem as relíquias primordiais.",
            s("Sky Journey over the Mournland", "02a", "airship_sky", "Navegação arriscada nos céus das bordas da terra devastada enfrentando correntes tempestuosas."),
            s("The Temple Approaches", "02b", "crab_temple_exterior", "Descida de rapel do dirigível sobre a cidadela ciclópica cercada por guardiões autômatos."),
            s("The Elemental Vault", "02c", "crab_temple_inner", "Desativação dos nós de vinculação elemental com teste de Arcano CD 15 para abrir o relicário."),
            s("Escape with the Codex", "02d", "airship_deck", "Extração aérea sob ataque das forças rivais; decisão sobre entregar a relíquia a Morgrave ou retê-la."))
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

    /**
     * EFA: rotas persistentes com consequências distintas. IDs legados de cenas e
     * transições são preservados para não invalidar partidas já iniciadas.
     * O texto é um módulo de campanha; não constitui certificação editorial.
     */
    private static AdventureModule build(Spec sp) {
        Map<String, AdventureNode> nodes = new LinkedHashMap<>();
        boolean sharn = "sharn_inquisitive".equals(sp.slug);
        String prefix = "efa_" + sp.slug;

        List<AdventureTransition> start = new ArrayList<>();
        start.add(new AdventureTransition("begin", "scene_1",
                "Aceitar o encargo e iniciar a missão.", "", "", false, null,
                flagEffect(prefix + "_started")));
        nodes.put("setup", new AdventureNode("setup", sp.title + " — Premissa",
                sp.premise, directives(), sp.scenes[0].loc, null, start, null, null));

        for (int i = 0; i < sp.scenes.length; i++) {
            Scene sc = sp.scenes[i];
            String nodeId = "scene_" + (i + 1);
            List<AdventureTransition> transitions = new ArrayList<>();

            if (i == 0) {
                transitions.add(new AdventureTransition("advance_1", "scene_2",
                        sharn ? "Examinar as pistas e seguir os informantes."
                              : "Completar a aproximação aérea e iniciar a expedição.",
                        "", "", false, null, flagEffect(prefix + "_first_lead")));
            } else if (i == 1) {
                transitions.add(new AdventureTransition("advance_2", "scene_3",
                        sharn ? "Investigar pessoalmente os Cogs e obter testemunhos."
                              : "Explorar o templo e examinar os mecanismos do relicário.",
                        "", "", false, null, flagEffect(prefix + "_path_main")));
                // Rota alternativa precisa de perícia e tem consequência distinta.
                transitions.add(new AdventureTransition("alternative_clues", "scene_4",
                        sharn ? "Negociar o dossiê com um informante e evitar os Cogs."
                              : "Decifrar à distância a rota até o cofre e evitar os guardiões.",
                        "", "", false, check(sharn ? "Investigation" : "Arcana", sharn ? 14 : 15),
                        flagEffect(prefix + "_path_alternative")));
            } else if (i == 2) {
                transitions.add(new AdventureTransition("advance_3", "scene_4",
                        "Resolver o conflito e chegar à decisão final.",
                        "", "", false, null, flagEffect(prefix + "_confrontation_resolved")));
            } else {
                // A conclusão depende de uma escolha real e não de texto gerado pela IA.
                transitions.add(new AdventureTransition(
                        sharn ? "publish_truth" : "deliver_relic", "completed",
                        sharn ? "Divulgar as provas e expor publicamente a conspiração."
                              : "Entregar o códice à Universidade Morgrave.",
                        "", "", false, null,
                        flagEffect(sharn ? "efa_sharn_truth_published" : "efa_morgrave_relic_delivered")));
                transitions.add(new AdventureTransition(
                        sharn ? "protect_alliance" : "seal_relic", "completed",
                        sharn ? "Negociar um acordo discreto e preservar os aliados."
                              : "Selar o relicário para impedir sua exploração.",
                        "", "", false, null,
                        flagEffect(sharn ? "efa_sharn_sealed_alliance" : "efa_morgrave_relic_sealed")));
            }

            JSONObject required = null;
            if ("01a".equals(sc.id)) required = check("Investigation", 13);
            else if ("02c".equals(sc.id)) required = check("Arcana", 15);
            nodes.put(nodeId, new AdventureNode(nodeId, sp.title + " — " + sc.title,
                    sc.beat, directives(), sc.loc, null, transitions, required, null));
        }

        JSONObject meta = new JSONObject();
        try {
            meta.put("source_book", SOURCE_BOOK).put("source_code", SOURCE_CODE)
                .put("slug", sp.slug).put("recommended_level", sp.level)
                .put("editorial_classification", "UNVERIFIED_SOURCE")
                .put("non_linear", true).put("auto_rolls", false)
                .put("distinct_persistent_endings", true);
        } catch (Exception ignored) {}
        return new AdventureModule("efa_" + sp.slug, sp.title + " (EFA)",
                "1.0.0", "setup", nodes, meta);
    }

    private static JSONObject flagEffect(String flag) {
        JSONObject effects = new JSONObject();
        try { effects.put("add_flags", new JSONArray().put(flag)); }
        catch (Exception ignored) {}
        return effects;
    }

    private static JSONObject check(String skill, int dc) {
        JSONObject required = new JSONObject();
        try {
            required.put("requires_check", true).put("skill", skill).put("dc", dc);
        } catch (Exception ignored) {}
        return required;
    }

    private static String directives() {
        return "Manter atmosfera noir, tecnologia magitec de Eberron e intriga entre Casas Dracossinaladas. Rolagens explícitas sem auto-rolls.";
    }
}
