package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor compilado para geração e regras de masmorras brutais e armadilhas letais
 * baseado nos suplementos "HB Masmorras Cruéis" e "Masmorras crueis".
 */
public final class CruelDungeonEngine {
    public static final String BOOK = "HB Masmorras Cruéis.pdf";

    private CruelDungeonEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "cruel_dungeons_rules");
            o.put("book", BOOK);
            o.put("title", "Masmorras Cruéis");
            o.put("category", "Perigos Letais, Armadilhas e Tortura Subterrânea");
            o.put("version", "1.0.0");
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray trapCatalog() {
        JSONArray a = new JSONArray();
        try {
            a.put(trap("Pêndulo Decapitador de Prata Negra", "Mecânica", 15, 16, "Placa de pressão na soleira da porta.",
                    "Lâmina semicircular com peso de 300kg balança do teto. Ataque +8, causa 4d10 de dano cortante. Em acerto crítico, decepa um membro."));

            a.put(trap("Fosso de Espinhos Farpados com Lodo Ácido", "Ambiental", 14, 15, "Piso falso de tábuas finas camuflado com poeira.",
                    "Queda de 9 metros (3d6 contundente) sobre estacas de ferro afiadas (3d8 perfurante) banhadas em ácido cáustico (+2d6 ácido por rodada)."));

            a.put(trap("Câmara do Teto Esmagador de Granito", "Sala Completa", 16, 17, "Retirada de um artefato do pedestal central sem contrapeso.",
                    "As portas de ferro caem. O teto desce 60cm por rodada. Em 5 rodadas esmaga quem estiver dentro (10d10 contundente ou morte instantânea)."));

            a.put(trap("Gás Venenoso da Asfixia Sangrenta", "Mágica / Alquímica", 15, 14, "Girar de maçaneta em falso.",
                    "Válvulas disfarçadas expelem névoa avermelhada. Teste de Constituição CD 15 ou sofre 4d6 de dano de veneno e fica cego por 1 hora."));

            a.put(trap("Corredor das Paredes de Lâminas Rotativas", "Mecânica Rápida", 17, 18, "Fio de tropeço de tripa de ovelha quase invisível.",
                    "Lâminas giratórias saem das canaletas laterais cobrindo 18 metros. Salvaguarda de Destreza CD 16 para se jogar no chão, ou 6d6 cortante."));

            a.put(trap("Espelho Sugador de Almas", "Mágica / Arcana", 18, 19, "Olhar diretamente para o reflexo a menos de 3 metros.",
                    "Salvaguarda de Carisma CD 16 ou a alma do alvo é aprisionada no vidro enquanto seu corpo cai em estado catatônico."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray cruelAtmospheres() {
        JSONArray a = new JSONArray();
        try {
            a.put("Gotas de condensação fria caem sobre o pescoço em intervalos irregulares, soando como passos atrás de você.");
            a.put("O cheiro de carne queimada antiga e sangue coagulado impregna a argamassa das pedras cinzentas.");
            a.put("Grilhões vazios com pedaços de ossos ressecados pendem do teto rangendo com a corrente de ar fétida.");
            a.put("Riscos desesperados nas paredes contam dias, até que terminam em arranhões verticais de unhas quebradas.");
            a.put("Um zumbido sub-audível contínuo faz os dentes vibrarem e impede qualquer descanso tranquilo.");
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONObject generateDungeonRoom(int dangerLevel) {
        JSONObject room = new JSONObject();
        try {
            JSONArray traps = trapCatalog();
            JSONObject selectedTrap = traps.optJSONObject(Math.abs(dangerLevel + (int)System.currentTimeMillis()) % traps.length());
            JSONArray atmos = cruelAtmospheres();
            String selectedAtmo = atmos.optString(Math.abs(dangerLevel) % atmos.length());

            room.put("nivel_perigo", dangerLevel <= 0 ? 1 : dangerLevel);
            room.put("atmosfera", selectedAtmo);
            room.put("armadilha_instalada", selectedTrap);
            room.put("condicao_descanso", "Descanso curto possível somente com sentinela atenta; descanso longo impossível devido ao terror psicológico.");
        } catch (Exception ignored) {}
        return room;
    }

    private static JSONObject trap(String nome, String tipo, int cdPercepcao, int cdDesarmar, String gatilho, String efeito) throws Exception {
        return new JSONObject().put("nome", nome).put("tipo", tipo).put("cd_percepcao", cdPercepcao).put("cd_desarmar", cdDesarmar).put("gatilho", gatilho).put("efeito", efeito);
    }
}
