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

        // --- EXPANSÃO LOTE B: EBERRON FORGE OF THE ARTIFICER (EFA) ---

        // 5. Demais Casas Dracossinaladas Canônicas
        Map<String, String> kundarakRel = new HashMap<>();
        kundarakRel.put("rival", "eberron_house_deneith");
        kundarakRel.put("banking_partner", "eberron_house_sivis");
        list.add(new Entity(
                "eberron_house_kundarak", "EFA", "eberron", "998 YK", "faction",
                "Casa Kundarak (Marca da Proteção)", "House Kundarak (Mark of Warding)",
                "A casa dos anões banqueiros e mestres da segurança mágica, operando os cofres mais impenetráveis de Khorvaire.",
                Arrays.asList(
                        "Portadores da Marca da Proteção (Mark of Warding); controlam as finanças internacionais e a emissão de cartas de crédito.",
                        "Seus cofres em Drakkenheim e Sharn são selados por glifos e fechaduras arcanas lendárias imunes a ladinos comuns."
                ),
                Arrays.asList(
                        "Mantêm prisões secretas de segurança máxima (Dreadhold) esculpidas em rochedos oceânicos para aprisionar aberrações e nobres caídos.",
                        "Pesquisam runas antigas de ferro dos anões de Mror para construir cofres de isolamento planar."
                ),
                Arrays.asList("eberron", "house_kundarak", "warding", "banking", "dwarves", "vaults"),
                kundarakRel, "grupo", true
        ));

        Map<String, String> sivisRel = new HashMap<>();
        sivisRel.put("ally", "eberron_house_kundarak");
        list.add(new Entity(
                "eberron_house_sivis", "EFA", "eberron", "998 YK", "faction",
                "Casa Sivis (Marca da Escrita)", "House Sivis (Mark of Scribing)",
                "A corporação de notários, tradutores, mensageiros e escribas gnomos originária de Zilargo.",
                Arrays.asList(
                        "Portadores da Marca da Escrita (Mark of Scribing); operam as Estações da Mensagem (Speaking Stones) que transmitem palavras instantâneas entre cidades.",
                        "Autenticam documentos legais oficiais, tratados diplomáticos e contratos entre todas as nações e casas."
                ),
                Arrays.asList(
                        "Mestres da criptografia de Sivis monitoram mensagens diplomáticas interceptadas em busca de indícios da Profecia Dracônica.",
                        "Mantêm o Trust, a temida rede de inteligência e espionagem silenciosa do governo de Zilargo."
                ),
                Arrays.asList("eberron", "house_sivis", "scribing", "gnomes", "communication", "diplomacy"),
                sivisRel, "grupo", true
        ));

        list.add(new Entity(
                "eberron_house_jorasco", "EFA", "eberron", "998 YK", "faction",
                "Casa Jorasco (Marca da Cura)", "House Jorasco (Mark of Healing)",
                "A corporação dos halflings curandeiros, cirurgiões e boticários que mantém o monopólio da medicina em Khorvaire.",
                Arrays.asList(
                        "Portadores da Marca da Cura (Mark of Healing); administram casas de cura em todas as capitais das Cinco Nações.",
                        "Neutralizam pestes raras, doenças mágicas e prestam socorro médico profissional sob cobrança estrita de taxas padronizadas."
                ),
                Arrays.asList(
                        "Recusam categoricamente prestar tratamento a quem não puder pagar a taxa contratual, gerando atritos com ordens caridosas.",
                        "Investigam remédios proibidos e mutações biológicas surgidas nas fronteiras da Mournland."
                ),
                Arrays.asList("eberron", "house_jorasco", "healing", "halflings", "medicine"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "eberron_house_ghallanda", "EFA", "eberron", "998 YK", "faction",
                "Casa Ghallanda (Marca da Hospitalidade)", "House Ghallanda (Mark of Hospitality)",
                "Halflings nobres que operam a vasta rede de estalagens, tavernas e santuários de descanso de Khorvaire.",
                Arrays.asList(
                        "Portadores da Marca da Hospitalidade (Mark of Hospitality); garantem comida fresca, alojamento seguro e refúgio neutro.",
                        "O símbolo do cão dourado em uma pousada assegura que as leis de paz e proteção serão rigorosamente mantidas."
                ),
                Arrays.asList(
                        "Suas suítes mais caras utilizam rituais de mansão magnífica e santuário privado imunes a escuta telepática.",
                        "Servem frequentemente como mediadores discretos para encontros secretos entre governantes inimigos."
                ),
                Arrays.asList("eberron", "house_ghallanda", "hospitality", "halflings", "inn", "refuge"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "eberron_house_tharashk", "EFA", "eberron", "998 YK", "faction",
                "Casa Tharashk (Marca do Encontro)", "House Tharashk (Mark of Finding)",
                "Clãs de humanos e meio-orcs dos Pântanos das Sombras (Shadow Marches) especializados em prospecção e rastreamento.",
                Arrays.asList(
                        "Portadores da Marca do Encontro (Mark of Finding); mineram e refinam os cobiçados Dragonshards de Eberron e Khyber.",
                        "Alugam inquisitivos de faro aguçado e prospectores de minérios para expedições perigosas em terras ermas."
                ),
                Arrays.asList(
                        "Mediaram a contratação de tropas monstruosas (orcs, gnolls e ogros) de Droaam durante os anos finais da Última Guerra.",
                        "Xamãs da casa investigam cultos aos Senhores da Loucura (Daelkyr) selados sob os pântanos ocidentais."
                ),
                Arrays.asList("eberron", "house_tharashk", "finding", "half_orcs", "dragonshards", "shadow_marches"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "eberron_house_vadalis", "EFA", "eberron", "998 YK", "faction",
                "Casa Vadalis (Marca do Adestramento)", "House Vadalis (Mark of Handling)",
                "Criadores e geneticistas arcanos dos Reinos Eldeen que desenvolvem montarias melhoradas e animais magebred.",
                Arrays.asList(
                        "Portadores da Marca do Adestramento (Mark of Handling); fornecem cavalos superiores, grifos e bestas de carga para exércitos.",
                        "Suas linhagens de animais mágicos possuem resistência física acelerada e inteligência instintiva superior."
                ),
                Arrays.asList(
                        "Pesquisas secretas exploram o aprimoramento genético de seres humanoides em violação aos acordos de Thronehold.",
                        "Bioterapeutas de Vadalis tentam domesticar monstruosidades aladas capturadas nas montanhas de Droaam."
                ),
                Arrays.asList("eberron", "house_vadalis", "handling", "magebred", "mounts", "beasts"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "eberron_house_medani", "EFA", "eberron", "998 YK", "faction",
                "Casa Medani (Marca da Detecção)", "House Medani (Mark of Detection)",
                "Inquisitivos meio-elfos de Breland que formam a Guilda de Alerta (Warning Guild) para prever e neutralizar ameaças.",
                Arrays.asList(
                        "Portadores da Marca da Detecção (Mark of Detection); especializados em segurança preventiva e contra-espionagem.",
                        "Seus agentes detectam venenos, emboscadas e ilusões antes que seus clientes nobres sejam atacados."
                ),
                Arrays.asList(
                        "Competem amargamente contra os espiões das casas Phiarlan e Thuranni nas cortes de Sharn e Wroat.",
                        "Possuem métodos analíticos dedutivos que rivalizam com as maiores mentes forenses de Khorvaire."
                ),
                Arrays.asList("eberron", "house_medani", "detection", "half_elves", "inquisitives", "security"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "eberron_house_phiarlan_thuranni", "EFA", "eberron", "998 YK", "faction",
                "Casas Phiarlan & Thuranni (Marcas da Sombra)", "Houses Phiarlan & Thuranni (Marks of Shadow)",
                "As duas casas élficas rivais de artistas, dançarinos e músicos que controlam a espionagem e assassinatos de elite.",
                Arrays.asList(
                        "Portadores da Marca da Sombra (Mark of Shadow); dividiram-se após a 'Cisma da Sombra' em uma sangrenta guerra civil interna.",
                        "Phiarlan comanda o Entretenimento dos Cinco Reinos, enquanto Thuranni atua com assassinos de sombras desavergonhados."
                ),
                Arrays.asList(
                        "Cada trupe teatral itinerante de elfos é simultaneamente uma célula de agentes de inteligência e espionagem.",
                        "Ambas as casas negociam segredos de estado com os governos de Breland, Thrane, Karrnath e Aundair."
                ),
                Arrays.asList("eberron", "house_phiarlan", "house_thuranni", "shadow", "elves", "espionage", "assassins"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        // 6. Tecnologia, Itens e Fenômenos de Eberron (EFA)
        list.add(new Entity(
                "eberron_item_dragonshards", "EFA", "eberron", "998 YK", "item",
                "Fragmentos de Dragão (Dragonshards)", "Dragonshards of Eberron",
                "Gemas mágicas cristalinas fundamentais para a canalização de marcas de dragão e combustível de toda a magitecnologia.",
                Arrays.asList(
                        "Divididos em três tipos: Fragmentos de Siberys (dourados, celestes), Eberron (carmesim, da terra) e Khyber (violeta escuro, do submundo).",
                        "Fragmentos de Khyber são indispensáveis para aprisionar e vincular elementais nos motores de dirigíveis e trens elétricos."
                ),
                Arrays.asList(
                        "Disputas de mineração de fragmentos nas fronteiras de Q'barra e Xen'drik deflagram conflitos militares armados.",
                        "Fragmentos sintéticos instáveis criados por artífices renegados podem explodir em ondas de energia selvagem."
                ),
                Arrays.asList("eberron", "dragonshards", "siberys", "khyber", "elemental_binding", "magic_item"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "eberron_argonnessen", "EFA", "eberron", "998 YK", "location",
                "Argonnessen (Continente dos Dragões)", "Argonnessen",
                "O vasto continente isolado ao sudeste, lar ancestral de dragões sábios e mestres da magia primordial.",
                Arrays.asList(
                        "Cercado por tempestades arcanas e frotas de dragões que destroem qualquer navio intruso que se aproxime sem convite.",
                        "Abriga os Grandes Observatórios onde os dragões decifram o desenrolar milenar da Profecia Dracônica."
                ),
                Arrays.asList(
                        "A seita da Câmara (The Chamber) opera com dragões metamorfoseados infiltrados em Khorvaire.",
                        "Criptas titânicas guardam os restos mortais dos dragões primordiais que lutaram contra os Overlords na Primeira Era."
                ),
                Arrays.asList("eberron", "argonnessen", "dragons", "chamber", "prophecy", "ancient"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "eberron_droaam", "EFA", "eberron", "998 YK", "location",
                "Droaam (Nação dos Monstros)", "Droaam",
                "Território ocidental não reconhecido pelo Tratado de Thronehold, governado pela aliança das Filhas de Sora Kell.",
                Arrays.asList(
                        "Povoado por orcs, gnolls, ogros, medusas, harpias e minotauros que construíram uma sociedade funcional e bélica.",
                        "A capital do Grande Crag ergue-se com arquitetura de pedra esculpida e postos mercantis de Droaam."
                ),
                Arrays.asList(
                        "As Três Bruxas (Sora Kell) utilizam profecias do sangue para manipular os monarcas vizinhos de Breland.",
                        "O sindicato criminoso Daask exporta mercenários monstruosos para controlar as favelas de Sharn."
                ),
                Arrays.asList("eberron", "droaam", "monsters", "sora_kell", "daask", "frontier"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "eberron_warforged_colossus", "EFA", "eberron", "998 YK", "monster",
                "Colosso Forjado da Guerra", "Warforged Colossus",
                "Titã blindado mecânico colossal construído nos anos finais da Última Guerra para dizimar cidades fortificadas.",
                Arrays.asList(
                        "Muralhas vivas de placas de adamante e metal estelar impulsionadas por motores elementais gigantescos.",
                        "A maioria repousa inerte e tombada nas terras cinzentas da Mournland como monumentos fúnebres de aço."
                ),
                Arrays.asList(
                        "O Senhor das Lâminas tenta reparar os núcleos de força de um colosso sepultado para marchar sobre Breland.",
                        "A reativação de um colosso requer múltiplos fragmentos de dragão Khyber de pureza perfeita."
                ),
                Arrays.asList("eberron", "warforged_colossus", "mournland", "titan", "cannith", "construct"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        return Collections.unmodifiableList(list);
    }
}
