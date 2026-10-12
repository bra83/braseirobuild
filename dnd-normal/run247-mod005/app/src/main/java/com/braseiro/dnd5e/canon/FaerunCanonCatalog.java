package com.braseiro.dnd5e.canon;

import com.braseiro.dnd5e.NarrativeContentEngine.Entity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo canônico estruturado de Forgotten Realms:
 * - Heroes of Faerûn (FRHoF)
 * - Adventures in Faerûn (FRAiF)
 *
 * Abrange:
 * - Regiões & Cidades: Dalelands, Icewind Dale, Calimshan, Moonshae Isles, Baldur's Gate, Waterdeep, Neverwinter.
 * - Facções: Harpistas, Zhentarim, Enclave Esmeralda, Aliança dos Lordes, Ordem do Gantelete, Cavaleiros do Dragão Púrpuro, Magos Vermelhos, Culto do Dragão.
 * - Deuses & Religião: Mystra, Selûne, Shar, Ilmater, Bane, Lathander, Kelemvor, Oghma.
 * - NPCs Notáveis: Elminster, Laeral Silverhand, Drizzt Do'Urden, Sammaster, Khelben "Blackstaff", Szass Tam.
 * - Ganchos, Mistérios & Segredos: Biblioteca Perdida de Lethchauntos, Destinos Épicos, Ruínas de Myth Drannor.
 */
public final class FaerunCanonCatalog {
    private FaerunCanonCatalog() {}

    public static List<Entity> all() {
        List<Entity> list = new ArrayList<>();

        // 1. Facções Canônicas
        Map<String, String> harperRel = new HashMap<>();
        harperRel.put("rival", "faerun_zhentarim");
        harperRel.put("ally", "faerun_elminster");
        harperRel.put("ally_faction", "faerun_lords_alliance");
        list.add(new Entity(
                "faerun_harpers", "FRHoF", "forgotten_realms", "present", "faction",
                "Os Harpistas", "The Harpers",
                "Orquestra de agentes secretos que lutam pelo equilíbrio e contra a tirania em Faerûn.",
                Arrays.asList(
                        "Fundados por Elminster e líderes das raças livres para defender os fracos e preservar a história.",
                        "Operam através de células independentes e descentralizadas, usando o símbolo da harpa e da lua de prata.",
                        "Promovem canções, baladas e conhecimento preservado em refúgios ocultos."
                ),
                Arrays.asList(
                        "Possuem esconderijos mágicos camuflados sob tabernas ao longo de toda a Costa da Espada.",
                        "Monitoram em segredo o renascimento de cultos antigos nos picos de Netheril e as ações dos Magos Vermelhos.",
                        "Agentes veteranos carregam anéis encantados que dissipam mensagens psíquicas hostis."
                ),
                Arrays.asList("faerun", "harpers", "good", "secret_agents", "sword_coast"),
                harperRel, "grupo", true
        ));

        Map<String, String> zhentRel = new HashMap<>();
        zhentRel.put("rival", "faerun_harpers");
        zhentRel.put("rival_order", "faerun_order_gauntlet");
        list.add(new Entity(
                "faerun_zhentarim", "FRHoF", "forgotten_realms", "present", "faction",
                "A Rede Negra (Zhentarim)", "The Zhentarim",
                "Sindicato mercantil e militar que busca monopólio econômico e controle político.",
                Arrays.asList(
                        "Oferecem caravanas fortificadas, empréstimos mercantis e proteção profissional rigorosa.",
                        "Sua ala pública atua como guilda de mercenários respeitada nas cidades da Costa da Espada."
                ),
                Arrays.asList(
                        "O Círculo Interno conspira para subornar magistrados em Baldur's Gate e Waterdeep.",
                        "Mantêm armazéns subterrâneos com pólvora de fumaça, venenos e relíquias de Netheril.",
                        "Espiões zhentilares estão infiltrados em comitivas de caravanas mercantes de Amn e Sembia."
                ),
                Arrays.asList("faerun", "zhentarim", "mercenaries", "shadow_trade", "black_network"),
                zhentRel, "grupo", true
        ));

        Map<String, String> emeraldRel = new HashMap<>();
        emeraldRel.put("cooperation", "faerun_harpers");
        emeraldRel.put("opposition", "faerun_cult_dragon");
        list.add(new Entity(
                "faerun_emerald_enclave", "FRHoF", "forgotten_realms", "present", "faction",
                "Enclave Esmeralda", "The Emerald Enclave",
                "Irmandade de druidas, patrulheiros e guardiões dedicados a manter a ordem natural e conter horrores extraplanares.",
                Arrays.asList(
                        "Defendem a vida selvagem contra a exploração predatória e aberrações profanas.",
                        "Operam a partir de bosques sagrados, círculos de pedras e portais naturais em Cormanthor e nas Florestas do Norte."
                ),
                Arrays.asList(
                        "Monitoram as fissuras necromânticas deixadas pela queda de velhos impérios arcanos.",
                        "Possuem sementes despertas capazes de purificar terras corrompidas por magia selvagem."
                ),
                Arrays.asList("faerun", "druids", "nature", "balance", "wilderness"),
                emeraldRel, "grupo", true
        ));

        Map<String, String> lordsRel = new HashMap<>();
        lordsRel.put("allied_cities", "faerun_waterdeep,faerun_baldurs_gate,faerun_neverwinter");
        lordsRel.put("adversary", "faerun_zhentarim");
        list.add(new Entity(
                "faerun_lords_alliance", "FRHoF", "forgotten_realms", "present", "faction",
                "Aliança dos Lordes", "The Lords' Alliance",
                "Coalizão política e militar de governantes das grandes cidades livres de Faerûn para segurança mútua.",
                Arrays.asList(
                        "Reúne líderes de Waterdeep, Baldur's Gate, Neverwinter, Silverymoon e Mirabar.",
                        "Mobiliza exércitos conjuntos em tempos de invasão orc, dragões ou insurreições tirânicas."
                ),
                Arrays.asList(
                        "Diplomatas da aliança competem internamente por rotas comerciais e patentes navais.",
                        "Recrutam companhias mercenárias privadas para realizar missões de negação plausível nas fronteiras."
                ),
                Arrays.asList("faerun", "lords_alliance", "politics", "cities", "defense"),
                lordsRel, "grupo", true
        ));

        Map<String, String> gauntletRel = new HashMap<>();
        gauntletRel.put("patrons", "helm,torm,tyr");
        gauntletRel.put("enemy", "faerun_cult_dragon");
        list.add(new Entity(
                "faerun_order_gauntlet", "FRHoF", "forgotten_realms", "present", "faction",
                "Ordem do Gantelete", "Order of the Gauntlet",
                "Confederação de cavaleiros sagrados, clérigos e paladinos dedicada a confrontar ativamente o mal no momento em que ele surge.",
                Arrays.asList(
                        "Guiados pela retidão das divindades da justiça e proteção (Torm, Tyr, Helm e Ilmater).",
                        "Agem abertamente com armaduras brilhantes e bandeiras devotas, combatendo cultistas e mortos-vivos."
                ),
                Arrays.asList(
                        "Investigam relíquias corrompidas mantidas sob custódia em templos para destruição ritualística.",
                        "Têm ordens de purgar qualquer infestação demoníaca sem negociação preliminar."
                ),
                Arrays.asList("faerun", "paladins", "clerics", "justice", "holy_warriors"),
                gauntletRel, "grupo", true
        ));

        Map<String, String> purpleRel = new HashMap<>();
        purpleRel.put("realm", "cormyr");
        list.add(new Entity(
                "faerun_purple_dragons", "FRHoF", "forgotten_realms", "present", "faction",
                "Cavaleiros do Dragão Púrpuro", "The Purple Dragon Knights",
                "A lendária força cavalheiresca de elite do reino de Cormyr, famosa por sua disciplina e lealdade monárquica.",
                Arrays.asList(
                        "Defendem a coroa de Suzail e os povos de Cormyr contra monstruosidades e invasões.",
                        "São treinados em táticas de cavalaria pesada, comando de batalha e suporte heroico mútuo."
                ),
                Arrays.asList(
                        "Trabalham em estreita colaboração com os Magos de Guerra de Cormyr para caçar traidores da corte.",
                        "Mantêm postos avançados secretos nas bordas das Montanhas da Tempestade."
                ),
                Arrays.asList("faerun", "cormyr", "knights", "purple_dragon", "military"),
                purpleRel, "grupo", true
        ));

        Map<String, String> redWizardsRel = new HashMap<>();
        redWizardsRel.put("leader", "faerun_szass_tam");
        redWizardsRel.put("enemy", "faerun_harpers");
        list.add(new Entity(
                "faerun_red_wizards", "FRHoF", "forgotten_realms", "present", "faction",
                "Magos Vermelhos de Thay", "Red Wizards of Thay",
                "Magocracia de feiticeiros e necromantes tiranos que governa Thay com mãos de ferro e ambição imperial.",
                Arrays.asList(
                        "Famosos por suas cabeças raspadas, tatuagens arcanas intrincadas e vestes escarlates.",
                        "Mantêm enclaves comerciais através de Toril vendendo itens mágicos enquanto conspiram a longo prazo."
                ),
                Arrays.asList(
                        "Szass Tam utiliza os enclaves para canalizar almas e reunir reagentes para rituais de imortalidade em massa.",
                        "Células adormecidas em Faerûn buscam segredos arcanos esquecidos de Netheril e Imaskar."
                ),
                Arrays.asList("faerun", "thay", "necromancy", "wizards", "villains"),
                redWizardsRel, "grupo", true
        ));

        Map<String, String> cultDragonRel = new HashMap<>();
        cultDragonRel.put("historic_leader", "faerun_sammaster");
        cultDragonRel.put("worship", "tiamat_or_dracolich");
        list.add(new Entity(
                "faerun_cult_dragon", "FRHoF", "forgotten_realms", "present", "faction",
                "Culto do Dragão", "Cult of the Dragon",
                "Organização fanática fundada pelo arquimago louco Sammaster, reverenciando dragões cromáticos e dragões-esqueletos imortais.",
                Arrays.asList(
                        "Tradicionalmente criavam Dracoliches baseados nas profecias distorcidas de Sammaster.",
                        "Ramos modernos também prestam reverência e tributos a Tiamat, a Rainha Dracônica de Cinco Cabeças."
                ),
                Arrays.asList(
                        "Mantêm cavernas secretas de incubação de ovos dracônicos sob as Montanhas do Pôr do Sol.",
                        "Possuem textos originais do Livro do Dragão com rituais de transferência anímica proibida."
                ),
                Arrays.asList("faerun", "dragons", "cultists", "dracolich", "tiamat"),
                cultDragonRel, "grupo", true
        ));

        // 2. Cidades e Regiões de Faerûn
        list.add(new Entity(
                "faerun_baldurs_gate", "FRAiF", "forgotten_realms", "present", "location",
                "Portal de Baldur", "Baldur's Gate",
                "Metrópole comercial costeira, densa e implacável, cortada pelo Rio Chionthar.",
                Arrays.asList(
                        "Dividida entre a Cidade Alta fortificada, a Cidade Baixa movimentada e os Bairros Exteriores sem lei.",
                        "A ordem é mantida com mão pesada pela Companhia Mercenária do Punho Flamejante (Flaming Fist).",
                        "A Guilda comanda um império clandestino de contrabandistas, batedores e agiotas nas sombras dos becos."
                ),
                Arrays.asList(
                        "Catacumbas inundadas sob os esgotos conectam antigos santuários proibidos de Bhaal aos cais privados.",
                        "Casas nobres da Cidade Alta financiam secretamente corsários para atacar mercadores de Athkatla."
                ),
                Arrays.asList("faerun", "baldurs_gate", "city", "sword_coast", "trade"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_waterdeep", "FRHoF", "forgotten_realms", "present", "location",
                "Águas Profundas (Waterdeep)", "Waterdeep, the City of Splendors",
                "A Cidade dos Esplendores, centro supremo de comércio, cultura e intriga política no Norte.",
                Arrays.asList(
                        "Governada pelos Lordes Mascarados sob a liderança pública do Lorde Protetor Laeral Silverhand.",
                        "Dividida em distritos históricos: Castelo, Comércio, Mar, Norte, Docas, Cidade dos Mortos e Distrito Sul.",
                        "Assentada acima dos lendários salões abissais do Subterrâneo de Undermountain."
                ),
                Arrays.asList(
                        "A Guarda da Cidade conta com o Olho Vigilante de estátuas ambulantes arcanas preparadas para despertar em crises.",
                        "Passagens ocultas na Cidade dos Mortos conduzem a cofres de conhecimento proibido de séculos passados."
                ),
                Arrays.asList("faerun", "waterdeep", "city_of_splendors", "sword_coast", "metropolis"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_undermountain_depths", "FRAiF", "forgotten_realms", "present", "location",
                "Profundezas de Undermountain", "Undermountain Depths",
                "O colossal labirinto sob Waterdeep criado pelo Mago Louco Halaster Blackcloak.",
                Arrays.asList(
                        "Masmorra multinível com níveis que variam de cavernas goblinóides a portais estelares.",
                        "A entrada mais famosa é o poço dentro da Taverna do Portal Bocejante (Yawning Portal)."
                ),
                Arrays.asList(
                        "Halaster utiliza os nós arcanos da masmorra para absorver magias de aventureiros derrotados.",
                        "Existe um portal direto para os esgotos secretos de Baldur's Gate no quinto nível."
                ),
                Arrays.asList("faerun", "undermountain", "halaster", "dungeon"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "faerun_dalelands", "FRAiF", "forgotten_realms", "present", "location",
                "Os Vales (Dalelands)", "The Dalelands",
                "Região pastoral fértil de comunidades livres e florestas ancestrais cercadas pela misteriosa mata de Cormanthor.",
                Arrays.asList(
                        "Composta por vales independentes incluindo Shadowdale, Daggerdale, Mistledale, Battledale e Deepingdale.",
                        "História marcada pelo Tratado do Povo Livre com os elfos de Cormanthor firmado na Pedra Erguida (Standing Stone)."
                ),
                Arrays.asList(
                        "As ruínas de Myth Drannor em Cormanthor guardam mytals fraturados e monstros tocados pelo Weave selvagem.",
                        "Incursores zhentilares e espiões de Sembia operam nos limites arborizados fomentando desconfiança mútua."
                ),
                Arrays.asList("faerun", "dalelands", "cormanthor", "shadowdale", "myth_drannor"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_icewind_dale", "FRAiF", "forgotten_realms", "present", "location",
                "Vale do Vento Gélido", "Icewind Dale",
                "Região tundra implacável e gélida ao norte da Espinha do Mundo.",
                Arrays.asList(
                        "Lar das Dez Cidades (Ten-Towns) instaladas ao redor dos lagos Maer Dualdon, Lac Dinneshere e Redwaters.",
                        "Economia local dependente da pesca da truta cabeça-de-junta (knucklehead trout) e entalhe de marfim.",
                        "A sobrevivência exige tolerância ao frio ártico, nevascas repentinas e predadores da tundra."
                ),
                Arrays.asList(
                        "Geleiras antigas escondem fragmentos caídos de cidades flutuantes netherenses congeladas no tempo.",
                        "Cultos à deusa Auril entoam cânticos nas geleiras para prolongar noites sem aurora."
                ),
                Arrays.asList("faerun", "icewind_dale", "ten_towns", "arctic", "survival"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_calimshan", "FRAiF", "forgotten_realms", "present", "location",
                "Calimshan", "Calimshan",
                "Vasto império meridional desértico e costeiro com milênios de história mágica e influência de gênios.",
                Arrays.asList(
                        "A capital Calimport é famosa por palácios de cúpulas douradas, bazares gigantescos e minaretes.",
                        "Herança cultural profundamente marcada pela antiga guerra elemental entre djinn e efreeti.",
                        "Centro de comércio de especiarias raras, seda mágica e maravilhas mecânicas alquímicas."
                ),
                Arrays.asList(
                        "Facções de gênios rivais manipulam sultões e vizires através de acordos e pactos ardilosos.",
                        "Sob as areias do Deserto de Calim jazem templos esquecidos de deuses primordiais sepultados."
                ),
                Arrays.asList("faerun", "calimshan", "desert", "genies", "calimport"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_moonshae_isles", "FRAiF", "forgotten_realms", "present", "location",
                "Ilhas Moonshae", "Moonshae Isles",
                "Arquipélago ventoso e enevoado a oeste da Costa da Espada, lar dos povos Ffolk e guerreiros Northlanders.",
                Arrays.asList(
                        "Paisagem de montanhas escarpadas, pântanos misteriosos e florestas densas consagradas à Mãe-Terra.",
                        "Tensão perene entre os clãs agrícolas dos Ffolk e os invasores marinheiros de origem Northlander."
                ),
                Arrays.asList(
                        "Poços lunares sagrados conectam a ilha à Trama natural e à dimensão de Feywild.",
                        "Fomorians e gigantes da sombra espreitam em cavernas submarinas planejando reconquistar os reinos insulares."
                ),
                Arrays.asList("faerun", "moonshae", "islands", "druids", "earthmother"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_lost_library_lethchauntos", "FRAiF", "forgotten_realms", "present", "location",
                "A Biblioteca Perdida de Lethchauntos", "The Lost Library of Lethchauntos",
                "Complexo subterrâneo esquecido contendo tomos arcanos da era de Netheril e tratados sobre o Nathlum.",
                Arrays.asList(
                        "Construída pelo lendário sábio Lethchauntos para proteger segredos alquímicos e astronômicos.",
                        "Protegida por construtos nimblewrights, sentinelas de pedra e salas seladas por enigmas arcanos."
                ),
                Arrays.asList(
                        "O sanctum interno guarda pergaminhos capazes de decifrar línguas perdidas de antes da Queda de Karsus.",
                        "Infiltradores do Culto do Dragão vasculham as cavernas vizinhas em busca de armas dracônicas."
                ),
                Arrays.asList("faerun", "dungeon", "library", "netheril_lore", "lethchauntos"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        // 3. Deuses e Cosmologia de Faerûn
        list.add(new Entity(
                "faerun_deity_mystra", "FRHoF", "forgotten_realms", "present", "religion",
                "Mystra, Deusa da Magia", "Mystra, Mother of All Magic",
                "A personificação da Trama (Weave) que permite que mortais e deuses canalizem a magia em Toril.",
                Arrays.asList(
                        "Adorada por magos, feiticeiros, bardos e estudantes arcanos em todas as nações.",
                        "Seu dogma incentiva o uso responsável e a expansão do conhecimento mágico sem corrupção tirânica."
                ),
                Arrays.asList(
                        "Escolhidos de Mystra como Elminster e as Sete Irmãs recebem chamas de prata para restaurar a integridade da Trama.",
                        "Zonas de magia morta e selvagem ocorrem onde a conexão divina com a Trama foi rasgada por cataclismos."
                ),
                Arrays.asList("faerun", "deity", "mystra", "magic", "weave"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_deity_selune_shar", "FRHoF", "forgotten_realms", "present", "religion",
                "O Conflito Celestial: Selûne e Shar", "Selûne and Shar",
                "A eterna rivalidade cósmica entre a Senhora da Lua e da Luz Guia e a Deusa da Escuridão, Noite e Perda.",
                Arrays.asList(
                        "Selûne ilumina navegantes, andarilhos noturnos e purifica criaturas afligidas pela licantropia.",
                        "Shar comanda a Trama das Sombras (Shadow Weave), atraindo aqueles que buscam vingança, esquecimento ou morte."
                ),
                Arrays.asList(
                        "Cultos de Shar mantêm claustros sob cidades da Costa da Espada para apagar memórias de personalidades influentes.",
                        "A lua prateada cheia amplifica rituais de clérigos devotos de Selûne em momentos de batalha astral."
                ),
                Arrays.asList("faerun", "deity", "selune", "shar", "cosmic_rivalry"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        // 4. NPCs Lendários de Faerûn
        list.add(new Entity(
                "faerun_elminster", "FRAiF", "forgotten_realms", "present", "npc",
                "Elminster de Shadowdale", "Elminster of Shadowdale",
                "O lendário Escolhido de Mystra, ancião dos magos e conselheiro dos povos livres de Toril.",
                Arrays.asList(
                        "Vive em uma torre pacífica no Vale das Sombras, apreciando tabaco aromático e histórias de taverna.",
                        "Auxilia heróis quando a estabilidade fundamental da Trama e da vida mortal é posta em perigo extremo."
                ),
                Arrays.asList(
                        "Carrega em sua mente séculos de segredos da Queda de Netheril e das guerras de Cormanthor.",
                        "Pode manifestar a Chama de Prata de Mystra para purificar corrupção abissal e selar fendas cósmicas."
                ),
                Arrays.asList("faerun", "elminster", "archmage", "chosen_mystra", "legend"),
                harperRel, "grupo", true
        ));

        list.add(new Entity(
                "faerun_silver_hand", "FRAiF", "forgotten_realms", "present", "npc",
                "Laeral Silverhand", "Laeral Silverhand, Open Lord",
                "Lorde Protetora de Waterdeep, uma das Sete Irmãs e arquimaga de astúcia política incomparável.",
                Arrays.asList(
                        "Lidera a Aliança dos Lordes com firmeza diplomática e defesa inegociável da soberania cívica.",
                        "Respeitada tanto pelos nobres abastados quanto pela Guarda da Cidade e guildas de artesãos."
                ),
                Arrays.asList(
                        "Mantém vigilância constante sobre espiões zhentilares e necromantes infiltrados nas festas palacianas.",
                        "Tem acesso aos cofres mágicos dos antigos magos fundadores de Waterdeep."
                ),
                Arrays.asList("faerun", "laeral_silverhand", "waterdeep", "seven_sisters", "open_lord"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_sammaster", "FRAiF", "forgotten_realms", "present", "npc",
                "Sammaster, o Profeta Louco", "Sammaster the Mad",
                "Ex-Escolhido de Mystra que sucumbiu à megalomania e fundou o Culto do Dragão.",
                Arrays.asList(
                        "Criador dos primeiros rituais para transformar dragões vivos em dracoliches eternos.",
                        "Autor das profecias apócrifas que declaravam que 'dragões mortos governarão o mundo'."
                ),
                Arrays.asList(
                        "Fragmentos de sua consciência permanecem aprisionados em filactérias ocultas nas Montanhas dos Picos Escarpados.",
                        "O Culto continua buscando reunir suas anotações originais para reencarná-lo em uma casca dracônica."
                ),
                Arrays.asList("faerun", "sammaster", "cult_dragon", "dracolich", "legendary_villain"),
                cultDragonRel, "secreto", false
        ));

        list.add(new Entity(
                "faerun_cormanthor_myth_drannor", "FRAiF", "forgotten_realms", "present", "location",
                "Ruínas de Cormanthor e Myth Drannor", "Cormanthor & Myth Drannor",
                "A ancestral floresta élfica e as ruínas da gloriosa cidade que outrora uniu humanos e elfos.",
                Arrays.asList(
                        "Árvores gigantescas entrelaçam pedras ancestrais impregnadas de mythal enfraquecido mas potente.",
                        "Refúgio de elfos da floresta e patrulhas rangers que impedem intrusos de pilhar túmulos nobres."
                ),
                Arrays.asList(
                        "Salões subterrâneos ainda guardam portais desestabilizados que conectam a planos elementais.",
                        "Agentes do Zhentarim financiam expedições clandestinas para roubar estelas rúnicas do mythal."
                ),
                Arrays.asList("faerun", "cormanthor", "myth_drannor", "mythal", "elven_ruins"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_calimshan_genie_factions", "FRAiF", "forgotten_realms", "present", "faction",
                "Facções de Gênios de Calimshan", "Calimshan Genie Factions",
                "Linhagens nobres e seitas influenciadas por Efreeti e Djinn disputando hegemonia no sul.",
                Arrays.asList(
                        "Divididos entre lealdades à Chama Eterna e aos Senhores dos Ventos em Calimport.",
                        "Controlam caravanas mercantis de especiarias raras, gemas encantadas e seda solar."
                ),
                Arrays.asList(
                        "Conspiram para romper as algemas mágicas que mantêm arqui-efreeti selados sob os desertos.",
                        "Mantêm espiões em todas as casas de câmbio de Calimport para subornar sátrapas."
                ),
                Arrays.asList("faerun", "calimshan", "genie", "efreeti", "djinn", "factions"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_moonshae_moonwells", "FRAiF", "forgotten_realms", "present", "location",
                "Poços Lunares de Moonshae", "Moonshae Moonwells",
                "Fontes sagradas consagradas à Mãe Terra cujas águas purificadoras refletem os ciclos lunares.",
                Arrays.asList(
                        "Fontes de cura mística e comunhão druídica protegidas pelos Northmen e Ffolk.",
                        "Águas que neutralizam venenos mortais e revelam ilusões quando tocadas sob o luar."
                ),
                Arrays.asList(
                        "A corrupção de Malar e do deus-fera Kazgaroth tenta profanar os poços para transformá-los em fossas de sangue.",
                        "Druidas do Círculo Negro conspiram em segredo para secar a bacia de Alaron."
                ),
                Arrays.asList("faerun", "moonshae", "moonwell", "earthmother", "druidic_sacred"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_item_calimemnon_crystal", "FRAiF", "forgotten_realms", "present", "item",
                "Cristal de Calimemnon", "Calimemnon Crystal",
                "Relíquia lendária que contém a essência aprisionada de um arqui-efreeti de Calimshan.",
                Arrays.asList(
                        "Emite calor abrasador constante e concede resistência a chamas arcanas ao portador.",
                        "Cobiçado por magos do sul para alimentar forjas arcanas permanentes."
                ),
                Arrays.asList(
                        "Se o cristal for quebrado por dano radiante, a entidade aprisionada é liberta com sede de vingança.",
                        "Requer uma palavra de comando em primordial antigo para liberar seus feitiços de fogo sem consumir o portador."
                ),
                Arrays.asList("faerun", "calimemnon", "crystal", "magic_item", "calimshan"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "faerun_item_crown_of_horns", "FRAiF", "forgotten_realms", "present", "item",
                "Coroa de Chifres", "Crown of Horns",
                "Artefato macabro ligado aos vestígios do deus Myrkul que instila necromancia e loucura.",
                Arrays.asList(
                        "Concede poderes necromânticos devastadores a quem a coloca sobre a fronte.",
                        "Reconhecida historicamente como um dos artefatos mais perigosos dos Reinos."
                ),
                Arrays.asList(
                        "A coroa sussurra pensamentos homicidas ao usuário e tenta transformá-lo em um avatar de Myrkul.",
                        "Possui vontade própria e pode se teletransportar se o portador for purificado por bênção divina."
                ),
                Arrays.asList("faerun", "crown_of_horns", "myrkul", "artifact", "necromancy"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "faerun_item_tome_dragon", "FRAiF", "forgotten_realms", "present", "item",
                "Tomo do Dragão", "Tome of the Dragon",
                "Grimório original contendo as transcrições das profecias de Sammaster e rituais dracólicos.",
                Arrays.asList(
                        "Contém fórmulas arcanas para criação de filactérias dracônicas e conservação cadavérica.",
                        "Texto escrito em draconiano arcaico com tinta extraída de sangue de dragão verde."
                ),
                Arrays.asList(
                        "A leitura desprotegida exige salvaguarda de Sabedoria CD 15 contra perturbação mental permanente.",
                        "O Culto do Dragão pagará fortunas para recuperar qualquer cópia genuína das mãos de aventureiros."
                ),
                Arrays.asList("faerun", "tome_dragon", "sammaster", "cult_dragon", "grimoire"),
                cultDragonRel, "secreto", false
        ));

        list.add(new Entity(
                "faerun_beast_of_malar", "FRAiF", "forgotten_realms", "present", "monster",
                "Fera de Malar", "Beast of Malar",
                "Monstruosidade metamórfica invocada por cultos de caçadores de sangue do Senhor das Feras.",
                Arrays.asList(
                        "Criatura colossal com mandíbulas de sabre, pelos farpados e sede inesgotável por presas.",
                        "Caça incansavelmente sob a lua cheia, aterrorizando aldeias isoladas nas fronteiras florestais."
                ),
                Arrays.asList(
                        "Regenera ferimentos a menos que seja golpeada com prata consagrada ou fogo sagrado.",
                        "Possui faro sobrenatural capaz de rastrear aventureiros feridos a quilômetros de distância."
                ),
                Arrays.asList("faerun", "beast_of_malar", "malar", "bestiary", "predator"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_dead_three_scion", "FRAiF", "forgotten_realms", "present", "monster",
                "Progênie dos Três Mortos", "Dead Three Scion",
                "Fanático possuído pelo zelo fanático de Bane, Bhaal ou Myrkul nos esgotos urbanos.",
                Arrays.asList(
                        "Assassinos e tiranos cruéis que organizam células de terror e sequestros nas cidades livres.",
                        "Usam máscaras de ferro forjado e adagas rituais gravadas com o crânio e a mão de osso."
                ),
                Arrays.asList(
                        "Ao tombar em combate, o corpo explode em uma névoa necrótica com CD 14 de Constituição.",
                        "Mantêm cadernos cifrados com ordens diretas de mestres de guilda ocultos em Baldur's Gate."
                ),
                Arrays.asList("faerun", "dead_three", "bhaal", "bane", "myrkul", "cultists"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        // --- EXPANSÃO LOTE A: HEROES OF FAERÛN & ADVENTURES IN FAERÛN ---

        // 5. Cidades e Regiões Fundamentais Adicionais (FRAiF)
        list.add(new Entity(
                "faerun_shadowdale", "FRAiF", "forgotten_realms", "present", "location",
                "Shadowdale (Vale das Sombras)", "Shadowdale",
                "O mais célebre vale dos Dalelands, símbolo de resistência e coração pastoral de Faerûn.",
                Arrays.asList(
                        "Composto pela estalagem lendária Old Skull Inn, campos cultivados e a imponente colina Old Skull.",
                        "Refúgio tradicional de aventureiros, mantido sob a vigilância benevolente da torre de Elminster.",
                        "Povoado autônomo governado pela virtude cívica e alianças históricas com os Harpistas."
                ),
                Arrays.asList(
                        "Cavernas profundas sob a colina Old Skull ligam os Dalelands diretamente ao Underdark.",
                        "Tratados ancestrais com os elfos de Cormanthor impedem o corte de carvalhos mágicos no perímetro."
                ),
                Arrays.asList("faerun", "dalelands", "shadowdale", "elminster", "old_skull_inn"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_neverwinter", "FRAiF", "forgotten_realms", "present", "location",
                "Neverwinter (Jóia do Norte)", "Neverwinter, City of Skilled Hands",
                "Metrópole costeira aquecida pelo Rio Neverwinter, em vibrante reconstrução sob Lord Dagult Neverember.",
                Arrays.asList(
                        "Famosa pelo artesanato refinado de relógios de água, lâmpadas de vidro e jardins suspensos.",
                        "O Lorde Protetor Dagult Neverember comanda a reconstrução dos distritos após a erupção do Monte Hotenow.",
                        "Membro proeminente da Aliança dos Lordes com forte presença militar de mercenários leais."
                ),
                Arrays.asList(
                        "O Abismo (The Chasm) sob o Distrito Sudeste ainda emana anomalias da Spellplague e aberrações do Reino Distante.",
                        "A seita dos Filhos de Alagondar opera clandestinamente contra o governo de Neverember nas tavernas do porto."
                ),
                Arrays.asList("faerun", "neverwinter", "sword_coast", "lords_alliance", "dagult_neverember"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_silverymoon", "FRAiF", "forgotten_realms", "present", "location",
                "Silverymoon (Gema do Norte)", "Silverymoon, Gem of the North",
                "Centro de alta magia, erudição e tolerância racial protegido por um mythal prateado resplandecente.",
                Arrays.asList(
                        "Governada pela Alta Senhora e pelo Conselho dos Doze Pares em harmonia com elfos, anões e humanos.",
                        "A Ponte Lunar (Moonbridge) de energia pura curva-se magicamente sobre o Rio Rauvin.",
                        "Sede da Universidade de Silverymoon e dos lendários Cavaleiros de Prata (Knights in Silver)."
                ),
                Arrays.asList(
                        "O Mythal de Silverymoon bloqueia teletransporte hostil e neutraliza feitiços necromânticos de morte instantânea.",
                        "Magos da Capa Estrelada monitoram passagens secretas nas Montanhas Nether para barrar orcs e orogs."
                ),
                Arrays.asList("faerun", "silverymoon", "mythal", "high_magic", "silver_marches"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_thay", "FRAiF", "forgotten_realms", "present", "location",
                "Império de Thay", "Thay",
                "Planalto vulcânico oriental governado pelo regime necromântico de ferro dos Magos Vermelhos.",
                Arrays.asList(
                        "Sociedade rigidamente estratificada com legiões de mortos-vivos subservientes e fortalezas de basalto.",
                        "Comandada pelos oito Zulkirs dedicados às escolas tradicionais de magia, sob supremacia de Szass Tam.",
                        "Mantêm enclaves comerciais diplomáticos ao redor de Faerûn para financiar pesquisas arcanas."
                ),
                Arrays.asList(
                        "Szass Tam executa rituais sombrios permanentes para erguer o Grande Mythal da Morte em todo o planalto.",
                        "Dissidentes e escravizados formam redes secretas de fuga auxiliadas por agentes Harpistas."
                ),
                Arrays.asList("faerun", "thay", "red_wizards", "szass_tam", "necromancy"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        // 6. NPCs Fundamentais de Faerûn e das Aventuras (FRAiF & FRHoF)
        Map<String, String> kharaRel = new HashMap<>();
        kharaRel.put("location", "faerun_shadowdale");
        kharaRel.put("ally", "faerun_harpers");
        list.add(new Entity(
                "faerun_khara_sulwood", "FRAiF", "forgotten_realms", "present", "npc",
                "Lady Khara Sulwood", "Lady Khara Sulwood",
                "Nobre e patrona de Shadowdale, defensora dos Dalelands que contrata heróis para purificar a Tumba de Damara.",
                Arrays.asList(
                        "Conhecida por sua retidão, generosidade com viajantes e lealdade às famílias pioneiras de Shadowdale.",
                        "Financia expedições armadas para resgatar reféns de incursores hobgoblins e monstros espectrais."
                ),
                Arrays.asList(
                        "Guarda o juramento secreto dos Protetores de Damara para nunca permitir que o Orbe caia nas mãos do Zhentarim.",
                        "Possui comunicação direta com os Harpistas para alertar sobre movimentações hostis em Cormanthor."
                ),
                Arrays.asList("faerun", "khara_sulwood", "shadowdale", "dalelands", "patron", "tomb_of_damara"),
                kharaRel, "grupo", true
        ));

        Map<String, String> myrnaRel = new HashMap<>();
        myrnaRel.put("residence", "faerun_shadowdale");
        myrnaRel.put("rival", "faerun_powell_the_owl_niccam");
        list.add(new Entity(
                "faerun_myrna_nor", "FRAiF", "forgotten_realms", "present", "npc",
                "Myrna Nor", "Myrna Nor",
                "Estudiosa arcana e maga de Shadowdale, patrona da busca pela Biblioteca Perdida de Lethchauntos.",
                Arrays.asList(
                        "Frequenta a taverna Old Skull Inn estudando manuscritos e mapas de expedições antigas em Cormanthor.",
                        "Dedicada à preservação de tomos históricos e fórmulas alquímicas pré-Queda de Netheril."
                ),
                Arrays.asList(
                        "Descobriu a rota exata de quatro dias pela floresta que leva à colina da biblioteca oculta.",
                        "Pretende purificar as fórmulas do tomo Nathlum para curar pestes raras sem gerar toxinas letais."
                ),
                Arrays.asList("faerun", "myrna_nor", "shadowdale", "lethchauntos", "scholar", "lost_library"),
                myrnaRel, "grupo", true
        ));

        Map<String, String> powellRel = new HashMap<>();
        powellRel.put("contractor", "faerun_zhentarim");
        powellRel.put("rival", "faerun_myrna_nor");
        list.add(new Entity(
                "faerun_powell_the_owl_niccam", "FRAiF", "forgotten_realms", "present", "npc",
                "Powell 'the Owl' Niccam", "Powell 'the Owl' Niccam",
                "Mercenário astuto e líder de batedores financiado pelo Zhentarim para saquear Lethchauntos.",
                Arrays.asList(
                        "Recebeu o apelido de 'Coruja' pela audição apurada, cálculo tático frio e ataques noturnos precisos.",
                        "Comanda uma vanguarda de mercenários e sentinelas armados com bestas pesadas e ferramentas de arrombamento."
                ),
                Arrays.asList(
                        "Planeja vender as fórmulas de venenos do tomo Nathlum ao melhor comprador nos mercados clandestinos de Sembia.",
                        "Se encurralado e subornado com ouro superior, está disposto a romper o contrato com a Rede Negra."
                ),
                Arrays.asList("faerun", "powell_niccam", "mercenary", "zhentarim", "lost_library", "rival"),
                powellRel, "grupo", true
        ));

        Map<String, String> bonegnasherRel = new HashMap<>();
        bonegnasherRel.put("haunt", "faerun_dalelands");
        bonegnasherRel.put("enemy", "faerun_khara_sulwood");
        list.add(new Entity(
                "faerun_bonegnasher", "FRAiF", "forgotten_realms", "present", "monster",
                "Bonegnasher, o Warlord Ogro Espectral", "Bonegnasher the Spectral Warlord",
                "Monstruosidade espectral colossal que assombra as ruínas da Tumba de Damara em busca do Orbe.",
                Arrays.asList(
                        "Ogro belicista do passado cuja alma vingativa foi reanimada por emanações necróticas profundas.",
                        "Subjugou bandos de ghouls carniceiros e hobgoblins mercenários para fazer valer sua vontade."
                ),
                Arrays.asList(
                        "Vulnerável a rituais sagrados de expulsão e à história ancestral de suas derrotas em vida.",
                        "Alimenta seu poder sugando a força vital de prisioneiros confinados nas celas subterrâneas."
                ),
                Arrays.asList("faerun", "bonegnasher", "specter", "ogre", "tomb_of_damara", "undead"),
                bonegnasherRel, "secreto", false
        ));

        list.add(new Entity(
                "faerun_drizzt_dourden", "FRHoF", "forgotten_realms", "present", "npc",
                "Drizzt Do'Urden", "Drizzt Do'Urden",
                "O patrulheiro drow heróico da Espinha do Mundo, devoto de Mielikki e portador das cimitarras Icingdeath e Twinkle.",
                Arrays.asList(
                        "Renegou a tirania matriarcal de Menzoberranzan para lutar com honra pelos povos da superfície.",
                        "Acompanhado pela pantera astral mágica Guenhwyvar, invocada através de uma estatueta de ônix.",
                        "Lenda viva nas Dez Cidades de Icewind Dale e ao longo de toda a Costa da Espada."
                ),
                Arrays.asList(
                        "Domina o estado guerreiro do Caçador (The Hunter), canalizando foco mortal em defesa dos indefesos.",
                        "Mantém vigilância sobre clãs de drows leais a Eilistraee que buscam refúgio e redenção sob a luz da lua."
                ),
                Arrays.asList("faerun", "drizzt", "drow", "ranger", "icewind_dale", "legend"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        Map<String, String> szassRel = new HashMap<>();
        szassRel.put("domain", "faerun_thay");
        szassRel.put("enemy", "faerun_harpers");
        szassRel.put("faction", "faerun_red_wizards");
        list.add(new Entity(
                "faerun_szass_tam", "FRHoF", "forgotten_realms", "present", "npc",
                "Szass Tam, Zulkir da Necromancia", "Szass Tam, Supreme Zulkir",
                "O tirano lich imortal e governante absoluto de Thay, senhor incontestável dos Magos Vermelhos.",
                Arrays.asList(
                        "Conduz pesquisas milenares sobre a mortalidade, criando legiões de dread warriors e vampiros.",
                        "Representa a maior ameaça necromântica unificada às nações orientais e meridionais de Faerûn."
                ),
                Arrays.asList(
                        "Sua filactéria original está escondida em um demiplano acessível apenas por chaves de sangue necromântico.",
                        "Usa simulacros perfeitos para despachar ordens e conduzir negociações perigosas sem se expor."
                ),
                Arrays.asList("faerun", "szass_tam", "lich", "red_wizards", "thay", "necromancy"),
                szassRel, "secreto", false
        ));

        // 7. Relíquias e Itens das Aventuras de Faerûn (FRAiF)
        list.add(new Entity(
                "faerun_item_orb_of_damara", "FRAiF", "forgotten_realms", "present", "item",
                "Orbe de Damara", "Orb of Damara",
                "Relíquia sagrada de proteção contra mortos-vivos resgatada nas profundezas da Tumba de Damara.",
                Arrays.asList(
                        "Esfera de cristal puro banhada por bênçãos clericais ancestrais de sacerdotes de Lathander e Ilmater.",
                        "Irradia aura suave que impede carniçais e aparições de se aproximarem sem sofrer queimaduras radiantes."
                ),
                Arrays.asList(
                        "Ao ser canalizado com uma ação, dissipa áreas de escuridão mágica e revela segredos ocultos em pedra.",
                        "Reivindicado por Lady Khara Sulwood como herança defensiva inalienável do povo de Shadowdale."
                ),
                Arrays.asList("faerun", "orb_of_damara", "tomb_of_damara", "holy_relic", "magic_item"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "faerun_item_nathlum_grimoire", "FRAiF", "forgotten_realms", "present", "item",
                "O Grimório Nathlum", "The Nathlum Grimoire",
                "Tratado arcano lendário preservado na Biblioteca de Lethchauntos sobre toxinas e antídotos.",
                Arrays.asList(
                        "Encadernado em couro alquímico imune à podridão do tempo, com fechos gravados em runas dracônicas.",
                        "Fonte de conhecimento valorizada tanto por curandeiros de prestígio quanto por guildas de assassinos."
                ),
                Arrays.asList(
                        "Contém receitas de antídotos universais capazes de neutralizar qualquer veneno mortal em três rodadas.",
                        "Guardado com glifos de vigilância que incendeiam cópias falsificadas ao menor contato de ferro comum."
                ),
                Arrays.asList("faerun", "nathlum", "lethchauntos", "grimoire", "alchemy", "magic_item"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        // 8. Panteão Maior Adicional (FRHoF)
        list.add(new Entity(
                "faerun_deity_lathander", "FRHoF", "forgotten_realms", "present", "religion",
                "Lathander, o Senhor da Manhã", "Lathander, the Morninglord",
                "Deus da alvorada, primavera, nascimento, renovação e luz eterna de Toril.",
                Arrays.asList(
                        "Padroeiro de novos começos, otimismo juvenil e cruzados que purificam horrores mortos-vivos.",
                        "Seus templos têm arquitetura voltada para o leste com vitrais rosados que capturam o primeiro raio de sol."
                ),
                Arrays.asList(
                        "O Sangue de Lathander é uma relíquia consagrada procurada para dissipar maldições cósmicas de sombras.",
                        "Clérigos lathanderianos lideram iniciativas de reconstrução em áreas devastadas por guerras mágicas."
                ),
                Arrays.asList("faerun", "lathander", "morninglord", "deity", "light", "dawn"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_deity_kelemvor", "FRHoF", "forgotten_realms", "present", "religion",
                "Kelemvor, Juiz dos Condenados", "Kelemvor, Lord of the Dead",
                "Deus da morte justa, guia impassível das almas no Plano de Fuga e inimigo jurado do morticínio zumbi.",
                Arrays.asList(
                        "Ensina que a morte é um ciclo natural sagrado da existência que não deve ser temido nem abreviado.",
                        "Condena veementemente a necromancia que escraviza almas ou impede a transição para os planos exteriores."
                ),
                Arrays.asList(
                        "Guardiões da Fé da Torre das Caveiras defendem cemitérios em cidades contra ladrões de cadáveres zhentilares.",
                        "A Cidade do Julgamento acolhe espíritos imparciais antes de sua passagem aos lares divinos."
                ),
                Arrays.asList("faerun", "kelemvor", "death", "deity", "justice", "graveyard"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_deity_ilmater", "FRHoF", "forgotten_realms", "present", "religion",
                "Ilmater, O Que Chora", "Ilmater, the Broken God",
                "Deus da compaixão, perseverança, martírio e acolhimento dos que sofrem e são oprimidos.",
                Arrays.asList(
                        "Conhecido como o Deus Quebrado, venerado pelos pobres, escravizados e feridos de guerra.",
                        "Seus monges e sacerdotes viajam descalços administrando curas e conforto espiritual sem cobrar tributos."
                ),
                Arrays.asList(
                        "Devotos de Ilmater podem assumir ferimentos de companheiros feridos através de rituais de empatia sagrada.",
                        "Confronta a crueldade dos deuses Loviatar e Bane resgatando prisioneiros de calabouços escuros."
                ),
                Arrays.asList("faerun", "ilmater", "compassion", "deity", "healing", "perseverance"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "faerun_deity_bane", "FRHoF", "forgotten_realms", "present", "religion",
                "Bane, o Senhor Negro", "Bane, the Black Lord",
                "Deus da tirania, medo, controle militar e ambição desmedida, membro supremo dos Três Mortos.",
                Arrays.asList(
                        "Venerado por tiranos, conquistadores implacáveis, generais corruptos e agentes do Zhentarim.",
                        "Exige obediência absoluta à hierarquia de força e desprezo total pela fraqueza ou misericórdia."
                ),
                Arrays.asList(
                        "O Culto da Mão Negra mantém santuários secretos sob quartéis para corromper jovens oficiais do exército.",
                        "Conspira continuamente para submeter os outros dois membros dos Três Mortos (Bhaal e Myrkul) ao seu comando."
                ),
                Arrays.asList("faerun", "bane", "tyranny", "deity", "dead_three", "zhentarim"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        return Collections.unmodifiableList(list);
    }
}
