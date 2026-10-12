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

    private static AdventureModule build(Spec sp) {
        Map<String, AdventureNode> nodes = new LinkedHashMap<>();
        List<AdventureTransition> setupTransitions = new ArrayList<>();
        setupTransitions.add(new AdventureTransition("begin", "scene_1", "Iniciar a missão ao aceitar a comissão de investigação ou expedição.", "", "", false, null, null));
        nodes.put("setup", new AdventureNode("setup", sp.title + " — Premissa", sp.premise, directives(), sp.scenes[0].loc, null, setupTransitions, null, null));

        for (int i = 0; i < sp.scenes.length; i++) {
            Scene sc = sp.scenes[i];
            String id = "scene_" + (i + 1);
            List<AdventureTransition> ts = new ArrayList<>();
            if (i + 1 < sp.scenes.length) {
                ts.add(new AdventureTransition("advance_" + (i + 1), "scene_" + (i + 2), "Avançar para a próxima cena após ação e escolha do grupo.", "", "", false, null, null));
                if (i == 1) {
                    ts.add(new AdventureTransition("alternative_clues", "scene_4", "Resolver por vias de chantagem ou perícia técnica, desviando do confronto nos Cogs.", "", "", false, null, null));
                }
            }
            JSONObject ck = null;
            if ("01a".equals(sc.id)) {
                ck = new JSONObject();
                try { ck.put("skill", "Investigation").put("dc", 13).put("requires_check", true); } catch (Exception ignored) {}
            } else if ("02c".equals(sc.id)) {
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

        return new AdventureModule("efa_" + sp.slug, sp.title + " (EFA)", "1.0.0", "setup", nodes, meta);
    }

    private static String directives() {
        return "Manter atmosfera noir, tecnologia magitec de Eberron e intriga entre Casas Dracossinaladas. Rolagens explícitas sem auto-rolls.";
    }
}
