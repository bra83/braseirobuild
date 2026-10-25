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
        val intentCandidate: String? = null,
        val textCandidate: String,
        val preconditions: List<String>,
        val riskHint: String,
        val timePreview: String,
        val sourceRuleRef: String,
        val executable: Boolean = true,
        val missingRequirement: String? = null
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("id", id)
            .put("plainLabel", plainLabel)
            .put("intentCandidate", intentCandidate ?: JSONObject.NULL)
            .put("textCandidate", textCandidate)
            .put("preconditions", JSONArray(preconditions))
            .put("riskHint", riskHint)
            .put("timePreview", timePreview)
            .put("sourceRuleRef", sourceRuleRef)
            .put("executable", executable)
            .put("missingRequirement", missingRequirement ?: JSONObject.NULL)
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
            summary = "Capacidade de absorver ferimentos e fadiga antes da morte.",
            details = "Determinados no nível 1 pelo Dado de Vida (DV) da classe (ex.: d8 para Guerreiro, d4 para Mago) mais modificador de CON. Em 0 PV ou menos, o personagem morre imediatamente pelas regras clássicas. Descanso em refúgio seguro cura 1d3 PV por dia de cama.",
            examples = listOf(
                "Guerreiro com CON 14 (+1) rola 1d8+1 (mínimo 2 PV, máximo 9 PV no 1º nível).",
                "Mago com CON 7 (-1) rola 1d4-1 (mínimo 1 PV)."
            ),
            sourceRuleRef = "Classic Rules Tome p. 9 / Advanced Player's Tome p. 13"
        ),
        HelpTopic(
            topicId = "saving_throws",
            title = "Jogadas de Proteção (Salvaguardas)",
            category = "Sobrevivência",
            summary = "Chances de resistir a perigos letais: Veneno, Varinhas, Paralisia, Sopro e Magias.",
            details = "Rola-se 1d20. Se o resultado for igual ou superior ao valor exigido na tabela da classe e nível, o perigo é evitado ou reduzido à metade.",
            examples = listOf(
                "Guerreiro de 1º nível testando contra Veneno: precisa de 12 ou mais em 1d20.",
                "Anões e Halflings recebem bônus substanciais em Salvaguardas devido à constituição robusta."
            ),
            sourceRuleRef = "Classic Rules Tome p. 105 / Advanced Player's Tome p. 120"
        ),
        HelpTopic(
            topicId = "dungeon_turns",
            title = "O Turno de Masmorra (10 minutos)",
            category = "Exploração",
            summary = "A unidade básica de tempo na exploração subterrânea é o turno de 10 minutos.",
            details = "A cada turno, o grupo pode se mover com cautela (mapeando), examinar uma sala, forçar uma porta emperrada ou procurar armadilhas. O Mestre rola encontro errante a cada 2 turnos (1 em 6 em 1d6). A cada 5 turnos de atividade, o grupo deve descansar 1 turno.",
            examples = listOf(
                "Procurar armadilhas em uma sala: consome 1 turno completo.",
                "Uma tocha dura 6 turnos (1 hora) antes de se extinguir."
            ),
            sourceRuleRef = "Classic Rules Tome p. 96 / Advanced Referee's Tome p. 88"
        ),
        HelpTopic(
            topicId = "combat_rounds",
            title = "A Rodada de Combate (10 segundos)",
            category = "Combate",
            summary = "O combate é dividido em rodadas de 10 segundos, resolvidas por iniciativa de grupo.",
            details = "Cada rodada tem sequência estrita: 1) Declaração de magias e recuo; 2) Iniciativa de grupo (1d6 contra 1d6); 3) Movimento; 4) Disparos de projéteis; 5) Magias; 6) Ataques corpo a corpo; 7) Testes de moral dos monstros.",
            examples = listOf(
                "Se o grupo vencer a iniciativa (número mais alto no 1d6), age antes dos monstros.",
                "Conjurador que sofrer dano antes da fase de magias perde a magia declarada."
            ),
            sourceRuleRef = "Classic Rules Tome p. 102 / Advanced Player's Tome p. 116"
        ),
        HelpTopic(
            topicId = "wilderness_travel",
            title = "Viagem pelos Ermos e Clima",
            category = "Ermos",
            summary = "Deslocamento por hexágonos no mapa-múndi medido em milhas diárias.",
            details = "O grupo viaja até seu limite de movimento diário (ex.: 24 milhas para humanos com carga leve). O terreno (floresta, pântano, montanha) e o clima (chuva, nevasca) reduzem a velocidade e aumentam o risco de se perder.",
            examples = listOf(
                "Pântano e montanhas dobram o custo de movimento (metade da velocidade normal).",
                "Sem comida e água limpa, a companhia sofre penalidades graves e exaustão."
            ),
            sourceRuleRef = "Classic Rules Tome p. 98 / Advanced Referee's Tome p. 92"
        ),
        HelpTopic(
            topicId = "torches_light",
            title = "Iluminação, Tochas e Recursos",
            category = "Recursos",
            summary = "Sem luz em masmorras fechadas, humanos ficam cegos e vulneráveis a emboscadas.",
            details = "Uma tocha ilumina um raio de 9 metros (30 pés) por 6 turnos (1 hora). Lanternas a óleo iluminam o mesmo raio com foco ajustável e óleo dura 24 turnos (4 horas). Se a luz apagar em território hostil, ataques recebem penalidades severas.",
            examples = listOf(
                "Carregar tocha em uma das mãos impede o uso de armas de duas mãos ou escudo.",
                "Soltar a tocha no chão durante o combate permite sacar o escudo, mas arrisca apagar a chama."
            ),
            sourceRuleRef = "Classic Rules Tome p. 44, 96 / Advanced Player's Tome p. 48"
        ),
        HelpTopic(
            topicId = "retainers",
            title = "Auxiliares, Mercenários e Moral",
            category = "Companhia",
            summary = "Contratar ajudantes na cidade aumenta as chances de sobrevivência da companhia.",
            details = "O Carisma do líder determina o número máximo de auxiliares (retainers) e sua Moral básica (2d6). Auxiliares exigem diária em moedas de ouro e meia cota de tesouro encontrado. Em perigo extremo ou morte de aliados, o Mestre testa Moral 2d6.",
            examples = listOf(
                "Carisma 13: até 5 auxiliares com Moral 8.",
                "Se o teste de 2d6 superar o valor de Moral, o auxiliar recua ou foge."
            ),
            sourceRuleRef = "Classic Rules Tome p. 28 / Advanced Player's Tome p. 34"
        ),
        HelpTopic(
            topicId = "rule_trace",
            title = "Explicação da Última Ação do Mestre",
            category = "Arbitragem",
            summary = "Como o árbitro determinístico do OSE calculou e comprovou o último evento mecânico.",
            details = "Todas as resoluções de ataque, dano, salvaguarda, movimento ou consumo de tempo são registradas em traces auditáveis com referências aos livros oficiais (Classic Tome / Advanced Tome). O guia de regras permite consultar a fonte exata sem consumir RNG.",
            examples = listOf(
                "Ataque corpo a corpo: comparado com a matriz THAC0 oficial da classe.",
                "Consumo de tempo: relógio avançado em turnos (10 min) ou dias inteiros."
            ),
            sourceRuleRef = "Classic Rules Tome p. 102-105"
        )
    )

    fun getAllTopics(): List<HelpTopic> = TOPICS

    fun getTopic(topicId: String): HelpTopic? = TOPICS.firstOrNull { it.topicId.equals(topicId, ignoreCase = true) }

    fun getContextualHelp(phase: String, locationKind: String): List<HelpTopic> {
        val matched = mutableListOf<HelpTopic>()
        when (phase.uppercase()) {
            "CHARACTER_LOADOUT", "READY_TO_START" -> {
                getTopic("attributes")?.let { matched.add(it) }
                getTopic("armor_class")?.let { matched.add(it) }
                getTopic("torches_light")?.let { matched.add(it) }
                getTopic("hit_points")?.let { matched.add(it) }
            }
            "COMBAT" -> {
                getTopic("combat_rounds")?.let { matched.add(it) }
                getTopic("armor_class")?.let { matched.add(it) }
                getTopic("saving_throws")?.let { matched.add(it) }
                getTopic("hit_points")?.let { matched.add(it) }
            }
            else -> {
                if (locationKind.contains("DUNGEON", ignoreCase = true)) {
                    getTopic("dungeon_turns")?.let { matched.add(it) }
                    getTopic("torches_light")?.let { matched.add(it) }
                    getTopic("saving_throws")?.let { matched.add(it) }
                } else if (locationKind.contains("HEX", ignoreCase = true) || locationKind.contains("WILD", ignoreCase = true)) {
                    getTopic("wilderness_travel")?.let { matched.add(it) }
                }
                getTopic("retainers")?.let { matched.add(it) }
                getTopic("hit_points")?.let { matched.add(it) }
            }
        }
        return matched.ifEmpty { TOPICS.take(4) }
    }

    /** Only gp-denominated coin stacks carried by active party members count toward an inn fee. */
    fun availableGoldGp(envelope: CampaignEnvelope): Int {
        val state = envelope.campaignState
        return state.game.inventory.items.values.filter { item ->
            val code = item.catalogId.uppercase()
            item.ownerId in state.party.characterIds && item.kind == InventoryItemKind.COIN &&
                (code == "GP" || code == "GOLD" || code == "GOLD_GP" ||
                 code == "GOLD_PIECE" || code == "GOLD_PIECES" ||
                 code.endsWith("_GOLD_GP") || code.endsWith("_GP") || code.endsWith("_GOLD")) &&
                item.quantity > 0
        }.sumOf { it.quantity }
    }

    fun getActionSuggestions(envelope: CampaignEnvelope): List<ActionSuggestion> {
        val state = envelope.campaignState
        val game = state.game
        val phase = game.session.phase.uppercase()
        val pos = state.position.primary
        val suggestions = mutableListOf<ActionSuggestion>()

        val primaryActor = state.party.characterIds.firstOrNull() ?: ""
        val livingPartyCount = state.party.characterIds.count { id ->
            game.characters[id]?.hitPoints?.dead == false
        }
        val canAffordInn = livingPartyCount > 0 && availableGoldGp(envelope) >= livingPartyCount
        val hasShield = game.inventory.items.values.any { it.equipped && it.kind == InventoryItemKind.SHIELD }
        val hasRanged = game.inventory.items.values.any { it.equipped && (it.catalogId.contains("BOW") || it.catalogId.contains("SLING") || it.catalogId.contains("CROSSBOW")) }
        val hasMelee = game.inventory.items.values.any { it.equipped && it.kind == InventoryItemKind.WEAPON && !it.catalogId.contains("BOW") }
        val unlitTorch = game.inventory.items.values.firstOrNull { it.kind == InventoryItemKind.LIGHT_SOURCE && it.quantity > 0 }
        val encounter = game.encounter
        val hasOpposition = encounter?.combatants?.values?.any { it.side == EncounterSide.OPPOSITION && (it.alive && it.hpCurrentNumerator > 0) } == true
        val targetOpponent = encounter?.combatants?.values?.firstOrNull { it.side == EncounterSide.OPPOSITION && (it.alive && it.hpCurrentNumerator > 0) }?.actorId
        // Only doors already known to the party may be used for contextual guidance.
        // Never reveal or enumerate secret dungeon edges in the player's guide.
        val activeDungeon = game.dungeon
        val knownDoorEdges = if (pos is SpatialRef.Dungeon && activeDungeon != null) {
            activeDungeon.edges.filter { edge ->
                !edge.secret && edge.kind.equals("DOOR", ignoreCase = true) &&
                    ((edge.from == pos.nodeId && (edge.to in activeDungeon.knownNodeIds || edge.to in activeDungeon.visibleNodeIds)) ||
                     (edge.to == pos.nodeId && (edge.from in activeDungeon.knownNodeIds || edge.from in activeDungeon.visibleNodeIds)))
            }
        } else emptyList()
        val hasKnownDoor = knownDoorEdges.isNotEmpty()
        val hasClosedKnownDoor = knownDoorEdges.any { !it.open }

        when {
            phase == "COMBAT" || encounter?.phase == EncounterPhase.COMBAT -> {
                if (hasMelee) {
                    val weapon = game.inventory.items.values.firstOrNull { it.equipped && it.kind == InventoryItemKind.WEAPON && !it.catalogId.contains("BOW") }
                    val weaponId = weapon?.itemId
                    val isMeleeExec = hasOpposition && targetOpponent != null && weaponId != null && primaryActor.isNotBlank()
                    suggestions.add(
                        ActionSuggestion(
                            id = "act-combat-melee",
                            plainLabel = "Atacar com arma corpo a corpo",
                            intentCandidate = if (isMeleeExec) "COMBAT_ATTACK_MELEE:PARTY:$primaryActor:$targetOpponent:$weaponId" else null,
                            textCandidate = "Avanço com arma em punho para desferir um ataque contra o adversário mais próximo.",
                            preconditions = listOf("Arma corpo a corpo equipada"),
                            riskHint = "Inimigo pode contra-atacar na mesma rodada",
                            timePreview = "1 rodada (10 segundos)",
                            sourceRuleRef = "Classic Rules Tome p. 102",
                            executable = isMeleeExec,
                            missingRequirement = when {
                                primaryActor.isBlank() -> "Nenhum combatente ativo na companhia"
                                !hasOpposition || targetOpponent == null -> "Nenhum oponente ativo no combate"
                                weaponId == null -> "Nenhuma arma corpo a corpo equipada"
                                else -> null
                            }
                        )
                    )
                }
                if (hasRanged) {
                    val ranged = game.inventory.items.values.firstOrNull { it.equipped && (it.catalogId.contains("BOW") || it.catalogId.contains("SLING") || it.catalogId.contains("CROSSBOW")) }
                    val rangedId = ranged?.itemId
                    val isRangedExec = hasOpposition && targetOpponent != null && rangedId != null && primaryActor.isNotBlank()
                    suggestions.add(
                        ActionSuggestion(
                            id = "act-combat-ranged",
                            plainLabel = "Disparar projétil à distância",
                            intentCandidate = if (isRangedExec) "COMBAT_ATTACK_MISSILE:PARTY:$primaryActor:$targetOpponent:$rangedId" else null,
                            textCandidate = "Mantenho distância segura e disparo minha arma de projéteis contra o inimigo mais exposto.",
                            preconditions = listOf("Arma de disparo e munição disponíveis"),
                            riskHint = "Não pode disparar se já engajado em combate corpo a corpo",
                            timePreview = "1 rodada (10 segundos)",
                            sourceRuleRef = "Classic Rules Tome p. 102",
                            executable = isRangedExec,
                            missingRequirement = when {
                                primaryActor.isBlank() -> "Nenhum combatente ativo na companhia"
                                !hasOpposition || targetOpponent == null -> "Nenhum oponente ativo no combate"
                                rangedId == null -> "Nenhuma arma de disparo equipada"
                                else -> null
                            }
                        )
                    )
                }
                val isRetreatExec = primaryActor.isNotBlank() && encounter != null
                suggestions.add(
                    ActionSuggestion(
                        id = "act-combat-defend",
                        plainLabel = if (hasShield) "Recuo cauteloso com escudo" else "Postura defensiva e recuo",
                        intentCandidate = if (isRetreatExec) "COMBAT_MOVE:PARTY:$primaryActor:RETREAT" else null,
                        textCandidate = if (hasShield) "Mantenho a guarda alta com o escudo e recuo com cautela, evitando expor as costas." else "Mantenho a guarda alta com minha arma e recuo com cautela, evitando expor as costas.",
                        preconditions = if (hasShield) listOf("Escudo equipado", "Espaço livre para recuo") else listOf("Arma em punho", "Espaço livre para recuo"),
                        riskHint = "Movimento limitado à metade da velocidade normal",
                        timePreview = "1 rodada (10 segundos)",
                        sourceRuleRef = "Classic Rules Tome p. 103",
                        executable = isRetreatExec,
                        missingRequirement = if (!isRetreatExec) "Recuo exige combate e combatente ativo" else null
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
                        sourceRuleRef = "Classic Rules Tome p. 97",
                        executable = hasKnownDoor,
                        missingRequirement = if (!hasKnownDoor) "Não há porta conhecida e acessível diante da companhia" else null
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-dungeon-stuck-door",
                        plainLabel = "Forçar porta emperrada",
                        intentCandidate = "DUNGEON_OPEN_STUCK_DOOR",
                        textCandidate = "Apoio o ombro contra a porta de carvalho e forço o mecanismo com toda a força.",
                        preconditions = listOf("Porta emperrada diante do grupo"),
                        riskHint = "Ruído alto alerta criaturas próximas (1 turno)",
                        timePreview = "1 turno (10 minutos)",
                        sourceRuleRef = "Classic Rules Tome p. 97",
                        executable = hasClosedKnownDoor,
                        missingRequirement = if (!hasClosedKnownDoor) "Não há porta fechada conhecida diante da companhia" else null
                    )
                )
                if (unlitTorch != null) {
                    suggestions.add(
                        ActionSuggestion(
                            id = "act-dungeon-torch",
                            plainLabel = "Acender tocha do inventário",
                            intentCandidate = "LIGHT_IGNITE_ITEM:${unlitTorch.itemId}:$primaryActor",
                            textCandidate = "Retiro uma tocha e pederneira da mochila e acendo a chama para manter o raio de 9 metros iluminado.",
                            preconditions = listOf("Tocha e pederneira no inventário"),
                            riskHint = "Ocupa uma das mãos durante a exploração",
                            timePreview = "Instantâneo (dentro do turno)",
                            sourceRuleRef = "Classic Rules Tome p. 96"
                        )
                    )
                }
            }
            pos is SpatialRef.Settlement -> {
                suggestions.add(
                    ActionSuggestion(
                        id = "act-settle-inn",
                        plainLabel = "Descansar na estalagem para recuperar PV",
                        intentCandidate = "SETTLEMENT_REST_DAY",
                        textCandidate = "Procuramos a estalagem local para alugar quartos confortáveis, comer refeições quentes e descansar o dia inteiro.",
                        preconditions = listOf("1 po por aventureiro vivo da companhia"),
                        riskHint = "Consome 1 dia de tempo de jogo; recupera 1d3 PV",
                        timePreview = "1 dia (24 horas)",
                        sourceRuleRef = "Classic Rules Tome p. 9",
                        executable = canAffordInn,
                        missingRequirement = when {
                            livingPartyCount == 0 -> "Nenhum aventureiro vivo na companhia"
                            !canAffordInn -> "A companhia precisa de $livingPartyCount po para a diária"
                            else -> null
                        }
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-settle-wait",
                        plainLabel = "Aguardar 1 turno no assentamento",
                        intentCandidate = "WAIT_TURN",
                        textCandidate = "Aguardamos alguns instantes observando o movimento ao redor da praça e dos comerciantes.",
                        preconditions = listOf("Sem combate ativo"),
                        riskHint = "Avança 1 turno (10 minutos)",
                        timePreview = "1 turno (10 minutos)",
                        sourceRuleRef = "Classic Rules Tome p. 110"
                    )
                )
            }
            else -> {
                // Wilderness or Hex
                data class D(val key: String, val label: String, val dq: Int, val dr: Int)
                val dirs = listOf(D("E","LESTE",1,0),D("SE","SUDESTE",0,1),D("NE","NORDESTE",1,-1),D("W","OESTE",-1,0),D("SW","SUDOESTE",-1,1),D("NW","NOROESTE",0,-1))
                val hexPos = pos as? SpatialRef.Hex
                val cells = game.hexWorld?.cells.orEmpty()
                val validDirs = if (hexPos != null) {
                    dirs.filter { d -> cells.any { it.q == hexPos.q + d.dq && it.r == hexPos.r + d.dr && it.hydrology.name != "SEA_OCEAN" } }
                } else dirs

                val primaryDir = validDirs.firstOrNull() ?: dirs[0]
                val secondaryDir = validDirs.getOrNull(1) ?: dirs[1]

                suggestions.add(
                    ActionSuggestion(
                        id = "act-wild-travel-day",
                        plainLabel = "Viajar 1 dia · ${primaryDir.label}",
                        intentCandidate = "WILD_TRAVEL_DAY:${primaryDir.key}",
                        textCandidate = "Organizamos a marcha em fila com vigias na vanguarda e retaguarda, avançando por um dia em direção a ${primaryDir.label.lowercase()}.",
                        preconditions = listOf("Sem combate ativo", "Companhia em condições de marcha"),
                        riskHint = "Gasto de rações diárias; teste de encontro errante e risco de se perder",
                        timePreview = "1 dia de viagem",
                        sourceRuleRef = "Classic Rules Tome p. 98",
                        executable = !game.expedition.immobile && !game.expedition.forcedMarchRestRequired,
                        missingRequirement = when {
                            game.expedition.immobile -> "Grupo imóvel pela carga excessiva"
                            game.expedition.forcedMarchRestRequired -> "Descanso obrigatório de 1 dia após marcha forçada"
                            else -> null
                        }
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-wild-travel-forage",
                        plainLabel = "Viajar + Forragear · ${secondaryDir.label}",
                        intentCandidate = "WILD_TRAVEL_FORAGE_DAY:${secondaryDir.key}",
                        textCandidate = "Avançamos por um dia em direção a ${secondaryDir.label.lowercase()} procurando alimento e caça no caminho.",
                        preconditions = listOf("Sem combate ativo", "Terreno com flora/fauna"),
                        riskHint = "Velocidade de marcha reduzida; teste de forrageamento",
                        timePreview = "1 dia de viagem",
                        sourceRuleRef = "Classic Rules Tome p. 98",
                        executable = !game.expedition.immobile && !game.expedition.forcedMarchRestRequired,
                        missingRequirement = when {
                            game.expedition.immobile -> "Grupo imóvel pela carga excessiva"
                            game.expedition.forcedMarchRestRequired -> "Descanso obrigatório de 1 dia após marcha forçada"
                            else -> null
                        }
                    )
                )
                suggestions.add(
                    ActionSuggestion(
                        id = "act-wild-rest-day",
                        plainLabel = "Descansar 1 dia completo nos ermos",
                        intentCandidate = "WILD_REST_DAY",
                        textCandidate = "Montamos acampamento e permanecemos parados um dia inteiro para descansar, cuidar dos ferimentos e reorganizar provisões.",
                        preconditions = listOf("Provisões diárias disponíveis"),
                        riskHint = "Consome rações diárias; recupera 1d3 PV; zera contador de marcha",
                        timePreview = "1 dia (24 horas)",
                        sourceRuleRef = "Classic Rules Tome p. 121"
                    )
                )
            }
        }

        return suggestions
    }
}
