package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor compilado para tatuagens místicas baseado no suplemento "Tome of Mystical Tattoos".
 * Implementa agulhas mágicas, slots corporais, sintonização, aplicação e catálogo de tatuagens arcanas.
 */
public final class MysticalTattoosEngine {
    public static final String BOOK = "D&D5e - Tome of Mystical Tattoos.pdf";

    private MysticalTattoosEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "mystical_tattoos_rules");
            o.put("book", BOOK);
            o.put("title", "Tome of Mystical Tattoos");
            o.put("category", "Itens Mágicos Corporais e Modificação Mística");
            o.put("version", "1.0.0");
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray bodySlots() {
        JSONArray a = new JSONArray();
        try {
            a.put(slot("Braço Direito", "Médio", 1, "Permite tatuagens focadas em armas, golpes e escudos."));
            a.put(slot("Braço Esquerdo", "Médio", 1, "Permite tatuagens defensivas, absorção ou foco arcano."));
            a.put(slot("Peito / Torso", "Grande", 2, "Permite tatuagens de vitalidade, respiração elemental ou armadura espiritual."));
            a.put(slot("Costas", "Grande", 2, "Ideal para tatuagens de asas, auras e monstros míticos."));
            a.put(slot("Pernas", "Médio", 1, "Tatuagens de mobilidade, velocidade e acrobacia."));
            a.put(slot("Rosto e Mãos", "Pequeno", 1, "Tatuagens de percepção sensorial, carisma e prestidigitação."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray tattooCatalog() {
        JSONArray a = new JSONArray();
        try {
            a.put(tattoo("Tatuagem das Garras da Fera", "incomum", "Braço ou Mão", true, 200,
                    "Ataques desarmados contam como mágicos e causam +1d4 de dano cortante."));

            a.put(tattoo("Tatuagem das Escamas de Dragão", "raro", "Peito ou Costas", true, 800,
                    "Concede CA base 13 + mod Destreza quando sem armadura, além de resistência a um tipo de dano dracônico escolhido."));

            a.put(tattoo("Tatuagem das Asas do Falcão Fantasma", "raro", "Costas", true, 1000,
                    "Com uma ação bônus, projeta asas translúcidas por 10 minutos (1x por descanso longo), conferindo deslocamento de voo 12m."));

            a.put(tattoo("Tatuagem do Olho Onisciente", "incomum", "Rosto ou Mão", true, 300,
                    "Concede visão no escuro 18m e vantagem em testes de Percepção para notar armadilhas e ilusões."));

            a.put(tattoo("Tatuagem do Relâmpago Serpentino", "raro", "Pernas", true, 900,
                    "Aumenta o deslocamento em 3m e permite usar Desengajar como ação bônus. Uma vez por dia, causa 2d8 de dano elétrico em reação a um golpe."));

            a.put(tattoo("Tatuagem do Coração de Lava", "muito raro", "Peito", true, 3000,
                    "Imunidade a dano por frio natural e resistência a dano de fogo; ataques corpo a corpo causam 1d6 de fogo adicional."));

            a.put(tattoo("Tatuagem da Fênix Renascida", "lendário", "Peito e Costas", true, 8000,
                    "Ao cair a 0 PV, em vez disso cai a 1 PV e libera uma onda de fogo sagrado de 4d10 em criaturas hostis a até 3m (1x a cada 7 dias)."));

            a.put(tattoo("Tatuagem das Sombras Fluídas", "incomum", "Braço", false, 250,
                    "Permite conjurar o truque Ilusão Menor e concede +2 em testes de Furtividade na penumbra."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONObject applicationCheck(String tattooRarity, int tattooistDexOrToolRoll) {
        JSONObject o = new JSONObject();
        try {
            int dc = 12;
            int hours = 4;
            int inkCostPo = 50;
            if ("incomum".equalsIgnoreCase(tattooRarity)) { dc = 14; hours = 8; inkCostPo = 150; }
            else if ("raro".equalsIgnoreCase(tattooRarity)) { dc = 16; hours = 16; inkCostPo = 500; }
            else if ("muito raro".equalsIgnoreCase(tattooRarity)) { dc = 18; hours = 32; inkCostPo = 1500; }
            else if ("lendário".equalsIgnoreCase(tattooRarity)) { dc = 21; hours = 64; inkCostPo = 4000; }

            boolean success = tattooistDexOrToolRoll >= dc;
            o.put("success", success);
            o.put("dc", dc);
            o.put("roll", tattooistDexOrToolRoll);
            o.put("hours_required", hours);
            o.put("ink_cost_po", inkCostPo);
            o.put("message", success ? "A tatuagem mágica foi entalhada com perfeição na pele e brilha com energia arcana!" :
                    "O entalhe falhou em canalizar o fluxo mágico; as tintas foram desperdiçadas parcialmente e é necessário descansar.");
        } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject slot(String nome, String tamanho, int espacos, String desc) throws Exception {
        return new JSONObject().put("slot", nome).put("tamanho", tamanho).put("espacos_consumidos", espacos).put("descricao", desc);
    }

    private static JSONObject tattoo(String nome, String raridade, String slot, boolean attunement, int valorPo, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("raridade", raridade).put("slot_corpo", slot).put("requer_sintonizacao", attunement).put("preco_po", valorPo).put("propriedades", desc);
    }
}
