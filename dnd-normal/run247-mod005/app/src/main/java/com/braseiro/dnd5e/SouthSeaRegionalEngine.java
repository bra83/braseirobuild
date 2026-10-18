package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor compilado para o cenário e encontros dos Mares do Sul baseado em "South Sea.pdf".
 * Cobre o arquipélago tropical, pirataria, ilhas vulcânicas, recifes e lendas marítimas.
 */
public final class SouthSeaRegionalEngine {
    public static final String BOOK = "South Sea.pdf";

    private SouthSeaRegionalEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "south_sea_rules");
            o.put("book", BOOK);
            o.put("title", "South Sea - Os Mares do Sul");
            o.put("category", "Cenário Marítimo, Ilhas Tropicais e Pirataria");
            o.put("version", "1.0.0");
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray archipelagoIslands() {
        JSONArray a = new JSONArray();
        try {
            a.put(island("Porto Livre de Tortuga", "Cidade Portuária Pirata", "Cais de madeira cheios de marinheiros, tabernas barulhentas, armazéns de saque e governantes eleitos por voto dos capitães."));
            a.put(island("Arquipélago das Caveiras", "Ilhas Vulcânicas", "Formações de rocha basáltica negra que lembram crânios na névoa marítima; cavernas secretas de contrabandistas."));
            a.put(island("Atol dos Corais de Sangue", "Recife de Coral Místico", "Águas azul-turquesa rasas pontilhadas de corais carmesim afiados como navalhas que estraçalham o casco de navios desavisados."));
            a.put(island("Ilha da Serpente do Trovão", "Selva Tropical Selvagem", "Picos pontiagudos onde nuvens de tempestade nunca se dissipam; lar de lagartos gigantes e templos submersos."));
            a.put(island("Recife dos Naufrágios Eternos", "Cemitério de Navios", "Dezenas de carcaças de galeões e caravelas encalhadas ao longo dos séculos, ligadas por pontes de corda improvisadas."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray maritimeFactions() {
        JSONArray a = new JSONArray();
        try {
            a.put(faction("Irmandade dos Corsários Livres", "Confederação de capitães piratas que operam sob um código estrito de partilha de pilhagem e auxílio mútuo."));
            a.put(faction("Armada da Companhia Real dos Mares do Sul", "Frota mercante pesadamente artilhada do império, empenhada em erradicar a pirataria e monopolizar o comércio de especiarias."));
            a.put(faction("Guardiões do Recife Esmeralda", "Tritões e elfos aquáticos que protegem os santuários subaquáticos contra a ganância de caçadores de pérolas e tesouros."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray seaEncounters() {
        JSONArray a = new JSONArray();
        try {
            a.put("Um navio à deriva sem tripulação visível tem o sino tocando sozinho ao balanço das ondas.");
            a.put("Canhões soam no horizonte: uma caravela imperial está engajada em combate feroz contra dois chalupas de piratas.");
            a.put("O mar fica subitamente calmo e reluzente como espelho; sombras serpentinas colossais nadam sob a quilha.");
            a.put("Náufragos em uma jangada acenam desesperados com uma tocha apagada.");
        } catch (Exception ignored) {}
        return a;
    }

    private static JSONObject island(String nome, String tipo, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("tipo", tipo).put("descricao", desc);
    }

    private static JSONObject faction(String nome, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("objetivos", desc);
    }
}
