package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor compilado para as 12 Torres Peculiares baseado no suplemento "12 Peculiar Towers".
 * Fornece a estrutura modular de cada torre, perigos, quebra-cabeças, guardiões e tesouros.
 */
public final class PeculiarTowersEngine {
    public static final String BOOK = "12 peculiar towers.pdf";

    private PeculiarTowersEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "peculiar_towers_rules");
            o.put("book", BOOK);
            o.put("title", "12 Torres Peculiares");
            o.put("category", "Masmorras Verticais e Locais de Aventura");
            o.put("version", "1.0.0");
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray listTowers() {
        JSONArray a = new JSONArray();
        try {
            a.put(tower(1, "Torre do Vidro Cantante", "Estrutura espiral de quartzo e vidro acústico que ressoa com o vento.",
                    "Vibração sônica contínua; magias de trovão têm alcance dobrado; quebra de frascos ao falhar em salvaguarda de Constituição CD 13.",
                    "Autômatos de Cristal e Harpias de Vidro", "Diapasão de Ressonância e Anel de Isolamento Acústico"));

            a.put(tower(2, "Espiral do Relógio Reverso", "Mecanismos gigantes de engrenagens de latão onde o tempo flui em espasmos retroativos.",
                    "No início de cada rodada, criaturas rolam 1d6; em um 1, são puxadas para a posição em que estavam na rodada anterior.",
                    "Golens Mecânicos e Guardiões Crono-Engrenados", "Ampulheta da Segunda Chance"));

            a.put(tower(3, "Torre Flutuante de Magnetita", "Blocos de pedra magnética preta suspensos em gravidade variável.",
                    "Criaturas portando armaduras ou armas de metal pesado sofrem atração ou repulsão magnética (testes de Força CD 14).",
                    "Horrores de Ferro e Elementais Magnéticos", "Escudo do Vetor Gravitacional"));

            a.put(tower(4, "Torre da Biblioteca Sussurrante", "Estantes infinitas de pergaminhos vivos que murmuram segredos proibidos.",
                    "A cada descanso curto, rola-se Sabedoria CD 13 contra ganho de insanidade passageira ou vislumbre profético.",
                    "Espectros da Tinta e Guardiões de Papiro", "Tomo dos Feitiços Esquecidos"));

            a.put(tower(5, "Ossuário do Necromante Verde", "Paredes de ossos fundidos entrelaçados com trepadeiras espinhosas carnívoras.",
                    "Necromancia e magia da natureza fundidas; plantas venenosas drenam vida de mortos-vivos e vivos indistintamente.",
                    "Zumbis Fúngicos e Dríade Corrompida", "Cajado das Raízes Fúnebres"));

            a.put(tower(6, "Torre do Farol Espectral", "Antigo farol costeiro cuja luz ilumina o Plano Etéreo.",
                    "O feixe de luz revela criaturas invisíveis e planos coexistentes, atraindo fantasmas furiosos do mar.",
                    "Espectros de Marinheiros e Fantasma do Faroleiro", "Lampião da Luz Verdadeira"));

            a.put(tower(7, "Torre Espelhada dos Ecos", "Câmaras forradas de espelhos prateados que duplicam ações e feitiços.",
                    "Qualquer magia lançada na sala central cria um reflexo espelhado que dispara uma versão enfraquecida em alvo aleatório.",
                    "Cópias Espelhadas Sombrias dos Aventureiros", "Espelho de Bolso da Distorção"));

            a.put(tower(8, "Forja dos Quatro Elementos", "Chaminés incandescentes alimentadas por fendas para os planos elementais.",
                    "Zonas alternadas de calor extremo, tempestades elétricas, inundações ferventes e desabamentos de rocha.",
                    "Elementais Menores e Salamandras da Forja", "Martelo do Artífice Primordial"));

            a.put(tower(9, "Torre das Raízes Astrais", "Tronco oco de uma árvore colossal cujos galhos superiores tocam as estrelas.",
                    "Quedas não causam dano fatal; os galhos flutuantes funcionam como plataformas planares.",
                    "Fadas Guardiãs e Falcões Astrais", "Semente da Árvore-Mundo"));

            a.put(tower(10, "Espiral do Alquimista Cego", "Laboratório vertical repleto de caldeirões borbulhantes, vapores coloridos e ácidos.",
                    "Gases alquímicos aleatórios a cada andar (sono, riso histérico, névoa ácida ou fumaça densa).",
                    "Homúnculos Mutantes e Lodo Alquímico Corrosivo", "Frasco Infinito de Reagentes"));

            a.put(tower(11, "Menagerie Enjaulada dos Horrores", "Zoológico místico vertical com jaulas mágicas cujos selos estão rachando.",
                    "Jaulas com trincas arcanas que podem se romper durante combate por dano em área.",
                    "Quimeras Jovens, Mantícoras e Bestas Deslocadoras", "Chave Mestra de Contenção"));

            a.put(tower(12, "Torre do Vórtice Abissal", "Monólito de pedra negra construído sobre uma fenda perpétua para o Abismo.",
                    "Corrupção planar; criaturas boas sentem náusea constante; ventos ascendentes uivam lamentos de almas perdidas.",
                    "Demônios Menores, Quasits e Cultistas do Vórtice", "Adaga do Vórtice Negro"));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONObject getTower(int id) {
        JSONArray all = listTowers();
        for (int i = 0; i < all.length(); i++) {
            JSONObject t = all.optJSONObject(i);
            if (t != null && t.optInt("numero", -1) == id) return t;
        }
        return new JSONObject();
    }

    private static JSONObject tower(int num, String nome, String desc, String perigo, String monstros, String tesouro) throws Exception {
        return new JSONObject().put("numero", num).put("nome", nome).put("descricao", desc).put("perigo_ambiental", perigo).put("guardioes", monstros).put("tesouro_chave", tesouro);
    }
}
