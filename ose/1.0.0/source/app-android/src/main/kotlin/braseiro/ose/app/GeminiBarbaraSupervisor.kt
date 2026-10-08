package braseiro.ose.app

import braseiro.ose.barbara.BarbaraSupervisorPort
import braseiro.ose.model.CampaignEnvelope
import braseiro.ose.model.CharacterControlMode
import braseiro.ose.model.HexCellSnapshot
import braseiro.ose.model.InventoryItemKind
import braseiro.ose.model.LightSourceKind
import braseiro.ose.model.SpatialRef
import braseiro.ose.model.TerrainKind
import braseiro.ose.model.WeatherKind
import braseiro.ose.rules.api.RuleTrace

/**
 * Presentation-only Mestre Barbara. Mechanics are already committed by SessionEngine before this class runs.
 * Gemini may narrate those facts, but it cannot mutate campaign state, RNG, rules, resources, NPCs or position.
 */
class GeminiBarbaraSupervisor(
    private val credentials: GeminiCredentialStore,
    private val settings: GeminiRuntimeSettings,
    private val client: GeminiInteractionsClient
) : BarbaraSupervisorPort {

    override fun narrate(
        committed: CampaignEnvelope,
        playerReaction: String,
        mechanicalFeedback: String,
        trace: RuleTrace
    ): String {
        val key = credentials.get(GeminiCredentialStore.Slot.TEXT_SCENE)
            ?: return localNarration(committed, playerReaction, mechanicalFeedback)
        val model = settings.snapshot().textModel
        return runCatching {
            client.generateText(key, model, buildNarrationPrompt(committed, playerReaction, mechanicalFeedback, trace))
                .trim()
                .takeIf { it.length >= 120 && !looksTechnical(it) }
                ?: error("GEMINI_NARRATION_TOO_SHORT_OR_TECHNICAL")
        }.getOrElse {
            localNarration(committed, playerReaction, mechanicalFeedback)
        }
    }

    override fun help(readOnly: CampaignEnvelope, question: String): String {
        val key = credentials.get(GeminiCredentialStore.Slot.TEXT_SCENE)
            ?: return "Posso ajudar como Mestre sem alterar a campanha. Pergunta: “${question.trim()}”."
        val prompt = """
            Você é Barbara, Mestre de Old-School Essentials. Responda em português do Brasil, de forma clara e útil.
            Esta é uma consulta SOMENTE LEITURA: não avance tempo, não mova personagens, não role dados, não altere recursos e não determine consequências.
            Não mostre IDs internos, nomes de classes Kotlin, códigos em MAIÚSCULAS_COM_SUBLINHADO, coordenadas técnicas, nomes de streams RNG ou termos de implementação.
            ${publicContext(readOnly)}
            Pergunta do jogador: ${question.trim()}
        """.trimIndent()
        return runCatching { client.generateText(key, settings.snapshot().textModel, prompt).trim() }
            .getOrElse { "Não consegui consultar o Mestre Gemini agora. A campanha permaneceu inalterada." }
    }

    private fun buildNarrationPrompt(
        committed: CampaignEnvelope,
        playerReaction: String,
        mechanicalFeedback: String,
        trace: RuleTrace
    ): String = """
        Você é BARBARA, Mestre de uma campanha de Old-School Essentials em português do Brasil.

        FUNÇÃO:
        - A mecânica JÁ FOI resolvida e gravada pelo motor determinístico antes de você receber este pedido.
        - Você é responsável por transformar o estado confirmado em NARRAÇÃO DE RPG viva, literária, concreta e imersiva.
        - Você jamais altera regra, dado, posição, recurso, encontro ou resultado. Apenas narra o que já foi confirmado e o que é publicamente perceptível.

        ESTILO OBRIGATÓRIO:
        - Escreva como Mestre de RPG, nunca como depurador, sistema, relatório ou assistente técnico.
        - Em uma abertura ou mudança importante de lugar, produza de 4 a 7 parágrafos substanciais (aprox. 300–650 palavras).
        - Em respostas menores, ainda entregue 2 a 4 parágrafos sensoriais e úteis (aprox. 140–350 palavras), salvo quando uma resposta realmente curta for natural.
        - Descreva espaço, distância perceptível, luz, sons, cheiros, textura, clima aparente e elementos com os quais o jogador pode interagir quando houver base para isso.
        - Termine deixando a situação aberta à decisão do jogador.
        - O Mestre JAMAIS determina ações de personagens com controle PLAYER.
        - Personagens MASTER podem agir por decisão do Mestre, mas SOMENTE quando uma ação foi resolvida pelo motor e está confirmada no estado.
        - Personagens CURATED recebem PROPOSTAS do Mestre: não narre uma proposta pendente/rejeitada como se tivesse acontecido. Aguarde autorização explícita do jogador.
        - Não faça rolagem, gastos, deslocamento do grupo ou efeitos mecânicos por conta própria.
        - Para perguntas como “o que tem perto de mim?”, descreva o que é conhecido/visível no entorno. Não transforme automaticamente a pergunta em teste ou rolagem.

        FIREWALL DE APRESENTAÇÃO:
        - NUNCA escreva PLAYER_REACTION, WILD_TRAVEL, WILD_REST, RULES_DICE, ACTIVE, CHARACTER_LOADOUT, world-1, “posição canônica”, IDs internos, nomes de variáveis, códigos de regra ou mensagens de arbitragem.
        - NUNCA exponha o texto técnico de feedback abaixo literalmente. Use-o somente como restrição factual.
        - NUNCA revele informação oculta. Se algo não está confirmado/visível, preserve a incerteza.
        - Se o feedback disser que não houve mecânica ou que a intenção requer arbitragem, isso NÃO é fala para o jogador: simplesmente responda narrativamente a partir do contexto visível.

        ${publicContext(committed)}

        AÇÃO/PERGUNTA DO JOGADOR:
        ${playerReaction.trim()}

        RESULTADO MECÂNICO JÁ CONFIRMADO (uso interno; não repetir literalmente):
        ${mechanicalFeedback.trim().ifBlank { "Nenhuma mutação mecânica foi aplicada." }}

        EVIDÊNCIA MECÂNICA: ${trace.status}. Não cite este campo ao jogador.

        Agora escreva SOMENTE a narração do Mestre, sem cabeçalho técnico e sem explicar seu processo.
    """.trimIndent()

    private fun publicContext(envelope: CampaignEnvelope): String {
        val state = envelope.campaignState
        val game = state.game
        val partyNames = state.party.characterIds.mapNotNull { id -> game.characters[id]?.let { c -> c.name + " (" + when(game.characterControlModes[id] ?: CharacterControlMode.PLAYER) { CharacterControlMode.PLAYER -> "decisão exclusiva do jogador"; CharacterControlMode.MASTER -> "autonomia do Mestre"; CharacterControlMode.CURATED -> "Mestre com aprovação do jogador" } + ")" } }.ifEmpty { listOf("a companhia") }
        val time = "dia ${state.time.days + 1}, turno ${state.time.turns}"
        val place = when (val p = state.position.primary) {
            is SpatialRef.Hex -> {
                val world = game.hexWorld
                val here = world?.cells?.firstOrNull { it.q == p.q && it.r == p.r }
                val nearby = world?.let { w -> visibleNeighbors(w.cells, p.q, p.r) }.orEmpty()
                buildString {
                    append("A companhia está nos ermos, em terreno ${terrainLabel(here)}.")
                    if (nearby.isNotEmpty()) {
                        append(" Arredores conhecidos/visíveis: ")
                        append(nearby.joinToString("; ") { "${it.first}: ${terrainLabel(it.second)}" })
                        append('.')
                    }
                }
            }
            is SpatialRef.Dungeon -> {
                val dungeon = game.dungeon
                val visible = dungeon?.visibleNodeIds.orEmpty().size
                "A companhia está dentro de uma masmorra, numa área já alcançada. Há $visible área(s) atualmente visível(is) no mapa do grupo."
            }
            is SpatialRef.Settlement -> {
                val placeName = game.settlement?.locations?.firstOrNull { it.locationId == p.anchorId }?.name
                    ?: "um local do assentamento"
                val present = game.npcs.values.filter { it.spatialRef == p }.map { it.name }
                buildString {
                    append("A companhia está em $placeName.")
                    if (present.isNotEmpty()) append(" Pessoas presentes e perceptíveis: ${present.joinToString(", ")}.")
                }
            }
            is SpatialRef.Scene -> "A companhia está numa cena em andamento já estabelecida pelo Mestre."
        }
        val rations = game.inventory.items.values.filter { it.kind == InventoryItemKind.FOOD }.sumOf { it.quantity }
        val torches = game.inventory.items.values.filter { it.lightSourceKind == LightSourceKind.TORCH }.sumOf { it.quantity }
        val coinUnits = game.inventory.items.values.filter { it.kind == InventoryItemKind.COIN }.sumOf { it.quantity }
        val resources = buildList {
            add("tochas: $torches")
            add("rações: $rations")
            if (coinUnits > 0) add("moedas carregadas: $coinUnits")
        }
        return buildString {
            appendLine("CONTEXTO PÚBLICO DA CAMPANHA:")
            appendLine("- Grupo: ${partyNames.joinToString(", ")}.")
            appendLine("- Tempo de jogo: $time.")
            appendLine("- Clima: ${weatherLabel(game.livingWorld.weather.kind)}.")
            appendLine("- Lugar: $place")
            if (resources.isNotEmpty()) appendLine("- Recursos visíveis derivados do inventário atual: ${resources.joinToString(", ")}.")
        }.trim()
    }

    private fun visibleNeighbors(cells: List<HexCellSnapshot>, q: Int, r: Int): List<Pair<String, HexCellSnapshot>> {
        val dirs = listOf(
            Triple("leste", 1, 0), Triple("sudeste", 0, 1), Triple("nordeste", 1, -1),
            Triple("oeste", -1, 0), Triple("sudoeste", -1, 1), Triple("noroeste", 0, -1)
        )
        return dirs.mapNotNull { (label, dq, dr) ->
            cells.firstOrNull { it.q == q + dq && it.r == r + dr && (it.known || it.visible) }?.let { label to it }
        }
    }

    private fun terrainLabel(cell: HexCellSnapshot?): String {
        if (cell == null) return "não determinado"
        if (cell.hydrology.name == "SEA_OCEAN") return "mar aberto"
        if (cell.hydrology.name == "LAKE_RIVER") return "água doce"
        return when (cell.terrain) {
            TerrainKind.CLEAR_GRASSLANDS -> "campina aberta"
            TerrainKind.HILLS -> "colinas"
            TerrainKind.MOUNTAINS -> "montanhas"
            TerrainKind.FOREST -> "floresta"
            TerrainKind.DESERT -> "terreno árido"
            TerrainKind.SWAMP -> "pântano"
            TerrainKind.JUNGLE -> "mata densa"
            TerrainKind.BARREN_ADV -> "terras estéreis"
            TerrainKind.BROKEN_LANDS_ADV -> "terreno acidentado"
        }
    }

    private fun weatherLabel(kind: WeatherKind): String = when (kind) {
        WeatherKind.CLEAR -> "céu limpo"
        WeatherKind.OVERCAST -> "céu encoberto"
        WeatherKind.RAIN -> "chuva"
        WeatherKind.FOG -> "neblina"
        WeatherKind.STORM -> "tempestade"
        WeatherKind.COLD -> "frio intenso"
        WeatherKind.HOT -> "calor forte"
    }

    private fun localNarration(committed: CampaignEnvelope, reaction: String, feedback: String): String {
        val context = publicContext(committed)
            .lineSequence().drop(1).joinToString(" ") { it.removePrefix("- ") }
        val consequence = when {
            feedback.contains("encontr", ignoreCase = true) -> "Enquanto vocês avançam, alguma coisa rompe a monotonia do caminho: há sinais claros de presença adiante, suficientes para obrigar o grupo a prestar atenção antes de seguir no mesmo ritmo."
            feedback.contains("descans", ignoreCase = true) -> "As horas passam sem pressa. O grupo encontra tempo para baixar as mochilas, cuidar do corpo e deixar a tensão da jornada perder um pouco da força."
            feedback.contains("Viagem:", ignoreCase = true) -> "A paisagem muda gradualmente conforme a companhia atravessa o terreno, com cada trecho revelando novas linhas do horizonte e novos pontos de referência."
            else -> "O mundo não responde com números ou códigos. Ele responde com aquilo que os personagens conseguem perceber daqui: formas, sons, caminhos, obstáculos e sinais que podem ser investigados antes da próxima decisão."
        }
        return """
            ${context.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}

            Você declarou: “${reaction.trim()}”. Barbara considera apenas aquilo que já está diante dos personagens. $consequence

            Não há uma decisão tomada por você ainda além do que declarou. Observe o que foi revelado, escolha o que merece sua atenção e diga como seu personagem procede.
        """.trimIndent()
    }

    private fun looksTechnical(text: String): Boolean {
        val upper = text.uppercase()
        return listOf("PLAYER_REACTION", "WILD_TRAVEL", "WILD_REST", "RULES_DICE", "CHARACTER_LOADOUT", "WORLD-1", "POSIÇÃO CANÔNICA")
            .any { it in upper }
    }
}
