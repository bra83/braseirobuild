package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor compilado para o cenário e regras regionais de Evernura
 * baseado no livro "Os segredos que se escondem em evernura".
 */
public final class EvernuraRegionalEngine {
    public static final String BOOK = "Os segredos que se escondem em evernura.pdf";

    private EvernuraRegionalEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "evernura_regional_rules");
            o.put("book", BOOK);
            o.put("title", "Os Segredos de Evernura");
            o.put("category", "Cenário Regional, Florestas Sombrias e Mistérios Arcanos");
            o.put("version", "1.0.0");
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray keyLocations() {
        JSONArray a = new JSONArray();
        try {
            a.put(location("Bosque dos Lamentos", "Floresta Primordial", "Árvores centenárias com casca cinzenta cujos nós formam rostos humanos que parecem chorar quando o vento sopra."));
            a.put(location("Vilarejo Esquecido de Val-Frio", "Povoado Isolado", "Casas de madeira escura com telhados íngremes de palha negra; os aldeões raramente falam com estranhos antes do pôr do sol."));
            a.put(location("Cavernas de Âmbar Sombrio", "Complexo Subterrâneo", "Grutas repletas de estalactites de âmbar que preservam insetos pré-históricos gigantes e espíritos ancestrais."));
            a.put(location("Cripta dos Reis de Ferro", "Masmorra Ancestral", "Tumbas seladas com portões de ferro batido com runas antigas que impedem a saída de guerreiros amaldiçoados."));
            a.put(location("Clareira da Bruxa do Salgueiro", "Refúgio Místico", "Um salgueiro colossal debruçado sobre um lago límpido onde rituais esquecidos são conduzidos nas noites de lua nova."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray factions() {
        JSONArray a = new JSONArray();
        try {
            a.put(faction("Círculo das Cinzas", "Druidas corrompidos que acreditam que a floresta precisa ser purificada pelo fogo místico para renascer."));
            a.put(faction("Caçadores de Corvos", "Ordem de patrulheiros e rastreadores dedicados a manter os horrores da névoa contidos nas profundezas de Evernura."));
            a.put(faction("Comerciantes da Névoa", "Caravaneiros audazes que vendem ervas raras, raízes mágicas e âmbar sombrio aos mercados das grandes cidades."));
            a.put(faction("Irmandade da Lua Pálida", "Coven de bruxas e conjuradores que guardam segredos astronômicos e maldições ancestrais."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray regionalEncounters() {
        JSONArray a = new JSONArray();
        try {
            a.put("Uma névoa fria se ergue do solo, e sombras com formato de lobos com chifres circundam o acampamento.");
            a.put("Um caçador ferido dos Caçadores de Corvos pede ajuda para recuperar seu falcão mensageiro preso em trepadeiras carnívoras.");
            a.put("Uma caravana de carroças negras dos Comerciantes da Névoa oferece poções estranhas e pede escolta pelo desfiladeiro.");
            a.put("O som de um sino distante ecoa pela mata, embora não haja nenhuma igreja num raio de trinta quilômetros.");
        } catch (Exception ignored) {}
        return a;
    }

    private static JSONObject location(String nome, String tipo, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("tipo", tipo).put("descricao", desc);
    }

    private static JSONObject faction(String nome, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("objetivos", desc);
    }
}
