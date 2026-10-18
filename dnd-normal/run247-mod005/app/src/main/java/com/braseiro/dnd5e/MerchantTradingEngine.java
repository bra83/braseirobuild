package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor compilado para comerciantes, mercados vivos, regateio e flutuação de preços
 * baseado no suplemento "Traders and Merchants.pdf".
 */
public final class MerchantTradingEngine {
    public static final String BOOK = "Traders and Merchants.pdf";

    private MerchantTradingEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "traders_merchants_rules");
            o.put("book", BOOK);
            o.put("title", "Comerciantes e Mercadores (Traders and Merchants)");
            o.put("category", "Economia Viva, Negociação e Comércio");
            o.put("version", "1.0.0");
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray merchantProfiles() {
        JSONArray a = new JSONArray();
        try {
            a.put(profile("Boticário e Alquimista", "Ervas curativas, antídotos, poções simples, óleos e reagentes de laboratório.", 13, "Cauteloso e metódico; desconfia de aventureiros sujos de poeira."));
            a.put(profile("Mestre Armeiro e Ferreiro", "Armas marciais, cota de malha, placas sob medida, afiação e conserto de engrenagens.", 12, "Prático, direto e honrado; respeita guerreiros que cuidam bem de suas lâminas."));
            a.put(profile("Joalheiro e Cambista", "Gemas polidas, barras de ouro/prata, colares encantáveis e empréstimos a juros.", 16, "Ganancioso e perspicaz; examina cada moeda com lupa e balança de precisão."));
            a.put(profile("Mascate Itinerante", "Equipamento de viagem, cordas, tochas, barracas, rações de viagem e bugigangas curiosas.", 11, "Tagarela e bem-humorado; troca mercadorias por boas histórias de estrada."));
            a.put(profile("Contrabandista dos Becos", "Componentes de magia ilegais, venenos refinados, gazuas e passagens secretas.", 15, "Sombrio e arredio; cobra caro pela discrição absoluta."));
            a.put(profile("Importador de Especiarias Finas", "Café de terras distantes, seda, canela, baunilha, vinhos raros e queijos envelhecidos.", 14, "Aristocrático e exigente; oferece degustação aos clientes mais distintos."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONObject resolveHaggleAttempt(int persuasionCheck, int merchantGreedDc, int basePricePo) {
        JSONObject res = new JSONObject();
        try {
            int difference = persuasionCheck - merchantGreedDc;
            double discountFactor = 1.0;
            String outcome;

            if (persuasionCheck <= 1 || difference <= -5) {
                discountFactor = 1.15; // Mercador se ofende e sobe o preço
                outcome = "O mercador sentiu-se insultado com a proposta e aumentou o valor em 15%!";
            } else if (difference < 0) {
                discountFactor = 1.0;
                outcome = "O comerciante manteve sua postura irredutível: o preço permaneceu exatamente o valor de tabela.";
            } else if (difference < 5) {
                discountFactor = 0.90; // 10% de desconto
                outcome = "Com argumentos razoáveis, você conseguiu um desconto de cortesia de 10%.";
            } else {
                discountFactor = 0.80; // 20% de desconto
                outcome = "Excelente lábia! O comerciante simpatizou e concedeu um desconto generoso de 20%.";
            }

            int finalPrice = Math.max(1, (int)Math.round(basePricePo * discountFactor));
            res.put("sucesso", difference >= 0);
            res.put("preco_base_po", basePricePo);
            res.put("preco_final_po", finalPrice);
            res.put("resultado", outcome);
        } catch (Exception ignored) {}
        return res;
    }

    private static JSONObject profile(String tipo, String estoque, int cdGanancia, String temperamento) throws Exception {
        return new JSONObject().put("tipo", tipo).put("especialidade_estoque", estoque).put("cd_ganancia", cdGanancia).put("temperamento", temperamento);
    }
}
