package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Motor de regras compiladas para pesca esportiva e de sobrevivência
 * baseado no suplemento "The DM's Guide to Fishing" (Zeke Gonzalez).
 */
public final class FishingRulesEngine {
    public static final String BOOK = "The DMs Guide To Fishing.pdf";

    private FishingRulesEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "fishing_rules");
            o.put("book", BOOK);
            o.put("title", "Guia de Pesca do Mestre");
            o.put("category", "Sobrevivência e Atividades de Downtime");
            o.put("version", "1.0.0");
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray tackleCatalog() {
        JSONArray a = new JSONArray();
        try {
            a.put(tackle("Vara de Bambu Simples", "comum", 5, 0, 10, "Vara rústica para águas calmas."));
            a.put(tackle("Vara de Teixo com Carretilha", "incomum", 25, 2, 18, "Carretilha de latão e linha de linho encerado; +2 no teste de puxada."));
            a.put(tackle("Vara Élfica de Freixo Espiritual", "raro", 150, 4, 25, "Madeira encantada que vibra quando um peixe morde; +4 em testes de recolhimento."));
            a.put(tackle("Vara de Osso de Kraken", "muito raro", 800, 6, 35, "Entalhada em osso abissal com linha de tendão de leviatã; suporta peixes monstruosos."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray baitCatalog() {
        JSONArray a = new JSONArray();
        try {
            a.put(bait("Minhoca Comum", "comum", 1, 0, "Isca padrão para rios e lagos."));
            a.put(bait("Isca Artificial Brilhante", "incomum", 5, 2, "Chama atenção de predadores em águas turvas."));
            a.put(bait("Larva Luminescente do Subterrâneo", "raro", 20, 4, "Brilha suavemente; atrai peixes mágicos e do fundo."));
            a.put(bait("Isca Abissal com Sangue de Basilisco", "muito raro", 100, 6, "Atrai predadores perigosos e criaturas aquáticas lendárias."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray fishCatalog() {
        JSONArray a = new JSONArray();
        try {
            // Comuns
            a.put(fish("Truta Prateada", "Rio", "comum", 10, 8, 1, 2, "Peixe saboroso de correnteza; recupera 1 PV extra por descanso curto ao ser cozido."));
            a.put(fish("Bagre do Brejo", "Pântano", "comum", 11, 10, 2, 4, "Carne densa; fornece resistência a veneno na próxima refeição."));
            a.put(fish("Perca Dourada", "Lago", "comum", 10, 7, 1, 3, "Escamas reluzentes e carne leve."));
            a.put(fish("Arenque da Costa", "Mar", "comum", 9, 6, 1, 1, "Pescado em cardumes em águas costeiras."));

            // Incomuns
            a.put(fish("Salmão Real das Cataratas", "Rio", "incomum", 14, 15, 5, 15, "Salta correntezas bravias; cozido concede +3m de deslocamento por 4 horas."));
            a.put(fish("Lúcio Fantasma", "Pântano", "incomum", 15, 18, 8, 25, "Quase translúcido; comer seu filé concede visão no escuro 18m por 2 horas."));
            a.put(fish("Robalo do Recife", "Mar", "incomum", 13, 14, 6, 20, "Peixe forte e combatente de água salgada."));
            a.put(fish("Truta das Cavernas Cegas", "Subterrâneo", "incomum", 14, 12, 3, 30, "Vive em rios do subterrâneo; carne alva e sutil."));

            // Raros / Mágicos
            a.put(fish("Enguia da Tempestade", "Mar", "raro", 17, 24, 12, 120, "Descargas elétricas na captura; refeição concede resistência a dano elétrico por 8 horas."));
            a.put(fish("Carpa de Jade Mística", "Lago", "raro", 18, 20, 7, 180, "Diz-se trazer boa fortuna; comer sua carne concede vantagem no próximo teste de resistência."));
            a.put(fish("Baiacu Ígneo", "Subterrâneo", "raro", 16, 22, 10, 150, "Absorve calor vulcânico; concede resistência a fogo por 8 horas."));

            // Muito Raros / Lendários
            a.put(fish("Peixe-Lua Cósmico", "Mar", "muito raro", 21, 35, 30, 600, "Brilha com luz estelar; ao ser consumido restaura um espaço de magia de até 3º círculo."));
            a.put(fish("Leviatã Prateado Jovem", "Mar", "muito raro", 23, 45, 60, 1200, "Criatura mítica dos mares profundos; troféu lendário de navegadores."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONObject resolveFishingAttempt(String waterType, int survivalOrDexRoll, String rodType, String baitType) {
        JSONObject result = new JSONObject();
        try {
            int rodBonus = 0;
            int rodMaxStamina = 12;
            if ("Vara de Teixo com Carretilha".equalsIgnoreCase(rodType)) { rodBonus = 2; rodMaxStamina = 18; }
            else if ("Vara Élfica de Freixo Espiritual".equalsIgnoreCase(rodType)) { rodBonus = 4; rodMaxStamina = 25; }
            else if ("Vara de Osso de Kraken".equalsIgnoreCase(rodType)) { rodBonus = 6; rodMaxStamina = 35; }

            int baitBonus = 0;
            if ("Isca Artificial Brilhante".equalsIgnoreCase(baitType)) baitBonus = 2;
            else if ("Larva Luminescente do Subterrâneo".equalsIgnoreCase(baitType)) baitBonus = 4;
            else if ("Isca Abissal com Sangue de Basilisco".equalsIgnoreCase(baitType)) baitBonus = 6;

            int totalCheck = survivalOrDexRoll + rodBonus + baitBonus;
            result.put("total_check", totalCheck);
            result.put("rod_bonus", rodBonus);
            result.put("bait_bonus", baitBonus);

            JSONArray catalog = fishCatalog();
            List<JSONObject> eligible = new ArrayList<JSONObject>();
            for (int i = 0; i < catalog.length(); i++) {
                JSONObject f = catalog.optJSONObject(i);
                if (f == null) continue;
                String habitat = f.optString("habitat", "");
                if (waterType == null || waterType.isEmpty() || habitat.equalsIgnoreCase(waterType) || "Qualquer".equalsIgnoreCase(waterType)) {
                    if (totalCheck >= f.optInt("cd_captura", 10)) {
                        eligible.add(f);
                    }
                }
            }

            if (eligible.isEmpty()) {
                result.put("success", false);
                result.put("message", "Nenhum peixe mordeu a isca desta vez. Tente mudar de local ou usar uma isca mais atraente.");
                return result;
            }

            // Seleciona o melhor peixe elegível ao alcance da rolagem
            JSONObject caught = eligible.get(eligible.size() - 1);
            boolean lineHeld = rodMaxStamina >= caught.optInt("estamina_peixe", 10);

            result.put("success", lineHeld);
            result.put("caught_fish", caught);
            if (lineHeld) {
                result.put("message", "Sucesso! Você fisgou um(a) " + caught.optString("nome") + " (" + caught.optInt("peso_libras") + " lb)! " + caught.optString("beneficio_culinario"));
            } else {
                result.put("message", "O peixe mordeu! Porém a força do(a) " + caught.optString("nome") + " excedeu a resistência da sua linha e ela arrebentou!");
            }
        } catch (Exception e) {
            try { result.put("error", String.valueOf(e.getMessage())); } catch (Exception ignored) {}
        }
        return result;
    }

    private static JSONObject tackle(String nome, String raridade, int precoPo, int bonus, int estaminaMax, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("raridade", raridade).put("preco_po", precoPo).put("bonus_recolhimento", bonus).put("estamina_maxima", estaminaMax).put("descricao", desc);
    }

    private static JSONObject bait(String nome, String raridade, int precoPo, int bonus, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("raridade", raridade).put("preco_po", precoPo).put("bonus_atracao", bonus).put("descricao", desc);
    }

    private static JSONObject fish(String nome, String habitat, String raridade, int cd, int estamina, int peso, int valorPo, String beneficio) throws Exception {
        return new JSONObject().put("nome", nome).put("habitat", habitat).put("raridade", raridade).put("cd_captura", cd).put("estamina_peixe", estamina).put("peso_libras", peso).put("valor_po", valorPo).put("beneficio_culinario", beneficio);
    }
}
