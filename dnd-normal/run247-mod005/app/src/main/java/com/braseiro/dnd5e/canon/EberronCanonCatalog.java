package com.braseiro.dnd5e.canon;

import com.braseiro.dnd5e.NarrativeContentEngine.Entity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo canônico estruturado de Eberron: Forge of the Artificer (EFA).
 *
 * Abrange:
 * - Casas Dracossinaladas: Cannith, Deneith, Ghallanda, Jorasco, Kundarak, Lyrandar, Medani, Orien, Phiarlan, Sivis, Tharashk, Thuranni, Vadalis.
 * - Tecnologia & Magitecnologia: Forjas de Criação, Dirigíveis Elementais (Elemental Airships), Trens Elétricos (Lightning Rail), Autômatos Warforged.
 * - Locais & Cidades: Sharn (Cidade das Torres), Mournland (Terra Devastada de Cyre), Morgrave University, Argonnessen.
 * - Conflitos & Facções: Inquisitivos de Sharn, O Senhor das Lâminas (Lord of Blades), A Profecia Dracônica.
 */
public final class EberronCanonCatalog {
    private EberronCanonCatalog() {}

    public static List<Entity> all() {
        List<Entity> list = new ArrayList<>();

        // 1. Casas Dracossinaladas Principais
        Map<String, String> cannithRel = new HashMap<>();
        cannithRel.put("rivalry_internal", "cannith_south,cannith_west,cannith_east");
        cannithRel.put("trade_partner", "eberron_house_orien");
        list.add(new Entity(
                "eberron_house_cannith", "EFA", "eberron", "998 YK", "faction",
                "Casa Cannith (Marca da Fabricação)", "House Cannith (Mark of Making)",
                "A casa dracossinalada de engenheiros mágicos, ferreiros e artífices criadores dos Warforged.",
                Arrays.asList(
                        "Portadores da Marca da Fabricação (Mark of Making); produzem itens mágicos, ferramentas e tecnologia para Khorvaire.",
                        "Fraturada após a Última Guerra em três ramos rivais: Cannith Sul (Merrix em Sharn), Cannith Oeste (Jorlanna em Aundair) e Cannith Leste (Zorlan em Karrnath)."
                ),
                Arrays.asList(
                        "Baron Merrix d'Cannith mantém uma Forja de Criação clandestina operando nas profundezas sob Sharn.",
                        "Pesquisam segredos proibidos deixados nas ruínas da Mournland para reativar modelos de guerra experimentais."
                ),
                Arrays.asList("eberron", "house_cannith", "artificer", "dragonmarked", "warforged"),
                cannithRel, "grupo", true
        ));

        Map<String, String> lyrandarRel = new HashMap<>();
        lyrandarRel.put("rival", "eberron_house_orien");
        lyrandarRel.put("technology", "elemental_airships");
        list.add(new Entity(
                "eberron_house_lyrandar", "EFA", "eberron", "998 YK", "faction",
                "Casa Lyrandar (Marca da Tempestade)", "House Lyrandar (Mark of Storm)",
                "Senhores dos mares e dos céus de Khorvaire, portadores da Marca da Tempestade.",
                Arrays.asList(
                        "Operam a frota de galeões elementais e os revolucionários Dirigíveis Elementais (Elemental Airships).",
                        "Controlam o clima agrícola através de contratos de chuva e tempestades com os governos das Cinco Nações."
                ),
                Arrays.asList(
                        "A expansão das rotas de dirigíveis ameaça diretamente o monopólio ferroviário do Trem Elétrico (Lightning Rail) da Casa Orien.",
                        "Navegadores de elite desenvolvem anéis de controle elemental para dominar elementais de fogo e ar de nível superior sem leme."
                ),
                Arrays.asList("eberron", "house_lyrandar", "airships", "storm", "transport"),
                lyrandarRel, "grupo", true
        ));

        Map<String, String> orienRel = new HashMap<>();
        orienRel.put("rival", "eberron_house_lyrandar");
        list.add(new Entity(
                "eberron_house_orien", "EFA", "eberron", "998 YK", "faction",
                "Casa Orien (Marca da Passagem)", "House Orien (Mark of Passage)",
                "Mestres do transporte terrestre e da teletransportação, operando a vasta rede do Trem Elétrico (Lightning Rail).",
                Arrays.asList(
                        "Portadores da Marca da Passagem (Mark of Passage); mantêm estradas de pedras condutoras que cruzam o continente.",
                        "A destruição de Cyre cortou as linhas centrais de transporte, forçando desvios longos e perigosos."
                ),
                Arrays.asList(
                        "Financiam pesquisas secretas para desenvolver portais permanentes de teletransporte instantâneo e superar os dirigíveis de Lyrandar.",
                        "Exploram rotas clandestinas que cruzam a borda da Mournland guardadas por mercenários de Deneith."
                ),
                Arrays.asList("eberron", "house_orien", "lightning_rail", "passage", "transport"),
                orienRel, "grupo", true
        ));

        Map<String, String> deneithRel = new HashMap<>();
        deneithRel.put("allies", "defenders_guild,sentinel_marshals");
        list.add(new Entity(
                "eberron_house_deneith", "EFA", "eberron", "998 YK", "faction",
                "Casa Deneith (Marca da Sentinela)", "House Deneith (Mark of Sentinel)",
                "Fornecedores de segurança militar, guarda-costas de elite e a corporação dos Oficiais da Sentinela (Sentinel Marshals).",
                Arrays.asList(
                        "Mantêm neutralidade rigorosa sob o Édito de Korth, alugando corpos mercenários para todas as nações.",
                        "Os Sentinel Marshals possuem jurisdição internacional para caçar criminosos procurados em qualquer país de Khorvaire."
                ),
                Arrays.asList(
                        "Alguns generais da casa conspiram para unificar Khorvaire sob um regime militar comandado por Deneith.",
                        "Contratam secretamente guerreiros monstruosos de Droaam para tarefas mercenárias não oficiais."
                ),
                Arrays.asList("eberron", "house_deneith", "sentinel", "mercenaries", "law"),
                deneithRel, "grupo", true
        ));

        // 2. Geografia, Cidades e Fenômenos de Eberron
        list.add(new Entity(
                "eberron_sharn_city", "EFA", "eberron", "998 YK", "location",
                "Sharn, a Cidade das Torres", "Sharn, City of Towers",
                "A maior e mais vertical metrópole de Khorvaire, erguendo-se em milhas de torres acima do Rio Dagger.",
                Arrays.asList(
                        "Construída sobre uma zona manifesta do plano Syrania, permitindo que torres alcancem altitudes impossíveis e carruagens aéreas voem.",
                        "Dividida em platôs verticais: Cidade Alta (elites e magnatas), Cidade Média (comércio e artesãos) e Cidade Baixa (trabalhadores, goblins e favelas).",
                        "O submundo é disputado entre gangues como o Boromar Clan (halflings contrabandistas) e Daask (monstros de Droaam)."
                ),
                Arrays.asList(
                        "As profundezas esquecidas sob a Cidade Baixa, As Engrenagens (The Cogs), abrigam forjas ilegais e cultos ao Dragão Abaixo.",
                        "Inquisitivos particulares desvendam assassinatos de herdeiros de casas nas noites chuvosas dos distritos superiores."
                ),
                Arrays.asList("eberron", "sharn", "city_of_towers", "noir", "metropolis"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "eberron_mournland_fog", "EFA", "eberron", "998 YK", "hazard",
                "A Névoa da Terra Devastada (Mournland)", "The Mournland Dead-Gray Mists",
                "A vasta cicatriz necromântica e mágica onde outrora ficava o próspero reino de Cyre, destruído no Dia do Luto (Day of Mourning).",
                Arrays.asList(
                        "Cercada por uma muralha densa e impenetrável de névoa cinzenta espessa.",
                        "Dentro da Mournland, corpos não se decompõem, magias de cura não funcionam naturalmente e feitiços ganham vida própria como aberrações conscientes."
                ),
                Arrays.asList(
                        "A causa real do Dia do Luto permanece o maior mistério não solucionado de Khorvaire.",
                        "Névoas mutantes podem transformar aventureiros em estátuas de vidro se inaladas por períodos prolongados sem proteção arcana."
                ),
                Arrays.asList("eberron", "mournland", "cyre", "day_of_mourning", "mystery", "hazard"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "eberron_eston_creation_forge", "EFA", "eberron", "998 YK", "location",
                "As Forjas de Criação de Eston", "Creation Forges of Eston",
                "Complexos fabris da Casa Cannith agora sepultados na Mournland, onde os primeiros Warforged foram construídos.",
                Arrays.asList(
                        "Declaradas proibidas e desmanteladas pelo Tratado de Thronehold ao fim da Última Guerra.",
                        "Contêm projetos de forjas de titãs arcanos e maquinários de alquimia pesada intactos no silêncio espectral."
                ),
                Arrays.asList(
                        "O Senhor das Lâminas busca peças sobressalentes nas ruínas de Eston para construir uma nova Forja de Criação móvel.",
                        "Guardiões autômatos blindados ainda patrulham as instalações atacando qualquer ser de carne que se aproxime."
                ),
                Arrays.asList("eberron", "creation_forge", "eston", "cannith", "warforged_origin"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "eberron_morgrave_university", "EFA", "eberron", "998 YK", "location",
                "Universidade Morgrave", "Morgrave University",
                "Instituição acadêmica colossal situada em Sharn, célebre por suas expedições a ruínas antigas de Xen'drik e Dhakaan.",
                Arrays.asList(
                        "Financia arqueólogos, caçadores de relíquias e magos para recuperar artefatos arcanos em todo o mundo.",
                        "Seu lema público proclama iluminação e estudo, embora muitos a vejam como fachada para contrabandistas refinados."
                ),
                Arrays.asList(
                        "Coleções no cofre subterrâneo da universidade contêm tábuas com fragmentos perigosos da Profecia Dracônica.",
                        "Professores rivais competem ferozmente para publicar descobertas sobre civilizações gigantes de Xen'drik."
                ),
                Arrays.asList("eberron", "morgrave", "university", "archaeology", "sharn"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        // 3. NPCs e Figuras Centrais
        list.add(new Entity(
                "eberron_lord_of_blades", "EFA", "eberron", "998 YK", "npc",
                "O Senhor das Lâminas", "The Lord of Blades",
                "Líder messiânico dos Warforged renegados, operando a partir do coração sombrio da Mournland.",
                Arrays.asList(
                        "Prega a supremacia dos forjados bélicos sobre as raças biológicas e a criação de uma nação livre para sua espécie.",
                        "Sua fortaleza é composta por carcaças de trens elétricos caídos e metal fundido nas ruínas de Cyre."
                ),
                Arrays.asList(
                        "Acredita-se que ele possua esquemas para criar novos warforged sem necessidade de autorização da Casa Cannith.",
                        "Envia emissários para Sharn para resgatar operários forjados submetidos a servidão por contrato."
                ),
                Arrays.asList("eberron", "lord_of_blades", "warforged_leader", "mournland", "zealot"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        // 4. Cosmologia e Profecia
        list.add(new Entity(
                "eberron_draconic_prophecy", "EFA", "eberron", "998 YK", "misc",
                "A Profecia Dracônica", "The Draconic Prophecy",
                "A teia cósmica viva do destino que se manifesta nas constelações, marcas de dragão e cataclismos de Eberron.",
                Arrays.asList(
                        "Estudada há eons pelos dragões sábios de Argonnessen e pelos demônios Senhores Supremos (Overlords) aprisionados.",
                        "Não prevê um futuro imutável, mas condições condicionais ('Se o herdeiro de sangue trair o trono, o selo romperá')."
                ),
                Arrays.asList(
                        "Agentes dos dragões (A Câmara / The Chamber) manipulam eventos em Khorvaire para impedir que versos sombrios se cumpram.",
                        "Marcas aberrantes que surgem inesperadamente em mortais costumam alterar o curso previsto da profecia."
                ),
                Arrays.asList("eberron", "prophecy", "dragons", "destiny", "overlords"),
                Collections.<String, String>emptyMap(), "mestre", false
        ));

        return Collections.unmodifiableList(list);
    }
}
