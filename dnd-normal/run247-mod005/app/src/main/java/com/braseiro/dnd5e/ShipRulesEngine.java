package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor compilado para embarcações, tripulações, combate naval e viagens em alto-mar
 * baseado no suplemento "Navios.pdf".
 */
public final class ShipRulesEngine {
    public static final String BOOK = "Navios.pdf";

    private ShipRulesEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "ship_rules");
            o.put("book", BOOK);
            o.put("title", "Navios e Navegação");
            o.put("category", "Combate Naval, Embarcações e Viagens Marítimas");
            o.put("version", "1.0.0");
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray vesselCatalog() {
        JSONArray a = new JSONArray();
        try {
            a.put(vessel("Bote a Remos", 50, 11, 50, 0, 0, 1, 4, 0.5, 3, "Pequena embarcação costeira movida a remos; não possui velas nem canhões."));
            a.put(vessel("Chalupa Mercante Rápida", 3000, 15, 150, 12, 100, 4, 15, 10.0, 6, "Ágil e estreita; armada com 2 balistas leves; ideal para rios e baías protegidas."));
            a.put(vessel("Caravela de Exploração", 10000, 15, 300, 12, 120, 10, 30, 80.0, 5, "Três mastros e velas latinas; suporta longas viagens oceânicas; armada com 4 balistas médias."));
            a.put(vessel("Galé de Guerra com Aríete", 20000, 15, 400, 12, 150, 40, 100, 40.0, 7, "Movida por 60 remadores e vela auxiliar; aríete de bronze frontal causa 8d10 contundente no abalroamento."));
            a.put(vessel("Galeão Real de Linha", 45000, 16, 600, 12, 200, 30, 120, 250.0, 4, "Fortaleza flutuante com múltiplos conveses; armada com 12 canhões ou catapultas navais."));
            a.put(vessel("Dracar de Incursão Nórdica", 8000, 15, 200, 12, 100, 15, 50, 20.0, 6, "Fundo chato capaz de navegar em águas rasas e varar diretamente na praia."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray navalManeuvers() {
        JSONArray a = new JSONArray();
        try {
            a.put(maneuver("Abalroamento", "Ação do Capitão", "Testa Ferramentas de Navegador resistido pelo timoneiro inimigo; causa dano pelo casco e aríete."));
            a.put(maneuver("Andaina de Tiro", "Ação da Artilharia", "Todos os canhões ou balistas de um dos bordos disparam em conjunto em salva concentrada."));
            a.put(maneuver("Lançar Ganchos de Abordagem", "Ação dos Marinheiros", "Ganchos e cordas prendem as embarcações a até 9 metros, permitindo travessia física dos combatentes."));
            a.put(maneuver("Reparos de Emergência", "Ação do Carpinteiro", "Teste de Ferramentas de Carpinteiro CD 14 para restaurar 2d10 PV do casco durante o combate."));
            a.put(maneuver("Caçar as Velas (Velocidade Máxima)", "Ação do Contramestre", "Aproveita rajadas de vento para aumentar em 50% o deslocamento naquele turno."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONObject resolveNavalTravelDay(String vesselName, int weatherSeverity, int navigatorCheck) {
        JSONObject res = new JSONObject();
        try {
            int baseMiles = 48; // média de viagem em 24h
            if (vesselName != null && vesselName.toLowerCase().contains("chalupa")) baseMiles = 60;
            else if (vesselName != null && vesselName.toLowerCase().contains("galeão")) baseMiles = 40;

            int progress = baseMiles;
            String event = "Águas calmas e vento favorável; progresso marítimo regular.";

            if (navigatorCheck < 10) {
                progress = baseMiles / 2;
                event = "Erros de leitura astrológica e desvio de rota reduziram o avanço.";
            } else if (navigatorCheck >= 18) {
                progress = (int)(baseMiles * 1.3);
                event = "Ventos alísios capturados com maestria impulsionaram a embarcação rapidamente.";
            }

            if (weatherSeverity >= 3) {
                progress -= 20;
                event += " Uma tempestade súbita fustigou o convés, exigindo esforço contínuo da tripulação nas bombas de porão.";
            }

            res.put("milhas_percorridas", Math.max(0, progress));
            res.put("resumo_viagem", event);
        } catch (Exception ignored) {}
        return res;
    }

    private static JSONObject vessel(String nome, int precoPo, int caCasco, int pvCasco, int caVelas, int pvVelas, int tripMin, int tripMax, double cargaTon, int velMilhasHora, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("preco_po", precoPo).put("ca_casco", caCasco).put("pv_casco", pvCasco).put("ca_velas", caVelas).put("pv_velas", pvVelas).put("tripulacao_min", tripMin).put("tripulacao_max", tripMax).put("carga_toneladas", cargaTon).put("velocidade_milhas_hora", velMilhasHora).put("descricao", desc);
    }

    private static JSONObject maneuver(String nome, String papel, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("papel", papel).put("descricao", desc);
    }
}
