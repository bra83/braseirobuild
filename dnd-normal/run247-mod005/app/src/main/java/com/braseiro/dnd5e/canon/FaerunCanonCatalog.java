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

        return Collections.unmodifiableList(list);
    }
}
