package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo e gerador estruturado de opções de NPC baseado nos livros:
 * - "1000 Memorable NPCs for Any Roleplaying"
 * - "The Game Master's Book of NPCs"
 * - "Statblock Compendium"
 * - "The Game Master's Book of Random Encounters"
 * - "Catálogo 300 NPCs PT-BR" (npc_personalities_300.json)
 */
public final class NpcBookCatalogEngine {

    private NpcBookCatalogEngine() {}

    public static String[] getOriginBooks() {
        return new String[]{
                "The Game Master's Book of NPCs",
                "1000 Memorable NPCs for Any Roleplaying",
                "Statblock Compendium",
                "The Game Master's Book of Random Encounters",
                "Catálogo 300 NPCs PT-BR"
        };
    }

    public static String[] getArchetypes() {
        return new String[]{
                "Taverneiro & Estalajadeiro",
                "Mercador & Comerciante Itinerante",
                "Guarda & Patrulheiro Urbano",
                "Nobre Intrigante & Aristocrata",
                "Ladino, Espião & Contrabandista",
                "Mago, Conjurador & Alquimista",
                "Sacerdote, Clérigo & Cultista Oculto",
                "Marinheiro, Corsário & Lobo do Mar",
                "Caçador, Rastreador & Eremita dos Ermos",
                "Ferreiro, Artesão & Mestre de Ofício",
                "Menestrel, Artista & Cortesã de Bordel",
                "Cavaleiro Juramentado & Veterano de Guerra"
        };
    }

    public static String[] getAncestries() {
        return new String[]{
                "Humano",
                "Anão da Colina",
                "Anão da Montanha",
                "Alto Elfo",
                "Elfo da Floresta",
                "Drow (Elfo Negro)",
                "Meio-Elfo",
                "Meio-Orc",
                "Halfling Pés-Leves",
                "Draconato",
                "Gnomo das Rochas",
                "Tiefling"
        };
    }

    public static String[] getSocialClasses() {
        return new String[]{
                "Miserável / Desprovido",
                "Pobre / Plebeu da Cidade",
                "Modesto / Trabalhador Assalariado",
                "Confortável / Pequeno Burguês",
                "Rico / Mercador de Prestígio",
                "Aristocrata / Nobreza Imperial"
        };
    }

    public static String[] getProfessions() {
        return new String[]{
                "Estalajadeiro",
                "Taverneiro",
                "Boticário & Herbalista",
                "Ferreiro Armeiro",
                "Carpinteiro Naval",
                "Capitão de Navio Mercante",
                "Bucaneiro Corsário",
                "Guarda da Patrulha",
                "Capitão da Guarda Municipal",
                "Ladino Espião",
                "Contrabandista dos Becos",
                "Joalheiro & Cambista",
                "Mascate Itinerante",
                "Mago Alquimista",
                "Sacerdote de Templo",
                "Cultista Clandestino",
                "Cozinheiro Especialista",
                "Pescador de Alto-Mar",
                "Tatuador Místico",
                "Cortesã de Salão Privativo",
                "Menestrel Andarilho",
                "Caçador de Recompensas",
                "Guia Florestal de Evernura",
                "Erudito de Biblioteca Arcana"
        };
    }

    public static String[] getQuirks() {
        return new String[]{
                "Mastiga incessantemente folhas aromáticas de hortelã para disfarçar o cheiro de fumaça.",
                "Possui um papagaio sarcástico no ombro que repete e zomba da última palavra dita.",
                "Fala em sussurros cautelosos, olhando frequentemente por cima de ambos os ombros.",
                "Usa anéis pesados de latão em todos os dedos e bate na mesa com ritmo ao enfatizar ideias.",
                "Exibe uma cicatriz profunda em forma de raio que desce da testa até o queixo.",
                "Coleciona moedas antigas falsificadas e ri com um pigarro metálico e ruidoso.",
                "Carrega um monóculo de vidro rachado preso por cordão que ajusta nervosamente a cada minuto.",
                "Tem as pontas dos dedos manchadas de tinta mágica púrpura que emite uma leve fosforescência.",
                "Usa um chapéu de abas largas desbotado com uma pena de grifo chamuscada.",
                "Nunca olha diretamente nos olhos de ninguém por mais de dois segundos seguidos.",
                "Tem um dente de ouro polido com um minúsculo símbolo rúnico de proteção gravado.",
                "Carrega uma adaga de osso com a qual descasca maçãs ou talha madeira distraidamente.",
                "Possui uma risada rouca e estrondosa que surpreende a todos pela intensidade.",
                "Cheira fortemente a tabaco de cachimbo adocicado, canela e ervas secas.",
                "Tem um braço mecânico articulado de latão cujas engrenagens tilintam suavemente ao se mover.",
                "Usa botas de couro de crocodilo decoradas com dentes de tubarão dos mares do sul.",
                "Veste uma capa de veludo carmesim puída com remendos bem-feitos de couro cru.",
                "Possui olhos de cores diferentes: o olho esquerdo é azul gélido e o direito é âmbar brilhante.",
                "Exibe uma tatuagem mística cintilante de serpente no antebraço que parece se mover levemente.",
                "Gesticula com elegância dramática usando as mãos, como se regesse uma ópera invisível."
        };
    }

    public static String[] getSecrets() {
        return new String[]{
                "É um fugitivo procurado em uma província vizinha por roubo de carga nobre.",
                "Trabalha secretamente como espião informante para o sindicato do crime local.",
                "Guarda uma chave rúnica ancestral herdada que abre o portal de uma das 12 Torres Peculiares.",
                "Está endividado com agiotas perigosos e precisa pagar 300 PO antes da próxima lua cheia.",
                "É o herdeiro deserdado de uma influente casa aristocrática que teve o título usurpado.",
                "Pertence secretamente a um culto místico que realiza rituais nas noites sem estrelas.",
                "Carrega uma maldição ancestral que faz animais domésticos se arrepiarem em sua presença.",
                "Esconde um mapa de tesouro das ilhas de South Sea costurado no forro secreto de sua jaqueta.",
                "Sabe a verdadeira identidade do assassino do antigo capitão da guarda da cidade.",
                "É um veterano de guerra que presenciou uma traição militar acobertada pelo governo local."
        };
    }

    public static String[] getAdventureHooks() {
        return new String[]{
                "Pede ajuda urgente para resgatar um parente sequestrado por bandidos na estrada norte.",
                "Oferece hospedagem gratuita e cerveja em troca de dedetizar um ninho de feras no sótão.",
                "Procura aventureiros para escoltar uma carruagem com carga sigilosa até Porto Livre de Tortuga.",
                "Precisa de ervas raras que só nascem no Bosque dos Lamentos em Evernura e paga 250 PO.",
                "Afirma ter visto luzes e espectros no farol abandonado e teme pela segurança do cais.",
                "Deseja contratar a aplicação de uma tatuagem mística rara e busca as tintas adequadas.",
                "Suspeita que o prefeito e o conselho municipal estejam sendo chantageados por contrabandistas.",
                "Procura guarda-costas discretos para garantir sua sobrevivência durante uma partilha de lucros.",
                "Possui pistas sobre a entrada de uma masmorra cruel sob as ruínas da fortaleza velha.",
                "Quer contratar o grupo para recuperar uma relíquia roubada antes que seja vendida no mercado negro."
        };
    }

    public static String[] getStatblockTemplates() {
        return new String[]{
                "Plebeu (CR 0 | PV 4 | CA 10 | Porrete +2, 1d4)",
                "Guarda da Cidade (CR 1/8 | PV 11 | CA 16 | Lança +3, 1d6+1)",
                "Bandido Valentão (CR 1/8 | PV 11 | CA 12 | Cimitarra +3, 1d6+1)",
                "Espião Astuto (CR 1 | PV 27 | CA 12 | Espada Curta +4, 1d6+2 + 2d6 Furtivo)",
                "Mago Aprendiz (CR 1/4 | PV 9 | CA 10 | Mísseis Mágicos, Rajada de Fogo +4)",
                "Sacerdote de Templo (CR 2 | PV 27 | CA 13 | Curar Ferimentos, Espírito Guardião)",
                "Capitão Veterano (CR 3 | PV 58 | CA 17 | Espada Longa +5 [2 ataques], 1d8+3)",
                "Corsário Bucaneiro (CR 2 | PV 45 | CA 14 | Pistola +4, 1d10 / Sabre +4, 1d6+2)",
                "Mago Arcano da Corte (CR 6 | PV 40 | CA 12 | Bola de Fogo, Cone de Frio, Escudo)",
                "Assassino das Sombras (CR 8 | PV 78 | CA 15 | Lâmina Envenenada +6, 1d6+3 + 7d6 Veneno)"
        };
    }

    public static JSONObject buildNpcMemory(String name, String originBook, String archetype, String ancestry,
                                            String socialClass, String profession, String quirk,
                                            String secret, String hook, String statblock, String tokenUri) {
        JSONObject npc = new JSONObject();
        try {
            String cleanName = name == null || name.trim().isEmpty() ? "NPC Sem Nome" : name.trim();
            npc.put("name", cleanName);
            npc.put("stable_id", "npc_" + Math.abs(cleanName.hashCode()) + "_" + System.currentTimeMillis());
            npc.put("summary", archetype + " (" + ancestry + ", " + profession + "). Origem: " + originBook + ". Classe social: " + socialClass + ".");
            npc.put("visual_description", ancestry + " " + profession + ". " + quirk);
            npc.put("goals", "Almeja estabilidade e seus próprios interesses. " + hook);
            npc.put("secrets", secret);
            npc.put("attitude", "neutra");
            npc.put("relationship", "Recém-conhecido dos aventureiros");
            npc.put("knowledge", "Conhece detalhes sobre " + profession + ", o comércio local e rumores da região.");
            npc.put("token_uri", tokenUri == null ? "" : tokenUri.trim());
            npc.put("active", 1);
            npc.put("last_seen", "Assentamento Local");
            npc.put("stats_json", new JSONObject().put("template", statblock).put("status", "vivo").toString());
            npc.put("updated_at", System.currentTimeMillis());
        } catch (Exception ignored) {}
        return npc;
    }
}
