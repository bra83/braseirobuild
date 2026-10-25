package braseiro.ose.app

import braseiro.ose.model.CampaignEnvelope
import braseiro.ose.model.EncounterPhase
import braseiro.ose.model.SpatialRef
import org.json.JSONArray
import org.json.JSONObject

/**
 * Three deterministic, read-only suggestions from player-visible state only.
 * They are reaction text with valid, executable mechanicalIntent where applicable.
 */
object ContextualDecisionSuggester {
    data class Suggestion(val label: String, val reaction: String, val mechanicalIntent: String? = null)

    fun suggest(envelope: CampaignEnvelope): List<Suggestion> {
        val state = envelope.campaignState
        val game = state.game
        val session = game.session
        val visible = session.visibleNarration.lowercase()
        val phase = session.phase.uppercase()
        val position = state.position.primary
        val encounter = game.encounter
        val primaryActor = state.party.characterIds.firstOrNull() ?: ""

        return when {
            phase.contains("COMBAT") || encounter?.phase == EncounterPhase.COMBAT -> {
                val targetOpponent = encounter?.combatants?.values?.firstOrNull { it.side.name == "OPPOSITION" && (it.alive && it.hpCurrentNumerator > 0) }?.actorId
                val weapon = game.inventory.items.values.firstOrNull { it.equipped && it.kind.name == "WEAPON" }?.itemId
                val attackIntent = if (primaryActor.isNotBlank() && targetOpponent != null && weapon != null) "COMBAT_ATTACK_MELEE:PARTY:$primaryActor:$targetOpponent:$weapon" else null
                val retreatIntent = if (primaryActor.isNotBlank() && encounter != null) "COMBAT_MOVE:PARTY:$primaryActor:RETREAT" else null
                listOf(
                    Suggestion("ATACAR CORPO A CORPO", "Avanço com arma em punho para desferir um ataque contra o adversário.", attackIntent),
                    Suggestion("RECUO CAUTELOSO", "Mantenho a guarda defensiva e recuo com cautela sem expor as costas.", retreatIntent),
                    Suggestion("AGUARDAR TURNO", "Aguardamos e observamos a movimentação inimiga.", "WAIT_TURN")
                )
            }
            position is SpatialRef.Dungeon -> listOf(
                Suggestion("PROCURAR NA SALA", "Examinamos minuciosamente o ambiente em busca de passagens secretas e armadilhas.", "DUNGEON_SEARCH_ROOM"),
                Suggestion("ESCUTAR NA PORTA", "Aproximo o ouvido da fresta da porta de madeira escutando ruídos com atenção.", "DUNGEON_LISTEN_DOOR"),
                Suggestion("FORÇAR PORTA", "Apoio o ombro contra a porta emperrada e forço o mecanismo com toda a força.", "DUNGEON_OPEN_STUCK_DOOR")
            )
            position is SpatialRef.Settlement -> listOf(
                Suggestion("DESCANSAR NA ESTALAGEM", "Procuramos a estalagem local para alugar quartos e descansar o dia inteiro recuperando PV.", "SETTLEMENT_REST_DAY"),
                Suggestion("AGUARDAR 1 TURNO", "Aguardamos observando o movimento e conversando com os viajantes.", "WAIT_TURN"),
                Suggestion("REORGANIZAR GRUPO", "Verificamos as provisões e a condição de todos antes de partir.")
            )
            position is SpatialRef.Hex -> hexTravelSuggestions(envelope, position)
            visible.contains("trilha") || visible.contains("caminho") || visible.contains("colina") -> listOf(
                Suggestion("AGUARDAR 1 TURNO", "Fazemos uma breve pausa de 10 minutos para observar o terreno.", "WAIT_TURN"),
                Suggestion("SEGUIR COM CAUTELA", "Seguimos pela rota visível com cautela, atentos a mudanças no terreno."),
                Suggestion("BUSCAR VANTAGEM", "Procuramos um ponto visível que permita observar melhor os arredores.")
            )
            else -> listOf(
                Suggestion("AGUARDAR 1 TURNO", "Fazemos uma pausa de 10 minutos para avaliar os arredores.", "WAIT_TURN"),
                Suggestion("AVANÇAR DEVAGAR", "Avançamos devagar pela opção de caminho já visível."),
                Suggestion("VERIFICAR O GRUPO", "Verificamos a condição do grupo e o que está imediatamente ao nosso redor.")
            )
        }.also { check(it.size == 3) }
    }

    private fun hexTravelSuggestions(envelope: CampaignEnvelope, position: SpatialRef.Hex): List<Suggestion> {
        data class D(val key: String, val label: String, val dq: Int, val dr: Int)
        val dirs = listOf(D("E","LESTE",1,0), D("SE","SUDESTE",0,1), D("NE","NORDESTE",1,-1), D("W","OESTE",-1,0), D("SW","SUDOESTE",-1,1), D("NW","NOROESTE",0,-1))
        val cells = envelope.campaignState.game.hexWorld?.cells.orEmpty()
        val valid = dirs.filter { d -> cells.any { it.q == position.q + d.dq && it.r == position.r + d.dr && it.hydrology.name != "SEA_OCEAN" } }
        val first = valid.getOrNull(0) ?: dirs[0]
        val second = valid.getOrNull(1) ?: dirs[1]
        return listOf(
            Suggestion("VIAJAR UM DIA · ${first.label}", "Viajamos por um dia para ${first.label.lowercase()} com cautela, observando o caminho.", "WILD_TRAVEL_DAY:${first.key}"),
            Suggestion("VIAJAR + FORRAGEAR · ${second.label}", "Seguimos por um dia para ${second.label.lowercase()} procurando alimento durante a viagem.", "WILD_TRAVEL_FORAGE_DAY:${second.key}"),
            Suggestion("DESCANSAR UM DIA", "Paramos para descansar um dia inteiro e recuperar as forças.", "WILD_REST_DAY")
        )
    }

    fun asJson(envelope: CampaignEnvelope): JSONArray = JSONArray(suggest(envelope).map {
        JSONObject().put("label", it.label).put("reaction", it.reaction).put("mechanicalIntent", it.mechanicalIntent ?: JSONObject.NULL)
    })
}
