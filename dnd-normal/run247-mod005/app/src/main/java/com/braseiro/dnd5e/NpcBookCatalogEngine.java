package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Catálogo e gerador de tabelas de rolagem de dados para NPCs com curadoria por livro:
 * - "The Game Master's Book of NPCs" (Tabelas d20 de aparência, hábitos, segredos e ganchos)
 * - "1000 Memorable NPCs for Any Roleplaying" (Tabelas d20 de características marcantes, tiques e histórico)
 * - "The Game Master's Book of Random Encounters" (Tabelas d20 de encontros com NPCs, conflitos e objetivos)
 * - "Catálogo 300 NPCs PT-BR" (Extrações curadas do banco de dados de 300 NPCs em PT-BR)
 * - "Statblock Compendium" (Fichas de combate)
 */
public final class NpcBookCatalogEngine {

    public static final String BOOK_GMB_NPC = "The Game Master's Book of NPCs";
    public static final String BOOK_MASKS_1000 = "1000 Memorable NPCs for Any Roleplaying";
    public static final String BOOK_RANDOM_ENCOUNTERS = "The Game Master's Book of Random Encounters";
    public static final String BOOK_CATALOG_300 = "Catálogo 300 NPCs PT-BR";

    private static final Random RNG = new Random();

    public static final class RollResult {
        public final int roll;
        public final String die;
        public final String text;
        public final String book;

        public RollResult(int roll, String die, String text, String book) {
            this.roll = roll;
            this.die = die;
            this.text = text;
            this.book = book;
        }

        @Override
        public String toString() {
            return "[" + die + " = " + roll + "] " + text;
        }
    }

    private NpcBookCatalogEngine() {}

    public static String[] getOriginBooks() {
        return new String[]{
                BOOK_GMB_NPC,
                BOOK_MASKS_1000,
                BOOK_RANDOM_ENCOUNTERS,
                BOOK_CATALOG_300,
                "Statblock Compendium"
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

    // =========================================================================
    // TABELAS D20 DE APARÊNCIA FÍSICA POR LIVRO
    // =========================================================================
    private static final String[] APPEARANCES_GMB_NPC = new String[]{
            "Cicatriz em forma de garra cruzando a bochecha esquerda e barba rala por fazer.",
            "Olhar penetrante, queixo quadrado e postura militar perfeitamente ereta.",
            "Cabelos grisalhos desgrenhados e dedos permanentemente manchados de óleo e fuligem.",
            "Estatura imponente, ombros largos e peito coberto por colete de couro trabalhado.",
            "Rosto afunilado com sardas espalhadas e um sorriso irônico perpétuo nos lábios.",
            "Usa uma faixa de seda púrpura cobrindo o olho direito cego e brincos de prata.",
            "Corpo esguio e ágil, vestindo túnica verde-escura com fechos de osso entalhado.",
            "Mãos calejadas e nós dos dedos engrossados por anos de trabalho pesado na forja.",
            "Voz rouca e profunda, semblante cansado com olheiras escuras sob olhos castanhos.",
            "Vestimentas impecáveis de linho fino, anéis de ouro e aroma suave de água de rosas.",
            "Tatuagem de bússola desbotada no pescoço e pele crestada pelo sol do alto-mar.",
            "Cabelos ruivos compridos presos em trança lateral e olhar desconfiado e atento.",
            "Porte nobre e esguio, trajando gibão de veludo azul-marinho com botões de madrepérola.",
            "Careca lustrosa, sobrancelhas grossas unidas e dentes incisivos afiados.",
            "Capa de viagem puída com capuz profundo que esconde a maior parte de suas feições.",
            "Usa muleta de carvalho esculpida com runas e manca pronunciadamente da perna direita.",
            "Dentes manchados de tabaco e casaco de lã pesada com cheiro de fumaça de turfa.",
            "Pele muito pálida, quase translúcida, e olhos cinzentos que parecem não pestanejar.",
            "Compleição atarracada e musculosa, adornada com braceletes de latão polido.",
            "Sorriso caloroso e dentes brancos, com covinhas nas bochechas e olhar jovial."
    };

    private static final String[] APPEARANCES_MASKS_1000 = new String[]{
            "Nariz aquilino quebrado duas vezes, dando um aspecto de falcão predador.",
            "Heterocromia marcante: o olho esquerdo é azul gélido e o direito é castanho escuro.",
            "Unhas longas e limpas tratadas com lixa de quartzo e pele perfumada com lavanda.",
            "Veste um sobretudo de retalhos de couro exótico (incluindo escamas de réptil).",
            "Cabelos pretos oleosos caindo sobre a testa e tatuagem rúnica azul no lóbulo da orelha.",
            "Cicatrizes cirúrgicas precisas ao longo dos pulsos cobertas por faixas de linho cru.",
            "Porta um tapa-olho de veludo bordado com o desenho de uma serpente prateada.",
            "Queixo bipartido marcante e dente canino superior substituído por ouro polido.",
            "Cabelo cortado bem rente ao couro cabeludo revelando marcas de queimadura na têmpora.",
            "Usa óculos de lentes redondas com bordas de latão e braçadeiras de couro nos punhos.",
            "Passos tão silenciosos que parece flutuar sobre o chão de tábuas de madeira.",
            "Gola alta e bufante no estilo aristocrático antigo, contrastando com calças rasgadas.",
            "Tatuagem mística viva no antebraço que pulsa com leve luz violeta quando se irrita.",
            "Físico atlético e flexível com tatuagens tribais nas panturrilhas e pés descalços.",
            "Mãos delicadas com dedos compridos ideais para prestidigitação ou tocar alaúde.",
            "Costas levemente recurvadas sob o peso de uma mochila repleta de frascos de vidro.",
            "Olhar distante e sonhador, cabelos brancos como neve presos por grampos de prata.",
            "Balança suavemente de um lado para o outro enquanto parado, como se ainda estivesse no convés.",
            "Barba espessa trançada com pequenos ossos de animais e contas de argila vermelha.",
            "Voz melodiosa e sedutora com leve sotaque de terras costeiras distantes."
    };

    private static final String[] APPEARANCES_RANDOM_ENCOUNTERS = new String[]{
            "Armadura de placas amassada com marcas recentes de garras de fera no peitoral.",
            "Roupas de caçador camufladas com folhas e musgo fresco das matas de Evernura.",
            "Manto vermelho-sangue com capuz e bordas encharcadas de lama de estrada batida.",
            "Carrega um cinto de utilidades com pequenas bolsas de sal, alho e frascos de água benta.",
            "Rosto queimado de sol com rugas profundas ao redor dos olhos e olhar de náufrago.",
            "Bota de cano longo com uma adaga curva de lâmina envenenada presa na bainha externa.",
            "Capa de viagem forrada de pele de urso e um cajado de teixo com cabeça de corvo de bronze.",
            "Túnica simples de peregrino empoeirada com um rosário de madeira escura no pescoço.",
            "Traje de marinheiro manchado de salitre e tatuagem de tridente no dorso da mão.",
            "Veste avental de açougueiro de couro grosso com manchas escuras de gordura e sangue seco.",
            "Casaco de oficial da marinha desgastado com dragonas desfiadas e botões faltando.",
            "Cabelos presos em coque apertado com duas agulhas de tricô de prata pontiagudas.",
            "Bolsas de couro penduradas nos ombros tilintando com moedas, pregos e bugigangas de ferro.",
            "Sorriso desdentado e olhar febril de quem passou dias isolado em uma masmorra escura.",
            "Veste um gibão reforçado com placas de ossos de monstros marinhos das ilhas do sul.",
            "Pele bronzeada com poeira vermelha de pedreira nos cabelos e cílios.",
            "Olhar frenético de vigília contínua, segurando a bainha da espada a qualquer som súbito.",
            "Veste roupas de cortesã de seda verde-esmeralda sob uma capa rústica de lã cinzenta.",
            "Corpo coberto por cicatrizes de chicotadas antigas e marcas de grilhões nos tornozelos.",
            "Traz um papagaio sarcástico empoleirado no ombro ou um furão no bolso do colete."
    };

    private static final String[] APPEARANCES_CATALOG_300 = new String[]{
            "Aparência de padeira rústica com farinha nos antebraços e voz vigorosa.",
            "Ferreiro atarracado com avental de couro cru chamuscado e cheiro constante de carvão mineral.",
            "Escriba franzino de óculos tortos e dedos permanentemente azuis de tinta nanquim.",
            "Taverneiro corpulento de bigode espesso que seca canecas de cerveja enquanto examina a porta.",
            "Mendigo encurvado coberto por mantas de lã puída que pede esmolas com voz sussurrada.",
            "Guarda veterano com elmo de ferro desgastado e cotas de malha bem oleadas contra ferrugem.",
            "Alquimista de cabelos chamuscados com queimaduras leves de ácido nos nós dos dedos.",
            "Comerciante de queijos e embutidos com barriga saliente e sorriso de bom negociante.",
            "Cortesã refinada com maquiagem de pó de arroz, lábios carmesim e leque de penas de pavão.",
            "Bucaneiro com chapéu tricórnio inclinado, dente de tubarão no pescoço e casaco de lã crua.",
            "Cultista disfarçado de monge de túnica cinzenta com olhar calmo demais para ser sincero.",
            "Rastreador de Evernura com botas impermeáveis de pele de foca e arco longo nas costas.",
            "Erudita com bolsa tiracolo estufada de pergaminhos antigos e mapas astronômicos.",
            "Pescador de costeira com cheiro de maré e dedos ágeis em desatar nós de corda.",
            "Menestrel com casaco xadrez chamativo e alaúde de madeira de cerejeira polida.",
            "Cavaleiro andante com tabardo manchado de poeira da estrada e olhar altivo e solene.",
            "Carpinteira naval com serrote preso ao cinto e lápis de carpinteiro atrás da orelha.",
            "Herbalista idosa com cesto de raízes de mandrágora e sachês de camomila no pescoço.",
            "Ladino de beco com capuz cinzento, botas de sola macia e sorriso furtivo.",
            "Nobre decadente com casaco de veludo roído por traças e anel com brasão raspado."
    };

    // =========================================================================
    // TABELAS D20 DE PECULIARIDADES & MANEIRISMOS POR LIVRO
    // =========================================================================
    private static final String[] QUIRKS_GMB_NPC = new String[]{
            "Mastiga sementes de cardamomo sem parar e sopra fumaça imaginária entre as frases.",
            "Termina quase todas as perguntas com um 'não é mesmo?' conspiratório.",
            "Bate suavemente o pomo da adaga na mesa ao enfatizar pontos importantes da fala.",
            "Nunca se senta de costas para qualquer porta ou janela aberta do ambiente.",
            "Limpa e pole uma moeda de prata específica no casaco repetidamente enquanto escuta.",
            "Fala aos sussurros e inclina a cabeça como se ouvisse vozes através das paredes.",
            "Usa metáforas navais e termos marítimos para descrever qualquer situação cotidiana.",
            "Ajusta um monóculo rachado que parece não usar realmente para enxergar.",
            "Dá uma risadinha nervosa aguda antes de responder a qualquer pergunta direta.",
            "Cheira discretamente tudo o que lhe é oferecido antes de tocar ou consumir.",
            "Coleciona chaves antigas de ferro enferrujadas presas a um aro na cintura.",
            "Tem o hábito de desenhar círculos arcanos no ar com a ponta dos dedos ao pensar.",
            "Fala consigo mesmo na terceira pessoa em momentos de estresse ou perigo.",
            "Recusa-se a tocar em moedas de cobre, aceitando apenas prata, ouro ou trocas.",
            "Arranca fiapos invisíveis das roupas dos outros durante conversas íntimas.",
            "Cita provérbios anões antigos de batalha mesmo em negociações comerciais pacíficas.",
            "Evita pronunciar os nomes dos deuses em voz alta, usando apenas títulos indiretos.",
            "Olha fixamente para o peito das pessoas em vez de encarar seus olhos.",
            "Carrega sempre uma maçã verde que corta meticulosamente em fatias com um canivete.",
            "Pede desculpas excessivas mesmo quando claramente foi vítima de grosseria alheia."
    };

    private static final String[] QUIRKS_MASKS_1000 = new String[]{
            "Tem um tique de piscar o olho esquerdo três vezes seguidas antes de mentir.",
            "Carrega um ratinho albino manso no bolso do peito que alimenta com migalhas de pão.",
            "Fala com extrema lentidão e pausas dramáticas para obrigar os outros a ouvi-lo.",
            "Muda de postura bruscamente dependendo de quem julga ter mais autoridade na sala.",
            "Usa anéis pesados de latão em todos os dedos e os faz estalar contra os dentes.",
            "Anota palavras incomuns ditas pelos aventureiros em um caderninho de capa preta.",
            "Cheira fortemente a essência de cravo e canela para mascarar o cheiro de um remédio amargo.",
            "Tem mania de contar os degraus das escadas ou as portas dos cômodos em voz audível.",
            "Nunca faz juramentos nem diz 'eu prometo', substituindo sempre por 'se os ventos soprarem'.",
            "Bebe apenas água fervida ou chá fraco e recusa bebidas alcoólicas categoricamente.",
            "Finge não ouvir comentários agressivos e responde a insultos com elogios desconcertantes.",
            "Possui um riso descompassado e histérico que surge quando a situação fica perigosa.",
            "Usa frases feitas de livros de romance barato para descrever conflitos reais de sangue.",
            "Toca compulsivamente uma pequena cicatriz em forma de cruz atrás da orelha direita.",
            "Exibe orgulhosamente uma medalha militar desgastada cuja origem ele inventa toda vez.",
            "Carrega um frasquinho de sal que derrama sobre o ombro esquerdo toda vez que vê um gato preto.",
            "Gesticula como um maestro de ópera ao explicar direções ou planos para o grupo.",
            "Fala com uma polidez gélida e refinada mesmo quando está com uma faca no pescoço.",
            "Esfrega os nós dos dedos constantemente como se sentisse dor reumática persistente.",
            "Tem o costume de imitar a entonação da última pessoa com quem conversou."
    };

    private static final String[] QUIRKS_RANDOM_ENCOUNTERS = new String[]{
            "Olha constantemente para cima, para telhados e copas de árvores, temendo emboscadas aéreas.",
            "Mantém uma mão repousando casualmente sobre a besta de mão escondida sob o manto.",
            "Masca fumo amargo e cospe no chão antes de fechar qualquer acordo ou trato.",
            "Carrega um saco de estopa que se recusa a soltar e que parece se mover levemente por dentro.",
            "Sempre que ouve um estrondo ou relâmpago, agacha-se instintivamente procurando cobertura.",
            "Faz perguntas indiscretas sobre a família e terra natal dos heróis com interesse exagerado.",
            "Usa luvas de couro grossas até mesmo em dias de calor escaldante e nunca as retira.",
            "Tem o hábito bizarro de conversar com sua própria arma chamando-a pelo nome de uma mulher.",
            "Dorme com um olho meio aberto e acorda golpeando o ar com uma adaga ao menor toque.",
            "Acredita piamente que corvos são espiões de feiticeiros e atira pedras toda vez que vê um.",
            "Examina atentamente cada moeda recebida mordendo-a ou arranhando na sola da bota.",
            "Canta cantigas folclóricas sombrias de morte em tom alegre e infantil.",
            "Possui uma cicatriz recente de queimadura que coça furiosamente ao ficar nervoso.",
            "Carrega consigo um pote de barro com minhocas luminescentes para iluminar o caminho.",
            "Reclama incessantemente do clima, da lama, dos guardas, da cerveja e dos impostos.",
            "Oferece amuletos feitos de ossos de galinha jurando que protegem contra raios e bruxaria.",
            "Usa uma capa com dezenas de bolsos internos dos quais tira objetos absurdos e inesperados.",
            "Mede mentalmente a altura dos combatentes e calcula em voz alta quantas tábuas seriam precisas para um caixão.",
            "Fica subitamente mudo quando o assunto envolve a floresta de Evernura ou as 12 Torres.",
            "Sorri com entusiasmo exagerado e tenta vender qualquer quinquilharia que os heróis elogiem."
    };

    private static final String[] QUIRKS_CATALOG_300 = new String[]{
            "Fala alto com gestos amplos, acostumado ao barulho de salões cheios e tavernas.",
            "Desconfiado de promessas verbais, exige sempre aperto de mão firme e um gole partilhado.",
            "Tem pavor de cobras e foge de qualquer menção a répteis ou escamas.",
            "Fala sussurrando como se cada informação banal fosse segredo de estado.",
            "Costuma acariciar a barba enquanto calcula mentalmente moedas e prazos de entrega.",
            "Nunca come nada sem antes oferecer um pedaço aos presentes para testar boa-fé.",
            "Usa expressões do dialeto anão arcaico para xingar quando fica surpreso.",
            "Anda com as mãos cruzadas atrás das costas e postura de quem está inspecionando uma tropa.",
            "Balança um chaveiro de bronze com ritmo metálico compassado ao caminhar.",
            "Evita pisar nas frestas das lajes de pedra das calçadas por pura superstição.",
            "Ri alto e dá tapas nas costas de pessoas com quem mal acabou de conversar.",
            "Tem um tique no canto dos lábios quando tenta esconder um segredo comercial.",
            "Coleciona fitas coloridas que amarra nos dedos para lembrar de tarefas pendentes.",
            "Fala de seus ancestrais com orgulho feroz e exige respeito à história da família.",
            "Mastiga pontas de canela para disfarçar o cheiro de suor e forja pesada.",
            "Usa provérbios de pescadores de alto-mar para explicar a política da corte.",
            "Olha com desdém para roupas extravagantes de aventureiros nobres e esnobes.",
            "Ajeita as mangas da túnica compulsivamente após cada aperto de mãos.",
            "Tem o hábito de abençoar os alimentos em nome de deuses locais antes de qualquer refeição.",
            "Costuma terminar frases piscando um olho como se houvesse uma cumplicidade implícita."
    };

    // =========================================================================
    // TABELAS D20 DE SEGREDOS & MOTIVAÇÕES POR LIVRO
    // =========================================================================
    private static final String[] SECRETS_GMB_NPC = new String[]{
            "É um informante pago pela guarda que vende informações dos clientes da taverna.",
            "Assassinou o antigo sócio e assumiu o negócio forjando um testamento em cartório.",
            "Está escondendo o filho desertor de um exército nobre no porão da casa.",
            "Trabalha em segredo para contrabandistas de South Sea guardando mercadorias roubadas.",
            "Possui uma dívida impagável com a guilda dos ladrões e tem prazo até a próxima lua cheia.",
            "Na juventude foi um bandido perigoso e teme ser reconhecido por caçadores de recompensas.",
            "Guarda um artefato roubado de um templo élfico escondido sob o piso falso da oficina.",
            "É chantageado por um nobre influente que exige relatórios diários sobre viajantes.",
            "É o herdeiro legítimo deserdado de um baronato que finge ser um simples artesão.",
            "Sofre de uma maldição mágica que lentamente transforma sua carne em pedra.",
            "Vende poções adulteradas diluídas com água de poço e teme que alguém descubra.",
            "Já foi prisioneiro nas masmorras cruéis e tem pesadelos todas as noites com correntes.",
            "Está apaixonado secretamente pela esposa do capitão da guarda da cidade.",
            "Recebe cartas anônimas com ameaças de morte escritas em sangue seco de bode.",
            "Conhece a entrada de uma passagem subterrânea secreta que leva direto ao cofre da cidade.",
            "Trabalha secretamente como espião para um culto proibido dedicado a entidades abissais.",
            "Envenenou lentamente o pai para herdar o título e as propriedades da família.",
            "Roubou a fórmula alquímica de um mago das 12 Torres Peculiares e a vende como sua.",
            "É um doppelganger que assumiu a identidade do NPC real há três meses.",
            "Planeja fugir da cidade na calada da noite com todo o dinheiro guardado na taverna."
    };

    private static final String[] SECRETS_MASKS_1000 = new String[]{
            "Guarda um medalhão com o retrato de uma nobre assassinada cuja morte ele não evitou.",
            "Sabe onde está enterrado o baú com a partilha de um massacre de corsários.",
            "Finge ser analfabeto para ouvir conversas confidenciais de eruditos sem despertar suspeitas.",
            "Está vendendo os segredos militares da guarnição para espiões de outra província.",
            "É procurado por bigamia e abandono de família em três cidades portuárias diferentes.",
            "Acredita ouvir a voz de sua irmã gêmea falecida ecoando através do espelho de seu quarto.",
            "Está enfeitiçado por uma dríade da floresta de Evernura e deve obedecer a uma ordem futura.",
            "Trocou a filha recém-nascida por uma bolsa de joias durante um cerco de fome há 18 anos.",
            "Carrega uma tatuagem de prisioneiro das galés de guerra raspada com ferro quente.",
            "É o único sobrevivente de uma expedição à Torre do Vórtice Abissal e perdeu a sanidade parcialmente.",
            "Falsifica licenças de comércio naval carimbadas com o selo real da marinha imperial.",
            "Pertence à resistência clandestina que planeja assassinar o regente local no próximo festival.",
            "Esconde barras de ouro roubadas da igreja local dentro de estátuas de santos ocos.",
            "É um mago com a licença revogada que ainda pratica feitiçaria em segredo nos becos.",
            "Tem um acordo com um demônio menor que exige a entrega de segredos alheios a cada solstício.",
            "Sabe que o nobre mais respeitado da cidade é na verdade o líder da guilda dos ladrões.",
            "Sua arma favorita foi roubada do túmulo de um cavaleiro sagrado em Evernura.",
            "Está lentamente enlouquecendo por ter lido três páginas de um tomo das 12 Torres Peculiares.",
            "Foi contratado para envenenar a comida de um dos heróis do grupo por um rival do passado.",
            "Possui uma chave mestra capaz de abrir qualquer cela da prisão municipal."
    };

    private static final String[] SECRETS_RANDOM_ENCOUNTERS = new String[]{
            "Está fugindo de uma patrulha após testemunhar um massacre cometido por cavaleiros da ordem.",
            "Carrega uma mensagem lacrada que ordenam destruir se for capturado vivo.",
            "Tem um mapa falso que planeja vender a aventureiros ingênuos para levá-los a uma emboscada.",
            "É na verdade um assassino profissional disfarçado aguardando o momento certo para o golpe.",
            "O cavalo ou mula que está puxando foi roubado de um mercador na estalagem anterior.",
            "Sabe que a ponte principal da estrada foi sabotada por saqueadores e não avisou ninguém.",
            "Está infectado com licantropia (mordido por um lobisomem) e a lua cheia é hoje à noite.",
            "Carrega veneno potente escondido dentro da fivela do cinto para uso emergencial.",
            "É parente próximo de um chefe orc ou líder bandido que aterroriza as caravanas da região.",
            "Fugiu de um casamento forçado com um lorde tirano e está usando nome falso.",
            "Sabe a combinação do cofre de uma das caravelas mercantes atracadas no porto.",
            "Enterrou o corpo de um rival na curva da estrada a menos de duas léguas daqui.",
            "Recebeu moedas de ouro falsas e está desesperado para passá-las adiante para o primeiro viajante.",
            "Tem visões proféticas de incêndio e destruição que acontecem exatamente três dias depois.",
            "Está servindo de isca involuntária: uma alcateia de feras famintas está farejando seu rastro.",
            "Trabalha para um agiota implacável que sequestrou sua filha como garantia de pagamento.",
            "Sabe que o vinho servido na taverna local foi batizado com sonífero para um assalto.",
            "Roubou as botas e a armadura do cadáver de um aventureiro que encontrou na trilha.",
            "É um desertor que finge ter sido dispensado com honras militares do regimento da fronteira.",
            "Guarda um pergaminho com um feitiço proibido que não sabe como conjurar nem destruir."
    };

    private static final String[] SECRETS_CATALOG_300 = new String[]{
            "Esconde que a farinha usada no pão vem de trigo comprado de contrabandistas sem pagar taxa.",
            "Usa ferro de má qualidade disfarçado com pintura de graxa nas ferraduras de cavalos da guarda.",
            "Guarda cópia de cartas confidenciais de clientes para garantir que não será despejado.",
            "Dorme com uma faca sob o travesseiro com medo do retorno de parentes vingativos.",
            "Paga propina semanal a um sargento corrupto da patrulha para evitar batidas na loja.",
            "Foi testemunha de um duelo ilegal de nobres e foi ameaçado de morte caso fale.",
            "Falsifica rótulos de vinhos comuns vendendo-os como safras finas das ilhas de South Sea.",
            "Tem vergonha de ter nascido na miséria e gasta todo o lucro em roupas que mal pode pagar.",
            "É irmão gêmeo de um bandido procurado na estrada e teme ser enforcado por engano.",
            "Guarda cinzas de um altar profanado dentro de uma botija de cerâmica na despensa.",
            "Usa uma balança viciada para roubar alguns gramas de ouro ou pó em cada transação.",
            "Sabe quem botou fogo no celeiro do vizinho no ano passado mas jurou silêncio por dinheiro.",
            "Esconde poções de amor ilegais e venenos de rato refinados atrás do balcão de especiarias.",
            "Deseja desesperadamente comprar a liberdade de um parente escravizado nas galés.",
            "Perdeu todas as economias da família no jogo de dados e está prestes a perder a loja.",
            "Acredita que a casa que comprou é assombrada pelo fantasma do antigo dono enforcado.",
            "Mantém um altar secreto a Tymora no sótão mas diz a todos que só adora Helm.",
            "É analfabeto e paga um menino de rua para ler os contratos oficiais que assina.",
            "Recebeu um baú selado com cera vermelha para guardar por um ano sem fazer perguntas.",
            "Sabe que o capitão do navio mercante planeja abandonar a tripulação com a carga de joias."
    };

    // =========================================================================
    // TABELAS D20 DE GANCHOS DE AVENTURA POR LIVRO
    // =========================================================================
    private static final String[] HOOKS_GMB_NPC = new String[]{
            "Oferece 150 PO para quem recuperar sua carruagem de mercadorias emboscada na estrada sul.",
            "Pede que os heróis entreguem uma carta selada sem abri-la para um contato no bordel local.",
            "Afirma que ouviu barulhos de escavação sob o piso de sua taverna durante as madrugadas.",
            "Precisa de escolta armada para viajar até as colinas onde fica a oficina de seu mestre ferreiro.",
            "Quer contratar o grupo para descobrir quem está espalhando boatos infamantes sobre seu negócio.",
            "Oferece hospedagem e comida grátis por um mês em troca de eliminar uma praga de ratos gigantes.",
            "Sabe que um nobre está contratando mercenários para uma expedição à Torre do Vidro Cantante.",
            "Deseja comprar uma erva rara que só nasce nas margens do Bosque dos Lamentos em Evernura.",
            "Pede aos heróis que sirvam de testemunhas neutras durante uma partilha tensa de herança.",
            "Precisa de um grupo discreto para recuperar um documento comprometedor no cofre da prefeitura.",
            "Afirma ter visto um estranho vulto com olhos incandescentes rondando o cemitério da cidade.",
            "Oferece uma adaga mágica de herança familiar como pagamento para resgatar seu sobrinho raptado.",
            "Quer descobrir quem envenenou os cavalos de sua estalagem antes do grande festival.",
            "Pede ajuda para decifrar as runas de um mapa antigo encontrado no forro de uma arca velha.",
            "Contrata guarda-costas para uma reunião noturna com agiotas armados em um armazém do cais.",
            "Procura aventureiros para resgatar um navio à deriva visto perto dos recifes de South Sea.",
            "Precisa de um conjurador para quebrar uma maldição de sono que atingiu sua filha mais nova.",
            "Oferece pistas sobre a entrada esquecida de uma masmorra cruel cheia de armadilhas letais.",
            "Pede que o grupo investigue o desaparecimento misterioso de três pescadores na costa.",
            "Deseja encomendar a captura viva de um monstro raro para vendê-lo a um colecionador da corte."
    };

    private static final String[] HOOKS_MASKS_1000 = new String[]{
            "Promete revelar o ponto fraco de uma guilda de contrabandistas em troca de proteção.",
            "Pede que o grupo compre de volta um anel de família empenhado com um agiota implacável.",
            "Afirma ter recebido uma mensagem em código que prevê um atentado contra o templo amanhã.",
            "Oferece sociedade em uma mina de ferro desativada se o grupo expulsar os monstros locais.",
            "Implora para que os heróis encontrem seu irmão que viajou para as 12 Torres Peculiares.",
            "Sabe de um navio pirata encalhado nos corais de South Sea com o porão ainda lacrado.",
            "Contrata o grupo para agir como seus guarda-costas durante um banquete de aristocratas venenosos.",
            "Deseja obter uma tatuagem mística específica e precisa de escolta até o tatuador recluso.",
            "Oferece um baú de poções curativas em troca de ingredientes extraídos de feras da floresta.",
            "Pede ajuda para transportar um prisioneiro perigoso antes que seus aliados venham resgatá-lo.",
            "Diz saber quem é o traidor dentro da patrulha municipal, mas só fala longe dos portões.",
            "Precisa de alguém de confiança para entregar um pagamento de chantagem sem ser seguido.",
            "Afirma que o poço municipal está sendo contaminado por bruxas da Irmandade da Lua Pálida.",
            "Pede que os heróis encontrem seu cão de caça premiado perdido nas Cavernas de Âmbar.",
            "Quer contratar ladrões experientes para recuperar uma joia que lhe foi tomada injustamente.",
            "Oferece uma passagem marítima secreta se o grupo ajudá-lo a escapar da cidade antes do amanhecer.",
            "Sabe a localização de um cemitério antigo de caravelas cheias de esqueletos amaldiçoados.",
            "Pede aos aventureiros que entreguem um desafio formal de duelo a um valentão da corte.",
            "Oferece uma espada forjada com aço especial se o grupo lhe trouxer carvão de forja elemental.",
            "Implora para que o grupo descubra por que os mortos do vilarejo de Val-Frio estão sumindo das covas."
    };

    private static final String[] HOOKS_RANDOM_ENCOUNTERS = new String[]{
            "Está preso numa armadilha de caça na margem da estrada e promete recompensas generosas por socorro.",
            "Sua carroça quebrou o eixo e homens armados com arcos foram avistados no cume da colina.",
            "Foge apavorado gritando que um bando de goblins saqueadores tomou a ponte adiante.",
            "Tenta vender um artefato mágico de segunda mão por metade do valor com urgência suspeita.",
            "Oferece pagar um banquete na taverna se os heróis aceitarem ouvir uma proposta arriscada de pilhagem.",
            "Acusa o guarda do portão de extorsão e pede que aventureiros respeitados intervenham.",
            "Afirma ter encontrado um baú enterrado com o selo de um lorde assassinado há dez anos.",
            "Procura ajuda para apagar o incêndio de um moinho de vento onde pessoas ficaram presas.",
            "Diz que viu fogueiras e cantos pagãos no meio do Bosque dos Lamentos na noite passada.",
            "Oferece informações privilegiadas sobre as apostas na rinha ou jogos de cartas da taberna.",
            "Pede água e comida com urgência, afirmando ter escapado há poucas horas de um cativeiro.",
            "Quer contratar o grupo para recuperar uma relíquia religiosa roubada por cultistas em fuga.",
            "Desafia o guerreiro mais forte do grupo para uma disputa de queda de braço apostando ouro.",
            "Avisa que a água do riacho adiante está amaldiçoada e cheia de peixes mortos com olhos negros.",
            "Pede ajuda para transportar um barril pesado que insiste que contém apenas 'vinho raro'.",
            "Sabe que corsários planejam invadir o porto desprotegido durante a maré alta de hoje.",
            "Precisa de aventureiros para servir de árbitros em uma disputa sangrenta de fronteira de terras.",
            "Oferece uma chave dourada misteriosa que achou no estômago de um grande peixe pescado no mar.",
            "Pede que os heróis encontrem uma criança que seguiu uma fada brilhante para dentro da floresta.",
            "Contrata uma patrulha noturna após encontrar marcas de garras gigantes na porta de sua casa."
    };

    private static final String[] HOOKS_CATALOG_300 = new String[]{
            "Precisa de escolta para uma entrega de suprimentos até o vilarejo vizinho que está isolado.",
            "Oferece descontos e favores se o grupo conseguir que o lorde local reduza as taxas da ponte.",
            "Pede que os heróis descubram quem está roubando galinhas e ferramentas todas as noites.",
            "Quer contratar aventureiros para resgatar seu aprendiz que se perdeu nas catacumbas.",
            "Oferece um quarto privativo se o grupo intermediar uma dívida com uma gangue da cidade.",
            "Precisa de ajuda para expulsar bêbados violentos que estão ameaçando os clientes da taberna.",
            "Pede para os heróis comprarem ingredientes raros no mercado negro em seu nome.",
            "Oferece uma rodada de hidromel grátis em troca de notícias recentes das capitais distantes.",
            "Quer ajuda para construir uma paliçada de emergência contra ataques de feras da mata.",
            "Pede que os aventureiros vigiem seu armazém durante o eclipse lunar desta noite.",
            "Afirma ter visto um homem encapuzado jogando algo suspeito no reservatório de água.",
            "Oferece conserto gratuito de armas e armaduras se o grupo trouxer minério de boa procedência.",
            "Pede para os heróis entregarem uma quantia em dinheiro à sua mãe idosa em outra vila.",
            "Quer que o grupo confronte um comerciante rival que está vendendo mercadorias roubadas.",
            "Pede auxílio para desentupir um poço antigo onde algo monstruoso começou a respirar.",
            "Oferece uma carona em sua carroça até a cidade grande se o grupo fizer a segurança na estrada.",
            "Pede ajuda para capturar um gatuno ágil que roubou sua bolsa de moedas no meio da feira.",
            "Quer que os heróis testem uma nova poção ou refeição especial que promete vigor heróico.",
            "Oferece abrigo seguro contra a guarda municipal em troca de ajuda em uma causa pessoal.",
            "Pede aos heróis que levem uma mensagem de amor a uma cortesã proibida da corte nobre."
    };

    // =========================================================================
    // MÉTODOS DE CONSULTA E ROLAGEM DE DADOS (D20 / TABELAS)
    // =========================================================================

    public static String[] getAppearances(String book) {
        if (BOOK_MASKS_1000.equalsIgnoreCase(book)) return APPEARANCES_MASKS_1000;
        if (BOOK_RANDOM_ENCOUNTERS.equalsIgnoreCase(book)) return APPEARANCES_RANDOM_ENCOUNTERS;
        if (BOOK_CATALOG_300.equalsIgnoreCase(book)) return APPEARANCES_CATALOG_300;
        return APPEARANCES_GMB_NPC;
    }

    public static String[] getQuirks(String book) {
        if (BOOK_MASKS_1000.equalsIgnoreCase(book)) return QUIRKS_MASKS_1000;
        if (BOOK_RANDOM_ENCOUNTERS.equalsIgnoreCase(book)) return QUIRKS_RANDOM_ENCOUNTERS;
        if (BOOK_CATALOG_300.equalsIgnoreCase(book)) return QUIRKS_CATALOG_300;
        return QUIRKS_GMB_NPC;
    }

    public static String[] getSecrets(String book) {
        if (BOOK_MASKS_1000.equalsIgnoreCase(book)) return SECRETS_MASKS_1000;
        if (BOOK_RANDOM_ENCOUNTERS.equalsIgnoreCase(book)) return SECRETS_RANDOM_ENCOUNTERS;
        if (BOOK_CATALOG_300.equalsIgnoreCase(book)) return SECRETS_CATALOG_300;
        return SECRETS_GMB_NPC;
    }

    public static String[] getHooks(String book) {
        if (BOOK_MASKS_1000.equalsIgnoreCase(book)) return HOOKS_MASKS_1000;
        if (BOOK_RANDOM_ENCOUNTERS.equalsIgnoreCase(book)) return HOOKS_RANDOM_ENCOUNTERS;
        if (BOOK_CATALOG_300.equalsIgnoreCase(book)) return HOOKS_CATALOG_300;
        return HOOKS_GMB_NPC;
    }

    public static String[] getAppearances() { return getAppearances(BOOK_GMB_NPC); }
    public static String[] getQuirks() { return getQuirks(BOOK_GMB_NPC); }
    public static String[] getSecrets() { return getSecrets(BOOK_GMB_NPC); }
    public static String[] getHooks() { return getHooks(BOOK_GMB_NPC); }
    public static String[] getAdventureHooks() { return getHooks(BOOK_GMB_NPC); }
    public static String[] getAdventureHooks(String book) { return getHooks(book); }

    public static RollResult rollAppearance(String book) {
        String[] table = getAppearances(book);
        int roll = RNG.nextInt(table.length) + 1;
        return new RollResult(roll, "d" + table.length, table[roll - 1], book);
    }

    public static RollResult rollQuirk(String book) {
        String[] table = getQuirks(book);
        int roll = RNG.nextInt(table.length) + 1;
        return new RollResult(roll, "d" + table.length, table[roll - 1], book);
    }

    public static RollResult rollSecret(String book) {
        String[] table = getSecrets(book);
        int roll = RNG.nextInt(table.length) + 1;
        return new RollResult(roll, "d" + table.length, table[roll - 1], book);
    }

    public static RollResult rollHook(String book) {
        String[] table = getHooks(book);
        int roll = RNG.nextInt(table.length) + 1;
        return new RollResult(roll, "d" + table.length, table[roll - 1], book);
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
