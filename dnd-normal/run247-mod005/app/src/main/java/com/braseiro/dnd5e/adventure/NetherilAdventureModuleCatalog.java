package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulos de aventura não lineares para Netheril's Fall (NF).
 *
 * Módulos:
 * 1. nf_fools_needle: "Adventure at Fool's Needle" — Expedição através das Time Gates até a agulha cronomântica antes da Queda de Karsus.
 * 2. nf_forgotten_chamber: "The Forgotten Chamber" — Infiltração em cofre lacrado com mythallar em risco de colapso de Weave.
 */
public final class NetherilAdventureModuleCatalog {
    private NetherilAdventureModuleCatalog() {}

    public static final String SOURCE_BOOK = "Netheril's Fall";
    public static final String SOURCE_CODE = "NF";

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
        new Spec("fools_needle", "Adventure at Fool's Needle", "nf_01", 7,
            "No deserto de Anauroch, uma Time Gate ativa-se repentinamente revelando a torre flutuante de Fool's Needle momentos antes do cataclismo de Karsus; o grupo pode resgatar segredos da chronomancia antes que a fenda temporal se feche.",
            s("Through the Chrono Gate", "01a", "netheril_time_gate", "Transposição temporal do deserto atual para os céus opulentos do Império de Netheril no auge de seu poder."),
            s("The Spire of Needles", "01b", "fools_needle_spire", "Exploração da torre de agulhas e contenção de magens descontrolados com teste de Arcano CD 15."),
            s("The Mythallar Flux", "01c", "fools_needle_core", "O núcleo do mythallar oscila violentamente com anomalias temporais que distorcem o espaço e envelhecem matéria."),
            s("The Temporal Choice", "01d", "fools_needle_portal", "Decisão crítica: resgatar os tratados cronomânticos para o presente ou tentar alertar os sábios netherenses sobre a loucura de Karsus.")),
        new Spec("forgotten_chamber", "The Forgotten Chamber", "nf_02", 9,
            "Nas profundezas rochosas sob um enclave caído, a câmara esquecida de um arcanista conserva baterias arcanas intactas e segredos de magias além do 9º círculo.",
            s("The Vault Seals", "02a", "anauroch_subterranean", "Abertura dos selos de adamantina protegidos por runas de abjuração milenares com teste de Investigação/Ladroagem CD 16."),
            s("The Hall of Arcane Batteries", "02b", "forgotten_chamber_hall", "Navegação por pilares energizados por magia primórdia do Weave que lançam descargas de plasma arcano."),
            s("The Sentinel of Xinok", "02c", "forgotten_chamber_core", "Confronto ou desarme do guardião magen mestre que ainda obedece ordens de generais mortos há eras."),
            s("Sanctum Claimed or Sealed", "02d", "forgotten_chamber_inner", "Reivindicação das baterias para fortalecer defesas do presente ou selamento perpétuo contra cultistas da sombra."))
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
        setupTransitions.add(new AdventureTransition("begin", "scene_1", "Iniciar a transição ao entrar na Time Gate ou abrir a câmara antiga.", "", "", false, null, null));
        nodes.put("setup", new AdventureNode("setup", sp.title + " — Premissa", sp.premise, directives(), sp.scenes[0].loc, null, setupTransitions, null, null));

        for (int i = 0; i < sp.scenes.length; i++) {
            Scene sc = sp.scenes[i];
            String id = "scene_" + (i + 1);
            List<AdventureTransition> ts = new ArrayList<>();
            if (i + 1 < sp.scenes.length) {
                ts.add(new AdventureTransition("advance_" + (i + 1), "scene_" + (i + 2), "Prosseguir para a próxima etapa da exploração.", "", "", false, null, null));
                if (i == 1) {
                    ts.add(new AdventureTransition("bypass_spire", "scene_4", "Utilizar atalho dimensional decifrado nos glifos para alcançar diretamente a câmara central.", "", "", false, null, null));
                }
            }
            JSONObject ck = null;
            if ("01b".equals(sc.id)) {
                ck = new JSONObject();
                try { ck.put("skill", "Arcana").put("dc", 15).put("requires_check", true); } catch (Exception ignored) {}
            } else if ("02a".equals(sc.id)) {
                ck = new JSONObject();
                try { ck.put("skill", "Investigation").put("dc", 16).put("requires_check", true); } catch (Exception ignored) {}
            }
            nodes.put(id, new AdventureNode(id, sp.title + " — " + sc.title, sc.beat, directives(), sc.loc, null, ts, ck, null));
        }

        JSONObject meta = new JSONObject();
        try {
            meta.put("source_book", SOURCE_BOOK).put("source_code", SOURCE_CODE).put("slug", sp.slug)
                .put("recommended_level", sp.level).put("non_linear", true).put("auto_rolls", false);
        } catch (Exception ignored) {}

        return new AdventureModule("nf_" + sp.slug, sp.title + " (NF)", "1.0.0", "setup", nodes, meta);
    }

    private static String directives() {
        return "Respeitar a cronologia e grandiosidade arcana de Netheril. Anomalias do Weave exigem rolagens reais sem auto-rolls.";
    }
}
