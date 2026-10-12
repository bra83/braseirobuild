package com.braseiro.dnd5e.canon;

import com.braseiro.dnd5e.NarrativeContentEngine.Entity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo canônico estruturado dos reinos feéricos e da antiguidade mágica:
 * - Lorwyn: First Light (LFL)
 * - Netheril's Fall (NF)
 * - Arcana Unleashed (AU)
 *
 * Abrange:
 * - Lorwyn & Shadowmoor: Aeghith Complex, Grande Aurora (Great Aurora), Boggarts, Flamekin, Elfos da Beleza Implacável, Kithkin douts.
 * - Netheril Antigo: Enclave Flutuante de Xinok, O Sanctum de Karsus, Arquimago Larloch, Torres do Tempo (Time Gates), Mythal Mágico.
 * - Arcana Unleashed: Tecelões do Horizonte (Horizon Weavers), O Cofre Arcano Multiversal, Facções mágicas (Ninth Quill, Bejeweled Conclave).
 */
public final class LorwynNetherilArcanaCanonCatalog {
    private LorwynNetherilArcanaCanonCatalog() {}

    public static List<Entity> all() {
        List<Entity> list = new ArrayList<>();

        // 1. Lorwyn & Shadowmoor (LFL)
        list.add(new Entity(
                "lorwyn_aeghith_complex", "LFL", "lorwyn", "dawn_era", "location",
                "O Complexo Aeghith", "The Aeghith Complex",
                "Monumento arquitetônico feérico sagrado em Lorwyn onde artesãos e inventores exibem maravilhas voadoras.",
                Arrays.asList(
                        "Contém o Salão Comunitário (Community Hall), Salas de Reunião, Sala de Relaxamento e Escritório do Diretor.",
                        "Ponto nevrálgico onde kithkin, elfos, flamekin e boggarts se encontram em relativa trégua festiva sob o sol perene."
                ),
                Arrays.asList(
                        "Durante o crepúsculo da Aurora, armadilhas sombrias e espantalhos de Shadowmoor tentam invadir as entradas auxiliares.",
                        "O escritório guarda tratados secretos sobre a Grande Aurora que transforma Lorwyn em Shadowmoor a cada três séculos."
                ),
                Arrays.asList("lorwyn", "aeghith", "feywild", "first_light", "architecture"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "lorwyn_aurora_nexus", "LFL", "lorwyn", "dawn_era", "location",
                "O Nexus da Grande Aurora", "The Great Aurora Nexus",
                "Ponto de convergência mística onde o céu idílico de Lorwyn transmuta-se no crepúsculo aterrorizante de Shadowmoor.",
                Arrays.asList(
                        "Ciclo cósmico onde a luz dourada perpétua dá lugar a noites eternas, espinheiros venenosos e criaturas amargas.",
                        "Habitantes de Lorwyn invertem personalidades na virada da Aurora: kithkin pacíficos tornam-se caçadores xenófobos e flamekin ardentes tornam-se rimekin gélidos."
                ),
                Arrays.asList(
                        "A deusa Oona comanda faeries para colher memórias e sonhos durante a transição aurora.",
                        "Fragmentos do nexus podem estabilizar portais permitindo que aventureiros transitem entre as duas eras."
                ),
                Arrays.asList("lorwyn", "shadowmoor", "great_aurora", "fey_cycle", "transformation"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "lorwyn_boggart_warrens", "LFL", "lorwyn", "dawn_era", "faction",
                "Tocas dos Boggarts", "Boggart Warrens",
                "Comunidades de goblins feéricos hedonistas que valorizam sensações novas, travessuras caóticas e tesouros peculiares.",
                Arrays.asList(
                        "Compartilham todas as experiências através de contos orais cheios de humor negro e festejos comunais.",
                        "Seu maior objetivo de vida é experimentar sabores inéditos, texturas bizarras e dores suportáveis engraçadas."
                ),
                Arrays.asList(
                        "Mantêm armazéns subterrâneos onde guardam itens roubados de outras raças que consideram presentes deixados por descuido.",
                        "Têm pactos secretos com cães e insetos gigantes dos pântanos de Lorwyn."
                ),
                Arrays.asList("lorwyn", "boggarts", "fey_goblins", "chaos", "curiosity"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "lorwyn_flamekin_hearth", "LFL", "lorwyn", "dawn_era", "faction",
                "Lareira dos Flamekin", "Flamekin Hearth",
                "Povo de seres elementais de fogo que personificam paixão pura, iluminação espiritual e empatia calorosa.",
                Arrays.asList(
                        "Possuem corpos incandescentes de rocha vulcânica e chamas vívidas que refletem seus estados emocionais.",
                        "Praticam meditação meditativa comunitária para harmonizar a chama individual com a Grande Alma Cósmica."
                ),
                Arrays.asList(
                        "Ao transformarem-se em Rimekin em Shadowmoor, suas chamas se apagam tornando-se seres de gelo cinzento consumidos pelo ressentimento.",
                        "Carregam carvões sagrados que nunca se extinguem, capazes de queimar ilusões mágicas."
                ),
                Arrays.asList("lorwyn", "flamekin", "elemental_fire", "passion", "spiritual"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        // 2. Netheril's Fall (NF)
        list.add(new Entity(
                "netheril_xinok_enclave", "NF", "forgotten_realms", "ancient_netheril", "location",
                "O Enclave Flutuante de Xinok", "Xinok's Floating Enclave",
                "Célula voadora monumental de Netheril antes da Loucura de Karsus, sustentada pelo poder de um mythallar perfeito.",
                Arrays.asList(
                        "Cidade invertida esculpida no topo de uma montanha levitada através de magia arcana de 10º e 11º círculos.",
                        "Lar de arcanistas netherenses que governavam com orgulho incomparável sobre povos terrestres."
                ),
                Arrays.asList(
                        "O mythallar central começou a apresentar microfraturas de sobrecarga causadas pela extração predatória de magia da Trama.",
                        "Laboratórios subterrâneos no núcleo da rocha conduzem pesquisas secretas sobre a criação dos Magen (humanoides de alquimia artificial)."
                ),
                Arrays.asList("netheril", "xinok", "floating_city", "mythallar", "ancient_arcana"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "netheril_karsus_sanctum", "NF", "forgotten_realms", "ancient_netheril", "location",
                "O Sanctum de Karsus", "Karsus's Sanctum",
                "O observatório arcano supremo onde o arcanista Karsus teceu a lendária magia de 12º nível: O Avatar de Karsus.",
                Arrays.asList(
                        "Construído para canalizar a essência de uma divindade e fundir sua mente com a própria consciência cósmica de Toril.",
                        "O feitiço usurpou momentaneamente o poder da deusa da magia Mystryl, rompendo a Trama e derrubando instantaneamente todas as cidades flutuantes."
                ),
                Arrays.asList(
                        "A árvore petrificada de pedra sangrenta no Deserto de Anauroch ainda pulsa com a energia do cadáver fossilizado de Karsus.",
                        "Pergaminhos originais do experimento detalham a teoria do Weave primordial antes das restrições divinas aos círculos superiores."
                ),
                Arrays.asList("netheril", "karsus", "cataclysm", "avatar_of_karsus", "spell_fall"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "netheril_larloch", "NF", "forgotten_realms", "ancient_netheril", "npc",
                "Larloch, o Rei das Sombras", "Larloch the Shadow King",
                "O sobrevivente mais antigo e temível do Império de Netheril, ex-governante arcanista e lich milenar.",
                Arrays.asList(
                        "Mestre supremo da cripta de Warlock's Crypt, comandando dezenas de liches menores e arcanistas escravizados.",
                        "Possui conhecimento arcano profundo sobre feitiços pré-cataclismo e armaduras forjadas na era dos deuses primordiais."
                ),
                Arrays.asList(
                        "Planeja absorver a energia mística de múltiplos mytals em Faerûn para reconstituir o domínio temporal de Netheril.",
                        "Mantém uma coleção de artefatos temporais (Time Gates) capazes de projetar observadores de volta aos dias do império."
                ),
                Arrays.asList("netheril", "larloch", "archmage", "lich", "ancient_history"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "netheril_time_gates", "NF", "forgotten_realms", "ancient_netheril", "misc",
                "Os Portais Temporais (Time Gates)", "Netherese Time Gates",
                "Arcos monolíticos de chronomancia construídos por Netheril para explorar linhas temporais e extrair reagentes do futuro.",
                Arrays.asList(
                        "Permitem viagens temporais restritas para testemunhar os últimos dias antes da catástrofe de Karsus.",
                        "Operados através de Baterias Arcanas e Agulhas do Tolo (Fool's Needle) alinhadas astronomicamente."
                ),
                Arrays.asList(
                        "O uso indevido cria anomalias de tempo (Time Hazards) que envelhecem ou rejuvenescem viajantes em instantes.",
                        "Estar no passado quando o Weave colapsar aprisiona almas no esquecimento temporal perpétuo."
                ),
                Arrays.asList("netheril", "time_travel", "chronomancy", "anomalies", "time_gates"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        // 3. Arcana Unleashed (AU)
        list.add(new Entity(
                "arcana_unleashed_weavers", "AU", "forgotten_realms", "present", "faction",
                "Os Tecelões do Horizonte", "Horizon Weavers",
                "Confraria de arcanistas transplanares que rastreiam, estabilizam e reparam fendas dimensionais entre o Multiverso.",
                Arrays.asList(
                        "Especialistas em magias de portais, distorção espacial e conjuração de seres de outros planos de existência.",
                        "Operam a partir de torres misteriosas erguidas em cruzamentos planares entre o Plano Material, Astral e Feywild."
                ),
                Arrays.asList(
                        "Mantêm o cofre Vault of Horizons selado com chaves místicas de diamante e prata estelar.",
                        "Monitores dos Tecelões detectaram o enfraquecimento das barreiras entre Toril e Ravenloft."
                ),
                Arrays.asList("au", "horizon_weavers", "planar_magic", "multiverse", "portals"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "arcana_unleashed_vault", "AU", "forgotten_realms", "present", "location",
                "O Cofre dos Arcanos Perdidos", "The Vault of Arcana",
                "Estrutura extradimensional construída pela mestre Alustriel e arquimagos aliados para conter magias proibidas desestabilizadoras.",
                Arrays.asList(
                        "Guarda o lendário Bestiário do Arcanista, Grimórios de Detonação e relíquias de evolução mágica (Evolving Magic Items).",
                        "Protegida por guardiões arcanos inteligentes (Spellguards) e magias vivas conscientes (Living Spells)."
                ),
                Arrays.asList(
                        "Contém o cajado Staff of Skulls e a couraça Breastplate of the Tyrant que evoluem conforme os feitos de quem os empunha.",
                        "Espiões da Nona Pena (Ninth Quill) planejam uma invasão coordenada aos cofres durante a próxima conjunção planar."
                ),
                Arrays.asList("au", "vault", "magic_items", "alustriel", "planar_dungeon"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        list.add(new Entity(
                "arcana_unleashed_ninth_quill", "AU", "forgotten_realms", "present", "faction",
                "A Nona Pena", "The Ninth Quill",
                "Sindicato secreto de ladinos arcanos, arrombadores dimensionais e escribas mercenários especializados em roubos de segredos mágicos.",
                Arrays.asList(
                        "Conhecidos por suas penas de escrita mágicas (Dictation Quills) que copiam grimórios selados sem quebrar selos de cera.",
                        "Vendem relíquias resgatadas de masmorras planares para patronos de alto escalão em Waterdeep e Sigil."
                ),
                Arrays.asList(
                        "Possuem esconderijos móveis ocultos dentro de pinturas encantadas penduradas em mansões de nobres faerunianos.",
                        "Seus mestres de roubo utilizam anéis de componente místico que dispensam gestos arcanos visíveis."
                ),
                Arrays.asList("au", "ninth_quill", "arcane_heist", "thieves", "guild"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        // 4. Bestiário e Regras Fundamentais (XMM & XSAC)
        list.add(new Entity(
                "xmm_dragon_ecology_2025", "XMM", "multiverse", "present", "misc",
                "Ecologia dos Dragões Cromáticos e Metálicos 2025", "Dragon Ecology 2025",
                "Diretrizes narrativas oficiais sobre o comportamento, covis, tesouros e influência regional das linhagens de dragões.",
                Arrays.asList(
                        "Dragões adultos exercem efeitos regionais permanentes que transformam o terreno a milhas de seus covis.",
                        "Ações de Covil e Resistência Lendária representam a vontade suprema do dragão em combate tático."
                ),
                Arrays.asList(
                        "Tesouros dracônicos antigos acumulam magia ambiental que altera as propriedades de gemas e metais preciosos.",
                        "Dragões de profundidade (Deep Dragons) no Subterrâneo mantêm alianças secretas com casas drow renegadas."
                ),
                Arrays.asList("xmm", "dragons", "bestiary", "legendary", "ecology"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "xsac_adjudication_rules", "XSAC", "multiverse", "present", "misc",
                "Compêndio de Adjudicação Canônica 2025", "Sage Advice Compendium 2025",
                "Compilação autoritativa das decisões e interpretações oficiais dos designers de regras do D&D.",
                Arrays.asList(
                        "163 decisões canônicas sobre ataques desarmados, descansos, sintonização, concentração e salvamentos.",
                        "Proíbe rolagens automáticas ocultas e preserva o consentimento e ação explícita do jogador."
                ),
                Arrays.asList(
                        "A adjudicação do Mestre nunca substitui regras primárias quando a regra do livro for expressa e taxativa.",
                        "Efeitos de medo, charme e paralisia concedem novos testes ao fim de cada turno do alvo afetado."
                ),
                Arrays.asList("xsac", "rulings", "sage_advice", "mechanics", "rules_adjudication"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "au_bejeweled_conclave", "AU", "multiverse", "present", "faction",
                "O Conclave das Gemas Encantadas", "The Bejeweled Conclave",
                "Aliança de artifices e joalheiros arcanos que transmutam minérios raros em focos mágicos vivos.",
                Arrays.asList(
                        "Operam guildas suntuosas em metrópoles multiversais, comercializando gemas de armazenamento de feitiços.",
                        "Defendem o monopólio da lapidação de safiras astrais e rubis de chamas primordiais."
                ),
                Arrays.asList(
                        "Financiam secretamente expedições a planos elementais para minerar os núcleos de montanhas flutuantes.",
                        "Possuem golens de ametista que patrulham suas câmaras de cofres contra ladrões da Nona Pena."
                ),
                Arrays.asList("au", "bejeweled_conclave", "artificers", "gems", "magic_items"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "au_bringers_cosmic_dawn", "AU", "multiverse", "present", "faction",
                "Portadores da Alvorada Cósmica", "Bringers of Cosmic Dawn",
                "Ordem de místicos e astrônomos arcanos dedicados ao estudo de alinhamentos estelares e auroras planares.",
                Arrays.asList(
                        "Constroem observatórios no topo de picos remotos para mapear a convergência das esferas astrais.",
                        "Vestem túnicas tecidas com fios de prata estelar que brilham suavemente na escuridão."
                ),
                Arrays.asList(
                        "Preveem um grande alinhamento cósmico que abrirá passagens permanentes entre Lorwyn e o Plano Material.",
                        "Guardam relíquias estelares que canalizam luz radiante capaz de cegar abominações abissais."
                ),
                Arrays.asList("au", "cosmic_dawn", "astronomy", "mystics", "radiant"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "au_tomb_shadow_serpents", "AU", "multiverse", "present", "location",
                "Túmulo das Serpentes das Sombras", "Tomb of the Shadow Serpents",
                "Complexo sepulcral antigo onde repousam sacerdotes ofídicos e relíquias de magia primordial das sombras.",
                Arrays.asList(
                        "Corredores de basalto negro ornados com relevos de serpentes de duas cabeças e bacias de veneno mágico.",
                        "Local de peregrinação para necromantes e ilusionistas que buscam comunhão com espíritos sombrios."
                ),
                Arrays.asList(
                        "No coração da tumba repousa o Cajado das Escamas Negras protegido por uma hidra espectral das sombras.",
                        "Glifos ocultos no teto descarregam dano psíquico CD 15 em intrusos desprovidos de amuletos ofídicos."
                ),
                Arrays.asList("au", "shadow_serpents", "tomb", "dungeon", "adventure_location"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        // --- EXPANSÃO LOTE B: ARCANA UNLEASHED (AU) ---
        list.add(new Entity(
                "au_living_spells_cataclysm", "AU", "multiverse", "present", "monster",
                "Magias Vivas e Feitiços Conscientes", "Living Spells of the Cataclysm",
                "Feitiços de alta potência que ganharam consciência malevolente e forma física após cataclismos arcanos.",
                Arrays.asList(
                        "Manifestações errantes de magias como Cloudkill, Evard's Black Tentacles e Fireball em forma de lodo mágico.",
                        "Absorvem dano mágico do mesmo tipo para se regenerar e atacam qualquer criatura viva com pulsos de feitiço."
                ),
                Arrays.asList(
                        "Podem ser dispersadas por rituais de dissipar magia de 5º nível ou seladas em orbes alquímicos pelo Conclave das Gemas.",
                        "A Nona Pena tenta capturar feitiços vivos de sono para utilizá-los como armas de cerco furtivo."
                ),
                Arrays.asList("au", "living_spells", "construct", "magic_hazard", "multiverse"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "au_evolving_items_relics", "AU", "multiverse", "present", "item",
                "Relíquias de Evolução Mágica", "Evolving Magic Items",
                "Artefatos cuja potência, bônus de ataque e propriedades mágicas despertam progressivamente através de feitos épicos.",
                Arrays.asList(
                        "Exemplificados pelo Staff of Skulls e a Breastplate of the Tyrant criados nos nós de poder de AU.",
                        "Iniciam no estágio Dormente (Dormant), despertam para Desperto (Awakened) e atingem o ápice Exaltado (Exalted)."
                ),
                Arrays.asList(
                        "A transição entre estágios exige provações de caráter ou a derrota de inimigos jurados em batalha singular.",
                        "Tentativas de forçar a evolução por meios sombrios corrompem permanentemente a ressonância do artefato."
                ),
                Arrays.asList("au", "evolving_items", "magic_items", "vestiges", "attunement"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "au_item_dictation_quills", "AU", "multiverse", "present", "item",
                "Penas de Escrita Arcana (Dictation Quills)", "Dictation Quills",
                "Instrumentos arcanos de transcrição mágica automática utilizados pela Nona Pena e escribas multiversais.",
                Arrays.asList(
                        "Penas encantadas que transcrevem fala, pensamentos e runas sem necessidade de tinta ou mão mortal.",
                        "Capazes de traduzir dialetos antigos instantaneamente durante a gravação em pergaminho."
                ),
                Arrays.asList(
                        "Modelos superiores da Nona Pena conseguem duplicar fórmulas arcanas de grimórios selados sem quebrar lacres de cera.",
                        "Podem ser sintonizadas para apagar magicamente as anotações após leitura se uma senha for violada."
                ),
                Arrays.asList("au", "dictation_quills", "magic_item", "scribing", "ninth_quill"),
                Collections.<String, String>emptyMap(), "grupo", true
        ));

        list.add(new Entity(
                "au_relic_chronos_lens", "AU", "multiverse", "present", "item",
                "Lente de Cronos", "Chronos Lens",
                "Relíquia óptica astral que permite vislumbrar ecos de eventos passados e futuros potenciais em um local.",
                Arrays.asList(
                        "Lente de cristal estelar engastada em aro de prata forjada pelo Conclave das Gemas Encantadas.",
                        "Revela pistas invisíveis, armadilhas temporais e resíduos mágicos deixados há dias ou séculos."
                ),
                Arrays.asList(
                        "A observação prolongada de futuros prováveis exige salvaguarda de Inteligência CD 14 contra confusão temporal.",
                        "Cobiçada por inquisitivos de Eberron e batedores arcanos para desvendar crimes e conspirações perfeitas."
                ),
                Arrays.asList("au", "chronos_lens", "relic", "divination", "time_magic", "magic_item"),
                Collections.<String, String>emptyMap(), "secreto", false
        ));

        return Collections.unmodifiableList(list);
    }
}
