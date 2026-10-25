package braseiro.ose.app

import braseiro.ose.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Motor de Orientação ao Jogador / Aprender Jogando (OSE Player Guide Engine).
 *
 * Contrato de Segurança:
 * - Operações estritamente SOMENTE LEITURA.
 * - NUNCA consome RNG, nunca altera posições, relógio de jogo, recursos, fichas ou reputação.
 * - NUNCA expõe segredos do Mestre (gm_only).
 * - Totalmente funcional OFFLINE e sem dependência de APIs externas.
 */
object OSEPlayerGuide {

    enum class GuideMode(val wireName: String) {
        LEARN_PLAYING("LEARN_PLAYING"),
        EXPERIENCED("EXPERIENCED");

        companion object {
            fun fromString(s: String?): GuideMode = when (s?.uppercase()) {
                "EXPERIENCED" -> EXPERIENCED
                else -> LEARN_PLAYING
            }
        }
    }

    data class HelpTopic(
        val topicId: String,
        val title: String,
        val category: String,
        val summary: String,
        val details: String,
        val examples: List<String>,
        val sourceRuleRef: String
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("topicId", topicId)
            .put("title", title)
            .put("category", category)
            .put("summary", summary)
            .put("details", details)
            .put("examples", JSONArray(examples))
            .put("sourceRuleRef", sourceRuleRef)
    }

    data class ActionSuggestion(
        val id: String,
        val plainLabel: String,
        val intentCandidate: String,
        val textCandidate: String,
        val preconditions: List<String>,
        val riskHint: String,
        val timePreview: String,
        val sourceRuleRef: String
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("id", id)
            .put("plainLabel", plainLabel)
            .put("intentCandidate", intentCandidate)
            .put("textCandidate", textCandidate)
            .put("preconditions", JSONArray(preconditions))
            .put("riskHint", riskHint)
            .put("timePreview", timePreview)
            .put("sourceRuleRef", sourceRuleRef)
    }

    private val TOPICS = listOf(
        HelpTopic(
            topicId = "attributes",
            title = "Os 6 Atributos Básicos",
            category = "Criação de Personagem",
            summary = "Força, Inteligência, Sabedoria, Destreza, Constituição e Carisma medem as capacidades do aventureiro.",
            details = "Rolados com 3d6 em ordem (3 a 18). Valores de 13+ concedem bônus (+1 a +3), enquanto 8 ou menos acarretam penalidades (-1 a -3). Valores 9 a 12 são a média humana sem modificador.",
            examples = listOf(
                "FOR 13+: +1 no ataque e dano corpo a corpo e para abrir portas emperradas.",
                "DES 13+: +1 em ataques à distância e melhora na Classe de Armadura.",
                "CON 13+: +1 em pontos de vida por dado de vida (DV)."
            ),
            sourceRuleRef = "Classic Rules Tome p. 8-9 / Advanced Player's Tome p. 12"
        ),
        HelpTopic(
            topicId = "armor_class",
            title = "Classe de Armadura (CA)",
            category = "Combate",
            summary = "Medida da dificuldade de atingir o aventureiro em combate.",
            details = "No sistema clássico OSE descendente, uma pessoa sem armadura tem CA 9. Quanto MENOR o número, melhor a proteção (cota de malha CA 5, placa completa CA 3, escudo reduz em 1 ponto). O bônus de Destreza também reduz o valor da CA.",
            examples = listOf(
                "Sem armadura: CA 9",
                "Armadura de couro: CA 7",
                "Cota de malha + escudo: CA 4"
            ),
            sourceRuleRef = "Classic Rules Tome p. 104 / Advanced Player's Tome p. 118"
        ),
        HelpTopic(
            topicId = "hit_points",
            title = "Pontos de Vida (PV) e Morte",
            category = "Sobrevivência",
            summary = "A resistência a ferimentos e fadiga antes da incapacitação ou morte.",
            details = "Ao atingir 0 PV, o personagem está morto ou moribundo (conforme regra do grupo). No 1º nível, o dado de vida depende da classe (Guerreiro d8, Clérigo d6, Mago d4) somado ao bônus de Constituição (mínimo 1 PV).",
            examples = listOf(
                "Descanso de 24 horas em local seguro recupera 1d3 PV.",
                "Magias de cura (como Curar Ferimentos Leves) restauram 1d6+1 PV."
            ),
            sourceRuleRef = "Classic Rules Tome p. 106"
        ),
        HelpTopic(
            topicId = "dungeon_turns",
            title = "Turnos de Exploração em Masmorra",
            category = "Exploração",
            summary = "Em masmorras, o tempo avança em 'Turnos' de 10 minutos.",
            details = "A cada turno, o grupo pode percorrer uma distância com cautela, examinar uma sala, procurar armadilhas ou arrombar uma porta. A cada 2 turnos (ou conforme o local), o Mestre pode rolar 1d6 para encontros errantes (1 em 6 indica encontro).",
            examples = listOf(
                "Procurar armadilhas numa porta consome 1 turno (10 min).",
                "Uma tocha queima por 6 turnos (1 hora completa)."
            ),
            sourceRuleRef = "Classic Rules Tome p. 96"
        ),
        HelpTopic(
            topicId = "torches_light",
            title = "Luz e Tochas",
            category = "Equipamento",
            summary = "Monstros e perigos atacam no escuro sem chance de defesa se não houver iluminação.",
            details = "Humanos não enxergam no escuro. Uma tocha ou lanterna ilumina um raio de 9 metros (30 pés). Uma tocha dura exatamente 6 turnos (1 hora). Ficar no escuro total causa penalidades severas e impede o mapeamento.",
            examples = listOf(
                "Acender tocha gasta 1 unidade do inventário.",
                "Lanternas a óleo consomem 1 frasco de óleo a cada 24 turnos (4 horas)."
            ),
            sourceRuleRef = "Classic Rules Tome p. 44"
        ),
        HelpTopic(
            topicId = "wilderness_travel",
            title = "Viagem pelos Ermos",
            category = "Viagem",
            summary = "Nos ermos, a escala de tempo é medida em Dias de viagem.",
            details = "Um grupo a pé geralmente avança de 1 a 2 hexágonos por dia, dependendo do terreno (estradas e planícies são mais rápidas; pântanos e montanhas são mais lentos). Cada dia consome 1 ração e água potável por aventureiro.",
            examples = listOf(
                "Sem rações, a cada dia sem comida a companhia acumula penalidades de fadiga e perda gradual de moral.",
                "Em terreno desconhecido, há chance diária do grupo se perder (extravio)."
            ),
            sourceRuleRef = "Classic Rules Tome p. 110"
        ),
        HelpTopic(
            topicId = "retainers",
            title = "Auxiliares e Mercenários",
            category = "Companhia",
            summary = "Aventureiros podem contratar ajudantes para carregar tochas, vigiar acampamentos ou lutar.",
            details = "O número máximo de auxiliares é determinado pelo Carisma do contratante. Exigem pagamento de diária fixa (geralmente 1 po/dia) e uma cota de tesouro (geralmente 1/2 cota). Possuem índice de Lealdade que reage a bom tratamento e perigo extremo.",
            examples = listOf(
                "Se o empregador não pagar as diárias devidas, o auxiliar pode abandonar o grupo na primeira oportunidade segura.",
                "Auxiliares ganham metade do XP que os personagens dos jogadores recebem."
            ),
            sourceRuleRef = "Classic Rules Tome p. 19-20"
        ),
        HelpTopic(
            topicId = "combat_rounds",
            title = "Rodadas de Combate",
            category = "Combate",
            summary = "O combate é dividido em rodadas de 10 segundos, com iniciativa por lado.",
            details = "Sequência oficial OSE: 1. Declaração de magias ou recuo; 2. Iniciativa (1d6 por lado); 3. Lado vencedor age (moral, movimento, projéteis, magias, combate corpo a corpo); 4. Lado perdedor age na mesma ordem.",
            examples = listOf(
                "Se um conjurador for ferido antes do seu momento de lançar a magia na rodada, a magia é perdida."
            ),
            sourceRuleRef = "Classic Rules Tome p. 102"
        )
    )

    fun getAllTopics(): List<HelpTopic> = TOPICS

    fun getTopic(topicId: String): HelpTopic? = TOPICS.firstOrNull { it.topicId.equals(topicId, ignoreCase = true) }

    fun getContextualHelp(phase: String, locationKind: String): List<HelpTopic> {
        val matched = mutableListOf<HelpTopic>()
        when (phase.uppercase()) {
            "CHARACTER_LOADOUT", "READY_TO_START" -> {
                getTopic("attributes")?.let { matched.add(it) }
                getTopic("torches_light")?.let { matched.add(it) }
                getTopic("hit_points")?.let { matched.add(it) }
            }
            "COMBAT" -> {
                getTopic("combat_rounds")?.let { matched.add(it) }
                getTopic("armor_class")?.let { matched.add(it) }
                getTopic("hit_points")?.let { matched.add(it) }
            }
            else -> {
                if (locationKind.contains("DUNGEON", ignoreCase = true)) {
                    getTopic("dungeon_turns")?.let { matched.add(it) }
                    getTopic("torches_light")?.let { matched.add(it) }
                } else if (locationKind.contains("HEX", ignoreCase = true) || locationKind.contains("WILD", ignoreCase = true)) {
                    getTopic("wilderness_travel")?.let { matched.add(it) }
                }
                getTopic("retainers")?.let { matched.add(it) }
                getTopic("hit_points")?.let { matched.add(it) }
            }
        }
        return matched.ifEmpty { TOPICS.take(3) }
    }

    fun getActionSuggestions(envelope: CampaignEnvelope): List<ActionSuggestion> {
        val state = envelope.campaignState
        val game = state.game
        val phase = game.session.phase.uppercase()
        val pos = state.position.primary
        val suggestions = mutableListOf<ActionSuggestion>()

        when {
            phase == "COMBAT" -> {
                suggestions.add(
                    ActionSuggestion(
                        id = "act-combat-melee",
                        plainLabel = "Atacar o adversário mais próximo",
                        intentCandidate = "COMBAT_ATTACK_MELEE",
                        textCandidate = "Avanço com arma em punho para desferir um ataque contra o inimigo mais próximo.",
                        preconditions = listOf("Arma corpo a corpo equipada"),
                        riskHint = "Inimigo pode contra-atacar na mesma rodada",
                        timePreview = "1 rodada (10 segundos)",
                        sourceRuleRef = "Classic Rules Tome p. 102"
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-combat-defend",
                        plainLabel = "Postura defensiva e recuo",
                        intentCandidate = "COMBAT_FIGHTING_WITHDRAWAL",
                        textCandidate = "Mantenho a guarda alta com o escudo e recuo com cautela, evitando expor as costas.",
                        preconditions = listOf("Espaço livre para recuo"),
                        riskHint = "Movimento limitado à metade da velocidade",
                        timePreview = "1 rodada (10 segundos)",
                        sourceRuleRef = "Classic Rules Tome p. 103"
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-combat-parley",
                        plainLabel = "Tentar parlamentar ou exigir rendição",
                        intentCandidate = "COMBAT_PARLEY",
                        textCandidate = "Levanto a voz com firmeza propondo um cessar-fogo ou termos de rendição antes de mais sangue.",
                        preconditions = listOf("Inimigo inteligente com linguagem comum"),
                        riskHint = "Inimigo pode ignorar e atacar se a moral estiver firme",
                        timePreview = "Instantâneo (fase de declaração)",
                        sourceRuleRef = "Classic Rules Tome p. 102"
                    )
                )
            }
            pos is SpatialRef.Dungeon -> {
                suggestions.add(
                    ActionSuggestion(
                        id = "act-dungeon-search",
                        plainLabel = "Procurar armadilhas e passagens secretas",
                        intentCandidate = "DUNGEON_SEARCH_ROOM",
                        textCandidate = "Examinamos minuciosamente as paredes de pedra, o piso e as junções em busca de mecanismos ocultos ou armadilhas.",
                        preconditions = listOf("Área iluminada por tocha ou lanterna"),
                        riskHint = "Consome 10 minutos (1 turno); chance de encontro errante",
                        timePreview = "1 turno (10 minutos)",
                        sourceRuleRef = "Classic Rules Tome p. 96"
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-dungeon-listen",
                        plainLabel = "Aproximar-se da porta e escutar ruídos",
                        intentCandidate = "DUNGEON_LISTEN_DOOR",
                        textCandidate = "Aproximo o ouvido da fresta da porta de madeira, fazendo sinal para o grupo manter silêncio absoluto.",
                        preconditions = listOf("Porta ou passagem diante do grupo"),
                        riskHint = "Baixo risco; sucesso depende do teste de Ouvir Ruídos",
                        timePreview = "1 turno (10 minutos)",
                        sourceRuleRef = "Classic Rules Tome p. 97"
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-dungeon-torch",
                        plainLabel = "Verificar estado das tochas e suprimentos",
                        intentCandidate = "CHECK_SUPPLIES",
                        textCandidate = "Verifico quanto tempo resta na tocha acesa e confiro as provisões da mochila.",
                        preconditions = emptyList(),
                        riskHint = "Nenhum risco",
                        timePreview = "Sem custo de tempo",
                        sourceRuleRef = "Classic Rules Tome p. 44"
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-dungeon-advance",
                        plainLabel = "Avançar cautelosamente para a próxima câmara",
                        intentCandidate = "DUNGEON_ADVANCE",
                        textCandidate = "Avançamos em formação cautelosa pela passagem, testando o piso com a ponta da haste de madeira.",
                        preconditions = listOf("Passagem aberta"),
                        riskHint = "Pode revelar nova sala com monstros ou armadilhas",
                        timePreview = "1 turno (10 minutos)",
                        sourceRuleRef = "Classic Rules Tome p. 96"
                    )
                )
            }
            else -> {
                // Wilderness / Hex exploration
                suggestions.add(
                    ActionSuggestion(
                        id = "act-wild-observe",
                        plainLabel = "Examinar o terreno e sinais da região",
                        intentCandidate = "WILD_OBSERVE_TERRAIN",
                        textCandidate = "Subimos a um ponto elevado para examinar o horizonte, identificar trilhas, vegetação e possíveis abrigos.",
                        preconditions = listOf("Luz do dia"),
                        riskHint = "Revela detalhes do hexágono atual sem forçar deslocamento",
                        timePreview = "1 turno (10 minutos)",
                        sourceRuleRef = "Classic Rules Tome p. 110"
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-wild-travel-cautious",
                        plainLabel = "Viajar para o hexágono vizinho com cautela",
                        intentCandidate = "WILD_TRAVEL_CAUTIOUS",
                        textCandidate = "Organizamos a marcha em fila e seguimos viagem em direção à próxima região com batedores atentos.",
                        preconditions = listOf("Provisões e água potável"),
                        riskHint = "Consome 1 dia de viagem e 1 ração diária por aventureiro",
                        timePreview = "1 dia de marcha",
                        sourceRuleRef = "Classic Rules Tome p. 110"
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-wild-camp",
                        plainLabel = "Montar acampamento e repousar",
                        intentCandidate = "WILD_REST",
                        textCandidate = "Escolhemos um local protegido do vento, montamos o acampamento e organizamos turnos de vigília para a noite.",
                        preconditions = listOf("Local adequado de abrigo"),
                        riskHint = "Consome rações; exige vigília contra encontros noturnos",
                        timePreview = "1 dia / repouso completo",
                        sourceRuleRef = "Classic Rules Tome p. 111"
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-wild-consult-party",
                        plainLabel = "Pedir a opinião dos companheiros",
                        intentCandidate = "CONSULT_COMPANIONS",
                        textCandidate = "Reúno a companhia para avaliar o mapa, a condição dos ferimentos e ouvir as sugestões de cada um.",
                        preconditions = listOf("Companheiros presentes"),
                        riskHint = "Nenhum risco mecânico; não consome tempo",
                        timePreview = "Instantâneo",
                        sourceRuleRef = "Regra de Curadoria de Companhia"
                    )
                )
            }
        }

        return suggestions
    }
}
