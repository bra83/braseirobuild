package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor compilado para geração e gestão de tabernas, estalagens, bordéis e antros,
 * baseado nos suplementos "Tabernas, bordéis e afins" e "Taverns, Inns, and Taprooms; In Hell".
 */
public final class TavernBrothelCatalogEngine {
    public static final String BOOK = "Tabernas bordeis e afins.pdf";

    private TavernBrothelCatalogEngine() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", "tavern_brothel_rules");
            o.put("book", BOOK);
            o.put("title", "Tabernas, Bordéis e Afins");
            o.put("category", "Vida Urbana, Lazer e Negócios");
            o.put("version", "1.0.0");
        } catch (Exception ignored) {}
        return o;
    }

    public static JSONArray drinksCatalog() {
        JSONArray a = new JSONArray();
        try {
            a.put(drink("Cerveja Escura de Ferro Velho", "Cerveja Anã", 4, 10, "Cerveja densa e amarga; concede +1 PV temporário por 1 hora."));
            a.put(drink("Hidromel Dourado de Faéria", "Hidromel Élfico", 15, 12, "Doce e floral; concede vantagem em testes de Atuação por 2 horas."));
            a.put(drink("Vinho Sangue de Wyvern", "Vinho Raro", 50, 14, "Cor rubi profunda; concede resistência a veneno por 4 horas após o teste de vigor."));
            a.put(drink("Aguardente de Fogo do Pântano", "Destilado", 8, 13, "Queima a garganta; o bebedor não sente frio por 6 horas."));
            a.put(drink("Uísque de Enxofre de Avernus", "Bebida Infernal", 30, 15, "Fumaça tênue sobe do copo; concede resistência a fogo por 1 hora, mas exige salvaguarda contra queimação."));
            a.put(drink("Cidra do Pomar Prateado", "Bebida Suave", 2, 8, "Cidra de maçãs frescas, favorita de viajantes e menestréis."));
            a.put(drink("Grogue do Marinheiro Fantasma", "Rum Pirata", 5, 11, "Forte e salgado; concede vantagem contra a condição amedrontado por 1 hora."));
            a.put(drink("Absinto da Bruxa Noturna", "Elixir Alucinógeno", 40, 16, "Permite visões etéreas sutis; vantagem em Percepção, mas desvantagem em iniciativa por 2 horas."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray lodgingAndServices() {
        JSONArray a = new JSONArray();
        try {
            a.put(service("Cama Coletiva no Chão (Celeiro/Salão)", "Acomodação", 7, "Um canto aquecido com palha fresca perto da lareira."));
            a.put(service("Quarto Simples Individual", "Acomodação", 50, "Cama de madeira, ferrolho na porta e bacia de água fresca."));
            a.put(service("Suíte Aristocrática com Banho Quente", "Acomodação", 200, "Cama com dossel, toalhas de linho, lareira privativa e perfume de lavanda."));
            a.put(service("Companhia de Conversa & Massagem Relaxante", "Serviço de Bordel", 100, "Alivia exaustão acumulada e relaxa a musculatura após viagens longas."));
            a.put(service("Suíte de Seda Escarlate com Acompanhante de Luxo", "Serviço de Bordel", 300, "Acomodação refinada com privacidade absoluta e banquetes eróticos."));
            a.put(service("Banho Termal Mineral com Óleos Aromáticos", "Bem-Estar", 50, "Cura dores nas articulações; concede +1d4 na próxima iniciativa do dia."));
            a.put(service("Mesa Privativa com Cortinas para Reuniões Secretas", "Serviço", 30, "Garçons juramentados a não ouvir o que se passa na mesa."));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray gamesOfChance() {
        JSONArray a = new JSONArray();
        try {
            a.put(game("Dados de Dragão", "3d6 contra a banca; triplos pagam 5x a aposta.", 10));
            a.put(game("Cartas dos Trapaceiros", "Disputa de Enganação ou Prestidigitação vs Intuição da mesa.", 25));
            a.put(game("Círculo de Braço de Ferro", "Disputa de testes resistidos de Força (Atletismo).", 15));
            a.put(game("Roleta Infernal dos Nove Infernos", "Apostas em 9 círculos; o círculo de Avernus dobra a aposta.", 50));
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONArray tavernRumors() {
        JSONArray a = new JSONArray();
        try {
            a.put("Dizem que o porão da taverna vizinha foi construído sobre uma antiga catacumba cheia de ouro.");
            a.put("Um navio de bandeira negra foi visto rondando os recifes ao luar na maré baixa.");
            a.put("A cortesã mais cobiçada do bordel é na verdade uma espiã a serviço de um lorde rival.");
            a.put("Um mago viajou para as 12 Torres Peculiares há uma quinzena e não foi mais visto.");
            a.put("Os guardas da cidade estão aceitando subornos para ignorar o contrabando de tintas de tatuagem mística.");
            a.put("Na floresta de Evernura, as árvores mudam de lugar quando ninguém está olhando.");
        } catch (Exception ignored) {}
        return a;
    }

    public static JSONObject generateEstablishment(String type, String quality) {
        JSONObject o = new JSONObject();
        try {
            String[] namesPre = {"O Bode", "A Serpente", "O Corvo", "O Dragão", "A Sereia", "O Machado", "A Rosa", "O Cálice"};
            String[] namesPost = {"Saltitante", "Dourado", "Cego", "Embriagado", "Prateado", "Quebrado", "Escarlate", "Sombrio"};
            int hash = Math.abs((type + quality + System.currentTimeMillis()).hashCode());
            String name = namesPre[hash % namesPre.length] + " " + namesPost[(hash / 8) % namesPost.length];

            o.put("nome", name);
            o.put("tipo", type == null || type.isEmpty() ? "Taverna e Estalagem" : type);
            o.put("qualidade", quality == null || quality.isEmpty() ? "Média" : quality);
            o.put("seguranca", "Moderada com leões-de-chácara experientes");
            o.put("atmosfera", "Fumaça de cachimbo, cheiro de carne assada com cerveja e som de alaúde ao fundo.");
            o.put("cardapio_bebidas", drinksCatalog());
            o.put("servicos_disponiveis", lodgingAndServices());
            o.put("jogos", gamesOfChance());
        } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject drink(String nome, String tipo, int precoPc, int cdVigor, String efeito) throws Exception {
        return new JSONObject().put("nome", nome).put("tipo", tipo).put("preco_pc", precoPc).put("cd_vigor", cdVigor).put("efeito", efeito);
    }

    private static JSONObject service(String nome, String tipo, int precoPc, String desc) throws Exception {
        return new JSONObject().put("nome", nome).put("tipo", tipo).put("preco_pc", precoPc).put("descricao", desc);
    }

    private static JSONObject game(String nome, String regras, int apostaMinimaPc) throws Exception {
        return new JSONObject().put("nome", nome).put("regras", regras).put("aposta_minima_pc", apostaMinimaPc);
    }
}
